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
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: UserDao

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
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // ✅ 회원탈퇴 버튼
        view.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    /**
     * 회원탈퇴 확인 다이얼로그
     */
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원탈퇴")
            .setMessage("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("탈퇴") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * 회원탈퇴 처리
     */
    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return

        // Firebase에서 사용자 삭제
        currentUser.delete()
            .addOnSuccessListener {
                // ✅ Firebase 삭제 성공 → Room DB에서도 삭제
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // DB에서 사용자 정보 삭제
                        userDao.deleteUser(currentUser.uid.hashCode())

                        // UI 스레드에서 로그인 화면으로 이동
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    } catch (e: Exception) {
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "회원탈퇴 중 오류 발생", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "회원탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}