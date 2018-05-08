package com.vidyo.app.vidyoclient.model

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.vidyo.vidyoconnector.model.ConnectionData
import com.vidyo.vidyoconnector.model.ConnectorState
import com.vidyo.vidyoconnector.model.OptionsData
import com.vidyo.vidyoconnector.model.VidyoActivityRouter
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoMediaController
import com.vidyo.vidyoconnector.model.VidyoViewController
import com.vidyo.vidyoconnector.model.ViewPortListener
import io.reactivex.Maybe

class MainActivityViewModelFactoryStub : MainActivityViewModelFactory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        TODO("not implemented")
    }
}

class VidyoViewControllerStub : VidyoViewController {

    override val isShowToolbar: LiveData<Boolean>
        get() = TODO("not implemented")
    override val isShowInput: LiveData<Boolean>
        get() = TODO("not implemented")
    override val isConnecting: LiveData<Boolean>
        get() = TODO("not implemented")
    override val isShowVersion: MutableLiveData<Boolean>
        get() = TODO("not implemented")
    override val connectionStatus: LiveData<String>
        get() = TODO("not implemented")
    override val connectionStatusResource: LiveData<Int>
        get() = TODO("not implemented")
    override val connectButtonState: LiveData<Boolean>
        get() = TODO("not implemented")
    override val allowReconnect: LiveData<Boolean>
        get() = TODO("not implemented")
    override val connectorStateObserver: Observer<ConnectorState>
        get() = TODO("not implemented")
    override var router: VidyoActivityRouter?
        get() = TODO("not implemented")
        set(value) {}

    override fun applyConfiguration(options: OptionsData) {
        TODO("not implemented")
    }

    override fun requirePermissions(context: Context): Maybe<List<String>> {
        TODO("not implemented")
    }

    override fun toggleConnect(closure: (Boolean) -> Unit) {
        TODO("not implemented")
    }

    override fun toggleToolbarVisibility() {
        TODO("not implemented")
    }
}

class VidyoConnectorControllerStub : VidyoConnectorController {

    override val debug: LiveData<Boolean>
        get() = TODO("not implemented")
    override val version: LiveData<String>
        get() = TODO("not implemented")
    override val mediaController: VidyoMediaController
        get() = TODO("not implemented")

    override fun setDebug(enabled: Boolean) {
        TODO("not implemented")
    }

    override fun onCleared() {
        TODO("not implemented")
    }

    override fun connect(connection: ConnectionData) {
        TODO("not implemented")
    }

    override fun disconnect() {
        TODO("not implemented")
    }

    override fun start() {
        TODO("not implemented")
    }

    override fun stop() {
        TODO("not implemented")
    }

    override val connectionState: LiveData<ConnectorState>
        get() = TODO("not implemented")

    override var viewFrame: Any?
        get() = TODO("not implemented")
        set(value) {}

    override fun updateViewPort() {
        TODO("not implemented")
    }

    override var viewPortListener: ViewPortListener?
        get() = TODO("not implemented")
        set(value) {}
}