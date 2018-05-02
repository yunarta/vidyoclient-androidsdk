package com.vidyo.vidyoconnector

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.res.Configuration
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.vidyo.VidyoClient.Connector.ConnectorPkg
import com.vidyo.vidyoconnector.databinding.ActivityMainBinding
import com.vidyo.vidyoconnector.model.ConnectionData
import com.vidyo.vidyoconnector.model.MainActivityRouter
import com.vidyo.vidyoconnector.model.MainActivityRouterDelegate
import com.vidyo.vidyoconnector.model.MainActivityRouterImpl
import com.vidyo.vidyoconnector.model.MainActivityViewModel
import com.vidyo.vidyoconnector.model.MutableVidyoConnectorController
import com.vidyo.vidyoconnector.model.MutableVidyoViewController
import com.vidyo.vidyoconnector.model.OptionsData
import com.vidyo.vidyoconnector.model.VidyoConnectorController
import com.vidyo.vidyoconnector.model.VidyoConnectorControllerDelegate
import com.vidyo.vidyoconnector.model.VidyoViewController
import com.vidyo.vidyoconnector.model.VidyoViewControllerDelegate
import com.vidyo.vidyoconnector.model.ViewPort
import com.vidyo.vidyoconnector.model.ViewPortListener
import com.vidyo.vidyoconnector.model.create
import com.vidyo.vidyoconnector.model.isRunning
import com.vidyo.vidyoconnector.model.toggleDebug
import com.vidyo.vidyoconnector.util.ObservableExtensions
import com.vidyo.vidyoconnector.util.emitWhen
import com.vidyo.vidyoconnector.util.executeOnGlobalLayoutEvent
import com.vidyo.vidyoconnector.util.flip
import com.vidyo.vidyoconnector.util.takeTrue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.logging.Logger

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val logger: Logger? = Logger.getLogger("main")
    private var mRefreshSettings = true

    private lateinit var contentView: ActivityMainBinding

    private lateinit var connectorController: VidyoConnectorController
    private lateinit var router: MainActivityRouter

    private lateinit var viewController: VidyoViewController
    private lateinit var viewModel: MainActivityViewModel


    private val permissionGrantNotifier: PublishSubject<Array<String>> by lazy {
        PublishSubject.create<Array<String>>()
    }

    private var isResumed = false
    private val onResumeNotifier: PublishSubject<Boolean> by lazy {
        PublishSubject.create<Boolean>()
    }

    private var configure = PublishSubject.create<Intent>()
    private lateinit var bootstrap: Disposable

    /*
     *  Operating System Events
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        logger?.finest("onCreate")
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {

            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainActivityViewModel(
                        VidyoViewControllerDelegate(delegate = MutableVidyoViewController()),
                        VidyoConnectorControllerDelegate(delegate = MutableVidyoConnectorController())
                ) as T
            }
        }).get(MainActivityViewModel::class.java)

        router = MainActivityRouterDelegate(MainActivityRouterImpl(this))

        viewController = viewModel.view
        viewController.router = router

        connectorController = viewModel.connector
        connectorController.connectionState.observe(this, viewController.connectorStateObserver)
        connectorController.viewPortListener = object : ViewPortListener {
            override val rect: ViewPort
                get() = contentView.videoFrame.run {
                    return ViewPort(0, 0, width, height)
                }
        }
        connectorController.debug.observe(this, Observer {
            viewModel.view.isShowVersion.value = it
        })

        // Set the application's UI context to this activity.
        ConnectorPkg.setApplicationUIContext(this)
        bootstrap = bootstrap()

        bindContentView()
    }

    private fun bindContentView() {
        contentView = DataBindingUtil.setContentView(this, R.layout.activity_main)
        contentView.connector = connectorController
        contentView.view = viewController
        contentView.setLifecycleOwner(this)

        // Initialize the member variables
        contentView.videoFrame.setOnClickListener(this)
        contentView.microphonePrivacy.setOnClickListener(this)
        contentView.cameraPrivacy.setOnClickListener(this)
        contentView.cameraSwitch.setOnClickListener(this)
        contentView.connectButton.setOnClickListener(this)
        contentView.toggleDebug.setOnClickListener(this)
    }


    /**
     * For new intent that changes the connection configuration
     */
    override fun onNewIntent(intent: Intent) {
        logger?.finest("onNewIntent")
        super.onNewIntent(intent)

        // New intent was received so set it to use in onStart
        setIntent(intent)
        mRefreshSettings = true
    }

    /**
     * Restart entry point
     */
    override fun onRestart() {
        logger?.finest("onRestart")
        super.onRestart()
    }

    override fun onStart() {
        logger?.finest("onStart")
        super.onStart()
        configure.onNext(intent)
    }

    override fun onResume() {
        logger?.finest("onResume")
        super.onResume()

        isResumed = true
        onResumeNotifier.onNext(isResumed)
        connectorController.start()
    }

    override fun onPause() {
        logger?.finest("onPause")
        super.onPause()
        isResumed = false
        connectorController.stop()
    }

    override fun onStop() {
        logger?.finest("onStop")
        super.onStop()

        if (!connectorController.isRunning) {
            // Not connected/connecting to a resource.
            // Release camera, mic, and speaker from this app while backgrounded.
            connectorController.stop()
        }
    }

    override fun onDestroy() {
        logger?.finest("onDestroy")
        super.onDestroy()

        ConnectorPkg.setApplicationUIContext(null)
    }

    // The device interface orientation has changed
    override fun onConfigurationChanged(newConfig: Configuration) {
        logger?.finest("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)

        // Refresh the video size after it is painted
        contentView.videoFrame.executeOnGlobalLayoutEvent {
            connectorController.updateViewPort()
        }
    }

    /*
     * Private Utility Functions
     */

    // Callback containing the result of the permissions request. If permissions were not previously,
    // obtained, wait until this is received until calling createConnector where Connector is constructed.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        logger?.finest("onRequestPermissionsResult: number of requested permissions = " + permissions.size)

        // If the expected request code is received, start VidyoConnector
        if (requestCode == PERMISSIONS_REQUEST_ALL) {
            for (i in permissions.indices)
                logger?.finest("permission: " + permissions[i] + " " + grantResults[i])
            permissionGrantNotifier.onNext(permissions)
        } else {
            logger?.finest("Unexpected permission requested.")
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

            else -> logger?.warning("onClick: Unexpected click event, id=" + v.id)
        }
    }

    private fun toggleConnect() {
        viewController.toggleConnect { connect ->
            when (connect) {
                true -> contentView.connection?.run {
                    connectorController.connect(this)
                    Unit
                }
                else -> connectorController.disconnect()
            }
        }
    }


    private fun bootstrap() =
            configure.doOnNext {
                if (connectorController.isRunning) {
                    connectorController.disconnect()
                }
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
                viewController.requirePermissions(this@MainActivity)
                        .flatMapObservable {
                            ActivityCompat.requestPermissions(this@MainActivity, it.toTypedArray(),
                                    PERMISSIONS_REQUEST_ALL)
                            permissionGrantNotifier
                        }.ignoreElements().andThen(Observable.just(configuration))
            }.flatMap {
                Observable.merge(
                        ObservableExtensions.emitWhen({ isResumed }, true),
                        onResumeNotifier
                ).flatMapCompletable { _ ->
                    createConnector().andThen(applySettings(connection = it.first, options = it.second))
                }.andThen(Observable.empty<Unit>())
            }.subscribe()

    /**
     * Apply initial settings to new connection
     */
    private fun preconfigure(connection: ConnectionData, options: OptionsData) =
            Completable.create { emitter ->
                connection.displayName = "yunarta"
                connection.token = """cHJvdmlzaW9uAHVzZXIxQGI5MjIyMy52aWR5by5pbwA2MzY5MjQ2M
                    |TMyNwAAYTc3MzA0MTZjMzc5NjA4MjFkMzMzNGJiYjdhZmI1MzU0NjkzMWExMjVmODI0Y2F
                    |hN2YxNTFjYTQwN2FjY2NmNDcxNDhmN2QzMzRjNDQ0NDUwMTVhZjgxOTFlNGQ0YjVj""".trimMargin()

                logger?.info("$connection".trimIndent())
                contentView.connection = connection

                logger?.info("$options".trimIndent())
                connectorController.setDebug(options.enableDebug)
                options.experimentalOptions?.run {
                    ConnectorPkg.setExperimentalOptions(this)
                }

                contentView.showInput = !options.hideConfig
                emitter.onComplete()
            }

    // Construct Connector and register for event listeners.
    private fun createConnector(): Completable = Completable.create { emitter ->
        logger?.finest("createConnector")
        contentView.videoFrame.run {
            logger?.finest("""Viewport frame width = $width, height = $height""")
        }

        contentView.videoFrame.executeOnGlobalLayoutEvent {
            connectorController.viewFrame = it

            emitter.onComplete()
        }
    }

    /**
     * Apply settings to finalize connector
     */
    private fun applySettings(connection: ConnectionData, options: OptionsData): Completable =
            Completable.create { emitter ->
                viewController.applyConfiguration(connection, options)
                options.run {

                    connectorController.mediaController.cameraPrivacy.postValue(cameraPrivacy)
                    connectorController.mediaController.microphonePrivacy.postValue(microphonePrivacy)
                    autoJoin.takeTrue {
                        connectorController.connect(connection)
                    }
                    emitter.onComplete()
                }
            }

    companion object {

        // - This arbitrary, app-internal constant represents a group of requested permissions.
        // - For simplicity, this app treats all desired permissions as part of a single group.
        private const val PERMISSIONS_REQUEST_ALL = 1988
    }
}