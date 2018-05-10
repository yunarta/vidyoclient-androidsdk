package com.vidyo.vidyoconnector.util

import io.reactivex.Observable

abstract class ObservableExtensions<T> : Observable<T>() {

    companion object
}

fun <T> ObservableExtensions.Companion.emitWhen(closure: () -> Boolean, value: T): Observable<T> = if (closure()) {
    Observable.just(value)
} else {
    Observable.empty()
}
