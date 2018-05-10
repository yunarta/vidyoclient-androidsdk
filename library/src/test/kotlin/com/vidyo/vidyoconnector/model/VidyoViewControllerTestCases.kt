package com.vidyo.vidyoconnector.model

import android.Manifest
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.content.Context
import android.os.Build
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.vidyo.VidyoClient.Connector.Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected
import com.vidyo.VidyoClient.Connector.Connector.ConnectorFailReason.VIDYO_CONNECTORFAILREASON_ConnectionFailed
import com.vidyo.vidyoconnector.R
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockAndroidAssembly
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockBuildAccessor
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockPermissionChecker
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockVidyoViewController
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoViewControllerMaker
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

class `VidyoViewController change state tests` {

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @Test
    fun shakedown() {
        val controller = DaggerVidyoViewControllerMaker.builder()
                .build().make()

        assertNull(controller.router)
    }

    @Test
    fun `test state observer with Connecting`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.connectorStateObserver.onChanged(ConnectorState.Connecting)

        assertEquals(true, controller.isConnecting.value)
        assertEquals(true, controller.connectButtonState.value)
        assertEquals(R.string.connection_state_connecting, controller.connectionStatusResource.value)
    }

    @Test
    fun `test state observer with Connected`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.connectorStateObserver.onChanged(ConnectorState.Connected)

        assertEquals(false, controller.isShowInput.value)
        assertEquals(false, controller.isConnecting.value)
        assertEquals(true, controller.connectButtonState.value)
        assertEquals(R.string.connection_state_connected, controller.connectionStatusResource.value)
    }

    @Test
    fun `test state observer with Disconnecting`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.connectorStateObserver.onChanged(ConnectorState.Disconnecting)

        assertEquals(true, controller.connectButtonState.value)
        assertEquals(R.string.connection_state_disconnecting, controller.connectionStatusResource.value)
    }

    @Test
    fun `test state observer with Disconnected`() {
        testDisconnectedState(
                ConnectorState.Disconnected,
                R.string.connection_state_disconnected,
                OptionsData(allowReconnect = false),
                false
        )

        testDisconnectedState(
                ConnectorState.Disconnected,
                R.string.connection_state_disconnected,
                OptionsData(allowReconnect = true),
                true
        )
    }

    @Test
    fun `test state observer with DisconnectedUnexpected`() {
        val reason = VIDYO_CONNECTORDISCONNECTREASON_Disconnected
        testDisconnectedState(
                ConnectorState.DisconnectedUnexpected(reason),
                0,
                OptionsData(allowReconnect = false),
                true
        )

        testDisconnectedState(
                ConnectorState.DisconnectedUnexpected(reason),
                0,
                OptionsData(allowReconnect = true),
                true
        )

    }

    @Test
    fun `test state observer with Failure`() {
        val reason = VIDYO_CONNECTORFAILREASON_ConnectionFailed
        testDisconnectedState(
                ConnectorState.Failure(reason),
                0,
                OptionsData(allowReconnect = false),
                true
        )

        testDisconnectedState(
                ConnectorState.Failure(reason),
                0,
                OptionsData(allowReconnect = true),
                true
        )

    }

    @Test
    fun `test state observer with FailureInvalidResource`() {
        testDisconnectedState(
                ConnectorState.FailureInvalidResource,
                R.string.connection_state_failure_invalid_resource,
                OptionsData(allowReconnect = false),
                true
        )

        testDisconnectedState(
                ConnectorState.FailureInvalidResource,
                R.string.connection_state_failure_invalid_resource,
                OptionsData(allowReconnect = true),
                true
        )
    }

    private fun testDisconnectedState(state: ConnectorState, resource: Int, optionsData: OptionsData, allowReconnect: Boolean) {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.applyConfiguration(optionsData)
        controller.connectorStateObserver.onChanged(state)

        assertEquals(false, controller.isConnecting.value)
        assertEquals(false, controller.connectButtonState.value)
        assertEquals(allowReconnect, controller.allowReconnect.value)
        assertEquals(resource, controller.connectionStatusResource.value)
    }

    @Test
    fun `test state observer with Failure with hide config enabled`() {
        testInputStateState(ConnectorState.Failure(VIDYO_CONNECTORFAILREASON_ConnectionFailed), true, false)
    }

    @Test
    fun `test state observer with Failure with hide config disabled`() {
        testInputStateState(ConnectorState.Failure(VIDYO_CONNECTORFAILREASON_ConnectionFailed), false, true)
    }

    @Test
    fun `test state observer with Disconnected with hide config enabled`() {
        testInputStateState(ConnectorState.Disconnected, true, false, false, false)
    }

    @Test
    fun `test state observer with Disconnected with config disabled`() {
        testInputStateState(ConnectorState.Disconnected, false, false, false, false)
    }

    @Test
    fun `test state observer with Disconnected with hide config enabled, and allow reconnect`() {
        testInputStateState(ConnectorState.Disconnected, true, false, true, true)
    }

    private fun testInputStateState(state: ConnectorState, hideConfig: Boolean, isInputShown: Boolean, allowReconnect: Boolean = true, isReconnectAllowed: Boolean = true) {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.applyConfiguration(OptionsData(hideConfig = hideConfig, allowReconnect = allowReconnect))
        controller.connectorStateObserver.onChanged(state)

        assertEquals(isInputShown, controller.isShowInput.value)
        assertEquals(isReconnectAllowed, controller.allowReconnect.value)
    }

    @Test
    fun `test state observer with Disconnect with hide config disabled and no options`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.connectorStateObserver.onChanged(ConnectorState.FailureInvalidResource)

        assertEquals(true, controller.isShowInput.value)
        assertEquals(true, controller.allowReconnect.value)
    }

    @Test
    fun `test state observer with null`() {
        val controller: VidyoViewController = mockVidyoViewController()

        val isShowInput = controller.isShowInput.value
        val isConnecting = controller.isConnecting.value
        val connectButtonState = controller.connectButtonState.value
        val allowReconnect = controller.allowReconnect.value
        val connectionStatusResource = controller.connectionStatusResource.value
        val isShowToolbar = controller.isShowToolbar.value
        val isShowVersion = controller.isShowVersion.value

        controller.connectorStateObserver.onChanged(null)
        assertEquals(isShowInput, controller.isShowInput.value)
        assertEquals(isConnecting, controller.isConnecting.value)
        assertEquals(connectButtonState, controller.connectButtonState.value)
        assertEquals(allowReconnect, controller.allowReconnect.value)
        assertEquals(connectionStatusResource, controller.connectionStatusResource.value)
        assertEquals(isShowToolbar, controller.isShowToolbar.value)
        assertEquals(isShowVersion, controller.isShowVersion.value)
    }

    @Test
    fun `test state observer with returnURL after Disconnected`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.applyConfiguration(OptionsData(returnURL = "http://domain/returnUrl"))

        val router = mock(VidyoActivityRouter::class.java)
        controller.router = router

        controller.connectorStateObserver.onChanged(ConnectorState.Disconnected)
        verify(router, times(1)).returnToLaunchingApplication("http://domain/returnUrl", 1)
    }


    @Test
    fun `test state observer with returnURL after Failure`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.applyConfiguration(OptionsData(returnURL = "http://domain/returnUrl"))

        val router = mock(VidyoActivityRouter::class.java)
        controller.router = router

        controller.connectorStateObserver.onChanged(ConnectorState.Failure(VIDYO_CONNECTORFAILREASON_ConnectionFailed))
        verify(router, times(1)).returnToLaunchingApplication("http://domain/returnUrl", 0)
    }

    @Test
    fun `test state observer with returnURL without router`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.applyConfiguration(OptionsData(returnURL = "http://domain/returnUrl"))
        controller.connectorStateObserver.onChanged(ConnectorState.Disconnected)
    }
}

@RunWith(MockitoJUnitRunner.Silent::class)
class `VidyoViewController permission check tests` {

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @Mock
    private
    lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `test when user consent is required`() {
        val controller: VidyoViewController = mockVidyoViewController(
                androidAssembly = mockAndroidAssembly(
                        mockBuildAccessor(Build.VERSION_CODES.M),
                        mockPermissionChecker(context, *permissions)
                )
        )

        assertArrayEquals(permissions, controller.requirePermissions(context).blockingGet().toTypedArray())
    }

    @Test
    fun `test when user consent partially fulfilled`() {
        val rejectedPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val controller: VidyoViewController = mockVidyoViewController(
                androidAssembly = mockAndroidAssembly(
                        mockBuildAccessor(Build.VERSION_CODES.M),
                        mockPermissionChecker(context, *rejectedPermissions)
                )
        )

        assertArrayEquals(rejectedPermissions, controller.requirePermissions(context).blockingGet().toTypedArray())
    }

    @Test
    fun `test when user consent all fulfilled`() {
        val controller: VidyoViewController = mockVidyoViewController(
                androidAssembly = mockAndroidAssembly(
                        mockBuildAccessor(Build.VERSION_CODES.M),
                        mockPermissionChecker(context)
                )
        )

        assertNull(controller.requirePermissions(context).blockingGet())
    }

    @Test
    fun `test when user consent is not required`() {
        val controller: VidyoViewController = mockVidyoViewController(
                androidAssembly = mockAndroidAssembly()
        )

        assertNull(controller.requirePermissions(context).blockingGet())
    }

    companion object {

        private val permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

class `VidyoViewController ui interaction tests` {

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @Test
    fun `test toggle connect`() {
        val controller: VidyoViewController = mockVidyoViewController()
        var initial = controller.connectButtonState.value ?: false
        (0..5).forEach {
            controller.toggleConnect {
                assertEquals(!initial, it)
                initial = it
            }
        }
    }

    @Test
    fun `test toggle visibility with connected state`() {
        val controller: VidyoViewController = mockVidyoViewController()
        controller.connectorStateObserver.onChanged(ConnectorState.Connected)

        (0..5).forEach {
            val initial = controller.isShowToolbar.value ?: false
            controller.toggleToolbarVisibility()
            assertEquals(!initial, controller.isShowToolbar.value ?: false)
        }
    }

    @Test
    fun `test toggle visibility with disconnected state`() {
        val controller: VidyoViewController = mockVidyoViewController()
        (0..5).forEach {
            controller.toggleToolbarVisibility()
            assertEquals(true, controller.isShowToolbar.value ?: false)
        }
    }
}