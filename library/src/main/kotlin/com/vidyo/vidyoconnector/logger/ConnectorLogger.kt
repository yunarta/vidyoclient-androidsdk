package com.vidyo.vidyoconnector.logger

import com.vidyo.VidyoClient.Connector.Connector.IRegisterLocalCameraEventListener
import com.vidyo.VidyoClient.Connector.Connector.IRegisterLogEventListener
import com.vidyo.VidyoClient.Connector.Connector.IRegisterNetworkInterfaceEventListener
import com.vidyo.VidyoClient.Device.Device
import com.vidyo.VidyoClient.Device.LocalCamera
import com.vidyo.VidyoClient.Endpoint.LogRecord
import com.vidyo.VidyoClient.NetworkInterface
import com.vidyo.VidyoClient.NetworkInterface.NetworkInterfaceTransportType
import java.util.logging.Logger

internal open class NetworkInterfaceEventLogger : IRegisterNetworkInterfaceEventListener {

    private val logger = Logger.getLogger("vidyo.network")

    // Handle network interface events
    override fun onNetworkInterfaceAdded(vidyoNetworkInterface: NetworkInterface) {
        logger.finest("""onNetworkInterfaceAdded: name=${vidyoNetworkInterface.getName()}
            |address=${vidyoNetworkInterface.getAddress()}
            |type=${vidyoNetworkInterface.getType()}
            |family=${vidyoNetworkInterface.getFamily()}""".trimMargin())
    }

    override fun onNetworkInterfaceRemoved(vidyoNetworkInterface: NetworkInterface) {
        logger.finest("""onNetworkInterfaceRemoved:
            |name=${vidyoNetworkInterface.getName()}
            |address=${vidyoNetworkInterface.getAddress()}
            |type=${vidyoNetworkInterface.getType()}
            |family=${vidyoNetworkInterface.getFamily()}""".trimMargin())
    }

    override fun onNetworkInterfaceSelected(vidyoNetworkInterface: NetworkInterface,
                                            vidyoNetworkInterfaceTransportType: NetworkInterfaceTransportType) {
        logger.finest("""onNetworkInterfaceSelected:
            |name=${vidyoNetworkInterface.getName()}
            |address=${vidyoNetworkInterface.getAddress()}
            |type=${vidyoNetworkInterface.getType()}
            |family=${vidyoNetworkInterface.getFamily()}""".trimMargin())
    }

    override fun onNetworkInterfaceStateUpdated(vidyoNetworkInterface: NetworkInterface,
                                                vidyoNetworkInterfaceState: NetworkInterface.NetworkInterfaceState) {
        logger.finest("""onNetworkInterfaceStateUpdated:
            |name=${vidyoNetworkInterface.getName()}
            |address=${vidyoNetworkInterface.getAddress()}
            |type=${vidyoNetworkInterface.getType()}
            |family=${vidyoNetworkInterface.getFamily()}
            |state=$vidyoNetworkInterfaceState""".trimMargin())
    }
}

internal open class EventLogger : IRegisterLogEventListener {

    private val logger = Logger.getLogger("vidyo.event")

    override fun onLog(logRecord: LogRecord?) {
        logRecord?.let {
            val message = """${it.level} ${it.functionName} : ${it.message}"""
            when (it.level ?: LogRecord.LogLevel.VIDYO_LOGLEVEL_DEBUG) {
                LogRecord.LogLevel.VIDYO_LOGLEVEL_FATAL ->
                    logger.severe(message)

                LogRecord.LogLevel.VIDYO_LOGLEVEL_ERROR ->
                    logger.severe(message)

                LogRecord.LogLevel.VIDYO_LOGLEVEL_WARNING ->
                    logger.warning(message)

                LogRecord.LogLevel.VIDYO_LOGLEVEL_INFO ->
                    logger.info(message)

                LogRecord.LogLevel.VIDYO_LOGLEVEL_DEBUG ->
                    logger.finest(message)

                LogRecord.LogLevel.VIDYO_LOGLEVEL_SENT,
                LogRecord.LogLevel.VIDYO_LOGLEVEL_RECEIVED ->
                    logger.finest(message)

                LogRecord.LogLevel.VIDYO_LOGLEVEL_ENTER,
                LogRecord.LogLevel.VIDYO_LOGLEVEL_LEAVE,
                LogRecord.LogLevel.VIDYO_LOGLEVEL_INVALID ->
                    logger.finest(message)
            }
        }
    }
}

internal open class LocalCameraEventLogger : IRegisterLocalCameraEventListener {

    private val logger = Logger.getLogger("vidyo.camera")

    // Handle local camera events.
    override fun onLocalCameraAdded(localCamera: LocalCamera) {
        logger.finest("""onLocalCameraAdded: ${localCamera.getName()}""")
    }

    override fun onLocalCameraRemoved(localCamera: LocalCamera) {
        logger.finest("""onLocalCameraRemoved: ${localCamera.getName()}""")
    }

    override fun onLocalCameraSelected(localCamera: LocalCamera?) {
        logger.finest("""onLocalCameraSelected: ${if (localCamera == null) "none" else localCamera.getName()}""")
    }

    override fun onLocalCameraStateUpdated(localCamera: LocalCamera, state: Device.DeviceState) {
        logger.finest("""onLocalCameraStateUpdated: name=${localCamera.getName()} state=$state""")
    }
}