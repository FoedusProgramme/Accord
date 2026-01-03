package uk.akane.accord.ui.adapters.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import uk.akane.accord.ui.fragments.browse.ArtistDetailFragment

class ArtistAdapter(
    private val recyclerView: RecyclerView,
    private val fragment: Fragment,
    private val onContentLoaded: (() -> Unit)
) : RecyclerView.Adapter<ArtistAdapter.ViewHolder>() {

    private val list = mutableListOf<ArtistItem>()

    private val mainActivity
        get() = fragment.activity as MainActivity

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {
                mainActivity.reader.songListFlow.collectLatest { songs ->
                    submitFromSongs(songs)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_artist_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.artistImage.load(item.artworkUri) {
            crossfade(true)
            size(160.dp.px.toInt(), 160.dp.px.toInt())
        }

        holder.artistName.text = item.name

        holder.itemView.setOnClickListener {
            mainActivity.fragmentSwitcherView.addFragmentToCurrentStack(
                ArtistDetailFragment.newInstance(item.name)
            )
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artistImage: ImageView = view.findViewById(R.id.artistImage)
        val artistName: TextView = view.findViewById(R.id.artistName)
    }

    private fun submitFromSongs(songs: List<MediaItem>) {
        CoroutineScope(Dispatchers.Default).launch {
            val artistMap = LinkedHashMap<String, MutableList<MediaItem>>()

            for (song in songs) {
                val artist = song.mediaMetadata.artist?.toString()?.trim().orEmpty()
                val safeArtist = artist.ifEmpty { "(Unknown Artist)" }
                artistMap.getOrPut(safeArtist) { mutableListOf() }.add(song)
            }

            val newItems = artistMap.entries
                .map { (name, tracks) ->
                    ArtistItem(
                        name = name,
                        artworkUri = tracks.firstOrNull()?.mediaMetadata?.artworkUri,
                        tracks = tracks.toList()
                    )
                }
                .sortedBy { it.name.lowercase() }

            val diff = DiffUtil.calculateDiff(
                ArtistDiffCallback(list, newItems)
            )

            withContext(Dispatchers.Main) {
                list.clear()
                list.addAll(newItems)
                diff.dispatchUpdatesTo(this@ArtistAdapter)
                recyclerView.post { onContentLoaded.invoke() }
            }
        }
    }

    class ArtistDiffCallback(
        private val oldList: List<ArtistItem>,
        private val newList: List<ArtistItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos].name == newList[newPos].name

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos] == newList[newPos]
    }

    data class ArtistItem(
        val name: String,
        val artworkUri: android.net.Uri?,
        val tracks: List<MediaItem>
    )
}
