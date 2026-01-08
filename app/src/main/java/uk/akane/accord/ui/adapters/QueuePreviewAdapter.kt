package uk.akane.accord.ui.adapters

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.ui.components.QueueBlendView
import uk.akane.cupertino.widget.utils.AnimationUtils.FASTEST_DURATION
import java.util.Collections
import kotlinx.coroutines.*

data class QueueItem(val uid: Any, val mediaItem: MediaItem)

class QueuePreviewAdapter(
    private val items: MutableList<QueueItem>,
    private val targetView: View,
    private val onMove: ((Int, Int) -> Unit)? = null,
    private val dragStartListener: DragStartListener? = null
) : RecyclerView.Adapter<QueuePreviewAdapter.ViewHolder>() {

    var isDragging = false
        private set
    
    private var diffJob: Job? = null

    interface DragStartListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }

    fun updateItems(newItems: List<QueueItem>) {
        if (isDragging) return
        
        diffJob?.cancel()
        diffJob = CoroutineScope(Dispatchers.Default).launch {
            val diffCallback = QueueDiffCallback(items.toList(), newItems)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            
            if (isActive) {
                withContext(Dispatchers.Main) {
                    items.clear()
                    items.addAll(newItems)
                    diffResult.dispatchUpdatesTo(this@QueuePreviewAdapter)
                }
            }
        }
    }

    fun onDragStart() {
        isDragging = true
    }

    fun onDragEnd() {
        isDragging = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_queue_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
        holder.subtitle.text = item.mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"

        holder.cover.load(item.mediaItem.mediaMetadata.artworkUri) {
            size(54.dp.px.toInt(), 54.dp.px.toInt())
        }

        holder.blendView.setup(targetView)

        if (holder.itemView.background == null) {
            holder.itemView.alpha = 1f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
            holder.itemView.translationZ = 0f
        }

        holder.reorderHandle.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                dragStartListener?.onStartDrag(holder)
                v.performClick()
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
        onMove?.invoke(fromPosition, toPosition)
        return true
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val subtitle: TextView = view.findViewById(R.id.subtitle)
        val cover: android.widget.ImageView = view.findViewById(R.id.cover)
        val reorderHandle: View = view.findViewById(R.id.reorder_handle)
        val blendView: QueueBlendView = view.findViewById(R.id.queue_blend_view)
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
                adapter.onDragStart()
                currentDragViewHolder = viewHolder
                viewHolder?.itemView?.let { view ->
                    view.animate().cancel()
                    view.outlineProvider = ViewOutlineProvider.BOUNDS
                    view.clipToOutline = true
                    view.animate()
                        .translationZ(10f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(FASTEST_DURATION)
                        .start()
                }
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                currentDragViewHolder?.itemView?.background = null
                currentDragViewHolder = null
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        adapter.onDragEnd()

        viewHolder.itemView.animate().cancel()
        viewHolder.itemView.background = null
        viewHolder.itemView.outlineProvider = ViewOutlineProvider.BACKGROUND
        viewHolder.itemView.clipToOutline = false
        viewHolder.itemView.animate()
            .translationZ(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(FASTEST_DURATION)
            .start()
    }
}

class QueueDiffCallback(
    private val oldList: List<QueueItem>,
    private val newList: List<QueueItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].uid == newList[newItemPosition].uid
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition].mediaItem
        val newItem = newList[newItemPosition].mediaItem
        return oldItem.mediaMetadata.title == newItem.mediaMetadata.title &&
                oldItem.mediaMetadata.artist == newItem.mediaMetadata.artist &&
                oldItem.mediaMetadata.artworkUri == newItem.mediaMetadata.artworkUri
    }
}