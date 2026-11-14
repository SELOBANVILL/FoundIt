package com.example.foundit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foundit.data.Comment
import com.example.foundit.data.Item
import com.example.foundit.data.ItemStatus
import com.example.foundit.security.Validators
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.*
import com.example.foundit.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: AppViewModel, itemId: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    LaunchedEffect(itemId) { vm.loadDetail(itemId) }
    val item = vm.detailItem?.takeIf { it.id == itemId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                item != null -> DetailContent(vm, item, onBack, onEdit)
                vm.detailMissing -> EmptyState(Icons.Outlined.SearchOff, "Item not available", "It may have been deleted.")
                else -> LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DetailContent(vm: AppViewModel, item: Item, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val isOwner = vm.isMine(item)
    val myId = vm.currentUserId()
    var comment by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }
    var proof by remember { mutableStateOf("") }
    var proofError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClaimed by remember { mutableStateOf(false) }
    var showClaim by remember { mutableStateOf(false) }
    var deleteCommentId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { ItemPhoto(item.imageUrl, Modifier.fillMaxWidth().height(220.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusBadge(item)
                Text(item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (!item.description.isNullOrBlank()) Text(item.description, style = MaterialTheme.typography.bodyLarge)
                InfoRow(Icons.Filled.Place, item.location ?: "-")
                InfoRow(Icons.Filled.Category, item.category ?: "-")
                InfoRow(Icons.Filled.DateRange, item.createdAt?.toDateLabel() ?: "-")
            }
        }
        item {
            when {
                isOwner -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.status != ItemStatus.CLAIMED) {
                        Button(onClick = { confirmClaimed = true }, Modifier.fillMaxWidth()) { Text("Mark as claimed") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(item.id) }, Modifier.weight(1f)) { Text("Edit") }
                        OutlinedButton(
                            onClick = { confirmDelete = true }, Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    }
                }
                item.status != ItemStatus.CLAIMED ->
                    Button(onClick = { showClaim = true }, Modifier.fillMaxWidth()) { Text("This is mine — claim it") }
            }
        }
        item {
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Comments (${vm.comments.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (vm.comments.isEmpty()) item {
            Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(vm.comments, key = { it.id }) { c ->
            CommentRow(c, canDelete = c.userId == myId) { deleteCommentId = c.id }
        }
        item {
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    comment, { if (it.length <= Validators.COMMENT_MAX) { comment = it; commentError = null } },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a comment…") },
                    isError = commentError != null,
                    supportingText = { commentError?.let { Text(it) } }
                )
                IconButton(
                    onClick = {
                        commentError = Validators.comment(comment)
                        if (commentError == null) vm.addComment(item.id, comment, false) { comment = "" }
                    },
                    enabled = !vm.busy
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send comment") }
            }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Delete this post?", "This permanently removes the post, its photo and comments.", "Delete",
        destructive = true, onConfirm = { vm.deleteItem(item, onBack) }, onDismiss = { confirmDelete = false }
    )
    if (confirmClaimed) ConfirmDialog(
        "Mark as claimed?", "Others will see this item has been returned to its owner.", "Mark claimed",
        onConfirm = { vm.markClaimed(item) }, onDismiss = { confirmClaimed = false }
    )
    deleteCommentId?.let { id ->
        ConfirmDialog(
            "Delete comment?", "This cannot be undone.", "Delete", destructive = true,
            onConfirm = { vm.deleteComment(item.id, id) }, onDismiss = { deleteCommentId = null }
        )
    }
    if (showClaim) AlertDialog(
        onDismissRequest = { showClaim = false },
        title = { Text("Claim this item") },
        text = {
            OutlinedTextField(
                proof, { if (it.length <= 250) { proof = it; proofError = null } },
                label = { Text("Describe something only the owner would know") },
                isError = proofError != null, supportingText = { proofError?.let { Text(it) } },
                minLines = 2
            )
        },
        confirmButton = {
            TextButton(onClick = {
                proofError = Validators.comment(proof)
                if (proofError == null) {
                    vm.addComment(item.id, "CLAIM: ${proof.trim()}", true) { proof = "" }
                    showClaim = false
                }
            }) { Text("Send claim") }
        },
        dismissButton = { TextButton(onClick = { showClaim = false }) { Text("Cancel") } }
    )
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CommentRow(c: Comment, canDelete: Boolean, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (c.isClaim) StatusColors.LostContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (canDelete) "You" else (c.userName ?: "user"),
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    color = if (c.isClaim) StatusColors.Lost else MaterialTheme.colorScheme.onSurface
                )
                if (c.isClaim) Text("CLAIM", color = StatusColors.Lost, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (canDelete) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete comment") }
                else Spacer(Modifier.height(40.dp))
            }
            Text(c.text, color = if (c.isClaim) androidx.compose.ui.graphics.Color(0xFF3B2A12) else MaterialTheme.colorScheme.onSurface)
        }
    }
}
