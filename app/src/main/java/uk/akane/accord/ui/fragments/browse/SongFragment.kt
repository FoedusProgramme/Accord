package uk.akane.accord.ui.fragments.browse

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.adapters.browse.SongAdapter
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.navigation.SwitcherPostponeFragment

class SongFragment : SwitcherPostponeFragment() {

    private val activity
        get() = requireActivity() as MainActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var navigationBar: NavigationBar
    
    private lateinit var searchInput: EditText
    private var allSongs: List<MediaItem> = emptyList()

    init {
        postponeSwitcherAnimation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_browse_song, container, false)

        navigationBar = rootView.findViewById(R.id.navigation_bar)
        searchInput = navigationBar.findViewById(R.id.search_input)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }

        recyclerView = rootView.findViewById(R.id.rv)
        songAdapter = SongAdapter(recyclerView, this) { notifyContentLoaded() }
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = songAdapter
        recyclerView.layoutManager = layoutManager

        navigationBar.attach(recyclerView)
        navigationBar.setOnReturnClickListener {
            activity.fragmentSwitcherView.popBackTopFragmentIfExists()
        }

        lifecycleScope.launch {
            activity.reader.songListFlow.collectLatest { songs ->
                allSongs = songs
                performSearch(searchInput.text.toString())
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch(s.toString())
            }
        })

        return rootView
    }

    private fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        
        val filteredList = if (trimmedQuery.isEmpty()) {
            allSongs
        } else {
            allSongs.filter { item ->
                val title = item.mediaMetadata.title?.toString() ?: ""
                val artist = item.mediaMetadata.artist?.toString() ?: ""
                val album = item.mediaMetadata.albumTitle?.toString() ?: ""
                
                title.contains(trimmedQuery, ignoreCase = true) ||
                artist.contains(trimmedQuery, ignoreCase = true) ||
                album.contains(trimmedQuery, ignoreCase = true)
            }
        }
        
        songAdapter.submitList(filteredList)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }

}
