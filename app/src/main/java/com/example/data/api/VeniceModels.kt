package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VeniceChatRequest(
    val model: String,
    val messages: List<VeniceMessageDto>,
    val temperature: Double? = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int? = 2500,
    @Json(name = "venice_parameters") val veniceParameters: VeniceParametersDto? = VeniceParametersDto(false)
)

@JsonClass(generateAdapter = true)
data class VeniceParametersDto(
    @Json(name = "include_venice_system_prompt") val include_venice_system_prompt: Boolean = false
)

@JsonClass(generateAdapter = true)
data class VeniceMessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class VeniceChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<VeniceChoiceDto>? = null
)

@JsonClass(generateAdapter = true)
data class VeniceChoiceDto(
    val index: Int? = null,
    val message: VeniceMessageDto? = null,
    @Json(name = "finish_reason") val finish_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class VeniceModelsResponse(
    val data: List<VeniceModelDto>? = null
)

@JsonClass(generateAdapter = true)
data class VeniceModelDto(
    val id: String,
    val type: String? = null
)
