package edu.sswu.vitaday.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import edu.sswu.vitaday.R
import edu.sswu.vitaday.SharedSubjectViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 투두 추가 바텀시트
 */
class AddTodoBottomSheet : BottomSheetDialogFragment() {

    private val calendarViewModel: CalendarViewModel by activityViewModels()
    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()

    private var selectedDate: Date? = null
    private var selectedSubjectId: String? = null

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: Date?, subjects: List<edu.sswu.vitaday.SubjectData>): AddTodoBottomSheet {
            return AddTodoBottomSheet().apply {
                arguments = Bundle().apply {
                    date?.let { putLong(ARG_DATE, it.time) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getLong(ARG_DATE)?.let {
            selectedDate = Date(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_add_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDate = view.findViewById<TextView>(R.id.tv_selected_date)
        val rgSubjects = view.findViewById<RadioGroup>(R.id.rg_subjects)
        val etTodoTitle = view.findViewById<EditText>(R.id.et_todo_title)
        val cbRepeat = view.findViewById<CheckBox>(R.id.cb_repeat)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_todo)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        // 날짜 표시
        val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())
        tvDate.text = selectedDate?.let { dateFormat.format(it) } ?: "날짜 선택"

        // 과목 라디오 버튼 동적 생성
        val subjects = sharedViewModel.subjects.value

        if (subjects.isEmpty()) {
            // 과목이 없으면 안내 메시지
            val textView = TextView(requireContext()).apply {
                text = "먼저 홈에서 과목을 생성해주세요"
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                setPadding(16, 16, 16, 16)
            }
            rgSubjects.addView(textView)
            btnAdd.isEnabled = false
        } else {
            subjects.forEach { subject ->
                val radioButton = RadioButton(requireContext()).apply {
                    id = View.generateViewId()
                    text = subject.name
                    tag = subject.id
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                rgSubjects.addView(radioButton)
            }

            // 첫 번째 과목 자동 선택
            if (rgSubjects.childCount > 0) {
                (rgSubjects.getChildAt(0) as? RadioButton)?.isChecked = true
                selectedSubjectId = (rgSubjects.getChildAt(0) as? RadioButton)?.tag as? String
            }
        }

        // 과목 선택 리스너
        rgSubjects.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadio = view.findViewById<RadioButton>(checkedId)
            selectedSubjectId = selectedRadio?.tag as? String
        }

        // 추가 버튼
        btnAdd.setOnClickListener {
            val title = etTodoTitle.text.toString().trim()
            val subjectId = selectedSubjectId
            val date = selectedDate

            if (title.isEmpty()) {
                etTodoTitle.error = "할 일을 입력하세요"
                return@setOnClickListener
            }

            if (subjectId == null) {
                Toast.makeText(requireContext(), "과목을 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date == null) {
                Toast.makeText(requireContext(), "날짜를 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ViewModel에 투두 추가
            calendarViewModel.addTodo(
                subjectId = subjectId,
                title = title,
                date = date,
                isRepeat = cbRepeat.isChecked
            )

            Toast.makeText(requireContext(), "투두가 추가되었습니다", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        // 취소 버튼
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}