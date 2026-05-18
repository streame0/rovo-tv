package com.rovo.app.data.remote

import com.rovo.app.data.model.stremio.CatalogResponse
import com.rovo.app.data.model.stremio.Manifest
import com.rovo.app.data.model.stremio.MetaResponse
import com.rovo.app.data.model.stremio.SubtitleResponse
import com.rovo.app.data.model.stremio.StreamResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface StremioApiService {

    @GET
    suspend fun getManifest(@Url url: String): Manifest

    @GET
    suspend fun getCatalog(@Url url: String): CatalogResponse

    @GET
    suspend fun getMeta(@Url url: String): MetaResponse

    @GET
    suspend fun getStreams(@Url url: String): StreamResponse

    @GET
    suspend fun getSubtitles(@Url url: String): SubtitleResponse
}
