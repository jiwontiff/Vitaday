package edu.sswu.vitaday

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
import edu.sswu.vitaday.ui.timer.TimerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class HomeFragment : Fragment() {

    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
    private val timerViewModel: TimerViewModel by activityViewModels()

    // RecyclerView로 변경
    private var rvSubjects: RecyclerView? = null
    private var btnAddSubject: Button? = null
    private var tvTodayStudyTime: TextView? = null
    private var tvCurrentDate: TextView? = null
    private var tvEmptyMessage: TextView? = null

    private var subjectAdapter: SubjectAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_home, container, false)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error inflating layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // View 초기화 - RecyclerView 사용
            rvSubjects = view.findViewById(R.id.rv_subjects)
            btnAddSubject = view.findViewById(R.id.btn_add_subject)
            tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
            tvCurrentDate = view.findViewById(R.id.tv_current_date)
            tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

            if (rvSubjects == null || btnAddSubject == null || tvTodayStudyTime == null ||
                tvCurrentDate == null || tvEmptyMessage == null) {
                Log.e("HomeFragment", "Some views are null!")
                Toast.makeText(requireContext(), "레이아웃 로딩 오류", Toast.LENGTH_SHORT).show()
                return
            }

            // 현재 날짜 표시
            val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
            tvCurrentDate?.text = dateFormat.format(Date())

            // RecyclerView 설정
            setupRecyclerView()

            // 과목 추가 버튼
            btnAddSubject?.setOnClickListener {
                showAddSubjectDialog()
            }

            // ViewModel 관찰
            observeViewModel()

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        try {
            subjectAdapter = SubjectAdapter(
                onSubjectClick = { subject ->
                    openTimerFragment(subject.id)
                },
                onSubjectDoubleClick = { subject ->
                    showEditSubjectDialog(subject)
                },
                onSubjectDelete = { subject ->
                    showDeleteConfirmDialog(subject)
                },
                // ✅ 수정됨: 공부 시간을 초 단위(HH:mm:ss)까지 표시
                getTodayTime = { subjectId ->
                    val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subjectId] ?: 0L
                    "오늘: ${timerViewModel.formatTime(todayTime)}"
                }
            )

            rvSubjects?.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = subjectAdapter
            }

            // 드래그 앤 드롭 설정
            val callback = SubjectItemTouchHelper(subjectAdapter!!) {
                val reorderedList = subjectAdapter?.getReorderedList() ?: return@SubjectItemTouchHelper
                sharedViewModel.reorderSubjects(reorderedList)
                Toast.makeText(requireContext(), "순서가 변경되었습니다", Toast.LENGTH_SHORT).show()
            }
            itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper?.attachToRecyclerView(rvSubjects)

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up RecyclerView", e)
            Toast.makeText(requireContext(), "RecyclerView 설정 오류", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.subjects.collectLatest { subjects ->
                if (subjects.isEmpty()) {
                    rvSubjects?.visibility = View.GONE
                    tvEmptyMessage?.visibility = View.VISIBLE
                } else {
                    rvSubjects?.visibility = View.VISIBLE
                    tvEmptyMessage?.visibility = View.GONE
                    subjectAdapter?.submitList(subjects)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            timerViewModel.sessions.collectLatest {
                val todayTime = timerViewModel.getTotalTimeForDate(Date())
                // 여기도 초 단위로 보고 싶으시다면 아래와 같이 수정 가능합니다
                // tvTodayStudyTime?.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
                tvTodayStudyTime?.text = "오늘 공부 시간: ${timerViewModel.formatTime(todayTime)}"

                subjectAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun openTimerFragment(subjectId: Int) {
        try {
            val fragment = PomodoroTimerFragment.newInstance(subjectId)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error opening timer", e)
            Toast.makeText(requireContext(), "타이머 열기 오류", Toast.LENGTH_SHORT).show()
        }
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

        colors.forEach { colorHex ->
            val colorButton = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(colorHex.toColorInt())
                setOnClickListener {
                    selectedColor = colorHex
                    for (i in 0 until llColorPicker.childCount) {
                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
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

    private fun showEditSubjectDialog(subject: SubjectData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)

        etSubjectName.setText(subject.name)

        val colors = listOf(
            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
        )

        var selectedColor = subject.colorHex

        colors.forEach { colorHex ->
            val colorButton = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(colorHex.toColorInt())

                if (colorHex == subject.colorHex) {
                    setPadding(4, 4, 4, 4)
                }

                setOnClickListener {
                    selectedColor = colorHex
                    for (i in 0 until llColorPicker.childCount) {
                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
                    }
                    setPadding(4, 4, 4, 4)
                }
            }
            llColorPicker.addView(colorButton)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("과목 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val name = etSubjectName.text.toString().trim()
                if (name.isNotEmpty()) {
                    sharedViewModel.updateSubject(subject.id, name, selectedColor)
                    Toast.makeText(requireContext(), "과목이 수정되었습니다", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        rvSubjects = null
        btnAddSubject = null
        tvTodayStudyTime = null
        tvCurrentDate = null
        tvEmptyMessage = null
        subjectAdapter = null
        itemTouchHelper = null
    }
}
//package edu.sswu.vitaday
//
//import android.graphics.Color
//import android.os.Bundle
//import android.util.Log
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
//import androidx.recyclerview.widget.ItemTouchHelper
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
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
//    // RecyclerView로 변경
//    private var rvSubjects: RecyclerView? = null
//    private var btnAddSubject: Button? = null
//    private var tvTodayStudyTime: TextView? = null
//    private var tvCurrentDate: TextView? = null
//    private var tvEmptyMessage: TextView? = null
//
//    private var subjectAdapter: SubjectAdapter? = null
//    private var itemTouchHelper: ItemTouchHelper? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return try {
//            inflater.inflate(R.layout.fragment_home, container, false)
//        } catch (e: Exception) {
//            Log.e("HomeFragment", "Error inflating layout", e)
//            null
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        try {
//            // View 초기화 - RecyclerView 사용
//            rvSubjects = view.findViewById(R.id.rv_subjects)
//            btnAddSubject = view.findViewById(R.id.btn_add_subject)
//            tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
//            tvCurrentDate = view.findViewById(R.id.tv_current_date)
//            tvEmptyMessage = view.findViewById(R.id.tv_empty_message)
//
//            if (rvSubjects == null || btnAddSubject == null || tvTodayStudyTime == null ||
//                tvCurrentDate == null || tvEmptyMessage == null) {
//                Log.e("HomeFragment", "Some views are null!")
//                Toast.makeText(requireContext(), "레이아웃 로딩 오류", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            // 현재 날짜 표시
//            val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
//            tvCurrentDate?.text = dateFormat.format(Date())
//
//            // RecyclerView 설정
//            setupRecyclerView()
//
//            // 과목 추가 버튼
//            btnAddSubject?.setOnClickListener {
//                showAddSubjectDialog()
//            }
//
//            // ViewModel 관찰
//            observeViewModel()
//
//        } catch (e: Exception) {
//            Log.e("HomeFragment", "Error in onViewCreated", e)
//            Toast.makeText(requireContext(), "초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun setupRecyclerView() {
//        try {
//            subjectAdapter = SubjectAdapter(
//                onSubjectClick = { subject ->
//                    openTimerFragment(subject.id)
//                },
//                onSubjectDoubleClick = { subject ->
//                    showEditSubjectDialog(subject)
//                },
//                onSubjectDelete = { subject ->
//                    showDeleteConfirmDialog(subject)
//                },
//                getTodayTime = { subjectId ->
//                    val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subjectId] ?: 0L
//                    "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"
//                }
//            )
//
//            rvSubjects?.apply {
//                layoutManager = LinearLayoutManager(requireContext())
//                adapter = subjectAdapter
//            }
//
//            // 드래그 앤 드롭 설정
//            val callback = SubjectItemTouchHelper(subjectAdapter!!) {
//                val reorderedList = subjectAdapter?.getReorderedList() ?: return@SubjectItemTouchHelper
//                sharedViewModel.reorderSubjects(reorderedList)
//                Toast.makeText(requireContext(), "순서가 변경되었습니다", Toast.LENGTH_SHORT).show()
//            }
//            itemTouchHelper = ItemTouchHelper(callback)
//            itemTouchHelper?.attachToRecyclerView(rvSubjects)
//
//        } catch (e: Exception) {
//            Log.e("HomeFragment", "Error setting up RecyclerView", e)
//            Toast.makeText(requireContext(), "RecyclerView 설정 오류", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun observeViewModel() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            sharedViewModel.subjects.collectLatest { subjects ->
//                if (subjects.isEmpty()) {
//                    rvSubjects?.visibility = View.GONE
//                    tvEmptyMessage?.visibility = View.VISIBLE
//                } else {
//                    rvSubjects?.visibility = View.VISIBLE
//                    tvEmptyMessage?.visibility = View.GONE
//                    subjectAdapter?.submitList(subjects)
//                }
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            timerViewModel.sessions.collectLatest {
//                val todayTime = timerViewModel.getTotalTimeForDate(Date())
//                tvTodayStudyTime?.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
//                subjectAdapter?.notifyDataSetChanged()
//            }
//        }
//    }
//
//    private fun openTimerFragment(subjectId: Int) {
//        try {
//            val fragment = PomodoroTimerFragment.newInstance(subjectId)
//            requireActivity().supportFragmentManager.beginTransaction()
//                .replace(R.id.nav_host_fragment, fragment)
//                .addToBackStack(null)
//                .commit()
//        } catch (e: Exception) {
//            Log.e("HomeFragment", "Error opening timer", e)
//            Toast.makeText(requireContext(), "타이머 열기 오류", Toast.LENGTH_SHORT).show()
//        }
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
//        colors.forEach { colorHex ->
//            val colorButton = View(requireContext()).apply {
//                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
//                    setMargins(8, 8, 8, 8)
//                }
//                setBackgroundColor(colorHex.toColorInt())
//                setOnClickListener {
//                    selectedColor = colorHex
//                    for (i in 0 until llColorPicker.childCount) {
//                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
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
//    private fun showEditSubjectDialog(subject: SubjectData) {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
//        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
//        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
//
//        etSubjectName.setText(subject.name)
//
//        val colors = listOf(
//            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
//            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
//            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
//        )
//
//        var selectedColor = subject.colorHex
//
//        colors.forEach { colorHex ->
//            val colorButton = View(requireContext()).apply {
//                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
//                    setMargins(8, 8, 8, 8)
//                }
//                setBackgroundColor(colorHex.toColorInt())
//
//                if (colorHex == subject.colorHex) {
//                    setPadding(4, 4, 4, 4)
//                }
//
//                setOnClickListener {
//                    selectedColor = colorHex
//                    for (i in 0 until llColorPicker.childCount) {
//                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
//                    }
//                    setPadding(4, 4, 4, 4)
//                }
//            }
//            llColorPicker.addView(colorButton)
//        }
//
//        AlertDialog.Builder(requireContext())
//            .setTitle("과목 수정")
//            .setView(dialogView)
//            .setPositiveButton("수정") { _, _ ->
//                val name = etSubjectName.text.toString().trim()
//                if (name.isNotEmpty()) {
//                    sharedViewModel.updateSubject(subject.id, name, selectedColor)
//                    Toast.makeText(requireContext(), "과목이 수정되었습니다", Toast.LENGTH_SHORT).show()
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
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        rvSubjects = null
//        btnAddSubject = null
//        tvTodayStudyTime = null
//        tvCurrentDate = null
//        tvEmptyMessage = null
//        subjectAdapter = null
//        itemTouchHelper = null
//    }
//}
////package edu.sswu.vitaday
////
////import android.graphics.Color
////import android.os.Bundle
////import android.os.Handler
////import android.os.Looperpackage edu.sswu.vitaday
////
////import android.graphics.Color
////import android.os.Bundle
////import android.util.Log
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.Button
////import android.widget.EditText
////import android.widget.LinearLayout
////import android.widget.TextView
////import android.widget.Toast
////import androidx.appcompat.app.AlertDialog
////import androidx.core.graphics.toColorInt
////import androidx.fragment.app.Fragment
////import androidx.fragment.app.activityViewModels
////import androidx.lifecycle.lifecycleScope
////import androidx.recyclerview.widget.ItemTouchHelper
////import androidx.recyclerview.widget.LinearLayoutManager
////import androidx.recyclerview.widget.RecyclerView
////import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
////import edu.sswu.vitaday.ui.timer.TimerViewModel
////import kotlinx.coroutines.flow.collectLatest
////import kotlinx.coroutines.launch
////import java.util.Date
////
////class HomeFragment : Fragment() {
////
////    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
////    private val timerViewModel: TimerViewModel by activityViewModels()
////
////    // RecyclerView로 변경
////    private var rvSubjects: RecyclerView? = null
////    private var btnAddSubject: Button? = null
////    private var tvTodayStudyTime: TextView? = null
////    private var tvCurrentDate: TextView? = null
////    private var tvEmptyMessage: TextView? = null
////
////    private var subjectAdapter: SubjectAdapter? = null
////    private var itemTouchHelper: ItemTouchHelper? = null
////
////    override fun onCreateView(
////        inflater: LayoutInflater,
////        container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        return try {
////            inflater.inflate(R.layout.fragment_home, container, false)
////        } catch (e: Exception) {
////            Log.e("HomeFragment", "Error inflating layout", e)
////            null
////        }
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        try {
////            // View 초기화 - RecyclerView 사용
////            rvSubjects = view.findViewById(R.id.rv_subjects)
////            btnAddSubject = view.findViewById(R.id.btn_add_subject)
////            tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
////            tvCurrentDate = view.findViewById(R.id.tv_current_date)
////            tvEmptyMessage = view.findViewById(R.id.tv_empty_message)
////
////            if (rvSubjects == null || btnAddSubject == null || tvTodayStudyTime == null ||
////                tvCurrentDate == null || tvEmptyMessage == null) {
////                Log.e("HomeFragment", "Some views are null!")
////                Toast.makeText(requireContext(), "레이아웃 로딩 오류", Toast.LENGTH_SHORT).show()
////                return
////            }
////
////            // 현재 날짜 표시
////            val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
////            tvCurrentDate?.text = dateFormat.format(Date())
////
////            // RecyclerView 설정
////            setupRecyclerView()
////
////            // 과목 추가 버튼
////            btnAddSubject?.setOnClickListener {
////                showAddSubjectDialog()
////            }
////
////            // ViewModel 관찰
////            observeViewModel()
////
////        } catch (e: Exception) {
////            Log.e("HomeFragment", "Error in onViewCreated", e)
////            Toast.makeText(requireContext(), "초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
////        }
////    }
////
////    private fun setupRecyclerView() {
////        try {
////            subjectAdapter = SubjectAdapter(
////                onSubjectClick = { subject ->
////                    openTimerFragment(subject.id)
////                },
////                onSubjectDoubleClick = { subject ->
////                    showEditSubjectDialog(subject)
////                },
////                onSubjectDelete = { subject ->
////                    showDeleteConfirmDialog(subject)
////                },
////                getTodayTime = { subjectId ->
////                    val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subjectId] ?: 0L
////                    "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"
////                }
////            )
////
////            rvSubjects?.apply {
////                layoutManager = LinearLayoutManager(requireContext())
////                adapter = subjectAdapter
////            }
////
////            // 드래그 앤 드롭 설정
////            val callback = SubjectItemTouchHelper(subjectAdapter!!) {
////                val reorderedList = subjectAdapter?.getReorderedList() ?: return@SubjectItemTouchHelper
////                sharedViewModel.reorderSubjects(reorderedList)
////                Toast.makeText(requireContext(), "순서가 변경되었습니다", Toast.LENGTH_SHORT).show()
////            }
////            itemTouchHelper = ItemTouchHelper(callback)
////            itemTouchHelper?.attachToRecyclerView(rvSubjects)
////
////        } catch (e: Exception) {
////            Log.e("HomeFragment", "Error setting up RecyclerView", e)
////            Toast.makeText(requireContext(), "RecyclerView 설정 오류", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    private fun observeViewModel() {
////        viewLifecycleOwner.lifecycleScope.launch {
////            sharedViewModel.subjects.collectLatest { subjects ->
////                if (subjects.isEmpty()) {
////                    rvSubjects?.visibility = View.GONE
////                    tvEmptyMessage?.visibility = View.VISIBLE
////                } else {
////                    rvSubjects?.visibility = View.VISIBLE
////                    tvEmptyMessage?.visibility = View.GONE
////                    subjectAdapter?.submitList(subjects)
////                }
////            }
////        }
////
////        viewLifecycleOwner.lifecycleScope.launch {
////            timerViewModel.sessions.collectLatest {
////                val todayTime = timerViewModel.getTotalTimeForDate(Date())
////                tvTodayStudyTime?.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
////                subjectAdapter?.notifyDataSetChanged()
////            }
////        }
////    }
////
////    private fun openTimerFragment(subjectId: Int) {
////        try {
////            val fragment = PomodoroTimerFragment.newInstance(subjectId)
////            requireActivity().supportFragmentManager.beginTransaction()
////                .replace(R.id.nav_host_fragment, fragment)
////                .addToBackStack(null)
////                .commit()
////        } catch (e: Exception) {
////            Log.e("HomeFragment", "Error opening timer", e)
////            Toast.makeText(requireContext(), "타이머 열기 오류", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    private fun showAddSubjectDialog() {
////        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
////        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
////        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
////
////        val colors = listOf(
////            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
////            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
////            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
////        )
////
////        var selectedColor = colors[0]
////
////        colors.forEach { colorHex ->
////            val colorButton = View(requireContext()).apply {
////                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
////                    setMargins(8, 8, 8, 8)
////                }
////                setBackgroundColor(colorHex.toColorInt())
////                setOnClickListener {
////                    selectedColor = colorHex
////                    for (i in 0 until llColorPicker.childCount) {
////                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
////                    }
////                    setPadding(4, 4, 4, 4)
////                }
////            }
////            llColorPicker.addView(colorButton)
////        }
////
////        AlertDialog.Builder(requireContext())
////            .setTitle("과목 추가")
////            .setView(dialogView)
////            .setPositiveButton("추가") { _, _ ->
////                val name = etSubjectName.text.toString().trim()
////                if (name.isNotEmpty()) {
////                    sharedViewModel.addSubject(name, selectedColor)
////                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
////                }
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////
////    private fun showEditSubjectDialog(subject: SubjectData) {
////        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
////        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
////        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
////
////        etSubjectName.setText(subject.name)
////
////        val colors = listOf(
////            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
////            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
////            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
////        )
////
////        var selectedColor = subject.colorHex
////
////        colors.forEach { colorHex ->
////            val colorButton = View(requireContext()).apply {
////                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
////                    setMargins(8, 8, 8, 8)
////                }
////                setBackgroundColor(colorHex.toColorInt())
////
////                if (colorHex == subject.colorHex) {
////                    setPadding(4, 4, 4, 4)
////                }
////
////                setOnClickListener {
////                    selectedColor = colorHex
////                    for (i in 0 until llColorPicker.childCount) {
////                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
////                    }
////                    setPadding(4, 4, 4, 4)
////                }
////            }
////            llColorPicker.addView(colorButton)
////        }
////
////        AlertDialog.Builder(requireContext())
////            .setTitle("과목 수정")
////            .setView(dialogView)
////            .setPositiveButton("수정") { _, _ ->
////                val name = etSubjectName.text.toString().trim()
////                if (name.isNotEmpty()) {
////                    sharedViewModel.updateSubject(subject.id, name, selectedColor)
////                    Toast.makeText(requireContext(), "과목이 수정되었습니다", Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
////                }
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////
////    private fun showDeleteConfirmDialog(subject: SubjectData) {
////        AlertDialog.Builder(requireContext())
////            .setTitle("과목 삭제")
////            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
////            .setPositiveButton("삭제") { _, _ ->
////                sharedViewModel.removeSubject(subject.id)
////                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////
////    override fun onDestroyView() {
////        super.onDestroyView()
////        rvSubjects = null
////        btnAddSubject = null
////        tvTodayStudyTime = null
////        tvCurrentDate = null
////        tvEmptyMessage = null
////        subjectAdapter = null
////        itemTouchHelper = null
////    }
////}
////import android.view.GestureDetector
////import android.view.LayoutInflater
////import android.view.MotionEvent
////import android.view.View
////import android.view.ViewGroup
////import android.widget.Button
////import android.widget.EditText
////import android.widget.ImageButton
////import android.widget.LinearLayout
////import android.widget.TextView
////import android.widget.Toast
////import androidx.appcompat.app.AlertDialog
////import androidx.core.graphics.toColorInt
////import androidx.core.view.GestureDetectorCompat
////import androidx.fragment.app.Fragment
////import androidx.fragment.app.activityViewModels
////import androidx.lifecycle.lifecycleScope
////import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
////import edu.sswu.vitaday.ui.timer.TimerViewModel
////import kotlinx.coroutines.flow.collectLatest
////import kotlinx.coroutines.launch
////import java.util.Date
////
////class HomeFragment : Fragment() {
////
////    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
////    private val timerViewModel: TimerViewModel by activityViewModels()
////
////    private lateinit var llSubjectContainer: LinearLayout
////    private lateinit var btnAddSubject: Button
////    private lateinit var tvTodayStudyTime: TextView
////    private lateinit var tvCurrentDate: TextView
////
////    // 드래그 앤 드롭을 위한 변수
////    private var draggedView: View? = null
////    private var draggedSubjectId: Int? = null
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
////        llSubjectContainer = view.findViewById(R.id.ll_subject_container)
////        btnAddSubject = view.findViewById(R.id.btn_add_subject)
////        tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
////        tvCurrentDate = view.findViewById(R.id.tv_current_date)
////
////        // 현재 날짜 표시
////        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
////        tvCurrentDate.text = dateFormat.format(Date())
////
////        // 과목 추가 버튼
////        btnAddSubject.setOnClickListener {
////            showAddSubjectDialog()
////        }
////
////        // ViewModel 관찰
////        observeViewModel()
////    }
////
////    private fun observeViewModel() {
////        // 과목 리스트 관찰
////        viewLifecycleOwner.lifecycleScope.launch {
////            sharedViewModel.subjects.collectLatest { subjects ->
////                updateSubjectList(subjects)
////            }
////        }
////
////        // 오늘의 총 공부 시간 관찰
////        viewLifecycleOwner.lifecycleScope.launch {
////            timerViewModel.sessions.collectLatest {
////                val todayTime = timerViewModel.getTotalTimeForDate(Date())
////                tvTodayStudyTime.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
////            }
////        }
////    }
////
////    private fun updateSubjectList(subjects: List<SubjectData>) {
////        llSubjectContainer.removeAllViews()
////
////        if (subjects.isEmpty()) {
////            val emptyView = TextView(requireContext()).apply {
////                text = "과목을 추가해주세요"
////                textSize = 16f
////                setTextColor(Color.parseColor("#AAAAAA"))
////                gravity = android.view.Gravity.CENTER
////                setPadding(0, 32, 0, 32)
////            }
////            llSubjectContainer.addView(emptyView)
////            return
////        }
////
////        subjects.forEach { subject ->
////            val subjectCard = createSubjectCard(subject)
////            llSubjectContainer.addView(subjectCard)
////        }
////    }
////
////    private fun createSubjectCard(subject: SubjectData): View {
////        val cardView = layoutInflater.inflate(R.layout.item_subject_home_card, llSubjectContainer, false)
////
////        val viewColor = cardView.findViewById<View>(R.id.view_subject_color)
////        val tvName = cardView.findViewById<TextView>(R.id.tv_subject_name)
////        val tvTodayTime = cardView.findViewById<TextView>(R.id.tv_today_time)
////        val btnDelete = cardView.findViewById<ImageButton>(R.id.btn_delete_subject)
////
////        viewColor.setBackgroundColor(subject.colorHex.toColorInt())
////        tvName.text = subject.name
////
////        // 오늘의 과목별 공부 시간
////        val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subject.id] ?: 0L
////        tvTodayTime.text = "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"
////
////        // 제스처 감지기 설정
////        val gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
////            // 더블 클릭: 과목 수정
////            override fun onDoubleTap(e: MotionEvent): Boolean {
////                showEditSubjectDialog(subject)
////                return true
////            }
////
////            // 싱글 클릭: 타이머 시작
////            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
////                openTimerFragment(subject.id)
////                return true
////            }
////
////            // 롱 프레스: 순서 변경 모드
////            override fun onLongPress(e: MotionEvent) {
////                startDragMode(cardView, subject)
////            }
////        })
////
////        // 카드뷰 터치 리스너
////        cardView.setOnTouchListener { _, event ->
////            gestureDetector.onTouchEvent(event)
////            true
////        }
////
////        // 삭제 버튼 (X 아이콘)
////        btnDelete.setOnClickListener {
////            showDeleteConfirmDialog(subject)
////        }
////
////        return cardView
////    }
////
////    private fun startDragMode(view: View, subject: SubjectData) {
////        // 드래그 모드 시작 피드백
////        view.alpha = 0.7f
////        Toast.makeText(requireContext(), "드래그하여 순서 변경", Toast.LENGTH_SHORT).show()
////
////        draggedView = view
////        draggedSubjectId = subject.id
////
////        // TODO: 실제 드래그 앤 드롭 구현 (복잡하므로 여기서는 간단히 Toast만 표시)
////        // 실제 구현하려면 ItemTouchHelper와 RecyclerView 사용 권장
////
////        Handler(Looper.getMainLooper()).postDelayed({
////            view.alpha = 1.0f
////            draggedView = null
////            draggedSubjectId = null
////        }, 2000)
////    }
////
////    private fun openTimerFragment(subjectId: Int) {
////        val fragment = PomodoroTimerFragment.newInstance(subjectId)
////
////        requireActivity().supportFragmentManager.beginTransaction()
////            .replace(R.id.nav_host_fragment, fragment)
////            .addToBackStack(null)
////            .commit()
////    }
////
////    private fun showAddSubjectDialog() {
////        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
////        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
////        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
////
////        val colors = listOf(
////            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
////            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
////            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
////        )
////
////        var selectedColor = colors[0]
////
////        // 색상 선택 버튼 생성
////        colors.forEach { colorHex ->
////            val colorButton = View(requireContext()).apply {
////                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
////                    setMargins(8, 8, 8, 8)
////                }
////                setBackgroundColor(colorHex.toColorInt())
////                setOnClickListener {
////                    selectedColor = colorHex
////                    // 선택된 색상 표시 (테두리 추가)
////                    llColorPicker.children.forEach { view ->
////                        view.setPadding(0, 0, 0, 0)
////                    }
////                    setPadding(4, 4, 4, 4)
////                }
////            }
////            llColorPicker.addView(colorButton)
////        }
////
////        AlertDialog.Builder(requireContext())
////            .setTitle("과목 추가")
////            .setView(dialogView)
////            .setPositiveButton("추가") { _, _ ->
////                val name = etSubjectName.text.toString().trim()
////                if (name.isNotEmpty()) {
////                    sharedViewModel.addSubject(name, selectedColor)
////                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
////                }
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////
////    private fun showEditSubjectDialog(subject: SubjectData) {
////        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
////        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
////        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
////
////        // 기존 값 설정
////        etSubjectName.setText(subject.name)
////
////        val colors = listOf(
////            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
////            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
////            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
////        )
////
////        var selectedColor = subject.colorHex
////
////        // 색상 선택 버튼 생성
////        colors.forEach { colorHex ->
////            val colorButton = View(requireContext()).apply {
////                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
////                    setMargins(8, 8, 8, 8)
////                }
////                setBackgroundColor(colorHex.toColorInt())
////
////                // 현재 색상 표시
////                if (colorHex == subject.colorHex) {
////                    setPadding(4, 4, 4, 4)
////                }
////
////                setOnClickListener {
////                    selectedColor = colorHex
////                    llColorPicker.children.forEach { view ->
////                        view.setPadding(0, 0, 0, 0)
////                    }
////                    setPadding(4, 4, 4, 4)
////                }
////            }
////            llColorPicker.addView(colorButton)
////        }
////
////        AlertDialog.Builder(requireContext())
////            .setTitle("과목 수정")
////            .setView(dialogView)
////            .setPositiveButton("수정") { _, _ ->
////                val name = etSubjectName.text.toString().trim()
////                if (name.isNotEmpty()) {
////                    sharedViewModel.updateSubject(subject.id, name, selectedColor)
////                    Toast.makeText(requireContext(), "과목이 수정되었습니다", Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
////                }
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////
////    private fun showDeleteConfirmDialog(subject: SubjectData) {
////        AlertDialog.Builder(requireContext())
////            .setTitle("과목 삭제")
////            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
////            .setPositiveButton("삭제") { _, _ ->
////                sharedViewModel.removeSubject(subject.id)
////                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////}
////
////// ViewGroup의 자식 뷰들을 순회하기 위한 확장 함수
////val ViewGroup.children: Sequence<View>
////    get() = (0 until childCount).asSequence().map { getChildAt(it) }
////
//////package edu.sswu.vitaday
//////
//////import android.graphics.Color
//////import android.os.Bundle
//////import android.view.LayoutInflater
//////import android.view.View
//////import android.view.ViewGroup
//////import android.widget.Button
//////import android.widget.EditText
//////import android.widget.LinearLayout
//////import android.widget.TextView
//////import android.widget.Toast
//////import androidx.appcompat.app.AlertDialog
//////import androidx.core.graphics.toColorInt
//////import androidx.fragment.app.Fragment
//////import androidx.fragment.app.activityViewModels
//////import androidx.lifecycle.lifecycleScope
//////import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
//////import edu.sswu.vitaday.ui.timer.TimerViewModel
//////import kotlinx.coroutines.flow.collectLatest
//////import kotlinx.coroutines.launch
//////import java.util.Date
//////
//////class HomeFragment : Fragment() {
//////
//////    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
//////    private val timerViewModel: TimerViewModel by activityViewModels()
//////
//////    private lateinit var llSubjectContainer: LinearLayout
//////    private lateinit var btnAddSubject: Button
//////    private lateinit var tvTodayStudyTime: TextView
//////    private lateinit var tvCurrentDate: TextView
//////
//////    override fun onCreateView(
//////        inflater: LayoutInflater,
//////        container: ViewGroup?,
//////        savedInstanceState: Bundle?
//////    ): View? {
//////        return inflater.inflate(R.layout.fragment_home, container, false)
//////    }
//////
//////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////        super.onViewCreated(view, savedInstanceState)
//////
//////        llSubjectContainer = view.findViewById(R.id.ll_subject_container)
//////        btnAddSubject = view.findViewById(R.id.btn_add_subject)
//////        tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
//////        tvCurrentDate = view.findViewById(R.id.tv_current_date)
//////
//////        // 현재 날짜 표시
//////        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
//////        tvCurrentDate.text = dateFormat.format(Date())
//////
//////        // 과목 추가 버튼
//////        btnAddSubject.setOnClickListener {
//////            showAddSubjectDialog()
//////        }
//////
//////        // ViewModel 관찰
//////        observeViewModel()
//////    }
//////
//////    private fun observeViewModel() {
//////        // 과목 리스트 관찰
//////        viewLifecycleOwner.lifecycleScope.launch {
//////            sharedViewModel.subjects.collectLatest { subjects ->
//////                updateSubjectList(subjects)
//////            }
//////        }
//////
//////        // 오늘의 총 공부 시간 관찰
//////        viewLifecycleOwner.lifecycleScope.launch {
//////            timerViewModel.sessions.collectLatest {
//////                val todayTime = timerViewModel.getTotalTimeForDate(Date())
//////                tvTodayStudyTime.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
//////            }
//////        }
//////    }
//////
//////    private fun updateSubjectList(subjects: List<SubjectData>) {
//////        llSubjectContainer.removeAllViews()
//////
//////        if (subjects.isEmpty()) {
//////            val emptyView = TextView(requireContext()).apply {
//////                text = "과목을 추가해주세요"
//////                textSize = 16f
//////                setTextColor(Color.parseColor("#AAAAAA"))
//////                gravity = android.view.Gravity.CENTER
//////                setPadding(0, 32, 0, 32)
//////            }
//////            llSubjectContainer.addView(emptyView)
//////            return
//////        }
//////
//////        subjects.forEach { subject ->
//////            val subjectCard = createSubjectCard(subject)
//////            llSubjectContainer.addView(subjectCard)
//////        }
//////    }
//////
//////    private fun createSubjectCard(subject: SubjectData): View {
//////        val cardView = layoutInflater.inflate(R.layout.item_subject_home_card, llSubjectContainer, false)
//////
//////        val viewColor = cardView.findViewById<View>(R.id.view_subject_color)
//////        val tvName = cardView.findViewById<TextView>(R.id.tv_subject_name)
//////        val tvTodayTime = cardView.findViewById<TextView>(R.id.tv_today_time)
//////        val btnStartTimer = cardView.findViewById<Button>(R.id.btn_start_timer)
//////        val btnDelete = cardView.findViewById<Button>(R.id.btn_delete_subject)
//////
//////        viewColor.setBackgroundColor(subject.colorHex.toColorInt())
//////        tvName.text = subject.name
//////
//////        // 오늘의 과목별 공부 시간
//////        val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subject.id] ?: 0L
//////        tvTodayTime.text = "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"
//////
//////        // 타이머 시작 버튼
//////        btnStartTimer.setOnClickListener {
//////            openTimerFragment(subject.id)
//////        }
//////
//////        // 삭제 버튼
//////        btnDelete.setOnClickListener {
//////            showDeleteConfirmDialog(subject)
//////        }
//////
//////        return cardView
//////    }
//////
//////    private fun openTimerFragment(subjectId: Int) {
//////        val fragment = PomodoroTimerFragment.newInstance(subjectId)
//////
//////        // ✅ 수정: parentFragmentManager를 activity의 supportFragmentManager로 변경
//////        requireActivity().supportFragmentManager.beginTransaction()
//////            .replace(R.id.nav_host_fragment, fragment)
//////            .addToBackStack(null)
//////            .commit()
//////    }
//////
//////    private fun showAddSubjectDialog() {
//////        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
//////        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
//////        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
//////
//////        val colors = listOf(
//////            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
//////            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
//////            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
//////        )
//////
//////        var selectedColor = colors[0]
//////
//////        // 색상 선택 버튼 생성
//////        colors.forEach { colorHex ->
//////            val colorButton = View(requireContext()).apply {
//////                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
//////                    setMargins(8, 8, 8, 8)
//////                }
//////                setBackgroundColor(colorHex.toColorInt())
//////                setOnClickListener {
//////                    selectedColor = colorHex
//////                    // 선택된 색상 표시 (테두리 추가)
//////                    llColorPicker.children.forEach { view ->
//////                        view.setPadding(0, 0, 0, 0)
//////                    }
//////                    setPadding(4, 4, 4, 4)
//////                }
//////            }
//////            llColorPicker.addView(colorButton)
//////        }
//////
//////        AlertDialog.Builder(requireContext())
//////            .setTitle("과목 추가")
//////            .setView(dialogView)
//////            .setPositiveButton("추가") { _, _ ->
//////                val name = etSubjectName.text.toString().trim()
//////                if (name.isNotEmpty()) {
//////                    sharedViewModel.addSubject(name, selectedColor)
//////                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
//////                } else {
//////                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
//////                }
//////            }
//////            .setNegativeButton("취소", null)
//////            .show()
//////    }
//////
//////    private fun showDeleteConfirmDialog(subject: SubjectData) {
//////        AlertDialog.Builder(requireContext())
//////            .setTitle("과목 삭제")
//////            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
//////            .setPositiveButton("삭제") { _, _ ->
//////                sharedViewModel.removeSubject(subject.id)
//////                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
//////            }
//////            .setNegativeButton("취소", null)
//////            .show()
//////    }
//////}
//////
//////// ViewGroup의 자식 뷰들을 순회하기 위한 확장 함수
//////val ViewGroup.children: Sequence<View>
//////    get() = (0 until childCount).asSequence().map { getChildAt(it) }
////////package edu.sswu.vitaday
////////
////////import android.graphics.Color
////////import android.os.Bundle
////////import android.view.LayoutInflater
////////import android.view.View
////////import android.view.ViewGroup
////////import android.widget.Button
////////import android.widget.EditText
////////import android.widget.LinearLayout
////////import android.widget.TextView
////////import android.widget.Toast
////////import androidx.appcompat.app.AlertDialog
////////import androidx.core.graphics.toColorInt
////////import androidx.fragment.app.Fragment
////////import androidx.fragment.app.activityViewModels
////////import androidx.lifecycle.lifecycleScope
////////import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
////////import edu.sswu.vitaday.ui.timer.TimerViewModel
////////import kotlinx.coroutines.flow.collectLatest
////////import kotlinx.coroutines.launch
////////import java.util.Date
////////
////////class HomeFragment : Fragment() {
////////
////////    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
////////    private val timerViewModel: TimerViewModel by activityViewModels()
////////
////////    private lateinit var llSubjectContainer: LinearLayout
////////    private lateinit var btnAddSubject: Button
////////    private lateinit var tvTodayStudyTime: TextView
////////    private lateinit var tvCurrentDate: TextView
////////
////////    override fun onCreateView(
////////        inflater: LayoutInflater,
////////        container: ViewGroup?,
////////        savedInstanceState: Bundle?
////////    ): View? {
////////        return inflater.inflate(R.layout.fragment_home, container, false)
////////    }
////////
////////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////////        super.onViewCreated(view, savedInstanceState)
////////
////////        llSubjectContainer = view.findViewById(R.id.ll_subject_container)
////////        btnAddSubject = view.findViewById(R.id.btn_add_subject)
////////        tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
////////        tvCurrentDate = view.findViewById(R.id.tv_current_date)
////////
////////        // 현재 날짜 표시
////////        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
////////        tvCurrentDate.text = dateFormat.format(Date())
////////
////////        // 과목 추가 버튼
////////        btnAddSubject.setOnClickListener {
////////            showAddSubjectDialog()
////////        }
////////
////////        // ViewModel 관찰
////////        observeViewModel()
////////    }
////////
////////    private fun observeViewModel() {
////////        // 과목 리스트 관찰
////////        viewLifecycleOwner.lifecycleScope.launch {
////////            sharedViewModel.subjects.collectLatest { subjects ->
////////                updateSubjectList(subjects)
////////            }
////////        }
////////
////////        // 오늘의 총 공부 시간 관찰
////////        viewLifecycleOwner.lifecycleScope.launch {
////////            timerViewModel.sessions.collectLatest {
////////                val todayTime = timerViewModel.getTotalTimeForDate(Date())
////////                tvTodayStudyTime.text = "오늘 공부 시간: ${timerViewModel.formatTimeInMinutes(todayTime)}"
////////            }
////////        }
////////    }
////////
////////    private fun updateSubjectList(subjects: List<SubjectData>) {
////////        llSubjectContainer.removeAllViews()
////////
////////        if (subjects.isEmpty()) {
////////            val emptyView = TextView(requireContext()).apply {
////////                text = "과목을 추가해주세요"
////////                textSize = 16f
////////                setTextColor(Color.parseColor("#AAAAAA"))
////////                gravity = android.view.Gravity.CENTER
////////                setPadding(0, 32, 0, 32)
////////            }
////////            llSubjectContainer.addView(emptyView)
////////            return
////////        }
////////
////////        subjects.forEach { subject ->
////////            val subjectCard = createSubjectCard(subject)
////////            llSubjectContainer.addView(subjectCard)
////////        }
////////    }
////////
////////    private fun createSubjectCard(subject: SubjectData): View {
////////        val cardView = layoutInflater.inflate(R.layout.item_subject_home_card, llSubjectContainer, false)
////////
////////        val viewColor = cardView.findViewById<View>(R.id.view_subject_color)
////////        val tvName = cardView.findViewById<TextView>(R.id.tv_subject_name)
////////        val tvTodayTime = cardView.findViewById<TextView>(R.id.tv_today_time)
////////        val btnStartTimer = cardView.findViewById<Button>(R.id.btn_start_timer)
////////        val btnDelete = cardView.findViewById<Button>(R.id.btn_delete_subject)
////////
////////        viewColor.setBackgroundColor(subject.colorHex.toColorInt())
////////        tvName.text = subject.name
////////
////////        // 오늘의 과목별 공부 시간
////////        val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subject.id] ?: 0L
////////        tvTodayTime.text = "오늘: ${timerViewModel.formatTimeInMinutes(todayTime)}"
////////
////////        // 타이머 시작 버튼
////////        btnStartTimer.setOnClickListener {
////////            openTimerFragment(subject.id)
////////        }
////////
////////        // 삭제 버튼
////////        btnDelete.setOnClickListener {
////////            showDeleteConfirmDialog(subject)
////////        }
////////
////////        return cardView
////////    }
////////
////////    private fun openTimerFragment(subjectId: Int) {
////////        val fragment = PomodoroTimerFragment.newInstance(subjectId)
////////        parentFragmentManager.beginTransaction()
////////            .replace(R.id.nav_host_fragment, fragment)
////////            .addToBackStack(null)
////////            .commit()
////////    }
////////
////////    private fun showAddSubjectDialog() {
////////        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
////////        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
////////        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)
////////
////////        val colors = listOf(
////////            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
////////            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
////////            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
////////        )
////////
////////        var selectedColor = colors[0]
////////
////////        // 색상 선택 버튼 생성
////////        colors.forEach { colorHex ->
////////            val colorButton = View(requireContext()).apply {
////////                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
////////                    setMargins(8, 8, 8, 8)
////////                }
////////                setBackgroundColor(colorHex.toColorInt())
////////                setOnClickListener {
////////                    selectedColor = colorHex
////////                    // 선택된 색상 표시 (테두리 추가)
////////                    llColorPicker.children.forEach { view ->
////////                        view.setPadding(0, 0, 0, 0)
////////                    }
////////                    setPadding(4, 4, 4, 4)
////////                }
////////            }
////////            llColorPicker.addView(colorButton)
////////        }
////////
////////        AlertDialog.Builder(requireContext())
////////            .setTitle("과목 추가")
////////            .setView(dialogView)
////////            .setPositiveButton("추가") { _, _ ->
////////                val name = etSubjectName.text.toString().trim()
////////                if (name.isNotEmpty()) {
////////                    sharedViewModel.addSubject(name, selectedColor)
////////                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
////////                } else {
////////                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
////////                }
////////            }
////////            .setNegativeButton("취소", null)
////////            .show()
////////    }
////////
////////    private fun showDeleteConfirmDialog(subject: SubjectData) {
////////        AlertDialog.Builder(requireContext())
////////            .setTitle("과목 삭제")
////////            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
////////            .setPositiveButton("삭제") { _, _ ->
////////                sharedViewModel.removeSubject(subject.id)
////////                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
////////            }
////////            .setNegativeButton("취소", null)
////////            .show()
////////    }
////////}
////////
////////// ViewGroup의 자식 뷰들을 순회하기 위한 확장 함수
////////val ViewGroup.children: Sequence<View>
////////    get() = (0 until childCount).asSequence().map { getChildAt(it) }
//////////package edu.sswu.vitaday
//////////
//////////import android.os.Bundle
//////////import android.view.LayoutInflater
//////////import android.view.View
//////////import android.view.ViewGroup
//////////import androidx.fragment.app.Fragment
//////////
//////////class HomeFragment : Fragment() {
//////////
//////////    override fun onCreateView(
//////////        inflater: LayoutInflater,
//////////        container: ViewGroup?,
//////////        savedInstanceState: Bundle?
//////////    ): View? {
//////////        return inflater.inflate(R.layout.fragment_home, container, false)
//////////    }
//////////
//////////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////////        super.onViewCreated(view, savedInstanceState)
//////////
//////////        // TODO: 세원님이 구현하는 뽀모도로 타이머 로직 추가
//////////        // TODO: 과목 생성 기능 추가
//////////    }
//////////}