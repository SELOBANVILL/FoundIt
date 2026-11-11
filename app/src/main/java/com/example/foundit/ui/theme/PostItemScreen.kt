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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItemScreen(vm: AppViewModel, editing: Item?, onDone: () -> Unit) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(editing?.type ?: "LOST") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: CATEGORIES.first()) }
    var location by remember { mutableStateOf(editing?.location ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        imageUri = it
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "Report item" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == "LOST", { type = "LOST" }, { Text("I lost something") })
                FilterChip(type == "FOUND", { type = "FOUND" }, { Text("I found something") })
            }

            Box(
                Modifier.fillMaxWidth().height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8ECF4))
                    .clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                val model: Any? = imageUri ?: editing?.imageUrl
                if (model != null) {
                    AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("Tap to add a photo")
                }
            }

            OutlinedTextField(name, { name = it }, label = { Text("Item name") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description (color, brand, marks)") },
                minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("Where was it lost/found?") },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c ->
                    FilterChip(category == c, { category = c }, { Text(c) })
                }
            }

            Button(
                onClick = {
                    vm.saveItem(
                        context, editing, name.trim(), description.trim(),
                        category, location.trim(), type, imageUri, onDone
                    )
                },
                enabled = name.isNotBlank() && !vm.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (editing == null) "Post" else "Save changes") }

            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}