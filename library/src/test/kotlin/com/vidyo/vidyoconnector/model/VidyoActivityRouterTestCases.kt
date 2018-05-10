package com.vidyo.vidyoconnector.model

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.mockito.Mockito.mock

class `VidyoActivityRouter tests` {

    @Test
    fun `test routing`() {
        val intent = mock(Intent::class.java)

        val activity = mock(Activity::class.java)
        val packageManager = mock(PackageManager::class.java)

        whenever(activity.packageManager).thenReturn(packageManager)
        whenever(packageManager.getLaunchIntentForPackage(any())).thenReturn(intent)

        val router = VidyoActivityRouterDelegate(VidyoActivityRouterImpl(activity))
        router.returnToLaunchingApplication("http://localhost/item", 0)

        verify(activity).startActivity(intent)
    }

    @Test
    fun `test routing - alternate`() {
        val activity = mock(Activity::class.java)
        val packageManager = mock(PackageManager::class.java)

        whenever(activity.packageManager).thenReturn(packageManager)
        whenever(packageManager.getLaunchIntentForPackage(any())).thenReturn(null)

        val router = VidyoActivityRouterDelegate(VidyoActivityRouterImpl(activity))
        router.returnToLaunchingApplication("http://localhost/item", 0)

        verify(activity, never()).startActivity(any())
    }
}