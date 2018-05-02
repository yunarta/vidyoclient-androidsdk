package com.vidyo.vidyoconnector

import android.app.Application
import com.vidyo.VidyoClient.Connector.ConnectorPkg
import com.vidyo.vidyoconnector.logger.AndroidLogHandler
import java.util.logging.Logger

class VidyoConnectorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.getGlobal().addHandler(AndroidLogHandler())

        // Initialize the VidyoClient library - this should be done once in the lifetime of the application.
        ConnectorPkg.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()

        ConnectorPkg.uninitialize()
    }
}