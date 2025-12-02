package edu.sswu.vitaday

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionDao {

    @Query("SELECT * FROM TimerSession ORDER BY date DESC")
    fun getAllSessions(): Flow<List<TimerSessionEntity>>

    @Query("SELECT * FROM TimerSession WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getSessionsBySubject(subjectId: Int): Flow<List<TimerSessionEntity>>

    @Query("SELECT * FROM TimerSession WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date DESC")
    suspend fun getSessionsByDate(startOfDay: Long, endOfDay: Long): List<TimerSessionEntity>

    @Query("SELECT * FROM TimerSession WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getSessionsByDateRange(startDate: Long, endDate: Long): List<TimerSessionEntity>

    @Query("""
        SELECT COALESCE(SUM(duration), 0) 
        FROM TimerSession 
        WHERE date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalDurationForPeriod(startDate: Long, endDate: Long): Long

    @Query("""
        SELECT subjectId, subjectName, SUM(duration) as totalDuration 
        FROM TimerSession 
        WHERE date >= :startDate AND date <= :endDate 
        GROUP BY subjectId
        ORDER BY totalDuration DESC
    """)
    suspend fun getSubjectDurationsForPeriod(startDate: Long, endDate: Long): List<SubjectDuration>

    /**
     * ⭐ 완벽히 수정된 Daily Duration 그룹핑 쿼리 ⭐
     * 밀리초 기반 timestamp(date)를 → 하루 단위 timestamp로 변환하여 정확히 그룹핑함
     */
    @Query("""
        SELECT 
            (date / 86400000) * 86400000 AS date, 
            SUM(duration) AS totalDuration
        FROM TimerSession
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date / 86400000
        ORDER BY date ASC
    """)
    suspend fun getDailyDurationsForPeriod(startDate: Long, endDate: Long): List<DailyDuration>

    @Query("SELECT COUNT(*) FROM TimerSession")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT COUNT(*) FROM TimerSession WHERE date >= :startDate AND date <= :endDate")
    suspend fun getSessionCountForPeriod(startDate: Long, endDate: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimerSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<TimerSessionEntity>)

    @Delete
    suspend fun deleteSession(session: TimerSessionEntity)

    @Query("DELETE FROM TimerSession WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM TimerSession WHERE subjectId = :subjectId")
    suspend fun deleteSessionsBySubject(subjectId: Int)

    @Query("DELETE FROM TimerSession")
    suspend fun deleteAllSessions()
}

data class SubjectDuration(
    val subjectId: Int,
    val subjectName: String,
    val totalDuration: Long
)

data class DailyDuration(
    val date: Long,
    val totalDuration: Long
)
