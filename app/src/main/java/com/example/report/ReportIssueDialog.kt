package com.example.report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.CalculatorViewModel

@Composable
fun ReportIssueDialog(
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val submitState by viewModel.reportSubmitState.collectAsState()

    val categories = listOf("🐛 Bug Report", "🧮 Calculation", "💡 Suggestion", "💬 Feedback")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var includeDiagnostics by remember { mutableStateOf(false) }
    var userConsentAgreed by remember { mutableStateOf(false) }

    val cardBg = if (isDark) Color(0xFF1F1B27) else Color(0xFFFAF7FD)
    val inputBorder = if (isDark) Color(0xFF4A4456) else Color(0xFFC7BED4)
    val textColor = if (isDark) Color(0xFFEBE4F5) else Color(0xFF1E1A26)
    val subtextColor = if (isDark) Color(0xFFA59EB5) else Color(0xFF6E6879)
    val accentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val chipSelectedBg = if (isDark) Color(0xFF4A3978) else Color(0xFFE8DEF8)
    val chipUnselectedBg = if (isDark) Color(0xFF2B2636) else Color(0xFFF3EDF7)

    AlertDialog(
        onDismissRequest = {
            viewModel.resetReportSubmitState()
            onDismiss()
        },
        modifier = Modifier.testTag("dialog_report_issue"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💬", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Help us improve the app",
                            style = MaterialTheme.typography.labelSmall.copy(color = subtextColor)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        viewModel.resetReportSubmitState()
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = subtextColor
                    )
                }
            }
        },
        text = {
            if (submitState is ReportSubmitState.Success) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E3A24) else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✅", fontSize = 28.sp)
                    }
                    Text(
                        text = "Feedback Recorded!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        )
                    )
                    Text(
                        text = (submitState as ReportSubmitState.Success).message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Error alert if submission failed
                    if (submitState is ReportSubmitState.Error) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF3E2723) else Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (submitState as ReportSubmitState.Error).error,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFFFFCCBC) else Color(0xFFC62828),
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }

                    // Category Selector (2x2 grid to prevent text truncation/wrapping)
                    Text(
                        text = "Feedback Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = textColor)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Bug Report & Calculation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0..1) {
                                val cat = categories[i]
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) chipSelectedBg else chipUnselectedBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) accentColor else inputBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) (if (isDark) Color(0xFFF3EDFF) else Color(0xFF381E72)) else textColor,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Row 2: Suggestion & Feedback
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 2..3) {
                                val cat = categories[i]
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) chipSelectedBg else chipUnselectedBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) accentColor else inputBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) (if (isDark) Color(0xFFF3EDFF) else Color(0xFF381E72)) else textColor,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Subject / Summary") },
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
                        label = { Text("Details & Description") },
                        placeholder = { Text("Describe the issue, formula, or suggestion...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_desc_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = inputBorder
                        )
                    )

                    // Optional User Email Input with client-side validation
                    OutlinedTextField(
                        value = email,
                        onValueChange = { input ->
                            email = input
                            if (emailError != null) {
                                emailError = if (input.isNotBlank() && !EmailValidator.isValid(input)) {
                                    "Please enter a valid email address (e.g. name@domain.com)"
                                } else {
                                    null
                                }
                            }
                        },
                        label = { Text("Your Email (optional, for follow-up)") },
                        placeholder = { Text("you@domain.com") },
                        singleLine = true,
                        isError = emailError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        supportingText = {
                            if (emailError != null) {
                                Text(
                                    text = emailError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    modifier = Modifier.testTag("report_email_error")
                                )
                            }
                        },
                        trailingIcon = {
                            if (emailError != null) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Invalid email format",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_email_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = inputBorder,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            errorTrailingIconColor = MaterialTheme.colorScheme.error
                        )
                    )

                    // Privacy & Consent Checkbox for Diagnostics (Optional)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { includeDiagnostics = !includeDiagnostics }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeDiagnostics,
                            onCheckedChange = { includeDiagnostics = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Include system diagnostics (Android version & device model) to help debug",
                            style = MaterialTheme.typography.bodySmall.copy(color = textColor, fontSize = 11.5.sp)
                        )
                    }

                    // Diagnostics Preview Card (Visible only when consent is granted)
                    AnimatedVisibility(visible = includeDiagnostics) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Diagnostics to be attached:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = accentColor)
                                )
                                Text(
                                    text = "• App Version: 19.0\n• OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n• Device: ${Build.MANUFACTURER} ${Build.MODEL}\n• Angle Mode: ${if (viewModel.isRadMode.collectAsState().value) "RAD" else "DEG"}",
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

                    // Mandatory User Consent Checkbox (Required for Play Store offline privacy compliance)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { userConsentAgreed = !userConsentAgreed }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = userConsentAgreed,
                            onCheckedChange = { userConsentAgreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I voluntarily agree to share this feedback to help improve the application in accordance with our Privacy Policy & Terms.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = textColor,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (submitState is ReportSubmitState.Success) {
                Button(
                    onClick = {
                        viewModel.resetReportSubmitState()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDark) Color(0xFF381E72) else Color.White
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Action: Submit Feedback (Disabled until user explicitly grants consent)
                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            if (description.isBlank()) {
                                Toast.makeText(context, "Please enter some details first", Toast.LENGTH_SHORT).show()
                            } else if (trimmedEmail.isNotEmpty() && !EmailValidator.isValid(trimmedEmail)) {
                                emailError = "Please enter a valid email address (e.g. name@domain.com)"
                                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                            } else {
                                emailError = null
                                viewModel.submitReportToSheet(
                                    category = selectedCategory,
                                    title = title.ifBlank { "User Feedback" },
                                    description = description,
                                    email = trimmedEmail,
                                    includeDiagnostics = includeDiagnostics
                                )
                            }
                        },
                        enabled = userConsentAgreed && submitState !is ReportSubmitState.Submitting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isDark) Color(0xFF381E72) else Color.White,
                            disabledContainerColor = if (isDark) Color(0xFF3A3445) else Color(0xFFE0DAE8),
                            disabledContentColor = if (isDark) Color(0xFF888099) else Color(0xFF9E96A8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("submit_report_button")
                    ) {
                        if (submitState is ReportSubmitState.Submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) Color(0xFF381E72) else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submitting...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Submit Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Secondary Actions Row: Email and Copy (50/50 split, zero overflow)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val trimmedEmail = email.trim()
                                if (trimmedEmail.isNotEmpty() && !EmailValidator.isValid(trimmedEmail)) {
                                    emailError = "Please enter a valid email address (e.g. name@domain.com)"
                                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                } else {
                                    emailError = null
                                    val intent = viewModel.createEmailIntent(
                                        category = selectedCategory,
                                        title = title.ifBlank { "User Feedback" },
                                        description = description,
                                        email = trimmedEmail,
                                        includeDiagnostics = includeDiagnostics
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No email client found. Use 'Copy' instead.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("✉️ Email", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val trimmedEmail = email.trim()
                                if (trimmedEmail.isNotEmpty() && !EmailValidator.isValid(trimmedEmail)) {
                                    emailError = "Please enter a valid email address (e.g. name@domain.com)"
                                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                } else {
                                    emailError = null
                                    val text = viewModel.getFormattedReportText(
                                        category = selectedCategory,
                                        title = title.ifBlank { "User Feedback" },
                                        description = description.ifBlank { "(No details entered)" },
                                        email = trimmedEmail,
                                        includeDiagnostics = includeDiagnostics
                                    )
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Calculator Feedback", text))
                                    Toast.makeText(context, "Feedback details copied!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📋 Copy", maxLines = 1)
                        }
                    }
                }
            }
        },
        dismissButton = null
    )
}
