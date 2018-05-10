package com.vidyo.vidyoconnector.model

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.VidyoClient.Device.LocalCamera
import com.vidyo.vidyoconnector.expansions.ConnectorFactory
import com.vidyo.vidyoconnector.expansions.LocalCameraEventAdapter
import com.vidyo.vidyoconnector.expansions.MultiCastLocalCameraEventListener
import com.vidyo.vidyoconnector.logger.EventLogger
import com.vidyo.vidyoconnector.logger.LocalCameraEventLogger
import com.vidyo.vidyoconnector.logger.NetworkInterfaceEventLogger
import java.util.logging.Logger
import javax.inject.Inject

class ViewPort(
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0
)

interface ViewPortListener {

    val rect: ViewPort
}

interface VidyoConnectorController {
    val debug: LiveData<Boolean>
    val version: LiveData<String>

    val mediaController: VidyoMediaController

    fun setDebug(enabled: Boolean)

    fun onCleared()

    fun connect(connection: ConnectionData)
    fun disconnect()

    fun start()
    fun stop()

    val connectionState: LiveData<ConnectorState>
    var viewFrame: Any?
    fun updateViewPort()

    var viewPortListener: ViewPortListener?
}

internal class VidyoConnectorControllerDelegate(delegate: VidyoConnectorController) :
        VidyoConnectorController by delegate

internal class MutableVidyoConnectorController : VidyoConnectorController {
    private val mutableControlOptions = MutableVidyoMediaController()
    override val mediaController: VidyoMediaController = VidyoMediaControllerDelegate(mutableControlOptions)

    // CustomLogger members
    private val logger = Logger.getLogger("vidyo.connector")
    private val networkInterfaceLogger = NetworkInterfaceEventLogger()
    private val eventLogger = EventLogger()


    // Exposed data
    override val debug: LiveData<Boolean>
        get() = mutableDebug
    override val version: LiveData<String>
        get() = mutableVersion
    override val connectionState: LiveData<ConnectorState>
        get() = mutableConnectorState

    override var viewFrame: Any? = null
        set(value) {
            field = value
            mutableViewFrame.postValue(value)
        }

    val localCamera: LiveData<LocalCamera>
        get() = mutableLocalCamera

    override var viewPortListener: ViewPortListener? = null

    // Mutable data
//    private val mutableSdkData = MutableSDKData()
//    private val sdkDelegate = SDKDelegate(mutableSdkData)
    private val mutableDebug = MutableLiveData<Boolean>()
    private val mutableViewFrame = MutableLiveData<Any>()
    private val mutableLocalCamera = MutableLiveData<LocalCamera>()
    private var mutableConnectorState = MutableLiveData<ConnectorState>()
    private val mutableVersion = MutableLiveData<String>()

    // Important members
    private val multiCastLocalCameraEventListener = MultiCastLocalCameraEventListener()

    private var connector: Connector? = null
    private var devicesSelected = false

    private val localCameraHolder = object : LocalCameraEventAdapter() {

        override fun onLocalCameraSelected(p0: LocalCamera?) {
            // If a camera is selected, then update mLastSelectedCamera.
            super.onLocalCameraSelected(p0)
            p0?.let {
                mutableLocalCamera.postValue(it)
            }
        }
    }

    init {
        multiCastLocalCameraEventListener.add(LocalCameraEventLogger())
        multiCastLocalCameraEventListener.add(localCameraHolder)

        mutableDebug.observeForever {
            applyDebug(it == true)
        }

        mutableViewFrame.observeForever {
            when (it) {
                null -> disposeConnector()
                else -> updateConnector(it)
            }
        }
    }

    private val connectorFactory: ConnectorFactory

    @Suppress("ConvertSecondaryConstructorToPrimary")
    @Inject constructor(connectorFactory: ConnectorFactory) {
        this.connectorFactory = connectorFactory
    }

    private fun assignConnector(connector: Connector): Connector {
        this.connector = connector.also {
            mutableVersion.postValue("VidyoClient-AndroidSDK " + it.version)
        }.also { it ->
            if (!it.registerLocalCameraEventListener(multiCastLocalCameraEventListener)) {
                logger.warning("registerLocalCameraEventListener failed")
            }
            // Register for network interface events
            if (!it.registerNetworkInterfaceEventListener(networkInterfaceLogger)) {
                logger.warning("registerNetworkInterfaceEventListener failed")
            }
            // Register for log events
            if (!it.registerLogEventListener(eventLogger, "info@VidyoClient info@VidyoConnector warning")) {
                logger.warning("registerLogEventListener failed")
            }
        }.also {
            mutableControlOptions.connector = connector
        }

        applyDebug(mutableDebug.value == true)
        return connector
    }

    private fun applyDebug(debug: Boolean) {
        connector?.let { connector ->
            if (debug) {
                connector.enableDebug(DEBUG_PORT, "warning info@VidyoClient info@VidyoConnector")
            } else {
                connector.disableDebug()
            }
        }
    }

    private fun updateConnector(it: Any) {
        val connector = connector
        (if (connector == null) {
            assignConnector(createConnector(it))
        } else {
            logger.info("Constructing Connector")
            connector.assignViewToCompositeRenderer(
                    it,
                    Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                    MAX_REMOTE_PARTICIPANTS
            )
            connector
        }).let {
            updateViewPortInternally(it)
        }
    }

    private fun createConnector(it: Any): Connector = connectorFactory.create(it)

    override fun updateViewPort() {
        connector?.let {
            updateViewPortInternally(it)
        }
    }

    internal fun updateViewPortInternally(it: Connector): Boolean? {
        return viewPortListener?.let { listener ->
            viewFrame?.let { frame ->
                val rect = listener.rect
                logger.info("""ShowViewAt:
                            |x = ${rect.x},
                            |y = ${rect.y},
                            |w = ${rect.width},
                            |h = ${rect.height}""".trimMargin())

                it.showViewAt(frame, rect.x, rect.y, rect.width, rect.height)
            }
        }
    }

    private fun disposeConnector() {
        destroy()
    }

    override fun setDebug(enabled: Boolean) {
        mutableDebug.postValue(enabled)
    }

    override fun onCleared() {
        destroy()
    }

    override fun connect(connection: ConnectionData) {
        val trimmedConnection = connection.trimmed()
        return when {
            trimmedConnection.resourceId.contains(Regex("[ @]")) -> changeState(ConnectorState.FailureInvalidResource)
            else -> connectInternally(trimmedConnection)
        }
    }

    private fun connectInternally(connection: ConnectionData) {
        val connector = connector
        if (connector != null) {
            changeState(ConnectorState.Connecting)
            connector.connect(
                    connection.host,
                    connection.token,
                    connection.displayName,
                    connection.resourceId,
                    connectionDelegate
            ).also { status ->
                //                if (!status) {
//                    changeState(ConnectorState.Failure())
//                }
                logger.info("VidyoConnectorConnect status = $status")
            }
        }
    }

    override fun disconnect() {
        connector?.let {
            changeState(ConnectorState.Disconnecting)
            it.disconnect()
        }
    }

    private fun changeState(state: ConnectorState) {
        logger.info("changeState: $state")
        mutableConnectorState.postValue(state)
    }

    /**
     *  Connector Events
     */
    private var connectionDelegate = object : Connector.IConnect {

        // Handle successful connection.
        override fun onSuccess() {
            logger.info("onSuccess: successfully connected.")
            changeState(ConnectorState.Connected)
        }

        // Handle attempted connection failure.
        override fun onFailure(reason: Connector.ConnectorFailReason) {
            logger.info("onFailure: connection attempt failed, reason = " + reason.toString())
            changeState(ConnectorState.Failure(reason))
        }

        // Handle an existing session being disconnected.
        override fun onDisconnected(reason: Connector.ConnectorDisconnectReason) {
            if (reason == Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
                logger.info("onDisconnected: successfully disconnected, reason = " + reason.toString())
                changeState(ConnectorState.Disconnected)
            } else {
                logger.info("onDisconnected: unexpected disconnection, reason = " + reason.toString())
                changeState(ConnectorState.DisconnectedUnexpected(reason))
            }
        }
    }

    override fun start() {
        if (!devicesSelected) {
            devicesSelected = true

            connector?.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground)
            connector?.let {
                mutableLocalCamera.value?.let { camera ->
                    it.selectLocalCamera(camera)
                }
                it.selectDefaultMicrophone()
                it.selectDefaultSpeaker()
            }
            mutableControlOptions.apply()
        }
    }

    override fun stop() {
        connector?.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Background)
        connector?.let {
            //            it.selectLocalCamera(null)
            it.selectLocalMicrophone(null)
            it.selectLocalSpeaker(null)
        }

        devicesSelected = false
    }

    private fun destroy() {
        connector?.disable()
        connector?.let {
            it.selectLocalCamera(null)
            it.selectLocalMicrophone(null)
            it.selectLocalSpeaker(null)
        }
        connector = null
        devicesSelected = false
    }

    companion object {

        const val MAX_REMOTE_PARTICIPANTS = 15
        const val DEBUG_PORT = 7776
    }
}


fun VidyoConnectorController.toggleDebug() {
    setDebug(debug.value != true)
}

val VidyoConnectorController.isRunning: Boolean
    get() = when (connectionState.value) {
        is ConnectorState.Connecting,
        is ConnectorState.Connected -> true
        else -> false
    }