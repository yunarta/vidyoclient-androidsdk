package com.vidyo.vidyoconnector.model

import android.content.Intent
import android.net.Uri
import com.vidyo.VidyoClient.Connector.Connector
import com.vidyo.VidyoClient.Connector.Connector.ConnectorDisconnectReason
import com.vidyo.VidyoClient.Connector.Connector.ConnectorFailReason
import java.util.*

/**
 * Data structure for connecting to server
 */
class ConnectionData(
        var host: String = "",
        var token: String = "",
        var displayName: String = "",
        var resourceId: String = ""
) {
    fun trimmed(): ConnectionData = ConnectionData(
            host.trim(),
            token.trim(),
            displayName.trim(),
            resourceId.trim()
    )

    companion object
}

class OptionsData(
        val hideConfig: Boolean = false,
        val autoJoin: Boolean = false,
        val allowReconnect: Boolean = true,
        val cameraPrivacy: Boolean = false,
        val microphonePrivacy: Boolean = false,
        val enableDebug: Boolean = false,
        val experimentalOptions: String? = null,
        val returnURL: String? = null
) {
    companion object
}

sealed class ConnectorState {

    class Connecting : ConnectorState()
    class Connected : ConnectorState()
    class FailureInvalidResource : ConnectorState()
    class Disconnecting : ConnectorState()
    class Disconnected : ConnectorState()
    class DisconnectedUnexpected(
            val reason: ConnectorDisconnectReason = Connector.ConnectorDisconnectReason
                    .VIDYO_CONNECTORDISCONNECTREASON_Disconnected
    ) : ConnectorState() {
        override fun equals(other: Any?): Boolean = if (other is DisconnectedUnexpected) {
            reason == other.reason
        } else false

        override fun hashCode(): Int {
            return Objects.hash(javaClass.hashCode(), reason.hashCode())
        }
    }

    class Failure(
            val reason: ConnectorFailReason = Connector.ConnectorFailReason
                    .VIDYO_CONNECTORFAILREASON_ConnectionFailed
    ) : ConnectorState() {
        override fun equals(other: Any?): Boolean = if (other is Failure) {
            reason == other.reason
        } else false

        override fun hashCode(): Int {
            return Objects.hash(javaClass.hashCode(), reason.hashCode())
        }
    }

    companion object {

        val Connecting = Connecting()
        val Connected = Connected()
        val Disconnecting = Disconnecting()
        val Disconnected = Disconnected()
        val FailureInvalidResource = FailureInvalidResource()
    }

    override fun equals(other: Any?): Boolean =
            if (other == null) false
            else javaClass == other.javaClass

    override fun hashCode(): Int {
        return javaClass.hashCode()
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
        uri.getQueryParameter("token") ?: "",
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