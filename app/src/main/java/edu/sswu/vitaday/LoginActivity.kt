package edu.sswu.vitaday

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var db: UserDatabase
    private lateinit var userDao: UserDao
    private lateinit var btnGoogleLogin: Button
    private lateinit var progressBar: ProgressBar

    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSignInResult(result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 데이터베이스 초기화
        db = UserDatabase.getDatabase(applicationContext) // Context 안전하게 변경
        userDao = db.userDao()

        // View 초기화 (아까 XML에 만든 progress_bar와 연결됨)
        btnGoogleLogin = findViewById(R.id.btn_google_login)
        progressBar = findViewById(R.id.progress_bar)

        // 로그인 버튼 클릭 (수동 로그인)
        btnGoogleLogin.setOnClickListener {
            startSignInFlow()
        }

        // 앱 켜자마자 로그인 상태 확인 (자동 로그인)
        checkCurrentUser()
    }

    /**
     * [수정됨] 현재 로그인된 사용자 확인
     * - 기존: 바로 메인으로 이동 (DB가 비어있으면 오류 위험)
     * - 수정: saveUserToDatabase를 거쳐서 DB를 채워넣고 메인으로 이동 (안전!)
     */
    private fun checkCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: ${currentUser.email}")

            // 로딩 바를 보여줘서 사용자가 기다리게 함
            showLoading(true)

            // 바로 이동하지 않고, DB에 저장(동기화) 확인 후 이동
            saveUserToDatabase(
                currentUser.email ?: "unknown@example.com",
                currentUser.displayName ?: "익명"
            )
        }
    }

    /**
     * Google 로그인 창 띄우기
     */
    private fun startSignInFlow() {
        showLoading(true)

        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .setTheme(R.style.Theme_VitaDay)
            .build()

        signInLauncher.launch(signInIntent)
    }

    /**
     * 로그인 결과 처리 (수동 로그인 결과)
     */
    private fun handleSignInResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            // 로그인 성공
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d(TAG, "Sign in successful: ${user.email}")
                // 수동 로그인 때도 DB 저장 과정을 거침
                saveUserToDatabase(user.email ?: "unknown@example.com", user.displayName ?: "익명")
            } else {
                showLoading(false)
                showError("로그인 정보를 가져올 수 없습니다")
            }
        } else {
            // 로그인 실패 또는 취소
            showLoading(false)
            handleSignInError(data)
        }
    }

    /**
     * 사용자 정보를 데이터베이스에 저장 (동기화 핵심 함수)
     * - 신규 유저면 Insert
     * - 기존 유저면 Pass
     * - 끝나면 메인 화면으로 이동
     */
    private fun saveUserToDatabase(email: String, nickname: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 기존 사용자 확인
                // 주의: password는 예시로 "firebase"로 고정함
                val existingUser = userDao.login(email, "firebase")

                if (existingUser == null) {
                    // 신규 사용자(또는 재설치 유저) 등록
                    userDao.insertUser(
                        User(
                            email = email,
                            password = "firebase",
                            nickname = nickname,
                            gender = null,
                            ageRange = null,
                            statusMessage = null,
                            category = null
                        )
                    )
                    Log.d(TAG, "New user registered (Synced): $email")
                } else {
                    Log.d(TAG, "Existing user checked: $email")
                }

                // DB 작업 끝! 메인 화면으로 이동
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    // 자동 로그인일 때는 토스트 메시지가 살짝 뜰 수 있음 (정상)
                    showSuccess("로그인 성공!")
                    navigateToMain()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database error", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("데이터 저장 중 오류가 발생했습니다")
                }
            }
        }
    }

    /**
     * 로그인 에러 메시지 처리
     */
    private fun handleSignInError(data: Intent?) {
        val response = IdpResponse.fromResultIntent(data)

        if (response == null) {
            Log.d(TAG, "Sign in cancelled by user")
            showInfo("로그인이 취소되었습니다")
            return
        }

        val errorMessage = when (response.error?.errorCode) {
            ErrorCodes.NO_NETWORK -> "인터넷 연결을 확인해주세요"
            ErrorCodes.UNKNOWN_ERROR -> "알 수 없는 오류가 발생했습니다\n잠시 후 다시 시도해주세요"
            ErrorCodes.ERROR_USER_DISABLED -> "비활성화된 계정입니다"
            else -> "로그인 실패: ${response.error?.message ?: "알 수 없는 오류"}"
        }

        Log.e(TAG, "Sign in error: ${response.error?.message}", response.error)
        showError(errorMessage)
    }

    /**
     * 메인 화면으로 이동
     */
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // 뒤로가기 눌러도 로그인 화면으로 안 돌아오게 종료
    }

    private fun showLoading(show: Boolean) {
        btnGoogleLogin.isEnabled = !show
        // XML에 추가했던 progress_bar가 여기서 쓰입니다!
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showInfo(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
//package edu.sswu.vitaday
//
//import android.app.Activity
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import com.firebase.ui.auth.AuthUI
//import com.firebase.ui.auth.IdpResponse
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//class LoginActivity : AppCompatActivity() {
//
//    private lateinit var db: UserDatabase
//    private lateinit var userDao: UserDao
//
//    private val signInLauncher: ActivityResultLauncher<Intent> =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val user = FirebaseAuth.getInstance().currentUser
//                if (user != null) {
//                    val email = user.email ?: "unknown@example.com"
//
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        val dao = userDao
//                        val existingUser = dao.login(email, "firebase")
//
//                        if (existingUser == null) {
//                            dao.insertUser(
//                                User(
//                                    email = email,
//                                    password = "firebase",
//                                    nickname = user.displayName ?: "익명",
//                                    gender = null,
//                                    ageRange = null,
//                                    statusMessage = null,
//                                    category = null
//                                )
//                            )
//                        }
//                    }
//                }
//
//                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
//                startActivity(Intent(this, MainActivity::class.java))
//                finish()
//            } else {
//                val response = IdpResponse.fromResultIntent(result.data)
//                Toast.makeText(
//                    this,
//                    "로그인 실패: ${response?.error?.message ?: "취소됨"}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_login)
//
//        db = UserDatabase.getDatabase(this)
//        userDao = db.userDao()
//
//        // 버튼 클릭 리스너 추가
//        val btnGoogleLogin = findViewById<Button>(R.id.btn_google_login)
//        btnGoogleLogin.setOnClickListener {
//            startSignInFlow()
//        }
//    }
//
//    private fun startSignInFlow() {
//        val providers = arrayListOf(
//            AuthUI.IdpConfig.GoogleBuilder().build()
//        )
//
//        val signInIntent = AuthUI.getInstance()
//            .createSignInIntentBuilder()
//            .setAvailableProviders(providers)
//            .setIsSmartLockEnabled(false)
//            .build()
//
//        signInLauncher.launch(signInIntent)
//    }
//}