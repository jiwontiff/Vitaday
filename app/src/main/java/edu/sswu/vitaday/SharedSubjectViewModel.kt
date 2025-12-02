package edu.sswu.vitaday

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 홈과 투두창 간 과목 데이터를 공유하는 ViewModel
 * Activity-scoped로 사용
 */
class SharedSubjectViewModel : ViewModel() {

    // 과목 리스트 (전역 공유)
    private val _subjects = MutableStateFlow<List<SubjectData>>(
        listOf(
            SubjectData(1, "모바일 프로그래밍", "#FF5252"),
            SubjectData(2, "자연어 처리", "#7C4DFF"),
            SubjectData(3, "인공지능 수학", "#00BCD4")
        )
    )
    val subjects: StateFlow<List<SubjectData>> = _subjects.asStateFlow()

    /**
     * 과목 추가
     */
    fun addSubject(name: String, colorHex: String) {
        val newId = (_subjects.value.maxOfOrNull { it.id } ?: 0) + 1
        val newSubject = SubjectData(
            id = newId,
            name = name,
            colorHex = colorHex
        )
        _subjects.value = _subjects.value + newSubject
    }

    /**
     * 과목 삭제
     */
    fun removeSubject(subjectId: Int) {
        _subjects.value = _subjects.value.filter { it.id != subjectId }
    }

    /**
     * 과목 수정
     */
    fun updateSubject(subjectId: Int, name: String, colorHex: String) {
        _subjects.value = _subjects.value.map { subject ->
            if (subject.id == subjectId) {
                subject.copy(name = name, colorHex = colorHex)
            } else {
                subject
            }
        }
    }

    /**
     * 과목 순서 변경 (드래그 앤 드롭)
     */
    fun reorderSubjects(newOrder: List<SubjectData>) {
        _subjects.value = newOrder
    }

    /**
     * ID로 과목 찾기
     */
    fun getSubjectById(id: Int): SubjectData? {
        return _subjects.value.find { it.id == id }
    }
}
//package edu.sswu.vitaday
//
//import androidx.lifecycle.ViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
///**
// * 홈과 투두창 간 과목 데이터를 공유하는 ViewModel
// * Activity-scoped로 사용
// */
//class SharedSubjectViewModel : ViewModel() {
//
//    // 과목 리스트 (전역 공유)
//    private val _subjects = MutableStateFlow<List<SubjectData>>(
//        listOf(
//            SubjectData(1, "모바일 프로그래밍", "#FF5252"),
//            SubjectData(2, "자연어 처리", "#7C4DFF"), // 보라색
//            SubjectData(3, "인공지능 수학", "#00BCD4")
//        )
//    )
//    val subjects: StateFlow<List<SubjectData>> = _subjects.asStateFlow()
//
//    /**
//     * 과목 추가
//     */
//    fun addSubject(name: String, colorHex: String) {
//        val newId = (_subjects.value.maxOfOrNull { it.id } ?: 0) + 1
//        val newSubject = SubjectData(
//            id = newId,
//            name = name,
//            colorHex = colorHex
//        )
//        _subjects.value = _subjects.value + newSubject
//    }
//
//    /**
//     * 과목 삭제
//     */
//    fun removeSubject(subjectId: Int) {
//        _subjects.value = _subjects.value.filter { it.id != subjectId }
//    }
//
//    /**
//     * 과목 수정
//     */
//    fun updateSubject(subjectId: Int, name: String, colorHex: String) {
//        _subjects.value = _subjects.value.map { subject ->
//            if (subject.id == subjectId) {
//                subject.copy(name = name, colorHex = colorHex)
//            } else {
//                subject
//            }
//        }
//    }
//
//    /**
//     * ID로 과목 찾기
//     */
//    fun getSubjectById(id: Int): SubjectData? {
//        return _subjects.value.find { it.id == id }
//    }
//}