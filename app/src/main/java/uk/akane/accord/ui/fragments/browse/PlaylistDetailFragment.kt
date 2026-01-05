package uk.akane.accord.ui.fragments.browse

import android.content.ContentValues
import android.app.Activity
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.getFile
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.adapters.browse.PlaylistAdapter
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.navigation.SwitcherPostponeFragment
import uk.akane.libphonograph.dynamicitem.Favorite
import uk.akane.libphonograph.items.Playlist
import uk.akane.libphonograph.manipulator.ItemManipulator
import uk.akane.libphonograph.manipulator.PlaylistSerializer
import java.io.File
import kotlin.random.Random

class PlaylistDetailFragment : SwitcherPostponeFragment() {

    private val activity
        get() = requireActivity() as MainActivity

    private lateinit var navigationBar: NavigationBar
    private lateinit var contentRecycler: RecyclerView
    private var didLoadOnce = false
    private var shouldRefreshSuggestions = true
    private var currentPlaylist: Playlist? = null
    private var targetPlaylistId: Long = NO_ID
    private var headerTitle = ""
    private var headerSubtitle = ""
    private var allSongs: List<MediaItem> = emptyList()
    private var suggestedSongs: List<MediaItem> = emptyList()
    private var playlistSongs: MutableList<MediaItem> = mutableListOf()
    private var isFavoriteTarget = false
    private var pendingFavoriteSong: MediaItem? = null
    private val suggestedAdapter = SuggestedSongAdapter { song ->
        addSuggestedToPlaylist(song)
    }
    private val playlistSongsAdapter = PlaylistSongAdapter()
    private val headerAdapter = HeaderAdapter()
    private val footerAdapter = FooterAdapter()
    private lateinit var concatAdapter: ConcatAdapter
    private val favoriteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingFavoriteSong?.let { updateAfterSuggestionAdded(it) }
            activity.updateLibrary()
        }
        pendingFavoriteSong = null
    }

    init {
        postponeSwitcherAnimation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_browse_playlist, container, false)

        navigationBar = rootView.findViewById(R.id.navigation_bar)
        contentRecycler = rootView.findViewById(R.id.rvContent)
        targetPlaylistId = requireArguments().getLong(ARG_ID, NO_ID)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        navigationBar.setOnReturnClickListener {
            activity.fragmentSwitcherView.popBackTopFragmentIfExists()
        }
        navigationBar.attach(contentRecycler, applyTopPadding = false)

        concatAdapter = ConcatAdapter(headerAdapter, playlistSongsAdapter, footerAdapter)
        contentRecycler.layoutManager = LinearLayoutManager(requireContext())
        contentRecycler.adapter = concatAdapter

        val fallbackTitle = requireArguments().getString(ARG_TITLE).orEmpty()
        headerTitle = fallbackTitle
        headerSubtitle = getString(R.string.library_head_playlist)
        isFavoriteTarget =
            targetPlaylistId == NO_ID && headerTitle == PlaylistAdapter.FAVORITE_PLAYLIST_TITLE
        val initialCount = requireArguments().getInt(ARG_COUNT, 0)
        headerAdapter.update(
            headerTitle,
            headerSubtitle,
            resources.getString(R.string.artist_song_count, initialCount)
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                activity.reader.playlistListFlow.collectLatest { playlists ->
                    resolvePlaylist(playlists)?.let { updateFromPlaylist(it) }
                    if (!didLoadOnce) {
                        didLoadOnce = true
                        notifyContentLoaded()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                activity.reader.songListFlow.collectLatest { songs ->
                    allSongs = songs
                    if (isFavoriteTarget) {
                        updateFromFavoriteSongs(songs)
                    }
                    refreshSuggestions()
                }
            }
        }

        return rootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }

    private fun resolvePlaylist(playlists: List<Playlist>): Playlist? {
        val targetId = requireArguments().getLong(ARG_ID, NO_ID)
        val targetTitle = requireArguments().getString(ARG_TITLE).orEmpty()
        val wantsFavorite = targetId == NO_ID && targetTitle == PlaylistAdapter.FAVORITE_PLAYLIST_TITLE
        return playlists.firstOrNull { playlist ->
            (targetId != NO_ID && playlist.id == targetId) ||
                (targetId == NO_ID && playlist.title == targetTitle) ||
                (wantsFavorite && playlist is Favorite)
        }
    }

    private fun updateFromPlaylist(playlist: Playlist) {
        currentPlaylist = playlist
        val title = playlist.title?.takeIf { it.isNotBlank() }
            ?: requireArguments().getString(ARG_TITLE).orEmpty()
        headerTitle = title
        applyPlaylistSongs(playlist.songList)
    }

    private fun updateFromFavoriteSongs(songs: List<MediaItem>) {
        val favoriteSongs = songs.filter {
            (it.mediaMetadata.userRating as? HeartRating)?.isHeart == true
        }
        applyPlaylistSongs(favoriteSongs)
    }

    private fun applyPlaylistSongs(songs: List<MediaItem>) {
        playlistSongs = mergePlaylistSongs(songs)
        playlistSongsAdapter.submitList(playlistSongs)
        headerAdapter.update(
            headerTitle,
            headerSubtitle,
            resources.getString(R.string.artist_song_count, playlistSongs.size)
        )
        if (suggestedSongs.isNotEmpty()) {
            val playlistKeys = playlistSongs.map { buildSongKey(it) }.toSet()
            val filtered = suggestedSongs.filter { buildSongKey(it) !in playlistKeys }
            if (filtered.size != suggestedSongs.size) {
                suggestedSongs = filtered
                suggestedAdapter.submitList(filtered)
            }
        }
        if (suggestedSongs.isEmpty() || shouldRefreshSuggestions) {
            refreshSuggestions(force = true)
        }
    }

    private fun refreshSuggestions(force: Boolean = false) {
        if (!force && !shouldRefreshSuggestions && suggestedSongs.isNotEmpty()) return
        if (allSongs.isEmpty()) {
            suggestedSongs = emptyList()
            suggestedAdapter.submitList(emptyList())
            shouldRefreshSuggestions = false
            return
        }

        val playlistKeys = playlistSongs
            .map { buildSongKey(it) }
            .toSet()
        val candidates = allSongs.filter { buildSongKey(it) !in playlistKeys }
        val newSuggestions = candidates
            .shuffled(Random(System.currentTimeMillis()))
            .take(SUGGESTED_COUNT)

        suggestedSongs = newSuggestions
        suggestedAdapter.submitList(newSuggestions)
        shouldRefreshSuggestions = false
    }

    private fun addSuggestedToPlaylist(song: MediaItem) {
        val songKey = buildSongKey(song)
        if (playlistSongs.any { buildSongKey(it) == songKey }) return
        if (currentPlaylist is Favorite || isFavoriteTarget) {
            addSuggestedToFavorites(song)
            return
        }

        val playlist = currentPlaylist
        val rawPlaylistId = playlist?.id ?: targetPlaylistId.takeIf { it != NO_ID }
        val playlistId = rawPlaylistId?.takeIf { it >= 0 }
        val playlistPath = playlist?.path ?: playlistId?.let { lookupPlaylistPath(it) }
        if (playlistId == null && playlistPath == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            val added = withContext(Dispatchers.IO) {
                addSongToPlaylist(playlistId, playlistPath, song)
            }
            if (!added) return@launch
            updateAfterSuggestionAdded(song)
            activity.updateLibrary()
        }
    }

    private fun addSuggestedToFavorites(song: MediaItem) {
        val songUri = buildFavoriteUri(song) ?: return
        val intentSender = ItemManipulator.setFavorite(
            requireContext(),
            setOf(songUri),
            true
        )
        if (intentSender != null) {
            pendingFavoriteSong = song
            favoriteRequestLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
            return
        }
        updateAfterSuggestionAdded(song)
        activity.updateLibrary()
    }

    private fun addSongToPlaylist(
        playlistId: Long?,
        playlistPath: File?,
        song: MediaItem
    ): Boolean {
        val outFile = playlistPath?.takeIf { it.exists() }
        if (outFile != null) {
            if (PlaylistSerializer.isAppPrivatePlaylist(requireContext(), outFile)) {
                val mediaId = parseMediaStoreId(song) ?: return false
                return addToPrivatePlaylist(outFile, mediaId)
            }
            val songFile = song.getFile() ?: return false
            return runCatching {
                ItemManipulator.addToPlaylist(requireContext(), outFile, listOf(songFile))
                true
            }.getOrDefault(false)
        }

        val resolvedPlaylistId = playlistId ?: return false
        val audioId = parseMediaStoreId(song) ?: return false
        val resolver = requireContext().contentResolver
        val membersUri = MediaStore.Audio.Playlists.Members.getContentUri("external", resolvedPlaylistId)
        val playOrder = queryNextPlayOrder(resolver, membersUri, playlistSongs.size)

        val values = ContentValues().apply {
            put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId)
            put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, playOrder)
        }
        return runCatching {
            resolver.insert(membersUri, values) != null
        }.getOrDefault(false)
    }

    private fun addToPrivatePlaylist(outFile: File, mediaId: Long): Boolean {
        return runCatching {
            val existing = PlaylistSerializer.readPrivatePlaylistEntries(outFile)
            val entry = "${PlaylistSerializer.ACCORD_ID_PREFIX}$mediaId"
            if (existing.contains(entry)) return@runCatching true
            PlaylistSerializer.writePrivatePlaylistEntries(outFile, existing + entry)
            true
        }.getOrDefault(false)
    }

    private fun lookupPlaylistPath(playlistId: Long): File? {
        if (playlistId == NO_ID) return null
        val resolver = requireContext().contentResolver
        resolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Playlists.DATA),
            "${MediaStore.Audio.Playlists._ID} = ?",
            arrayOf(playlistId.toString()),
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val index = cursor.getColumnIndex(MediaStore.Audio.Playlists.DATA)
            if (index < 0) return null
            val path = cursor.getString(index)?.trim().orEmpty()
            if (path.isBlank()) return null
            return File(path)
        }
        return null
    }

    private fun buildFavoriteUri(song: MediaItem): Uri? {
        val mediaId = parseMediaStoreId(song)
        if (mediaId != null) {
            return ContentUris.withAppendedId(
                MediaStore.Audio.Media.getContentUri("external"),
                mediaId
            )
        }
        val uri = song.localConfiguration?.uri
        return if (uri != null && uri.scheme == "content") uri else null
    }

    private fun updateAfterSuggestionAdded(song: MediaItem) {
        val songKey = buildSongKey(song)
        if (playlistSongs.none { buildSongKey(it) == songKey }) {
            playlistSongs.add(song)
            playlistSongsAdapter.submitList(playlistSongs)
            headerAdapter.update(
                headerTitle,
                headerSubtitle,
                resources.getString(R.string.artist_song_count, playlistSongs.size)
            )
        }

        val updatedSuggestions = suggestedSongs
            .filter { buildSongKey(it) != songKey }
            .toMutableList()

        if (updatedSuggestions.size < SUGGESTED_COUNT && allSongs.isNotEmpty()) {
            val excludeKeys = (playlistSongs.map { buildSongKey(it) } +
                updatedSuggestions.map { buildSongKey(it) }).toSet()
            val replacement = allSongs
                .filter { buildSongKey(it) !in excludeKeys }
                .shuffled(Random(System.currentTimeMillis()))
                .firstOrNull()
            if (replacement != null) {
                updatedSuggestions.add(replacement)
            }
        }

        suggestedSongs = updatedSuggestions
        suggestedAdapter.submitList(updatedSuggestions)
        shouldRefreshSuggestions = false
    }

    private fun parseMediaStoreId(item: MediaItem): Long? {
        val mediaId = item.mediaId
        val prefix = "MediaStore:"
        if (mediaId.startsWith(prefix)) {
            return mediaId.removePrefix(prefix).toLongOrNull()
        }
        return null
    }

    private fun queryNextPlayOrder(
        resolver: android.content.ContentResolver,
        membersUri: Uri,
        fallback: Int
    ): Int {
        val column = MediaStore.Audio.Playlists.Members.PLAY_ORDER
        resolver.query(
            membersUri,
            arrayOf(column),
            null,
            null,
            "$column DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(column)
                if (index >= 0) {
                    return cursor.getInt(index) + 1
                }
            }
        }
        return fallback
    }

    private fun buildSongKey(item: MediaItem): String {
        val mediaId = item.mediaId
        if (mediaId.isNotBlank()) return mediaId
        return item.localConfiguration?.uri?.toString() ?: item.hashCode().toString()
    }

    private fun mergePlaylistSongs(fromLibrary: List<MediaItem>): MutableList<MediaItem> {
        val merged = fromLibrary.toMutableList()
        val mergedKeys = merged.map { buildSongKey(it) }.toMutableSet()
        playlistSongs.forEach { song ->
            val key = buildSongKey(song)
            if (mergedKeys.add(key)) {
                merged.add(song)
            }
        }
        return merged
    }

    private inner class SuggestedSongAdapter(
        private val onAddClicked: (MediaItem) -> Unit
    ) : RecyclerView.Adapter<SuggestedSongAdapter.ViewHolder>() {
        private val items = mutableListOf<MediaItem>()

        fun submitList(songs: List<MediaItem>) {
            items.clear()
            items.addAll(songs)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_suggested_song_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val artUri = item.mediaMetadata.artworkUri
            if (artUri != null) {
                holder.cover.load(artUri) {
                    crossfade(true)
                    size(120.dp.px.toInt(), 120.dp.px.toInt())
                }
            } else {
                holder.cover.setImageResource(R.drawable.default_cover)
            }
            holder.title.text = item.mediaMetadata.title?.toString().orEmpty()
            holder.subtitle.text = item.mediaMetadata.artist?.toString().orEmpty()
            holder.addButton.setOnClickListener { onAddClicked(item) }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cover: ImageView = view.findViewById(R.id.cover)
            val title: TextView = view.findViewById(R.id.title)
            val subtitle: TextView = view.findViewById(R.id.subtitle)
            val addButton: ImageButton = view.findViewById(R.id.btnAdd)
        }
    }

    private inner class HeaderAdapter : RecyclerView.Adapter<HeaderAdapter.ViewHolder>() {
        private var titleText = ""
        private var subtitleText = ""
        private var updatedText = ""

        fun update(title: String, subtitle: String, updated: String) {
            titleText = title
            subtitleText = subtitle
            updatedText = updated
            notifyItemChanged(0)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_playlist_detail_header, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.title.text = titleText
            holder.subtitle.text = subtitleText
            holder.updated.text = updatedText
        }

        override fun getItemCount(): Int = 1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
            val subtitle: TextView = view.findViewById(R.id.tvArtist)
            val updated: TextView = view.findViewById(R.id.tvUpdated)
        }
    }

    private inner class FooterAdapter : RecyclerView.Adapter<FooterAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_playlist_detail_footer, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

        override fun getItemCount(): Int = 1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val refreshButton: ImageButton = view.findViewById(R.id.btnRefresh)
            private val suggestedRecycler: RecyclerView = view.findViewById(R.id.rvSuggested)

            init {
                suggestedRecycler.layoutManager = LinearLayoutManager(view.context)
                suggestedRecycler.adapter = suggestedAdapter
                refreshButton.setOnClickListener {
                    shouldRefreshSuggestions = true
                    refreshSuggestions(force = true)
                }
            }
        }
    }

    private inner class PlaylistSongAdapter :
        RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder>() {
        private val items = mutableListOf<MediaItem>()

        fun submitList(songs: List<MediaItem>) {
            items.clear()
            items.addAll(songs)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_song_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val artUri = item.mediaMetadata.artworkUri
            if (artUri != null) {
                holder.cover.load(artUri) {
                    crossfade(true)
                    size(120.dp.px.toInt(), 120.dp.px.toInt())
                }
            } else {
                holder.cover.setImageResource(R.drawable.default_cover)
            }
            holder.title.text = item.mediaMetadata.title?.toString().orEmpty()
            holder.subtitle.text = item.mediaMetadata.artist?.toString().orEmpty()
            holder.divider.visibility = if (position == items.lastIndex) View.GONE else View.VISIBLE

            holder.itemView.setOnClickListener {
                val mediaController = activity.getPlayer() ?: return@setOnClickListener
                if (items.isEmpty()) return@setOnClickListener
                mediaController.setMediaItems(items, position, C.TIME_UNSET)
                mediaController.prepare()
                mediaController.play()
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cover: ImageView = view.findViewById(R.id.cover)
            val title: TextView = view.findViewById(R.id.title)
            val subtitle: TextView = view.findViewById(R.id.subtitle)
            val divider: View = view.findViewById(R.id.divider)
        }
    }

    companion object {
        private const val ARG_ID = "playlist_id"
        private const val ARG_TITLE = "playlist_title"
        private const val ARG_COUNT = "playlist_count"
        private const val NO_ID = -1L
        private const val SUGGESTED_COUNT = 5

        fun newInstance(playlistId: Long?, title: String, songCount: Int): PlaylistDetailFragment {
            return PlaylistDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, playlistId ?: NO_ID)
                    putString(ARG_TITLE, title)
                    putInt(ARG_COUNT, songCount)
                }
            }
        }
    }
}
