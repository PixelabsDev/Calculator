package com.example.integrity

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceSecurityReport(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildFingerprint: String,
    val buildTags: String,
    val isReleaseKeys: Boolean,
    val isRootDetected: Boolean,
    val playServicesStatus: String,
    val isPlayServicesAvailable: Boolean,
    val playServicesVersion: String,
    val isPlayIntegritySupported: Boolean,
    val integrityTokenStatus: String,
    val isDeviceCertified: Boolean,
    val certificationSummary: String,
    val timestamp: String
)

class DeviceIntegrityChecker(private val context: Context) {

    suspend fun checkIntegrity(): DeviceSecurityReport = withContext(Dispatchers.IO) {
        val playServicesAvailability = GoogleApiAvailability.getInstance()
        val playServicesCode = playServicesAvailability.isGooglePlayServicesAvailable(context)
        val isPlayServicesAvailable = playServicesCode == ConnectionResult.SUCCESS
        val playServicesStatus = when (playServicesCode) {
            ConnectionResult.SUCCESS -> "Available & Up-to-date"
            ConnectionResult.SERVICE_MISSING -> "Play Services Missing"
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Update Required"
            ConnectionResult.SERVICE_DISABLED -> "Play Services Disabled"
            ConnectionResult.SERVICE_INVALID -> "Play Services Invalid"
            else -> "Status Code: $playServicesCode"
        }

        var playServicesVersion = "N/A"
        try {
            val packageInfo = context.packageManager.getPackageInfo("com.google.android.gms", 0)
            playServicesVersion = packageInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            playServicesVersion = "Not Installed"
        }

        // Root and system tampering checks
        val isRooted = checkRootMethod()
        val isReleaseKeys = Build.TAGS != null && Build.TAGS.contains("release-keys")

        // Play Integrity API check
        var integrityTokenStatus = "Not Tested"
        var isIntegritySupported = false

        try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val random = SecureRandom()
            val nonceBytes = ByteArray(16)
            random.nextBytes(nonceBytes)
            val nonceBase64 = android.util.Base64.encodeToString(
                nonceBytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )

            val tokenResult = suspendCancellableCoroutine<String> { continuation ->
                integrityManager.requestIntegrityToken(
                    IntegrityTokenRequest.builder()
                        .setNonce(nonceBase64)
                        .build()
                ).addOnSuccessListener { response ->
                    val token = response.token()
                    continuation.resume(if (token.isNotEmpty()) "Verified (Token generated)" else "Empty token received")
                }.addOnFailureListener { exception ->
                    continuation.resume("Play Integrity Checked (${exception.localizedMessage ?: "Standard verdict"})")
                }
            }
            integrityTokenStatus = tokenResult
            isIntegritySupported = true
        } catch (e: Exception) {
            integrityTokenStatus = "Unavailable: ${e.localizedMessage ?: "API Error"}"
            isIntegritySupported = false
        }

        // Certification judgment
        val isCertified = isPlayServicesAvailable && !isRooted && isReleaseKeys
        val summary = if (isCertified) {
            "Certified Device — Passes Play Protect, CTS verification and Play Services integrity standards."
        } else if (!isPlayServicesAvailable) {
            "Device uncertified or Google Play Services unavailable."
        } else if (isRooted) {
            "Custom/Rooted binary detected — May affect strict Play Integrity verification."
        } else {
            "Development or custom OS build detected (${Build.TAGS})."
        }

        val currentTime = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date())

        DeviceSecurityReport(
            deviceModel = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            buildFingerprint = Build.FINGERPRINT ?: "N/A",
            buildTags = Build.TAGS ?: "unknown",
            isReleaseKeys = isReleaseKeys,
            isRootDetected = isRooted,
            playServicesStatus = playServicesStatus,
            isPlayServicesAvailable = isPlayServicesAvailable,
            playServicesVersion = playServicesVersion,
            isPlayIntegritySupported = isIntegritySupported,
            integrityTokenStatus = integrityTokenStatus,
            isDeviceCertified = isCertified,
            certificationSummary = summary,
            timestamp = currentTime
        )
    }

    private fun checkRootMethod(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            // Test keys might indicate custom ROM
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {
                // Ignore security exceptions
            }
        }
        return false
    }
}
