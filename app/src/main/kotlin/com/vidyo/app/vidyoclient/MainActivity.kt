package com.vidyo.app.vidyoclient

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.res.Configuration
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.View
import com.vidyo.VidyoClient.Connector.ConnectorPkg
import com.vidyo.app.vidyoclient.databinding.ActivityMainBinding
import com.vidyo.app.vidyoclient.model.MainActivityViewModel
import com.vidyo.app.vidyoclient.model.MainActivityViewModelFactory
import com.vidyo.app.vidyoclient.model.MainActivityViewModelFactoryStub
import com.vidyo.app.vidyoclient.model.VidyoConnectorControllerStub
import com.vidyo.app.vidyoclient.model.VidyoViewControllerStub
import com.vidyo.app.vidyoclient.util.executeOnGlobalLayoutEvent
import com.vidyo.vidyoconnector.model.ConnectionData
import com.vidyo.vidyoconnector.model.OptionsData
import com.vidyo.vidyoconnector.model.VidyoActivityRouterDelegate
import com.vidyo.vidyoconnector.model.VidyoActivityRouterImpl
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoViewController
import com.vidyo.vidyoconnector.model.ViewPort
import com.vidyo.vidyoconnector.model.ViewPortListener
import com.vidyo.vidyoconnector.model.create
import com.vidyo.vidyoconnector.model.isRunning
import com.vidyo.vidyoconnector.model.toggleDebug
import com.vidyo.vidyoconnector.util.ObservableExtensions
import com.vidyo.vidyoconnector.util.emitWhen
import com.vidyo.vidyoconnector.util.flip
import com.vidyo.vidyoconnector.util.takeTrue
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.logging.Logger
import javax.inject.Inject

open class MainActivity : DaggerAppCompatActivity(), View.OnClickListener {

    private val logger = Logger.getLogger("vidyo.main")
    private var mRefreshSettings = false

    private var contentView: ActivityMainBinding? = null
    private var videoFrame: View? = null

    private var connectorController: VidyoConnectorController = VidyoConnectorControllerStub()
    private var viewController: VidyoViewController = VidyoViewControllerStub()

    private val permissionGrantNotifier: PublishSubject<Array<String>> by lazy {
        PublishSubject.create<Array<String>>()
    }

    private var isResumed = false
    private val onResumeNotifier: PublishSubject<Boolean> by lazy {
        PublishSubject.create<Boolean>()
    }

    private var configure = PublishSubject.create<Pair<Intent, Boolean>>()
    private lateinit var bootstrap: Disposable

    private var mutableIsInitialized: Boolean = false
    val isInitialized: Boolean
        get() = mutableIsInitialized

    @JvmField
    @Inject
    var factory: MainActivityViewModelFactory = MainActivityViewModelFactoryStub()

    /*
     *  Operating System Events
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        logger.finest("onCreate")
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this, factory).get(MainActivityViewModel::class.java)
        val router = VidyoActivityRouterDelegate(VidyoActivityRouterImpl(this))

        viewController = viewModel.view
        viewController.router = router

        connectorController = viewModel.connector
        connectorController.connectionState.observe(this, viewController.connectorStateObserver)
        connectorController.viewPortListener = object : ViewPortListener {
            override val rect: ViewPort
                get() = videoFrame?.run {
                    return ViewPort(0, 0, width, height)
                } ?: ViewPort()
        }
        connectorController.debug.observe(this, Observer {
            viewModel.view.isShowVersion.value = it
        })

        // Set the application's UI context to this activity.
        ConnectorPkg.setApplicationUIContext(this)
        bootstrap = bootstrap().subscribe()

        bindContentView()

        mRefreshSettings = savedInstanceState == null
    }

    private fun bindContentView() {
        val activity = this@MainActivity
        val contentView: ActivityMainBinding = DataBindingUtil.setContentView(activity, R.layout.activity_main)
        contentView.run {
            connector = connectorController
            view = viewController

            setLifecycleOwner(activity)

            // Initialize the member variables
            videoFrame.setOnClickListener(activity)
            microphonePrivacy.setOnClickListener(activity)
            cameraPrivacy.setOnClickListener(activity)
            cameraSwitch.setOnClickListener(activity)
            connectButton.setOnClickListener(activity)
            toggleDebug.setOnClickListener(activity)
        }

        this.contentView = contentView
        videoFrame = contentView.videoFrame
    }

    /**
     * For new intent that changes the connection configuration
     */
    override fun onNewIntent(intent: Intent) {
        logger.finest("onNewIntent")
        super.onNewIntent(intent)

        // New intent was received so set it to use in onStart
        setIntent(intent)
        mRefreshSettings = true
    }

    /**
     * Restart entry point
     */
    override fun onRestart() {
        logger.finest("onRestart")
        super.onRestart()
    }

    override fun onStart() {
        logger.finest("onStart")
        super.onStart()

        configure.onNext(Pair(intent, mRefreshSettings))
    }

    override fun onResume() {
        logger.finest("onResume")
        super.onResume()

        isResumed = true
        onResumeNotifier.onNext(isResumed)
        connectorController.start()
    }

    override fun onPause() {
        logger.finest("onPause")
        super.onPause()
        isResumed = false
        connectorController.stop()
    }

    override fun onStop() {
        logger.finest("onStop")
        super.onStop()

        if (!connectorController.isRunning) {
            // Not connected/connecting to a resource.
            // Release camera, mic, and speaker from this app while backgrounded.
            connectorController.stop()
        }
    }

    override fun onDestroy() {
        logger.finest("onDestroy")
        super.onDestroy()

        ConnectorPkg.setApplicationUIContext(null)
    }

    // The device interface orientation has changed
    override fun onConfigurationChanged(newConfig: Configuration) {
        logger.finest("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)

        // Refresh the video size after it is painted
        videoFrame?.executeOnGlobalLayoutEvent {
            connectorController.updateViewPort()
        }
    }

    /*
     * Private Utility Functions
     */

    // Callback containing the result of the permissions request. If permissions were not previously,
    // obtained, wait until this is received until calling createConnector where Connector is constructed.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        logger.finest("onRequestPermissionsResult: number of requested permissions = " + permissions.size)

        // If the expected request code is received, start VidyoConnector
        if (requestCode == PERMISSIONS_REQUEST_ALL) {
            for (i in permissions.indices)
                logger.finest("permission: " + permissions[i] + " " + grantResults[i])
            permissionGrantNotifier.onNext(permissions)
            println("onRequestPermissionsResult")
        } else {
            logger.finest("Unexpected permission requested.")
        }
    }

    /*
     * Button Event Callbacks
     */
    override fun onClick(v: View) {
        when (v.id) {
        // Connect or disconnect.
            R.id.connect_button -> toggleConnect()

        // Cycle the camera.
            R.id.camera_switch -> connectorController.mediaController.cycleCamera()

        // Toggle the camera privacy.
            R.id.camera_privacy -> connectorController.mediaController.cameraPrivacy.flip()

        // Toggle the microphone privacy.
            R.id.microphone_privacy -> connectorController.mediaController.microphonePrivacy.flip()

        // Toggle debugging.
            R.id.toggle_debug -> connectorController.toggleDebug()

        // Toggle toolbar visibility
            R.id.videoFrame -> viewController.toggleToolbarVisibility()

            else -> logger.warning("onClick: Unexpected click event, id=" + v.id)
        }
    }

    private fun toggleConnect() {
        viewController.toggleConnect { connect ->
            when (connect) {
                true -> contentView?.connection?.run {
                    connectorController.connect(this)
                    Unit
                }
                else -> connectorController.disconnect()
            }
        }
    }

    private fun bootstrap() =
            configure.map {
                if (connectorController.isRunning && it.second) {
                    connectorController.disconnect()
                }
                it.first
            }.map {
                val useUri = intent.data != null
                Pair(if (useUri) ConnectionData.create(intent.data) else {
                    ConnectionData.create(intent)
                }, if (useUri) OptionsData.create(intent.data) else {
                    OptionsData.create(intent)
                })
            }.flatMap {
                // apply settings
                preconfigure(it.first, it.second).andThen(Observable.just(it))
            }.flatMap { configuration ->
                // apply permissions
                Observable.concat(viewController.requirePermissions(this@MainActivity)
                        .flatMapObservable {
                            ActivityCompat.requestPermissions(this@MainActivity, it.toTypedArray(),
                                    PERMISSIONS_REQUEST_ALL)
                            permissionGrantNotifier.map { configuration }
                        },
                        Observable.just(configuration)
                ).take(1)
            }.flatMap {
                Observable.merge(
                        ObservableExtensions.emitWhen({ isResumed }, true),
                        onResumeNotifier
                ).flatMapCompletable { _ ->
                    createConnector().andThen(applySettings(connection = it.first, options = it.second))
                }.andThen(Observable.just(it))
            }

    /**
     * Apply initial settings to new connection
     */
    private fun preconfigure(connection: ConnectionData, options: OptionsData) =
            Completable.create { emitter ->
                logger.info("$connection".trimIndent())
                contentView?.connection = connection

                logger.info("$options".trimIndent())
                connectorController.setDebug(options.enableDebug)
                options.experimentalOptions?.run {
                    ConnectorPkg.setExperimentalOptions(this)
                }

                contentView?.showInput = !options.hideConfig
                emitter.onComplete()
            }

    // Construct Connector and register for event listeners.
    private fun createConnector(): Completable = Completable.create { emitter ->
        logger.finest("createConnector")
        videoFrame?.executeOnGlobalLayoutEvent {
            connectorController.viewFrame = it

            emitter.onComplete()
        }
    }

    /**
     * Apply settings to finalize connector
     */
    private fun applySettings(connection: ConnectionData, options: OptionsData): Completable =
            Completable.create { emitter ->
                viewController.applyConfiguration(options)
                options.run {
                    connectorController.mediaController.cameraPrivacy.postValue(cameraPrivacy)
                    connectorController.mediaController.microphonePrivacy.postValue(microphonePrivacy)
                    autoJoin.takeTrue {
                        connectorController.connect(connection)
                    }

                    mutableIsInitialized = true
                    emitter.onComplete()
                }
            }

    companion object {

        // - This arbitrary, app-internal constant represents a group of requested permissions.
        // - For simplicity, this app treats all desired permissions as part of a single group.
        const val PERMISSIONS_REQUEST_ALL = 1988
    }
}