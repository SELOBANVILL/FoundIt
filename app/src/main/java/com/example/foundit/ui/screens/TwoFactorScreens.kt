package com.example.foundit.ui.screens

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foundit.security.Validators
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.OtpField
import com.example.foundit.ui.components.QrCode

/**
 * Sign-up step 2: show the TOTP QR code, then confirm the first 6-digit code.
 * FLAG_SECURE blocks screenshots/screen recording while the secret is visible.
 */
@Composable
fun TwoFactorSetupScreen(vm: AppViewModel) {
    SecureWindow()
    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (vm.enrollment == null) vm.startEnrollment() }

    TwoFactorScaffold(
        icon = Icons.Filled.Security,
        title = "Set up two-step verification",
        subtitle = "Protect your account with Google Authenticator.",
        onCancel = vm::logout
    ) {
        val enrollment = vm.enrollment
        if (enrollment == null) {
            if (vm.busy) CircularProgressIndicator()
            else {
                Text(vm.error ?: "Could not start setup.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = vm::startEnrollment) { Text("Try again") }
            }
            return@TwoFactorScaffold
        }

        Step("1", "Install Google Authenticator from the Play Store.")
        Step("2", "Tap + then \"Scan a QR code\" and scan this code:")
        Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
            QrCode(enrollment.otpauthUri, Modifier.size(220.dp).padding(8.dp))
        }
        Text("Can't scan? Choose \"Enter a setup key\" and type:", style = MaterialTheme.typography.bodySmall)
        SelectionContainer {
            Text(
                enrollment.secret.chunked(4).joinToString(" "),
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
            )
        }
        Step("3", "Enter the 6-digit code shown in the app.")
        OtpField(code, { code = it; codeError = null; vm.clearError() }, codeError ?: vm.error)
        Button(
            onClick = {
                codeError = Validators.otp(code)
                if (codeError == null) vm.confirmEnrollment(code)
            },
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Verify and turn on")
        }
    }
}

/** Login step 2: enter the current 6-digit code from Google Authenticator. */
@Composable
fun TwoFactorVerifyScreen(vm: AppViewModel) {
    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }

    TwoFactorScaffold(
        icon = Icons.Filled.Lock,
        title = "Two-step verification",
        subtitle = "Open Google Authenticator and enter the 6-digit code for FoundIt (${maskEmail(vm.email())}).",
        onCancel = vm::logout
    ) {
        OtpField(code, { code = it; codeError = null; vm.clearError() }, codeError ?: vm.error)
        Button(
            onClick = {
                codeError = Validators.otp(code)
                if (codeError == null) vm.verifyCode(code)
            },
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Verify")
        }
        Text(
            "Codes change every 30 seconds. Make sure your phone's time is set automatically.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TwoFactorScaffold(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            content()
            TextButton(onClick = onCancel) { Text("Cancel and log out") }
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(number, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Shows j***@school.edu instead of the full email (avoid displaying private info). */
fun maskEmail(email: String): String {
    val name = email.substringBefore("@")
    val domain = email.substringAfter("@", "")
    if (name.isEmpty() || domain.isEmpty()) return email
    return "${name.first()}***@$domain"
}

/** Prevents screenshots and screen recording while this composable is shown. */
@Composable
fun SecureWindow() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
