package com.example.foundit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foundit.security.Validators
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.ErrorText
import com.example.foundit.ui.components.FormField
import com.example.foundit.ui.components.PasswordField

/** Step 1 of login: email + password (or create an account). */
@Composable
fun AuthScreen(vm: AppViewModel) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords are deliberately NOT saved across process death.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var forgot by rememberSaveable { mutableStateOf(false) }
    if (forgot) {
        ForgotPasswordScreen(vm, initialEmail = email, onBack = { forgot = false })
        return
    }

    fun switchMode(toRegister: Boolean) {
        registering = toRegister
        password = ""; confirm = ""
        emailError = null; passwordError = null; confirmError = null
        vm.clearError()
    }

    fun submit() {
        emailError = Validators.email(email)
        passwordError = if (registering) Validators.password(password) else Validators.loginPassword(password)
        confirmError = if (registering) Validators.confirmPassword(password, confirm) else null
        if (emailError != null || passwordError != null || confirmError != null) return
        if (registering) vm.register(email, password) { switchMode(false) }
        else vm.login(email, password)
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.TravelExplore, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("FoundIt", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Campus lost & found", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!registering, { switchMode(false) }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Log in") }
                SegmentedButton(registering, { switchMode(true) }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Create account") }
            }
            Spacer(Modifier.height(20.dp))

            FormField(
                email, { email = it; emailError = null }, "School email", emailError,
                keyboardType = KeyboardType.Email, maxLength = null
            )
            PasswordField(
                password, { password = it; passwordError = null }, "Password", passwordError,
                supporting = if (registering) "At least 8 characters, with a letter and a number" else null,
                imeAction = if (registering) ImeAction.Next else ImeAction.Done
            )
            if (registering) {
                PasswordField(confirm, { confirm = it; confirmError = null }, "Confirm password", confirmError, imeAction = ImeAction.Done)
            }

            ErrorText(vm.error)
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = ::submit, enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(if (registering) "Create account" else "Log in")
            }
            if (!registering) {
                TextButton(onClick = { vm.clearError(); forgot = true }) { Text("Forgot password?") }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "You'll need the Google Authenticator app for two-step verification.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
