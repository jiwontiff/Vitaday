package edu.sswu.vitaday

class UserRepository(private val userDao: UserDao) {

    // 회원가입
    fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    // 로그인
    fun login(email: String, password: String): User? {
        return userDao.login(email, password)
    }
}
