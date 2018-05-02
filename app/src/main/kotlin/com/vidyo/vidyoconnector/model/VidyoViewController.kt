package com.vidyo.vidyoconnector.model

import android.Manifest
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.content.ContextCompat
import com.vidyo.vidyoconnector.util.apply
import io.reactivex.Maybe
import io.reactivex.internal.operators.observable.ObservableFromArray


class VidyoViewControllerDelegate(delegate: VidyoViewController) : VidyoViewController by delegate

interface VidyoViewController {

    val isShowToolbar: LiveData<Boolean>
    val isShowInput: LiveData<Boolean>
    val isConnecting: LiveData<Boolean>
    val isShowVersion: MutableLiveData<Boolean>
    val connectionStatus: LiveData<String>
    val connectButtonState: LiveData<Boolean>
    val allowReconnect: LiveData<Boolean>

    val connectorStateObserver: Observer<ConnectorState>
    var router: MainActivityRouter?

    fun applyConfiguration(connection: ConnectionData, options: OptionsData)
    fun requirePermissions(context: Context): Maybe<List<String>>
    fun toggleConnect(closure: (Boolean) -> Unit)
    fun toggleToolbarVisibility()
}

class MutableVidyoViewController : VidyoViewController {

    override val isShowToolbar: LiveData<Boolean>
        get() = mutableIsShowToolbar
    override val isShowInput: LiveData<Boolean>
        get() = mutableIsShowInput
    override val isConnecting: LiveData<Boolean>
        get() = mutableIsConnecting
    override val connectionStatus: LiveData<String>
        get() = mutableConnectionStatus
    override val connectButtonState: LiveData<Boolean>
        get() = mutableConnectButtonState
    override val allowReconnect: LiveData<Boolean>
        get() = mutableAllowReconnect
    override val isShowVersion = MutableLiveData<Boolean>().apply(false)

    override var router: MainActivityRouter? = null

    private val mutableIsShowToolbar = MutableLiveData<Boolean>().apply(true)
    private val mutableIsShowInput = MutableLiveData<Boolean>().apply(true)
    private val mutableConnectionStatus = MutableLiveData<String>().apply("Ready to Connect")
    private val mutableConnectButtonState = MutableLiveData<Boolean>().apply(false)
    private val mutableAllowReconnect = MutableLiveData<Boolean>().apply(true)
    private val mutableIsConnecting = MutableLiveData<Boolean>()

    private var options: OptionsData? = null
    private var isConnected = false

    // The state of the VidyoConnector connection changed, reconfigure the UI.
    // If connected, dismiss the controls layout
    override val connectorStateObserver = Observer<ConnectorState> {
        // Execute this code on the main thread since it is updating the UI layout.
        it?.let {
            // Set the status text in the toolbar.
            when (it) {
                is ConnectorState.Failure ->
                    mutableConnectionStatus.postValue(it.reason.toString())

                is ConnectorState.DisconnectedUnexpected ->
                    mutableConnectionStatus.postValue(it.reason.toString())

                else -> mStateDescription[it]?.let {
                    mutableConnectionStatus.postValue(it)
                }
            }

            // Depending on the state, do a subset of the following:
            // - update the toggle connect button to either start call or end call image: mToggleConnectButton
            // - display toolbar in case it is hidden: mToolbarLayout
            // - show/hide the connection spinner: mConnectionSpinner
            // - show/hide the input form: mControlsLayout
            isConnected = false
            when (it) {
                is ConnectorState.Connecting -> {
                    mutableConnectButtonState.postValue(true)
                    mutableIsConnecting.postValue(true)
                }

                is ConnectorState.Connected -> {
                    mutableConnectButtonState.postValue(true)
                    mutableIsShowInput.postValue(false)
                    mutableIsConnecting.postValue(false)
                    isConnected = true
                }

                is ConnectorState.Disconnecting -> {
                    // The button just switched to the callStart image.
                    // Change the button back to the callEnd image because do not want to assume that the Disconnect
                    // call will actually end the call. Need to wait for the callback to be received
                    // before swapping to the callStart image.
                    mutableConnectButtonState.postValue(true)
                }

                is ConnectorState.Disconnected,
                is ConnectorState.DisconnectedUnexpected,
                is ConnectorState.Failure,
                is ConnectorState.FailureInvalidResource -> {
                    val disconnected = it == ConnectorState.Disconnected

                    mutableIsShowToolbar.postValue(true)
                    mutableConnectButtonState.postValue(false)
                    mutableIsConnecting.postValue(false)

                    // If a return URL was provided as an input parameter, then return to that application
                    options?.returnURL?.let {
                        router?.returnToLaunchingApplication(it, if (disconnected) 1 else 0)
                    }

                    // If the allow-reconnect flag is set to false and a normal (non-failure) disconnect occurred,
                    // then disable the toggle connect button, in order to prevent reconnection.
                    if (options?.allowReconnect != true && disconnected) {
                        mutableAllowReconnect.postValue(false)
                        mutableConnectionStatus.postValue("Call ended")
                    }

                    if (options?.hideConfig == false) {
                        // Display the controls
                        mutableIsShowInput.postValue(true)
                    }
                }
            }
        }
    }

    override fun applyConfiguration(connection: ConnectionData, options: OptionsData) {
        this.options = options
    }

    override fun requirePermissions(context: Context): Maybe<List<String>> = checkPermissionsInternally(context)

    private fun checkPermissionsInternally(context: Context) = when {
        Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 -> {
            ObservableFromArray(mPermissions).filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }.toList().flatMapMaybe {
                if (it.isEmpty()) Maybe.empty() else Maybe.just(it)
            }
        }
        else -> Maybe.empty()
    }

    override fun toggleConnect(closure: (Boolean) -> Unit) {
        val newState = !(mutableConnectButtonState.value ?: false)
        mutableConnectButtonState.value = newState

        closure(newState)
    }

    override fun toggleToolbarVisibility() {
        if (isConnected) {
            val newState = !(mutableIsShowToolbar.value ?: true)
            mutableIsShowToolbar.postValue(newState)
        } else {
            mutableIsShowToolbar.postValue(true)
        }
    }

    companion object {

        // Map the application state to the status to display in the toolbar.
        private val mStateDescription = object : HashMap<ConnectorState, String>() {
            init {
                put(ConnectorState.Connecting, "Connecting...")
                put(ConnectorState.Connected, "Connected")
                put(ConnectorState.Disconnecting, "Disconnecting...")
                put(ConnectorState.Disconnected, "Disconnected")
//                put(ConnectorState.DisconnectedUnexpected(), "Unexpected disconnection")
//                put(ConnectorState.Failure, "Connection failed")
                put(ConnectorState.FailureInvalidResource, "Connection failed")
            }
        }

        // Helps check whether app has permission to access what is declared in its manifest.
        // - Permissions from app's manifest that have a "protection level" of "dangerous".
        private val mPermissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
