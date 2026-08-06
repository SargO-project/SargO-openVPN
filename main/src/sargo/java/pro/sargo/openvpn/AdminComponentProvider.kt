package pro.sargo.openvpn

import android.content.ComponentName
import android.content.Context

/**
 * Reads the SargO launcher device-admin component name from the launcher
 * ContentProvider. Like [SargoVpnConfigProvider], this avoids any compile-time
 * dependency on launcher internals or the GPL-licensed OpenVPN code.
 */
class AdminComponentProvider(private val context: Context) {

    fun getAdminComponent(): ComponentName? {
        val cursor = try {
            context.contentResolver.query(SargoVpnContract.ADMIN_URI, null, null, null, null)
        } catch (e: Exception) {
            null
        } ?: return null

        cursor.use {
            if (!it.moveToFirst()) return null
            val index = it.getColumnIndex(SargoVpnContract.AdminColumns.ADMIN_COMPONENT)
            if (index < 0) return null
            val flat = it.getString(index) ?: return null
            return ComponentName.unflattenFromString(flat)
        }
    }
}
