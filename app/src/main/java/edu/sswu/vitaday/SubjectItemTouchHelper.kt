package edu.sswu.vitaday

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * 드래그 앤 드롭을 위한 ItemTouchHelper 콜백
 */
class SubjectItemTouchHelper(
    private val adapter: SubjectAdapter,
    private val onDragComplete: () -> Unit
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // 위아래로만 드래그 가능
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0 // 스와이프 비활성화
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition

        // 어댑터에 이동 알림
        adapter.onItemMove(fromPosition, toPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // 스와이프 비활성화
    }

    override fun isLongPressDragEnabled(): Boolean {
        // 롱 프레스로 드래그 시작
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        // 스와이프 비활성화
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // 드래그 시작 시 투명도 변경
            viewHolder?.itemView?.alpha = 0.7f
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // 드래그 종료 시 원래대로
        viewHolder.itemView.alpha = 1.0f

        // 드래그 완료 콜백
        onDragComplete()
    }
}