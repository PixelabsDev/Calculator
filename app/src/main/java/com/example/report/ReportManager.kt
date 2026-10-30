package com.example.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class UserReport(
    val category: String,
    val title: String,
    val description: String,
    val email: String,
    val appVersion: String = "18.0",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val isRadMode: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ReportSubmitState {
    object Idle : ReportSubmitState()
    object Submitting : ReportSubmitState()
    data class Success(val message: String) : ReportSubmitState()
    data class Error(val error: String) : ReportSubmitState()
}

class ReportManager(private val context: Context) {

    companion object {
        // Securely hardcoded backend webhook endpoint - users cannot view or modify this
        private const val BACKEND_WEBHOOK_URL =
            "https://script.google.com/macros/s/AKfycbwe1elw5cNmY2zoa1LMmO3D5bEIhnfVE6MpbDk1wc6iePfiQ4Mh_YeNQYAye2TNsune/exec"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _submitState = MutableStateFlow<ReportSubmitState>(ReportSubmitState.Idle)
    val submitState: StateFlow<ReportSubmitState> = _submitState.asStateFlow()

    fun resetSubmitState() {
        _submitState.value = ReportSubmitState.Idle
    }

    suspend fun submitReport(report: UserReport): Boolean {
        _submitState.value = ReportSubmitState.Submitting
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(report.timestamp)))
                    put("category", report.category)
                    put("title", report.title)
                    put("description", report.description)
                    put("email", report.email.ifBlank { "Anonymous" })
                    put("deviceModel", report.deviceModel)
                    put("androidVersion", report.androidVersion)
                    put("appVersion", report.appVersion)
                    put("angleMode", if (report.isRadMode) "RAD" else "DEG")
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(BACKEND_WEBHOOK_URL)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val isSuccessful = response.isSuccessful
                response.close()

                if (isSuccessful) {
                    _submitState.value = ReportSubmitState.Success(
                        "Thank you! Your feedback has been received."
                    )
                    true
                } else {
                    _submitState.value = ReportSubmitState.Error(
                        "Server returned HTTP ${response.code}. Please try again or use Email."
                    )
                    false
                }
            } catch (e: Exception) {
                _submitState.value = ReportSubmitState.Error(
                    e.localizedMessage ?: "Network error. Please check your connection or use Email."
                )
                false
            }
        }
    }

    fun createEmailIntent(report: UserReport): Intent {
        val subject = "[Calculator ${report.category}] ${report.title.ifBlank { "User Feedback" }}"
        val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(report.timestamp))
        val body = buildString {
            appendLine("=== USER REPORT & FEEDBACK ===")
            appendLine("Category: ${report.category}")
            appendLine("Title: ${report.title}")
            appendLine("User Contact: ${report.email.ifBlank { "Not provided" }}")
            appendLine()
            appendLine("=== DESCRIPTION ===")
            appendLine(report.description)
            appendLine()
            appendLine("=== SYSTEM DIAGNOSTICS (No DB) ===")
            appendLine("App: Calculator (pixelabs.in)")
            appendLine("App Version: ${report.appVersion}")
            appendLine("Device: ${report.deviceModel}")
            appendLine("Android: ${report.androidVersion}")
            appendLine("Mode: ${if (report.isRadMode) "Radians (RAD)" else "Degrees (DEG)"}")
            appendLine("Timestamp: $timeFormatted")
        }

        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@pixelabs.in")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getFormattedReportText(report: UserReport): String {
        val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(report.timestamp))
        return buildString {
            appendLine("Category: ${report.category}")
            appendLine("Title: ${report.title}")
            appendLine("Description: ${report.description}")
            appendLine("Contact: ${report.email.ifBlank { "Anonymous" }}")
            appendLine("Device: ${report.deviceModel} | ${report.androidVersion}")
            appendLine("App Version: ${report.appVersion} | Mode: ${if (report.isRadMode) "RAD" else "DEG"}")
            appendLine("Time: $timeFormatted")
        }
    }
}
