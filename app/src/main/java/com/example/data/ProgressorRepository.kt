package com.example.data

import com.example.data.local.AgentMessageEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ProgressorDao
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import kotlinx.coroutines.flow.Flow

class ProgressorRepository(private val dao: ProgressorDao) {

    val allSessions: Flow<List<ResearchSessionEntity>> = dao.getAllSessions()

    fun getCyclesForSession(sessionId: Long): Flow<List<EvolutionCycleEntity>> =
        dao.getCyclesForSession(sessionId)

    fun getMessagesForSession(sessionId: Long): Flow<List<AgentMessageEntity>> =
        dao.getMessagesForSession(sessionId)

    fun getHypothesesForSession(sessionId: Long): Flow<List<ScientificHypothesisEntity>> =
        dao.getHypothesesForSession(sessionId)

    fun getScenariosForSession(sessionId: Long): Flow<List<VerificationScenarioEntity>> =
        dao.getScenariosForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): ResearchSessionEntity? =
        dao.getSessionById(sessionId)

    suspend fun createSession(
        topic: String,
        domain: String,
        theoristModel: String,
        skepticModel: String,
        synthesizerModel: String
    ): Long {
        val title = if (topic.length > 50) topic.take(47) + "..." else topic
        val session = ResearchSessionEntity(
            title = title,
            topic = topic,
            domain = domain,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            totalCycles = 0,
            activeTheoristModel = theoristModel,
            activeSkepticModel = skepticModel,
            activeSynthesizerModel = synthesizerModel,
            status = "IDLE"
        )
        return dao.insertSession(session)
    }

    suspend fun updateSessionModels(
        sessionId: Long,
        theoristModel: String,
        skepticModel: String,
        synthesizerModel: String
    ) {
        val existing = dao.getSessionById(sessionId) ?: return
        dao.updateSession(
            existing.copy(
                activeTheoristModel = theoristModel,
                activeSkepticModel = skepticModel,
                activeSynthesizerModel = synthesizerModel,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSessionStatus(sessionId: Long, status: String, totalCycles: Int? = null) {
        val existing = dao.getSessionById(sessionId) ?: return
        dao.updateSession(
            existing.copy(
                status = status,
                totalCycles = totalCycles ?: existing.totalCycles,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun insertCycle(cycle: EvolutionCycleEntity): Long = dao.insertCycle(cycle)

    suspend fun updateCycle(cycle: EvolutionCycleEntity) = dao.updateCycle(cycle)

    suspend fun insertMessage(message: AgentMessageEntity): Long = dao.insertMessage(message)

    suspend fun insertHypothesis(hypothesis: ScientificHypothesisEntity): Long =
        dao.insertHypothesis(hypothesis)

    suspend fun insertScenario(scenario: VerificationScenarioEntity): Long =
        dao.insertScenario(scenario)

    suspend fun deleteSession(sessionId: Long) = dao.deleteSessionById(sessionId)

    suspend fun getLatestCycle(sessionId: Long): EvolutionCycleEntity? =
        dao.getLatestCycle(sessionId)
}
