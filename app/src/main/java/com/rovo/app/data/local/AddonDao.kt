package com.rovo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rovo.app.data.model.AddonEntity
import com.rovo.app.data.model.CatalogConfigEntity
import com.rovo.app.data.model.HubRowEntity
import com.rovo.app.data.model.HubRowWithItems
import com.rovo.app.data.model.HubRowItemEntity
import com.rovo.app.data.model.ProfileEntity
import com.rovo.app.data.model.ThemeEntity
import com.rovo.app.data.model.SeriesNextUpEntity
import com.rovo.app.data.model.WatchHistoryEntity
import com.rovo.app.data.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import androidx.room.Delete

@Dao
interface AddonDao {

    @Query("SELECT * FROM addons ORDER BY sortOrder ASC")
    fun getAllAddons(): Flow<List<AddonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddon(addon: AddonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddons(addons: List<AddonEntity>)

    @Query("DELETE FROM addons WHERE transportUrl = :url")
    suspend fun deleteAddonByUrl(url: String)

    @Query("DELETE FROM addons")
    suspend fun clearAddons()

    @Query("SELECT * FROM addons WHERE transportUrl = :transportUrl")
    suspend fun getAddon(transportUrl: String): AddonEntity?

    @Query("SELECT * FROM catalog_configs")
    fun getAllCatalogConfigs(): Flow<List<CatalogConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalogConfig(config: CatalogConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalogConfigs(configs: List<CatalogConfigEntity>)

    @Query("DELETE FROM catalog_configs WHERE transportUrl = :url")
    suspend fun deleteCatalogConfigs(url: String)

    @Query("DELETE FROM catalog_configs")
    suspend fun clearCatalogConfigs()

    @Query("SELECT * FROM catalog_configs WHERE uniqueId = :uniqueId")
    suspend fun getCatalogConfig(uniqueId: String): CatalogConfigEntity?

    @Query("SELECT * FROM profiles")
    fun getProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileFlow(id: Int): Flow<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: Int)

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatched DESC")
    fun getWatchHistory(profileId: Int): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId")
    suspend fun getAllWatchHistoryOnce(profileId: Int): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoryItems(items: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearWatchHistory(profileId: Int)

    @Query("SELECT * FROM watch_history WHERE id = :id AND profileId = :profileId")
    suspend fun getHistoryItem(id: String, profileId: Int): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE id LIKE :prefix || '%' AND profileId = :profileId")
    suspend fun getHistoryItemsByPrefix(prefix: String, profileId: Int): List<WatchHistoryEntity>

    @Query(
        "SELECT * FROM watch_history " +
            "WHERE type = 'series' AND id LIKE :episodePrefix AND profileId = :profileId " +
            "ORDER BY lastWatched DESC LIMIT 1"
    )
    suspend fun getLatestSeriesEpisodeHistory(episodePrefix: String, profileId: Int): WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE id = :id AND profileId = :profileId")
    suspend fun deleteHistoryItem(id: String, profileId: Int)

    @Query("SELECT * FROM watch_history WHERE type = 'series' AND id LIKE :episodePrefix AND profileId = :profileId")
    suspend fun getSeriesEpisodeHistory(episodePrefix: String, profileId: Int): List<WatchHistoryEntity>

    @Query("DELETE FROM watch_history WHERE type = 'series' AND id LIKE :episodePrefix AND profileId = :profileId")
    suspend fun deleteSeriesHistory(episodePrefix: String, profileId: Int)

    @Query("SELECT * FROM themes")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getThemeById(id: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity)

    @Delete
    suspend fun deleteTheme(theme: ThemeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRow(row: HubRowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRows(rows: List<HubRowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRowItems(items: List<HubRowItemEntity>)

    @Transaction
    suspend fun insertHubRowWithItems(row: HubRowEntity, items: List<HubRowItemEntity>) {
        insertHubRow(row)
        insertHubRowItems(items)
    }

    @Query("SELECT * FROM hub_rows ORDER BY homeOrder ASC, createdAt ASC")
    fun getAllHubRows(): Flow<List<HubRowEntity>>

    @Query("SELECT * FROM hub_row_items ORDER BY itemOrder ASC")
    fun getAllHubRowItems(): Flow<List<HubRowItemEntity>>

    @Query("DELETE FROM hub_rows WHERE id = :hubRowId")
    suspend fun deleteHubRow(hubRowId: String)

    @Query("DELETE FROM hub_row_items WHERE hubRowId = :hubRowId")
    suspend fun deleteHubRowItems(hubRowId: String)

    @Query("DELETE FROM hub_row_items")
    suspend fun clearHubRowItems()

    @Query("DELETE FROM hub_rows")
    suspend fun clearHubRows()

    @Transaction
    suspend fun deleteHubRowWithItems(hubRowId: String) {
        deleteHubRowItems(hubRowId)
        deleteHubRow(hubRowId)
    }

    @Query("UPDATE hub_row_items SET customImageUrl = :imageUrl WHERE hubRowId = :hubRowId AND configUniqueId = :configUniqueId")
    suspend fun updateHubItemImage(hubRowId: String, configUniqueId: String, imageUrl: String?)

    @Transaction
    @Query("SELECT * FROM hub_rows ORDER BY homeOrder ASC, createdAt ASC")
    fun getHubRowsWithItems(): Flow<List<HubRowWithItems>>



    @Query("SELECT MAX(homeOrder) FROM hub_rows")
    suspend fun getMaxHubHomeOrder(): Int?

    @Query("SELECT MAX(moviesOrder) FROM hub_rows")
    suspend fun getMaxHubMoviesOrder(): Int?

    @Query("SELECT MAX(seriesOrder) FROM hub_rows")
    suspend fun getMaxHubSeriesOrder(): Int?



    @Update
    suspend fun updateHubRow(row: HubRowEntity)

    @Update
    suspend fun updateHubRows(rows: List<HubRowEntity>)

    @Query("DELETE FROM hub_row_items WHERE hubRowId = :hubRowId AND configUniqueId = :configUniqueId")
    suspend fun deleteHubRowItem(hubRowId: String, configUniqueId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHubRowItem(item: HubRowItemEntity)

    @Query("SELECT MAX(itemOrder) FROM hub_row_items WHERE hubRowId = :hubRowId")
    suspend fun getMaxHubItemOrder(hubRowId: String): Int?

    @Update
    suspend fun updateHubRowItem(item: HubRowItemEntity)

    @Update
    suspend fun updateHubRowItems(items: List<HubRowItemEntity>)

    // ── Watchlist ──

    @Query("SELECT * FROM watchlist WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun getWatchlist(profileId: Int): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE type = :type AND profileId = :profileId ORDER BY addedAt DESC")
    fun getWatchlistByType(type: String, profileId: Int): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE profileId = :profileId ORDER BY addedAt DESC")
    suspend fun getWatchlistOnce(profileId: Int): List<WatchlistEntity>

    @Query("DELETE FROM watchlist WHERE profileId = :profileId")
    suspend fun clearWatchlist(profileId: Int)

    @Query("SELECT * FROM watchlist WHERE id = :id AND profileId = :profileId")
    suspend fun getWatchlistItem(id: String, profileId: Int): WatchlistEntity?

    @Query("SELECT * FROM watch_history WHERE scrobbled = 1 AND watched = 0 AND profileId = :profileId")
    suspend fun getScrobbledInProgressItems(profileId: Int): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE scrobbled = 1 AND watched = 1 AND profileId = :profileId")
    suspend fun getScrobbledWatchedItems(profileId: Int): List<WatchHistoryEntity>

    @Query("SELECT id FROM watch_history WHERE watched = 1 AND profileId = :profileId")
    fun getWatchedIds(profileId: Int): Flow<List<String>>

    @Query("UPDATE watch_history SET poster = :poster, background = :background, logo = :logo WHERE id = :id AND profileId = :profileId")
    suspend fun updateHistoryImages(id: String, profileId: Int, poster: String?, background: String?, logo: String?)

    // ── Series Next Up ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeriesNextUp(entry: SeriesNextUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeriesNextUpItems(items: List<SeriesNextUpEntity>)

    @Query("DELETE FROM series_next_up WHERE profileId = :profileId")
    suspend fun clearSeriesNextUp(profileId: Int)

    @Query("SELECT * FROM series_next_up WHERE isComplete = 0 AND profileId = :profileId ORDER BY updatedAt DESC")
    fun getActiveSeriesNextUp(profileId: Int): Flow<List<SeriesNextUpEntity>>

    @Query("SELECT * FROM series_next_up WHERE seriesId = :seriesId AND profileId = :profileId")
    suspend fun getSeriesNextUp(seriesId: String, profileId: Int): SeriesNextUpEntity?

    @Query("DELETE FROM series_next_up WHERE seriesId = :seriesId AND profileId = :profileId")
    suspend fun deleteSeriesNextUp(seriesId: String, profileId: Int)

    @Query("SELECT * FROM series_next_up WHERE profileId = :profileId")
    suspend fun getAllSeriesNextUpOnce(profileId: Int): List<SeriesNextUpEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id AND profileId = :profileId)")
    suspend fun isInWatchlist(id: String, profileId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id AND profileId = :profileId)")
    fun isInWatchlistFlow(id: String, profileId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlistItems(items: List<WatchlistEntity>)

    @Query("DELETE FROM watchlist WHERE id = :id AND profileId = :profileId")
    suspend fun removeFromWatchlist(id: String, profileId: Int)

    @Transaction
    suspend fun replaceRuntimeState(
        profileId: Int,
        addons: List<AddonEntity>,
        catalogConfigs: List<CatalogConfigEntity>,
        hubRows: List<HubRowEntity>,
        hubRowItems: List<HubRowItemEntity>,
        watchHistory: List<WatchHistoryEntity>,
        watchlist: List<WatchlistEntity>,
        seriesNextUp: List<SeriesNextUpEntity>
    ) {
        // 1. Clear everything first to ensure strict per-profile isolation
        clearHubRowItems()
        clearHubRows()
        clearCatalogConfigs()
        clearAddons()
        clearWatchHistory(profileId)
        clearWatchlist(profileId)
        clearSeriesNextUp(profileId)

        // 2. Insert new state from snapshot
        if (addons.isNotEmpty()) insertAddons(addons)
        if (catalogConfigs.isNotEmpty()) saveCatalogConfigs(catalogConfigs)
        if (hubRows.isNotEmpty()) insertHubRows(hubRows)
        if (hubRowItems.isNotEmpty()) insertHubRowItems(hubRowItems)
        if (watchHistory.isNotEmpty()) upsertHistoryItems(watchHistory)
        if (watchlist.isNotEmpty()) addToWatchlistItems(watchlist)
        if (seriesNextUp.isNotEmpty()) upsertSeriesNextUpItems(seriesNextUp)
    }
}
