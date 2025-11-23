package edu.sswu.vitaday.ui.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.sswu.vitaday.R
import edu.sswu.vitaday.SharedSubjectViewModel
import edu.sswu.vitaday.SubjectData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 캘린더 + 투두 메인 Fragment
 * 주간뷰/월간뷰 전환 기능 포함
 */
class CalendarFragment : Fragment() {

    private val viewModel: CalendarViewModel by activityViewModels()
    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()

    private lateinit var tvMonthYear: TextView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var llSubjectContainer: LinearLayout
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnToggleCalendar: ImageButton
    private lateinit var fabAddTodo: FloatingActionButton

    private lateinit var calendarAdapter: CalendarAdapter
    private var currentCalendar = Calendar.getInstance()
    private var isMonthView = true // true: 월간뷰, false: 주간뷰

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View 초기화
        tvMonthYear = view.findViewById(R.id.tv_month_year)
        rvCalendar = view.findViewById(R.id.rv_calendar)
        llSubjectContainer = view.findViewById(R.id.ll_subject_container)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        btnToggleCalendar = view.findViewById(R.id.btn_toggle_calendar)
        fabAddTodo = view.findViewById(R.id.fab_add_todo)

        // 캘린더 RecyclerView 설정
        setupCalendarRecyclerView()

        // 버튼 리스너
        setupButtons()

        // ViewModel 관찰
        observeViewModel()

        // 초기 캘린더 표시
        updateCalendar()
    }

    private fun setupCalendarRecyclerView() {
        calendarAdapter = CalendarAdapter(
            onDateClick = { date ->
                viewModel.selectDate(date)
            },
            getTodoCount = { date ->
                viewModel.getTodoCountForDate(date)
            }
        )

        rvCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }
    }

    private fun setupButtons() {
        btnPrevMonth.setOnClickListener {
            if (isMonthView) {
                currentCalendar.add(Calendar.MONTH, -1)
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            }
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            if (isMonthView) {
                currentCalendar.add(Calendar.MONTH, 1)
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            updateCalendar()
        }

        // 주간뷰/월간뷰 토글
        btnToggleCalendar.setOnClickListener {
            isMonthView = !isMonthView
            updateCalendar()

            // 아이콘 회전 애니메이션
            btnToggleCalendar.animate()
                .rotation(if (isMonthView) 0f else 180f)
                .setDuration(200)
                .start()
        }

        fabAddTodo.setOnClickListener {
            val bottomSheet = AddTodoBottomSheet.newInstance(
                date = viewModel.selectedDate.value,
                subjects = sharedViewModel.subjects.value
            )
            bottomSheet.show(childFragmentManager, "AddTodoBottomSheet")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDate.collectLatest { date ->
                updateSubjectList(date)
                calendarAdapter.submitDates(generateCalendarDates(), date)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todoMap.collectLatest {
                calendarAdapter.submitDates(
                    generateCalendarDates(),
                    viewModel.selectedDate.value
                )
                updateSubjectList(viewModel.selectedDate.value)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expandedSubjectId.collectLatest { expandedId ->
                updateSubjectList(viewModel.selectedDate.value)
            }
        }

        // SharedViewModel의 과목 변경 감지
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.subjects.collectLatest {
                updateSubjectList(viewModel.selectedDate.value)
            }
        }
    }

    private fun updateCalendar() {
        // 월/년도 또는 주차 표시
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH) + 1

        if (isMonthView) {
            tvMonthYear.text = "${month}월"
        } else {
            val weekOfMonth = currentCalendar.get(Calendar.WEEK_OF_MONTH)
            tvMonthYear.text = "${month}월 ${weekOfMonth}주차"
        }

        // 캘린더 날짜 생성
        val dates = generateCalendarDates()
        calendarAdapter.submitDates(dates, viewModel.selectedDate.value)
    }

    private fun generateCalendarDates(): List<CalendarAdapter.CalendarDate> {
        return if (isMonthView) {
            generateMonthDates()
        } else {
            generateWeekDates()
        }
    }

    private fun generateMonthDates(): List<CalendarAdapter.CalendarDate> {
        val dates = mutableListOf<CalendarAdapter.CalendarDate>()
        val calendar = currentCalendar.clone() as Calendar

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

        val prevMonthCalendar = calendar.clone() as Calendar
        prevMonthCalendar.add(Calendar.MONTH, -1)
        val prevMonthLastDay = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in firstDayOfWeek - 1 downTo 0) {
            prevMonthCalendar.set(Calendar.DAY_OF_MONTH, prevMonthLastDay - i)
            dates.add(
                CalendarAdapter.CalendarDate(
                    date = prevMonthCalendar.time,
                    dayOfMonth = prevMonthLastDay - i,
                    isCurrentMonth = false
                )
            )
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            dates.add(
                CalendarAdapter.CalendarDate(
                    date = calendar.time,
                    dayOfMonth = day,
                    isCurrentMonth = true
                )
            )
        }

        val remainingDays = 42 - dates.size
        val nextMonthCalendar = calendar.clone() as Calendar
        nextMonthCalendar.add(Calendar.MONTH, 1)

        for (day in 1..remainingDays) {
            nextMonthCalendar.set(Calendar.DAY_OF_MONTH, day)
            dates.add(
                CalendarAdapter.CalendarDate(
                    date = nextMonthCalendar.time,
                    dayOfMonth = day,
                    isCurrentMonth = false
                )
            )
        }

        return dates
    }

    private fun generateWeekDates(): List<CalendarAdapter.CalendarDate> {
        val dates = mutableListOf<CalendarAdapter.CalendarDate>()
        val calendar = currentCalendar.clone() as Calendar

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        for (i in 0..6) {
            dates.add(
                CalendarAdapter.CalendarDate(
                    date = calendar.time,
                    dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = true
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dates
    }

    /*
    private fun updateSubjectList(date: Date) {
        llSubjectContainer.removeAllViews()

        // 타입을 명시적으로 지정
        val todosBySubject: Map<String, List<TodoItem>> = viewModel.getTodosBySubject(date)

        sharedViewModel.subjects.value.forEach { subject ->
            // subject.id를 String으로 변환
            val subjectCard = createSubjectCard(
                subject,
                todosBySubject[subject.id.toString()] ?: emptyList()
            )
            llSubjectContainer.addView(subjectCard)
        }
    }
    */

    private fun updateSubjectList(date: Date) {
        llSubjectContainer.removeAllViews()

        val todosBySubject: Map<String, List<TodoItem>> = viewModel.getTodosBySubject(date)

        android.util.Log.d("CalendarFragment", "📊 전체 과목 수: ${sharedViewModel.subjects.value.size}")

        sharedViewModel.subjects.value.forEach { subject ->
            try {
                android.util.Log.d("CalendarFragment", "🔄 처리 중: ${subject.name} (색상: ${subject.colorHex})")

                val subjectCard = createSubjectCard(
                    subject,
                    todosBySubject[subject.id.toString()] ?: emptyList()
                )
                llSubjectContainer.addView(subjectCard)

                android.util.Log.d("CalendarFragment", "✅ ${subject.name} 생성 성공")

            } catch (e: Exception) {
                android.util.Log.e("CalendarFragment", "❌ ${subject.name} 생성 실패", e)
                e.printStackTrace()
            }
        }
    }

    private fun createSubjectCard(subject: SubjectData, todos: List<TodoItem>): View {
        val cardView = layoutInflater.inflate(R.layout.item_subject_card, llSubjectContainer, false)

        val viewColor = cardView.findViewById<View>(R.id.view_subject_color)
        val tvName = cardView.findViewById<TextView>(R.id.tv_subject_name)
        val tvCount = cardView.findViewById<TextView>(R.id.tv_todo_count)
        val ivExpand = cardView.findViewById<ImageView>(R.id.iv_expand)
        val llHeader = cardView.findViewById<LinearLayout>(R.id.ll_subject_header)
        val llTodoContainer = cardView.findViewById<LinearLayout>(R.id.ll_todo_container)
        val etTodoInput = cardView.findViewById<EditText>(R.id.et_todo_input)
        val cbRepeat = cardView.findViewById<CheckBox>(R.id.cb_repeat)
        val rvTodos = cardView.findViewById<RecyclerView>(R.id.rv_todos)

        viewColor.setBackgroundColor(subject.colorHex.toColorInt())
        tvName.text = subject.name
        tvCount.text = todos.size.toString()

        val isExpanded = viewModel.expandedSubjectId.value == subject.id.toString()
        llTodoContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        ivExpand.rotation = if (isExpanded) 180f else 0f

        llHeader.setOnClickListener {
            viewModel.toggleSubjectExpansion(subject.id.toString())
        }

        etTodoInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = etTodoInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.addTodo(
                        subjectId = subject.id.toString(),
                        title = title,
                        date = viewModel.selectedDate.value,
                        isRepeat = cbRepeat.isChecked
                    )
                    etTodoInput.text.clear()
                    cbRepeat.isChecked = false
                }
                true
            } else {
                false
            }
        }

        val todoAdapter = TodoListAdapter(
            onToggleComplete = { todo ->
                viewModel.toggleTodoComplete(todo.id)
            },
            onDelete = { todo ->
                viewModel.deleteTodo(todo.id)
            }
        )
        rvTodos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todoAdapter
        }
        todoAdapter.submitList(todos)

        return cardView
    }
}