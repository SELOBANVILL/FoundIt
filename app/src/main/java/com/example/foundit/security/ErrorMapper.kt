package com.example.foundit.security

import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

/**
 * Converts any exception into a short, safe message for the user.
 * Never returns raw server text, SQL errors or stack traces.
 */
object ErrorMapper {

    /** Generic login error: never reveals whether the email exists. */
    const val INVALID_LOGIN = "Invalid email or password."
    const val INVALID_CODE = "Invalid or expired code. Check Google Authenticator and try again."

    fun friendly(e: Throwable): String = when (e) {
        is HttpRequestException, is IOException, is TimeoutCancellationException ->
            "No connection. Check your internet and try again."
        is AuthRestException -> authMessage(e)
        is RestException -> restMessage(e)
        else -> "Something went wrong. Please try again."
    }

    private fun authMessage(e: AuthRestException): String {
        val code = e.error.lowercase()
        return when {
            code.contains("invalid_credentials") || code.contains("invalid_grant") -> INVALID_LOGIN
            code.contains("email_not_confirmed") -> "Please confirm your email first, then log in."
            code.contains("user_already_exists") || code.contains("email_exists") ->
                "Could not create account. Try logging in instead."
            code.contains("weak_password") -> "Password is too weak. Use at least 8 characters with letters and numbers."
            code.contains("mfa") || code.contains("totp") -> INVALID_CODE
            code.contains("over_") || code.contains("rate") -> "Too many attempts. Please wait a minute and try again."
            code.contains("same_password") -> "Your new password must be different from the old one."
            code.contains("otp") || code.contains("expired") -> "Invalid or expired code. Request a new one and try again."
            e.statusCode == 429 -> "Too many attempts. Please wait a minute and try again."
            else -> "Authentication failed. Please try again."
        }
    }

    private fun restMessage(e: RestException): String {
        val text = (e.error + " " + (e.description ?: "")).lowercase()
        return when {
            e.statusCode == 401 -> "Your session expired. Please log in again."
            e.statusCode == 403 || text.contains("row-level security") -> "You are not allowed to do that."
            text.contains("duplicate") || text.contains("23505") || text.contains("unique") ->
                "You already have an open post for this item."
            text.contains("check constraint") || text.contains("23514") -> "Some fields are invalid. Please review your input."
            e.statusCode == 429 -> "Too many requests. Please wait a moment."
            else -> "Something went wrong. Please try again."
        }
    }
}
