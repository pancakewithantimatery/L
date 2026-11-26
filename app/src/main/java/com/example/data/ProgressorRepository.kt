package com.example.data

import com.example.data.local.AgentMessageEntity
import com.example.data.local.CustomAgentRoleEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ProgressorDao
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificCitationEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import kotlinx.coroutines.flow.Flow

class ProgressorRepository(private val dao: ProgressorDao) {

    val allSessions: Flow<List<ResearchSessionEntity>> = dao.getAllSessions()
    val allCustomRoles: Flow<List<CustomAgentRoleEntity>> = dao.getAllCustomRoles()

    fun getCyclesForSession(sessionId: Long): Flow<List<EvolutionCycleEntity>> =
        dao.getCyclesForSession(sessionId)

    fun getMessagesForSession(sessionId: Long): Flow<List<AgentMessageEntity>> =
        dao.getMessagesForSession(sessionId)

    fun getHypothesesForSession(sessionId: Long): Flow<List<ScientificHypothesisEntity>> =
        dao.getHypothesesForSession(sessionId)

    fun getScenariosForSession(sessionId: Long): Flow<List<VerificationScenarioEntity>> =
        dao.getScenariosForSession(sessionId)

    fun getCitationsForSession(sessionId: Long): Flow<List<ScientificCitationEntity>> =
        dao.getCitationsForSession(sessionId)

    suspend fun getCitationsForSessionOnce(sessionId: Long): List<ScientificCitationEntity> =
        dao.getCitationsForSessionOnce(sessionId)

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

    // --- Scientific Citations ---
    suspend fun insertCitation(citation: ScientificCitationEntity): Long =
        dao.insertCitation(citation)

    suspend fun deleteCitation(citationId: Long) =
        dao.deleteCitationById(citationId)

    // --- Custom Agent Roles ---
    suspend fun ensureDefaultCustomRolesSeeded() {
        val existing = dao.getAllCustomRolesOnce()
        if (existing.isEmpty()) {
            val presets = listOf(
                CustomAgentRoleEntity(
                    name = "Рецензент №2 (Nature/Science)",
                    baseSlot = "SKEPTIC",
                    systemPrompt = "Ты — бескомпромиссный анонимный рецензент топ-журнала Nature/Science. Ищи скрытые артефакты измерений, p-hacking, ошибки воспроизводимости и требуй строжайших контрольных экспериментов.",
                    temperature = 0.3f,
                    accentColorHex = "#F43F5E",
                    isActive = false,
                    isPreset = true
                ),
                CustomAgentRoleEntity(
                    name = "Патентный поверенный R&D",
                    baseSlot = "SYNTHESIZER",
                    systemPrompt = "Ты — ведущий патентный поверенный и инженер по коммерциализации технологий. Оценивай изобретательский уровень, промышленную применимость и формулируй защищаемые формулы изобретения.",
                    temperature = 0.5f,
                    accentColorHex = "#38BDF8",
                    isActive = false,
                    isPreset = true
                ),
                CustomAgentRoleEntity(
                    name = "Междисциплинарный визионер",
                    baseSlot = "THEORIST",
                    systemPrompt = "Ты — ученый-энциклопедист на стыке теоретической физики, системной биологии и теории информации. Строй смелые изоморфизмы между далекими областями науки и выдвигай проверяемые прорывные гипотезы.",
                    temperature = 0.85f,
                    accentColorHex = "#A855F7",
                    isActive = false,
                    isPreset = true
                )
            )
            presets.forEach { dao.insertCustomRole(it) }
        }
    }

    suspend fun getActiveRoleForSlot(baseSlot: String): CustomAgentRoleEntity? =
        dao.getActiveRoleForSlot(baseSlot)

    suspend fun saveCustomRole(role: CustomAgentRoleEntity): Long {
        if (role.isActive) {
            dao.deactivateRolesForSlot(role.baseSlot)
        }
        return dao.insertCustomRole(role)
    }

    suspend fun toggleCustomRoleActivation(role: CustomAgentRoleEntity) {
        if (role.isActive) {
            dao.setRoleActive(role.id, false)
        } else {
            dao.deactivateRolesForSlot(role.baseSlot)
            dao.setRoleActive(role.id, true)
        }
    }

    suspend fun deleteCustomRole(roleId: Long) = dao.deleteCustomRoleById(roleId)
}
