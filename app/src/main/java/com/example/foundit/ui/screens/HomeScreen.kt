package com.example.foundit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foundit.data.CATEGORIES
import com.example.foundit.data.ItemType
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.EmptyState
import com.example.foundit.ui.components.ItemCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.startListening(context) }
    // Debounced search: reload 300 ms after the user stops typing or changes a filter.
    LaunchedEffect(vm.query, vm.typeFilter, vm.categoryFilter) {
        delay(300)
        vm.loadItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoundIt") },
                actions = {
                    IconButton(onClick = { vm.unread = 0; vm.typeFilter = ItemType.LOST }) {
                        BadgedBox(badge = { if (vm.unread > 0) Badge { Text("${vm.unread}") } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "New lost items")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it.take(60) },
                placeholder = { Text("Search name, description, place") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (vm.query.isNotEmpty()) IconButton(onClick = { vm.query = "" }) {
                        Icon(Icons.Filled.Clear, "Clear search")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(vm.typeFilter == null, { vm.typeFilter = null }, { Text("All") })
                FilterChip(vm.typeFilter == ItemType.LOST, { vm.typeFilter = ItemType.LOST }, { Text("Lost") })
                FilterChip(vm.typeFilter == ItemType.FOUND, { vm.typeFilter = ItemType.FOUND }, { Text("Found") })
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CATEGORIES) { c ->
                    FilterChip(
                        selected = vm.categoryFilter == c,
                        onClick = { vm.categoryFilter = if (vm.categoryFilter == c) null else c },
                        label = { Text(c) }
                    )
                }
            }

            when {
                !vm.itemsLoaded -> Box(Modifier.fillMaxSize()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
                }
                vm.items.isEmpty() -> EmptyState(
                    Icons.Outlined.Inventory2,
                    "No items found",
                    if (vm.query.isNotBlank() || vm.typeFilter != null || vm.categoryFilter != null)
                        "Try a different search or clear the filters."
                    else "Nothing has been reported yet. Use the Post tab to report an item."
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(vm.items, key = { it.id }) { item -> ItemCard(item, onClick = { onOpen(item.id) }) }
                }
            }
        }
    }
}
