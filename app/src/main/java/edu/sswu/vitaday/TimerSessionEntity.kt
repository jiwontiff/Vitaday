package edu.sswu.vitaday

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "TimerSession",
/*
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
 */
    indices = [Index(value = ["subjectId"]), Index(value = ["date"])]
)
data class TimerSessionEntity(
    @PrimaryKey
    val sessionId: String,

    val subjectId: Int,
    val subjectName: String,
    val duration: Long,
    val date: Long,
    val startTime: Long?,
    val endTime: Long?
)