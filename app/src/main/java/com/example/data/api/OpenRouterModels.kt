package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessageDto>,
    val temperature: Double? = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int? = 2500
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenRouterChoiceDto>? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoiceDto(
    val index: Int? = null,
    val message: OpenRouterMessageDto? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModelItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelItemDto(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    @Json(name = "context_length") val contextLength: Int? = null
)
