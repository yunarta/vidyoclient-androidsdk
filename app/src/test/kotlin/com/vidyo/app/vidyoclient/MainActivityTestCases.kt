package com.vidyo.app.vidyoclient

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewTreeObserver
import com.nhaarman.mockito_kotlin.whenever
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.app.vidyoclient.MainActivity.Companion.PERMISSIONS_REQUEST_ALL
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoViewController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication


@RunWith(RobolectricTestRunner::class)
@Config(manifest = "AndroidManifest.xml", sdk = [26], constants = BuildConfig::class, application = TestVidyoConnectorApplication::class)
class `MainActivity tests` : Base() {

    lateinit var connector: Connector
    lateinit var viewController: VidyoViewController
    lateinit var connectorController: VidyoConnectorController

    @Before
    fun setupMockito() {
        val application = RuntimeEnvironment.application as TestVidyoConnectorApplication
        connector = application.connector
        viewController = application.viewController
        connectorController = application.connectorController

        val instance = ShadowApplication.getInstance()
        instance.denyPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @Test
    fun shakedown() {
        val buildActivity = Robolectric.buildActivity(MainActivity::class.java)
        buildActivity.setup()
        buildActivity.pause()
        buildActivity.stop()
        buildActivity.destroy()
    }

    @Test
    fun `test permission granted`() {
        val instance = ShadowApplication.getInstance()
        instance.grantPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val buildActivity = Robolectric.buildActivity(MainActivity::class.java)
        val activity = buildActivity.get()

        buildActivity.setup()
        assertEquals(true, activity.isInitialized)

        buildActivity.pause()
        buildActivity.stop()
        buildActivity.destroy()
    }

    @Test
    fun `test grant permission`() {
        val buildActivity = Robolectric.buildActivity(MainActivity::class.java)
        val activity = buildActivity.get()
        val mockView = mock(View::class.java)
        val mockViewTreeObserver = mock(ViewTreeObserver::class.java)

        val listenerCaptor = ArgumentCaptor.forClass(ViewTreeObserver.OnGlobalLayoutListener::class.java)
        whenever(mockViewTreeObserver.isAlive).thenReturn(true)
        whenever(mockViewTreeObserver.addOnGlobalLayoutListener(ArgumentMatchers.any())).then {
            verify(mockViewTreeObserver).addOnGlobalLayoutListener(listenerCaptor.capture())
            listenerCaptor.value.onGlobalLayout()
        }

        whenever(mockView.width).thenReturn(100)
        whenever(mockView.height).thenReturn(100)
        whenever(mockView.viewTreeObserver).thenReturn(mockViewTreeObserver)

        buildActivity.create()
        activity.videoFrame = mockView
        buildActivity.start().postCreate(null).resume().visible()

        Shadows.shadowOf(activity).grantPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        buildActivity.get().onRequestPermissionsResult(PERMISSIONS_REQUEST_ALL, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED))

        println("assertTrue")
        assertEquals(true, activity.isInitialized)

        buildActivity.pause()
        buildActivity.stop()
        buildActivity.destroy()
    }

    @Test
    fun `test unexpected permission requested`() {
        val buildActivity = Robolectric.buildActivity(MainActivity::class.java)
        val activity = buildActivity.get()

        buildActivity.setup()
        buildActivity.get().onRequestPermissionsResult(-1, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), intArrayOf(
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED
        ))

        assertFalse(activity.isInitialized)

        buildActivity.pause()
        buildActivity.stop()
        buildActivity.destroy()
    }
}