package edu.sswu.vitaday.ui.timer


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import edu.sswu.vitaday.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 터치로 시간을 설정할 수 있는 원형 타이머 뷰
 * - 1분 단위 눈금 표시
 * - 설정한 시간만큼 색상으로 채우기
 * - 1분마다 진동 피드백
 */
class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

// 페인트 객체들
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E1E1E.toInt()
        style = Paint.Style.FILL
    }



    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B5CF6.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }


// ✅ 진행 호를 채운 스타일로 변경
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B5CF6.toInt()
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }



    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        color = 0xFFFFFFFF.toInt()

        textSize = 80f

        textAlign = Paint.Align.CENTER

    }



    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        color = 0xFFAAAAAA.toInt()

        textSize = 40f

        textAlign = Paint.Align.CENTER

    }



// ✅ 눈금용 페인트

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        color = 0xFF666666.toInt()

        strokeWidth = 4f

    }



// 타이머 설정 (분 단위, 0~60분)

    private var minutes = 20

    private var maxMinutes = 60



// 터치 핸들러 위치

    private var angle = 120f // 초기 각도 (20분)



// 리스너

    var onTimeChangedListener: ((Int) -> Unit)? = null



    private val bounds = RectF()



// ✅ 진동 관련

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var lastMinute = minutes // 이전 분 값 저장



    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)



        val centerX = width / 2f

        val centerY = height / 2f

        val radius = (Math.min(width, height) / 2f) - 40f



// 원형 배경

        canvas.drawCircle(centerX, centerY, radius, circlePaint)



// 테두리

        canvas.drawCircle(centerX, centerY, radius, strokePaint)



// ✅ 진행 호 (채워진 스타일 - 파이 형태)

        bounds.set(

            centerX - radius,

            centerY - radius,

            centerX + radius,

            centerY + radius

        )



// 파이 형태로 색칠 (중앙에서 시작)

        canvas.drawArc(bounds, -90f, angle, true, progressPaint)



// // 원형 배경을 다시 그려서 중앙 부분만 어둡게 (도넛 형태)

// val innerRadius = radius * 0.7f

// val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

// color = 0xFF1E1E1E.toInt()

// style = Paint.Style.FILL

// }

// canvas.drawCircle(centerX, centerY, innerRadius, innerCirclePaint)



// 시간 표시 (중앙)

        canvas.drawText(

            "$minutes",

            centerX,

            centerY + 20f,

            textPaint

        )



// "분" 표시

        canvas.drawText(

            "분",

            centerX,

            centerY + 70f,

            smallTextPaint

        )



// 터치 핸들러 (흰색 점)

        val handleX = centerX + radius * cos(Math.toRadians((angle - 90).toDouble())).toFloat()

        val handleY = centerY + radius * sin(Math.toRadians((angle - 90).toDouble())).toFloat()

        canvas.drawCircle(handleX, handleY, 20f, dotPaint)



// ✅ 1분 간격 눈금 (60개)

        for (i in 0 until 60) {

            val tickAngle = i * 6.0 // 6도 간격 (360/60 = 6)

            val isMainTick = i % 5 == 0 // 5분 단위는 굵게



            val tickLength = if (isMainTick) 30f else 15f

            val startRadius = radius - tickLength



            val startX = centerX + startRadius * cos(Math.toRadians(tickAngle - 90)).toFloat()

            val startY = centerY + startRadius * sin(Math.toRadians(tickAngle - 90)).toFloat()

            val endX = centerX + radius * cos(Math.toRadians(tickAngle - 90)).toFloat()

            val endY = centerY + radius * sin(Math.toRadians(tickAngle - 90)).toFloat()



            tickPaint.strokeWidth = if (isMainTick) 4f else 2f

            canvas.drawLine(startX, startY, endX, endY, tickPaint)

        }

    }



    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN,

            MotionEvent.ACTION_MOVE -> {

                val centerX = width / 2f

                val centerY = height / 2f



                val dx = event.x - centerX

                val dy = event.y - centerY



// 터치 지점이 원 안쪽인지 확인

                val distance = sqrt(dx * dx + dy * dy)

                val radius = (Math.min(width, height) / 2f) - 40f



                if (distance > radius - 50 && distance < radius + 50) {

// 각도 계산

                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f

                    if (touchAngle < 0) touchAngle += 360f



                    angle = touchAngle



// 각도를 분으로 변환 (0~360도 -> 0~60분)

                    val newMinutes = ((angle / 360f) * maxMinutes).toInt()

                    val finalMinutes = if (newMinutes == 0) 1 else newMinutes



// ✅ 1분 단위로 변경될 때만 진동

                    if (finalMinutes != lastMinute) {

                        vibrateOnMinuteChange()

                        lastMinute = finalMinutes

                    }



                    minutes = finalMinutes



                    onTimeChangedListener?.invoke(minutes)

                    invalidate()

                    return true

                }

            }

        }

        return super.onTouchEvent(event)

    }



    /**

     * ✅ 1분 변경 시 진동 피드백

     */

    private fun vibrateOnMinuteChange() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

// Android 8.0 이상

            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))

        } else {

// Android 8.0 미만

            @Suppress("DEPRECATION")

            vibrator.vibrate(50)

        }

    }



    /**

     * 시간 설정 (분)

     */

    fun setMinutes(min: Int) {

        minutes = min.coerceIn(1, maxMinutes)

        angle = (minutes.toFloat() / maxMinutes) * 360f

        lastMinute = minutes

        invalidate()

    }



    /**

     * 현재 설정된 시간 가져오기

     */

    fun getMinutes(): Int {

        return minutes

    }

}
//package edu.sswu.vitaday.ui.timer  // 👈 1. 대문자 T로 수정됨
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.graphics.RectF
//import android.os.VibrationEffect
//import android.os.Vibrator
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.View
//import kotlin.math.atan2
//import kotlin.math.cos
//import kotlin.math.sin
//import kotlin.math.sqrt
//
///**
// * 터치로 시간을 설정할 수 있는 원형 타이머 뷰
// * - 1분 단위 눈금 표시
// * - 설정한 시간만큼 색상으로 채우기 (파이 차트)
// * - 1분마다 진동 피드백
// */
//class CircularTimerView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    // 페인트 객체들
//    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFF1E1E1E.toInt()
//        style = Paint.Style.FILL
//    }
//
//    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFF8B5CF6.toInt()
//        style = Paint.Style.STROKE
//        strokeWidth = 8f
//    }
//
//    // ✅ 진행 호 (채워진 스타일 - 파이 형태)
//    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFF8B5CF6.toInt()
//        style = Paint.Style.FILL
//    }
//
//    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFFFFFFFF.toInt()
//        style = Paint.Style.FILL
//    }
//
//    // 중앙 텍스트 페인트 (더 이상 사용 안 함)
//    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFFFFFFFF.toInt()
//        textSize = 80f
//        textAlign = Paint.Align.CENTER
//    }
//
//    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFFAAAAAA.toInt()
//        textSize = 40f
//        textAlign = Paint.Align.CENTER
//    }
//
//    // ✅ 눈금용 페인트
//    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFF666666.toInt()
//        strokeWidth = 4f
//    }
//
//    // 타이머 설정 (분 단위, 0~60분)
//    private var minutes = 20
//    private var maxMinutes = 60
//
//    // 터치 핸들러 위치
//    private var angle = 120f // 초기 각도 (20분)
//
//    // 리스너
//    var onTimeChangedListener: ((Int) -> Unit)? = null
//
//    private val bounds = RectF()
//
//    // ✅ 진동 관련
//    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//    private var lastMinute = minutes // 이전 분 값 저장
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        val centerX = width / 2f
//        val centerY = height / 2f
//        val radius = (Math.min(width, height) / 2f) - 40f
//
//        // 원형 배경
//        canvas.drawCircle(centerX, centerY, radius, circlePaint)
//
//        // 테두리
//        canvas.drawCircle(centerX, centerY, radius, strokePaint)
//
//        // ✅ 진행 호 (채워진 스타일 - 파이 형태)
//        bounds.set(
//            centerX - radius,
//            centerY - radius,
//            centerX + radius,
//            centerY + radius
//        )
//
//        // 파이 형태로 색칠 (중앙에서 시작)
//        canvas.drawArc(bounds, -90f, angle, true, progressPaint)
//
//        // 👈 2. 글자 그리는 부분 주석 처리 (XML의 TextView와 겹침 방지)
//        /*
//        // 시간 표시 (중앙)
//        canvas.drawText(
//            "$minutes",
//            centerX,
//            centerY + 20f,
//            textPaint
//        )
//
//        // "분" 표시
//        canvas.drawText(
//            "분",
//            centerX,
//            centerY + 70f,
//            smallTextPaint
//        )
//        */
//
//        // 터치 핸들러 (흰색 점)
//        val handleX = centerX + radius * cos(Math.toRadians((angle - 90).toDouble())).toFloat()
//        val handleY = centerY + radius * sin(Math.toRadians((angle - 90).toDouble())).toFloat()
//        canvas.drawCircle(handleX, handleY, 20f, dotPaint)
//
//        // ✅ 1분 간격 눈금 (60개)
//        for (i in 0 until 60) {
//            val tickAngle = i * 6.0 // 6도 간격 (360/60 = 6)
//            val isMainTick = i % 5 == 0 // 5분 단위는 굵게
//
//            val tickLength = if (isMainTick) 30f else 15f
//            val startRadius = radius - tickLength
//
//            val startX = centerX + startRadius * cos(Math.toRadians(tickAngle - 90)).toFloat()
//            val startY = centerY + startRadius * sin(Math.toRadians(tickAngle - 90)).toFloat()
//            val endX = centerX + radius * cos(Math.toRadians(tickAngle - 90)).toFloat()
//            val endY = centerY + radius * sin(Math.toRadians(tickAngle - 90)).toFloat()
//
//            tickPaint.strokeWidth = if (isMainTick) 4f else 2f
//            canvas.drawLine(startX, startY, endX, endY, tickPaint)
//        }
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_DOWN,
//            MotionEvent.ACTION_MOVE -> {
//                val centerX = width / 2f
//                val centerY = height / 2f
//
//                val dx = event.x - centerX
//                val dy = event.y - centerY
//
//                // 터치 지점이 원 안쪽인지 확인
//                val distance = sqrt(dx * dx + dy * dy)
//                val radius = (Math.min(width, height) / 2f) - 40f
//
//                if (distance > radius - 100 && distance < radius + 100) { // 터치 범위 약간 여유 있게 수정
//                    // 각도 계산
//                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
//                    if (touchAngle < 0) touchAngle += 360f
//
//                    angle = touchAngle
//
//                    // 각도를 분으로 변환 (0~360도 -> 0~60분)
//                    val newMinutes = ((angle / 360f) * maxMinutes).toInt()
//                    val finalMinutes = if (newMinutes == 0) 60 else newMinutes // 0분이면 60분으로 처리
//
//                    // ✅ 1분 단위로 변경될 때만 진동
//                    if (finalMinutes != lastMinute) {
//                        vibrateOnMinuteChange()
//                        lastMinute = finalMinutes
//                    }
//
//                    minutes = finalMinutes
//
//                    onTimeChangedListener?.invoke(minutes)
//                    invalidate()
//                    return true
//                }
//            }
//        }
//        return super.onTouchEvent(event)
//    }
//
//    /**
//     * ✅ 1분 변경 시 진동 피드백
//     */
//    private fun vibrateOnMinuteChange() {
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            // Android 8.0 이상
//            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
//        } else {
//            // Android 8.0 미만
//            @Suppress("DEPRECATION")
//            vibrator.vibrate(50)
//        }
//    }
//
//    /**
//     * 시간 설정 (분)
//     */
//    fun setMinutes(min: Int) {
//        minutes = min.coerceIn(1, maxMinutes)
//        angle = (minutes.toFloat() / maxMinutes) * 360f
//        lastMinute = minutes
//        invalidate()
//    }
//
//    /**
//     * 현재 설정된 시간 가져오기
//     */
//    fun getMinutes(): Int {
//        return minutes
//    }
//}
////package edu.sswu.vitaday.ui.timer
////
////import android.content.Context
////import android.graphics.Canvas
////import android.graphics.Paint
////import android.graphics.RectF
////import android.os.VibrationEffect
////import android.os.Vibrator
////import android.util.AttributeSet
////import android.view.MotionEvent
////import android.view.View
////import androidx.core.content.ContextCompat
////import edu.sswu.vitaday.R
////import kotlin.math.atan2
////import kotlin.math.cos
////import kotlin.math.sin
////import kotlin.math.sqrt
////
/////**
//// * 터치로 시간을 설정할 수 있는 원형 타이머 뷰
//// * - 1분 단위 눈금 표시
//// * - 설정한 시간만큼 색상으로 채우기
//// * - 1분마다 진동 피드백
//// */
////class CircularTimerView @JvmOverloads constructor(
////    context: Context,
////    attrs: AttributeSet? = null,
////    defStyleAttr: Int = 0
////) : View(context, attrs, defStyleAttr) {
////
////    // 페인트 객체들
////    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFF1E1E1E.toInt()
////        style = Paint.Style.FILL
////    }
////
////    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFF8B5CF6.toInt()
////        style = Paint.Style.STROKE
////        strokeWidth = 8f
////    }
////
////    // ✅ 진행 호를 채운 스타일로 변경
////    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFF8B5CF6.toInt()
////        style = Paint.Style.FILL
////    }
////
////    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFFFFFFFF.toInt()
////        style = Paint.Style.FILL
////    }
////
////    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFFFFFFFF.toInt()
////        textSize = 80f
////        textAlign = Paint.Align.CENTER
////    }
////
////    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFFAAAAAA.toInt()
////        textSize = 40f
////        textAlign = Paint.Align.CENTER
////    }
////
////    // ✅ 눈금용 페인트
////    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////        color = 0xFF666666.toInt()
////        strokeWidth = 4f
////    }
////
////    // 타이머 설정 (분 단위, 0~60분)
////    private var minutes = 20
////    private var maxMinutes = 60
////
////    // 터치 핸들러 위치
////    private var angle = 120f // 초기 각도 (20분)
////
////    // 리스너
////    var onTimeChangedListener: ((Int) -> Unit)? = null
////
////    private val bounds = RectF()
////
////    // ✅ 진동 관련
////    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
////    private var lastMinute = minutes // 이전 분 값 저장
////
////    override fun onDraw(canvas: Canvas) {
////        super.onDraw(canvas)
////
////        val centerX = width / 2f
////        val centerY = height / 2f
////        val radius = (Math.min(width, height) / 2f) - 40f
////
////        // 원형 배경
////        canvas.drawCircle(centerX, centerY, radius, circlePaint)
////
////        // 테두리
////        canvas.drawCircle(centerX, centerY, radius, strokePaint)
////
////        // ✅ 진행 호 (채워진 스타일 - 파이 형태)
////        bounds.set(
////            centerX - radius,
////            centerY - radius,
////            centerX + radius,
////            centerY + radius
////        )
////
////        // 파이 형태로 색칠 (중앙에서 시작)
////        canvas.drawArc(bounds, -90f, angle, true, progressPaint)
////
//////        // 원형 배경을 다시 그려서 중앙 부분만 어둡게 (도넛 형태)
//////        val innerRadius = radius * 0.7f
//////        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////            color = 0xFF1E1E1E.toInt()
//////            style = Paint.Style.FILL
//////        }
//////        canvas.drawCircle(centerX, centerY, innerRadius, innerCirclePaint)
////
////        // 시간 표시 (중앙)
////        canvas.drawText(
////            "$minutes",
////            centerX,
////            centerY + 20f,
////            textPaint
////        )
////
////        // "분" 표시
////        canvas.drawText(
////            "분",
////            centerX,
////            centerY + 70f,
////            smallTextPaint
////        )
////
////        // 터치 핸들러 (흰색 점)
////        val handleX = centerX + radius * cos(Math.toRadians((angle - 90).toDouble())).toFloat()
////        val handleY = centerY + radius * sin(Math.toRadians((angle - 90).toDouble())).toFloat()
////        canvas.drawCircle(handleX, handleY, 20f, dotPaint)
////
////        // ✅ 1분 간격 눈금 (60개)
////        for (i in 0 until 60) {
////            val tickAngle = i * 6.0 // 6도 간격 (360/60 = 6)
////            val isMainTick = i % 5 == 0 // 5분 단위는 굵게
////
////            val tickLength = if (isMainTick) 30f else 15f
////            val startRadius = radius - tickLength
////
////            val startX = centerX + startRadius * cos(Math.toRadians(tickAngle - 90)).toFloat()
////            val startY = centerY + startRadius * sin(Math.toRadians(tickAngle - 90)).toFloat()
////            val endX = centerX + radius * cos(Math.toRadians(tickAngle - 90)).toFloat()
////            val endY = centerY + radius * sin(Math.toRadians(tickAngle - 90)).toFloat()
////
////            tickPaint.strokeWidth = if (isMainTick) 4f else 2f
////            canvas.drawLine(startX, startY, endX, endY, tickPaint)
////        }
////    }
////
////    override fun onTouchEvent(event: MotionEvent): Boolean {
////        when (event.action) {
////            MotionEvent.ACTION_DOWN,
////            MotionEvent.ACTION_MOVE -> {
////                val centerX = width / 2f
////                val centerY = height / 2f
////
////                val dx = event.x - centerX
////                val dy = event.y - centerY
////
////                // 터치 지점이 원 안쪽인지 확인
////                val distance = sqrt(dx * dx + dy * dy)
////                val radius = (Math.min(width, height) / 2f) - 40f
////
////                if (distance > radius - 50 && distance < radius + 50) {
////                    // 각도 계산
////                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
////                    if (touchAngle < 0) touchAngle += 360f
////
////                    angle = touchAngle
////
////                    // 각도를 분으로 변환 (0~360도 -> 0~60분)
////                    val newMinutes = ((angle / 360f) * maxMinutes).toInt()
////                    val finalMinutes = if (newMinutes == 0) 1 else newMinutes
////
////                    // ✅ 1분 단위로 변경될 때만 진동
////                    if (finalMinutes != lastMinute) {
////                        vibrateOnMinuteChange()
////                        lastMinute = finalMinutes
////                    }
////
////                    minutes = finalMinutes
////
////                    onTimeChangedListener?.invoke(minutes)
////                    invalidate()
////                    return true
////                }
////            }
////        }
////        return super.onTouchEvent(event)
////    }
////
////    /**
////     * ✅ 1분 변경 시 진동 피드백
////     */
////    private fun vibrateOnMinuteChange() {
////        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
////            // Android 8.0 이상
////            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
////        } else {
////            // Android 8.0 미만
////            @Suppress("DEPRECATION")
////            vibrator.vibrate(50)
////        }
////    }
////
////    /**
////     * 시간 설정 (분)
////     */
////    fun setMinutes(min: Int) {
////        minutes = min.coerceIn(1, maxMinutes)
////        angle = (minutes.toFloat() / maxMinutes) * 360f
////        lastMinute = minutes
////        invalidate()
////    }
////
////    /**
////     * 현재 설정된 시간 가져오기
////     */
////    fun getMinutes(): Int {
////        return minutes
////    }
////}
//////package edu.sswu.vitaday.ui.timer
//////
//////import android.content.Context
//////import android.graphics.Canvas
//////import android.graphics.Paint
//////import android.graphics.RectF
//////import android.util.AttributeSet
//////import android.view.MotionEvent
//////import android.view.View
//////import androidx.core.content.ContextCompat
//////import edu.sswu.vitaday.R
//////import kotlin.math.atan2
//////import kotlin.math.cos
//////import kotlin.math.sin
//////import kotlin.math.sqrt
//////
///////**
////// * 터치로 시간을 설정할 수 있는 원형 타이머 뷰
////// */
//////class CircularTimerView @JvmOverloads constructor(
//////    context: Context,
//////    attrs: AttributeSet? = null,
//////    defStyleAttr: Int = 0
//////) : View(context, attrs, defStyleAttr) {
//////
//////    // 페인트 객체들
//////    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////        color = 0xFF1E1E1E.toInt()
//////        style = Paint.Style.FILL
//////    }
//////
//////    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////        color = 0xFF8B5CF6.toInt()
//////        style = Paint.Style.STROKE
//////        strokeWidth = 8f
//////    }
//////
//////    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////        color = 0xFF8B5CF6.toInt()
//////        style = Paint.Style.STROKE
//////        strokeWidth = 12f
//////        strokeCap = Paint.Cap.ROUND
//////    }
//////
//////    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////        color = 0xFFFFFFFF.toInt()
//////        style = Paint.Style.FILL
//////    }
//////
//////    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////        color = 0xFFFFFFFF.toInt()
//////        textSize = 80f
//////        textAlign = Paint.Align.CENTER
//////    }
//////
//////    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////        color = 0xFFAAAAAA.toInt()
//////        textSize = 40f
//////        textAlign = Paint.Align.CENTER
//////    }
//////
//////    // 타이머 설정 (분 단위, 0~60분)
//////    private var minutes = 20
//////    private var maxMinutes = 60
//////
//////    // 터치 핸들러 위치
//////    private var angle = 120f // 초기 각도 (20분)
//////
//////    // 리스너
//////    var onTimeChangedListener: ((Int) -> Unit)? = null
//////
//////    private val bounds = RectF()
//////
//////    override fun onDraw(canvas: Canvas) {
//////        super.onDraw(canvas)
//////
//////        val centerX = width / 2f
//////        val centerY = height / 2f
//////        val radius = (Math.min(width, height) / 2f) - 40f
//////
//////        // 원형 배경
//////        canvas.drawCircle(centerX, centerY, radius, circlePaint)
//////
//////        // 테두리
//////        canvas.drawCircle(centerX, centerY, radius, strokePaint)
//////
//////        // 진행 호 (0도부터 시계방향)
//////        bounds.set(
//////            centerX - radius,
//////            centerY - radius,
//////            centerX + radius,
//////            centerY + radius
//////        )
//////        canvas.drawArc(bounds, -90f, angle, false, progressPaint)
//////
//////        // 시간 표시 (중앙)
//////        canvas.drawText(
//////            "$minutes",
//////            centerX,
//////            centerY + 20f,
//////            textPaint
//////        )
//////
//////        // "분" 표시
//////        canvas.drawText(
//////            "분",
//////            centerX,
//////            centerY + 70f,
//////            smallTextPaint
//////        )
//////
//////        // 터치 핸들러 (흰색 점)
//////        val handleX = centerX + radius * cos(Math.toRadians((angle - 90).toDouble())).toFloat()
//////        val handleY = centerY + radius * sin(Math.toRadians((angle - 90).toDouble())).toFloat()
//////        canvas.drawCircle(handleX, handleY, 20f, dotPaint)
//////
//////        // 5분 간격 눈금
//////        for (i in 0..11) {
//////            val tickAngle = i * 30.0 // 30도 간격 (12등분)
//////            val startX = centerX + (radius - 20) * cos(Math.toRadians(tickAngle - 90)).toFloat()
//////            val startY = centerY + (radius - 20) * sin(Math.toRadians(tickAngle - 90)).toFloat()
//////            val endX = centerX + (radius - 30) * cos(Math.toRadians(tickAngle - 90)).toFloat()
//////            val endY = centerY + (radius - 30) * sin(Math.toRadians(tickAngle - 90)).toFloat()
//////
//////            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//////                color = 0xFF666666.toInt()
//////                strokeWidth = 4f
//////            }
//////            canvas.drawLine(startX, startY, endX, endY, tickPaint)
//////        }
//////    }
//////
//////    override fun onTouchEvent(event: MotionEvent): Boolean {
//////        when (event.action) {
//////            MotionEvent.ACTION_DOWN,
//////            MotionEvent.ACTION_MOVE -> {
//////                val centerX = width / 2f
//////                val centerY = height / 2f
//////
//////                val dx = event.x - centerX
//////                val dy = event.y - centerY
//////
//////                // 터치 지점이 원 안쪽인지 확인
//////                val distance = sqrt(dx * dx + dy * dy)
//////                val radius = (Math.min(width, height) / 2f) - 40f
//////
//////                if (distance > radius - 50 && distance < radius + 50) {
//////                    // 각도 계산
//////                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
//////                    if (touchAngle < 0) touchAngle += 360f
//////
//////                    angle = touchAngle
//////
//////                    // 각도를 분으로 변환 (0~360도 -> 0~60분)
//////                    minutes = ((angle / 360f) * maxMinutes).toInt()
//////                    if (minutes == 0) minutes = 1 // 최소 1분
//////
//////                    onTimeChangedListener?.invoke(minutes)
//////                    invalidate()
//////                    return true
//////                }
//////            }
//////        }
//////        return super.onTouchEvent(event)
//////    }
//////
//////    /**
//////     * 시간 설정 (분)
//////     */
//////    fun setMinutes(min: Int) {
//////        minutes = min.coerceIn(1, maxMinutes)
//////        angle = (minutes.toFloat() / maxMinutes) * 360f
//////        invalidate()
//////    }
//////
//////    /**
//////     * 현재 설정된 시간 가져오기
//////     */
//////    fun getMinutes(): Int {
//////        return minutes
//////    }
//////}
////////package edu.sswu.vitaday.ui.timer
////////
////////import android.content.Context
////////import android.graphics.Canvas
////////import android.graphics.Paint
////////import android.graphics.RectF
////////import android.util.AttributeSet
////////import android.view.MotionEvent
////////import android.view.View
////////import androidx.core.content.ContextCompat
////////import edu.sswu.vitaday.R
////////import kotlin.math.atan2
////////import kotlin.math.cos
////////import kotlin.math.sin
////////import kotlin.math.sqrt
////////
/////////**
//////// * 터치로 시간을 설정할 수 있는 원형 타이머 뷰
//////// */
////////class CircularTimerView @JvmOverloads constructor(
////////    context: Context,
////////    attrs: AttributeSet? = null,
////////    defStyleAttr: Int = 0
////////) : View(context, attrs, defStyleAttr) {
////////
////////    // 페인트 객체들
////////    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////        color = 0xFF1E1E1E.toInt()
////////        style = Paint.Style.FILL
////////    }
////////
////////    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////        color = 0xFF8B5CF6.toInt()
////////        style = Paint.Style.STROKE
////////        strokeWidth = 8f
////////    }
////////
////////    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////        color = 0xFF8B5CF6.toInt()
////////        style = Paint.Style.STROKE
////////        strokeWidth = 12f
////////        strokeCap = Paint.Cap.ROUND
////////    }
////////
////////    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////        color = 0xFFFFFFFF.toInt()
////////        style = Paint.Style.FILL
////////    }
////////
////////    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////        color = 0xFFFFFFFF.toInt()
////////        textSize = 80f
////////        textAlign = Paint.Align.CENTER
////////    }
////////
////////    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////        color = 0xFFAAAAAA.toInt()
////////        textSize = 40f
////////        textAlign = Paint.Align.CENTER
////////    }
////////
////////    // 타이머 설정 (분 단위, 0~60분)
////////    private var minutes = 20
////////    private var maxMinutes = 60
////////
////////    // 터치 핸들러 위치
////////    private var angle = 120f // 초기 각도 (20분)
////////
////////    // 리스너
////////    var onTimeChangedListener: ((Int) -> Unit)? = null
////////
////////    private val bounds = RectF()
////////
////////    override fun onDraw(canvas: Canvas) {
////////        super.onDraw(canvas)
////////
////////        val centerX = width / 2f
////////        val centerY = height / 2f
////////        val radius = (Math.min(width, height) / 2f) - 40f
////////
////////        // 원형 배경
////////        canvas.drawCircle(centerX, centerY, radius, circlePaint)
////////
////////        // 테두리
////////        canvas.drawCircle(centerX, centerY, radius, strokePaint)
////////
////////        // 진행 호 (0도부터 시계방향)
////////        bounds.set(
////////            centerX - radius,
////////            centerY - radius,
////////            centerX + radius,
////////            centerY + radius
////////        )
////////        canvas.drawArc(bounds, -90f, angle, false, progressPaint)
////////
////////        // 시간 표시 (중앙)
////////        canvas.drawText(
////////            "$minutes",
////////            centerX,
////////            centerY + 20f,
////////            textPaint
////////        )
////////
////////        // "분" 표시
////////        canvas.drawText(
////////            "분",
////////            centerX,
////////            centerY + 70f,
////////            smallTextPaint
////////        )
////////
////////        // 터치 핸들러 (흰색 점)
////////        val handleX = centerX + radius * cos(Math.toRadians((angle - 90).toDouble())).toFloat()
////////        val handleY = centerY + radius * sin(Math.toRadians((angle - 90).toDouble())).toFloat()
////////        canvas.drawCircle(handleX, handleY, 20f, dotPaint)
////////
////////        // 5분 간격 눈금
////////        for (i in 0..11) {
////////            val tickAngle = i * 30.0 // 30도 간격 (12등분)
////////            val startX = centerX + (radius - 20) * cos(Math.toRadians(tickAngle - 90)).toFloat()
////////            val startY = centerY + (radius - 20) * sin(Math.toRadians(tickAngle - 90)).toFloat()
////////            val endX = centerX + (radius - 30) * cos(Math.toRadians(tickAngle - 90)).toFloat()
////////            val endY = centerY + (radius - 30) * sin(Math.toRadians(tickAngle - 90)).toFloat()
////////
////////            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
////////                color = 0xFF666666.toInt()
////////                strokeWidth = 4f
////////            }
////////            canvas.drawLine(startX, startY, endX, endY, tickPaint)
////////        }
////////    }
////////
////////    override fun onTouchEvent(event: MotionEvent): Boolean {
////////        when (event.action) {
////////            MotionEvent.ACTION_DOWN,
////////            MotionEvent.ACTION_MOVE -> {
////////                val centerX = width / 2f
////////                val centerY = height / 2f
////////
////////                val dx = event.x - centerX
////////                val dy = event.y - centerY
////////
////////                // 터치 지점이 원 안쪽인지 확인
////////                val distance = sqrt(dx * dx + dy * dy)
////////                val radius = (Math.min(width, height) / 2f) - 40f
////////
////////                if (distance > radius - 50 && distance < radius + 50) {
////////                    // 각도 계산
////////                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
////////                    if (touchAngle < 0) touchAngle += 360f
////////
////////                    angle = touchAngle
////////
////////                    // 각도를 분으로 변환 (0~360도 -> 0~60분)
////////                    minutes = ((angle / 360f) * maxMinutes).toInt()
////////                    if (minutes == 0) minutes = 1 // 최소 1분
////////
////////                    onTimeChangedListener?.invoke(minutes)
////////                    invalidate()
////////                    return true
////////                }
////////            }
////////        }
////////        return super.onTouchEvent(event)
////////    }
////////
////////    /**
////////     * 시간 설정 (분)
////////     */
////////    fun setMinutes(min: Int) {
////////        minutes = min.coerceIn(1, maxMinutes)
////////        angle = (minutes.toFloat() / maxMinutes) * 360f
////////        invalidate()
////////    }
////////
////////    /**
////////     * 현재 설정된 시간 가져오기
////////     */
////////    fun getMinutes(): Int {
////////        return minutes
////////    }
////////}