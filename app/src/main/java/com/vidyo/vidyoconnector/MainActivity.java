package com.vidyo.vidyoconnector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Endpoint.LogRecord;
import com.vidyo.VidyoClient.NetworkInterface;

public class MainActivity extends Activity implements
        View.OnClickListener,
        Connector.IConnect,
        Connector.IRegisterLogEventListener,
        Connector.IRegisterNetworkInterfaceEventListener,
        Connector.IRegisterLocalCameraEventListener,
        IVideoFrameListener {

    // Define the various states of this application.
    enum VidyoConnectorState {
        Connecting,
        Connected,
        Disconnecting,
        Disconnected,
        DisconnectedUnexpected,
        Failure,
        FailureInvalidResource
    }

    // Map the application state to the status to display in the toolbar.
    private static final Map<VidyoConnectorState, String> mStateDescription = new HashMap<VidyoConnectorState, String>() {{
        put(VidyoConnectorState.Connecting, "Connecting...");
        put(VidyoConnectorState.Connected, "Connected");
        put(VidyoConnectorState.Disconnecting, "Disconnecting...");
        put(VidyoConnectorState.Disconnected, "Disconnected");
        put(VidyoConnectorState.DisconnectedUnexpected, "Unexpected disconnection");
        put(VidyoConnectorState.Failure, "Connection failed");
        put(VidyoConnectorState.FailureInvalidResource, "Invalid Resource ID");
    }};

    // Helps check whether app has permission to access what is declared in its manifest.
    // - Permissions from app's manifest that have a "protection level" of "dangerous".
    private static final String[] mPermissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    // - This arbitrary, app-internal constant represents a group of requested permissions.
    // - For simplicity, this app treats all desired permissions as part of a single group.
    private final int PERMISSIONS_REQUEST_ALL = 1988;

    private VidyoConnectorState mVidyoConnectorState = VidyoConnectorState.Disconnected;
    private boolean mVidyoClientInitialized = false;
    private Logger mLogger = Logger.getInstance();
    private Connector mVidyoConnector = null;
    private LocalCamera mLastSelectedCamera = null;
    private ToggleButton mToggleConnectButton;
    private ToggleButton mMicrophonePrivacyButton;
    private ToggleButton mCameraPrivacyButton;
    private ProgressBar mConnectionSpinner;
    private LinearLayout mControlsLayout;
    private LinearLayout mToolbarLayout;
    private EditText mHost;
    private EditText mDisplayName;
    private EditText mToken;
    private EditText mResourceId;
    private TextView mToolbarStatus;
    private TextView mClientVersion;
    private VideoFrameLayout mVideoFrame;
    private boolean mHideConfig = false;
    private boolean mAutoJoin = false;
    private boolean mAllowReconnect = true;
    private boolean mCameraPrivacy = false;
    private boolean mMicrophonePrivacy = false;
    private boolean mEnableDebug = false;
    private String mReturnURL = null;
    private String mExperimentalOptions = null;
    private MainActivity mSelf;
    private boolean mRefreshSettings = true;
    private boolean mDevicesSelected = true;

    /*
     *  Operating System Events
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLogger.Log("onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize the member variables
        mControlsLayout = (LinearLayout) findViewById(R.id.controlsLayout);
        mToolbarLayout = (LinearLayout) findViewById(R.id.toolbarLayout);
        mVideoFrame = (VideoFrameLayout) findViewById(R.id.videoFrame);
        mVideoFrame.Register(this);
        mHost = (EditText) findViewById(R.id.hostTextBox);
        mDisplayName = (EditText) findViewById(R.id.displayNameTextBox);
        mToken = (EditText) findViewById(R.id.tokenTextBox);
        mResourceId = (EditText) findViewById(R.id.resourceIdTextBox);
        mToolbarStatus = (TextView) findViewById(R.id.toolbarStatusText);
        mClientVersion = (TextView) findViewById(R.id.clientVersion);
        mConnectionSpinner = (ProgressBar) findViewById(R.id.connectionSpinner);
        mSelf = this;

        // Set the onClick listeners for the buttons
        mToggleConnectButton = (ToggleButton) findViewById(R.id.connect);
        mToggleConnectButton.setOnClickListener(this);
        mMicrophonePrivacyButton = (ToggleButton) findViewById(R.id.microphone_privacy);
        mMicrophonePrivacyButton.setOnClickListener(this);
        mCameraPrivacyButton = (ToggleButton) findViewById(R.id.camera_privacy);
        mCameraPrivacyButton.setOnClickListener(this);
        ToggleButton button = (ToggleButton) findViewById(R.id.camera_switch);
        button.setOnClickListener(this);
        button = (ToggleButton) findViewById(R.id.toggle_debug);
        button.setOnClickListener(this);

        // Set the application's UI context to this activity.
        ConnectorPkg.setApplicationUIContext(this);

        // Initialize the VidyoClient library - this should be done once in the lifetime of the application.
        mVidyoClientInitialized = ConnectorPkg.initialize();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mLogger.Log("onNewIntent");
        super.onNewIntent(intent);

        // Set the refreshSettings flag so the app settings are refreshed in onStart
        mRefreshSettings = true;

        // New intent was received so set it to use in onStart
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        mLogger.Log("onStart");
        super.onStart();

        // Initialize or refresh the app settings.
        // When app is first launched, mRefreshSettings will always be true.
        // Each successive time that onStart is called, app is coming back to foreground so check if the
        // settings need to be refreshed again, as app may have been launched via URI.
        if (mRefreshSettings &&
            mVidyoConnectorState != VidyoConnectorState.Connected &&
            mVidyoConnectorState != VidyoConnectorState.Connecting) {

            Intent intent = getIntent();
            Uri uri = intent.getData();

            // Check if app was launched via URI
            if (uri != null) {
                String param = uri.getQueryParameter("host");
                mHost.setText( param != null ? param : "prod.vidyo.io");

                param = uri.getQueryParameter("token");
                mToken.setText(param != null ? param : "");

                param = uri.getQueryParameter("displayName");
                mDisplayName.setText(param != null ? param : "");

                param = uri.getQueryParameter("resourceId");
                mResourceId.setText(param != null ? param : "");

                mReturnURL = uri.getQueryParameter("returnURL");
                mHideConfig = uri.getBooleanQueryParameter("hideConfig", false);
                mAutoJoin = uri.getBooleanQueryParameter("autoJoin", false);
                mAllowReconnect = uri.getBooleanQueryParameter("allowReconnect", true);
                mCameraPrivacy = uri.getBooleanQueryParameter("cameraPrivacy", false);
                mMicrophonePrivacy = uri.getBooleanQueryParameter("microphonePrivacy", false);
                mEnableDebug = uri.getBooleanQueryParameter("enableDebug", false);
                mExperimentalOptions = uri.getQueryParameter("experimentalOptions");
            } else {
                // If the app was launched by a different app, then get any parameters; otherwise use default settings
                mHost.setText(intent.hasExtra("host") ? intent.getStringExtra("host") : "prod.vidyo.io");
                mToken.setText(intent.hasExtra("token") ? intent.getStringExtra("token") : "");
                mDisplayName.setText(intent.hasExtra("displayName") ? intent.getStringExtra("displayName") : "");
                mResourceId.setText(intent.hasExtra("resourceId") ? intent.getStringExtra("resourceId") : "");
                mReturnURL = intent.hasExtra("returnURL") ? intent.getStringExtra("returnURL") : null;
                mHideConfig = intent.getBooleanExtra("hideConfig", false);
                mAutoJoin = intent.getBooleanExtra("autoJoin", false);
                mAllowReconnect = intent.getBooleanExtra("allowReconnect", true);
                mCameraPrivacy = intent.getBooleanExtra("cameraPrivacy", false);
                mMicrophonePrivacy = intent.getBooleanExtra("microphonePrivacy", false);
                mEnableDebug = intent.getBooleanExtra("enableDebug", false);
                mExperimentalOptions = intent.hasExtra("experimentalOptions") ? intent.getStringExtra("experimentalOptions") : null;
            }

            mLogger.Log("onStart: hideConfig = " + mHideConfig + ", autoJoin = " + mAutoJoin + ", allowReconnect = " + mAllowReconnect + ", enableDebug = " + mEnableDebug);

            // Hide the controls if hideConfig enabled
            if (mHideConfig) {
                mControlsLayout.setVisibility(View.GONE);
            }

            // Apply the app settings if the Connector object has been created
            if (mVidyoConnector != null) {
                mSelf.applySettings();
            }
        }
        mRefreshSettings = false;

        // If Vidyo Client has been successfully initialized and the Connector has
        // not yet been constructed, then check permissions and construct Connector.
        if (mVidyoClientInitialized && mVidyoConnector == null) {
            // Beginning in Android 6.0 (API level 23), users grant permissions to apps while
            // app is running, not when they install the app. Check whether app has permission
            // to access what is declared in its manifest.
            if (Build.VERSION.SDK_INT > 22) {
                List<String> permissionsNeeded = new ArrayList<>();
                for (String permission : mPermissions) {
                    // Check if the permission has already been granted.
                    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                        permissionsNeeded.add(permission);
                }
                if (permissionsNeeded.size() > 0) {
                    // Request any permissions which have not been granted. The result will be called back in onRequestPermissionsResult.
                    ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_ALL);
                } else {
                    startVidyoConnector();
                }
            } else {
                startVidyoConnector();
            }
        }
    }

    @Override
    protected void onResume() {
        mLogger.Log("onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        mLogger.Log("onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        mLogger.Log("onStop");
        super.onStop();

        if (mVidyoConnector != null) {
            if (mVidyoConnectorState != VidyoConnectorState.Connected &&
                mVidyoConnectorState != VidyoConnectorState.Connecting) {
                // Not connected/connecting to a resource.
                // Release camera, mic, and speaker from this app while backgrounded.
                mVidyoConnector.selectLocalCamera(null);
                mVidyoConnector.selectLocalMicrophone(null);
                mVidyoConnector.selectLocalSpeaker(null);
                mDevicesSelected = false;
            }
            mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Background);
        }
    }

    @Override
    protected void onRestart() {
        mLogger.Log("onRestart");
        super.onRestart();

        if (mVidyoConnector != null) {
            mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);

            if (!mDevicesSelected) {
                // Devices have been released when backgrounding (in onStop). Re-select them.
                mDevicesSelected = true;

                // Select the previously selected local camera and default mic/speaker
                mVidyoConnector.selectLocalCamera(mLastSelectedCamera);
                mVidyoConnector.selectDefaultMicrophone();
                mVidyoConnector.selectDefaultSpeaker();

                // Reestablish camera and microphone privacy states
                mVidyoConnector.setCameraPrivacy(mCameraPrivacy);
                mVidyoConnector.setMicrophonePrivacy(mMicrophonePrivacy);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mLogger.Log("onDestroy");
        super.onDestroy();

        // Release device resources
        mLastSelectedCamera = null;
        mVidyoConnector.disable();

        // Connector will be destructed upon garbage collection.
        mVidyoConnector = null;

        ConnectorPkg.setApplicationUIContext(null);

        // Uninitialize the VidyoClient library - this should be done once in the lifetime of the application.
        ConnectorPkg.uninitialize();
    }

    // The device interface orientation has changed
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mLogger.Log("onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

        // Refresh the video size after it is painted
        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mVideoFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Width/height values of views not updated at this point so need to wait
                    // before refreshing UI

                    refreshUI();
                }
            });
        }
    }

    /*
     * Private Utility Functions
     */

    // Callback containing the result of the permissions request. If permissions were not previously,
    // obtained, wait until this is received until calling startVidyoConnector where Connector is constructed.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mLogger.Log("onRequestPermissionsResult: number of requested permissions = " + permissions.length);

        // If the expected request code is received, start VidyoConnector
        if (requestCode == PERMISSIONS_REQUEST_ALL) {
            for (int i = 0; i < permissions.length; ++i)
                mLogger.Log("permission: " + permissions[i] + " " + grantResults[i]);
            startVidyoConnector();
        } else {
            mLogger.Log("Unexpected permission requested.");
        }
    }

    // Construct Connector and register for event listeners.
    private void startVidyoConnector() {
        mLogger.Log("startVidyoConnector");

        // Wait until mVideoFrame is drawn until getting started.
        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mVideoFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Construct Connector
                    try {
                        mLogger.Log("Constructing Connector");
                        mVidyoConnector = new Connector(mVideoFrame,
                                Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                                15,
                                "info@VidyoClient info@VidyoConnector warning",
                                "",
                                0);

                        // Set the client version in the toolbar
                        mClientVersion.setText("VidyoClient-AndroidSDK " + mVidyoConnector.getVersion());

                        // Set initial position
                        refreshUI();

                        // Register for local camera events
                        if (!mVidyoConnector.registerLocalCameraEventListener(mSelf)) {
                            mLogger.Log("registerLocalCameraEventListener failed");
                        }
                        // Register for network interface events
                        if (!mVidyoConnector.registerNetworkInterfaceEventListener(mSelf)) {
                            mLogger.Log("registerNetworkInterfaceEventListener failed");
                        }
                        // Register for log events
                        if (!mVidyoConnector.registerLogEventListener(mSelf, "info@VidyoClient info@VidyoConnector warning")) {
                            mLogger.Log("registerLogEventListener failed");
                        }

                        // Apply the app settings
                        mSelf.applySettings();
                    }
                    catch (Exception e) {
                        mLogger.Log("Connector Construction failed");
                        mLogger.Log(e.getMessage());
                    }
                }
            });
        }
    }

    // Apply some of the app settings
    private void applySettings() {
        // If enableDebug is configured then enable debugging
        if (mEnableDebug) {
            mVidyoConnector.enableDebug(7776, "warning info@VidyoClient info@VidyoConnector");
            mClientVersion.setVisibility(View.VISIBLE);
        } else {
            mVidyoConnector.disableDebug();
        }

        // If cameraPrivacy is configured then mute the camera
        mCameraPrivacyButton.setChecked(false); // reset state
        if (mCameraPrivacy) {
            mCameraPrivacyButton.performClick();
        }

        // If microphonePrivacy is configured then mute the microphone
        mMicrophonePrivacyButton.setChecked(false); // reset state
        if (mMicrophonePrivacy) {
            mMicrophonePrivacyButton.performClick();
        }

        // Set experimental options if any exist
        if (mExperimentalOptions != null) {
            ConnectorPkg.setExperimentalOptions(mExperimentalOptions);
        }

        // If configured to auto-join, then simulate a click of the toggle connect button
        if (mAutoJoin) {
            mToggleConnectButton.performClick();
        }
    }

    // Refresh the UI
    private void refreshUI() {
        // Refresh the rendering of the video
        mLogger.Log("ShowViewAt: x = 0, y = 0, w = " + mVideoFrame.getWidth() + ", h = " + mVideoFrame.getHeight());
        mVidyoConnector.showViewAt(mVideoFrame, 0, 0, mVideoFrame.getWidth(), mVideoFrame.getHeight());
    }

    // The state of the VidyoConnector connection changed, reconfigure the UI.
    // If connected, dismiss the controls layout
    private void changeState(VidyoConnectorState state) {
        mLogger.Log("changeState: " + state.toString());

        mVidyoConnectorState = state;

        // Execute this code on the main thread since it is updating the UI layout.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Set the status text in the toolbar.
                mToolbarStatus.setText(mStateDescription.get(mVidyoConnectorState));

                // Depending on the state, do a subset of the following:
                // - update the toggle connect button to either start call or end call image: mToggleConnectButton
                // - display toolbar in case it is hidden: mToolbarLayout
                // - show/hide the connection spinner: mConnectionSpinner
                // - show/hide the input form: mControlsLayout
                switch (mVidyoConnectorState) {
                    case Connecting:
                        mToggleConnectButton.setChecked(true);
                        mConnectionSpinner.setVisibility(View.VISIBLE);
                        break;

                    case Connected:
                        mToggleConnectButton.setChecked(true);
                        mControlsLayout.setVisibility(View.GONE);
                        mConnectionSpinner.setVisibility(View.INVISIBLE);
                        break;

                    case Disconnecting:
                        // The button just switched to the callStart image.
                        // Change the button back to the callEnd image because do not want to assume that the Disconnect
                        // call will actually end the call. Need to wait for the callback to be received
                        // before swapping to the callStart image.
                        mToggleConnectButton.setChecked(true);
                        break;

                    case Disconnected:
                    case DisconnectedUnexpected:
                    case Failure:
                    case FailureInvalidResource:
                        mToggleConnectButton.setChecked(false);
                        mToolbarLayout.setVisibility(View.VISIBLE);
                        mConnectionSpinner.setVisibility(View.INVISIBLE);

                        // If a return URL was provided as an input parameter, then return to that application
                        if (mReturnURL != null) {
                            // Provide a callstate of either 0 or 1, depending on whether the call was successful
                            Intent returnApp = getPackageManager().getLaunchIntentForPackage(mReturnURL);
                            returnApp.putExtra("callstate", (mVidyoConnectorState == VidyoConnectorState.Disconnected) ? 1 : 0);
                            startActivity(returnApp);
                        }

                        // If the allow-reconnect flag is set to false and a normal (non-failure) disconnect occurred,
                        // then disable the toggle connect button, in order to prevent reconnection.
                        if (!mAllowReconnect && (mVidyoConnectorState == VidyoConnectorState.Disconnected)) {
                            mToggleConnectButton.setEnabled(false);
                            mToolbarStatus.setText("Call ended");
                        }

                        if (!mHideConfig ) {
                            // Display the controls
                            mControlsLayout.setVisibility(View.VISIBLE);
                        }
                        break;
                }
            }
        });
    }

    /*
     * Button Event Callbacks
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                // Connect or disconnect.
                this.toggleConnect();
                break;

            case R.id.camera_switch:
                // Cycle the camera.
                mVidyoConnector.cycleCamera();
                break;

            case R.id.camera_privacy:
                // Toggle the camera privacy.
                mCameraPrivacy = mCameraPrivacyButton.isChecked();
                mVidyoConnector.setCameraPrivacy(mCameraPrivacy);
                break;

            case R.id.microphone_privacy:
                // Toggle the microphone privacy.
                mMicrophonePrivacy = mMicrophonePrivacyButton.isChecked();
                mVidyoConnector.setMicrophonePrivacy(mMicrophonePrivacy);
                break;

            case R.id.toggle_debug:
                // Toggle debugging.
                mEnableDebug = !mEnableDebug;
                if (mEnableDebug) {
                    mVidyoConnector.enableDebug(7776, "warning info@VidyoClient info@VidyoConnector");
                    mClientVersion.setVisibility(View.VISIBLE);
                } else {
                    mVidyoConnector.disableDebug();
                    mClientVersion.setVisibility(View.INVISIBLE);
                }
                break;

            default:
                mLogger.Log("onClick: Unexpected click event, id=" + v.getId());
                break;
        }
    }

    // The Connect button was pressed.
    // If not in a call, attempt to connect to the backend service.
    // If in a call, disconnect.
    public void toggleConnect() {
        if (mToggleConnectButton.isChecked()) {
            // Abort the Connect call if resource ID is invalid. It cannot contain empty spaces or "@".
            String resourceId = mResourceId.getText().toString().trim(); // trim leading and trailing white space
            if (resourceId.contains(" ") || resourceId.contains("@")) {
                changeState(VidyoConnectorState.FailureInvalidResource);
            } else {
                changeState(VidyoConnectorState.Connecting);

                final boolean status = mVidyoConnector.connect(
                        mHost.getText().toString().trim(),
                        mToken.getText().toString().trim(),
                        mDisplayName.getText().toString().trim(),
                        resourceId,
                        this);
                if (!status) {
                    changeState(VidyoConnectorState.Failure);
                }
                mLogger.Log("VidyoConnectorConnect status = " + status);
            }
        } else {
            // The user is either connected to a resource or is in the process of connecting to a resource;
            // Call VidyoConnectorDisconnect to either disconnect or abort the connection attempt.
            changeState(VidyoConnectorState.Disconnecting);
            mVidyoConnector.disconnect();
        }
    }

    // Toggle visibility of the toolbar
    @Override
    public void onVideoFrameClicked() {
        if (mVidyoConnectorState == VidyoConnectorState.Connected) {
            if (mToolbarLayout.getVisibility() == View.VISIBLE) {
                mToolbarLayout.setVisibility(View.INVISIBLE);
            } else {
                mToolbarLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    /*
     *  Connector Events
     */

    // Handle successful connection.
    @Override
    public void onSuccess() {
        mLogger.Log("onSuccess: successfully connected.");
        changeState(VidyoConnectorState.Connected);
    }

    // Handle attempted connection failure.
    @Override
    public void onFailure(Connector.ConnectorFailReason reason) {
        mLogger.Log("onFailure: connection attempt failed, reason = " + reason.toString());
        changeState(VidyoConnectorState.Failure);
    }

    // Handle an existing session being disconnected.
    @Override
    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {
        if (reason == Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            mLogger.Log("onDisconnected: successfully disconnected, reason = " + reason.toString());
            changeState(VidyoConnectorState.Disconnected);
        } else {
            mLogger.Log("onDisconnected: unexpected disconnection, reason = " + reason.toString());
            changeState(VidyoConnectorState.DisconnectedUnexpected);
        }
    }

    // Handle local camera events.
    @Override
    public void onLocalCameraAdded(LocalCamera localCamera) {
        mLogger.Log("onLocalCameraAdded: " + localCamera.getName());
    }

    @Override
    public void onLocalCameraRemoved(LocalCamera localCamera) {
        mLogger.Log("onLocalCameraRemoved: " + localCamera.getName());
    }

    @Override
    public void onLocalCameraSelected(LocalCamera localCamera) {
        mLogger.Log("onLocalCameraSelected: " + (localCamera == null ? "none" : localCamera.getName()));

        // If a camera is selected, then update mLastSelectedCamera.
        if (localCamera != null) {
            mLastSelectedCamera = localCamera;
        }
    }

    @Override
    public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState state) {
        mLogger.Log("onLocalCameraStateUpdated: name=" + localCamera.getName() + " state=" + state);
    }

    // Handle a message being logged.
    @Override
    public void onLog(LogRecord logRecord) {
        mLogger.LogClientLib(logRecord.name + " : " + logRecord.level + " : " + logRecord.functionName + " : " + logRecord.message);
    }

    // Handle network interface events
    @Override
    public void onNetworkInterfaceAdded(NetworkInterface vidyoNetworkInterface) {
        mLogger.Log("onNetworkInterfaceAdded: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily());
    }

    @Override
    public void onNetworkInterfaceRemoved(NetworkInterface vidyoNetworkInterface) {
        mLogger.Log("onNetworkInterfaceRemoved: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily());
    }

    @Override
    public void onNetworkInterfaceSelected(NetworkInterface vidyoNetworkInterface, NetworkInterface.NetworkInterfaceTransportType vidyoNetworkInterfaceTransportType) {
        mLogger.Log("onNetworkInterfaceSelected: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily());
    }

    @Override
    public void onNetworkInterfaceStateUpdated(NetworkInterface vidyoNetworkInterface, NetworkInterface.NetworkInterfaceState vidyoNetworkInterfaceState) {
        mLogger.Log("onNetworkInterfaceStateUpdated: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily() + " state=" + vidyoNetworkInterfaceState);
    }
}
