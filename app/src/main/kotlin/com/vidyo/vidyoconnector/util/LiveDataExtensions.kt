package com.vidyo.vidyoconnector.util

import android.arch.lifecycle.MutableLiveData

fun MutableLiveData<Boolean>.flip() {
    val current = this.value ?: false
    postValue(!current)
}