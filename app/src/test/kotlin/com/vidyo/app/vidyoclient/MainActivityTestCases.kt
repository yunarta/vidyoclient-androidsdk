package com.vidyo.app.vidyoclient

import android.Manifest
import android.content.pm.PackageManager
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.app.vidyoclient.MainActivity.Companion.PERMISSIONS_REQUEST_ALL
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoViewController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

        buildActivity.setup()

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