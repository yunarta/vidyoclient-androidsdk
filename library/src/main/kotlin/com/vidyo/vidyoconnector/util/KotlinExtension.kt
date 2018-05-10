package com.vidyo.vidyoconnector.util

fun Boolean.takeTrue(closure: () -> Unit) {
    if (this) {
        closure()
    }
}