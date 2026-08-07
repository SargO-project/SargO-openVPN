package pro.sargo.openvpn

import android.content.Context
import android.database.Cursor
import android.util.Log
import pro.sargo.openvpn.SargoVpnContract.ConfigColumns

/**
 * Reads VPN configuration from the SargO launcher ContentProvider.
 *
 * Using a ContentProvider keeps the launcher as a separate application with no
 * compile-time link to the GPL-licensed ics-openvpn code. If the provider is
 * unavailable, the configuration is simply treated as not set and the standard
 * OpenVPN UI is shown.
 *
 * That fallback is deliberate and it is also DANGEROUSLY QUIET, which is why the
 * SecurityException case below is separated out. "No configuration" and "I was refused
 * the configuration" produce the same behaviour here -- an unmanaged VPN app showing its
 * own UI -- but they have completely different causes, and the second one is a missing
 * <uses-permission> that no build in either repository can detect.
 */
class SargoVpnConfigProvider(private val context: Context) {

    fun loadConfig(): SargoVpnConfig? {
        val cursor = try {
            context.contentResolver.query(SargoVpnContract.CONFIG_URI, null, null, null, null)
        } catch (e: SecurityException) {
            // Behaviour is unchanged; only the silence is. Reaching here means this app does not
            // hold pro.sargo.permission.READ_VPN_CONFIG, which the launcher's provider requires.
            Log.e(TAG, "Refused by the launcher's VPN config provider. This app must declare " +
                    "<uses-permission android:name=\"pro.sargo.permission.READ_VPN_CONFIG\" /> " +
                    "and be signed with the launcher's key.", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the launcher's VPN configuration", e)
            null
        } ?: return null

        cursor.use {
            if (!it.moveToFirst()) return null
            return SargoVpnConfig.fromPreferences(
                vpnName = it.getString(ConfigColumns.VPN_NAME),
                vpnConfig = it.getString(ConfigColumns.VPN_CONFIG),
                vpnConfigContent = it.getString(ConfigColumns.VPN_CONFIG_CONTENT),
                connect = it.getString(ConfigColumns.CONNECT),
                alwaysOn = it.getString(ConfigColumns.ALWAYS_ON),
                remove = it.getString(ConfigColumns.REMOVE),
                removeAll = it.getString(ConfigColumns.REMOVE_ALL),
                disconnectOnConfigChange = it.getString(ConfigColumns.DISCONNECT_ON_CONFIG_CHANGE),
                allowUserDisconnect = it.getString(ConfigColumns.ALLOW_USER_DISCONNECT),
                autoReconnect = it.getString(ConfigColumns.AUTO_RECONNECT),
                vpnUsername = it.getString(ConfigColumns.VPN_USERNAME),
                vpnPassword = it.getString(ConfigColumns.VPN_PASSWORD),
                logLevel = it.getString(ConfigColumns.LOG_LEVEL)
            )
        }
    }

    private fun Cursor.getString(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0) getString(index) else null
    }

    private companion object {
        const val TAG = "SargoVpnConfig"
    }
}
