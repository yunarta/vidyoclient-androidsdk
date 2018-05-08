package com.vidyo.app.vidyoclient

import com.vidyo.VidyoClient.Connector.ConnectorPkg
import com.vidyo.app.vidyoclient.logger.AndroidLogHandler
import com.vidyo.app.vidyoclient.model.MainActivityViewModelFactory
import com.vidyo.app.vidyoclient.model.MainActivityViewModelFactoryImpl
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import java.util.logging.Level
import java.util.logging.Logger


@Module
class MainActivityModule {

    @Provides
    fun providerViewModelFactory(): MainActivityViewModelFactory {
        return MainActivityViewModelFactoryImpl()
    }
}

@Module
abstract class AppModule {

    @ContributesAndroidInjector
    abstract fun mainActivity(): MainActivity
}

@Component(modules = [(AndroidSupportInjectionModule::class), (AppModule::class), (MainActivityModule::class)])
internal interface MainComponent : AndroidInjector<VidyoConnectorApplication> {

    @dagger.Component.Builder
    abstract class Builder : AndroidInjector.Builder<VidyoConnectorApplication>()
}

class VidyoConnectorApplication : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerMainComponent.builder().create(this)
    }

    override fun onCreate() {
        super.onCreate()
        Logger.getLogger("vidyo").apply {
            level = Level.FINE
        }.addHandler(AndroidLogHandler())

        // Initialize the VidyoClient library - this should be done once in the lifetime of the application.
        ConnectorPkg.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()

        ConnectorPkg.uninitialize()
    }
}