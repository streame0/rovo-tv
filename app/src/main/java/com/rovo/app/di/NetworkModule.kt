package com.rovo.app.di

import com.rovo.app.BuildConfig
import com.rovo.app.data.remote.StremioApiService
import com.rovo.app.data.remote.IntroDbService
import com.rovo.app.data.remote.TmdbApiService
import com.rovo.app.data.remote.TraktApiService
import com.rovo.app.data.remote.TraktSyncApiService
import com.rovo.app.data.trakt.TraktAuthInterceptor
import com.rovo.app.data.trakt.TraktAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TraktRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TraktAuthenticatedRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://stremio-addons.netlify.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @TmdbRetrofit
    fun provideTmdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStremioApi(retrofit: Retrofit): StremioApiService {
        return retrofit.create(StremioApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIntroDbService(retrofit: Retrofit): IntroDbService {
        return retrofit.create(IntroDbService::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbApiService(@TmdbRetrofit retrofit: Retrofit): TmdbApiService {
        return retrofit.create(TmdbApiService::class.java)
    }

    @Provides
    @Singleton
    @TraktRetrofit
    fun provideTraktRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val client = okHttpClient.newBuilder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()
        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTraktApiService(@TraktRetrofit retrofit: Retrofit): TraktApiService {
        return retrofit.create(TraktApiService::class.java)
    }

    @Provides
    @Singleton
    @TraktAuthenticatedRetrofit
    fun provideTraktAuthenticatedRetrofit(
        okHttpClient: OkHttpClient,
        traktAuthInterceptor: TraktAuthInterceptor,
        traktAuthenticator: TraktAuthenticator
    ): Retrofit {
        val authenticatedClient = okHttpClient.newBuilder()
            .addInterceptor(traktAuthInterceptor)
            .authenticator(traktAuthenticator)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTraktSyncApiService(@TraktAuthenticatedRetrofit retrofit: Retrofit): TraktSyncApiService {
        return retrofit.create(TraktSyncApiService::class.java)
    }
}
