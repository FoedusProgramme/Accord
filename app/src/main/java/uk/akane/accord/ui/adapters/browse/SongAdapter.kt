package uk.akane.accord.ui.adapters.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.ui.MainActivity

class SongAdapter(
    private val recyclerView: RecyclerView,
    private val fragment: Fragment,
    private val onContentLoaded: (() -> Unit)
) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

    private val list = mutableListOf<SongListItem>()
    private val songList = mutableListOf<MediaItem>()

    private val mainActivity
        get() = fragment.activity as MainActivity

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                (fragment.activity as MainActivity).reader.songListFlow.collectLatest { newList ->
                    submitList(newList)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_CONTROL -> R.layout.layout_master_control
            VIEW_TYPE_CATEGORY -> R.layout.adapter_category_header
            else -> R.layout.layout_song_item
        }
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        )
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is SongListItem.Control -> VIEW_TYPE_CONTROL
            is SongListItem.Header -> VIEW_TYPE_CATEGORY
            is SongListItem.Track -> VIEW_TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        when (val item = list[position]) {
            is SongListItem.Control -> {
            }
            is SongListItem.Header -> {
                holder.title?.text = item.title
                holder.subtitle?.visibility = View.GONE
                holder.cover?.visibility = View.GONE
            }
            is SongListItem.Track -> {
                holder.cover?.visibility = View.VISIBLE
                holder.subtitle?.visibility = View.VISIBLE
                holder.cover?.load(item.mediaItem.mediaMetadata.artworkUri) {
                    crossfade(true)
                    size(62.dp.px.toInt(), 62.dp.px.toInt())
                }
                holder.title?.text = item.mediaItem.mediaMetadata.title
                holder.subtitle?.text = item.mediaItem.mediaMetadata.artist

                holder.itemView.setOnClickListener {
                    val mediaController = mainActivity.getPlayer()
                    mediaController?.apply {
                        setMediaItems(
                            songList,
                            songList.indexOf(item.mediaItem),
                            C.TIME_UNSET
                        )
                        prepare()
                        play()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView? = view.findViewById(R.id.cover)
        val title: TextView? = view.findViewById(R.id.title)
        val subtitle: TextView? = view.findViewById(R.id.subtitle)
    }

    private fun submitList(newList: List<MediaItem>) {
        CoroutineScope(Dispatchers.Default).launch {
            val grouped = newList
                .groupBy { it.mediaMetadata.title?.firstOrNull()?.uppercaseChar() ?: '#' }
                .toSortedMap()

            val items = mutableListOf<SongListItem>()
            val newSongList = mutableListOf<MediaItem>()

            items.add(SongListItem.Control)
            for ((key, tracks) in grouped) {
                items.add(SongListItem.Header(key.toString()))
                items.addAll(tracks.map {
                    newSongList.add(it)
                    SongListItem.Track(it)
                })
            }

            val diffResult = DiffUtil.calculateDiff(GenreDiffCallback(list, items))

            withContext(Dispatchers.Main) {
                list.clear()
                list.addAll(items)

                songList.clear()
                songList.addAll(newSongList)

                diffResult.dispatchUpdatesTo(this@SongAdapter)
                recyclerView.post { onContentLoaded.invoke() }
            }
        }
    }

    class GenreDiffCallback(
        private val oldList: List<SongListItem>,
        private val newList: List<SongListItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            when {
                oldList[oldItemPosition] is SongListItem.Control &&
                        newList[newItemPosition] is SongListItem.Control -> true
                oldList[oldItemPosition] is SongListItem.Header &&
                        newList[newItemPosition] is SongListItem.Header ->
                    (oldList[oldItemPosition] as SongListItem.Header).title ==
                            (newList[newItemPosition] as SongListItem.Header).title
                oldList[oldItemPosition] is SongListItem.Track &&
                        newList[newItemPosition] is SongListItem.Track ->
                    (oldList[oldItemPosition] as SongListItem.Track).mediaItem.mediaId ==
                            (newList[newItemPosition] as SongListItem.Track).mediaItem.mediaId
                else -> false
            }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    sealed class SongListItem {
        object Control : SongListItem()
        data class Header(val title: String) : SongListItem()
        data class Track(val mediaItem: MediaItem) : SongListItem()
    }


    companion object {
        const val VIEW_TYPE_NORMAL = 0
        const val VIEW_TYPE_CONTROL = 1
        const val VIEW_TYPE_CATEGORY = 2
    }
}