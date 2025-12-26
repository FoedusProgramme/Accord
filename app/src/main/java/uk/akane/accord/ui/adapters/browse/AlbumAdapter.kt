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
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class AlbumAdapter(
    private val recyclerView: RecyclerView,
    private val fragment: Fragment,
    private val onContentLoaded: (() -> Unit)
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    private val list = mutableListOf<AlbumListItem>()

    private val mainActivity
        get() = fragment.activity as MainActivity

    private var latestSongList: List<MediaItem> = emptyList()

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainActivity.reader.songListFlow.collectLatest { songs ->
                    latestSongList = songs
                    submitFromSongs(songs)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_CONTROL -> R.layout.layout_master_control
            else -> R.layout.layout_album_item
        }
        return ViewHolder(LayoutInflater.from(parent.context).inflate(layoutId, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is AlbumListItem.Control -> VIEW_TYPE_CONTROL
            is AlbumListItem.Album -> VIEW_TYPE_ALBUM
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = list[position]) {
            is AlbumListItem.Control -> bindControl(holder)
            is AlbumListItem.Album -> bindAlbum(holder, item)
        }
    }

    override fun getItemCount(): Int = list.size

    private fun bindControl(holder: ViewHolder) {
        val playAll = holder.itemView.findViewById<MaterialButton?>(R.id.play_all)
        val shuffleAll = holder.itemView.findViewById<MaterialButton?>(R.id.shuffle_all)

        playAll?.setOnClickListener {
            val mediaController = mainActivity.getPlayer() ?: return@setOnClickListener
            if (latestSongList.isEmpty()) return@setOnClickListener

            mediaController.setMediaItems(latestSongList, /* startIndex */ 0, C.TIME_UNSET)
            mediaController.prepare()
            mediaController.play()
        }

        shuffleAll?.setOnClickListener {
            val mediaController = mainActivity.getPlayer() ?: return@setOnClickListener
            if (latestSongList.isEmpty()) return@setOnClickListener

            val shuffled = latestSongList.shuffled(Random(System.currentTimeMillis()))
            mediaController.setMediaItems(shuffled, /* startIndex */ 0, C.TIME_UNSET)
            mediaController.prepare()
            mediaController.play()
        }
    }

    private fun bindAlbum(holder: ViewHolder, item: AlbumListItem.Album) {
        holder.cover?.load(item.artworkUri) {
            crossfade(true)
            size(260.dp.px.toInt(), 260.dp.px.toInt())
        }
        holder.title?.text = item.title
        holder.subtitle?.text = item.artist

        holder.itemView.setOnClickListener {
            val mediaController = mainActivity.getPlayer() ?: return@setOnClickListener
            if (item.tracks.isEmpty()) return@setOnClickListener

            mediaController.setMediaItems(item.tracks, 0, C.TIME_UNSET)
            mediaController.prepare()
            mediaController.play()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView? = view.findViewById(R.id.cover)
        val title: TextView? = view.findViewById(R.id.title)
        val subtitle: TextView? = view.findViewById(R.id.subtitle)
    }

    private fun submitFromSongs(songs: List<MediaItem>) {
        CoroutineScope(Dispatchers.Default).launch {
            // Build album groups from the song list
            val albumMap = LinkedHashMap<String, MutableList<MediaItem>>()

            for (song in songs) {
                val albumTitle = song.mediaMetadata.albumTitle?.toString()?.trim().orEmpty()
                val artist = song.mediaMetadata.artist?.toString()?.trim().orEmpty()

                // Fallbacks so grouping is stable
                val safeAlbum = albumTitle.ifEmpty { "(Unknown Album)" }
                val safeArtist = artist.ifEmpty { "(Unknown Artist)" }

                val key = "$safeAlbum\u0000$safeArtist"
                albumMap.getOrPut(key) { mutableListOf() }.add(song)
            }

            val albums = albumMap.entries
                .map { (key, tracks) ->
                    val parts = key.split("\u0000")
                    val title = parts.getOrNull(0).orEmpty()
                    val artist = parts.getOrNull(1).orEmpty()
                    val artwork = tracks.firstOrNull()?.mediaMetadata?.artworkUri
                    AlbumListItem.Album(
                        title = title,
                        artist = artist,
                        artworkUri = artwork,
                        tracks = tracks.toList()
                    )
                }
                .sortedBy { it.title.lowercase() }

            val newItems = mutableListOf<AlbumListItem>()
            newItems.add(AlbumListItem.Control)
            newItems.addAll(albums)

            val diff = DiffUtil.calculateDiff(AlbumDiffCallback(list, newItems))

            withContext(Dispatchers.Main) {
                list.clear()
                list.addAll(newItems)
                diff.dispatchUpdatesTo(this@AlbumAdapter)
                recyclerView.post { onContentLoaded.invoke() }
            }
        }
    }

    class AlbumDiffCallback(
        private val oldList: List<AlbumListItem>,
        private val newList: List<AlbumListItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return when {
                old is AlbumListItem.Control && new is AlbumListItem.Control -> true
                old is AlbumListItem.Album && new is AlbumListItem.Album ->
                    old.title == new.title && old.artist == new.artist
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    sealed class AlbumListItem {
        object Control : AlbumListItem()
        data class Album(
            val title: String,
            val artist: String,
            val artworkUri: android.net.Uri?,
            val tracks: List<MediaItem>
        ) : AlbumListItem()
    }

    companion object {
        const val VIEW_TYPE_ALBUM = 0
        const val VIEW_TYPE_CONTROL = 1
    }
}
