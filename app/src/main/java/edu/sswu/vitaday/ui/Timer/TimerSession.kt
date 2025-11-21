package edu.sswu.vitaday.ui.timer

import java.util.Date

/**
 * 타이머 세션 데이터 클래스
 * 각 공부 세션의 정보를 저장
 */
data class TimerSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subjectId: Int,           // 과목 ID
    val subjectName: String,      // 과목 이름
    val duration: Long,           // 공부 시간 (밀리초)
    val date: Date = Date(),      // 세션 날짜
    val startTime: Date? = null,  // 시작 시간
    val endTime: Date? = null     // 종료 시간
)