package com.rovo.app.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "watchlist", primaryKeys = ["id", "profileId"])
data class WatchlistEntity(
    val id: String,       // IMDb or addon ID (e.g., "tt0111161")
    val profileId: Int,
    val type: String,                 // "movie" or "series"
    val title: String,
    val poster: String?,
    val addedAt: Long                 // System.currentTimeMillis() when bookmarked
)
