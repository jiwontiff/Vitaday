package edu.sswu.vitaday

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import edu.sswu.vitaday.ui.timer.TimerViewModel
import edu.sswu.vitaday.ui.timer.TimerViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: UserDao

    // TimerViewModel (Application 기반 Factory 적용)
    private val timerViewModel: TimerViewModel by activityViewModels {
        TimerViewModelFactory(requireActivity().application)
    }

    // StatisticsViewModel (Repository 기반 Factory 적용)
    private val statisticsViewModel: StatisticsViewModel by activityViewModels {
        val database = UserDatabase.getDatabase(requireContext())
        val repository = StatisticsRepository(
            database.timerSessionDao(),
            database.subjectDao()
        )
        StatisticsViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        userDao = UserDatabase.getDatabase(requireContext()).userDao()

        // 로그아웃 버튼
        view.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // 회원탈퇴 버튼
        view.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // 카카오톡 공유 버튼
        view.findViewById<Button>(R.id.btnShareKakao)?.setOnClickListener {
            shareToKakao()
        }
    }

    // 회원탈퇴 확인 다이얼로그
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원탈퇴")
            .setMessage("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("탈퇴") { _, _ -> deleteAccount() }
            .setNegativeButton("취소", null)
            .show()
    }

    // 회원탈퇴 처리
    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return

        currentUser.delete()
            .addOnSuccessListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        userDao.deleteUser(currentUser.uid.hashCode())

                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
                            requireActivity().finish()
                        }
                    } catch (e: Exception) {
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "회원탈퇴 중 오류 발생", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "회원탈퇴 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 카카오톡 공유 기능
    private fun shareToKakao() {
        val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
        val today = dateFormat.format(Date())

        // 오늘 공부 시간
        val todayTime = timerViewModel.getTotalTimeForDate(Date())
        val todayHours = todayTime / (1000 * 60 * 60)
        val todayMinutes = (todayTime / (1000 * 60)) % 60

        // 전체 통계
        val totalTime = statisticsViewModel.totalDuration.value
        val totalHours = totalTime / (1000 * 60 * 60)
        val totalMinutes = (totalTime / (1000 * 60)) % 60
        val totalCount = statisticsViewModel.totalSessionCount.value

        val message = """
            📚 VitaDay 공부 기록 📚
            
            📅 $today
            ⏰ 오늘: ${todayHours}시간 ${todayMinutes}분
            
            📊 전체 통계
            🔥 총 집중 횟수: ${totalCount}회
            ⏱️ 합계: ${totalHours}시간 ${totalMinutes}분
            
            #VitaDay #공부기록 #뽀모도로 #타이머
        """.trimIndent()

        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.kakao.talk")
            }
            startActivity(shareIntent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            startActivity(Intent.createChooser(shareIntent, "공유하기"))
        }
    }
}

//package edu.sswu.vitaday
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.lifecycleScope
//import com.firebase.ui.auth.AuthUI
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.ktx.Firebase
//import com.google.firebase.auth.ktx.auth
//import edu.sswu.vitaday.ui.timer.TimerViewModel
//import edu.sswu.vitaday.ui.timer.TimerViewModelFactory
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class SettingsFragment : Fragment() {
//
//    private lateinit var auth: FirebaseAuth
//    private lateinit var userDao: UserDao
//
//    // ⭐ TimerViewModel (Application 기반)
//    private val timerViewModel: TimerViewModel by activityViewModels {
//        TimerViewModelFactory(requireActivity().application)
//    }
//
//    // ⭐ StatisticsViewModel
//    private val statisticsViewModel: StatisticsViewModel by activityViewModels {
//        val database = UserDatabase.getDatabase(requireContext())
//        val repository = StatisticsRepository(
//            database.timerSessionDao(),
//            database.subjectDao()
//        )
//        StatisticsViewModelFactory(repository)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_settings, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        auth = Firebase.auth
//        userDao = UserDatabase.getDatabase(requireContext()).userDao()
//
//        // 로그아웃 버튼
//        view.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
//            AuthUI.getInstance()
//                .signOut(requireContext())
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        startActivity(Intent(requireContext(), LoginActivity::class.java))
//                        requireActivity().finish()
//                    } else {
//                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
//                    }
//                }
//        }
//
//        // 회원탈퇴 버튼
//        view.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
//            showDeleteConfirmDialog()
//        }
//
//        // 카카오톡 공유
//        view.findViewById<Button>(R.id.btnShareKakao)?.setOnClickListener {
//            shareToKakao()
//        }
//    }
//
//    /**
//     * 회원탈퇴 확인 다이얼로그
//     */
//    private fun showDeleteConfirmDialog() {
//        AlertDialog.Builder(requireContext())
//            .setTitle("회원탈퇴")
//            .setMessage("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
//            .setPositiveButton("탈퇴") { _, _ -> deleteAccount() }
//            .setNegativeButton("취소", null)
//            .show()
//    }
//
//    /**
//     * 회원탈퇴 처리
//     */
//    private fun deleteAccount() {
//        val currentUser = auth.currentUser ?: return
//
//        currentUser.delete()
//            .addOnSuccessListener {
//                lifecycleScope.launch(Dispatchers.IO) {
//                    try {
//                        userDao.deleteUser(currentUser.uid.hashCode())
//
//                        lifecycleScope.launch {
//                            Toast.makeText(requireContext(), "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
//                            startActivity(Intent(requireContext(), LoginActivity::class.java))
//                            requireActivity().finish()
//                        }
//                    } catch (e: Exception) {
//                        lifecycleScope.launch {
//                            Toast.makeText(requireContext(), "회원탈퇴 중 오류 발생", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "회원탈퇴 실패: ${it.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    /**
//     * 카카오톡 공유 기능
//     */
//    private fun shareToKakao() {
//
//        val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
//        val today = dateFormat.format(Date())
//
//        // 오늘의 공부 시간 (millis)
//        val todayTime = timerViewModel.getTotalTimeForDate(Date())
//        val todayHours = todayTime / (1000 * 60 * 60)
//        val todayMinutes = (todayTime / (1000 * 60)) % 60
//
//        // 전체 통계
//        val totalTime = statisticsViewModel.totalDuration.value
//        val totalHours = totalTime / (1000 * 60 * 60)
//        val totalMinutes = (totalTime / (1000 * 60)) % 60
//        val totalCount = statisticsViewModel.totalSessionCount.value
//
//        val message = """
//            📚 VitaDay 공부 기록 📚
//
//            📅 $today
//            ⏰ 오늘: ${todayHours}시간 ${todayMinutes}분
//
//            📊 전체 통계
//            🔥 총 집중 횟수: ${totalCount}회
//            ⏱️ 합계: ${totalHours}시간 ${totalMinutes}분
//
//            #VitaDay #공부기록 #뽀모도로 #타이머
//        """.trimIndent()
//
//        try {
//            val shareIntent = Intent(Intent.ACTION_SEND).apply {
//                type = "text/plain"
//                putExtra(Intent.EXTRA_TEXT, message)
//                setPackage("com.kakao.talk") // 카카오톡 우선
//            }
//            startActivity(shareIntent)
//        } catch (e: Exception) {
//            // 카카오톡이 없으면 일반 공유
//            val shareIntent = Intent(Intent.ACTION_SEND).apply {
//                type = "text/plain"
//                putExtra(Intent.EXTRA_TEXT, message)
//            }
//            startActivity(Intent.createChooser(shareIntent, "공유하기"))
//        }
//    }
//}
//
////package edu.sswu.vitaday
////
////import android.content.Intent
////import android.os.Bundle
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.Button
////import android.widget.Toast
////import androidx.appcompat.app.AlertDialog
////import androidx.fragment.app.Fragment
////import androidx.fragment.app.activityViewModels
////import androidx.lifecycle.lifecycleScope
////import com.firebase.ui.auth.AuthUI
////import com.google.firebase.auth.FirebaseAuth
////import com.google.firebase.ktx.Firebase
////import com.google.firebase.auth.ktx.auth
////import edu.sswu.vitaday.ui.timer.TimerViewModel
////import kotlinx.coroutines.Dispatchers
////import kotlinx.coroutines.launch
////import java.text.SimpleDateFormat
////import java.util.Date
////import java.util.Locale
////
////class SettingsFragment : Fragment() {
////
////    private lateinit var auth: FirebaseAuth
////    private lateinit var userDao: UserDao
////
////    // ⭐ ViewModel 추가 (통계 데이터 가져오기용)
////    private val timerViewModel: TimerViewModel by activityViewModels()
////    private val statisticsViewModel: StatisticsViewModel by activityViewModels {
////        val database = UserDatabase.getDatabase(requireContext())
////        val repository = StatisticsRepository(
////            database.timerSessionDao(),
////            database.subjectDao()
////        )
////        StatisticsViewModelFactory(repository)
////    }
////
////    override fun onCreateView(
////        inflater: LayoutInflater,
////        container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        return inflater.inflate(R.layout.fragment_settings, container, false)
////    }
////
////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////        super.onViewCreated(view, savedInstanceState)
////
////        auth = Firebase.auth
////        userDao = UserDatabase.getDatabase(requireContext()).userDao()
////
////        // 로그아웃 버튼
////        view.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
////            AuthUI.getInstance()
////                .signOut(requireContext())
////                .addOnCompleteListener { task ->
////                    if (task.isSuccessful) {
////                        val intent = Intent(requireContext(), LoginActivity::class.java)
////                        startActivity(intent)
////                        requireActivity().finish()
////                    } else {
////                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
////                    }
////                }
////        }
////
////        // 회원탈퇴 버튼
////        view.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
////            showDeleteConfirmDialog()
////        }
////
////        // ⭐ 카카오톡 공유 버튼
////        view.findViewById<Button>(R.id.btnShareKakao)?.setOnClickListener {
////            shareToKakao()
////        }
////    }
////
////    /**
////     * 회원탈퇴 확인 다이얼로그
////     */
////    private fun showDeleteConfirmDialog() {
////        AlertDialog.Builder(requireContext())
////            .setTitle("회원탈퇴")
////            .setMessage("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
////            .setPositiveButton("탈퇴") { _, _ ->
////                deleteAccount()
////            }
////            .setNegativeButton("취소", null)
////            .show()
////    }
////
////    /**
////     * 회원탈퇴 처리
////     */
////    private fun deleteAccount() {
////        val currentUser = auth.currentUser ?: return
////
////        currentUser.delete()
////            .addOnSuccessListener {
////                lifecycleScope.launch(Dispatchers.IO) {
////                    try {
////                        userDao.deleteUser(currentUser.uid.hashCode())
////
////                        lifecycleScope.launch {
////                            Toast.makeText(requireContext(), "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
////                            val intent = Intent(requireContext(), LoginActivity::class.java)
////                            startActivity(intent)
////                            requireActivity().finish()
////                        }
////                    } catch (e: Exception) {
////                        lifecycleScope.launch {
////                            Toast.makeText(requireContext(), "회원탈퇴 중 오류 발생", Toast.LENGTH_SHORT).show()
////                        }
////                    }
////                }
////            }
////            .addOnFailureListener { exception ->
////                Toast.makeText(requireContext(), "회원탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
////            }
////    }
////
////    /**
////     * ⭐ 카카오톡으로 공유하기
////     */
////    private fun shareToKakao() {
////        // 오늘 날짜
////        val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
////        val today = dateFormat.format(Date())
////
////        // 오늘의 공부 시간
////        val todayTime = timerViewModel.getTotalTimeForDate(Date())
////        val todayHours = todayTime / (1000 * 60 * 60)
////        val todayMinutes = (todayTime / (1000 * 60)) % 60
////
////        // 총 공부 시간 (통계에서 가져오기)
////        val totalTime = statisticsViewModel.totalDuration.value
////        val totalHours = totalTime / (1000 * 60 * 60)
////        val totalMinutes = (totalTime / (1000 * 60)) % 60
////
////        // 총 집중 횟수
////        val totalCount = statisticsViewModel.totalSessionCount.value
////
////        // 공유할 메시지 생성
////        val message = """
////            📚 VitaDay 공부 기록 📚
////
////            📅 $today
////            ⏰ 오늘: ${todayHours}시간 ${todayMinutes}분
////
////            📊 전체 통계
////            🔥 총 집중 횟수: ${totalCount}회
////            ⏱️ 합계: ${totalHours}시간 ${totalMinutes}분
////
////            #VitaDay #공부기록 #뽀모도로 #타이머
////        """.trimIndent()
////
////        // Intent를 사용한 공유 (카카오톡 앱이 설치되어 있으면 카카오톡으로, 없으면 다른 앱으로)
////        try {
////            val shareIntent = Intent(Intent.ACTION_SEND).apply {
////                type = "text/plain"
////                putExtra(Intent.EXTRA_TEXT, message)
////
////                // 카카오톡 패키지명 지정 (카카오톡 우선)
////                setPackage("com.kakao.talk")
////            }
////
////            startActivity(shareIntent)
////        } catch (e: Exception) {
////            // 카카오톡이 없는 경우 일반 공유
////            val shareIntent = Intent(Intent.ACTION_SEND).apply {
////                type = "text/plain"
////                putExtra(Intent.EXTRA_TEXT, message)
////            }
////
////            startActivity(Intent.createChooser(shareIntent, "공유하기"))
////        }
////    }
////}
//////package edu.sswu.vitaday
//////
//////import android.content.Intent
//////import android.os.Bundle
//////import android.view.LayoutInflater
//////import android.view.View
//////import android.view.ViewGroup
//////import android.widget.Button
//////import android.widget.Toast
//////import androidx.appcompat.app.AlertDialog
//////import androidx.fragment.app.Fragment
//////import androidx.fragment.app.activityViewModels
//////import androidx.lifecycle.lifecycleScope
//////import com.firebase.ui.auth.AuthUI
//////import com.google.firebase.auth.FirebaseAuth
//////import com.google.firebase.ktx.Firebase
//////import com.google.firebase.auth.ktx.auth
//////import edu.sswu.vitaday.ui.timer.TimerViewModel
//////import edu.sswu.vitaday.ui.timer.TimerViewModelFactory // ⭐ Factory 임포트
//////import kotlinx.coroutines.Dispatchers
//////import kotlinx.coroutines.launch
//////import java.text.SimpleDateFormat
//////import java.util.Date
//////import java.util.Locale
//////
//////class SettingsFragment : Fragment() {
//////
//////    private lateinit var auth: FirebaseAuth
//////    private lateinit var userDao: UserDao
//////
//////    // ⭐ [수정됨] TimerViewModel 초기화 (Factory 사용)
//////    private val timerViewModel: TimerViewModel by activityViewModels {
//////        TimerViewModelFactory(UserDatabase.getDatabase(requireContext()).timerSessionDao())
//////    }
//////
//////    // ⭐ StatisticsViewModel 초기화
//////    private val statisticsViewModel: StatisticsViewModel by activityViewModels {
//////        val database = UserDatabase.getDatabase(requireContext())
//////        val repository = StatisticsRepository(
//////            database.timerSessionDao(),
//////            database.subjectDao()
//////        )
//////        StatisticsViewModelFactory(repository)
//////    }
//////
//////    override fun onCreateView(
//////        inflater: LayoutInflater,
//////        container: ViewGroup?,
//////        savedInstanceState: Bundle?
//////    ): View? {
//////        return inflater.inflate(R.layout.fragment_settings, container, false)
//////    }
//////
//////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//////        super.onViewCreated(view, savedInstanceState)
//////
//////        auth = Firebase.auth
//////        userDao = UserDatabase.getDatabase(requireContext()).userDao()
//////
//////        // 로그아웃 버튼
//////        view.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
//////            AuthUI.getInstance()
//////                .signOut(requireContext())
//////                .addOnCompleteListener { task ->
//////                    if (task.isSuccessful) {
//////                        val intent = Intent(requireContext(), LoginActivity::class.java)
//////                        startActivity(intent)
//////                        requireActivity().finish()
//////                    } else {
//////                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
//////                    }
//////                }
//////        }
//////
//////        // 회원탈퇴 버튼
//////        view.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
//////            showDeleteConfirmDialog()
//////        }
//////
//////        // ⭐ 카카오톡 공유 버튼
//////        view.findViewById<Button>(R.id.btnShareKakao)?.setOnClickListener {
//////            shareToKakao()
//////        }
//////    }
//////
//////    /**
//////     * 회원탈퇴 확인 다이얼로그
//////     */
//////    private fun showDeleteConfirmDialog() {
//////        AlertDialog.Builder(requireContext())
//////            .setTitle("회원탈퇴")
//////            .setMessage("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
//////            .setPositiveButton("탈퇴") { _, _ ->
//////                deleteAccount()
//////            }
//////            .setNegativeButton("취소", null)
//////            .show()
//////    }
//////
//////    /**
//////     * 회원탈퇴 처리
//////     */
//////    private fun deleteAccount() {
//////        val currentUser = auth.currentUser ?: return
//////
//////        // Firebase에서 사용자 삭제
//////        currentUser.delete()
//////            .addOnSuccessListener {
//////                lifecycleScope.launch(Dispatchers.IO) {
//////                    try {
//////                        // DB에서 사용자 정보 삭제
//////                        userDao.deleteUser(currentUser.uid.hashCode())
//////
//////                        lifecycleScope.launch {
//////                            Toast.makeText(requireContext(), "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
//////                            val intent = Intent(requireContext(), LoginActivity::class.java)
//////                            startActivity(intent)
//////                            requireActivity().finish()
//////                        }
//////                    } catch (e: Exception) {
//////                        lifecycleScope.launch {
//////                            Toast.makeText(requireContext(), "회원탈퇴 중 오류 발생", Toast.LENGTH_SHORT).show()
//////                        }
//////                    }
//////                }
//////            }
//////            .addOnFailureListener { exception ->
//////                Toast.makeText(requireContext(), "회원탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
//////            }
//////    }
//////
//////    /**
//////     * ⭐ 카카오톡으로 공유하기
//////     */
//////    private fun shareToKakao() {
//////        val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
//////        val today = dateFormat.format(Date())
//////
//////        // 1. 통계 데이터 최신화
//////        statisticsViewModel.loadAllStatistics()
//////
//////        // 2. 값 가져오기
//////        // 주의: StateFlow의 value는 즉시 가져오지만, loadAllStatistics가 비동기라
//////        // 완벽한 실시간이 아닐 수 있습니다. 일반적으로는 화면 진입 시 로드되므로 큰 문제는 없습니다.
//////        val todayTime = statisticsViewModel.todayDuration.value
//////        val todayHours = todayTime / (1000 * 60 * 60)
//////        val todayMinutes = (todayTime / (1000 * 60)) % 60
//////
//////        val totalTime = statisticsViewModel.totalDuration.value
//////        val totalHours = totalTime / (1000 * 60 * 60)
//////        val totalMinutes = (totalTime / (1000 * 60)) % 60
//////
//////        val totalCount = statisticsViewModel.totalSessionCount.value
//////
//////        val message = """
//////            📚 VitaDay 공부 기록 📚
//////
//////            📅 $today
//////            ⏰ 오늘: ${todayHours}시간 ${todayMinutes}분
//////
//////            📊 전체 통계
//////            🔥 총 집중 횟수: ${totalCount}회
//////            ⏱️ 합계: ${totalHours}시간 ${totalMinutes}분
//////
//////            #VitaDay #공부기록 #뽀모도로 #타이머
//////        """.trimIndent()
//////
//////        try {
//////            val shareIntent = Intent(Intent.ACTION_SEND).apply {
//////                type = "text/plain"
//////                putExtra(Intent.EXTRA_TEXT, message)
//////                setPackage("com.kakao.talk") // 카카오톡 패키지 지정
//////            }
//////            startActivity(shareIntent)
//////        } catch (e: Exception) {
//////            // 카카오톡이 없으면 일반 공유 팝업 띄우기
//////            val shareIntent = Intent(Intent.ACTION_SEND).apply {
//////                type = "text/plain"
//////                putExtra(Intent.EXTRA_TEXT, message)
//////            }
//////            startActivity(Intent.createChooser(shareIntent, "공유하기"))
//////        }
//////    }
//////}
////////package edu.sswu.vitaday
////////
////////import android.content.Intent
////////import android.os.Bundle
////////import android.view.LayoutInflater
////////import android.view.View
////////import android.view.ViewGroup
////////import android.widget.Button
////////import android.widget.Toast
////////import androidx.appcompat.app.AlertDialog
////////import androidx.fragment.app.Fragment
////////import androidx.lifecycle.lifecycleScope
////////import com.firebase.ui.auth.AuthUI
////////import com.google.firebase.auth.FirebaseAuth
////////import com.google.firebase.ktx.Firebase
////////import com.google.firebase.auth.ktx.auth
////////import kotlinx.coroutines.Dispatchers
////////import kotlinx.coroutines.launch
////////
////////class SettingsFragment : Fragment() {
////////
////////    private lateinit var auth: FirebaseAuth
////////    private lateinit var userDao: UserDao
////////
////////    override fun onCreateView(
////////        inflater: LayoutInflater,
////////        container: ViewGroup?,
////////        savedInstanceState: Bundle?
////////    ): View? {
////////        return inflater.inflate(R.layout.fragment_settings, container, false)
////////    }
////////
////////    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
////////        super.onViewCreated(view, savedInstanceState)
////////
////////        auth = Firebase.auth
////////        userDao = UserDatabase.getDatabase(requireContext()).userDao()
////////
////////        // 로그아웃 버튼
////////        view.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
////////            AuthUI.getInstance()
////////                .signOut(requireContext())
////////                .addOnCompleteListener { task ->
////////                    if (task.isSuccessful) {
////////                        val intent = Intent(requireContext(), LoginActivity::class.java)
////////                        startActivity(intent)
////////                        requireActivity().finish()
////////                    } else {
////////                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
////////                    }
////////                }
////////        }
////////
////////        // ✅ 회원탈퇴 버튼
////////        view.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
////////            showDeleteConfirmDialog()
////////        }
////////    }
////////
////////    /**
////////     * 회원탈퇴 확인 다이얼로그
////////     */
////////    private fun showDeleteConfirmDialog() {
////////        AlertDialog.Builder(requireContext())
////////            .setTitle("회원탈퇴")
////////            .setMessage("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
////////            .setPositiveButton("탈퇴") { _, _ ->
////////                deleteAccount()
////////            }
////////            .setNegativeButton("취소", null)
////////            .show()
////////    }
////////
////////    /**
////////     * 회원탈퇴 처리
////////     */
////////    private fun deleteAccount() {
////////        val currentUser = auth.currentUser ?: return
////////
////////        // Firebase에서 사용자 삭제
////////        currentUser.delete()
////////            .addOnSuccessListener {
////////                // ✅ Firebase 삭제 성공 → Room DB에서도 삭제
////////                lifecycleScope.launch(Dispatchers.IO) {
////////                    try {
////////                        // DB에서 사용자 정보 삭제
////////                        userDao.deleteUser(currentUser.uid.hashCode())
////////
////////                        // UI 스레드에서 로그인 화면으로 이동
////////                        lifecycleScope.launch {
////////                            Toast.makeText(requireContext(), "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
////////                            val intent = Intent(requireContext(), LoginActivity::class.java)
////////                            startActivity(intent)
////////                            requireActivity().finish()
////////                        }
////////                    } catch (e: Exception) {
////////                        lifecycleScope.launch {
////////                            Toast.makeText(requireContext(), "회원탈퇴 중 오류 발생", Toast.LENGTH_SHORT).show()
////////                        }
////////                    }
////////                }
////////            }
////////            .addOnFailureListener { exception ->
////////                Toast.makeText(requireContext(), "회원탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
////////            }
////////    }
////////}