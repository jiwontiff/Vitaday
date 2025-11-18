package edu.sswu.vitaday.ui.calendar

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.sswu.vitaday.R

/**
 * 투두 리스트 어댑터
 */
class TodoListAdapter(
    private val onToggleComplete: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoListAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbComplete: CheckBox = itemView.findViewById(R.id.cb_complete)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_todo_title)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_todo)

        fun bind(todo: TodoItem) {
            cbComplete.isChecked = todo.isCompleted
            tvTitle.text = todo.title

            // 완료된 투두는 취소선 표시
            if (todo.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.5f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1f
            }

            // 체크박스 클릭
            cbComplete.setOnClickListener {
                onToggleComplete(todo)
            }

            // 삭제 버튼 클릭
            btnDelete.setOnClickListener {
                onDelete(todo)
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
}