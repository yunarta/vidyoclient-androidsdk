package com.vidyo.vidyoconnector.logger

import android.util.Log
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class AndroidLogHandler : Handler() {

    override fun publish(record: LogRecord?) {
        record?.let {
            when (it.level.intValue()) {
                in Level.FINER.intValue()..Level.FINEST.intValue() ->
                    Log.v(it.loggerName, it.message, it.thrown)

                Level.SEVERE.intValue() ->
                    Log.e(it.loggerName, it.message, it.thrown)

                Level.INFO.intValue(),
                Level.CONFIG.intValue() ->
                    Log.i(it.loggerName, it.message, it.thrown)

                Level.WARNING.intValue() ->
                    Log.w(it.loggerName, it.message, it.thrown)

                else ->
                    Log.d(it.loggerName, it.message, it.thrown)
            }
        }
    }

    override fun flush() {
    }

    override fun close() {
    }
}