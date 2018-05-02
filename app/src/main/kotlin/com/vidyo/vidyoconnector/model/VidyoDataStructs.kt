package com.vidyo.vidyoconnector.model

import android.content.Intent
import android.net.Uri
import com.vidyo.VidyoClient.Connector.Connector

/**
 * Data structure for connecting to server
 */
data class ConnectionData(
        var host: String = "",
        var token: String = "",
        var displayName: String = "",
        var resourceId: String = ""
) {

    override fun toString(): String {
        return """ConnectionData(
            |host='$host',
            |token='$token',
            |displayName='$displayName',
            |resourceId='$resourceId')""".trimMargin()
    }

    companion object
}

data class OptionsData(
        val hideConfig: Boolean = false,
        val autoJoin: Boolean = false,
        val allowReconnect: Boolean = true,
        var cameraPrivacy: Boolean = false,
        var microphonePrivacy: Boolean = false,
        val enableDebug: Boolean = false,
        val experimentalOptions: String? = null,
        val returnURL: String? = null
) {

    override fun toString(): String {
        return """OptionsData(
            |hideConfig=$hideConfig,
            |autoJoin=$autoJoin,
            |allowReconnect=$allowReconnect,
            |cameraPrivacy=$cameraPrivacy,
            |microphonePrivacy=$microphonePrivacy,
            |enableDebug=$enableDebug,
            |experimentalOptions=$experimentalOptions,
            |returnURL=$returnURL)""".trimMargin()
    }

    companion object
}

sealed class ConnectorState {

    class Connecting : ConnectorState()
    class Connected : ConnectorState()
    class Disconnecting : ConnectorState()
    class Disconnected : ConnectorState()
    class DisconnectedUnexpected(val reason: Connector.ConnectorDisconnectReason) : ConnectorState()
    class Failure(val reason: Connector.ConnectorFailReason) : ConnectorState()
    class FailureInvalidResource : ConnectorState()

    companion object {

        val Connecting = Connecting()
        val Connected = Connected()
        val Disconnecting = Disconnecting()
        val Disconnected = Disconnected()
        val FailureInvalidResource = FailureInvalidResource()
    }
}

fun ConnectionData.Companion.create(intent: Intent): ConnectionData = ConnectionData(
        intent.getStringExtra("host") ?: "prod.vidyo.io",
        intent.getStringExtra("token") ?: "",
        intent.getStringExtra("displayName") ?: "",
        intent.getStringExtra("resourceId") ?: "demoRoom"
)

fun ConnectionData.Companion.create(uri: Uri): ConnectionData = ConnectionData(
        uri.getQueryParameter("host") ?: "prod.vidyo.io",
        uri.getQueryParameter("host") ?: "",
        uri.getQueryParameter("displayName") ?: "",
        uri.getQueryParameter("resourceId") ?: "demoRoom"
)

fun OptionsData.Companion.create(intent: Intent): OptionsData = OptionsData(
        intent.getBooleanExtra("hideConfig", false),
        intent.getBooleanExtra("autoJoin", false),
        intent.getBooleanExtra("allowReconnect", true),
        intent.getBooleanExtra("cameraPrivacy", false),
        intent.getBooleanExtra("microphonePrivacy", false),
        intent.getBooleanExtra("enableDebug", false),
        intent.getStringExtra("experimentalOptions"),
        intent.getStringExtra("returnURL")
)

fun OptionsData.Companion.create(uri: Uri): OptionsData = OptionsData(
        uri.getBooleanQueryParameter("hideConfig", false),
        uri.getBooleanQueryParameter("autoJoin", false),
        uri.getBooleanQueryParameter("allowReconnect", true),
        uri.getBooleanQueryParameter("cameraPrivacy", false),
        uri.getBooleanQueryParameter("microphonePrivacy", false),
        uri.getBooleanQueryParameter("enableDebug", false),
        uri.getQueryParameter("experimentalOptions"),
        uri.getQueryParameter("returnURL")
)