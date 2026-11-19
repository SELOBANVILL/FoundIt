package com.example.foundit.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foundit.data.AuthStage
import com.example.foundit.data.Comment
import com.example.foundit.data.Item
import com.example.foundit.data.ItemType
import com.example.foundit.data.Repository
import com.example.foundit.data.TotpEnrollment
import com.example.foundit.notify.NotificationHelper
import com.example.foundit.security.ErrorMapper
import com.example.foundit.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AppViewModel : ViewModel() {

    // ---------- auth state ----------
    /** null while checking the saved session on startup. */
    var stage by mutableStateOf<AuthStage?>(null); private set
    var startupFailed by mutableStateOf(false); private set
    var enrollment by mutableStateOf<TotpEnrollment?>(null); private set
    private var failedCodeAttempts = 0

    // ---------- data ----------
    var items by mutableStateOf<List<Item>>(emptyList()); private set
    var myItems by mutableStateOf<List<Item>>(emptyList()); private set
    var detailItem by mutableStateOf<Item?>(null); private set
    var detailMissing by mutableStateOf(false); private set
    var comments by mutableStateOf<List<Comment>>(emptyList()); private set
    var itemsLoaded by mutableStateOf(false); private set
    var myItemsLoaded by mutableStateOf(false); private set

    // ---------- UI feedback ----------
    var busy by mutableStateOf(false); private set
    /** Error for the form currently on screen (shown inline). */
    var error by mutableStateOf<String?>(null)
    /** One-off confirmation/info message shown in a snackbar. */
    var snackbar by mutableStateOf<String?>(null)
    var unread by mutableIntStateOf(0)

    // ---------- search & filter ----------
    var query by mutableStateOf("")
    var typeFilter by mutableStateOf<String?>(null)
    var categoryFilter by mutableStateOf<String?>(null)

    private var listenJob: Job? = null

    init {
        refreshStage()
        // If the session expires or is revoked, return to the login screen.
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.NotAuthenticated && stage != null && stage != AuthStage.LOGGED_OUT) {
                    clearUserData()
                    stage = AuthStage.LOGGED_OUT
                }
            }
        }
    }

    /** Runs [block] with a busy flag; any failure becomes a safe, friendly message. */
    private fun launchSafe(onError: (String) -> Unit = { error = it }, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                onError(ErrorMapper.friendly(e))
            } finally {
                busy = false
            }
        }
    }

    fun clearError() { error = null }

    // =====================================================================
    // AUTHENTICATION
    // =====================================================================

    fun refreshStage() {
        viewModelScope.launch {
            startupFailed = false
            stage = try {
                Repository.authStage()
            } catch (e: Exception) {
                // Offline with a saved session: we cannot confirm 2FA, so do not open the app.
                startupFailed = true
                null
            }
        }
    }

    fun login(email: String, password: String) = launchSafe {
        Repository.signIn(email.trim(), password)
        failedCodeAttempts = 0
        stage = Repository.authStage()
    }

    fun register(email: String, password: String, onNeedsEmailConfirm: () -> Unit) = launchSafe {
        val hasSession = Repository.signUp(email.trim(), password)
        if (hasSession) {
            stage = Repository.authStage()
        } else {
            snackbar = "Account created. Check your email to confirm it, then log in."
            onNeedsEmailConfirm()
        }
    }

    fun sendResetCode(email: String, onSent: () -> Unit) = launchSafe {
        Repository.sendPasswordResetCode(email.trim())
        onSent()
    }

    fun resetPassword(
        email: String, emailCode: String, newPassword: String, authCode: String, onDone: () -> Unit
    ) = launchSafe {
        Repository.resetPassword(email.trim(), emailCode.trim(), newPassword, authCode)
        snackbar = "Password changed. Log in with your new password."
        onDone()
    }

    fun startEnrollment() = launchSafe {
        enrollment = Repository.startTotpEnrollment()
    }

    fun confirmEnrollment(code: String) {
        val factorId = enrollment?.factorId ?: return
        launchSafe(onError = ::onCodeFailed) {
            Repository.confirmTotpEnrollment(factorId, code)
            enrollment = null
            failedCodeAttempts = 0
            snackbar = "Two-factor authentication is on."
            stage = AuthStage.READY
        }
    }

    fun verifyCode(code: String) = launchSafe(onError = ::onCodeFailed) {
        Repository.verifyTotpLogin(code)
        failedCodeAttempts = 0
        stage = AuthStage.READY
    }

    /** After too many wrong codes the half-logged-in session is thrown away. */
    private fun onCodeFailed(message: String) {
        if (message.startsWith("No connection")) { error = message; return }
        failedCodeAttempts++
        if (failedCodeAttempts >= MAX_CODE_ATTEMPTS) {
            failedCodeAttempts = 0
            logout()
            snackbar = "Too many wrong codes. Please log in again."
        } else {
            error = ErrorMapper.INVALID_CODE + " (${MAX_CODE_ATTEMPTS - failedCodeAttempts} tries left)"
        }
    }

    fun logout() {
        viewModelScope.launch {
            busy = true
            runCatching { Repository.stopListening() }
            runCatching { Repository.signOut() } // clears encrypted session even if offline
            clearUserData()
            stage = AuthStage.LOGGED_OUT
            busy = false
        }
    }

    private fun clearUserData() {
        listenJob?.cancel()
        listenJob = null
        items = emptyList(); myItems = emptyList(); comments = emptyList()
        detailItem = null; enrollment = null
        itemsLoaded = false; myItemsLoaded = false
        query = ""; typeFilter = null; categoryFilter = null
        unread = 0; error = null
    }

    // =====================================================================
    // READ
    // =====================================================================

    fun loadItems() {
        viewModelScope.launch {
            runCatching { Repository.getItems(query, typeFilter, categoryFilter) }
                .onSuccess { items = it; itemsLoaded = true }
                .onFailure { snackbar = ErrorMapper.friendly(it); itemsLoaded = true }
        }
    }

    fun loadMyItems() {
        viewModelScope.launch {
            runCatching { Repository.getMyItems() }
                .onSuccess { myItems = it; myItemsLoaded = true }
                .onFailure { snackbar = ErrorMapper.friendly(it); myItemsLoaded = true }
        }
    }

    fun loadDetail(itemId: String) {
        detailItem = (items + myItems).firstOrNull { it.id == itemId }
        detailMissing = false
        comments = emptyList()
        viewModelScope.launch {
            runCatching { Repository.getItem(itemId) }
                .onSuccess { detailItem = it; detailMissing = it == null }
                .onFailure { snackbar = ErrorMapper.friendly(it) }
            runCatching { Repository.getComments(itemId) }.onSuccess { comments = it }
        }
    }

    fun isMine(item: Item) = item.userId == Repository.currentUserIdOrNull()

    fun currentUserId() = Repository.currentUserIdOrNull()

    fun email() = Repository.email()

    private suspend fun refreshLists() {
        items = Repository.getItems(query, typeFilter, categoryFilter)
        myItems = Repository.getMyItems()
    }

    // =====================================================================
    // CREATE / UPDATE / DELETE
    // =====================================================================

    fun saveItem(
        context: Context, editing: Item?, name: String, description: String,
        category: String, location: String, type: String, imageUri: Uri?,
        contactEmail: String?, contactInfo: String?, onDone: () -> Unit
    ) = launchSafe {
        if (Repository.hasOpenDuplicate(name, location, type, editing?.id)) {
            error = "You already have an open post for \"$name\" at $location."
            return@launchSafe
        }
        val bytes = imageUri?.let { withContext(Dispatchers.IO) { context.uriToJpegBytes(it) } }
        if (editing == null) {
            Repository.createItem(name, description, category, location, type, bytes, contactEmail, contactInfo)
            snackbar = "Item posted."
        } else {
            Repository.updateItem(
                editing.id, name, description, category, location, type, bytes, editing.imageUrl, contactEmail, contactInfo
            )
            snackbar = "Changes saved."
        }
        refreshLists()
        onDone()
    }

    fun markClaimed(item: Item) = launchSafe(onError = { snackbar = it }) {
        Repository.markClaimed(item.id)
        refreshLists()
        if (detailItem?.id == item.id) detailItem = Repository.getItem(item.id)
        snackbar = "Marked as claimed."
    }

    fun deleteItem(item: Item, onDone: () -> Unit = {}) = launchSafe(onError = { snackbar = it }) {
        Repository.deleteItem(item)
        refreshLists()
        snackbar = "Post deleted."
        onDone()
    }

    fun addComment(itemId: String, text: String, isClaim: Boolean, onSent: () -> Unit = {}) =
        launchSafe(onError = { snackbar = it }) {
            Repository.addComment(itemId, text.trim(), isClaim)
            comments = Repository.getComments(itemId)
            snackbar = if (isClaim) "Claim sent to the poster." else "Comment posted."
            onSent()
        }

    fun deleteComment(itemId: String, commentId: String) = launchSafe(onError = { snackbar = it }) {
        Repository.deleteComment(commentId)
        comments = Repository.getComments(itemId)
        snackbar = "Comment deleted."
    }

    // =====================================================================
    // NOTIFICATIONS (realtime)
    // =====================================================================

    fun startListening(context: Context) {
        if (listenJob?.isActive == true) return
        val appContext = context.applicationContext
        listenJob = viewModelScope.launch {
            runCatching {
                Repository.listenForNewItems { item ->
                    // Alert for every post made by someone else (both LOST and FOUND).
                    if (item.userId != Repository.currentUserIdOrNull()) {
                        unread++
                        val kind = if (item.type == ItemType.LOST) "Lost" else "Found"
                        // In-app popup (always visible while the app is open)…
                        snackbar = "New $kind item: ${item.name}"
                        // …plus a system notification (heads-up banner / notification shade).
                        NotificationHelper.showNewItem(appContext, item)
                    }
                    viewModelScope.launch { runCatching { refreshLists() } }
                }
            }
        }
    }

    private companion object {
        const val MAX_CODE_ATTEMPTS = 5
    }
}

/** Shrinks the photo to max 1280 px, JPEG quality 80, so uploads are fast and small. */
private fun Context.uriToJpegBytes(uri: Uri, maxSize: Int = 1280): ByteArray {
    val source = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: throw java.io.IOException("Could not read the selected image")
    val scale = minOf(1f, maxSize / maxOf(source.width, source.height).toFloat())
    val bitmap = if (scale < 1f) {
        source.scale((source.width * scale).toInt(), (source.height * scale).toInt())
    } else source
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
}
