package com.vidyo.vidyoconnector.mocks

import android.content.Context
import android.content.pm.PackageManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.vidyoconnector.expansions.BuildAccessor
import com.vidyo.vidyoconnector.expansions.ConnectorFactory
import com.vidyo.vidyoconnector.expansions.InjectablePermissionChecker
import com.vidyo.vidyoconnector.model.MutableVidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoConnectorControllerDelegate
import com.vidyo.vidyoconnector.model.ViewPort
import com.vidyo.vidyoconnector.model.ViewPortListener
import com.vidyo.vidyoconnector.model.inject.AndroidAssembly
import com.vidyo.vidyoconnector.model.inject.DaggerVidyoViewControllerMaker
import org.mockito.Mockito.mock

class Mockitos {

    companion object {

        fun mockVidyoConnectorController(connector: Connector = mock(Connector::class.java)): VidyoConnectorController =
                VidyoConnectorControllerDelegate(MutableVidyoConnectorController(mockConnectionFactory(connector))).apply {
                    viewPortListener = mockViewPortListener()
                }

        fun mockVidyoViewController(androidAssembly: AndroidAssembly = mockAndroidAssembly()) =
                DaggerVidyoViewControllerMaker.builder()
                        .androidAssembly(androidAssembly)
                        .build()
                        .make()

        fun mockBuildAccessor(buildVersion: Int = 0): BuildAccessor =
                mock(BuildAccessor::class.java).apply {
                    whenever(sdkVersion()).thenReturn(buildVersion)
                }

        fun mockPermissionChecker(context: Context, vararg permissions: String): InjectablePermissionChecker =
                mock(InjectablePermissionChecker::class.java).apply {
                    permissions.forEach {
                        whenever(checkSelfPermission(context, it)).thenReturn(PackageManager.PERMISSION_DENIED)
                    }
                }

        fun mockAndroidAssembly(buildAccessor: BuildAccessor = mockBuildAccessor(),
                                mockPermissionChecker: InjectablePermissionChecker = mockPermissionChecker(
                                        mock(Context::class.java)
                                )): AndroidAssembly =
                mock(AndroidAssembly::class.java).apply {
                    whenever(providesBuildAccessor()).thenReturn(buildAccessor)
                    whenever(providesPermissionChecker()).thenReturn(mockPermissionChecker)
                }

        fun mockViewPortListener(): ViewPortListener =
                mock(ViewPortListener::class.java).apply {
                    whenever(this.rect).thenReturn(ViewPort(0, 0, 100, 100))
                }

        fun mockConnectionFactory(connector: Connector = mock(Connector::class.java)): ConnectorFactory =
                mock(ConnectorFactory::class.java).apply {
                    whenever(create(any())).thenReturn(connector)
                }
    }
}