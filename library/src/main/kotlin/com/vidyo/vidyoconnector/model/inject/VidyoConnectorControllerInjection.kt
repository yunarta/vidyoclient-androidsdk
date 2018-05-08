package com.vidyo.vidyoconnector.model.inject

import com.vidyo.vidyoconnector.expansions.ConnectorFactory
import com.vidyo.vidyoconnector.model.MutableVidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoConnectorControllerDelegate
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides

@Module
class VidyoConnectorControllerModule {

    @Provides
    internal fun providesController(delegate: MutableVidyoConnectorController): VidyoConnectorController {
        return VidyoConnectorControllerDelegate(delegate)
    }
}

@Component(modules = [(VidyoConnectorControllerModule::class)])
interface VidyoConnectorControllerMaker {

    fun make(): VidyoConnectorController

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun connectorFactory(factory: ConnectorFactory): Builder

        fun build(): VidyoConnectorControllerMaker
    }
}