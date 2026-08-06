package de.blinkt.openvpn.sargo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SargoConfigReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: SargoConfigReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = SargoConfigReceiver()
        context.getSharedPreferences("sargo_config_receiver", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun onReceive_recordsLastReceivedTimestamp() {
        val intent = Intent("pro.sargo.push.configUpdated")

        receiver.onReceive(context, intent)

        val prefs = context.getSharedPreferences("sargo_config_receiver", Context.MODE_PRIVATE)
        assertTrue(prefs.getLong("last_config_received_ms", 0) > 0)
    }

    @Test
    fun onReceive_ignoresBroadcastsThatArriveTooSoon() {
        val prefs = context.getSharedPreferences("sargo_config_receiver", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_config_received_ms", System.currentTimeMillis()).apply()
        val before = prefs.getLong("last_config_received_ms", 0)

        val intent = Intent("pro.sargo.push.configUpdated")
        receiver.onReceive(context, intent)

        assertEquals(before, prefs.getLong("last_config_received_ms", 0))
    }
}
