package edu.sswu.vitaday.ui.timer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B5CF6.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
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

    // 타이머 설정 (분 단위, 0~60분)
    private var minutes = 20
    private var maxMinutes = 60

    // 터치 핸들러 위치
    private var angle = 120f // 초기 각도 (20분)

    // 리스너
    var onTimeChangedListener: ((Int) -> Unit)? = null

    private val bounds = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) - 40f

        // 원형 배경
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // 테두리
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        // 진행 호 (0도부터 시계방향)
        bounds.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawArc(bounds, -90f, angle, false, progressPaint)

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

        // 5분 간격 눈금
        for (i in 0..11) {
            val tickAngle = i * 30.0 // 30도 간격 (12등분)
            val startX = centerX + (radius - 20) * cos(Math.toRadians(tickAngle - 90)).toFloat()
            val startY = centerY + (radius - 20) * sin(Math.toRadians(tickAngle - 90)).toFloat()
            val endX = centerX + (radius - 30) * cos(Math.toRadians(tickAngle - 90)).toFloat()
            val endY = centerY + (radius - 30) * sin(Math.toRadians(tickAngle - 90)).toFloat()

            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF666666.toInt()
                strokeWidth = 4f
            }
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
                    minutes = ((angle / 360f) * maxMinutes).toInt()
                    if (minutes == 0) minutes = 1 // 최소 1분

                    onTimeChangedListener?.invoke(minutes)
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 시간 설정 (분)
     */
    fun setMinutes(min: Int) {
        minutes = min.coerceIn(1, maxMinutes)
        angle = (minutes.toFloat() / maxMinutes) * 360f
        invalidate()
    }

    /**
     * 현재 설정된 시간 가져오기
     */
    fun getMinutes(): Int {
        return minutes
    }
}
//package edu.sswu.vitaday.ui.timer
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.graphics.RectF
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.View
//import androidx.core.content.ContextCompat
//import edu.sswu.vitaday.R
//import kotlin.math.atan2
//import kotlin.math.cos
//import kotlin.math.sin
//import kotlin.math.sqrt
//
///**
// * 터치로 시간을 설정할 수 있는 원형 타이머 뷰
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
//    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFF8B5CF6.toInt()
//        style = Paint.Style.STROKE
//        strokeWidth = 12f
//        strokeCap = Paint.Cap.ROUND
//    }
//
//    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = 0xFFFFFFFF.toInt()
//        style = Paint.Style.FILL
//    }
//
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
//        // 진행 호 (0도부터 시계방향)
//        bounds.set(
//            centerX - radius,
//            centerY - radius,
//            centerX + radius,
//            centerY + radius
//        )
//        canvas.drawArc(bounds, -90f, angle, false, progressPaint)
//
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
//
//        // 터치 핸들러 (흰색 점)
//        val handleX = centerX + radius * cos(Math.toRadians((angle - 90).toDouble())).toFloat()
//        val handleY = centerY + radius * sin(Math.toRadians((angle - 90).toDouble())).toFloat()
//        canvas.drawCircle(handleX, handleY, 20f, dotPaint)
//
//        // 5분 간격 눈금
//        for (i in 0..11) {
//            val tickAngle = i * 30.0 // 30도 간격 (12등분)
//            val startX = centerX + (radius - 20) * cos(Math.toRadians(tickAngle - 90)).toFloat()
//            val startY = centerY + (radius - 20) * sin(Math.toRadians(tickAngle - 90)).toFloat()
//            val endX = centerX + (radius - 30) * cos(Math.toRadians(tickAngle - 90)).toFloat()
//            val endY = centerY + (radius - 30) * sin(Math.toRadians(tickAngle - 90)).toFloat()
//
//            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//                color = 0xFF666666.toInt()
//                strokeWidth = 4f
//            }
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
//                if (distance > radius - 50 && distance < radius + 50) {
//                    // 각도 계산
//                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
//                    if (touchAngle < 0) touchAngle += 360f
//
//                    angle = touchAngle
//
//                    // 각도를 분으로 변환 (0~360도 -> 0~60분)
//                    minutes = ((angle / 360f) * maxMinutes).toInt()
//                    if (minutes == 0) minutes = 1 // 최소 1분
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
//     * 시간 설정 (분)
//     */
//    fun setMinutes(min: Int) {
//        minutes = min.coerceIn(1, maxMinutes)
//        angle = (minutes.toFloat() / maxMinutes) * 360f
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