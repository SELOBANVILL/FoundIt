package com.example.foundit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foundit.security.Validators
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.ErrorText
import com.example.foundit.ui.components.FormField
import com.example.foundit.ui.components.OtpField
import com.example.foundit.ui.components.PasswordField

/** Forgot password: step 1 = email a code, step 2 = code + new password + authenticator code. */
@Composable
fun ForgotPasswordScreen(vm: AppViewModel, initialEmail: String, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var emailCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var authCode by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var emailCodeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var authCodeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.clearError() }
    BackHandler(onBack = onBack)

    fun sendCode() {
        emailError = Validators.email(email)
        if (emailError != null) return
        vm.sendResetCode(email) {
            codeSent = true
            emailCode = ""; authCode = ""
            vm.snackbar = "If an account exists for that email, a code was sent."
        }
    }

    fun submit() {
        emailCodeError = Validators.emailCode(emailCode)
        passwordError = Validators.password(password)
        confirmError = Validators.confirmPassword(password, confirm)
        authCodeError = Validators.otp(authCode)
        if (listOf(emailCodeError, passwordError, confirmError, authCodeError).any { it != null }) return
        vm.resetPassword(email, emailCode, password, authCode) { onBack() }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.LockReset, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Reset password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (!codeSent) "Enter your school email and we'll send you a code."
                else "Enter the code we emailed to $email, choose a new password, and enter your authenticator code.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (!codeSent) {
                FormField(
                    email, { email = it; emailError = null }, "School email", emailError,
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Done
                )
                ErrorText(vm.error)
                Button(
                    onClick = ::sendCode, enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Send code")
                }
            } else {
                FormField(
                    emailCode, { emailCode = it.filter(Char::isDigit); emailCodeError = null },
                    "Code from email", emailCodeError, maxLength = 8, keyboardType = KeyboardType.Number
                )
                PasswordField(
                    password, { password = it; passwordError = null }, "New password", passwordError,
                    supporting = "At least 8 characters, with a letter and a number"
                )
                PasswordField(
                    confirm, { confirm = it; confirmError = null }, "Confirm new password", confirmError
                )
                OtpField(authCode, { authCode = it; authCodeError = null }, authCodeError)
                ErrorText(vm.error)
                Button(
                    onClick = ::submit, enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Reset password")
                }
                TextButton(onClick = ::sendCode, enabled = !vm.busy) { Text("Send a new code") }
                TextButton(onClick = { codeSent = false; vm.clearError() }) { Text("Use a different email") }
            }
            TextButton(onClick = onBack) { Text("Back to log in") }
        }
    }
}