package com.vidyo.vidyoconnector.model.inject

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.nhaarman.mockito_kotlin.whenever
import com.vidyo.vidyoconnector.mocks.Mockitos.Companion.mockConnectionFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [21])
class `VidyoConnectorController injector tests` {

    @Test
    fun shakedown() {
        DaggerVidyoConnectorControllerMaker.builder()
                .connectorFactory(mockConnectionFactory())
                .build().make()
    }
}


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [21])
class `VidyoViewControllerInjection injector tests` {

    @Test
    fun shakedown() {
        DaggerVidyoViewControllerMaker.builder().build().make()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [26])
class `AndroidAssembly tests` {

    @Test
    fun `test build accessor`() {
        val buildAccessor = AndroidAssembly().providesBuildAccessor()
        assertEquals(26, buildAccessor.sdkVersion())
    }

    @Test
    fun `test permission checker`() {
        val permissionChecker = AndroidAssembly().providesPermissionChecker()

        val context = Mockito.mock(Context::class.java)
        whenever(context.checkPermission(Manifest.permission.CAMERA, android.os.Process.myPid(), Process.myUid())).thenReturn(PackageManager.PERMISSION_DENIED)
        whenever(context.checkPermission(Manifest.permission.CAPTURE_AUDIO_OUTPUT, android.os.Process.myPid(), Process.myUid())).thenReturn(PackageManager.PERMISSION_GRANTED)

        val checkSelfPermission = permissionChecker.checkSelfPermission(context, Manifest.permission.CAMERA)
        assertEquals(PackageManager.PERMISSION_DENIED, checkSelfPermission)
        assertEquals(PackageManager.PERMISSION_GRANTED, permissionChecker.checkSelfPermission(context, Manifest.permission.CAPTURE_AUDIO_OUTPUT))
    }
}