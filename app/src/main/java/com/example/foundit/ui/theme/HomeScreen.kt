package com.example.foundit.ui.theme

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foundit.data.Comment
import com.example.foundit.data.Item
import com.example.foundit.data.Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onPost: () -> Unit,
    onOpen: (String) -> Unit,
    onMine: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.loadItems()
        vm.startListening(context.applicationContext)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoundIt") },
                actions = {
                    IconButton(onClick = { vm.unread = 0 }) {
                        BadgedBox(badge = { if (vm.unread > 0) Badge { Text("${vm.unread}") } }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onMine) { Icon(Icons.Default.Person, "My reports") }
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Log out") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPost,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Report item") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it; vm.loadItems() },
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(vm.typeFilter == null, { vm.typeFilter = null; vm.loadItems() }, { Text("All") })
                FilterChip(vm.typeFilter == "LOST", { vm.typeFilter = "LOST"; vm.loadItems() }, { Text("Lost") })
                FilterChip(vm.typeFilter == "FOUND", { vm.typeFilter = "FOUND"; vm.loadItems() }, { Text("Found") })
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CATEGORIES) { c ->
                    FilterChip(
                        selected = vm.categoryFilter == c,
                        onClick = {
                            vm.categoryFilter = if (vm.categoryFilter == c) null else c
                            vm.loadItems()
                        },
                        label = { Text(c) }
                    )
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(vm.items, key = { it.id }) { item ->
                    ItemCard(item) { onOpen(item.id) }
                }
            }
        }
    }
}

// Shared card used by Home and My Reports
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "${item.location ?: ""}  ${item.createdAt?.substringBefore("T") ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
                val claimed = item.status == "CLAIMED"
                Text(
                    if (claimed) "CLAIMED" else item.type,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        claimed -> Color(0xFF14A38B)
                        item.type == "LOST" -> Color(0xFFD98200)
                        else -> Color(0xFF1E6FD9)
                    }
                )
            }
        }
    }
}