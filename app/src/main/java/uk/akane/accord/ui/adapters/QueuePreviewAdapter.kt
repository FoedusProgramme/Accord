package uk.akane.accord.ui.adapters

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.cupertino.widget.utils.AnimationUtils.FASTEST_DURATION
import java.util.Collections

class QueuePreviewAdapter(
    private val items: MutableList<Item>,
    private val dragStartListener: DragStartListener? = null
) : RecyclerView.Adapter<QueuePreviewAdapter.ViewHolder>() {

    data class Item(
        val title: String,
        val subtitle: String
    )

    interface DragStartListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_queue_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        if (holder.itemView.background == null) {
            holder.itemView.alpha = 1f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
            holder.itemView.translationZ = 0f
        }

        holder.reorderHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                dragStartListener?.onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = items.size

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false
        }
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val subtitle: TextView = view.findViewById(R.id.subtitle)
        val reorderHandle: View = view.findViewById(R.id.reorder_handle)
        val cover: ImageView = view.findViewById(R.id.cover)
        private val shadowEffectView: uk.akane.cupertino.widget.special.BlendView = view.findViewById(R.id.shadow_effect)
        private val contentContainer: View = view.findViewById(R.id.content_container)

        fun setDragState(dragging: Boolean) {
            if (dragging) {
                val drawable = cover.drawable
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    shadowEffectView.setImageBitmap(drawable.bitmap)
                }
                shadowEffectView.startRotationAnimation()
                shadowEffectView.visibility = View.VISIBLE
            } else {
                shadowEffectView.stopRotationAnimation()
                shadowEffectView.visibility = View.GONE
            }
        }
    }
}

class QueueItemTouchHelperCallback(
    private val adapter: QueuePreviewAdapter
) : ItemTouchHelper.Callback() {
    private var currentDragViewHolder: RecyclerView.ViewHolder? = null

    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                currentDragViewHolder = viewHolder
                if (viewHolder is QueuePreviewAdapter.ViewHolder) {
                    viewHolder.setDragState(true)
                }
                viewHolder?.itemView?.let { view ->
                    view.animate().cancel()
                    view.animate()
                        .translationZ(16f)
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(FASTEST_DURATION)
                        .start()
                }
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                currentDragViewHolder = null
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (viewHolder is QueuePreviewAdapter.ViewHolder) {
            viewHolder.setDragState(false)
        }
        viewHolder.itemView.animate().cancel()
        viewHolder.itemView.animate()
            .translationZ(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(FASTEST_DURATION)
            .start()
    }
}
