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
fun DetailScreen(vm: AppViewModel, itemId: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val item = (vm.items + vm.myItems).firstOrNull { it.id == itemId }
    LaunchedEffect(itemId) { vm.loadComments(itemId) }
    if (item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Item not found") }
    } else {
        DetailContent(vm, item, onBack, onEdit)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(vm: AppViewModel, item: Item, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val isOwner = item.userId == Repository.userId()
    var text by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var showClaim by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AsyncImage(
                    model = item.imageUrl, contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.LightGray)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${item.type}  |  ${item.status}", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(item.name, style = MaterialTheme.typography.headlineSmall)
                    Text(item.description ?: "")
                    Text("Location: ${item.location ?: "-"}")
                    Text("Category: ${item.category ?: "-"}")
                    Text("Posted: ${item.createdAt?.substringBefore("T") ?: "-"}")
                }
            }
            item {
                if (isOwner) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.status != "CLAIMED") {
                            Button(onClick = { vm.markClaimed(item) }) { Text("Mark as claimed") }
                        }
                        OutlinedButton(onClick = { onEdit(item.id) }) { Text("Edit") }
                        OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
                    }
                } else if (item.status != "CLAIMED") {
                    Button(onClick = { showClaim = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("This is mine!")
                    }
                }
            }
            item { Text("Comments (${vm.comments.size})", style = MaterialTheme.typography.titleMedium) }
            items(vm.comments, key = { it.id }) { c ->
                CommentRow(c, canDelete = c.userId == Repository.userId()) {
                    vm.deleteComment(item.id, c.id)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        text, { text = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a comment...") }
                    )
                    IconButton(onClick = {
                        if (text.isNotBlank()) {
                            vm.addComment(item.id, text.trim(), false)
                            text = ""
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this post?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteItem(item, onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    if (showClaim) {
        AlertDialog(
            onDismissRequest = { showClaim = false },
            title = { Text("Claim this item") },
            text = {
                OutlinedTextField(
                    proof, { proof = it },
                    label = { Text("Tell the finder something only the owner would know") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addComment(item.id, "THIS IS MINE: ${proof.trim()}", true)
                    proof = ""
                    showClaim = false
                }) { Text("Send claim") }
            },
            dismissButton = { TextButton(onClick = { showClaim = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CommentRow(c: Comment, canDelete: Boolean, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (c.isClaim) Color(0xFFFFF3D6) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.userName ?: "user", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (c.isClaim) Text("CLAIM", color = Color(0xFFB26A00), style = MaterialTheme.typography.labelSmall)
                if (canDelete) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete comment") }
                }
            }
            Text(c.text)
        }
    }
}