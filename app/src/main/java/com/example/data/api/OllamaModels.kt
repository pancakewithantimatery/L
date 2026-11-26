package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaTagsResponse(
    val models: List<OllamaModelTagDto>? = null
)

@JsonClass(generateAdapter = true)
data class OllamaModelTagDto(
    val name: String,
    val model: String? = null,
    val size: Long? = null,
    val details: OllamaModelDetailsDto? = null
)

@JsonClass(generateAdapter = true)
data class OllamaModelDetailsDto(
    val family: String? = null,
    @Json(name = "parameter_size") val parameterSize: String? = null,
    @Json(name = "quantization_level") val quantizationLevel: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaVersionResponse(
    val version: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaChatMessageDto>,
    val stream: Boolean = false,
    val options: OllamaOptionsDto? = OllamaOptionsDto(0.7)
)

@JsonClass(generateAdapter = true)
data class OllamaChatMessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OllamaOptionsDto(
    val temperature: Double? = 0.7
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    val model: String? = null,
    val message: OllamaChatMessageDto? = null,
    val done: Boolean? = true
)
