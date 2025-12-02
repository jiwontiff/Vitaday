package edu.sswu.vitaday

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class SubjectDistributionAdapter : ListAdapter<SubjectDuration, SubjectDistributionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_distribution, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubjectName: TextView = itemView.findViewById(R.id.tvSubjectName)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        private val viewColorIndicator: View = itemView.findViewById(R.id.viewColorIndicator)

        fun bind(subjectDuration: SubjectDuration, position: Int) {
            tvSubjectName.text = subjectDuration.subjectName

            val hours = subjectDuration.totalDuration / (1000 * 60 * 60)
            val minutes = (subjectDuration.totalDuration / (1000 * 60)) % 60
            tvDuration.text = when {
                hours > 0 -> "${hours}시간 ${minutes}분"
                else -> "${minutes}분"
            }

            val totalDuration = currentList.sumOf { it.totalDuration }
            val percentage = if (totalDuration > 0) {
                (subjectDuration.totalDuration.toFloat() / totalDuration * 100).toInt()
            } else 0
            tvPercentage.text = "${percentage}%"

            val colors = listOf(
                "#FF5252", "#7C4DFF", "#00BCD4", "#4CAF50",
                "#FFC107", "#FF9800", "#E91E63"
            )
            val color = colors[position % colors.size]
            viewColorIndicator.setBackgroundColor(Color.parseColor(color))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SubjectDuration>() {
        override fun areItemsTheSame(oldItem: SubjectDuration, newItem: SubjectDuration): Boolean {
            return oldItem.subjectId == newItem.subjectId
        }

        override fun areContentsTheSame(oldItem: SubjectDuration, newItem: SubjectDuration): Boolean {
            return oldItem == newItem
        }
    }
}