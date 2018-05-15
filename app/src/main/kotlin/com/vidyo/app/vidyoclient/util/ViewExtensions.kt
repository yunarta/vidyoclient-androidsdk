package com.vidyo.app.vidyoclient.util

import android.view.View
import android.view.ViewTreeObserver

val View.sizeNotConfigured: Boolean
    get() {
        return this.width == 0 && this.height == 0
    }

fun View.executeOnGlobalLayoutEvent(closure: (View) -> Unit) {
    if (!sizeNotConfigured) {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    closure(this@executeOnGlobalLayoutEvent)
                }
            })
        }
    } else {
        closure(this)
    }
}
