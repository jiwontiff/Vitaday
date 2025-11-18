package edu.sswu.vitaday.ui.calendar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * CalendarFragment의 ViewModel
 * 캘린더 날짜 선택, 투두 관리 로직
 */
class CalendarViewModel : ViewModel() {

    // 선택된 날짜
    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    // 투두 맵 (날짜별로 투두 리스트 저장)
    // Key: "yyyy-MM-dd" 형식의 날짜 문자열
    private val _todoMap = MutableStateFlow<Map<String, List<TodoItem>>>(emptyMap())
    val todoMap: StateFlow<Map<String, List<TodoItem>>> = _todoMap.asStateFlow()

    // 확장된 과목 ID (과목 카드 펼침/접힘 상태)
    private val _expandedSubjectId = MutableStateFlow<String?>(null)
    val expandedSubjectId: StateFlow<String?> = _expandedSubjectId.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 날짜 선택
     */
    fun selectDate(date: Date) {
        _selectedDate.value = date
    }

    /**
     * 특정 날짜의 투두 개수 반환
     */
    fun getTodoCountForDate(date: Date): Int {
        val dateKey = dateFormat.format(date)
        return _todoMap.value[dateKey]?.size ?: 0
    }

    /**
     * 특정 날짜의 투두를 과목별로 그룹화하여 반환
     */
    fun getTodosBySubject(date: Date): Map<String, List<TodoItem>> {
        val dateKey = dateFormat.format(date)
        val todosForDate = _todoMap.value[dateKey] ?: emptyList()
        return todosForDate.groupBy { it.subjectId }
    }

    /**
     * 과목 카드 펼침/접힘 토글
     */
    fun toggleSubjectExpansion(subjectId: String) {
        _expandedSubjectId.value = if (_expandedSubjectId.value == subjectId) {
            null // 이미 펼쳐져 있으면 접기
        } else {
            subjectId // 접혀있으면 펼치기
        }
    }

    /**
     * 투두 추가
     */
    fun addTodo(subjectId: String, title: String, date: Date, isRepeat: Boolean = false) {
        val dateKey = dateFormat.format(date)
        val currentMap = _todoMap.value.toMutableMap()
        val currentTodos = currentMap[dateKey]?.toMutableList() ?: mutableListOf()

        val newTodo = TodoItem(
            subjectId = subjectId,
            title = title,
            date = date,
            isRepeat = isRepeat
        )

        currentTodos.add(newTodo)
        currentMap[dateKey] = currentTodos
        _todoMap.value = currentMap

        // 반복 투두인 경우 다음 날들에도 추가
        if (isRepeat) {
            addRepeatTodos(newTodo)
        }
    }

    /**
     * 반복 투두를 다음 7일간 추가
     */
    private fun addRepeatTodos(todo: TodoItem) {
        val calendar = Calendar.getInstance()
        calendar.time = todo.date
        val currentMap = _todoMap.value.toMutableMap()

        for (i in 1..7) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dateKey = dateFormat.format(calendar.time)
            val todosForDate = currentMap[dateKey]?.toMutableList() ?: mutableListOf()

            val repeatTodo = todo.copy(
                date = calendar.time,
                id = java.util.UUID.randomUUID().toString()
            )
            todosForDate.add(repeatTodo)
            currentMap[dateKey] = todosForDate
        }

        _todoMap.value = currentMap
    }

    /**
     * 투두 완료/미완료 토글
     */
    fun toggleTodoComplete(todoId: String) {
        val currentMap = _todoMap.value.toMutableMap()

        currentMap.forEach { (dateKey, todos) ->
            val updatedTodos = todos.map { todo ->
                if (todo.id == todoId) {
                    todo.copy(isCompleted = !todo.isCompleted)
                } else {
                    todo
                }
            }
            currentMap[dateKey] = updatedTodos
        }

        _todoMap.value = currentMap
    }

    /**
     * 투두 삭제
     */
    fun deleteTodo(todoId: String) {
        val currentMap = _todoMap.value.toMutableMap()

        currentMap.forEach { (dateKey, todos) ->
            val updatedTodos = todos.filter { it.id != todoId }
            if (updatedTodos.isEmpty()) {
                currentMap.remove(dateKey)
            } else {
                currentMap[dateKey] = updatedTodos
            }
        }

        _todoMap.value = currentMap
    }
}