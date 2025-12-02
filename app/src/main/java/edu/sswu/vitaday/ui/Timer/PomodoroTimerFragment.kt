package edu.sswu.vitaday.ui.timer


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import edu.sswu.vitaday.R
import edu.sswu.vitaday.SharedSubjectViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 뽀모도로 타이머 Fragment
 * 원형 다이얼로 시간을 설정하고 타이머를 실행
 */
class PomodoroTimerFragment : Fragment() {

    private val timerViewModel: TimerViewModel by activityViewModels()
    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()

    private lateinit var circularTimerView: CircularTimerView
    private lateinit var tvSubjectName: TextView
    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnBack: ImageButton

    private var currentSubjectId: Int = 0
    private var currentSubjectName: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            timerViewModel.tick()
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val ARG_SUBJECT_ID = "subject_id"

        fun newInstance(subjectId: Int): PomodoroTimerFragment {
            return PomodoroTimerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SUBJECT_ID, subjectId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pomodoro_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View 초기화
        circularTimerView = view.findViewById(R.id.circular_timer_view)
        tvSubjectName = view.findViewById(R.id.tv_subject_name)
        tvTimerDisplay = view.findViewById(R.id.tv_timer_display)
        btnStart = view.findViewById(R.id.btn_start)
        btnPause = view.findViewById(R.id.btn_pause)
        btnStop = view.findViewById(R.id.btn_stop)
        btnReset = view.findViewById(R.id.btn_reset)
        btnBack = view.findViewById(R.id.btn_back)

        // 전달받은 과목 ID 가져오기
        currentSubjectId = arguments?.getInt(ARG_SUBJECT_ID) ?: return
        val subject = sharedViewModel.getSubjectById(currentSubjectId)

        if (subject == null) {
            Toast.makeText(requireContext(), "과목을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        currentSubjectName = subject.name
        tvSubjectName.text = subject.name
        timerViewModel.selectSubject(currentSubjectId)

        // 원형 다이얼 초기 설정
        circularTimerView.setMinutes(20)

        setupButtons()
        setupCircularTimer()
        observeViewModel()
    }

    private fun setupCircularTimer() {
        // 시간 변경 리스너
        circularTimerView.onTimeChangedListener = { minutes ->
            Log.d("PomodoroTimer", "Time changed to: $minutes minutes")
        }
    }

    private fun setupButtons() {
        // 시작 버튼
        btnStart.setOnClickListener {
            val minutes = circularTimerView.getMinutes()
            Log.d("PomodoroTimer", "Starting timer for $minutes minutes")

            timerViewModel.startTimer(minutes, currentSubjectId)
            startTimerHandler()

            // 원형 다이얼 터치 비활성화
            circularTimerView.isEnabled = false

            Toast.makeText(requireContext(), "${minutes}분 타이머 시작", Toast.LENGTH_SHORT).show()
        }

        // 일시정지/재개 버튼 (수정됨)
        btnPause.setOnClickListener {
            if (timerViewModel.isPaused.value) {
                // 현재 일시정지 상태라면 -> 재개
                timerViewModel.resumeTimer()
                startTimerHandler() // 핸들러 다시 시작
                Toast.makeText(requireContext(), "타이머 재개", Toast.LENGTH_SHORT).show()
            } else {
                // 현재 실행 중이라면 -> 일시정지
                timerViewModel.pauseTimer()
                stopTimerHandler() // 핸들러 정지
                Toast.makeText(requireContext(), "일시정지", Toast.LENGTH_SHORT).show()
            }
        }

        // 정지 버튼 (세션 저장)
        btnStop.setOnClickListener {
            Log.d("PomodoroTimer", "Stopping timer and saving session")
            timerViewModel.stopTimer(currentSubjectName)
            stopTimerHandler()

            // 원형 다이얼 터치 활성화
            circularTimerView.isEnabled = true
            circularTimerView.setMinutes(20)

            Toast.makeText(requireContext(), "타이머 종료 및 저장 완료", Toast.LENGTH_SHORT).show()
        }

        // 리셋 버튼
        btnReset.setOnClickListener {
            timerViewModel.resetTimer()
            stopTimerHandler()
            circularTimerView.setMinutes(20)

            // 원형 다이얼 터치 활성화
            circularTimerView.isEnabled = true

            Toast.makeText(requireContext(), "리셋", Toast.LENGTH_SHORT).show()
        }

        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            // 실행 중이면 경고
            if (timerViewModel.isRunning.value) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("타이머 실행 중")
                    .setMessage("타이머가 실행 중입니다. 나가시겠습니까?")
                    .setPositiveButton("나가기") { _, _ ->
                        timerViewModel.resetTimer()
                        stopTimerHandler()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            } else {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    private fun observeViewModel() {
        // 남은 시간 표시
        viewLifecycleOwner.lifecycleScope.launch {
            timerViewModel.remainingTime.collectLatest { time ->
                // 시:분:초 형식으로 표시
                tvTimerDisplay.text = timerViewModel.formatTime(time)

                // 타이머가 0이 되면
                if (time <= 0 && timerViewModel.isRunning.value) {
                    stopTimerHandler()
                    Toast.makeText(requireContext(), "타이머 완료!", Toast.LENGTH_LONG).show()

                    // 자동으로 세션 저장
                    timerViewModel.stopTimer(currentSubjectName)
                    circularTimerView.isEnabled = true
                }
            }
        }

        // 타이머 실행 상태에 따른 버튼 표시
        viewLifecycleOwner.lifecycleScope.launch {
            timerViewModel.isRunning.collectLatest { isRunning ->
                updateButtonsVisibility(isRunning)
            }
        }

        // 일시정지 상태에 따라 버튼 텍스트 변경
        viewLifecycleOwner.lifecycleScope.launch {
            timerViewModel.isPaused.collectLatest { isPaused ->
                if (isPaused) {
                    btnPause.text = "재개"
                } else {
                    btnPause.text = "일시정지"
                }
            }
        }
    }

    private fun updateButtonsVisibility(isRunning: Boolean) {
        if (isRunning) {
            // 타이머 실행 중
            btnStart.visibility = View.GONE
            btnPause.visibility = View.VISIBLE
            btnStop.visibility = View.VISIBLE
            btnReset.visibility = View.VISIBLE
        } else {
            // 타이머 대기 중
            btnStart.visibility = View.VISIBLE
            btnPause.visibility = View.GONE
            btnStop.visibility = View.GONE
            btnReset.visibility = View.GONE
        }
    }

    private fun startTimerHandler() {
        // 중복 실행 방지
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private fun stopTimerHandler() {
        handler.removeCallbacks(timerRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimerHandler()
    }
}
//package edu.sswu.vitaday.ui.timer
//
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.lifecycleScope
//import edu.sswu.vitaday.R
//import edu.sswu.vitaday.SharedSubjectViewModel
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.launch
//
///**
// * 뽀모도로 타이머 Fragment
// * 원형 다이얼로 시간을 설정하고 타이머를 실행
// */
//class PomodoroTimerFragment : Fragment() {
//
//    private val timerViewModel: TimerViewModel by activityViewModels()
//    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
//
//    private lateinit var circularTimerView: CircularTimerView
//    private lateinit var tvSubjectName: TextView
//    private lateinit var tvTimerDisplay: TextView
//    private lateinit var btnStart: Button
//    private lateinit var btnPause: Button
//    private lateinit var btnStop: Button
//    private lateinit var btnReset: Button
//    private lateinit var btnBack: ImageButton
//
//    private var currentSubjectId: Int = 0
//    private var currentSubjectName: String = ""
//
//    private val handler = Handler(Looper.getMainLooper())
//    private val timerRunnable = object : Runnable {
//        override fun run() {
//            timerViewModel.tick()
//            handler.postDelayed(this, 1000)
//        }
//    }
//
//    companion object {
//        private const val ARG_SUBJECT_ID = "subject_id"
//
//        fun newInstance(subjectId: Int): PomodoroTimerFragment {
//            return PomodoroTimerFragment().apply {
//                arguments = Bundle().apply {
//                    putInt(ARG_SUBJECT_ID, subjectId)
//                }
//            }
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_pomodoro_timer, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // View 초기화
//        circularTimerView = view.findViewById(R.id.circular_timer_view)
//        tvSubjectName = view.findViewById(R.id.tv_subject_name)
//        tvTimerDisplay = view.findViewById(R.id.tv_timer_display)
//        btnStart = view.findViewById(R.id.btn_start)
//        btnPause = view.findViewById(R.id.btn_pause)
//        btnStop = view.findViewById(R.id.btn_stop)
//        btnReset = view.findViewById(R.id.btn_reset)
//        btnBack = view.findViewById(R.id.btn_back)
//
//        // 전달받은 과목 ID 가져오기
//        currentSubjectId = arguments?.getInt(ARG_SUBJECT_ID) ?: return
//        val subject = sharedViewModel.getSubjectById(currentSubjectId)
//
//        if (subject == null) {
//            Toast.makeText(requireContext(), "과목을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
//            requireActivity().supportFragmentManager.popBackStack()
//            return
//        }
//
//        currentSubjectName = subject.name
//        tvSubjectName.text = subject.name
//        timerViewModel.selectSubject(currentSubjectId)
//
//        // 원형 다이얼 초기 설정
//        circularTimerView.setMinutes(20)
//
//        setupButtons()
//        setupCircularTimer()
//        observeViewModel()
//    }
//
//    private fun setupCircularTimer() {
//        // 시간 변경 리스너
//        circularTimerView.onTimeChangedListener = { minutes ->
//            Log.d("PomodoroTimer", "Time changed to: $minutes minutes")
//        }
//    }
//
//    private fun setupButtons() {
//        // 시작 버튼
//        btnStart.setOnClickListener {
//            val minutes = circularTimerView.getMinutes()
//            Log.d("PomodoroTimer", "Starting timer for $minutes minutes")
//
//            timerViewModel.startTimer(minutes, currentSubjectId)
//            startTimerHandler()
//
//            // 원형 다이얼 터치 비활성화
//            circularTimerView.isEnabled = false
//
//            Toast.makeText(requireContext(), "${minutes}분 타이머 시작", Toast.LENGTH_SHORT).show()
//        }
//
//        // 일시정지 버튼
//        btnPause.setOnClickListener {
//            timerViewModel.pauseTimer()
//            stopTimerHandler()
//            Toast.makeText(requireContext(), "일시정지", Toast.LENGTH_SHORT).show()
//        }
//
//        // 재개 (일시정지 후 다시 시작)
//        btnStart.setOnClickListener {
//            if (timerViewModel.isPaused.value) {
//                timerViewModel.resumeTimer()
//                startTimerHandler()
//                Toast.makeText(requireContext(), "재개", Toast.LENGTH_SHORT).show()
//            } else if (!timerViewModel.isRunning.value) {
//                val minutes = circularTimerView.getMinutes()
//                timerViewModel.startTimer(minutes, currentSubjectId)
//                startTimerHandler()
//                circularTimerView.isEnabled = false
//                Toast.makeText(requireContext(), "${minutes}분 타이머 시작", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        // 정지 버튼 (세션 저장)
//        btnStop.setOnClickListener {
//            Log.d("PomodoroTimer", "Stopping timer and saving session")
//            timerViewModel.stopTimer(currentSubjectName)
//            stopTimerHandler()
//
//            // 원형 다이얼 터치 활성화
//            circularTimerView.isEnabled = true
//            circularTimerView.setMinutes(20)
//
//            Toast.makeText(requireContext(), "타이머 종료 및 저장 완료", Toast.LENGTH_SHORT).show()
//        }
//
//        // 리셋 버튼
//        btnReset.setOnClickListener {
//            timerViewModel.resetTimer()
//            stopTimerHandler()
//            circularTimerView.setMinutes(20)
//
//            // 원형 다이얼 터치 활성화
//            circularTimerView.isEnabled = true
//
//            Toast.makeText(requireContext(), "리셋", Toast.LENGTH_SHORT).show()
//        }
//
//        // 뒤로가기 버튼
//        btnBack.setOnClickListener {
//            // 실행 중이면 경고
//            if (timerViewModel.isRunning.value) {
//                android.app.AlertDialog.Builder(requireContext())
//                    .setTitle("타이머 실행 중")
//                    .setMessage("타이머가 실행 중입니다. 나가시겠습니까?")
//                    .setPositiveButton("나가기") { _, _ ->
//                        timerViewModel.resetTimer()
//                        stopTimerHandler()
//                        requireActivity().supportFragmentManager.popBackStack()
//                    }
//                    .setNegativeButton("취소", null)
//                    .show()
//            } else {
//                requireActivity().supportFragmentManager.popBackStack()
//            }
//        }
//    }
//
//    private fun observeViewModel() {
//        // 남은 시간 표시
//        viewLifecycleOwner.lifecycleScope.launch {
//            timerViewModel.remainingTime.collectLatest { time ->
//                // 시:분:초 형식으로 표시
//                tvTimerDisplay.text = timerViewModel.formatTime(time)
//
//                // 타이머가 0이 되면
//                if (time <= 0 && timerViewModel.isRunning.value) {
//                    stopTimerHandler()
//                    Toast.makeText(requireContext(), "타이머 완료!", Toast.LENGTH_LONG).show()
//
//                    // 자동으로 세션 저장
//                    timerViewModel.stopTimer(currentSubjectName)
//                    circularTimerView.isEnabled = true
//                }
//            }
//        }
//
//        // 타이머 실행 상태에 따른 버튼 표시
//        viewLifecycleOwner.lifecycleScope.launch {
//            timerViewModel.isRunning.collectLatest { isRunning ->
//                updateButtonsVisibility(isRunning)
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            timerViewModel.isPaused.collectLatest { isPaused ->
//                if (isPaused) {
//                    btnPause.text = "재개"
//                } else {
//                    btnPause.text = "일시정지"
//                }
//            }
//        }
//    }
//
//    private fun updateButtonsVisibility(isRunning: Boolean) {
//        if (isRunning) {
//            // 타이머 실행 중
//            btnStart.visibility = View.GONE
//            btnPause.visibility = View.VISIBLE
//            btnStop.visibility = View.VISIBLE
//            btnReset.visibility = View.VISIBLE
//        } else {
//            // 타이머 대기 중
//            btnStart.visibility = View.VISIBLE
//            btnPause.visibility = View.GONE
//            btnStop.visibility = View.GONE
//            btnReset.visibility = View.GONE
//        }
//    }
//
//    private fun startTimerHandler() {
//        handler.post(timerRunnable)
//    }
//
//    private fun stopTimerHandler() {
//        handler.removeCallbacks(timerRunnable)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        stopTimerHandler()
//    }
//}
////package edu.sswu.vitaday.ui.timer
////
////import android.os.Bundle
////import android.os.Handler
////import android.os.Looper
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.Button
////import android.widget.ImageButton
////import android.widget.TextView
////import androidx.fragment.app.Fragment
////import androidx.fragment.app.activityViewModels
////import androidx.lifecycle.lifecycleScope
////import edu.sswu.vitaday.R
////import edu.sswu.vitaday.SharedSubjectViewModel
////import kotlinx.coroutines.flow.collectLatest
////import kotlinx.coroutines.launch
////
/////**
//// * 뽀모도로 타이머 Fragment
//// * 원형 다이얼로 시간을 설정하고 타이머를 실행
//// */
////class PomodoroTimerFragment : Fragment() {
////
////    private val timerViewModel: TimerViewModel by activityViewModels()
////    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
////
////    private lateinit var circularTimerView: CircularTimerView
////    private lateinit var tvSubjectName: TextView
////    private lateinit var btnStart: Button
////    private lateinit var btnPause: Button
////    private lateinit var btnStop: Button
////    private lateinit var btnReset: Button
////    private lateinit var btnBack: ImageButton
////
////    private val handler = Handler(Looper.getMainLooper())
////    private val timerRunnable = object : Runnable {
////        override fun run() {
////            timerViewModel.tick()
////            handler.postDelayed(this, 1000)
////        }
////    }
////
////    companion object {
////        private const val ARG_SUBJECT_ID = "subject_id"
////
////        fun newInstance(subjectId: Int): PomodoroTimerFragment {
////            return PomodoroTimerFragment().apply {
////                arguments = Bundle().apply {
////                    putInt(ARG_SUBJECT_ID, subjectId)
////                }
////            }
////        }
////    }
////
////    override fun onCreateView(
////        inflater: LayoutInflater,
////        container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        return inflater.inflate(R.layout.fragment_pomodoro_timer, container, false)
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        // View 초기화
////        circularTimerView = view.findViewById(R.id.circular_timer_view)
////        tvSubjectName = view.findViewById(R.id.tv_subject_name)
////        btnStart = view.findViewById(R.id.btn_start)
////        btnPause = view.findViewById(R.id.btn_pause)
////        btnStop = view.findViewById(R.id.btn_stop)
////        btnReset = view.findViewById(R.id.btn_reset)
////        btnBack = view.findViewById(R.id.btn_back)
////
////        // 전달받은 과목 ID 가져오기
////        val subjectId = arguments?.getInt(ARG_SUBJECT_ID) ?: return
////        val subject = sharedViewModel.getSubjectById(subjectId)
////
////        if (subject == null) {
////            // 과목이 없으면 뒤로가기
////            requireActivity().supportFragmentManager.popBackStack()
////            return
////        }
////
////        tvSubjectName.text = subject.name
////        timerViewModel.selectSubject(subjectId)
////
////        // 원형 다이얼 초기 설정
////        circularTimerView.setMinutes(20)
////
////        setupButtons(subjectId, subject.name)
////        setupCircularTimer()
////        observeViewModel()
////    }
////
////    private fun setupCircularTimer() {
////        // 시간 변경 리스너
////        circularTimerView.onTimeChangedListener = { minutes ->
////            // 타이머가 실행 중이 아닐 때만 시간 변경 가능
////            if (!timerViewModel.isRunning.value) {
////                // 시간 표시 업데이트는 CircularTimerView 내부에서 처리
////            }
////        }
////    }
////
////    private fun setupButtons(subjectId: Int, subjectName: String) {
////        // 시작 버튼
////        btnStart.setOnClickListener {
////            val minutes = circularTimerView.getMinutes()
////            timerViewModel.startTimer(minutes, subjectId)
////            startTimerHandler()
////
////            // 원형 다이얼 터치 비활성화
////            circularTimerView.isEnabled = false
////        }
////
////        // 일시정지 버튼
////        btnPause.setOnClickListener {
////            timerViewModel.pauseTimer()
////            stopTimerHandler()
////        }
////
////        // 재개 버튼 (일시정지 후)
////        btnPause.setOnLongClickListener {
////            timerViewModel.resumeTimer()
////            startTimerHandler()
////            true
////        }
////
////        // 정지 버튼
////        btnStop.setOnClickListener {
////            timerViewModel.stopTimer(subjectName)
////            stopTimerHandler()
////
////            // 원형 다이얼 터치 활성화
////            circularTimerView.isEnabled = true
////        }
////
////        // 리셋 버튼
////        btnReset.setOnClickListener {
////            timerViewModel.resetTimer()
////            stopTimerHandler()
////            circularTimerView.setMinutes(20)
////
////            // 원형 다이얼 터치 활성화
////            circularTimerView.isEnabled = true
////        }
////
////        // 뒤로가기 버튼
////        btnBack.setOnClickListener {
////            requireActivity().supportFragmentManager.popBackStack()
////        }
////    }
////
////    private fun observeViewModel() {
////        // 남은 시간 표시 (원형 다이얼 업데이트)
////        viewLifecycleOwner.lifecycleScope.launch {
////            timerViewModel.remainingTime.collectLatest { time ->
////                // 분 단위로 변환
////                val remainingMinutes = (time / 1000 / 60).toInt()
////                if (remainingMinutes > 0) {
////                    circularTimerView.setMinutes(remainingMinutes)
////                }
////            }
////        }
////
////        // 타이머 실행 상태에 따른 버튼 표시
////        viewLifecycleOwner.lifecycleScope.launch {
////            timerViewModel.isRunning.collectLatest { isRunning ->
////                updateButtonsVisibility(isRunning)
////            }
////        }
////
////        viewLifecycleOwner.lifecycleScope.launch {
////            timerViewModel.isPaused.collectLatest { isPaused ->
////                if (isPaused) {
////                    btnStart.visibility = View.VISIBLE
////                    btnPause.visibility = View.GONE
////                } else if (timerViewModel.isRunning.value) {
////                    btnStart.visibility = View.GONE
////                    btnPause.visibility = View.VISIBLE
////                }
////            }
////        }
////    }
////
////    private fun updateButtonsVisibility(isRunning: Boolean) {
////        if (isRunning) {
////            // 타이머 실행 중
////            btnStart.visibility = View.GONE
////            btnPause.visibility = View.VISIBLE
////            btnStop.visibility = View.VISIBLE
////            btnReset.visibility = View.VISIBLE
////        } else {
////            // 타이머 대기 중
////            btnStart.visibility = View.VISIBLE
////            btnPause.visibility = View.GONE
////            btnStop.visibility = View.GONE
////            btnReset.visibility = View.GONE
////        }
////    }
////
////    private fun startTimerHandler() {
////        handler.post(timerRunnable)
////    }
////
////    private fun stopTimerHandler() {
////        handler.removeCallbacks(timerRunnable)
////    }
////
////    override fun onDestroyView() {
////        super.onDestroyView()
////        stopTimerHandler()
////    }
////}
//////package edu.sswu.vitaday.ui.timer
//////
//////import android.os.Bundle
//////import android.os.Handler
//////import android.os.Looper
//////import android.view.LayoutInflater
//////import android.view.View
//////import android.view.ViewGroup
//////import android.widget.Button
//////import android.widget.ImageButton
//////import android.widget.TextView
//////import androidx.fragment.app.Fragment
//////import androidx.fragment.app.activityViewModels
//////import androidx.lifecycle.lifecycleScope
//////import edu.sswu.vitaday.R
//////import edu.sswu.vitaday.SharedSubjectViewModel
//////import kotlinx.coroutines.flow.collectLatest
//////import kotlinx.coroutines.launch
//////
///////**
////// * 뽀모도로 타이머 Fragment
////// * 선택한 과목으로 타이머를 실행
////// */
//////class PomodoroTimerFragment : Fragment() {
//////
//////    private val timerViewModel: TimerViewModel by activityViewModels()
//////    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()
//////
//////    private lateinit var tvTimerDisplay: TextView
//////    private lateinit var tvSubjectName: TextView
//////    private lateinit var btnStart: Button
//////    private lateinit var btnPause: Button
//////    private lateinit var btnStop: Button
//////    private lateinit var btnReset: Button
//////    private lateinit var btnBack: ImageButton
//////    private lateinit var btn10Min: Button
//////    private lateinit var btn20Min: Button
//////    private lateinit var btn30Min: Button
//////
//////    private val handler = Handler(Looper.getMainLooper())
//////    private val timerRunnable = object : Runnable {
//////        override fun run() {
//////            timerViewModel.tick()
//////            handler.postDelayed(this, 1000)
//////        }
//////    }
//////
//////    companion object {
//////        private const val ARG_SUBJECT_ID = "subject_id"
//////
//////        fun newInstance(subjectId: Int): PomodoroTimerFragment {
//////            return PomodoroTimerFragment().apply {
//////                arguments = Bundle().apply {
//////                    putInt(ARG_SUBJECT_ID, subjectId)
//////                }
//////            }
//////        }
//////    }
//////
//////    override fun onCreateView(
//////        inflater: LayoutInflater,
//////        container: ViewGroup?,
//////        savedInstanceState: Bundle?
//////    ): View? {
//////        return inflater.inflate(R.layout.fragment_pomodoro_timer, container, false)
//////    }
//////
//////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////        super.onViewCreated(view, savedInstanceState)
//////
//////        // View 초기화
//////        tvTimerDisplay = view.findViewById(R.id.tv_timer_display)
//////        tvSubjectName = view.findViewById(R.id.tv_subject_name)
//////        btnStart = view.findViewById(R.id.btn_start)
//////        btnPause = view.findViewById(R.id.btn_pause)
//////        btnStop = view.findViewById(R.id.btn_stop)
//////        btnReset = view.findViewById(R.id.btn_reset)
//////        btnBack = view.findViewById(R.id.btn_back)
//////        btn10Min = view.findViewById(R.id.btn_10min)
//////        btn20Min = view.findViewById(R.id.btn_20min)
//////        btn30Min = view.findViewById(R.id.btn_30min)
//////
//////        // 전달받은 과목 ID 가져오기
//////        val subjectId = arguments?.getInt(ARG_SUBJECT_ID) ?: return
//////        val subject = sharedViewModel.getSubjectById(subjectId)
//////
//////        if (subject == null) {
//////            // 과목이 없으면 뒤로가기
//////            parentFragmentManager.popBackStack()
//////            return
//////        }
//////
//////        tvSubjectName.text = subject.name
//////        timerViewModel.selectSubject(subjectId)
//////
//////        setupButtons(subjectId, subject.name)
//////        observeViewModel()
//////    }
//////
//////    private fun setupButtons(subjectId: Int, subjectName: String) {
//////        // 시간 설정 버튼
//////        btn10Min.setOnClickListener {
//////            timerViewModel.startTimer(10, subjectId)
//////            startTimerHandler()
//////        }
//////
//////        btn20Min.setOnClickListener {
//////            timerViewModel.startTimer(20, subjectId)
//////            startTimerHandler()
//////        }
//////
//////        btn30Min.setOnClickListener {
//////            timerViewModel.startTimer(30, subjectId)
//////            startTimerHandler()
//////        }
//////
//////        // 시작 버튼
//////        btnStart.setOnClickListener {
//////            timerViewModel.resumeTimer()
//////            startTimerHandler()
//////        }
//////
//////        // 일시정지 버튼
//////        btnPause.setOnClickListener {
//////            timerViewModel.pauseTimer()
//////            stopTimerHandler()
//////        }
//////
//////        // 정지 버튼
//////        btnStop.setOnClickListener {
//////            timerViewModel.stopTimer(subjectName)
//////            stopTimerHandler()
//////        }
//////
//////        // 리셋 버튼
//////        btnReset.setOnClickListener {
//////            timerViewModel.resetTimer()
//////            stopTimerHandler()
//////        }
//////
//////        // 뒤로가기 버튼
//////        btnBack.setOnClickListener {
//////            parentFragmentManager.popBackStack()
//////        }
//////    }
//////
//////    private fun observeViewModel() {
//////        // 남은 시간 표시
//////        viewLifecycleOwner.lifecycleScope.launch {
//////            timerViewModel.remainingTime.collectLatest { time ->
//////                tvTimerDisplay.text = timerViewModel.formatTime(time)
//////            }
//////        }
//////
//////        // 타이머 실행 상태에 따른 버튼 표시
//////        viewLifecycleOwner.lifecycleScope.launch {
//////            timerViewModel.isRunning.collectLatest { isRunning ->
//////                updateButtonsVisibility(isRunning)
//////            }
//////        }
//////
//////        viewLifecycleOwner.lifecycleScope.launch {
//////            timerViewModel.isPaused.collectLatest { isPaused ->
//////                if (isPaused) {
//////                    btnStart.visibility = View.VISIBLE
//////                    btnPause.visibility = View.GONE
//////                } else {
//////                    btnStart.visibility = View.GONE
//////                    btnPause.visibility = View.VISIBLE
//////                }
//////            }
//////        }
//////    }
//////
//////    private fun updateButtonsVisibility(isRunning: Boolean) {
//////        if (isRunning) {
//////            // 타이머 실행 중
//////            btn10Min.visibility = View.GONE
//////            btn20Min.visibility = View.GONE
//////            btn30Min.visibility = View.GONE
//////            btnStart.visibility = View.GONE
//////            btnPause.visibility = View.VISIBLE
//////            btnStop.visibility = View.VISIBLE
//////            btnReset.visibility = View.VISIBLE
//////        } else {
//////            // 타이머 대기 중
//////            btn10Min.visibility = View.VISIBLE
//////            btn20Min.visibility = View.VISIBLE
//////            btn30Min.visibility = View.VISIBLE
//////            btnStart.visibility = View.GONE
//////            btnPause.visibility = View.GONE
//////            btnStop.visibility = View.GONE
//////            btnReset.visibility = View.GONE
//////        }
//////    }
//////
//////    private fun startTimerHandler() {
//////        handler.post(timerRunnable)
//////    }
//////
//////    private fun stopTimerHandler() {
//////        handler.removeCallbacks(timerRunnable)
//////    }
//////
//////    override fun onDestroyView() {
//////        super.onDestroyView()
//////        stopTimerHandler()
//////    }
//////}