package com.example.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ReportSubmitState {
    object Idle : ReportSubmitState
    object Submitting : ReportSubmitState
    data class Success(val message: String) : ReportSubmitState
    data class Error(val error: String) : ReportSubmitState
}

data class UserReport(
    val category: String,
    val title: String,
    val description: String,
    val email: String = "",
    val includeDiagnostics: Boolean = false,
    val appVersion: String = "19.0",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val isRadMode: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class ReportManager(private val context: Context) {

    companion object {
        const val DEVELOPER_EMAIL = "hello@pixelabs.in"
        const val WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbwe1eIw5cNmY2zoa1LMmO3D5bElhnfVE6MpbDk1wc6iePfiQ4Mh_YeNQYAye2TNsune/exec"
    }

    suspend fun submitReportToSheet(report: UserReport): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val jsonPayload = JSONObject().apply {
                put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(report.timestamp)))
                put("category", report.category)
                put("title", report.title.ifBlank { "User Feedback" })
                put("description", report.description)
                put("email", report.email)
                put("deviceModel", if (report.includeDiagnostics) report.deviceModel else "N/A (Omitted by user)")
                put("androidVersion", if (report.includeDiagnostics) report.androidVersion else "N/A (Omitted by user)")
                put("appVersion", report.appVersion)
                put("angleMode", if (report.includeDiagnostics) (if (report.isRadMode) "RAD" else "DEG") else "N/A")
            }.toString()

            val url = URL(WEBHOOK_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            var responseCode = connection.responseCode
            // Follow 302/301/303/307 redirect from Google Apps Script Web App
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307
            ) {
                val redirectLocation = connection.getHeaderField("Location")
                connection.disconnect()

                if (!redirectLocation.isNullOrEmpty()) {
                    connection = (URL(redirectLocation).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                    responseCode = connection.responseCode
                }
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            if (responseCode in 200..299 && responseBody.contains("\"status\":\"success\"")) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned status ($responseCode). Please try again or use Email."))
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Unable to connect to Google service. Please check your internet connection or Private DNS, or use Email / Copy below."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Please check your network and try again, or use Email."))
        } catch (e: ConnectException) {
            Result.failure(Exception("Connection failed. Please check your internet connection or use Email."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection or send via Email."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unexpected error occurred. Please use Email or Copy."))
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    fun createEmailIntent(report: UserReport): Intent {
        val subject = "[Calculator Feedback] ${report.category} - ${report.title.ifBlank { "Feedback" }}"
        val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(report.timestamp))
        val body = buildString {
            appendLine("=== USER FEEDBACK ===")
            appendLine("Type: ${report.category}")
            appendLine("Subject: ${report.title.ifBlank { "Feedback" }}")
            if (report.email.isNotBlank()) {
                appendLine("Contact: ${report.email}")
            }
            appendLine()
            appendLine("=== DETAILS ===")
            appendLine(report.description.ifBlank { "(No details entered)" })
            appendLine()
            if (report.includeDiagnostics) {
                appendLine("=== SYSTEM DIAGNOSTICS (With User Consent) ===")
                appendLine("App: Calculator")
                appendLine("App Version: ${report.appVersion}")
                appendLine("Device: ${report.deviceModel}")
                appendLine("Android: ${report.androidVersion}")
                appendLine("Angle Mode: ${if (report.isRadMode) "Radians (RAD)" else "Degrees (DEG)"}")
                appendLine("Timestamp: $timeFormatted")
            } else {
                appendLine("System Diagnostics: Omitted by user choice")
            }
        }

        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$DEVELOPER_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getFormattedReportText(report: UserReport): String {
        val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(report.timestamp))
        return buildString {
            appendLine("Type: ${report.category}")
            appendLine("Subject: ${report.title.ifBlank { "Feedback" }}")
            if (report.email.isNotBlank()) {
                appendLine("Contact: ${report.email}")
            }
            appendLine("Details: ${report.description.ifBlank { "(No details entered)" }}")
            if (report.includeDiagnostics) {
                appendLine("Device: ${report.deviceModel} | ${report.androidVersion}")
                appendLine("App Version: ${report.appVersion} | Mode: ${if (report.isRadMode) "RAD" else "DEG"}")
                appendLine("Time: $timeFormatted")
            }
        }
    }
}
