package pro.sargo.openvpn

import android.content.ComponentName
import android.content.Context
import android.util.Log

/**
 * Reads the SargO launcher device-admin component name from the launcher
 * ContentProvider. Like [SargoVpnConfigProvider], this avoids any compile-time
 * dependency on launcher internals or the GPL-licensed OpenVPN code.
 */
class AdminComponentProvider(private val context: Context) {

    fun getAdminComponent(): ComponentName? {
        val cursor = try {
            context.contentResolver.query(SargoVpnContract.ADMIN_URI, null, null, null, null)
        } catch (e: SecurityException) {
            // Same missing declaration as in [SargoVpnConfigProvider]; see the note there. Without
            // the admin component, always-on VPN lockdown is never applied and nothing says so.
            Log.e(TAG, "Refused by the launcher's provider. This app must declare " +
                    "<uses-permission android:name=\"pro.sargo.permission.READ_VPN_CONFIG\" /> " +
                    "and be signed with the launcher's key.", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the launcher's device-admin component", e)
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

    private companion object {
        const val TAG = "SargoAdminComponent"
    }
}
