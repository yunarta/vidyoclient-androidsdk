package com.vidyo.vidyoconnector.model.inject

import android.os.Build
import android.support.v4.content.ContextCompat
import com.vidyo.vidyoconnector.expansions.BuildAccessor
import com.vidyo.vidyoconnector.expansions.InjectablePermissionChecker
import com.vidyo.vidyoconnector.model.MutableVidyoViewController
import com.vidyo.vidyoconnector.model.VidyoViewController
import com.vidyo.vidyoconnector.model.VidyoViewControllerDelegate
import dagger.Component
import dagger.Module
import dagger.Provides

@Module
open class AndroidAssembly {

    @Provides
    open fun providesBuildAccessor() = BuildAccessor { Build.VERSION.SDK_INT }

    @Provides
    open fun providesPermissionChecker() = InjectablePermissionChecker { context, permission ->
        ContextCompat.checkSelfPermission(context, permission)
    }
}

@Module
class VidyoViewControllerModule {

    @Provides
    internal fun providesController(delegate: MutableVidyoViewController): VidyoViewController {
        return VidyoViewControllerDelegate(delegate)
    }
}

@Component(modules = [(VidyoViewControllerModule::class), (AndroidAssembly::class)])
interface VidyoViewControllerMaker {

    fun make(): VidyoViewController

    @Component.Builder
    interface Builder {

        fun androidAssembly(assembly: AndroidAssembly): Builder

        fun build(): VidyoViewControllerMaker
    }
}