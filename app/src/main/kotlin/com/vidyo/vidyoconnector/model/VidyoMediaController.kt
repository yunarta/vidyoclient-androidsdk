package com.vidyo.vidyoconnector.model

import android.arch.lifecycle.MutableLiveData
import com.vidyo.VidyoClient.Connector.Connector

interface VidyoMediaController {

    var cameraPrivacy: MutableLiveData<Boolean>
    var microphonePrivacy: MutableLiveData<Boolean>
    fun cycleCamera()
}

class VidyoMediaControllerDelegate(delegate: VidyoMediaController) : VidyoMediaController by delegate
class MutableVidyoMediaController : VidyoMediaController {

    var connector: Connector? = null

    override var cameraPrivacy = MutableLiveData<Boolean>()
    override var microphonePrivacy = MutableLiveData<Boolean>()

    init {
        cameraPrivacy.observeForever {
            connector?.setCameraPrivacy(it ?: false)
        }
        microphonePrivacy.observeForever {
            connector?.setMicrophonePrivacy(it ?: false)
        }
    }

    override fun cycleCamera() {
        connector?.cycleCamera()
    }

    fun apply() {
        connector?.setCameraPrivacy(cameraPrivacy.value ?: false)
        connector?.setMicrophonePrivacy(microphonePrivacy.value ?: false)
    }
}