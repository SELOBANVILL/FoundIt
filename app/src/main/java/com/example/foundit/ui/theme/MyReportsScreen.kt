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
fun MyReportsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    LaunchedEffect(Unit) { vm.loadMyItems() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (vm.myItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("You have not posted anything yet.")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(vm.myItems, key = { it.id }) { item ->
                    Column {
                        ItemCard(item) { onOpen(item.id) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (item.status != "CLAIMED") {
                                TextButton(onClick = { vm.markClaimed(item) }) { Text("Mark claimed") }
                            }
                            TextButton(onClick = { onEdit(item.id) }) { Text("Edit") }
                            TextButton(onClick = { vm.deleteItem(item) {} }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}