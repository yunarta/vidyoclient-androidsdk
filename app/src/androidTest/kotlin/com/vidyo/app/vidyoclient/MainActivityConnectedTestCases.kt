package com.vidyo.app.vidyoclient

import android.Manifest
import android.os.Build
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityConnectedTestCases {

    @JvmField
    @Rule
    var grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @JvmField
    @Rule
    var activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Before
    fun grantPhonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val automation = getInstrumentation().uiAutomation
            val packageName = getTargetContext().packageName

            automation.executeShellCommand("pm grant $packageName android.permission.CAMERA")
            automation.executeShellCommand("pm grant $packageName android.permission.RECORD_AUDIO")
            automation.executeShellCommand("pm grant $packageName android.permission.WRITE_EXTERNAL_STORAGE")
        }
    }

    @Test
    fun shakedown() {

    }
}