package com.example.foundit

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://tljyfqubjnksiusrbzjl.supabase.co",
    supabaseKey = "sb_publishable_B0mhxOynPJ8c5LIUm3o1Lw_zkmlf9bl"
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
    install(Realtime)
}