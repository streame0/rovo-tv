package com.rovo.app.data.supabase

import kotlinx.serialization.Serializable

@Serializable
data class CloudWatchHistory(
    val id: String,
    val title: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val position: Long,
    val duration: Long,
    val last_watched: Long,
    val type: String,
    val watched: Boolean = false,
    val scrobbled: Boolean = false,
    val profile_id: Int = 1,
    val updated_at: Long = System.currentTimeMillis()
)

@Serializable
data class CloudWatchlist(
    val id: String,
    val type: String,
    val title: String,
    val poster: String? = null,
    val added_at: Long,
    val profile_id: Int = 1,
    val updated_at: Long = System.currentTimeMillis()
)

@Serializable
data class CloudHubRow(
    val id: String,
    val title: String = "Hub Row",
    val shape: String,
    val show_in_home: Boolean = false,
    val show_in_movies: Boolean = false,
    val show_in_series: Boolean = false,
    val home_order: Int = 999,
    val movies_order: Int = 999,
    val series_order: Int = 999,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

@Serializable
data class CloudHubRowItem(
    val hub_row_id: String,
    val config_unique_id: String,
    val title: String,
    val custom_image_url: String? = null,
    val item_order: Int = 0,
    val updated_at: Long = System.currentTimeMillis()
)

@Serializable
data class CloudAddon(
    val transport_url: String,
    val id: String,
    val name: String? = null,
    val version: String,
    val description: String? = null,
    val icon_url: String? = null,
    val is_trusted: Boolean = false,
    val is_enabled: Boolean = true,
    val nickname: String? = null,
    val catalogs_json: String = "[]",
    val supports_meta: Boolean = false,
    val supports_stream: Boolean = true,
    val types_json: String = "[]",
    val id_prefixes_json: String = "[]",
    val sort_order: Int = 999,
    val updated_at: Long = System.currentTimeMillis()
)

@Serializable
data class CloudSeriesNextUp(
    val series_id: String,
    val title: String,
    val poster: String? = null,
    val next_season: Int,
    val next_episode: Int,
    val next_episode_title: String? = null,
    val next_released: String? = null,
    val is_complete: Boolean = false,
    val is_new_episode: Boolean = false,
    val updated_at: Long
)

@Serializable
data class CloudProfile(
    val local_id: Int,
    val name: String,
    val theme_id: String = "void",
    val avatar_ref: String = "avatar_1",
    val updated_at: Long = System.currentTimeMillis()
)
