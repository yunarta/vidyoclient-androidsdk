package com.vidyo.vidyoconnector.model

import android.arch.lifecycle.MutableLiveData
import com.vidyo.VidyoClient.Connector.Connector

interface VidyoMediaController {

    val cameraPrivacy: MutableLiveData<Boolean>
    val microphonePrivacy: MutableLiveData<Boolean>
    fun cycleCamera()
}

internal class VidyoMediaControllerDelegate(delegate: VidyoMediaController) : VidyoMediaController by delegate
internal class MutableVidyoMediaController : VidyoMediaController {

    var connector: Connector? = null

    override val cameraPrivacy = MutableLiveData<Boolean>()
    override val microphonePrivacy = MutableLiveData<Boolean>()

    init {
        cameraPrivacy.observeForever {
            connector?.setCameraPrivacy(it == true)
        }
        microphonePrivacy.observeForever {
            connector?.setMicrophonePrivacy(it == true)
        }
    }

    override fun cycleCamera() {
        connector?.cycleCamera()
    }

    fun apply() {
        connector?.setCameraPrivacy(cameraPrivacy.value == true)
        connector?.setMicrophonePrivacy(microphonePrivacy.value == true)
    }
}