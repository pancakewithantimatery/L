package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressorDao {

    // Sessions
    @Query("SELECT * FROM research_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ResearchSessionEntity>>

    @Query("SELECT * FROM research_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ResearchSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ResearchSessionEntity): Long

    @Update
    suspend fun updateSession(session: ResearchSessionEntity)

    @Query("DELETE FROM research_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    // Cycles
    @Query("SELECT * FROM evolution_cycles WHERE sessionId = :sessionId ORDER BY cycleNumber ASC")
    fun getCyclesForSession(sessionId: Long): Flow<List<EvolutionCycleEntity>>

    @Query("SELECT * FROM evolution_cycles WHERE sessionId = :sessionId ORDER BY cycleNumber DESC LIMIT 1")
    suspend fun getLatestCycle(sessionId: Long): EvolutionCycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: EvolutionCycleEntity): Long

    @Update
    suspend fun updateCycle(cycle: EvolutionCycleEntity)

    // Messages
    @Query("SELECT * FROM agent_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<AgentMessageEntity>>

    @Query("SELECT * FROM agent_messages WHERE cycleId = :cycleId ORDER BY timestamp ASC")
    fun getMessagesForCycle(cycleId: Long): Flow<List<AgentMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity): Long

    // Hypotheses
    @Query("SELECT * FROM scientific_hypotheses WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getHypothesesForSession(sessionId: Long): Flow<List<ScientificHypothesisEntity>>

    @Query("SELECT * FROM scientific_hypotheses WHERE cycleId = :cycleId LIMIT 1")
    suspend fun getHypothesisForCycle(cycleId: Long): ScientificHypothesisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHypothesis(hypothesis: ScientificHypothesisEntity): Long

    // Verification Scenarios
    @Query("SELECT * FROM verification_scenarios WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getScenariosForSession(sessionId: Long): Flow<List<VerificationScenarioEntity>>

    @Query("SELECT * FROM verification_scenarios WHERE cycleId = :cycleId")
    suspend fun getScenariosForCycle(cycleId: Long): List<VerificationScenarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: VerificationScenarioEntity): Long

    // Scientific Citations (arXiv / DOI)
    @Query("SELECT * FROM scientific_citations WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getCitationsForSession(sessionId: Long): Flow<List<ScientificCitationEntity>>

    @Query("SELECT * FROM scientific_citations WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun getCitationsForSessionOnce(sessionId: Long): List<ScientificCitationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitation(citation: ScientificCitationEntity): Long

    @Query("DELETE FROM scientific_citations WHERE id = :citationId")
    suspend fun deleteCitationById(citationId: Long)

    // Custom Agent Roles
    @Query("SELECT * FROM custom_agent_roles ORDER BY isActive DESC, createdAt DESC")
    fun getAllCustomRoles(): Flow<List<CustomAgentRoleEntity>>

    @Query("SELECT * FROM custom_agent_roles")
    suspend fun getAllCustomRolesOnce(): List<CustomAgentRoleEntity>

    @Query("SELECT * FROM custom_agent_roles WHERE baseSlot = :baseSlot AND isActive = 1 LIMIT 1")
    suspend fun getActiveRoleForSlot(baseSlot: String): CustomAgentRoleEntity?

    @Query("UPDATE custom_agent_roles SET isActive = 0 WHERE baseSlot = :baseSlot")
    suspend fun deactivateRolesForSlot(baseSlot: String)

    @Query("UPDATE custom_agent_roles SET isActive = :isActive WHERE id = :roleId")
    suspend fun setRoleActive(roleId: Long, isActive: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomRole(role: CustomAgentRoleEntity): Long

    @Update
    suspend fun updateCustomRole(role: CustomAgentRoleEntity)

    @Query("DELETE FROM custom_agent_roles WHERE id = :roleId")
    suspend fun deleteCustomRoleById(roleId: Long)
}

