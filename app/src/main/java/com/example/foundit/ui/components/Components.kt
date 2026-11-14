package com.example.foundit.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.foundit.data.Item
import com.example.foundit.data.ItemStatus
import com.example.foundit.data.ItemType
import com.example.foundit.ui.theme.StatusColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

// ---------------------------------------------------------------------
// Status badge: LOST / FOUND / CLAIMED
// ---------------------------------------------------------------------
@Composable
fun StatusBadge(item: Item) {
    val (label, fg, bg) = when {
        item.status == ItemStatus.CLAIMED -> Triple("Claimed", StatusColors.Claimed, StatusColors.ClaimedContainer)
        item.type == ItemType.LOST -> Triple("Lost", StatusColors.Lost, StatusColors.LostContainer)
        else -> Triple("Found", StatusColors.Found, StatusColors.FoundContainer)
    }
    Text(
        label.uppercase(),
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// ---------------------------------------------------------------------
// Item photo with a neutral placeholder
// ---------------------------------------------------------------------
@Composable
fun ItemPhoto(url: Any?, modifier: Modifier = Modifier) {
    val placeholder = @Composable {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Box(modifier.clip(MaterialTheme.shapes.medium)) {
        if (url == null) placeholder() else SubcomposeAsyncImage(
            model = url, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { placeholder() }, error = { placeholder() }
        )
    }
}

// ---------------------------------------------------------------------
// Item card used by Home and My Reports
// ---------------------------------------------------------------------
@Composable
fun ItemCard(item: Item, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemPhoto(item.imageUrl, Modifier.size(72.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(item.location, item.createdAt?.toDateLabel()).joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                StatusBadge(item)
            }
            trailing?.invoke()
        }
    }
}

fun String.toDateLabel(): String = substringBefore("T")

// ---------------------------------------------------------------------
// Empty / message states
// ---------------------------------------------------------------------
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) { Spacer(Modifier.height(16.dp)); action() }
    }
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------------
// Confirmation dialog for important actions
// ---------------------------------------------------------------------
@Composable
fun ConfirmDialog(
    title: String, message: String, confirmLabel: String,
    destructive: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------
// Text fields with inline validation errors
// ---------------------------------------------------------------------
@Composable
fun FormField(
    value: String, onValueChange: (String) -> Unit, label: String,
    error: String? = null, modifier: Modifier = Modifier,
    singleLine: Boolean = true, minLines: Int = 1, maxLength: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Next,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
        label = { Text(label) },
        isError = error != null,
        supportingText = {
            when {
                error != null -> Text(error)
                maxLength != null -> Text("${value.length}/$maxLength", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                supporting != null -> Text(supporting)
            }
        },
        singleLine = singleLine, minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PasswordField(
    value: String, onValueChange: (String) -> Unit, label: String,
    error: String? = null, supporting: String? = null, imeAction: ImeAction = ImeAction.Next
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 72) onValueChange(it) },
        label = { Text(label) },
        isError = error != null,
        supportingText = { (error ?: supporting)?.let { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** 6-digit authenticator code input: digits only, max 6. */
@Composable
fun OtpField(value: String, onValueChange: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter(Char::isDigit).take(6)) },
        label = { Text("6-digit code") },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------
// QR code (ZXing) for Google Authenticator setup
// ---------------------------------------------------------------------
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, sizePx: Int = 600) {
    val bitmap = remember(content) { generateQrBitmap(content, sizePx) }
    Image(
        bitmap.asImageBitmap(),
        contentDescription = "QR code to scan with Google Authenticator",
        modifier = modifier
    )
}

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1)
    )
    val pixels = IntArray(size * size) { i ->
        if (matrix.get(i % size, i / size)) AndroidColor.BLACK else AndroidColor.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.RGB_565)
}
