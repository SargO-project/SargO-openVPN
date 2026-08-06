package de.blinkt.openvpn.sargo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives SargO configuration update broadcasts and triggers the launcher activity,
 * which will re-apply the remote VPN configuration.
 *
 * A minimum interval between processed broadcasts prevents a malfunctioning or
 * malicious MDM component from launching the activity in a tight loop.
 */
class SargoConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CONFIG_UPDATED) {
            return
        }

        if (shouldIgnore(context)) {
            Log.w(TAG, "Config update broadcast ignored: too soon after previous one")
            return
        }

        Log.i(TAG, "Received SargO config update broadcast")

        val launchIntent = Intent(context, SargoLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(launchIntent)
    }

    private fun shouldIgnore(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_RECEIVED, 0)
        if (now - last < MIN_INTERVAL_MS) {
            return true
        }
        prefs.edit().putLong(KEY_LAST_RECEIVED, now).apply()
        return false
    }

    companion object {
        private const val TAG = "SargOConfigReceiver"
        private const val ACTION_CONFIG_UPDATED = "pro.sargo.push.configUpdated"
        private const val PREFS_NAME = "sargo_config_receiver"
        private const val KEY_LAST_RECEIVED = "last_config_received_ms"
        private const val MIN_INTERVAL_MS = 5_000L
    }
}
