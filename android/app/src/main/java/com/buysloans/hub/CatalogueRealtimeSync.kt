package com.buysloans.hub

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps the native catalogue aligned with the canonical Supabase catalogue revision row.
 *
 * Realtime is the primary path. A slow reconciliation loop remains as recovery for process
 * suspension, radio changes, or a websocket reconnect gap; it is deliberately not a fast poll.
 */
object CatalogueRealtimeSync {
    private const val RECOVERY_RECONCILE_MS = 60_000L
    private const val RECONNECT_DELAY_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeJob: Job? = null
    private var recoveryJob: Job? = null
    private var client: SupabaseClient? = null

    @Volatile
    private var boundToken = ""

    fun ensure(context: Context) {
        val appContext = context.applicationContext
        if (!AuthManager.isSignedIn(appContext)) {
            stop()
            return
        }

        val currentToken = AuthManager.accessToken(appContext)
        if (realtimeJob?.isActive == true && (currentToken.isBlank() || currentToken == boundToken)) {
            ensureRecoveryLoop(appContext)
            return
        }

        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            while (isActive && AuthManager.isSignedIn(appContext)) {
                val token = runCatching { AuthManager.validAccessToken(appContext) }.getOrNull()
                if (token.isNullOrBlank()) break
                boundToken = token

                val realtimeClient = createSupabaseClient(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
                ) {
                    accessToken = { AuthManager.validAccessToken(appContext) }
                    install(Realtime)
                }
                client = realtimeClient

                try {
                    val channel = realtimeClient.channel("morley-catalogue-sync-v1")
                    val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "catalog_sync_state"
                        filter = "id=eq.1"
                    }
                    val collector = changes.onEach {
                        runCatching { LiveDevicePricing.refresh(appContext) }
                    }.launchIn(this)

                    channel.subscribe()
                    collector.join()
                } catch (_: Throwable) {
                    // Supabase Realtime normally reconnects itself. If the channel terminates,
                    // recreate it after a bounded delay; the recovery loop covers the gap.
                } finally {
                    client = null
                }

                if (isActive) delay(RECONNECT_DELAY_MS)
            }
        }
        ensureRecoveryLoop(appContext)
    }

    fun stop() {
        boundToken = ""
        realtimeJob?.cancel()
        realtimeJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        client = null
    }

    private fun ensureRecoveryLoop(context: Context) {
        if (recoveryJob?.isActive == true) return
        recoveryJob = scope.launch {
            while (isActive && AuthManager.isSignedIn(context)) {
                delay(RECOVERY_RECONCILE_MS)
                runCatching { LiveDevicePricing.reconcile(context) }
            }
        }
    }
}
