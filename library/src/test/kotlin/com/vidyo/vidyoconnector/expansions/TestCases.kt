package com.vidyo.vidyoconnector.expansions

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.VidyoClient.Device.Device
import com.vidyo.VidyoClient.Device.LocalCamera
import org.junit.Test
import org.mockito.Mockito.mock

class `MultiCastLocalCameraEventListener tests` {

    @Test
    fun `test sending events`() {
        val listeners = arrayOf(
                mock(Connector.IRegisterLocalCameraEventListener::class.java),
                mock(Connector.IRegisterLocalCameraEventListener::class.java),
                mock(Connector.IRegisterLocalCameraEventListener::class.java)
        )

        val multiCast = MultiCastLocalCameraEventListener()
        listeners.forEach {
            multiCast.add(it)
        }

        val camera = mock(LocalCamera::class.java)

        multiCast.onLocalCameraAdded(camera)
        listeners.forEach {
            verify(it, times(1)).onLocalCameraAdded(camera)
        }

        multiCast.onLocalCameraRemoved(camera)
        listeners.forEach {
            verify(it, times(1)).onLocalCameraRemoved(camera)
        }

        multiCast.onLocalCameraSelected(camera)
        listeners.forEach {
            verify(it, times(1)).onLocalCameraSelected(camera)
        }

        multiCast.onLocalCameraStateUpdated(camera, Device.DeviceState.VIDYO_DEVICESTATE_Added)
        listeners.forEach {
            verify(it, times(1)).onLocalCameraStateUpdated(camera, Device.DeviceState.VIDYO_DEVICESTATE_Added)
        }
    }

    @Test
    fun `test listener removal`() {
        val target = mock(Connector.IRegisterLocalCameraEventListener::class.java)

        val multiCast = MultiCastLocalCameraEventListener()
        multiCast.add(mock(Connector.IRegisterLocalCameraEventListener::class.java))
        multiCast.add(target)
        multiCast.add(mock(Connector.IRegisterLocalCameraEventListener::class.java))

        val camera = mock(LocalCamera::class.java)

        multiCast.onLocalCameraAdded(camera)
        multiCast.onLocalCameraRemoved(camera)
        multiCast.onLocalCameraSelected(camera)
        multiCast.onLocalCameraStateUpdated(camera, Device.DeviceState.VIDYO_DEVICESTATE_Added)

        verify(target, times(1)).onLocalCameraAdded(camera)
        verify(target, times(1)).onLocalCameraRemoved(camera)
        verify(target, times(1)).onLocalCameraSelected(camera)
        verify(target, times(1)).onLocalCameraStateUpdated(camera, Device.DeviceState.VIDYO_DEVICESTATE_Added)

        multiCast.remove(target)

        multiCast.onLocalCameraAdded(null)
        multiCast.onLocalCameraRemoved(null)
        multiCast.onLocalCameraSelected(null)
        multiCast.onLocalCameraStateUpdated(null, null)

        verifyZeroInteractions(target)
    }
}

class `LocalCameraEventAdapter tests` {

    @Test
    fun shakedown() {
        val camera = mock(LocalCamera::class.java)
        LocalCameraEventAdapter().let {
            it.onLocalCameraRemoved(camera)
            it.onLocalCameraSelected(camera)
            it.onLocalCameraAdded(camera)
            it.onLocalCameraStateUpdated(camera, Device.DeviceState.VIDYO_DEVICESTATE_Added)
        }
    }
}