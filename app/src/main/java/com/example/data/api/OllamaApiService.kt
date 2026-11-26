package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface OllamaApiService {

    @GET
    suspend fun listModels(@Url url: String): Response<OllamaTagsResponse>

    @POST
    suspend fun chat(@Url url: String, @Body request: OllamaChatRequest): Response<OllamaChatResponse>

    @GET
    suspend fun checkVersion(@Url url: String): Response<OllamaVersionResponse>

    companion object {
        // Dummy baseUrl required by Retrofit when using @Url
        private const val DEFAULT_BASE_URL = "http://localhost/"

        fun create(): OllamaApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS) // Local LLM inference might take time
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(OllamaApiService::class.java)
        }
    }
}
