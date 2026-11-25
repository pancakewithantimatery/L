package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "research_sessions")
data class ResearchSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val topic: String,
    val domain: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalCycles: Int = 0,
    val activeTheoristModel: String = "llama-3.3-70b",
    val activeSkepticModel: String = "deepseek-r1-671b",
    val activeSynthesizerModel: String = "qwen-2.5-72b",
    val status: String = "IDLE" // IDLE, RUNNING, COMPLETED
)

@Entity(
    tableName = "evolution_cycles",
    foreignKeys = [
        ForeignKey(
            entity = ResearchSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class EvolutionCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cycleNumber: Int,
    val theoristModel: String,
    val skepticModel: String,
    val synthesizerModel: String,
    val userGuidance: String? = null,
    val status: String = "COMPLETED", // RUNNING, COMPLETED, FAILED
    val viabilityScore: Int = 0,
    val hypothesisTitle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "agent_messages",
    foreignKeys = [
        ForeignKey(
            entity = ResearchSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["cycleId"])]
)
data class AgentMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cycleId: Long,
    val role: String, // THEORIST, SKEPTIC, SYNTHESIZER, HUMAN
    val modelName: String,
    val agentTitle: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val turnNumber: Int = 1
)

@Entity(
    tableName = "scientific_hypotheses",
    foreignKeys = [
        ForeignKey(
            entity = ResearchSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["cycleId"])]
)
data class ScientificHypothesisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cycleId: Long,
    val title: String,
    val thesis: String,
    val mechanism: String,
    val theoreticalBasis: String,
    val noveltyScore: Int = 85,
    val confidenceScore: Int = 75,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "verification_scenarios",
    foreignKeys = [
        ForeignKey(
            entity = ResearchSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["cycleId"])]
)
data class VerificationScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cycleId: Long,
    val hypothesisId: Long = 0,
    val title: String,
    val methodology: String,
    val experimentalSetup: String,
    val falsificationCriteria: String,
    val controlGroups: String,
    val metricsAndThresholds: String,
    val feasibilityScore: Int = 80,
    val createdAt: Long = System.currentTimeMillis()
)
