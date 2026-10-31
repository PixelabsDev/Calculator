package com.example.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
