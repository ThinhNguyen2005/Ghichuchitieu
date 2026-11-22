package com.notepay.data.remote.supabase

import com.notepay.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazily provides a configured [SupabaseClient] when `SUPABASE_URL` and `SUPABASE_ANON_KEY`
 * are present in [BuildConfig] (sourced from `local.properties` or CI env vars).
 *
 * Returns `null` when cloud credentials are absent so the app runs strictly offline-only
 * with zero crashes.
 */
@Singleton
open class SupabaseClientProvider @Inject constructor(
    private val json: Json,
) {
    open val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    open val client: SupabaseClient? by lazy {
        if (!isConfigured) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim(),
            ) {
                httpEngine = OkHttp.create()
                defaultSerializer = KotlinXSerializer(json)
                install(Auth)
                install(Postgrest)
                install(Realtime)
            }
        }
    }
}
