package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContentDto>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContentDto? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfigDto? = GeminiGenerationConfigDto(0.7, 2500)
)

@JsonClass(generateAdapter = true)
data class GeminiContentDto(
    val role: String? = "user",
    val parts: List<GeminiPartDto>
)

@JsonClass(generateAdapter = true)
data class GeminiPartDto(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfigDto(
    val temperature: Double? = 0.7,
    val maxOutputTokens: Int? = 2500
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidateDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateDto(
    val content: GeminiContentDto? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelsListResponse(
    val models: List<GeminiModelItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelItemDto(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null,
    val inputTokenLimit: Int? = null,
    val outputTokenLimit: Int? = null
)
