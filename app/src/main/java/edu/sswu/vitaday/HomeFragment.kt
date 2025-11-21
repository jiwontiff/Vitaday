package edu.sswu.vitaday

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
import edu.sswu.vitaday.ui.timer.TimerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class HomeFragment : Fragment() {

    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
    private val timerViewModel: TimerViewModel by activityViewModels()

    private lateinit var llSubjectContainer: LinearLayout
    private lateinit var btnAddSubject: Button
    private lateinit var tvTodayStudyTime: TextView
    private lateinit var tvCurrentDate: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llSubjectContainer = view.findViewById(R.id.ll_subject_container)
        btnAddSubject = view.findViewById(R.id.btn_add_subject)
        tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
        tvCurrentDate = view.findViewById(R.id.tv_current_date)

        // 현재 날짜 표시
        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
        tvCurrentDate.text = dateFormat.format(Date())

        // 과목 추가 버튼
        btnAddSubject.setOnClickListener {
            showAddSubjectDialog()
        }

        // ViewModel 관찰
        observeViewModel()
    }

    private fun observeViewModel() {
        // 과목 리스트 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.subjects.collectLatest { subjects ->
                updateSubjectList(subjects)
            }
        }

        // 오늘의 총 공부 시간 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            timerViewModel.sessions.collectLatest {
                val todayTime = timerViewModel.getTotalTimeForDate(Date())
                tvTodayStudyTime.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
            }
        }
    }

    private fun updateSubjectList(subjects: List<SubjectData>) {
        llSubjectContainer.removeAllViews()

        if (subjects.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "과목을 추가해주세요"
                textSize = 16f
                setTextColor(Color.parseColor("#AAAAAA"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            llSubjectContainer.addView(emptyView)
            return
        }

        subjects.forEach { subject ->
            val subjectCard = createSubjectCard(subject)
            llSubjectContainer.addView(subjectCard)
        }
    }

    private fun createSubjectCard(subject: SubjectData): View {
        val cardView = layoutInflater.inflate(R.layout.item_subject_home_card, llSubjectContainer, false)

        val viewColor = cardView.findViewById<View>(R.id.view_subject_color)
        val tvName = cardView.findViewById<TextView>(R.id.tv_subject_name)
        val tvTodayTime = cardView.findViewById<TextView>(R.id.tv_today_time)
        val btnStartTimer = cardView.findViewById<Button>(R.id.btn_start_timer)
        val btnDelete = cardView.findViewById<Button>(R.id.btn_delete_subject)

        viewColor.setBackgroundColor(subject.colorHex.toColorInt())
        tvName.text = subject.name

        // 오늘의 과목별 공부 시간
        val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subject.id] ?: 0L
        tvTodayTime.text = "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"

        // 타이머 시작 버튼
        btnStartTimer.setOnClickListener {
            openTimerFragment(subject.id)
        }

        // 삭제 버튼
        btnDelete.setOnClickListener {
            showDeleteConfirmDialog(subject)
        }

        return cardView
    }

    private fun openTimerFragment(subjectId: Int) {
        val fragment = PomodoroTimerFragment.newInstance(subjectId)

        // ✅ 수정: parentFragmentManager를 activity의 supportFragmentManager로 변경
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showAddSubjectDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)

        val colors = listOf(
            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
        )

        var selectedColor = colors[0]

        // 색상 선택 버튼 생성
        colors.forEach { colorHex ->
            val colorButton = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(colorHex.toColorInt())
                setOnClickListener {
                    selectedColor = colorHex
                    // 선택된 색상 표시 (테두리 추가)
                    llColorPicker.children.forEach { view ->
                        view.setPadding(0, 0, 0, 0)
                    }
                    setPadding(4, 4, 4, 4)
                }
            }
            llColorPicker.addView(colorButton)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("과목 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val name = etSubjectName.text.toString().trim()
                if (name.isNotEmpty()) {
                    sharedViewModel.addSubject(name, selectedColor)
                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteConfirmDialog(subject: SubjectData) {
        AlertDialog.Builder(requireContext())
            .setTitle("과목 삭제")
            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                sharedViewModel.removeSubject(subject.id)
                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}

// ViewGroup의 자식 뷰들을 순회하기 위한 확장 함수
val ViewGroup.children: Sequence<View>
    get() = (0 until childCount).asSequence().map { getChildAt(it) }
//package edu.sswu.vitaday
//
//import android.graphics.Color
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.core.graphics.toColorInt
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.lifecycleScope
//import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
//import edu.sswu.vitaday.ui.timer.TimerViewModel
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.launch
//import java.util.Date
//
//class HomeFragment : Fragment() {
//
//    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
//    private val timerViewModel: TimerViewModel by activityViewModels()
//
//    private lateinit var llSubjectContainer: LinearLayout
//    private lateinit var btnAddSubject: Button
//    private lateinit var tvTodayStudyTime: TextView
//    private lateinit var tvCurrentDate: TextView
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_home, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        llSubjectContainer = view.findViewById(R.id.ll_subject_container)
//        btnAddSubject = view.findViewById(R.id.btn_add_subject)
//        tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
//        tvCurrentDate = view.findViewById(R.id.tv_current_date)
//
//        // 현재 날짜 표시
//        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
//        tvCurrentDate.text = dateFormat.format(Date())
//
//        // 과목 추가 버튼
//        btnAddSubject.setOnClickListener {
//            showAddSubjectDialog()
//        }
//
//        // ViewModel 관찰
//        observeViewModel()
//    }
//
//    private fun observeViewModel() {
//        // 과목 리스트 관찰
//        viewLifecycleOwner.lifecycleScope.launch {
//            sharedViewModel.subjects.collectLatest { subjects ->
//                updateSubjectList(subjects)
//            }
//        }
//
//        // 오늘의 총 공부 시간 관찰
//        viewLifecycleOwner.lifecycleScope.launch {
//            timerViewModel.sessions.collectLatest {
//                val todayTime = timerViewModel.getTotalTimeForDate(Date())
//                tvTodayStudyTime.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
//            }
//        }
//    }
//
//    private fun updateSubjectList(subjects: List<SubjectData>) {
//        llSubjectContainer.removeAllViews()
//
//        if (subjects.isEmpty()) {
//            val emptyView = TextView(requireContext()).apply {
//                text = "과목을 추가해주세요"
//                textSize = 16f
//                setTextColor(Color.parseColor("#AAAAAA"))
//                gravity = android.view.Gravity.CENTER
//                setPadding(0, 32, 0, 32)
//            }
//            llSubjectContainer.addView(emptyView)
//            return
//        }
//
//        subjects.forEach { subject ->
//            val subjectCard = createSubjectCard(subject)
//            llSubjectContainer.addView(subjectCard)
//        }
//    }
//
//    private fun createSubjectCard(subject: SubjectData): View {
//        val cardView = layoutInflater.inflate(R.layout.item_subject_home_card, llSubjectContainer, false)
//
//        val viewColor = cardView.findViewById<View>(R.id.view_subject_color)
//        val tvName = cardView.findViewById<TextView>(R.id.tv_subject_name)
//        val tvTodayTime = cardView.findViewById<TextView>(R.id.tv_today_time)
//        val btnStartTimer = cardView.findViewById<Button>(R.id.btn_start_timer)
//        val btnDelete = cardView.findViewById<Button>(R.id.btn_delete_subject)
//
//        viewColor.setBackgroundColor(subject.colorHex.toColorInt())
//        tvName.text = subject.name
//
//        // 오늘의 과목별 공부 시간
//        val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subject.id] ?: 0L
//        tvTodayTime.text = "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"
//
//        // 타이머 시작 버튼
//        btnStartTimer.setOnClickListener {
//            openTimerFragment(subject.id)
//        }
//
//        // 삭제 버튼
//        btnDelete.setOnClickListener {
//            showDeleteConfirmDialog(subject)
//        }
//
//        return cardView
//    }
//
//    private fun openTimerFragment(subjectId: Int) {
//        val fragment = PomodoroTimerFragment.newInstance(subjectId)
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.nav_host_fragment, fragment)
//            .addToBackStack(null)
//            .commit()
//    }
//
//    private fun showAddSubjectDialog() {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
//        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
//        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
//
//        val colors = listOf(
//            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
//            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
//            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
//        )
//
//        var selectedColor = colors[0]
//
//        // 색상 선택 버튼 생성
//        colors.forEach { colorHex ->
//            val colorButton = View(requireContext()).apply {
//                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
//                    setMargins(8, 8, 8, 8)
//                }
//                setBackgroundColor(colorHex.toColorInt())
//                setOnClickListener {
//                    selectedColor = colorHex
//                    // 선택된 색상 표시 (테두리 추가)
//                    llColorPicker.children.forEach { view ->
//                        view.setPadding(0, 0, 0, 0)
//                    }
//                    setPadding(4, 4, 4, 4)
//                }
//            }
//            llColorPicker.addView(colorButton)
//        }
//
//        AlertDialog.Builder(requireContext())
//            .setTitle("과목 추가")
//            .setView(dialogView)
//            .setPositiveButton("추가") { _, _ ->
//                val name = etSubjectName.text.toString().trim()
//                if (name.isNotEmpty()) {
//                    sharedViewModel.addSubject(name, selectedColor)
//                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("취소", null)
//            .show()
//    }
//
//    private fun showDeleteConfirmDialog(subject: SubjectData) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("과목 삭제")
//            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
//            .setPositiveButton("삭제") { _, _ ->
//                sharedViewModel.removeSubject(subject.id)
//                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
//            }
//            .setNegativeButton("취소", null)
//            .show()
//    }
//}
//
//// ViewGroup의 자식 뷰들을 순회하기 위한 확장 함수
//val ViewGroup.children: Sequence<View>
//    get() = (0 until childCount).asSequence().map { getChildAt(it) }
////package edu.sswu.vitaday
////
////import android.os.Bundle
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import androidx.fragment.app.Fragment
////
////class HomeFragment : Fragment() {
////
////    override fun onCreateView(
////        inflater: LayoutInflater,
////        container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        return inflater.inflate(R.layout.fragment_home, container, false)
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        // TODO: 세원님이 구현하는 뽀모도로 타이머 로직 추가
////        // TODO: 과목 생성 기능 추가
////    }
////}