package edu.sswu.vitaday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView

/**
 * 과목 리스트 어댑터 (드래그 앤 드롭 지원)
 */
class SubjectAdapter(
    private val onSubjectClick: (SubjectData) -> Unit,
    private val onSubjectDoubleClick: (SubjectData) -> Unit,
    private val onSubjectDelete: (SubjectData) -> Unit,
    private val getTodayTime: (Int) -> String
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    private val items = mutableListOf<SubjectData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_home_card, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<SubjectData>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /**
     * 드래그 앤 드롭: 아이템 이동
     */
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || fromPosition >= items.size ||
            toPosition < 0 || toPosition >= items.size) {
            return false
        }

        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    /**
     * 드래그 완료 후 순서 변경된 리스트 반환
     */
    fun getReorderedList(): List<SubjectData> {
        return items.toList()
    }

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColor: View = itemView.findViewById(R.id.view_subject_color)
        private val tvName: TextView = itemView.findViewById(R.id.tv_subject_name)
        private val tvTodayTime: TextView = itemView.findViewById(R.id.tv_today_time)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_subject)

        private var lastClickTime = 0L
        private val DOUBLE_CLICK_TIME_DELTA = 300L // 300ms

        fun bind(subject: SubjectData) {
            viewColor.setBackgroundColor(subject.colorHex.toColorInt())
            tvName.text = subject.name
            tvTodayTime.text = getTodayTime(subject.id)

            // 싱글 클릭 / 더블 클릭 감지
            itemView.setOnClickListener {
                val clickTime = System.currentTimeMillis()
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    // 더블 클릭
                    onSubjectDoubleClick(subject)
                    lastClickTime = 0L
                } else {
                    // 싱글 클릭 (300ms 후 실행)
                    lastClickTime = clickTime
                    itemView.postDelayed({
                        if (System.currentTimeMillis() - lastClickTime >= DOUBLE_CLICK_TIME_DELTA) {
                            onSubjectClick(subject)
                        }
                    }, DOUBLE_CLICK_TIME_DELTA)
                }
            }

            // 삭제 버튼
            btnDelete.setOnClickListener {
                onSubjectDelete(subject)
            }
        }
    }
}
//package edu.sswu.vitaday
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageButton
//import android.widget.TextView
//import androidx.core.graphics.toColorInt
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import java.util.Collections
//
///**
// * 과목 리스트 어댑터 (드래그 앤 드롭 지원)
// */
//class SubjectAdapter(
//    private val onSubjectClick: (SubjectData) -> Unit,
//    private val onSubjectDoubleClick: (SubjectData) -> Unit,
//    private val onSubjectDelete: (SubjectData) -> Unit,
//    private val getTodayTime: (Int) -> String
//) : ListAdapter<SubjectData, SubjectAdapter.SubjectViewHolder>(SubjectDiffCallback()) {
//
//    private val items = mutableListOf<SubjectData>()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_subject_home_card, parent, false)
//        return SubjectViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
//        holder.bind(getItem(position))
//    }
//
//    override fun submitList(list: List<SubjectData>?) {
//        super.submitList(list)
//        items.clear()
//        list?.let { items.addAll(it) }
//    }
//
//    /**
//     * 드래그 앤 드롭: 아이템 이동
//     */
//    fun onItemMove(fromPosition: Int, toPosition: Int) {
//        Collections.swap(items, fromPosition, toPosition)
//        notifyItemMoved(fromPosition, toPosition)
//    }
//
//    /**
//     * 드래그 완료 후 순서 변경된 리스트 반환
//     */
//    fun getReorderedList(): List<SubjectData> {
//        return items.toList()
//    }
//
//    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val viewColor: View = itemView.findViewById(R.id.view_subject_color)
//        private val tvName: TextView = itemView.findViewById(R.id.tv_subject_name)
//        private val tvTodayTime: TextView = itemView.findViewById(R.id.tv_today_time)
//        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_subject)
//
//        private var lastClickTime = 0L
//        private val DOUBLE_CLICK_TIME_DELTA = 300L // 300ms
//
//        fun bind(subject: SubjectData) {
//            viewColor.setBackgroundColor(subject.colorHex.toColorInt())
//            tvName.text = subject.name
//            tvTodayTime.text = getTodayTime(subject.id)
//
//            // 싱글 클릭 / 더블 클릭 감지
//            itemView.setOnClickListener {
//                val clickTime = System.currentTimeMillis()
//                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
//                    // 더블 클릭
//                    onSubjectDoubleClick(subject)
//                    lastClickTime = 0L
//                } else {
//                    // 싱글 클릭 (300ms 후 실행)
//                    lastClickTime = clickTime
//                    itemView.postDelayed({
//                        if (System.currentTimeMillis() - lastClickTime >= DOUBLE_CLICK_TIME_DELTA) {
//                            onSubjectClick(subject)
//                        }
//                    }, DOUBLE_CLICK_TIME_DELTA)
//                }
//            }
//
//            // 삭제 버튼
//            btnDelete.setOnClickListener {
//                onSubjectDelete(subject)
//            }
//        }
//    }
//
//    class SubjectDiffCallback : DiffUtil.ItemCallback<SubjectData>() {
//        override fun areItemsTheSame(oldItem: SubjectData, newItem: SubjectData): Boolean {
//            return oldItem.id == newItem.id
//        }
//
//        override fun areContentsTheSame(oldItem: SubjectData, newItem: SubjectData): Boolean {
//            return oldItem == newItem
//        }
//    }
//}