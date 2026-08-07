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
        } catch (e: Exception) {
        // Logged, not swallowed. A denied query and an absent launcher are indistinguishable at
        // this call site, and both end as "no configuration" — which is how the missing
        // PUSH_CONFIG permission stayed invisible for a whole release cycle. One line in logcat is
        // the difference between "the integration is off" and "the integration is broken".
            Log.w(TAG, "Could not read the admin component from ${SargoVpnContract.ADMIN_URI}: ${e.javaClass.simpleName}: ${e.message}")
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
        const val TAG = "SargoVpn"
    }
}
