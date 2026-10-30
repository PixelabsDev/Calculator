package com.example.report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthState
import com.example.calculator.CalculatorViewModel

@Composable
fun ReportIssueDialog(
    viewModel: CalculatorViewModel,
    authState: AuthState,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val submitState by viewModel.reportSubmitState.collectAsState()

    val categories = listOf("🐛 Bug", "🧮 Calculation", "💡 Suggestion", "💬 Feedback")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val defaultEmail = (authState as? AuthState.SignedIn)?.user?.email ?: ""
    var email by remember { mutableStateOf(defaultEmail) }

    var showDiagnostics by remember { mutableStateOf(false) }

    val cardBg = if (isDark) Color(0xFF1F1B27) else Color(0xFFFAF7FD)
    val inputBorder = if (isDark) Color(0xFF4A4456) else Color(0xFFC7BED4)
    val textColor = if (isDark) Color(0xFFEBE4F5) else Color(0xFF1E1A26)
    val subtextColor = if (isDark) Color(0xFFA59EB5) else Color(0xFF6E6879)
    val accentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)

    AlertDialog(
        onDismissRequest = {
            viewModel.resetReportSubmitState()
            onDismiss()
        },
        modifier = Modifier.testTag("dialog_report_issue"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📢", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Report & Feedback",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Direct Google Sheet / Email • No Database",
                        style = MaterialTheme.typography.labelSmall.copy(color = subtextColor)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Selector
                Text(
                    text = "Report Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = textColor)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor else (if (isDark) Color(0xFF2B2636) else Color(0xFFEAE3F2)))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (isDark) Color(0xFF381E72) else Color.White) else textColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Short Summary / Title") },
                    placeholder = { Text("e.g. Percentage calculation formula") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = inputBorder
                    )
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Issue or Feedback Details") },
                    placeholder = { Text("Describe what happened or what you'd like improved...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = inputBorder
                    )
                )

                // User Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Your Email (optional, for reply)") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = inputBorder
                    )
                )

                // Status Message Box (Submitting, Success, Error)
                when (val state = submitState) {
                    is ReportSubmitState.Submitting -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Submitting report...",
                                style = MaterialTheme.typography.bodySmall.copy(color = textColor)
                            )
                        }
                    }
                    is ReportSubmitState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ " + state.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    is ReportSubmitState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ " + state.error,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFFE57373) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💡 Tip: You can tap 'Send via Email' below to deliver your report immediately.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = subtextColor)
                                )
                            }
                        }
                    }
                    ReportSubmitState.Idle -> { /* Nothing */ }
                }

                // Diagnostics Preview Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDiagnostics = !showDiagnostics }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱 Automatic System Diagnostics",
                        style = MaterialTheme.typography.labelSmall.copy(color = subtextColor, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (showDiagnostics) "Hide ▲" else "View ▼",
                        style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontWeight = FontWeight.Bold)
                    )
                }

                AnimatedVisibility(visible = showDiagnostics) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• App Version: 18.0\n• OS: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n• Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n• Angle Mode: ${if (viewModel.isRadMode.collectAsState().value) "RAD" else "DEG"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = subtextColor,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isBlank()) {
                        Toast.makeText(context, "Please enter some details first", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.submitReport(
                            category = selectedCategory,
                            title = title.ifBlank { "User Feedback" },
                            description = description,
                            email = email
                        )
                    }
                },
                enabled = submitState !is ReportSubmitState.Submitting,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (isDark) Color(0xFF381E72) else Color.White
                ),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text(if (submitState is ReportSubmitState.Submitting) "Submitting..." else "Submit Report")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val intent = viewModel.createEmailIntent(
                            category = selectedCategory,
                            title = title.ifBlank { "User Feedback" },
                            description = description.ifBlank { "(No details entered)" },
                            email = email
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("email_report_button")
                ) {
                    Text("✉️ Email")
                }

                TextButton(
                    onClick = {
                        val text = viewModel.getFormattedReportText(
                            category = selectedCategory,
                            title = title.ifBlank { "User Feedback" },
                            description = description.ifBlank { "(No details entered)" },
                            email = email
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Calculator Report", text))
                        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("📋 Copy")
                }
            }
        }
    )
}
