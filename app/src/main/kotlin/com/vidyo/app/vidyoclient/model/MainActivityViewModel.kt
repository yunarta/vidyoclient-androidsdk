package com.vidyo.app.vidyoclient.model

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.vidyoconnector.expansions.ConnectorFactory
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoViewController
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoConnectorControllerMaker
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoViewControllerMaker

class MainActivityViewModel(val view: VidyoViewController, val connector: VidyoConnectorController) : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        connector.onCleared()
    }
}

interface MainActivityViewModelFactory : ViewModelProvider.Factory

class MainActivityViewModelFactoryImpl : MainActivityViewModelFactory {

    companion object {
        const val MAX_PARTICIPANTS = 15
    }

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainActivityViewModel(
                DaggerVidyoViewControllerMaker.builder().build().make(),
                DaggerVidyoConnectorControllerMaker.builder()
                        .connectorFactory(ConnectorFactory {

                            Connector(
                                    it,
                                    Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                                    MAX_PARTICIPANTS,
                                    "info@VidyoClient info@VidyoConnector warning",
                                    "",
                                    0
                            )
                        })
                        .build().make()
        ) as T
    }
}