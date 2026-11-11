package com.example.foundit.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foundit.data.Comment
import com.example.foundit.data.Item
import com.example.foundit.data.Repository
import com.example.foundit.notify.NotificationHelper
import com.example.foundit.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

val CATEGORIES = listOf("Bags", "IDs & Cards", "Electronics", "Clothing", "Books", "Keys", "Others")

class AppViewModel : ViewModel() {
    var items by mutableStateOf<List<Item>>(emptyList()); private set
    var myItems by mutableStateOf<List<Item>>(emptyList()); private set
    var comments by mutableStateOf<List<Comment>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    var unread by mutableIntStateOf(0)

    var query by mutableStateOf("")
    var typeFilter by mutableStateOf<String?>(null)
    var categoryFilter by mutableStateOf<String?>(null)

    private var listening = false

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true
            message = null
            try {
                block()
            } catch (e: Exception) {
                message = e.message ?: "Something went wrong"
            }
            loading = false
        }
    }

    private suspend fun refreshAll() {
        items = Repository.getItems(query, typeFilter, categoryFilter)
        myItems = Repository.getMyItems()
    }

    // ---------- AUTH ----------
    fun login(email: String, password: String, onSuccess: () -> Unit) = launchSafe {
        Repository.signIn(email, password)
        onSuccess()
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) = launchSafe {
        Repository.signUp(email, password)
        onSuccess()
    }

    fun logout(onDone: () -> Unit) = launchSafe {
        Repository.signOut()
        items = emptyList()
        myItems = emptyList()
        unread = 0
        onDone()
    }

    // ---------- READ ----------
    fun loadItems() {
        viewModelScope.launch {
            runCatching { Repository.getItems(query, typeFilter, categoryFilter) }
                .onSuccess { items = it }
                .onFailure { message = it.message }
        }
    }

    fun loadMyItems() {
        viewModelScope.launch {
            runCatching { Repository.getMyItems() }
                .onSuccess { myItems = it }
                .onFailure { message = it.message }
        }
    }

    fun loadComments(itemId: String) {
        viewModelScope.launch {
            runCatching { Repository.getComments(itemId) }.onSuccess { comments = it }
        }
    }

    // ---------- CREATE / UPDATE ----------
    fun saveItem(
        context: Context, editing: Item?, name: String, description: String,
        category: String, location: String, type: String, imageUri: Uri?, onDone: () -> Unit
    ) = launchSafe {
        val bytes = imageUri?.let { withContext(Dispatchers.IO) { context.uriToJpegBytes(it) } }
        if (editing == null) {
            Repository.createItem(name, description, category, location, type, bytes)
        } else {
            Repository.updateItem(editing.id, name, description, category, location, type, bytes)
        }
        refreshAll()
        onDone()
    }

    fun markClaimed(item: Item) = launchSafe {
        Repository.markClaimed(item.id)
        refreshAll()
    }

    // ---------- DELETE ----------
    fun deleteItem(item: Item, onDone: () -> Unit) = launchSafe {
        Repository.deleteItem(item)
        refreshAll()
        onDone()
    }

    // ---------- COMMENTS ----------
    fun addComment(itemId: String, text: String, isClaim: Boolean) = launchSafe {
        Repository.addComment(itemId, text, isClaim)
        comments = Repository.getComments(itemId)
    }

    fun deleteComment(itemId: String, commentId: String) = launchSafe {
        Repository.deleteComment(commentId)
        comments = Repository.getComments(itemId)
    }

    // ---------- BELL: realtime listener ----------
    fun startListening(context: Context) {
        if (listening) return
        listening = true
        viewModelScope.launch {
            try {
                Repository.listenForNewItems { item ->
                    val me = supabase.auth.currentUserOrNull()?.id
                    if (item.type == "LOST" && item.userId != me) {
                        unread++
                        NotificationHelper.showLostItem(context, item)
                    }
                    viewModelScope.launch { runCatching { refreshAll() } }
                }
            } catch (e: Exception) {
                listening = false
                message = e.message
            }
        }
    }
}

// Shrinks the photo to max 1280 px and JPEG quality 80 so uploads are fast and small.
private fun Context.uriToJpegBytes(uri: Uri, maxSize: Int = 1280): ByteArray {
    val source = contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
    val scale = minOf(1f, maxSize / maxOf(source.width, source.height).toFloat())
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            source, (source.width * scale).toInt(), (source.height * scale).toInt(), true
        )
    } else source
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
    return out.toByteArray()
}