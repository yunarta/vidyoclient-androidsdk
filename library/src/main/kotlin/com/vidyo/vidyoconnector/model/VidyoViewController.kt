package com.vidyo.vidyoconnector.model

import android.Manifest
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES.LOLLIPOP_MR1
import com.vidyo.vidyoconnector.R
import com.vidyo.vidyoconnector.expansions.BuildAccessor
import com.vidyo.vidyoconnector.expansions.InjectablePermissionChecker
import com.vidyo.vidyoconnector.util.apply
import io.reactivex.Maybe
import io.reactivex.internal.operators.observable.ObservableFromArray
import javax.inject.Inject

interface VidyoViewController {

    val isShowToolbar: LiveData<Boolean>
    val isShowInput: LiveData<Boolean>
    val isConnecting: LiveData<Boolean>
    val isShowVersion: MutableLiveData<Boolean>
    val connectionStatus: LiveData<String>
    val connectionStatusResource: LiveData<Int>
    val connectButtonState: LiveData<Boolean>
    val allowReconnect: LiveData<Boolean>

    val connectorStateObserver: Observer<ConnectorState>
    var router: VidyoActivityRouter?

    fun applyConfiguration(options: OptionsData)
    fun requirePermissions(context: Context): Maybe<List<String>>
    fun toggleConnect(closure: (Boolean) -> Unit)
    fun toggleToolbarVisibility()
}

internal class VidyoViewControllerDelegate(delegate: VidyoViewController) : VidyoViewController by delegate
internal class MutableVidyoViewController : VidyoViewController {

    override val isShowToolbar: LiveData<Boolean>
        get() = mutableIsShowToolbar
    override val isShowInput: LiveData<Boolean>
        get() = mutableIsShowInput
    override val isConnecting: LiveData<Boolean>
        get() = mutableIsConnecting
    override val connectionStatus: LiveData<String>
        get() = mutableConnectionStatus
    override val connectionStatusResource: LiveData<Int>
        get() = mutableConnectionStatusResource
    override val connectButtonState: LiveData<Boolean>
        get() = mutableConnectButtonState
    override val allowReconnect: LiveData<Boolean>
        get() = mutableAllowReconnect
    override val isShowVersion = MutableLiveData<Boolean>().apply(false)

    override var router: VidyoActivityRouter? = null

    private val mutableIsShowToolbar = MutableLiveData<Boolean>().apply(true)
    private val mutableIsShowInput = MutableLiveData<Boolean>().apply(true)
    private val mutableConnectionStatus = MutableLiveData<String>().apply("Ready to Connect")
    private val mutableConnectionStatusResource = MutableLiveData<Int>().apply(R.string.connection_state_idle)
    private val mutableConnectButtonState = MutableLiveData<Boolean>().apply(false)
    private val mutableAllowReconnect = MutableLiveData<Boolean>().apply(true)
    private val mutableIsConnecting = MutableLiveData<Boolean>()

    private var options: OptionsData? = null
    private var isConnected = false

    private val buildAccessor: BuildAccessor
    private val permissionChecker: InjectablePermissionChecker

    @Suppress("ConvertSecondaryConstructorToPrimary")
    @Inject constructor(buildAccessor: BuildAccessor, permissionChecker: InjectablePermissionChecker) {
        this.buildAccessor = buildAccessor
        this.permissionChecker = permissionChecker
    }

    // The state of the VidyoConnector connection changed, reconfigure the UI.
    // If connected, dismiss the controls layout
    override val connectorStateObserver = Observer<ConnectorState> {
        // Execute this code on the main thread since it is updating the UI layout.
        it?.let {
            // Set the status text in the toolbar.
            when (it) {
                is ConnectorState.Failure -> {
                    mutableConnectionStatus.postValue(it.reason.toString())
                    mutableConnectionStatusResource.postValue(0)
                }

                is ConnectorState.DisconnectedUnexpected -> {
                    mutableConnectionStatus.postValue(it.reason.toString())
                    mutableConnectionStatusResource.postValue(0)
                }

                else -> {
                    stateIntDescription.getOrDefault(it, 0).let {
                        mutableConnectionStatusResource.postValue(it)
                    }
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
                        mutableIsShowInput.postValue(false)

                        mutableConnectionStatus.postValue("Call ended")
                    } else {
                        mutableAllowReconnect.postValue(true)
                        mutableIsShowInput.postValue(options?.hideConfig != true)
                    }
                }
            }
        }
    }

    override fun applyConfiguration(options: OptionsData) {
        this.options = options
        mutableAllowReconnect.value = options.allowReconnect
    }

    override fun requirePermissions(context: Context): Maybe<List<String>> {
        return checkPermissionsInternally(context)
    }


    private fun checkPermissionsInternally(context: Context): Maybe<List<String>> {
        return when {
            buildAccessor.sdkVersion() > LOLLIPOP_MR1 -> {
                ObservableFromArray(mPermissions).filter {
                    permissionChecker.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }.toList().flatMapMaybe { it: List<String> ->
                    if (it.isEmpty()) Maybe.empty() else Maybe.just(it)
                }
            }
            else -> Maybe.empty()
        }
    }

    override fun toggleConnect(closure: (Boolean) -> Unit) {
        val newState = mutableConnectButtonState.value != true
        mutableConnectButtonState.value = newState

        closure(newState)
    }

    override fun toggleToolbarVisibility() {
        if (isConnected) {
            val newState = mutableIsShowToolbar.value == false
            mutableIsShowToolbar.postValue(newState)
        } else {
            mutableIsShowToolbar.postValue(true)
        }
    }

    companion object {

        // Map the application state to the status to display in the toolbar.
        private val mStateDescription = mapOf(
                ConnectorState.Connecting to "Connecting...",
                ConnectorState.Connected to "Connected",
                ConnectorState.Disconnecting to "Disconnecting...",
                ConnectorState.Disconnected to "Disconnected",
                ConnectorState.FailureInvalidResource to "Connection failed"
        )

        // Map the application state to the status to display in the toolbar.
        private val stateIntDescription = mapOf(
                ConnectorState.Connecting to R.string.connection_state_connecting,
                ConnectorState.Connected to R.string.connection_state_connected,
                ConnectorState.Disconnecting to R.string.connection_state_disconnecting,
                ConnectorState.Disconnected to R.string.connection_state_disconnected,
                ConnectorState.FailureInvalidResource to R.string.connection_state_failure_invalid_resource
        )

        // Helps check whether app has permission to access what is declared in its manifest.
        // - Permissions from app's manifest that have a "protection level" of "dangerous".
        private val mPermissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
