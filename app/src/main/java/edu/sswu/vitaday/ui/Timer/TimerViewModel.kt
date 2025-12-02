package edu.sswu.vitaday.ui.timer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 타이머 관련 ViewModel
 * 타이머 상태, 세션 기록, 통계 관리
 */
class TimerViewModel : ViewModel() {

    // 타이머 상태
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // 현재 선택된 과목
    private val _selectedSubjectId = MutableStateFlow<Int?>(null)
    val selectedSubjectId: StateFlow<Int?> = _selectedSubjectId.asStateFlow()

    // 타이머 세션 기록
    private val _sessions = MutableStateFlow<List<TimerSession>>(emptyList())
    val sessions: StateFlow<List<TimerSession>> = _sessions.asStateFlow()

    private var sessionStartTime: Date? = null
    private var totalElapsedTime = 0L

    // ⭐ 새로 추가: 마지막으로 재생(재개)한 시점의 실제 시간(ms)
    private var lastResumeTime = 0L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 과목 선택
     */
    fun selectSubject(subjectId: Int) {
        _selectedSubjectId.value = subjectId
    }

    /**
     * 타이머 시작
     */
    fun startTimer(durationMinutes: Int, subjectId: Int) {
        _remainingTime.value = durationMinutes * 60 * 1000L
        _selectedSubjectId.value = subjectId
        _isRunning.value = true
        _isPaused.value = false

        sessionStartTime = Date()
        totalElapsedTime = 0L

        // ⭐ 시작 시각 기록
        lastResumeTime = System.currentTimeMillis()
    }

    /**
     * 타이머 일시정지
     */
    fun pauseTimer() {
        if (_isRunning.value && !_isPaused.value) {
            _isPaused.value = true

            val now = System.currentTimeMillis()

            // ⭐ 일시정지 순간까지의 실제 경과 시간 누적
            totalElapsedTime += (now - lastResumeTime)
        }
    }

    /**
     * 타이머 재개
     */
    fun resumeTimer() {
        if (_isPaused.value) {
            _isPaused.value = false

            // ⭐ 다시 시작된 시각을 새 기준점으로 설정
            lastResumeTime = System.currentTimeMillis()
        }
    }

    /**
     * 타이머 정지 및 세션 저장
     */
    fun stopTimer(subjectName: String) {

        // ⭐ pause 없이 바로 stop하면 누락될 수 있는 시간 보정
        if (_isRunning.value && !_isPaused.value) {
            val now = System.currentTimeMillis()
            totalElapsedTime += (now - lastResumeTime)
        }

        if (_isRunning.value && sessionStartTime != null) {
            val session = TimerSession(
                subjectId = _selectedSubjectId.value ?: 0,
                subjectName = subjectName,
                duration = totalElapsedTime,
                startTime = sessionStartTime,
                endTime = Date()
            )
            _sessions.value = _sessions.value + session
        }

        resetTimer()
    }

    /**
     * 타이머 리셋
     */
    fun resetTimer() {
        _remainingTime.value = 0L
        _isRunning.value = false
        _isPaused.value = false
        sessionStartTime = null

        totalElapsedTime = 0L
        lastResumeTime = 0L
    }

    /**
     * 타이머 틱 (1초마다 호출)
     */
    fun tick() {
        if (_isRunning.value && !_isPaused.value && _remainingTime.value > 0) {
            _remainingTime.value -= 1000

            // UI 업데이트용이므로 totalElapsedTime 증가시키지 않음

            // 종료 시
            if (_remainingTime.value <= 0) {
                _isRunning.value = false
            }
        }
    }

    /**
     * 특정 날짜의 총 공부 시간 (밀리초)
     */
    fun getTotalTimeForDate(date: Date): Long {
        val dateKey = dateFormat.format(date)
        return _sessions.value
            .filter { dateFormat.format(it.date) == dateKey }
            .sumOf { it.duration }
    }

    /**
     * 특정 날짜의 과목별 공부 시간
     */
    fun getTimeBySubjectForDate(date: Date): Map<Int, Long> {
        val dateKey = dateFormat.format(date)
        return _sessions.value
            .filter { dateFormat.format(it.date) == dateKey }
            .groupBy { it.subjectId }
            .mapValues { (_, sessions) -> sessions.sumOf { it.duration } }
    }

    /**
     * 전체 기간의 과목별 총 공부 시간
     */
    fun getTotalTimeBySubject(): Map<Int, Long> {
        return _sessions.value
            .groupBy { it.subjectId }
            .mapValues { (_, sessions) -> sessions.sumOf { it.duration } }
    }

    /**
     * 최근 N일간의 일별 공부 시간
     */
    fun getDailyStudyTime(days: Int = 7): List<Pair<String, Long>> {
        val calendar = java.util.Calendar.getInstance()
        val result = mutableListOf<Pair<String, Long>>()

        for (i in days - 1 downTo 0) {
            calendar.time = Date()
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -i)
            val dateKey = dateFormat.format(calendar.time)
            val totalTime = _sessions.value
                .filter { dateFormat.format(it.date) == dateKey }
                .sumOf { it.duration }

            val displayFormat = SimpleDateFormat("M/d", Locale.getDefault())
            result.add(displayFormat.format(calendar.time) to totalTime)
        }

        return result
    }

    /**
     * 시간을 "HH:mm:ss" 형식으로 변환
     */
    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * 시간을 "분" 단위로 변환
     */
    fun formatTimeInMinutes(millis: Long): String {
        val minutes = millis / (1000 * 60)
        return "${minutes}분"
    }
}

//package edu.sswu.vitaday.ui.timer
//
//import androidx.lifecycle.ViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
///**
// * 타이머 관련 ViewModel
// * 타이머 상태, 세션 기록, 통계 관리
// */
//class TimerViewModel : ViewModel() {
//
//    // 타이머 상태
//    private val _remainingTime = MutableStateFlow(0L)
//    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()
//
//    private val _isRunning = MutableStateFlow(false)
//    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
//
//    private val _isPaused = MutableStateFlow(false)
//    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
//
//    // 현재 선택된 과목
//    private val _selectedSubjectId = MutableStateFlow<Int?>(null)
//    val selectedSubjectId: StateFlow<Int?> = _selectedSubjectId.asStateFlow()
//
//    // 타이머 세션 기록
//    private val _sessions = MutableStateFlow<List<TimerSession>>(emptyList())
//    val sessions: StateFlow<List<TimerSession>> = _sessions.asStateFlow()
//
//    private var sessionStartTime: Date? = null
//    private var totalElapsedTime = 0L
//
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//
//    /**
//     * 과목 선택
//     */
//    fun selectSubject(subjectId: Int) {
//        _selectedSubjectId.value = subjectId
//    }
//
//    /**
//     * 타이머 시작
//     */
//    fun startTimer(durationMinutes: Int, subjectId: Int) {
//        _remainingTime.value = durationMinutes * 60 * 1000L
//        _selectedSubjectId.value = subjectId
//        _isRunning.value = true
//        _isPaused.value = false
//        sessionStartTime = Date()
//        totalElapsedTime = 0L
//    }
//
//    /**
//     * 타이머 일시정지
//     */
//    fun pauseTimer() {
//        _isPaused.value = true
//    }
//
//    /**
//     * 타이머 재개
//     */
//    fun resumeTimer() {
//        _isPaused.value = false
//    }
//
//    /**
//     * 타이머 정지 및 세션 저장
//     */
//    fun stopTimer(subjectName: String) {
//        if (_isRunning.value && sessionStartTime != null) {
//            val session = TimerSession(
//                subjectId = _selectedSubjectId.value ?: 0,
//                subjectName = subjectName,
//                duration = totalElapsedTime,
//                startTime = sessionStartTime,
//                endTime = Date()
//            )
//            _sessions.value = _sessions.value + session
//        }
//        resetTimer()
//    }
//
//    /**
//     * 타이머 리셋
//     */
//    fun resetTimer() {
//        _remainingTime.value = 0L
//        _isRunning.value = false
//        _isPaused.value = false
//        sessionStartTime = null
//        totalElapsedTime = 0L
//    }
//
//    /**
//     * 타이머 틱 (1초마다 호출)
//     */
//    fun tick() {
//        if (_isRunning.value && !_isPaused.value && _remainingTime.value > 0) {
//            _remainingTime.value -= 1000
//            totalElapsedTime += 1000
//
//            // 타이머 종료
//            if (_remainingTime.value <= 0) {
//                _isRunning.value = false
//            }
//        }
//    }
//
//    /**
//     * 특정 날짜의 총 공부 시간 (밀리초)
//     */
//    fun getTotalTimeForDate(date: Date): Long {
//        val dateKey = dateFormat.format(date)
//        return _sessions.value
//            .filter { dateFormat.format(it.date) == dateKey }
//            .sumOf { it.duration }
//    }
//
//    /**
//     * 특정 날짜의 과목별 공부 시간
//     */
//    fun getTimeBySubjectForDate(date: Date): Map<Int, Long> {
//        val dateKey = dateFormat.format(date)
//        return _sessions.value
//            .filter { dateFormat.format(it.date) == dateKey }
//            .groupBy { it.subjectId }
//            .mapValues { (_, sessions) -> sessions.sumOf { it.duration } }
//    }
//
//    /**
//     * 전체 기간의 과목별 총 공부 시간
//     */
//    fun getTotalTimeBySubject(): Map<Int, Long> {
//        return _sessions.value
//            .groupBy { it.subjectId }
//            .mapValues { (_, sessions) -> sessions.sumOf { it.duration } }
//    }
//
//    /**
//     * 최근 N일간의 일별 공부 시간
//     */
//    fun getDailyStudyTime(days: Int = 7): List<Pair<String, Long>> {
//        val calendar = java.util.Calendar.getInstance()
//        val result = mutableListOf<Pair<String, Long>>()
//
//        for (i in days - 1 downTo 0) {
//            calendar.time = Date()
//            calendar.add(java.util.Calendar.DAY_OF_MONTH, -i)
//            val dateKey = dateFormat.format(calendar.time)
//            val totalTime = _sessions.value
//                .filter { dateFormat.format(it.date) == dateKey }
//                .sumOf { it.duration }
//
//            // 날짜 포맷을 "M/d" 형식으로 변경
//            val displayFormat = SimpleDateFormat("M/d", Locale.getDefault())
//            result.add(displayFormat.format(calendar.time) to totalTime)
//        }
//
//        return result
//    }
//
//    /**
//     * 시간을 "HH:mm:ss" 형식으로 변환
//     */
//    fun formatTime(millis: Long): String {
//        val seconds = (millis / 1000) % 60
//        val minutes = (millis / (1000 * 60)) % 60
//        val hours = millis / (1000 * 60 * 60)
//        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
//    }
//
//    /**
//     * 시간을 "분" 단위로 변환
//     */
//    fun formatTimeInMinutes(millis: Long): String {
//        val minutes = millis / (1000 * 60)
//        return "${minutes}분"
//    }
//}