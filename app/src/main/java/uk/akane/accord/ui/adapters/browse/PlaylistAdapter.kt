package uk.akane.accord.ui.adapters.browse

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.fragments.browse.PlaylistDetailFragment
import uk.akane.libphonograph.dynamicitem.Favorite
import uk.akane.libphonograph.dynamicitem.RecentlyAdded
import uk.akane.libphonograph.items.Playlist
import com.google.android.material.card.MaterialCardView

class PlaylistAdapter(
    private val recyclerView: RecyclerView,
    private val fragment: Fragment,
    private val onContentLoaded: (() -> Unit)
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    private val list = mutableListOf<PlaylistRow>()

    private val mainActivity
        get() = fragment.activity as MainActivity

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainActivity.reader.playlistListFlow
                    .combine(mainActivity.reader.songListFlow) { playlists, songs ->
                        playlists to songs
                    }
                    .collectLatest { (playlists, songs) ->
                        submitPlaylists(playlists, songs)
                    }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_playlist_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context

        holder.title.text = item.title
        holder.favoriteMarker.visibility = if (item.isFavorite) View.VISIBLE else View.INVISIBLE
        holder.divider.visibility = if (position == list.lastIndex) View.GONE else View.VISIBLE

        if (item.iconRes != null) {
            holder.cover.scaleType = ImageView.ScaleType.CENTER
            holder.cover.setImageResource(item.iconRes)
            holder.cover.setColorFilter(ContextCompat.getColor(context, item.iconTintRes))
            holder.coverCard.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.searchBarBackground)
            )
        } else {
            holder.cover.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.cover.colorFilter = null
            holder.coverCard.setCardBackgroundColor(Color.TRANSPARENT)
            if (item.artworkUri != null) {
                holder.cover.load(item.artworkUri) {
                    crossfade(true)
                    size(160.dp.px.toInt(), 160.dp.px.toInt())
                }
            } else {
                holder.cover.setImageResource(R.drawable.default_cover)
            }
        }

        holder.itemView.setOnClickListener {
            mainActivity.fragmentSwitcherView.addFragmentToCurrentStack(
                PlaylistDetailFragment.newInstance(item.playlistId, item.title, item.songs.size)
            )
        }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val favoriteMarker: ImageView = view.findViewById(R.id.favorite_marker)
        val coverCard: MaterialCardView = view.findViewById(R.id.cover_card)
        val cover: ImageView = view.findViewById(R.id.cover)
        val title: TextView = view.findViewById(R.id.title)
        val divider: View = view.findViewById(R.id.divider)
    }

    private suspend fun submitPlaylists(playlists: List<Playlist>, songs: List<MediaItem>) {
        val oldList = list.toList()
        val coverPrefs = mainActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = withContext(Dispatchers.Default) {
            val favoriteFromPlaylists = playlists.firstOrNull { it is Favorite } as? Favorite
            val favoriteSongs = favoriteFromPlaylists?.songList
                ?: songs.filter { (it.mediaMetadata.userRating as? HeartRating)?.isHeart == true }

            val favoriteRow = PlaylistRow(
                key = "favorite",
                title = FAVORITE_PLAYLIST_TITLE,
                artworkUri = null,
                iconRes = uk.akane.cupertino.R.drawable.ic_star_filled,
                iconTintRes = R.color.accentColor,
                playlistId = favoriteFromPlaylists?.id,
                isFavorite = true,
                isRecentlyAdded = false,
                songs = favoriteSongs
            )

            val basePlaylists = playlists.filterNot { it is Favorite || it is RecentlyAdded }

            val items = basePlaylists.map { playlist ->
                val isFavorite = playlist is Favorite
                val isRecentlyAdded = playlist is RecentlyAdded
                val storedCoverUri = playlist.title
                    ?.takeIf { it.isNotBlank() }
                    ?.let { coverPrefs.getString(coverKey(it), null) }
                    ?.let { Uri.parse(it) }
                val hasCustomCover = storedCoverUri != null
                val title = when {
                    isFavorite -> FAVORITE_PLAYLIST_TITLE
                    isRecentlyAdded -> "New"
                    !playlist.title.isNullOrBlank() -> playlist.title!!
                    else -> "(Untitled Playlist)"
                }
                val iconRes = when {
                    isFavorite -> uk.akane.cupertino.R.drawable.ic_star_filled
                    isRecentlyAdded -> R.drawable.ic_playlist
                    playlist.songList.isEmpty() && !hasCustomCover -> R.drawable.ic_playlist
                    else -> null
                }
                val iconTintRes = when {
                    isFavorite -> R.color.accentColor
                    iconRes != null -> R.color.onSurfaceColorInactive
                    else -> R.color.onSurfaceColorInactive
                }
                val artworkUri = when {
                    hasCustomCover -> storedCoverUri
                    iconRes == null -> playlist.songList.firstOrNull()?.mediaMetadata?.artworkUri
                    else -> null
                }

                PlaylistRow(
                    key = buildKey(playlist, isFavorite, isRecentlyAdded, title),
                    title = title,
                    artworkUri = artworkUri,
                    iconRes = iconRes,
                    iconTintRes = iconTintRes,
                    playlistId = playlist.id,
                    isFavorite = isFavorite,
                    isRecentlyAdded = isRecentlyAdded,
                    songs = playlist.songList
                )
            }

            val regularItems = items.filter { !it.isFavorite && !it.isRecentlyAdded }
            val orderedItems = listOf(favoriteRow) + regularItems

            orderedItems to DiffUtil.calculateDiff(PlaylistDiffCallback(oldList, orderedItems))
        }

        list.clear()
        list.addAll(result.first)
        result.second.dispatchUpdatesTo(this@PlaylistAdapter)
        recyclerView.post { onContentLoaded.invoke() }
    }

    private fun coverKey(name: String): String = "$COVER_KEY_PREFIX$name"

    private fun buildKey(
        playlist: Playlist,
        isFavorite: Boolean,
        isRecentlyAdded: Boolean,
        title: String
    ): String {
        return when {
            playlist.id != null -> "id:${playlist.id}"
            isFavorite -> "favorite"
            isRecentlyAdded -> "recent"
            else -> "title:$title"
        }
    }

    class PlaylistDiffCallback(
        private val oldList: List<PlaylistRow>,
        private val newList: List<PlaylistRow>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].key == newList[newItemPosition].key
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    data class PlaylistRow(
        val key: String,
        val title: String,
        val artworkUri: android.net.Uri?,
        val iconRes: Int?,
        val iconTintRes: Int,
        val playlistId: Long?,
        val isFavorite: Boolean,
        val isRecentlyAdded: Boolean,
        val songs: List<MediaItem>
    )

    companion object {
        private const val PREFS_NAME = "playlist_covers"
        private const val COVER_KEY_PREFIX = "playlist_cover_"
        const val FAVORITE_PLAYLIST_TITLE = "Favourite Songs"
    }
}
