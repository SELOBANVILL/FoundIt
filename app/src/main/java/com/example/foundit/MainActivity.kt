package com.example.foundit

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foundit.data.AuthStage
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.EmptyState
import com.example.foundit.ui.screens.*
import com.example.foundit.ui.theme.FoundItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FoundItTheme { FoundItRoot() } }
    }
}

/**
 * Access control for the UI: the main app (and every record screen) only exists in the
 * composition when the session has passed BOTH password and Google Authenticator checks.
 * The server enforces the same rule independently through RLS (aal2).
 */
@Composable
fun FoundItRoot(vm: AppViewModel = viewModel()) {
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(vm.snackbar) {
        vm.snackbar?.let { snackbarHost.showSnackbar(it); vm.snackbar = null }
    }

    Box(Modifier.fillMaxSize()) {
        when (vm.stage) {
            null -> if (vm.startupFailed) {
                EmptyState(Icons.Outlined.CloudOff, "Can't connect", "Check your internet connection to continue.") {
                    Button(onClick = vm::refreshStage) { Text("Retry") }
                }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            AuthStage.LOGGED_OUT -> AuthScreen(vm)
            AuthStage.NEEDS_ENROLL -> TwoFactorSetupScreen(vm)
            AuthStage.NEEDS_VERIFY -> TwoFactorVerifyScreen(vm)
            AuthStage.READY -> MainScaffold(vm)
        }
        SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp))
    }
}

private enum class Tab(val route: String, val label: String, val selected: ImageVector, val unselected: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    POST("post", "Post", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    MINE("mine", "My Reports", Icons.Filled.Inventory, Icons.Outlined.Inventory),
    ACCOUNT("account", "Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
}

@Composable
private fun MainScaffold(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == route }

    NotificationPermissionRequest()

    fun goToTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = route == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { goToTab(tab) },
                        icon = { Icon(if (selected) tab.selected else tab.unselected, null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        NavHost(nav, startDestination = Tab.HOME.route, modifier = Modifier.padding(padding)) {
            composable(Tab.HOME.route) { HomeScreen(vm, onOpen = { nav.navigate("detail/$it") }) }
            composable(Tab.POST.route) { PostItemScreen(vm, editing = null, onDone = { goToTab(Tab.HOME) }) }
            composable(Tab.MINE.route) {
                MyReportsScreen(
                    vm,
                    onOpen = { nav.navigate("detail/$it") },
                    onEdit = { nav.navigate("edit/$it") },
                    onPostNew = { goToTab(Tab.POST) }
                )
            }
            composable(Tab.ACCOUNT.route) { AccountScreen(vm) }
            composable("detail/{id}") { entry ->
                DetailScreen(
                    vm, entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit/$it") }
                )
            }
            composable("edit/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val item = (vm.myItems + listOfNotNull(vm.detailItem)).firstOrNull { it.id == id }
                when {
                    item == null -> {
                        LaunchedEffect(id) { vm.loadDetail(id) }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    // Only the owner may edit (also enforced by RLS on the server).
                    !vm.isMine(item) -> EmptyState(Icons.Outlined.Lock, "Not allowed", "You can only edit your own posts.") {
                        Button(onClick = { nav.popBackStack() }) { Text("Go back") }
                    }
                    else -> PostItemScreen(
                        vm, editing = item,
                        onDone = { nav.popBackStack() },
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Asks once for notification permission (Android 13+), used for "new lost item" alerts. */
@Composable
private fun NotificationPermissionRequest() {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
