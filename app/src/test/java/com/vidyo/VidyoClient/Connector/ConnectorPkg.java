package com.vidyo.VidyoClient.Connector;

import android.app.Activity;

public class ConnectorPkg {

    public ConnectorPkg() {

    }

    public static void setApplicationUIContextNative(Activity activity) {
    }

    public static boolean setExperimentalOptionsNative(String options) {
        return true;
    }

    private boolean initializeNative() {
        return true;
    }

    private static void uninitializeNative() {
    }

    public static boolean initialize() {
        return true;
    }

    public static void setApplicationUIContext(Activity activity) {
    }

    public static boolean setExperimentalOptions() {
        return true;
    }

    public static void uninitialize() {
    }
}
