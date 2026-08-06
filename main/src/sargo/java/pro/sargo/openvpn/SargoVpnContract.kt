package pro.sargo.openvpn

import android.net.Uri

/**
 * Public contract for the SargO launcher ContentProvider that supplies VPN
 * configuration and device-admin metadata to this OpenVPN flavor.
 *
 * This contract uses only standard Android APIs (ContentResolver/ContentProvider)
 * so that the SargO launcher remains a separate application with no compile-time
 * dependency on the GPL-licensed ics-openvpn code.
 */
object SargoVpnContract {

    const val AUTHORITY = "pro.sargo.launcher.vpn"

    val CONFIG_URI: Uri = Uri.parse("content://$AUTHORITY/config")
    val ADMIN_URI: Uri = Uri.parse("content://$AUTHORITY/admin")

    object ConfigColumns {
        const val VPN_NAME = "vpn_name"
        const val VPN_CONFIG = "vpn_config"
        const val VPN_CONFIG_CONTENT = "vpn_config_content"
        const val CONNECT = "connect"
        const val ALWAYS_ON = "always_on"
        const val REMOVE = "remove"
        const val REMOVE_ALL = "remove_all"
        const val DISCONNECT_ON_CONFIG_CHANGE = "disconnect_on_config_change"
        const val ALLOW_USER_DISCONNECT = "allow_user_disconnect"
        const val AUTO_RECONNECT = "auto_reconnect"
        const val VPN_USERNAME = "vpn_username"
        const val VPN_PASSWORD = "vpn_password"
        const val LOG_LEVEL = "log_level"
    }

    object AdminColumns {
        const val ADMIN_COMPONENT = "admin_component"
    }
}
