package edu.sswu.vitaday

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 과목 테이블
 * User와 1:N 관계 (한 유저가 여러 과목 소유)
 * User 삭제 시 해당 유저의 모든 과목도 삭제 (CASCADE)
 */
@Entity(
    tableName = "Subject",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val subjectId: Int = 0,

    val userId: Int,
    val name: String,
    val colorHex: String
)