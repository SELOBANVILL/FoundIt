package com.example.foundit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foundit.data.Item
import com.example.foundit.data.ItemStatus
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.ConfirmDialog
import com.example.foundit.ui.components.EmptyState
import com.example.foundit.ui.components.ItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(vm: AppViewModel, onOpen: (String) -> Unit, onEdit: (String) -> Unit, onPostNew: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadMyItems() }
    var toDelete by remember { mutableStateOf<Item?>(null) }
    var toClaim by remember { mutableStateOf<Item?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("My reports") }) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                !vm.myItemsLoaded -> LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.myItems.isEmpty() -> EmptyState(
                    Icons.Outlined.PostAdd, "No reports yet",
                    "Items you report as lost or found will appear here."
                ) { Button(onClick = onPostNew) { Text("Report an item") } }
                else -> LazyColumn(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(vm.myItems, key = { it.id }) { item ->
                        ItemCard(item, onClick = { onOpen(item.id) }) {
                            ItemMenu(
                                item,
                                onEdit = { onEdit(item.id) },
                                onClaim = { toClaim = item },
                                onDelete = { toDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { item ->
        ConfirmDialog(
            "Delete \"${item.name}\"?", "This permanently removes the post, its photo and comments.",
            "Delete", destructive = true, onConfirm = { vm.deleteItem(item) }, onDismiss = { toDelete = null }
        )
    }
    toClaim?.let { item ->
        ConfirmDialog(
            "Mark \"${item.name}\" as claimed?", "Others will see this item has been returned.",
            "Mark claimed", onConfirm = { vm.markClaimed(item) }, onDismiss = { toClaim = null }
        )
    }
}

@Composable
private fun ItemMenu(item: Item, onEdit: () -> Unit, onClaim: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, "Actions for ${item.name}") }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Edit") }, { open = false; onEdit() })
            if (item.status != ItemStatus.CLAIMED) DropdownMenuItem({ Text("Mark as claimed") }, { open = false; onClaim() })
            DropdownMenuItem(
                { Text("Delete", color = MaterialTheme.colorScheme.error) },
                { open = false; onDelete() }
            )
        }
    }
}
