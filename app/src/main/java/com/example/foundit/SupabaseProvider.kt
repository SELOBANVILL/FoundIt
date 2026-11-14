package com.example.foundit

import android.content.Context
import com.example.foundit.security.SecureSessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Single Supabase client for the app.
 *
 * - URL and publishable (anon) key come from local.properties via BuildConfig, so they are
 *   not hardcoded in source control. The publishable key is designed to be public; all data
 *   protection is enforced by Row Level Security policies on the server.
 * - Sessions are stored with [SecureSessionManager] (Android Keystore AES-GCM).
 * - All traffic is HTTPS (supabase.co) and cleartext is disabled in network_security_config.
 */
object SupabaseProvider {
    lateinit var client: SupabaseClient
        private set

    fun init(context: Context) {
        if (::client.isInitialized) return
        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                sessionManager = SecureSessionManager(context.applicationContext)
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}

val supabase: SupabaseClient get() = SupabaseProvider.client
