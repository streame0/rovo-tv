package com.rovo.app.data.supabase

import android.util.Log
import com.rovo.app.data.local.AddonDao
import com.rovo.app.data.model.AddonEntity
import com.rovo.app.data.model.HubRowEntity
import com.rovo.app.data.model.HubRowItemEntity
import com.rovo.app.data.model.SeriesNextUpEntity
import com.rovo.app.data.model.WatchHistoryEntity
import com.rovo.app.data.model.WatchlistEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val dao: AddonDao,
    private val authManager: SupabaseAuthManager,
    private val sessionStore: SupabaseSessionStore
) {
    companion object {
        private const val TAG = "SupabaseSync"
        private const val BATCH_SIZE = 100
    }

    private val syncMutex = Mutex()
    private var isSyncing = false

    /**
     * Full bidirectional sync for all tables.
     */
    suspend fun fullSync(): SyncResult {
        if (!sessionStore.syncEnabled) return SyncResult(0, 0, 0, "Sync disabled")
        if (!authManager.isConnected()) return SyncResult(0, 0, 0, "Not authenticated")

        return syncMutex.withLock {
            if (isSyncing) return@withLock SyncResult(0, 0, 0, "Already syncing")
            isSyncing = true
            try {
                withContext(Dispatchers.IO) {
                    var totalPushed = 0
                    var totalPulled = 0
                    var totalErrors = 0

                    val results = listOf(
                        syncWatchHistory(),
                        syncWatchlist(),
                        syncHubRows(),
                        syncHubRowItems(),
                        syncAddons(),
                        syncSeriesNextUp()
                    )

                    for (r in results) {
                        totalPushed += r.pushed
                        totalPulled += r.pulled
                        totalErrors += r.errors
                    }

                    sessionStore.lastSyncTimestamp = System.currentTimeMillis()

                    SyncResult(totalPushed, totalPulled, totalErrors, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Full sync failed", e)
                SyncResult(0, 0, 1, e.message)
            } finally {
                isSyncing = false
            }
        }
    }

    // ── Watch History ──

    suspend fun syncWatchHistory(): SyncResult = syncTable(
        tableName = "watch_history",
        localFetch = { dao.getAllWatchHistoryOnce() },
        remoteFetch = {
            supabase.from("watch_history")
                .select {
                order(column = "last_watched", order = Order.ASCENDING)
            }
                .decodeList<CloudWatchHistory>()
        },
        localToCloud = { item ->
            CloudWatchHistory(
                id = item.id,
                title = item.title,
                poster = item.poster,
                background = item.background,
                logo = item.logo,
                position = item.position,
                duration = item.duration,
                last_watched = item.lastWatched,
                type = item.type,
                watched = item.watched,
                scrobbled = item.scrobbled,
                updated_at = item.lastWatched
            )
        },
        cloudToLocal = { cloud ->
            WatchHistoryEntity(
                id = cloud.id,
                title = cloud.title,
                poster = cloud.poster,
                background = cloud.background,
                logo = cloud.logo,
                position = cloud.position,
                duration = cloud.duration,
                lastWatched = cloud.last_watched,
                type = cloud.type,
                watched = cloud.watched,
                scrobbled = cloud.scrobbled
            )
        },
        upsertLocal = { items -> dao.upsertHistoryItems(items) },
        upsertCloud = { items ->
            supabase.from("watch_history").upsert(items) { select() }
        },
        localId = { it.id },
        cloudId = { it.id }
    )

    // ── Watchlist ──

    suspend fun syncWatchlist(): SyncResult = syncTable(
        tableName = "watchlist",
        localFetch = { dao.getWatchlistOnce() },
        remoteFetch = {
            supabase.from("watchlist")
                .select {
                    order(column = "added_at", order = Order.ASCENDING)
                }
                .decodeList<CloudWatchlist>()
        },
        localToCloud = { item ->
            CloudWatchlist(
                id = item.id,
                type = item.type,
                title = item.title,
                poster = item.poster,
                added_at = item.addedAt,
                updated_at = item.addedAt
            )
        },
        cloudToLocal = { cloud ->
            WatchlistEntity(
                id = cloud.id,
                type = cloud.type,
                title = cloud.title,
                poster = cloud.poster,
                addedAt = cloud.added_at
            )
        },
        upsertLocal = { items ->
            for (item in items) dao.addToWatchlist(item)
        },
        upsertCloud = { items ->
            supabase.from("watchlist").upsert(items) { select() }
        },
        localId = { it.id },
        cloudId = { it.id }
    )

    // ── Hub Rows ──

    suspend fun syncHubRows(): SyncResult = syncTable(
        tableName = "hub_rows",
        localFetch = {
            dao.getAllHubRows().firstOrNull() ?: emptyList()
        },
        remoteFetch = {
            supabase.from("hub_rows")
                .select {
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<CloudHubRow>()
        },
        localToCloud = { item ->
            CloudHubRow(
                id = item.id,
                title = item.title,
                shape = item.shape,
                show_in_home = item.showInHome,
                show_in_movies = item.showInMovies,
                show_in_series = item.showInSeries,
                home_order = item.homeOrder,
                movies_order = item.moviesOrder,
                series_order = item.seriesOrder,
                created_at = item.createdAt,
                updated_at = item.createdAt
            )
        },
        cloudToLocal = { cloud ->
            HubRowEntity(
                id = cloud.id,
                title = cloud.title,
                shape = cloud.shape,
                showInHome = cloud.show_in_home,
                showInMovies = cloud.show_in_movies,
                showInSeries = cloud.show_in_series,
                homeOrder = cloud.home_order,
                moviesOrder = cloud.movies_order,
                seriesOrder = cloud.series_order,
                createdAt = cloud.created_at
            )
        },
        upsertLocal = { items -> items.forEach { dao.insertHubRow(it) } },
        upsertCloud = { items ->
            supabase.from("hub_rows").upsert(items) { select() }
        },
        localId = { it.id },
        cloudId = { it.id }
    )

    // ── Hub Row Items ──

    suspend fun syncHubRowItems(): SyncResult = syncTable(
        tableName = "hub_row_items",
        localFetch = {
            dao.getAllHubRowItems().firstOrNull() ?: emptyList()
        },
        remoteFetch = {
            supabase.from("hub_row_items")
                .select {
                    order(column = "item_order", order = Order.ASCENDING)
                }
                .decodeList<CloudHubRowItem>()
        },
        localToCloud = { item ->
            CloudHubRowItem(
                hub_row_id = item.hubRowId,
                config_unique_id = item.configUniqueId,
                title = item.title,
                custom_image_url = item.customImageUrl,
                item_order = item.itemOrder,
                updated_at = System.currentTimeMillis()
            )
        },
        cloudToLocal = { cloud ->
            HubRowItemEntity(
                hubRowId = cloud.hub_row_id,
                configUniqueId = cloud.config_unique_id,
                title = cloud.title,
                customImageUrl = cloud.custom_image_url,
                itemOrder = cloud.item_order
            )
        },
        upsertLocal = { items -> dao.insertHubRowItems(items) },
        upsertCloud = { items ->
            supabase.from("hub_row_items").upsert(items) { select() }
        },
        localId = { "${it.hubRowId}:${it.configUniqueId}" },
        cloudId = { "${it.hub_row_id}:${it.config_unique_id}" }
    )

    // ── Addons ──

    suspend fun syncAddons(): SyncResult = syncTable(
        tableName = "addons",
        localFetch = {
            dao.getAllAddons().firstOrNull() ?: emptyList()
        },
        remoteFetch = {
            supabase.from("addons")
                .select {
                    order(column = "sort_order", order = Order.ASCENDING)
                }
                .decodeList<CloudAddon>()
        },
        localToCloud = { item ->
            CloudAddon(
                transport_url = item.transportUrl,
                id = item.id,
                name = item.name,
                version = item.version,
                description = item.description,
                icon_url = item.iconUrl,
                is_trusted = item.isTrusted,
                is_enabled = item.isEnabled,
                nickname = item.nickname,
                catalogs_json = item.catalogsJson,
                supports_meta = item.supportsMeta,
                supports_stream = item.supportsStream,
                types_json = item.typesJson,
                id_prefixes_json = item.idPrefixesJson,
                sort_order = item.sortOrder,
                updated_at = System.currentTimeMillis()
            )
        },
        cloudToLocal = { cloud ->
            AddonEntity(
                transportUrl = cloud.transport_url,
                id = cloud.id,
                name = cloud.name ?: "",
                version = cloud.version,
                description = cloud.description,
                iconUrl = cloud.icon_url,
                isTrusted = cloud.is_trusted,
                isEnabled = cloud.is_enabled,
                nickname = cloud.nickname,
                catalogsJson = cloud.catalogs_json,
                supportsMeta = cloud.supports_meta,
                supportsStream = cloud.supports_stream,
                typesJson = cloud.types_json,
                idPrefixesJson = cloud.id_prefixes_json,
                sortOrder = cloud.sort_order
            )
        },
        upsertLocal = { items -> dao.insertAddons(items) },
        upsertCloud = { items ->
            supabase.from("addons").upsert(items) { select() }
        },
        localId = { it.transportUrl },
        cloudId = { it.transport_url }
    )

    // ── Series Next Up ──

    suspend fun syncSeriesNextUp(): SyncResult = syncTable(
        tableName = "series_next_up",
        localFetch = {
            dao.getActiveSeriesNextUp().firstOrNull() ?: emptyList()
        },
        remoteFetch = {
            supabase.from("series_next_up")
                .select {
                    order(column = "updated_at", order = Order.ASCENDING)
                }
                .decodeList<CloudSeriesNextUp>()
        },
        localToCloud = { item ->
            CloudSeriesNextUp(
                series_id = item.seriesId,
                title = item.title,
                poster = item.poster,
                next_season = item.nextSeason,
                next_episode = item.nextEpisode,
                next_episode_title = item.nextEpisodeTitle,
                next_released = item.nextReleased,
                is_complete = item.isComplete,
                is_new_episode = item.isNewEpisode,
                updated_at = item.updatedAt
            )
        },
        cloudToLocal = { cloud ->
            SeriesNextUpEntity(
                seriesId = cloud.series_id,
                title = cloud.title,
                poster = cloud.poster,
                nextSeason = cloud.next_season,
                nextEpisode = cloud.next_episode,
                nextEpisodeTitle = cloud.next_episode_title,
                nextReleased = cloud.next_released,
                isComplete = cloud.is_complete,
                isNewEpisode = cloud.is_new_episode,
                updatedAt = cloud.updated_at
            )
        },
        upsertLocal = { items -> items.forEach { dao.upsertSeriesNextUp(it) } },
        upsertCloud = { items ->
            supabase.from("series_next_up").upsert(items) { select() }
        },
        localId = { it.seriesId },
        cloudId = { it.series_id }
    )

    // ── Generic sync engine ──

    private suspend fun <TLocal, TCloud> syncTable(
        tableName: String,
        localFetch: suspend () -> List<TLocal>,
        remoteFetch: suspend () -> List<TCloud>,
        localToCloud: (TLocal) -> TCloud,
        cloudToLocal: (TCloud) -> TLocal,
        upsertLocal: suspend (List<TLocal>) -> Unit,
        upsertCloud: suspend (List<TCloud>) -> Unit,
        localId: (TLocal) -> String,
        cloudId: (TCloud) -> String
    ): SyncResult {
        var pushed = 0
        var pulled = 0
        var errors = 0

        try {
            val localItems = localFetch()
            val remoteItems = remoteFetch()

            val localMap = localItems.associateBy { localId(it) }
            val remoteMap = remoteItems.associateBy { cloudId(it) }

            val allIds = (localMap.keys + remoteMap.keys).toSet()

            val toPush = mutableListOf<TLocal>()
            val toPull = mutableListOf<TCloud>()

            for (id in allIds) {
                val local = localMap[id]
                val remote = remoteMap[id]

                when {
                    local != null && remote == null -> toPush.add(local)
                    local == null && remote != null -> toPull.add(remote)
                    local != null && remote != null -> {
                        val localTime = getTimestamp(local)
                        val remoteTime = getTimestamp(remote)
                        if (localTime > remoteTime) {
                            toPush.add(local)
                        } else if (remoteTime > localTime) {
                            toPull.add(remote)
                        }
                    }
                }
            }

            if (toPush.isNotEmpty()) {
                try {
                    upsertCloud(toPush.map(localToCloud))
                    pushed = toPush.size
                    Log.d(TAG, "$tableName: pushed $pushed items")
                } catch (e: Exception) {
                    Log.w(TAG, "$tableName: push failed", e)
                    errors++
                }
            }

            if (toPull.isNotEmpty()) {
                try {
                    upsertLocal(toPull.map(cloudToLocal))
                    pulled = toPull.size
                    Log.d(TAG, "$tableName: pulled $pulled items")
                } catch (e: Exception) {
                    Log.w(TAG, "$tableName: pull failed", e)
                    errors++
                }
            }

            if (toPush.isEmpty() && toPull.isEmpty()) {
                Log.d(TAG, "$tableName: no changes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "$tableName sync failed", e)
            errors++
        }

        return SyncResult(pushed, pulled, errors, null)
    }

    suspend fun pushWatchProgress(entity: WatchHistoryEntity) {
        if (!sessionStore.syncEnabled || !authManager.isConnected()) return
        try {
            val cloud = CloudWatchHistory(
                id = entity.id,
                title = entity.title,
                poster = entity.poster,
                background = entity.background,
                logo = entity.logo,
                position = entity.position,
                duration = entity.duration,
                last_watched = entity.lastWatched,
                type = entity.type,
                watched = entity.watched,
                scrobbled = entity.scrobbled,
                updated_at = System.currentTimeMillis()
            )
            supabase.from("watch_history").upsert(listOf(cloud)) { select() }
            Log.d(TAG, "Pushed watch progress: ${entity.id}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push watch progress", e)
        }
    }

    suspend fun pushWatchlistAdd(entity: WatchlistEntity) {
        if (!sessionStore.syncEnabled || !authManager.isConnected()) return
        try {
            val cloud = CloudWatchlist(
                id = entity.id,
                type = entity.type,
                title = entity.title,
                poster = entity.poster,
                added_at = entity.addedAt,
                updated_at = System.currentTimeMillis()
            )
            supabase.from("watchlist").upsert(listOf(cloud)) { select() }
            Log.d(TAG, "Pushed watchlist add: ${entity.id}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push watchlist add", e)
        }
    }

    suspend fun pushWatchlistRemove(id: String) {
        if (!sessionStore.syncEnabled || !authManager.isConnected()) return
        try {
            supabase.from("watchlist").delete { filter { eq("id", id) } }
            Log.d(TAG, "Pushed watchlist remove: $id")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push watchlist remove", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getTimestamp(item: T): Long {
        return when (item) {
            is CloudWatchHistory -> item.updated_at
            is CloudWatchlist -> item.updated_at
            is CloudHubRow -> item.updated_at
            is CloudHubRowItem -> item.updated_at
            is CloudAddon -> item.updated_at
            is CloudSeriesNextUp -> item.updated_at
            is WatchHistoryEntity -> item.lastWatched
            is WatchlistEntity -> item.addedAt
            is HubRowEntity -> item.createdAt
            is HubRowItemEntity -> 0L
            is AddonEntity -> 0L
            is SeriesNextUpEntity -> item.updatedAt
            else -> 0L
        }
    }
}

data class SyncResult(
    val pushed: Int,
    val pulled: Int,
    val errors: Int,
    val errorMessage: String?
) {
    val totalSynced: Int get() = pushed + pulled
    val isSuccess: Boolean get() = errors == 0
}
