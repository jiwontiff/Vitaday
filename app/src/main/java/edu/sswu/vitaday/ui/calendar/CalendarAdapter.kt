package edu.sswu.vitaday.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.sswu.vitaday.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 캘린더 날짜 표시 어댑터
 */
class CalendarAdapter(
    private val onDateClick: (Date) -> Unit,
    private val getTodoCount: (Date) -> Int
) : RecyclerView.Adapter<CalendarAdapter.DateViewHolder>() {

    private var dates: List<CalendarDate> = emptyList()
    private var selectedDate: Date? = null
    private val today = Date()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 날짜 데이터 클래스
     */
    data class CalendarDate(
        val date: Date,
        val dayOfMonth: Int,
        val isCurrentMonth: Boolean
    )

    fun submitDates(newDates: List<CalendarDate>, selected: Date?) {
        dates = newDates
        selectedDate = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_day_item, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val calendarDate = dates[position]
        holder.bind(calendarDate)
    }

    override fun getItemCount(): Int = dates.size

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        private val viewIndicator: View = itemView.findViewById(R.id.view_indicator)
        private val viewSelected: View = itemView.findViewById(R.id.view_selected)

        fun bind(calendarDate: CalendarDate) {
            tvDay.text = calendarDate.dayOfMonth.toString()

            // 현재 월이 아닌 날짜는 흐리게
            if (!calendarDate.isCurrentMonth) {
                tvDay.alpha = 0.3f
                tvDay.setTextColor(Color.parseColor("#666666"))
            } else {
                tvDay.alpha = 1f
                tvDay.setTextColor(Color.parseColor("#FFFFFF"))
            }

            // 오늘 날짜 표시
            val isToday = isSameDay(calendarDate.date, today)
            if (isToday) {
                tvDay.setTextColor(Color.parseColor("#8B5CF6"))
                tvDay.setBackgroundResource(R.drawable.bg_selected_day)
            } else {
                tvDay.background = null
            }

            // 선택된 날짜 표시
            val isSelected = selectedDate?.let { isSameDay(calendarDate.date, it) } ?: false
            viewSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            // 투두가 있는 날짜에 인디케이터 표시
            val todoCount = getTodoCount(calendarDate.date)
            viewIndicator.visibility = if (todoCount > 0) View.VISIBLE else View.GONE

            // 클릭 리스너
            itemView.setOnClickListener {
                onDateClick(calendarDate.date)
            }
        }

        private fun isSameDay(date1: Date, date2: Date): Boolean {
            return dateFormat.format(date1) == dateFormat.format(date2)
        }
    }
}