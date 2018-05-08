package com.vidyo.vidyoconnector.logger

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.vidyo.VidyoClient.Device.Device
import com.vidyo.VidyoClient.Device.LocalCamera
import com.vidyo.VidyoClient.Endpoint.LogRecord
import com.vidyo.VidyoClient.NetworkInterface
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

class `NetworkInterfaceEventLogger tests` {

    @Test
    fun `test logger`() {
        val logger = mock(Handler::class.java)
        Logger.getLogger("vidyo").apply { level = Level.ALL }.addHandler(logger)

        NetworkInterfaceEventLogger().onNetworkInterfaceAdded(Mockito.mock(NetworkInterface::class.java))
        verify(logger, times(1)).publish(any())
        reset(logger)

        NetworkInterfaceEventLogger().onNetworkInterfaceRemoved(Mockito.mock(NetworkInterface::class.java))
        verify(logger, times(1)).publish(any())
        reset(logger)

        NetworkInterfaceEventLogger().onNetworkInterfaceSelected(
                Mockito.mock(NetworkInterface::class.java),
                NetworkInterface.NetworkInterfaceTransportType.VIDYO_NETWORKINTERFACETRANSPORTTYPE_Signaling
        )
        verify(logger, times(1)).publish(any())
        reset(logger)

        NetworkInterfaceEventLogger().onNetworkInterfaceStateUpdated(
                Mockito.mock(NetworkInterface::class.java),
                NetworkInterface.NetworkInterfaceState.VIDYO_NETWORKINTERFACESTATE_Unknown
        )
        verify(logger, times(1)).publish(any())
        reset(logger)

        Logger.getLogger("vidyo").removeHandler(logger)
    }
}

class `EventLogger tests` {

    @Test
    fun shakedown() {
        val eventLogger = EventLogger()
        eventLogger.onLog(LogRecord())
        eventLogger.onLog(LogRecord().apply {  level = LogRecord.LogLevel.VIDYO_LOGLEVEL_INVALID })
        eventLogger.onLog(null)
    }

    @Test
    fun `test logger`() {
        val logger = mock(Handler::class.java)
        Logger.getLogger("vidyo").apply { level = Level.ALL }.addHandler(logger)

        val eventLogger = EventLogger()
        LogRecord.LogLevel.values().forEach { level ->
            reset(logger)
            eventLogger.onLog(LogRecord().also { it.level = level })
            verify(logger, times(1)).publish(any())
        }

        reset(logger)
        eventLogger.onLog(LogRecord())
        verify(logger, times(1)).publish(any())

        Logger.getLogger("vidyo").removeHandler(logger)
    }
}

class `LocalCameraEventLogger tests` {

    @Test
    fun `test logger`() {
        val logger = mock(Handler::class.java)
        Logger.getLogger("vidyo").apply { level = Level.ALL }.addHandler(logger)

        LocalCameraEventLogger().onLocalCameraAdded(Mockito.mock(LocalCamera::class.java))
        verify(logger, times(1)).publish(any())
        reset(logger)

        LocalCameraEventLogger().onLocalCameraRemoved(Mockito.mock(LocalCamera::class.java))
        verify(logger, times(1)).publish(any())
        reset(logger)

        LocalCameraEventLogger().onLocalCameraSelected(Mockito.mock(LocalCamera::class.java))
        verify(logger, times(1)).publish(any())
        reset(logger)

        LocalCameraEventLogger().onLocalCameraSelected(null)
        verify(logger, times(1)).publish(any())
        reset(logger)

        LocalCameraEventLogger().onLocalCameraStateUpdated(Mockito.mock(LocalCamera::class.java), Device.DeviceState.VIDYO_DEVICESTATE_Added)
        verify(logger, times(1)).publish(any())
        reset(logger)

        Logger.getLogger("vidyo").removeHandler(logger)
    }
}