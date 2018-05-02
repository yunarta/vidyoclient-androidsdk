package com.vidyo.vidyoconnector.util

import android.arch.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.apply(state: T): MutableLiveData<T> {
    this.value = state
    return this
}
