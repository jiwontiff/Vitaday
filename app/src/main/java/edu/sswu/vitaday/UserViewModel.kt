package edu.sswu.vitaday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // 회원가입
    fun insertUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUser(user)
        }
    }

    // 로그인
    fun login(email: String, password: String, callback: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.login(email, password)
            callback(user)
        }
    }
}
