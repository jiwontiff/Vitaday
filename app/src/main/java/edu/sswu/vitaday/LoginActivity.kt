package edu.sswu.vitaday

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var db: UserDatabase
    private lateinit var userDao: UserDao

    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val email = user.email ?: "unknown@example.com"

                    lifecycleScope.launch(Dispatchers.IO) {
                        val dao = userDao
                        val existingUser = dao.login(email, "firebase")

                        if (existingUser == null) {
                            dao.insertUser(
                                User(
                                    email = email,
                                    password = "firebase",
                                    nickname = user.displayName ?: "익명",
                                    gender = null,
                                    ageRange = null,
                                    statusMessage = null,
                                    category = null
                                )
                            )
                        }
                    }
                }

                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val response = IdpResponse.fromResultIntent(result.data)
                Toast.makeText(
                    this,
                    "로그인 실패: ${response?.error?.message ?: "취소됨"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = UserDatabase.getDatabase(this)
        userDao = db.userDao()

        // 버튼 클릭 리스너 추가
        val btnGoogleLogin = findViewById<Button>(R.id.btn_google_login)
        btnGoogleLogin.setOnClickListener {
            startSignInFlow()
        }
    }

    private fun startSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        signInLauncher.launch(signInIntent)
    }
}