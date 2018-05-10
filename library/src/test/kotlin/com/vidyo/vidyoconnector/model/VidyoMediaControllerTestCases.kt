package com.vidyo.vidyoconnector.model

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.vidyo.VidyoClient.Connector.Connector
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class `VidyoMediaController tests` {

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @Test
    fun `test apply`() {
        fun testWithPrivacy(privacy: Boolean) {
            val controller = MutableVidyoMediaController()
            val delegate = VidyoMediaControllerDelegate(controller)

            delegate.cameraPrivacy.postValue(privacy)
            delegate.microphonePrivacy.postValue(privacy)

            // ensure no issue when un-initialized
            controller.apply()

            val connector = mock(Connector::class.java)
            controller.connector = connector

            controller.apply()
            verify(connector, times(1)).setCameraPrivacy(privacy)
            verify(connector, times(1)).setMicrophonePrivacy(privacy)
        }

        testWithPrivacy(true)
        testWithPrivacy(false)
    }

    @Test
    fun `test observation`() {
        fun testWithPrivacy(privacy: Boolean) {
            val controller = MutableVidyoMediaController()
            val delegate = VidyoMediaControllerDelegate(controller)

            val connector = mock(Connector::class.java)
            controller.connector = connector

            delegate.cameraPrivacy.postValue(privacy)
            delegate.microphonePrivacy.postValue(privacy)
            verify(connector, times(1)).setCameraPrivacy(privacy)
            verify(connector, times(1)).setMicrophonePrivacy(privacy)
        }

        testWithPrivacy(true)
        testWithPrivacy(false)
    }

    @Test
    fun `test cycle camera`() {
        val controller = MutableVidyoMediaController()
        val delegate = VidyoMediaControllerDelegate(controller)

        // ensure no issue when un-initialized
        delegate.cycleCamera()

        val connector = mock(Connector::class.java)
        controller.connector = connector

        delegate.cycleCamera()
        verify(connector, times(1)).cycleCamera()

        delegate.cycleCamera()
        verify(connector, times(2)).cycleCamera()
    }
}