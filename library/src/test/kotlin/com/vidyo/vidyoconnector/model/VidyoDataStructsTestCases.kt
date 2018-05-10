package com.vidyo.vidyoconnector.model

import android.content.Intent
import android.net.Uri
import com.vidyo.VidyoClient.Connector.Connector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class `ConnectionData tests` {

    @Test
    fun `basic test`() {
        val a = ConnectionData()
        assertEquals("", a.host)
        assertEquals("host", a.apply { host = "host" }.host)
        assertEquals("", a.token)
        assertEquals("token", a.apply { token = "token" }.token)
        assertEquals("", a.displayName)
        assertEquals("displayName", a.apply { displayName = "displayName" }.displayName)
        assertEquals("", a.resourceId)
        assertEquals("resourceId", a.apply { resourceId = "resourceId" }.resourceId)
    }

    @Test
    fun `test create from Intent with full params`() {
        val intent = Intent()
        intent.putExtra("host", "host")
        intent.putExtra("token", "token")
        intent.putExtra("displayName", "displayName")
        intent.putExtra("resourceId", "resourceId")

        val connectionData = ConnectionData.create(intent)
        assertEquals("host", connectionData.host)
        assertEquals("token", connectionData.token)
        assertEquals("displayName", connectionData.displayName)
        assertEquals("resourceId", connectionData.resourceId)
    }

    @Test
    fun `test create from Intent with no param`() {
        val intent = Intent()

        val connectionData = ConnectionData.create(intent)
        assertDefaults(connectionData)
    }

    @Test
    fun `test create from Uri with full params`() {
        val uri = Uri.parse("http://domain/item" +
                "?host=host" +
                "&token=token" +
                "&displayName=displayName" +
                "&resourceId=resourceId")

        val connectionData = ConnectionData.create(uri)
        assertEquals("host", connectionData.host)
        assertEquals("token", connectionData.token)
        assertEquals("displayName", connectionData.displayName)
        assertEquals("resourceId", connectionData.resourceId)
    }

    @Test
    fun `test create from Uri with no param`() {
        val uri = Uri.parse("""http://domain/item""".trimMargin())

        val connectionData = ConnectionData.create(uri)
        assertDefaults(connectionData)
    }

    private fun assertDefaults(connectionData: ConnectionData) {
        assertEquals("prod.vidyo.io", connectionData.host)
        assertEquals("", connectionData.token)
        assertEquals("", connectionData.displayName)
        assertEquals("demoRoom", connectionData.resourceId)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class `OptionsData tests` {

    @Test
    fun `basic test`() {
        val a = OptionsData()
        assertDefaults(a)
        assertEquals(true, a.apply { cameraPrivacy = true }.cameraPrivacy)
        assertEquals(true, a.apply { microphonePrivacy = true }.microphonePrivacy)
    }

    @Test
    fun `test create from Intent with full params`() {
        val intent = Intent()
        intent.putExtra("hideConfig", true)
        intent.putExtra("autoJoin", true)
        intent.putExtra("allowReconnect", true)
        intent.putExtra("cameraPrivacy", true)
        intent.putExtra("microphonePrivacy", true)
        intent.putExtra("enableDebug", true)
        intent.putExtra("experimentalOptions", "experimentalOptions")
        intent.putExtra("returnURL", "returnURL")

        val optionsData = OptionsData.create(intent)

        assertDefaults(optionsData, true)
        assertEquals("experimentalOptions", optionsData.experimentalOptions)
        assertEquals("returnURL", optionsData.returnURL)
    }

    @Test
    fun `test create from Intent with full params - alternate`() {
        val intent = Intent()
        intent.putExtra("hideConfig", false)
        intent.putExtra("autoJoin", false)
        intent.putExtra("allowReconnect", false)
        intent.putExtra("cameraPrivacy", false)
        intent.putExtra("microphonePrivacy", false)
        intent.putExtra("enableDebug", false)

        val optionsData = OptionsData.create(intent)
        assertDefaults(optionsData, false)
    }

    @Test
    fun `test create from Intent with no param`() {
        val intent = Intent()
        val optionsData = OptionsData.create(intent)

        assertDefaults(optionsData)
    }

    @Test
    fun `test create from Uri with full params`() {
        val uri = Uri.parse("http://domain/item" +
                "?hideConfig=true" +
                "&autoJoin=true" +
                "&allowReconnect=true" +
                "&cameraPrivacy=true" +
                "&microphonePrivacy=true" +
                "&enableDebug=true" +
                "&experimentalOptions=experimentalOptions" +
                "&returnURL=returnURL")
        val optionsData = OptionsData.create(uri)

        assertDefaults(optionsData, true)
        assertEquals("experimentalOptions", optionsData.experimentalOptions)
        assertEquals("returnURL", optionsData.returnURL)
    }

    @Test
    fun `test create from Uri with full params - alternate`() {
        val uri = Uri.parse("http://domain/item" +
                "?hideConfig=false" +
                "&autoJoin=false" +
                "&allowReconnect=false" +
                "&cameraPrivacy=false" +
                "&microphonePrivacy=false" +
                "&enableDebug=false")
        val optionsData = OptionsData.create(uri)

        assertDefaults(optionsData, false)
        assertEquals(null, optionsData.experimentalOptions)
        assertEquals(null, optionsData.returnURL)
    }

    @Test
    fun `test create from Uri with no param`() {
        val uri = Uri.parse("http://domain/item")
        val optionsData = OptionsData.create(uri)

        assertDefaults(optionsData)
    }

    private fun assertDefaults(optionsData: OptionsData, value: Boolean) {
        assertEquals(value, optionsData.hideConfig)
        assertEquals(value, optionsData.autoJoin)
        assertEquals(value, optionsData.allowReconnect)
        assertEquals(value, optionsData.cameraPrivacy)
        assertEquals(value, optionsData.microphonePrivacy)
        assertEquals(value, optionsData.enableDebug)
    }

    private fun assertDefaults(optionsData: OptionsData) {
        assertEquals(false, optionsData.hideConfig)
        assertEquals(false, optionsData.autoJoin)
        assertEquals(true, optionsData.allowReconnect)
        assertEquals(false, optionsData.cameraPrivacy)
        assertEquals(false, optionsData.microphonePrivacy)
        assertEquals(false, optionsData.enableDebug)
        assertEquals(null, optionsData.experimentalOptions)
        assertEquals(null, optionsData.returnURL)
    }
}

class `ConnectorState edge tests` {

    @Suppress("ReplaceCallWithBinaryOperator")
    @Test
    fun `test equality`() {
        assertFalse("Compare state with null", ConnectorState.Connecting().equals(null))
    }

    @Test
    fun `test hash code`() {
        assertEquals(ConnectorState.Connecting::class.java.hashCode(), ConnectorState.Connecting().hashCode())
        assertEquals(ConnectorState.Connected::class.java.hashCode(), ConnectorState.Connected().hashCode())
        assertEquals(ConnectorState.FailureInvalidResource::class.java.hashCode(), ConnectorState.FailureInvalidResource().hashCode())
        assertEquals(ConnectorState.Disconnecting::class.java.hashCode(), ConnectorState.Disconnecting().hashCode())
        assertEquals(ConnectorState.Disconnected::class.java.hashCode(), ConnectorState.Disconnected().hashCode())
    }
}

class `ConnectorState DisconnectedUnexpected edge tests` {

    @Suppress("ReplaceCallWithBinaryOperator")
    @Test
    fun `test equality`() {
        val state = ConnectorState.DisconnectedUnexpected()

        assertFalse("Compare state with null", state.equals(null))
        assertNotEquals("Compare state with different state", ConnectorState.Connecting(), state)
        assertNotEquals("Compare state with different reason",
                ConnectorState.DisconnectedUnexpected(Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_ConnectionLost),
                state)
    }

    @Test
    fun `test hash code`() {
        val state = ConnectorState.DisconnectedUnexpected(Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_ConnectionLost)
        assertEquals(Objects.hash(
                ConnectorState.DisconnectedUnexpected::class.java.hashCode(),
                Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_ConnectionLost.hashCode()
        ), state.hashCode())
    }
}

class `ConnectorState Failure edge tests` {

    @Suppress("ReplaceCallWithBinaryOperator")
    @Test
    fun `test equality`() {
        val state = ConnectorState.Failure()

        assertFalse("Compare state with null", state.equals(null))
        assertNotEquals("Compare state with different state", ConnectorState.Connecting(), state)
        assertNotEquals(
                "Compare state with different reason",
                ConnectorState.Failure(Connector.ConnectorFailReason.VIDYO_CONNECTORFAILREASON_ConnectionLost),
                state
        )
    }

    @Test
    fun `test hash code`() {
        val state = ConnectorState.Failure(Connector.ConnectorFailReason.VIDYO_CONNECTORFAILREASON_ConnectionLost)
        assertEquals(Objects.hash(
                ConnectorState.Failure::class.java.hashCode(),
                Connector.ConnectorFailReason.VIDYO_CONNECTORFAILREASON_ConnectionLost.hashCode()
        ), state.hashCode())
    }
}