package com.example.report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calculator.CalculatorViewModel

@Composable
fun ReportIssueDialog(
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val submitState by viewModel.reportSubmitState.collectAsState()

    val categories = listOf("🐛 Bug Report", "🧮 Calculation", "💡 Suggestion", "💬 Feedback")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var includeDiagnostics by remember { mutableStateOf(false) }
    var userConsentAgreed by remember { mutableStateOf(false) }

    val dialogBg = if (isDark) Color(0xFF231E2B) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF1F1B27) else Color(0xFFFAF7FD)
    val inputBorder = if (isDark) Color(0xFF4A4456) else Color(0xFFC7BED4)
    val textColor = if (isDark) Color(0xFFEBE4F5) else Color(0xFF1E1A26)
    val subtextColor = if (isDark) Color(0xFFA59EB5) else Color(0xFF6E6879)
    val accentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val chipSelectedBg = if (isDark) Color(0xFF4A3978) else Color(0xFFE8DEF8)
    val chipUnselectedBg = if (isDark) Color(0xFF2B2636) else Color(0xFFF3EDF7)

    fun handleSubmit() {
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
    }

    fun handleEmail() {
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
    }

    fun handleCopy() {
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
    }

    Dialog(
        onDismissRequest = {
            viewModel.resetReportSubmitState()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("dialog_report_issue")
                .widthIn(max = if (isLandscape) 740.dp else 440.dp)
                .fillMaxWidth(if (isLandscape) 0.88f else 0.92f)
                .heightIn(max = if (isLandscape) 375.dp else 680.dp)
                .padding(vertical = if (isLandscape) 8.dp else 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header (Icon, Title, Subtitle, Close Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💬", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "Send Feedback",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "Help us improve Pixelabs Calculator",
                                style = MaterialTheme.typography.labelSmall.copy(color = subtextColor)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.resetReportSubmitState()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = subtextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = inputBorder.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Body Content
                if (submitState is ReportSubmitState.Success) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E3A24) else Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✅", fontSize = 26.sp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.resetReportSubmitState()
                                onDismiss()
                            },
                            modifier = Modifier
                                .widthIn(min = 140.dp)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = if (isDark) Color(0xFF381E72) else Color.White
                            )
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (isLandscape) {
                    // Landscape Mode: 2-Column Balanced Architecture
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column (weight 1f): Categories, Title, Email
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (submitState is ReportSubmitState.Error) {
                                ErrorCard(
                                    errorMessage = (submitState as ReportSubmitState.Error).error,
                                    isDark = isDark
                                )
                            }

                            Text(
                                text = "Feedback Type",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            )
                            CategoryChipGrid(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onSelect = { selectedCategory = it },
                                accentColor = accentColor,
                                chipSelectedBg = chipSelectedBg,
                                chipUnselectedBg = chipUnselectedBg,
                                inputBorder = inputBorder,
                                textColor = textColor,
                                isDark = isDark,
                                compact = true
                            )

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Subject / Summary") },
                                placeholder = { Text("e.g. Calculation issue") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("report_title_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = inputBorder
                                )
                            )

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
                                label = { Text("Your Email (optional)") },
                                placeholder = { Text("you@domain.com") },
                                singleLine = true,
                                isError = emailError != null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                supportingText = {
                                    if (emailError != null) {
                                        Text(
                                            text = emailError ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
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
                        }

                        // Right Column (weight 1f): Description, Checkboxes, Action Buttons
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { includeDiagnostics = !includeDiagnostics }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = includeDiagnostics,
                                    onCheckedChange = { includeDiagnostics = it },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Include system diagnostics (OS & model)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = textColor, fontSize = 11.sp)
                                )
                            }

                            AnimatedVisibility(visible = includeDiagnostics) {
                                DiagnosticsCard(
                                    isRadMode = viewModel.isRadMode.collectAsState().value,
                                    cardBg = cardBg,
                                    accentColor = accentColor,
                                    subtextColor = subtextColor
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { userConsentAgreed = !userConsentAgreed }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = userConsentAgreed,
                                    onCheckedChange = { userConsentAgreed = it },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "I agree to share feedback per Privacy Policy.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColor,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }

                            ActionButtons(
                                submitState = submitState,
                                userConsentAgreed = userConsentAgreed,
                                accentColor = accentColor,
                                isDark = isDark,
                                onSubmit = { handleSubmit() },
                                onEmail = { handleEmail() },
                                onCopy = { handleCopy() }
                            )
                        }
                    }
                } else {
                    // Portrait Mode: Single Continuous Vertical Scrollable Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (submitState is ReportSubmitState.Error) {
                            ErrorCard(
                                errorMessage = (submitState as ReportSubmitState.Error).error,
                                isDark = isDark
                            )
                        }

                        Text(
                            text = "Feedback Type",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        )
                        CategoryChipGrid(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelect = { selectedCategory = it },
                            accentColor = accentColor,
                            chipSelectedBg = chipSelectedBg,
                            chipUnselectedBg = chipUnselectedBg,
                            inputBorder = inputBorder,
                            textColor = textColor,
                            isDark = isDark,
                            compact = false
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Subject / Summary") },
                            placeholder = { Text("e.g. Percentage calculation formula") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_title_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = inputBorder
                            )
                        )

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

                        AnimatedVisibility(visible = includeDiagnostics) {
                            DiagnosticsCard(
                                isRadMode = viewModel.isRadMode.collectAsState().value,
                                cardBg = cardBg,
                                accentColor = accentColor,
                                subtextColor = subtextColor
                            )
                        }

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

                        ActionButtons(
                            submitState = submitState,
                            userConsentAgreed = userConsentAgreed,
                            accentColor = accentColor,
                            isDark = isDark,
                            onSubmit = { handleSubmit() },
                            onEmail = { handleEmail() },
                            onCopy = { handleCopy() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF3E2723) else Color(0xFFFFEBEE)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚠️", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFFFFCCBC) else Color(0xFFC62828),
                    fontSize = 11.5.sp
                )
            )
        }
    }
}

@Composable
private fun CategoryChipGrid(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
    accentColor: Color,
    chipSelectedBg: Color,
    chipUnselectedBg: Color,
    inputBorder: Color,
    textColor: Color,
    isDark: Boolean,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0..1) {
                val cat = categories[i]
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) chipSelectedBg else chipUnselectedBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) accentColor else inputBorder.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(cat) }
                        .padding(vertical = if (compact) 7.dp else 9.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) (if (isDark) Color(0xFFF3EDFF) else Color(0xFF381E72)) else textColor,
                            fontSize = if (compact) 11.sp else 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 2..3) {
                val cat = categories[i]
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) chipSelectedBg else chipUnselectedBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) accentColor else inputBorder.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(cat) }
                        .padding(vertical = if (compact) 7.dp else 9.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) (if (isDark) Color(0xFFF3EDFF) else Color(0xFF381E72)) else textColor,
                            fontSize = if (compact) 11.sp else 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(
    isRadMode: Boolean,
    cardBg: Color,
    accentColor: Color,
    subtextColor: Color
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Diagnostics to be attached:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 11.sp
                )
            )
            Text(
                text = "• App Version: 19.0\n• OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n• Device: ${Build.MANUFACTURER} ${Build.MODEL}\n• Angle Mode: ${if (isRadMode) "RAD" else "DEG"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = subtextColor,
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp
                )
            )
        }
    }
}

@Composable
private fun ActionButtons(
    submitState: ReportSubmitState,
    userConsentAgreed: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onSubmit: () -> Unit,
    onEmail: () -> Unit,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSubmit,
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
                .height(44.dp)
                .testTag("submit_report_button")
        ) {
            if (submitState is ReportSubmitState.Submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = if (isDark) Color(0xFF381E72) else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Text("Submit Feedback", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEmail,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("✉️ Email", maxLines = 1, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("📋 Copy", maxLines = 1, fontSize = 12.sp)
            }
        }
    }
}
