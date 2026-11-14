package com.example.foundit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foundit.BuildConfig
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.ConfirmDialog
import com.example.foundit.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(vm: AppViewModel) {
    var confirmLogout by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadMyItems() }

    Scaffold(topBar = { TopAppBar(title = { Text("Account") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.AccountCircle, null, Modifier.size(40.dp)) },
                    headlineContent = { Text(maskEmail(vm.email())) },
                    supportingContent = { Text("Signed in") }
                )
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.VerifiedUser, null, tint = StatusColors.Claimed) },
                    headlineContent = { Text("Two-step verification") },
                    supportingContent = { Text("On — Google Authenticator") }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Inventory, null) },
                    headlineContent = { Text("My reports") },
                    supportingContent = {
                        val open = vm.myItems.count { it.status == "OPEN" }
                        Text("${vm.myItems.size} total, $open open")
                    }
                )
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { confirmLogout = true },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Log out")
            }
            Text(
                "FoundIt v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (confirmLogout) ConfirmDialog(
        "Log out?", "You'll need your password and an authenticator code to sign in again.",
        "Log out", destructive = true, onConfirm = vm::logout, onDismiss = { confirmLogout = false }
    )
}
