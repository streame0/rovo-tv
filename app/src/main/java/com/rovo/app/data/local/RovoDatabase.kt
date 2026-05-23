package com.rovo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rovo.app.data.model.AddonEntity
import com.rovo.app.data.model.CatalogConfigEntity
import com.rovo.app.data.model.HubRowEntity
import com.rovo.app.data.model.HubRowItemEntity
import com.rovo.app.data.model.ProfileEntity
import com.rovo.app.data.model.ThemeEntity
import com.rovo.app.data.model.SeriesNextUpEntity
import com.rovo.app.data.model.WatchHistoryEntity
import com.rovo.app.data.model.WatchlistEntity


@Database(
    entities = [
        AddonEntity::class,
        ProfileEntity::class,
        WatchHistoryEntity::class,
        CatalogConfigEntity::class,
        ThemeEntity::class,
        HubRowEntity::class,
        HubRowItemEntity::class,
        WatchlistEntity::class,
        SeriesNextUpEntity::class
    ],
    version = 45
)
abstract class RovoDatabase : RoomDatabase() {
    abstract fun addonDao(): AddonDao
}