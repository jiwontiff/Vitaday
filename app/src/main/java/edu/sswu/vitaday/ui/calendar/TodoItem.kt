package edu.sswu.vitaday.ui.calendar

import java.util.Date
import java.util.UUID

/**
 * Todo 아이템 데이터 클래스
 */
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val date: Date,
    val isCompleted: Boolean = false,
    val isRepeat: Boolean = false,
    val createdAt: Date = Date()
)