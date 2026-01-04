package uk.akane.accord.ui.fragments.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.navigation.SwitcherPostponeFragment
import uk.akane.libphonograph.items.Playlist

class PlaylistDetailFragment : SwitcherPostponeFragment() {

    private val activity
        get() = requireActivity() as MainActivity

    private lateinit var navigationBar: NavigationBar
    private lateinit var scrollView: NestedScrollView
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var updatedView: TextView
    private var didLoadOnce = false

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
        scrollView = rootView.findViewById(R.id.scrollContainer)
        titleView = rootView.findViewById(R.id.tvTitle)
        subtitleView = rootView.findViewById(R.id.tvArtist)
        updatedView = rootView.findViewById(R.id.tvUpdated)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        navigationBar.setOnReturnClickListener {
            activity.fragmentSwitcherView.popBackTopFragmentIfExists()
        }
        navigationBar.attach(scrollView, applyTopPadding = false)

        val fallbackTitle = requireArguments().getString(ARG_TITLE).orEmpty()
        titleView.text = fallbackTitle
        subtitleView.setText(R.string.library_head_playlist)
        val initialCount = requireArguments().getInt(ARG_COUNT, 0)
        updatedView.text = resources.getString(R.string.artist_song_count, initialCount)

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

        return rootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }

    private fun resolvePlaylist(playlists: List<Playlist>): Playlist? {
        val targetId = requireArguments().getLong(ARG_ID, NO_ID)
        val targetTitle = requireArguments().getString(ARG_TITLE).orEmpty()
        return playlists.firstOrNull { playlist ->
            (targetId != NO_ID && playlist.id == targetId) ||
                (targetId == NO_ID && playlist.title == targetTitle)
        }
    }

    private fun updateFromPlaylist(playlist: Playlist) {
        val title = playlist.title?.takeIf { it.isNotBlank() }
            ?: requireArguments().getString(ARG_TITLE).orEmpty()
        titleView.text = title

        updatedView.text = resources.getString(
            R.string.artist_song_count,
            playlist.songList.size
        )

    }

    companion object {
        private const val ARG_ID = "playlist_id"
        private const val ARG_TITLE = "playlist_title"
        private const val ARG_COUNT = "playlist_count"
        private const val NO_ID = -1L

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
