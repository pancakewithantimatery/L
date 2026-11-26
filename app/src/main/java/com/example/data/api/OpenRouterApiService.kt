package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenRouterApiService {

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): Response<OpenRouterModelsResponse>

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://progressor.ai",
        @Header("X-Title") appTitle: String = "Progressor AI Research",
        @Body request: OpenRouterChatRequest
    ): Response<OpenRouterChatResponse>

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"

        fun create(): OpenRouterApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(OpenRouterApiService::class.java)
        }
    }
}
