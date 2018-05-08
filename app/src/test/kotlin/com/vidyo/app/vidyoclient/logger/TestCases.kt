package com.vidyo.app.vidyoclient.logger

import android.util.Log
import com.vidyo.app.vidyoclient.Base
import org.junit.Test
import org.powermock.core.classloader.annotations.PrepareForTest
import java.util.logging.Level
import java.util.logging.Logger

class `AndroidLogHandler - invocation safety` : Base() {

    @Test
    @PrepareForTest(Log::class)
    fun `ensure invocation is safe`() {
        val levels = arrayOf(Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST)
        levels.forEach {
            Logger.getLogger("vidyo").log(it, "test")
        }

        val handler = AndroidLogHandler()

        Logger.getLogger("vidyo").apply {
            level = Level.ALL
        }.addHandler(handler)

        levels.forEach {
            Logger.getLogger("vidyo").log(it, "test")
        }

        handler.flush()
        handler.close()
    }
}
