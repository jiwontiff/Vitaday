package edu.sswu.vitaday

import kotlinx.coroutines.flow.Flow
import java.util.*

class StatisticsRepository(
    private val timerSessionDao: TimerSessionDao,
    private val subjectDao: SubjectDao
) {

    fun getAllSessions(): Flow<List<TimerSessionEntity>> = timerSessionDao.getAllSessions()

    fun getSessionsBySubject(subjectId: Int): Flow<List<TimerSessionEntity>> =
        timerSessionDao.getSessionsBySubject(subjectId)

    suspend fun getTodayTotalDuration(): Long {
        val (startOfDay, endOfDay) = getTodayRange()
        // ⭐ 로그 추가
        android.util.Log.d("StatisticsRepo", "=== getTodayTotalDuration ===")
        android.util.Log.d("StatisticsRepo", "startOfDay: $startOfDay (${Date(startOfDay)})")
        android.util.Log.d("StatisticsRepo", "endOfDay: $endOfDay (${Date(endOfDay)})")

        val duration = timerSessionDao.getTotalDurationForPeriod(startOfDay, endOfDay)

        // ⭐ 로그 추가
        android.util.Log.d("StatisticsRepo", "조회된 duration: $duration")

        return timerSessionDao.getTotalDurationForPeriod(startOfDay, endOfDay)
    }

    suspend fun getThisWeekTotalDuration(): Long {
        val (startOfWeek, endOfWeek) = getThisWeekRange()
        return timerSessionDao.getTotalDurationForPeriod(startOfWeek, endOfWeek)
    }

    suspend fun getThisMonthTotalDuration(): Long {
        val (startOfMonth, endOfMonth) = getThisMonthRange()
        return timerSessionDao.getTotalDurationForPeriod(startOfMonth, endOfMonth)
    }

    suspend fun getLast7DaysTotalDuration(): Long {
        val (start, end) = getLastNDaysRange(7)
        return timerSessionDao.getTotalDurationForPeriod(start, end)
    }

    suspend fun getLast28DaysTotalDuration(): Long {
        val (start, end) = getLastNDaysRange(28)
        return timerSessionDao.getTotalDurationForPeriod(start, end)
    }

    suspend fun getSubjectDistribution(startDate: Long, endDate: Long): List<SubjectDuration> =
        timerSessionDao.getSubjectDurationsForPeriod(startDate, endDate)

    suspend fun getAllTimeSubjectDistribution(): List<SubjectDuration> =
        timerSessionDao.getSubjectDurationsForPeriod(0, System.currentTimeMillis())

    suspend fun getDailyDurations(startDate: Long, endDate: Long): List<DailyDuration> =
        timerSessionDao.getDailyDurationsForPeriod(startDate, endDate)

    suspend fun getLast7DaysDailyDurations(): List<DailyDuration> {
        val (start, end) = getLastNDaysRange(7)
        return timerSessionDao.getDailyDurationsForPeriod(start, end)
    }

    suspend fun getLast28DaysDailyDurations(): List<DailyDuration> {
        val (start, end) = getLastNDaysRange(28)
        return timerSessionDao.getDailyDurationsForPeriod(start, end)
    }

    suspend fun getTotalSessionCount(): Int =
        timerSessionDao.getTotalSessionCount()

    suspend fun getSessionCountForPeriod(startDate: Long, endDate: Long): Int =
        timerSessionDao.getSessionCountForPeriod(startDate, endDate)

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    private fun getThisWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // 이번 주 월요일로 이동
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (currentDayOfWeek) {
            Calendar.SUNDAY -> 6  // 일요일이면 6일 전이 월요일
            else -> currentDayOfWeek - Calendar.MONDAY  // 나머지는 계산
        }
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        // 월요일 00:00:00
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        // 일요일 23:59:59 (6일 후)
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    private fun getThisMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }

    private fun getLastNDaysRange(days: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, -(days - 1))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}