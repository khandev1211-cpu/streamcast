package com.streamcast.core.network.di

import com.streamcast.core.network.IptvApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideIptvApiClient(okHttpClient: OkHttpClient): IptvApiClient {
        // The base URL will be dynamic for Xtream Codes, so we use a dummy one here
        // and override it using a custom call or by using a dynamic base URL interceptor.
        // For simplicity, we'll assume a generic builder that can be used.
        return Retrofit.Builder()
            .baseUrl("https://dummy.com/") // Placeholder
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IptvApiClient::class.java)
    }
}
