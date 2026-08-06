package pro.sargo.openvpn

import android.content.Context
import android.database.Cursor
import pro.sargo.openvpn.SargoVpnContract.ConfigColumns

/**
 * Reads VPN configuration from the SargO launcher ContentProvider.
 *
 * Using a ContentProvider keeps the launcher as a separate application with no
 * compile-time link to the GPL-licensed ics-openvpn code. If the provider is
 * unavailable, the configuration is simply treated as not set and the standard
 * OpenVPN UI is shown.
 */
class SargoVpnConfigProvider(private val context: Context) {

    fun loadConfig(): SargoVpnConfig? {
        val cursor = try {
            context.contentResolver.query(SargoVpnContract.CONFIG_URI, null, null, null, null)
        } catch (e: Exception) {
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
}
