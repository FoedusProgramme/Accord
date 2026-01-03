package uk.akane.accord.ui.fragments.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.navigation.SwitcherPostponeFragment
import uk.akane.libphonograph.items.addDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArtistDetailFragment : SwitcherPostponeFragment() {

    private val activity
        get() = requireActivity() as MainActivity

    private lateinit var headerArt: ImageView
    private lateinit var artistNameView: TextView
    private lateinit var playButton: FloatingActionButton
    private lateinit var latestCard: View
    private lateinit var latestCover: ImageView
    private lateinit var latestDateView: TextView
    private lateinit var latestTitleView: TextView
    private lateinit var latestCountView: TextView
    private lateinit var latestAddButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollView: NestedScrollView
    private lateinit var headerContainer: View
    private lateinit var navigationBar: NavigationBar

    private val songAdapter = ArtistSongAdapter()
    private var currentTracks: List<MediaItem> = emptyList()
    private var latestAlbumTracks: List<MediaItem> = emptyList()
    private var didLoadOnce = false

    init {
        postponeSwitcherAnimation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_browse_artist, container, false)

        headerArt = rootView.findViewById(R.id.ivHeaderArt)
        artistNameView = rootView.findViewById(R.id.tvArtistName)
        playButton = rootView.findViewById(R.id.btnPlay)
        latestCard = rootView.findViewById(R.id.latestCard)
        latestCover = rootView.findViewById(R.id.ivLatestCover)
        latestDateView = rootView.findViewById(R.id.tvLatestDate)
        latestTitleView = rootView.findViewById(R.id.tvLatestTitle)
        latestCountView = rootView.findViewById(R.id.tvLatestCount)
        latestAddButton = rootView.findViewById(R.id.btnLatestAdd)
        recyclerView = rootView.findViewById(R.id.rvSongs)
        scrollView = rootView.findViewById(R.id.scrollContainer)
        headerContainer = rootView.findViewById(R.id.headerContainer)

        navigationBar = rootView.findViewById(R.id.navigation_bar)
        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        navigationBar.setOnReturnClickListener {
            activity.fragmentSwitcherView.popBackTopFragmentIfExists()
        }

        val artistName = requireArguments().getString(ARG_ARTIST).orEmpty()
        artistNameView.text = artistName
        navigationBar.setTitle(artistName)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = songAdapter
        navigationBar.attach(scrollView, applyTopPadding = false)

        val headerHeight = (resources.displayMetrics.heightPixels * 0.6f).toInt()
        val headerParams = headerContainer.layoutParams
        if (headerParams.height != headerHeight) {
            headerParams.height = headerHeight
            headerContainer.layoutParams = headerParams
        }
        navigationBar.doOnLayout {
            navigationBar.setCollapseStartOffsetPx((headerHeight - navigationBar.height).coerceAtLeast(0))
        }

        playButton.setOnClickListener {
            val mediaController = activity.getPlayer() ?: return@setOnClickListener
            if (currentTracks.isEmpty()) return@setOnClickListener
            mediaController.setMediaItems(currentTracks, 0, C.TIME_UNSET)
            mediaController.prepare()
            mediaController.play()
        }

        latestAddButton.setOnClickListener {
            val mediaController = activity.getPlayer() ?: return@setOnClickListener
            if (latestAlbumTracks.isEmpty()) return@setOnClickListener
            mediaController.addMediaItems(latestAlbumTracks)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                activity.reader.songListFlow.collectLatest { songs ->
                    val filtered = songs.filter { song ->
                        safeArtist(song.mediaMetadata.artist?.toString()) == artistName
                    }
                    val sorted = filtered.sortedWith(
                        compareByDescending<MediaItem> { it.mediaMetadata.addDate ?: 0L }
                            .thenBy { it.mediaMetadata.title?.toString().orEmpty() }
                    )
                    updateArtistDetails(sorted)
                }
            }
        }

        return rootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }

    private fun updateArtistDetails(tracks: List<MediaItem>) {
        currentTracks = tracks
        songAdapter.submitList(tracks)

        val latest = tracks.firstOrNull()
        if (latest == null) {
            latestCard.visibility = View.GONE
            headerArt.setImageResource(R.drawable.default_cover)
            if (!didLoadOnce) {
                didLoadOnce = true
                notifyContentLoaded()
            }
            return
        }

        latestCard.visibility = View.VISIBLE
        val latestArt = latest.mediaMetadata.artworkUri
        if (latestArt != null) {
            headerArt.load(latestArt) { crossfade(true) }
            latestCover.load(latestArt) {
                crossfade(true)
                size(140.dp.px.toInt(), 140.dp.px.toInt())
            }
        } else {
            headerArt.setImageResource(R.drawable.default_cover)
            latestCover.setImageResource(R.drawable.default_cover)
        }

        val albumTitle = safeAlbum(latest.mediaMetadata.albumTitle?.toString())
        latestTitleView.text = albumTitle
        latestAlbumTracks = tracks.filter { track ->
            safeAlbum(track.mediaMetadata.albumTitle?.toString()) == albumTitle
        }
        latestCountView.text = resources.getString(
            R.string.artist_song_count,
            latestAlbumTracks.size
        )

        val formattedDate = formatAddDate(latest.mediaMetadata.addDate)
        if (formattedDate.isNullOrBlank()) {
            latestDateView.visibility = View.GONE
        } else {
            latestDateView.visibility = View.VISIBLE
            latestDateView.text = formattedDate
        }

        if (!didLoadOnce) {
            didLoadOnce = true
            notifyContentLoaded()
        }
    }

    private fun formatAddDate(addDate: Long?): String? {
        if (addDate == null || addDate <= 0L) return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(addDate * 1000))
    }

    private fun safeAlbum(value: String?): String =
        value?.trim().orEmpty().ifEmpty { "(Unknown Album)" }

    private fun safeArtist(value: String?): String =
        value?.trim().orEmpty().ifEmpty { "(Unknown Artist)" }

    private inner class ArtistSongAdapter :
        RecyclerView.Adapter<ArtistSongAdapter.ViewHolder>() {
        private val items = mutableListOf<MediaItem>()

        fun submitList(tracks: List<MediaItem>) {
            items.clear()
            items.addAll(tracks)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_artist_song_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.cover?.load(item.mediaMetadata.artworkUri) {
                crossfade(true)
                size(62.dp.px.toInt(), 62.dp.px.toInt())
            }
            holder.title?.text = item.mediaMetadata.title?.toString().orEmpty()
            val album = safeAlbum(item.mediaMetadata.albumTitle?.toString())
            holder.subtitle?.text = album

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
            val cover: ImageView? = view.findViewById(R.id.cover)
            val title: TextView? = view.findViewById(R.id.title)
            val subtitle: TextView? = view.findViewById(R.id.subtitle)
        }
    }

    companion object {
        private const val ARG_ARTIST = "artist_name"

        fun newInstance(artist: String): ArtistDetailFragment {
            return ArtistDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTIST, artist)
                }
            }
        }
    }
}
