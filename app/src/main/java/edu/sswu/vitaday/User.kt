package edu.sswu.vitaday

// import 구문 : 코틀린에서 외부 라이브러리 기능을 불러오는 문법 , 여기서는 room 라이브러리 안에 있는 @Entity와 @PrimaryKey 기능을 가져옴
import androidx.room.Entity  // Room에서 테이블로 인식시키는 어노테이션 // 어노테이션(Annotation) : 코틀린에서 '이 코드가 어떤 특별한 역할을 한다'는 메타정보를 컴퓨터에게 알려주는 표시
import androidx.room.PrimaryKey  // 기본키(Primary Key) 지정에 필요

// @Entity : 이 클래스를 'DB 테이블'로 지정함 -> 실제로 SQLite 내부에서는 User라는 테이블이 만들어짐
@Entity(tableName = "User") //Users로 바꾸기
// 데이터를 담는 전용 클래스
data class User(
    // PrimaryKey : 테이블에서 각 데이터를 구분할 유일한 키 (각 회원을 구분할 ID)
    // autoGenerate = true -> true면 room이 자동으로 1,2,3,.. 번호를 붙여줌
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,

    val email: String,  // email column : DB에서 text 타입으로 저장됌
    val password: String,  // 실제 서비스에서는 암호화해야함
    val nickname: String,

    // 추가 항목
    // String? : 값이 없어도 된다는 의미, 회원가입 시 선택 안 해도 오류 없음
    val gender: String?,          // 성별: 남자 / 여자
    val ageRange: String?,        // 연령대: 10대 / 20~24세 / ...
    val statusMessage: String?,   // 상태 메시지
    val category: String?         // 카테고리: 대학생 / 직장인 등
)
