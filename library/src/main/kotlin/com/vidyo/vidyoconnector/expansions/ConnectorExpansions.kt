package com.vidyo.vidyoconnector.expansions

import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.VidyoClient.Device.Device
import com.vidyo.VidyoClient.Device.LocalCamera

internal open class LocalCameraEventAdapter : Connector.IRegisterLocalCameraEventListener {

    override fun onLocalCameraRemoved(p0: LocalCamera?) {
    }

    override fun onLocalCameraStateUpdated(p0: LocalCamera?, p1: Device.DeviceState?) {
    }

    override fun onLocalCameraSelected(p0: LocalCamera?) {
    }

    override fun onLocalCameraAdded(p0: LocalCamera?) {
    }
}

internal class MultiCastLocalCameraEventListener : Connector.IRegisterLocalCameraEventListener {

    private val listeners = mutableSetOf<Connector.IRegisterLocalCameraEventListener>()

    fun add(listener: Connector.IRegisterLocalCameraEventListener) {
        listeners.add(listener)
    }

    fun remove(listener: Connector.IRegisterLocalCameraEventListener) {
        listeners.remove(listener)
    }

    override fun onLocalCameraRemoved(p0: LocalCamera?) {
        listeners.forEach {
            it.onLocalCameraRemoved(p0)
        }
    }

    override fun onLocalCameraStateUpdated(p0: LocalCamera?, p1: Device.DeviceState?) {
        listeners.forEach {
            it.onLocalCameraStateUpdated(p0, p1)
        }
    }

    override fun onLocalCameraSelected(p0: LocalCamera?) {
        listeners.forEach {
            it.onLocalCameraSelected(p0)
        }
    }

    override fun onLocalCameraAdded(p0: LocalCamera?) {
        listeners.forEach {
            it.onLocalCameraAdded(p0)
        }
    }
}