package com.example.foundit

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foundit.notify.NotificationHelper
import com.example.foundit.ui.theme.*
import io.github.jan.supabase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        setContent { MaterialTheme { FoundItApp() } }
    }
}

@Composable
fun FoundItApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    var start by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        supabase.auth.awaitInitialization()
        start = if (supabase.auth.currentSessionOrNull() != null) "home" else "auth"
    }

    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        NavHost(nav, startDestination = startRoute) {
            composable("auth") {
                AuthScreen(vm) { nav.navigate("home") { popUpTo("auth") { inclusive = true } } }
            }
            composable("home") {
                HomeScreen(
                    vm,
                    onPost = { nav.navigate("post") },
                    onOpen = { nav.navigate("detail/$it") },
                    onMine = { nav.navigate("mine") },
                    onLogout = { vm.logout { nav.navigate("auth") { popUpTo(0) } } }
                )
            }
            composable("post") { PostItemScreen(vm, null) { nav.popBackStack() } }
            composable("edit/{id}") { entry ->
                val id = entry.arguments?.getString("id")
                val item = (vm.items + vm.myItems).firstOrNull { it.id == id }
                PostItemScreen(vm, item) { nav.popBackStack() }
            }
            composable("detail/{id}") { entry ->
                DetailScreen(
                    vm, entry.arguments?.getString("id") ?: "",
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit/$it") }
                )
            }
            composable("mine") {
                MyReportsScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate("detail/$it") },
                    onEdit = { nav.navigate("edit/$it") }
                )
            }
        }
    }
}