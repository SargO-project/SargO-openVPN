package pro.sargo.openvpn

/**
 * Parsed SargO remote VPN configuration.
 *
 * @property vpnName Display name for the imported profile (required).
 * @property vpnConfig Path/URI to the .ovpn file, or the inline config content.
 * @property vpnConfigContent Optional inline .ovpn content. If provided, takes precedence over [vpnConfig].
 * @property connect Whether to start the VPN immediately after import.
 * @property alwaysOn Whether to set this profile as always-on VPN (null = leave unchanged).
 * @property remove Comma-separated list of existing profile names to remove.
 * @property removeAll Whether to remove all other VPN profiles before importing.
 * @property disconnectOnConfigChange Whether to disconnect the active VPN before applying a new configuration.
 * @property allowUserDisconnect Whether the user is allowed to manually disconnect the VPN (null = leave unchanged).
 * @property autoReconnect Whether to automatically reconnect after a network change or disconnect.
 * @property vpnUsername Optional username for OpenVPN user-password authentication.
 * @property vpnPassword Optional password for OpenVPN user-password authentication.
 * @property logLevel Optional OpenVPN log verbosity level (verb 0..11). Null leaves the config unchanged.
 */
data class SargoVpnConfig(
    val vpnName: String,
    val vpnConfig: String,
    val vpnConfigContent: String = "",
    val connect: Boolean = false,
    val alwaysOn: Boolean? = null,
    val remove: String = "",
    val removeAll: Boolean = false,
    val disconnectOnConfigChange: Boolean = false,
    val allowUserDisconnect: Boolean? = null,
    val autoReconnect: Boolean = false,
    val vpnUsername: String? = null,
    val vpnPassword: String? = null,
    val logLevel: Int? = null
) {
    companion object {
        private const val MAX_CONFIG_LENGTH_CHARS = 1_048_576
        private const val ALLOWED_CONTENT_AUTHORITY = "pro.sargo.launcher"

        /**
         * Build a [SargoVpnConfig] from raw SargO app preference strings.
         * Returns null when the configuration is incomplete or invalid.
         *
         * A configuration is considered valid when [vpnName] is non-blank and at least one of
         * [vpnConfig] or [vpnConfigContent] is non-blank.
         *
         * Additional safety checks:
         * - config strings may not exceed [MAX_CONFIG_LENGTH_CHARS];
         * - content:// URIs are restricted to the SargO launcher authority.
         */
        fun fromPreferences(
            vpnName: String?,
            vpnConfig: String?,
            vpnConfigContent: String? = null,
            connect: String?,
            alwaysOn: String?,
            remove: String?,
            removeAll: String?,
            disconnectOnConfigChange: String? = null,
            allowUserDisconnect: String? = null,
            autoReconnect: String? = null,
            vpnUsername: String? = null,
            vpnPassword: String? = null,
            logLevel: String? = null
        ): SargoVpnConfig? {
            val trimmedName = vpnName?.trim()
            val trimmedConfig = vpnConfig?.trim() ?: ""
            val trimmedConfigContent = vpnConfigContent?.trim() ?: ""

            if (trimmedName.isNullOrBlank() || (trimmedConfig.isBlank() && trimmedConfigContent.isBlank())) {
                return null
            }

            if (trimmedConfig.length > MAX_CONFIG_LENGTH_CHARS ||
                trimmedConfigContent.length > MAX_CONFIG_LENGTH_CHARS
            ) {
                return null
            }

            // When inline content is present it takes precedence, so vpnConfig is ignored.
            // Otherwise vpnConfig must not be a network/file URI and, if it is a content://
            // URI, it must point to the SargO launcher authority.
            if (trimmedConfigContent.isBlank() &&
                trimmedConfig.isNotBlank() &&
                !isAllowedConfigSource(trimmedConfig)
            ) {
                return null
            }

            return SargoVpnConfig(
                vpnName = trimmedName,
                vpnConfig = trimmedConfig,
                vpnConfigContent = trimmedConfigContent,
                connect = "1" == connect?.trim(),
                alwaysOn = when (alwaysOn?.trim()) {
                    "1" -> true
                    "0" -> false
                    else -> null
                },
                remove = remove?.trim() ?: "",
                removeAll = "1" == removeAll?.trim(),
                disconnectOnConfigChange = "1" == disconnectOnConfigChange?.trim(),
                allowUserDisconnect = when (allowUserDisconnect?.trim()) {
                    "1" -> true
                    "0" -> false
                    else -> null
                },
                autoReconnect = "1" == autoReconnect?.trim(),
                vpnUsername = vpnUsername?.trim()?.takeIf { it.isNotBlank() },
                vpnPassword = vpnPassword?.trim()?.takeIf { it.isNotBlank() },
                logLevel = logLevel?.trim()?.toIntOrNull()?.takeIf { it in 0..11 }
            )
        }

        private fun isAllowedConfigSource(config: String): Boolean {
            val lower = config.lowercase()
            // Disallow network and local file schemes that could pull configs from untrusted sources.
            if (lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("file://")
            ) {
                return false
            }
            if (!lower.startsWith("content://")) {
                // Treat as inline configuration content.
                return true
            }
            val after = config.removePrefix("content://").removePrefix("CONTENT://")
            return after.startsWith("$ALLOWED_CONTENT_AUTHORITY/", ignoreCase = true) ||
                    after.equals(ALLOWED_CONTENT_AUTHORITY, ignoreCase = true)
        }
    }
}
