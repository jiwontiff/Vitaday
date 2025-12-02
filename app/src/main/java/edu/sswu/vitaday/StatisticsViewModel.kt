package edu.sswu.vitaday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _todayDuration = MutableStateFlow(0L)
    val todayDuration: StateFlow<Long> = _todayDuration.asStateFlow()

    private val _thisWeekDuration = MutableStateFlow(0L)
    val thisWeekDuration: StateFlow<Long> = _thisWeekDuration.asStateFlow()

    private val _thisMonthDuration = MutableStateFlow(0L)
    val thisMonthDuration: StateFlow<Long> = _thisMonthDuration.asStateFlow()

    private val _last7DaysDuration = MutableStateFlow(0L)
    val last7DaysDuration: StateFlow<Long> = _last7DaysDuration.asStateFlow()

    private val _last28DaysDuration = MutableStateFlow(0L)
    val last28DaysDuration: StateFlow<Long> = _last28DaysDuration.asStateFlow()

    private val _subjectDistribution = MutableStateFlow<List<SubjectDuration>>(emptyList())
    val subjectDistribution: StateFlow<List<SubjectDuration>> = _subjectDistribution.asStateFlow()

    private val _dailyDurations = MutableStateFlow<List<DailyDuration>>(emptyList())
    val dailyDurations: StateFlow<List<DailyDuration>> = _dailyDurations.asStateFlow()

    private val _totalSessionCount = MutableStateFlow(0)
    val totalSessionCount: StateFlow<Int> = _totalSessionCount.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllStatistics()
    }

    fun loadAllStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _todayDuration.value = repository.getTodayTotalDuration()
                _thisWeekDuration.value = repository.getThisWeekTotalDuration()
                _thisMonthDuration.value = repository.getThisMonthTotalDuration()
                _last7DaysDuration.value = repository.getLast7DaysTotalDuration()
                _last28DaysDuration.value = repository.getLast28DaysTotalDuration()

                _subjectDistribution.value = repository.getAllTimeSubjectDistribution()

                _dailyDurations.value = repository.getLast7DaysDailyDurations()

                _totalSessionCount.value = repository.getTotalSessionCount()
                _totalDuration.value = _subjectDistribution.value.sumOf { it.totalDuration }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSubjectDistribution(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            try {
                _subjectDistribution.value = repository.getSubjectDistribution(startDate, endDate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshDailyDurations(days: Int) {
        viewModelScope.launch {
            try {
                _dailyDurations.value = if (days == 7) {
                    repository.getLast7DaysDailyDurations()
                } else {
                    repository.getLast28DaysDailyDurations()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatTimeKorean(millis: Long): String {
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)

        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            minutes > 0 -> "${minutes}분"
            else -> "0분"
        }
    }

    fun formatTimeInMinutes(millis: Long): Long {
        return millis / (1000 * 60)
    }
}