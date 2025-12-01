package edu.sswu.vitaday

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM Subject WHERE userId = :userId ORDER BY subjectId ASC")
    fun getAllSubjectsByUser(userId: Int): Flow<List<Subject>>

    @Query("SELECT * FROM Subject WHERE subjectId = :subjectId")
    suspend fun getSubjectById(subjectId: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM Subject WHERE subjectId = :subjectId")
    suspend fun deleteSubjectById(subjectId: Int)

    @Query("DELETE FROM Subject WHERE userId = :userId")
    suspend fun deleteAllSubjectsByUser(userId: Int)
}