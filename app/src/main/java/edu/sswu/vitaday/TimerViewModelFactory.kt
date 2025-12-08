package edu.sswu.vitaday.ui.timer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * TimerViewModel Factory
 * Application 컨텍스트를 전달하기 위해 필요
 */
class TimerViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            return TimerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
//package edu.sswu.vitaday.ui.timer
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import edu.sswu.vitaday.TimerSessionDao
//
//class TimerViewModelFactory(private val dao: TimerSessionDao) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return TimerViewModel(dao) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}