package com.vidyo.vidyoconnector.util

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Observer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class `ObservableExtensions tests` {

    @Test
    fun `coverage for static helpers`() {
        object : ObservableExtensions<Unit>() {
            override fun subscribeActual(observer: Observer<in Unit>?) {
            }
        }.subscribe()
    }

    @Test
    fun `test emitWhen`() {
        assertEquals(ObservableExtensions.emitWhen({ true }, 1).blockingFirst(), 1)
        assertTrue(ObservableExtensions.emitWhen({ false }, 1).isEmpty.blockingGet())
    }
}

class `Kotlin extensions tests` {

    @Test
    fun `test Kotlin extension takeTrue`() {
        var taken = false
        (true).takeTrue {
            taken = true
        }
        assertTrue(taken)

        taken = false
        (false).takeTrue {
            taken = true
        }
        assertFalse(taken)
    }
}

class `LiveData extensions tests` {

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @Test
    fun `test apply`() {
        assertEquals(MutableLiveData<Boolean>().apply(true).value, true)
        assertEquals(MutableLiveData<Boolean>().apply(false).value, false)
        assertEquals(MutableLiveData<String>().apply("string").value, "string")
    }

    @Test
    fun `test flip`() {
        val data = MutableLiveData<Boolean>().apply(true)
        assertEquals(data.value, true)

        data.flip()
        assertEquals(data.value, false)

        data.flip()
        assertEquals(data.value, true)
    }

    @Test
    fun `test flip in uninitialized state`() {
        val data = MutableLiveData<Boolean>()
        assertEquals(data.value, null)

        data.flip()
        assertEquals(data.value, true)

        data.flip()
        assertEquals(data.value, false)
    }
}
