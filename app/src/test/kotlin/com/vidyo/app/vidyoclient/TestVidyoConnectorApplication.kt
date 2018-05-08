package com.vidyo.app.vidyoclient

import android.arch.lifecycle.ViewModel
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.app.vidyoclient.model.MainActivityViewModel
import com.vidyo.app.vidyoclient.model.MainActivityViewModelFactory
import com.vidyo.vidyoconnector.expansions.ConnectorFactory
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoViewController
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoConnectorControllerMaker
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoViewControllerMaker
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import org.mockito.Mockito.mock
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

@Module
class TestMainActivityModule {

    @Provides
    fun providerViewModelFactory(viewController: VidyoViewController, connectorController: VidyoConnectorController): MainActivityViewModelFactory {
        return object : MainActivityViewModelFactory {

            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainActivityViewModel(
                        viewController,
                        connectorController
                ) as T
            }
        }
    }
}

@Component(modules = [(AndroidSupportInjectionModule::class), (AppModule::class), (TestMainActivityModule::class)])
internal interface TestComponent : AndroidInjector<TestVidyoConnectorApplication> {

    @dagger.Component.Builder
    abstract class Builder : AndroidInjector.Builder<TestVidyoConnectorApplication>() {

        @BindsInstance
        abstract fun vidyoViewController(controller: VidyoViewController): Builder

        @BindsInstance
        abstract fun vidyoConnectorController(controller: VidyoConnectorController): Builder
    }
}

open class MockVidyoViewController(delegate: VidyoViewController) : VidyoViewController by delegate
open class MockVidyoConnectorController(delegate: VidyoConnectorController) : VidyoConnectorController by delegate

class TestVidyoConnectorApplication : DaggerApplication() {

    lateinit var connector: Connector

    lateinit var viewController: VidyoViewController
    lateinit var connectorController: VidyoConnectorController

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        connector = mock(Connector::class.java)

        viewController = MockVidyoViewController(DaggerVidyoViewControllerMaker.builder().build().make())
        connectorController = MockVidyoConnectorController(DaggerVidyoConnectorControllerMaker.builder()
                .connectorFactory(mock(ConnectorFactory::class.java).apply {
                    whenever(create(any())).thenReturn(connector)
                })
                .build().make())
        return DaggerTestComponent.builder()
                .vidyoViewController(viewController)
                .vidyoConnectorController(connectorController)
                .create(this)
    }

    override fun onCreate() {
        super.onCreate()
        Logger.getLogger("vidyo").apply {
            level = Level.ALL
        }.addHandler(ConsoleHandler())
    }
}
