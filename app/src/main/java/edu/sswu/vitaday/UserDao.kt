package edu.sswu.vitaday

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {

    // 회원가입 - 새 사용자 추가
    @Insert
    fun insertUser(user: User)

    // 로그인 - 이메일과 비밀번호로 사용자 찾기
    @Query("SELECT * FROM User WHERE email = :email AND password = :password")
    fun login(email: String, password: String): User?

    // 닉네임 수정
    @Query("UPDATE User SET nickname = :nickname WHERE userId = :userId")
    fun updateNickname(userId: Int, nickname: String)

    // 상태메시지 수정
    @Query("UPDATE User SET statusMessage = :status WHERE userId = :userId")
    fun updateStatusMessage(userId: Int, status: String)

    // 카테고리 수정
    @Query("UPDATE User SET category = :category WHERE userId = :userId")
    fun updateCategory(userId: Int, category: String)

    // 회원탈퇴
    @Query("DELETE FROM User WHERE userId = :userId")
    fun deleteUser(userId: Int)
}
