package uk.akane.accord.ui.adapters.browse

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity

class SongAdapter(
    context: Context,
    fragment: Fragment
) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

    private val list = mutableListOf<MediaItem>()

    init {
        fragment.lifecycleScope.launch {
            (fragment.activity as MainActivity).reader?.songListFlow?.collectLatest { newList ->
                Log.d("TAG", "newList: ${newList.size}")
                submitList(newList)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.layout_song_item, parent, false
            )
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]
        holder.cover.load(item.mediaMetadata.artworkUri) {
            crossfade(true)
        }
        holder.title.text = item.mediaMetadata.title
        holder.subtitle.text = item.mediaMetadata.subtitle
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover = view.findViewById<ImageView>(R.id.cover)
        val title = view.findViewById<TextView>(R.id.title)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
    }

    private fun submitList(newList: List<MediaItem>) {
        val diffResult = DiffUtil.calculateDiff(GenreDiffCallback(list, newList))
        list.clear()
        list.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    class GenreDiffCallback(
        private val oldList: List<MediaItem>,
        private val newList: List<MediaItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].mediaId == newList[newItemPosition].mediaId
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].mediaMetadata == newList[newItemPosition].mediaMetadata
    }

}