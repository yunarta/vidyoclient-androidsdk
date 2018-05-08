package com.vidyo.vidyoconnector.model

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.widget.FrameLayout
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.VidyoClient.Device.LocalCamera
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockConnectionFactory
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockVidyoConnectorController
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockViewPortListener
import com.vidyo.vidyoconnector.model.MutableVidyoConnectorController.Companion.DEBUG_PORT
import com.vidyo.vidyoconnector.model.MutableVidyoConnectorController.Companion.MAX_REMOTE_PARTICIPANTS
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoConnectorControllerMaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.verification.VerificationMode
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

abstract class ConnectorStateObserver : Observer<ConnectorState>

class `VidyoConnectorController tests` {

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @Test
    fun shakedown() {
        val controller = DaggerVidyoConnectorControllerMaker.builder()
                .connectorFactory(mockConnectionFactory())
                .build().make()

        assertFalse(controller.isRunning)
        assertNull(controller.viewFrame)
        assertNull(controller.viewPortListener)
        assertNull(controller.mediaController.cameraPrivacy.value)
        assertNull(controller.mediaController.microphonePrivacy.value)
        controller.updateViewPort()
        controller.onCleared()
    }

    @Test
    fun `test viewport in uninitialized state`() {
        val connector = mock(Connector::class.java)
        val controller = MutableVidyoConnectorController(mockConnectionFactory(connector))

        // ensure that code may run smoothly in uninitialized state
        controller.updateViewPort()
        controller.updateViewPortInternally(connector)
    }

    @Test
    fun `test viewport in initialized state`() {
        val connector = mock(Connector::class.java)
        val controller = MutableVidyoConnectorController(mockConnectionFactory(connector))

        controller.viewPortListener = mockViewPortListener()
        controller.updateViewPortInternally(connector)

        controller.viewFrame = mock(FrameLayout::class.java)
        verify(connector, times(1)).showViewAt(controller.viewFrame, 0, 0, 100, 100)
        controller.updateViewPort()
    }

    @Test
    fun `test local camera holder`() {
        val camera = mock(LocalCamera::class.java)
        val connector = mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IRegisterLocalCameraEventListener::class.java)
            whenever(registerLocalCameraEventListener(any())).then {
                verify(this).registerLocalCameraEventListener(connectListenerCaptor.capture())
                connectListenerCaptor.value.onLocalCameraSelected(null)
                connectListenerCaptor.value.onLocalCameraSelected(camera)
                true
            }
        }

        val controller = MutableVidyoConnectorController(mockConnectionFactory(connector))
        controller.viewPortListener = mockViewPortListener()
        controller.viewFrame = mock(FrameLayout::class.java)
        assertEquals(controller.localCamera.value, camera)
    }

    @Test
    fun `test sdk version`() {
        val connector = mock(Connector::class.java).apply {
            whenever(version).thenReturn("0.0.0")
        }

        val controller = mockVidyoConnectorController(connector)
        controller.viewPortListener = mockViewPortListener()
        controller.viewFrame = mock(FrameLayout::class.java)
        assertEquals("VidyoClient-AndroidSDK 0.0.0", controller.version.value)
    }

    @Test
    fun `test debug`() {
        val connector = mock(Connector::class.java)
        val controller = mockVidyoConnectorController(connector)

        controller.setDebug(true)
        assertEquals(true, controller.debug.value)
        verify(connector, times(0)).enableDebug(DEBUG_PORT, "warning info@VidyoClient info@VidyoConnector")

        reset(connector)
        controller.viewFrame = mock(FrameLayout::class.java)
        verify(connector, times(1)).enableDebug(DEBUG_PORT, "warning info@VidyoClient info@VidyoConnector")

        reset(connector)
        controller.toggleDebug()
        assertEquals(false, controller.debug.value)
        verify(connector, times(1)).disableDebug()

        reset(connector)
        controller.toggleDebug()
        assertEquals(true, controller.debug.value)
        verify(connector, times(1)).enableDebug(DEBUG_PORT, "warning info@VidyoClient info@VidyoConnector")
    }

    @Test
    fun `test viewport reassign new frame`() {
        val connector = mock(Connector::class.java)
        val controller = MutableVidyoConnectorController(mockConnectionFactory(connector))

        controller.viewPortListener = mockViewPortListener()
        controller.updateViewPortInternally(connector)

        controller.viewFrame = mock(FrameLayout::class.java)
        verify(connector, times(1)).showViewAt(controller.viewFrame, 0, 0, 100, 100)

        val newFrame = mock(FrameLayout::class.java)
        controller.viewFrame = newFrame

        verify(connector, times(1)).assignViewToCompositeRenderer(
                newFrame,
                Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                MAX_REMOTE_PARTICIPANTS
        )
    }

    @Test
    fun `test viewport reassign null frame`() {
        val connector = mock(Connector::class.java)
        val controller = MutableVidyoConnectorController(mockConnectionFactory(connector))

        controller.viewPortListener = mockViewPortListener()
        controller.updateViewPortInternally(connector)

        controller.viewFrame = mock(FrameLayout::class.java)
        verify(connector, times(1)).showViewAt(controller.viewFrame, 0, 0, 100, 100)

        controller.viewFrame = null
    }

    @Test
    fun `test connect with bad resource id`() {
        val controller = mockVidyoConnectorController()

        val observer = mock(ConnectorStateObserver::class.java)

        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java) as Any

        controller.connect(ConnectionData(resourceId = " @ "))
        verify(observer, times(1)).onChanged(ConnectorState.FailureInvalidResource)
    }

    @Test
    fun `test connect with proper resource id in uninitialized state`() {
        val controller = mockVidyoConnectorController()
        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)

        controller.connect(ConnectionData(resourceId = "demoRoom"))
        verify(observer, times(0)).onChanged(ConnectorState.Connecting())
        assertFalse(controller.isRunning)
    }

    @Test
    fun `test connect with proper resource id`() {
        val connector = mock(Connector::class.java).apply {
            whenever(connect(any(), any(), any(), any(), any())).then {
                val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IConnect::class.java)
                verify(this).connect(any(), any(), any(), any(), connectListenerCaptor.capture())

                connectListenerCaptor.value.onSuccess()
                true
            }
        }
        val controller = mockVidyoConnectorController(connector)

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java) as Any

        whenever(observer.onChanged(ConnectorState.Connecting())).then {
            assertTrue(controller.isRunning)
            true
        }

        controller.connect(ConnectionData(resourceId = "demoRoom"))
        verify(observer, times(1)).onChanged(ConnectorState.Connecting())
        verify(observer, times(1)).onChanged(ConnectorState.Connected())

        assertTrue(controller.isRunning)
    }

    @Test
    fun `test connect with proper resource id and then connection refused`() {
        val controller = mockVidyoConnectorController(mock(Connector::class.java).apply {
            whenever(connect(any(), any(), any(), any(), any())).then {
                val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IConnect::class.java)
                verify(this).connect(any(), any(), any(), any(), connectListenerCaptor.capture())

                connectListenerCaptor.value.onFailure(Connector.ConnectorFailReason.VIDYO_CONNECTORFAILREASON_ConnectionFailed)
                false
            }
        })

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java) as Any

        controller.connect(ConnectionData(resourceId = "demoRoom"))
        verify(observer, times(1)).onChanged(ConnectorState.Failure())
    }

    @Test
    fun `test connect with proper resource id and then random disconnect`() {
        val connector = mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IConnect::class.java)
            whenever(connect(any(), any(), any(), any(), any())).then {
                verify(this).connect(any(), any(), any(), any(), connectListenerCaptor.capture())
                connectListenerCaptor.value.onSuccess()
                true
            }

            whenever(onDisconnected(any())).then {
                val disconnectReasonCaptor = ArgumentCaptor.forClass(Connector.ConnectorDisconnectReason::class.java)
                verify(this).onDisconnected(disconnectReasonCaptor.capture())
                print(disconnectReasonCaptor.value)
                connectListenerCaptor.value.onDisconnected(disconnectReasonCaptor.value)
            }
        }

        val controller = mockVidyoConnectorController(connector)

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java) as Any

        controller.connect(ConnectionData(resourceId = "demoRoom"))
        verify(observer, times(1)).onChanged(ConnectorState.Connecting())
        verify(observer, times(1)).onChanged(ConnectorState.Connected())
        assertTrue(controller.isRunning)

        connector.onDisconnected(Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_ConnectionLost)
        verify(observer, times(1)).onChanged(ConnectorState.DisconnectedUnexpected(Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_ConnectionLost))
        assertFalse(controller.isRunning)
    }

    @Test
    fun `test disconnect`() {
        val controller = mockVidyoConnectorController(mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IConnect::class.java)

            whenever(connect(any(), any(), any(), any(), any())).then {
                verify(this).connect(any(), any(), any(), any(), connectListenerCaptor.capture())
                connectListenerCaptor.value.onSuccess()
                true
            }
            whenever(disconnect()).then {
                connectListenerCaptor.value.onDisconnected(Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected)
            }
        })

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java) as Any

        controller.connect(ConnectionData(resourceId = "demoRoom"))
        verify(observer, times(1)).onChanged(ConnectorState.Connecting())
        assertTrue(controller.isRunning)

        controller.disconnect()
        verify(observer, times(1)).onChanged(ConnectorState.Disconnecting())
        verify(observer, times(1)).onChanged(ConnectorState.Disconnected())
        assertFalse(controller.isRunning)
    }

    @Test
    fun `test disconnect in uninitialized state`() {
        val controller = mockVidyoConnectorController()

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)

        controller.disconnect()
        verify(observer, times(0)).onChanged(ConnectorState.Disconnecting())
        assertFalse(controller.isRunning)
    }

    @Test
    fun `test start in uninitialized state`() {
        val controller = mockVidyoConnectorController()

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)

        controller.start()
    }

    @Test
    fun `test start in initialized state`() {
        val connector = mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IRegisterLocalCameraEventListener::class.java)
            whenever(registerLocalCameraEventListener(any())).then {
                verify(this).registerLocalCameraEventListener(connectListenerCaptor.capture())
                connectListenerCaptor.value.onLocalCameraSelected(mock(LocalCamera::class.java))
                true
            }
        }

        val controller = mockVidyoConnectorController(connector)

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java)
        controller.start()
    }

    @Test
    fun `test start in initialized state with no camera`() {
        val controller = mockVidyoConnectorController()

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java)
        controller.start()
    }

    @Test
    fun `test start in initialized state with called twice`() {
        val connector = mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IRegisterLocalCameraEventListener::class.java)
            whenever(registerLocalCameraEventListener(any())).then {
                verify(this).registerLocalCameraEventListener(connectListenerCaptor.capture())
                connectListenerCaptor.value.onLocalCameraSelected(mock(LocalCamera::class.java))
                true
            }
        }

        val controller = mockVidyoConnectorController(connector)

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java)
        controller.start()
        controller.start()
    }

    @Test
    fun `test stop in uninitialized state`() {
        val controller = mockVidyoConnectorController()

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.stop()
    }

    @Test
    fun `test stop in initialized state`() {
        val connector = mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IRegisterLocalCameraEventListener::class.java)
            whenever(registerLocalCameraEventListener(any())).then {
                verify(this).registerLocalCameraEventListener(connectListenerCaptor.capture())
                connectListenerCaptor.value.onLocalCameraSelected(mock(LocalCamera::class.java))
                true
            }
        }

        val controller = mockVidyoConnectorController(connector)

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.viewFrame = mock(FrameLayout::class.java)
        controller.start()
        controller.stop()
    }

    @Test
    fun `test stop in initialized state without calling start`() {
        val connector = mock(Connector::class.java).apply {
            val connectListenerCaptor = ArgumentCaptor.forClass(Connector.IRegisterLocalCameraEventListener::class.java)
            whenever(registerLocalCameraEventListener(any())).then {
                verify(this).registerLocalCameraEventListener(connectListenerCaptor.capture())
                connectListenerCaptor.value.onLocalCameraSelected(mock(LocalCamera::class.java))
                true
            }
        }

        val controller = mockVidyoConnectorController(connector)

        val observer = mock(ConnectorStateObserver::class.java)
        controller.connectionState.observeForever(observer)
        controller.stop()
        controller.stop()
    }

    @Test
    fun `test listener with invalid registration`() {
        fun testListener(connector: Connector, mode: VerificationMode) {
            val logger = mock(Handler::class.java)
            Logger.getLogger("vidyo").apply { level = Level.ALL }.addHandler(logger)

            val controller = mockVidyoConnectorController(connector)

            controller.viewFrame = mock(FrameLayout::class.java)
            verify(logger, mode).publish(any())

            Logger.getLogger("vidyo").removeHandler(logger)
        }

        testListener(mock(Connector::class.java).apply {
            whenever(registerLocalCameraEventListener(any())).thenReturn(false)
            whenever(registerNetworkInterfaceEventListener(any())).thenReturn(true)
            whenever(registerLogEventListener(any(), any())).thenReturn(true)
        }, atLeast(1))
        testListener(mock(Connector::class.java).apply {
            whenever(registerLocalCameraEventListener(any())).thenReturn(true)
            whenever(registerNetworkInterfaceEventListener(any())).thenReturn(false)
            whenever(registerLogEventListener(any(), any())).thenReturn(true)
        }, atLeast(1))
        testListener(mock(Connector::class.java).apply {
            whenever(registerLocalCameraEventListener(any())).thenReturn(true)
            whenever(registerNetworkInterfaceEventListener(any())).thenReturn(true)
            whenever(registerLogEventListener(any(), any())).thenReturn(false)
        }, atLeast(1))

        testListener(mock(Connector::class.java).apply {
            whenever(registerLocalCameraEventListener(any())).thenReturn(true)
            whenever(registerNetworkInterfaceEventListener(any())).thenReturn(true)
            whenever(registerLogEventListener(any(), any())).thenReturn(true)
        }, atLeast(0))
    }
}