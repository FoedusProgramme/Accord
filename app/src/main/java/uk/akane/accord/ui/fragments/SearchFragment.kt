package uk.akane.accord.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.adapters.SearchAdapter
import uk.akane.accord.ui.adapters.browse.SongAdapter
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.fadOutAnimation
import uk.akane.cupertino.widget.utils.AnimationUtils

class SearchFragment: Fragment() {
    private lateinit var navigationBar: NavigationBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var indicatorTitleTextView: TextView
    private lateinit var indicatorSubtitleTextView: TextView
    private lateinit var searchInput: EditText

    // Adapters
    private lateinit var genreAdapter: SearchAdapter
    private var songAdapter: SongAdapter? = null

    // Data
    private var allSongs: List<MediaItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_search, container, false)
        navigationBar = rootView.findViewById(R.id.navigation_bar)
        searchInput = navigationBar.findViewById(R.id.search_input)
        recyclerView = rootView.findViewById(R.id.rv)
        indicatorTitleTextView = rootView.findViewById(R.id.no_track_title)
        indicatorSubtitleTextView = rootView.findViewById(R.id.no_track_subtitle)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                systemBars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
            )
            insets
        }

        setupGenreMode()
        navigationBar.attach(recyclerView)

        lifecycleScope.launch {
            (requireActivity() as MainActivity).reader.songListFlow.collectLatest { songs ->
                allSongs = songs
                if (searchInput.text.isNotEmpty()) {
                    performSearch(searchInput.text.toString())
                }
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    setupGenreMode()
                } else {
                    performSearch(query)
                }
            }
        })

        return rootView
    }

    private fun setupGenreMode() {
        if (recyclerView.adapter !is SearchAdapter) {
            recyclerView.layoutManager = GridLayoutManager(context, 2)
            genreAdapter = SearchAdapter(requireContext(), this) {
                hideEmptyIndicators()
            }
            recyclerView.adapter = genreAdapter
        }
    }

    private fun performSearch(query: String) {
        val filteredList = allSongs.filter { item ->
            val title = item.mediaMetadata.title?.toString() ?: ""
            val artist = item.mediaMetadata.artist?.toString() ?: ""
            val album = item.mediaMetadata.albumTitle?.toString() ?: ""
            
            title.contains(query, ignoreCase = true) ||
            artist.contains(query, ignoreCase = true) ||
            album.contains(query, ignoreCase = true)
        }

        if (recyclerView.layoutManager is GridLayoutManager) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        }

        if (songAdapter == null) {
            songAdapter = SongAdapter(recyclerView, this) {}
        }
        
        if (recyclerView.adapter != songAdapter) {
            recyclerView.adapter = songAdapter
        }

        songAdapter?.submitList(filteredList)

        if (filteredList.isNotEmpty()) {
            hideEmptyIndicators()
        } else {
            indicatorTitleTextView.alpha = 1f
            indicatorSubtitleTextView.alpha = 1f
        }
    }

    private fun hideEmptyIndicators() {
        indicatorSubtitleTextView.fadOutAnimation(interpolator = AnimationUtils.easingStandardInterpolator)
        indicatorTitleTextView.fadOutAnimation(interpolator = AnimationUtils.easingStandardInterpolator)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }
}