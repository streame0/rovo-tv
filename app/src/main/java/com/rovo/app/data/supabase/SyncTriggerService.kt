package com.rovo.app.data.supabase

import com.rovo.app.data.local.AddonDao
import com.rovo.app.data.model.AddonEntity
import com.rovo.app.data.model.HubRowEntity
import com.rovo.app.data.model.HubRowItemEntity
import com.rovo.app.data.model.SeriesNextUpEntity
import com.rovo.app.data.model.WatchHistoryEntity
import com.rovo.app.data.model.WatchlistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight service that triggers Supabase sync when local data changes.
 * Injected into ViewModels alongside the DAO — call the relevant method
 * after any insert/update/delete to push the change to the cloud.
 *
 * All calls are fire-and-forget on a background scope to avoid blocking.
 */
@Singleton
class SyncTriggerService @Inject constructor(
    private val syncManager: SupabaseSyncManager,
    private val sessionStore: SupabaseSessionStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun watchProgressSaved(entity: WatchHistoryEntity) {
        if (!sessionStore.syncEnabled) return
        scope.launch { syncManager.pushWatchProgress(entity) }
    }

    fun watchlistItemAdded(entity: WatchlistEntity) {
        if (!sessionStore.syncEnabled) return
        scope.launch { syncManager.pushWatchlistAdd(entity) }
    }

    fun watchlistItemRemoved(id: String) {
        if (!sessionStore.syncEnabled) return
        scope.launch { syncManager.pushWatchlistRemove(id) }
    }

    fun triggerFullSync() {
        if (!sessionStore.syncEnabled) return
        scope.launch { syncManager.fullSync() }
    }
}
