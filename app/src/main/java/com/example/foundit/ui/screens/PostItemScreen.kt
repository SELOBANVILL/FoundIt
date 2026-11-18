package com.example.foundit.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foundit.data.CATEGORIES
import com.example.foundit.data.CIIT_EMAIL_DOMAIN
import com.example.foundit.data.Item
import com.example.foundit.data.ItemType
import com.example.foundit.security.Validators
import com.example.foundit.ui.AppViewModel
import com.example.foundit.ui.components.ConfirmDialog
import com.example.foundit.ui.components.ErrorText
import com.example.foundit.ui.components.FormField
import com.example.foundit.ui.components.ItemPhoto

/**
 * Create (editing == null) or edit an item.
 * @param onBack shown as a back arrow only when this screen was opened on top of another.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItemScreen(vm: AppViewModel, editing: Item?, onDone: () -> Unit, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf(editing?.type ?: ItemType.LOST) }
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(editing?.description ?: "") }
    var category by rememberSaveable { mutableStateOf(editing?.category ?: "") }
    var location by rememberSaveable { mutableStateOf(editing?.location ?: "") }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Pre-fill with the login email when it is already a CIIT address.
    val defaultEmail = editing?.contactEmail
        ?: vm.email().takeIf { it.lowercase().endsWith("@$CIIT_EMAIL_DOMAIN") } ?: ""
    var contactEmail by rememberSaveable { mutableStateOf(defaultEmail) }
    var contactInfo by rememberSaveable { mutableStateOf(editing?.contactInfo ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var contactEmailError by remember { mutableStateOf<String?>(null) }
    var contactInfoError by remember { mutableStateOf<String?>(null) }
    var confirmPublicContact by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.clearError() }

    val hasChanges = type != (editing?.type ?: ItemType.LOST) || name != (editing?.name ?: "") ||
        description != (editing?.description ?: "") || category != (editing?.category ?: "") ||
        location != (editing?.location ?: "") || imageUri != null ||
        contactEmail != defaultEmail || contactInfo != (editing?.contactInfo ?: "")

    // Ask before throwing away typed input.
    BackHandler(enabled = onBack != null && hasChanges) { confirmDiscard = true }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    fun validate(): Boolean {
        nameError = Validators.itemName(name)
        locationError = Validators.location(location)
        descriptionError = Validators.description(description)
        categoryError = Validators.category(category)
        contactEmailError = Validators.ciitEmail(contactEmail)
        contactInfoError = Validators.contactInfo(contactInfo)
        return listOf(nameError, locationError, descriptionError, categoryError, contactEmailError, contactInfoError)
            .all { it == null }
    }

    fun save() = vm.saveItem(
        context, editing, name.trim(), description.trim(), category, location.trim(), type, imageUri,
        contactEmail = contactEmail.trim(),
        contactInfo = contactInfo.trim().ifEmpty { null }
    ) {
        if (editing == null) {
            type = ItemType.LOST; name = ""; description = ""; category = ""; location = ""; imageUri = null
            contactEmail = defaultEmail; contactInfo = ""
        }
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "Report an item" else "Edit item") },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = { if (hasChanges) confirmDiscard = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(type == ItemType.LOST, { type = ItemType.LOST }, SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text("I lost something")
                }
                SegmentedButton(type == ItemType.FOUND, { type = ItemType.FOUND }, SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text("I found something")
                }
            }

            Box(
                Modifier.fillMaxWidth().height(180.dp).clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                contentAlignment = Alignment.Center
            ) {
                val model: Any? = imageUri ?: editing?.imageUrl
                if (model != null) ItemPhoto(model, Modifier.fillMaxSize())
                else Surface(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.AddAPhoto, null)
                        Text("Add a photo (optional)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (imageUri != null || editing?.imageUrl != null) {
                Text("Tap the photo to change it.", style = MaterialTheme.typography.bodySmall)
            }

            FormField(name, { name = it; nameError = null }, "Item name *", nameError, maxLength = Validators.NAME_MAX)
            FormField(
                location, { location = it; locationError = null },
                if (type == ItemType.LOST) "Where did you lose it? *" else "Where did you find it? *",
                locationError, maxLength = Validators.LOCATION_MAX
            )
            FormField(
                description, { description = it; descriptionError = null }, "Description (color, brand, marks)",
                descriptionError, singleLine = false, minLines = 3, maxLength = Validators.DESCRIPTION_MAX,
                imeAction = ImeAction.Default
            )

            Text("Category *", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c -> FilterChip(category == c, { category = c; categoryError = null }, { Text(c) }) }
            }
            categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.height(4.dp))
            Text("Your contact details", style = MaterialTheme.typography.labelLarge)
            PublicContactWarning()
            FormField(
                contactEmail, { contactEmail = it; contactEmailError = null }, "CIIT email *", contactEmailError,
                keyboardType = KeyboardType.Email, supporting = "Must end in @$CIIT_EMAIL_DOMAIN"
            )
            FormField(
                contactInfo, { contactInfo = it; contactInfoError = null },
                "Other contact info (optional)", contactInfoError,
                singleLine = false, minLines = 2, maxLength = Validators.CONTACT_INFO_MAX,
                imeAction = ImeAction.Default
            )

            ErrorText(vm.error)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { if (validate()) confirmPublicContact = true },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(if (editing == null) "Post item" else "Save changes")
            }
        }
    }

    if (confirmDiscard) ConfirmDialog(
        "Discard changes?", "What you typed will be lost.", "Discard", destructive = true,
        onConfirm = { onBack?.invoke() }, onDismiss = { confirmDiscard = false }
    )
    if (confirmPublicContact) ConfirmDialog(
        "Share your contact info?",
        "Your CIIT email${if (contactInfo.isNotBlank()) " and other contact info" else ""} will be visible to " +
            "everyone using FoundIt so " +
            (if (type == ItemType.LOST) "whoever finds your item" else "the owner") + " can reach you.",
        if (editing == null) "Post item" else "Save changes",
        onConfirm = ::save, onDismiss = { confirmPublicContact = false }
    )
}

@Composable
private fun PublicContactWarning() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(8.dp))
            Text(
                "The contact information you enter here will be visible to everyone using the app. " +
                    "Only share what you're comfortable making public.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
