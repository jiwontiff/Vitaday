package edu.sswu.vitaday.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.sswu.vitaday.TimerSessionEntity
import edu.sswu.vitaday.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 타이머 관련 ViewModel
 * 타이머 상태, 세션 기록, 통계 관리 (DB 연동)
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UserDatabase.getDatabase(application)
    private val timerSessionDao = database.timerSessionDao()

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

    // 타이머 세션 기록 (메모리)
    private val _sessions = MutableStateFlow<List<TimerSession>>(emptyList())
    val sessions: StateFlow<List<TimerSession>> = _sessions.asStateFlow()

    private var sessionStartTime: Date? = null
    private var totalElapsedTime = 0L
    private var lastResumeTime = 0L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // 앱 시작 시 DB에서 세션 불러오기
        loadSessionsFromDB()
    }

    /**
     * DB에서 세션 불러오기
     */
    private fun loadSessionsFromDB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                timerSessionDao.getAllSessions().collect { entities ->
                    val sessions = entities.map { entity ->
                        TimerSession(
                            id = entity.sessionId,
                            subjectId = entity.subjectId,
                            subjectName = entity.subjectName,
                            duration = entity.duration,
                            date = Date(entity.date),
                            startTime = entity.startTime?.let { Date(it) },
                            endTime = entity.endTime?.let { Date(it) }
                        )
                    }
                    _sessions.value = sessions
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
        lastResumeTime = System.currentTimeMillis()
    }

    /**
     * 타이머 일시정지
     */
    fun pauseTimer() {
        if (_isRunning.value && !_isPaused.value) {
            _isPaused.value = true
            val now = System.currentTimeMillis()
            totalElapsedTime += (now - lastResumeTime)
        }
    }

    /**
     * 타이머 재개
     */
    fun resumeTimer() {
        if (_isPaused.value) {
            _isPaused.value = false
            lastResumeTime = System.currentTimeMillis()
        }
    }

    /**
     * 타이머 정지 및 세션 저장 (DB에 저장)
     */
    fun stopTimer(subjectName: String) {
        // 마지막 경과 시간 보정
        if (_isRunning.value && !_isPaused.value) {
            val now = System.currentTimeMillis()
            totalElapsedTime += (now - lastResumeTime)
        }

        // ⭐ 로그 추가
        android.util.Log.d("TimerViewModel", "=== stopTimer 시작 ===")
        android.util.Log.d("TimerViewModel", "isRunning: ${_isRunning.value}")
        android.util.Log.d("TimerViewModel", "sessionStartTime: $sessionStartTime")
        android.util.Log.d("TimerViewModel", "totalElapsedTime: $totalElapsedTime")
        android.util.Log.d("TimerViewModel", "selectedSubjectId: ${_selectedSubjectId.value}")  // ⭐ 추가

        if (_isRunning.value && sessionStartTime != null) {
            val subjectId = _selectedSubjectId.value

            // ⭐ subjectId 유효성 검사 추가
            if (subjectId == null || subjectId == 0) {
                android.util.Log.e("TimerViewModel", "유효하지 않은 subjectId: $subjectId - 저장 중단!")
                resetTimer()
                return
            }

            val session = TimerSession(
                subjectId = subjectId,  // ⭐ 검증된 subjectId 사용
                subjectName = subjectName,
                duration = totalElapsedTime,
                startTime = sessionStartTime,
                endTime = Date()
            )

            // ⭐ 로그 추가
            android.util.Log.d("TimerViewModel", "Session 생성: $session")
            android.util.Log.d("TimerViewModel", "Session.date: ${session.date}")
            android.util.Log.d("TimerViewModel", "Session.date.time: ${session.date.time}")

            // 메모리에 추가
            _sessions.value = _sessions.value + session

            // DB에 저장
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val entity = TimerSessionEntity(
                        sessionId = session.id,
                        subjectId = session.subjectId,
                        subjectName = session.subjectName,
                        duration = session.duration,
                        date = session.date.time,
                        startTime = session.startTime?.time,
                        endTime = session.endTime?.time
                    )

                    // ⭐ 로그 추가
                    android.util.Log.d("TimerViewModel", "Entity 생성: $entity")

                    timerSessionDao.insertSession(entity)

                    // ⭐ 로그 추가
                    android.util.Log.d("TimerViewModel", "DB 저장 완료!")

                } catch (e: Exception) {
                    android.util.Log.e("TimerViewModel", "DB 저장 실패!", e)
                    e.printStackTrace()
                }
            }
        } else {
            // ⭐ 로그 추가
            android.util.Log.d("TimerViewModel", "저장 조건 불만족 - isRunning: ${_isRunning.value}, sessionStartTime: $sessionStartTime")
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