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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.adapters.browse.AlbumAdapter
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.navigation.SwitcherPostponeFragment

class AlbumsFragment : SwitcherPostponeFragment() {

    private val activity get() = requireActivity() as MainActivity

    private lateinit var recyclerView: RecyclerView
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var layoutManager: GridLayoutManager
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
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_browse_albums, container, false)

        navigationBar = rootView.findViewById(R.id.navigation_bar)
        searchInput = navigationBar.findViewById(R.id.search_input)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        recyclerView = rootView.findViewById(R.id.rv)

        layoutManager = GridLayoutManager(requireContext(), 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = albumAdapter.getItemViewType(position)
                return if (viewType == AlbumAdapter.VIEW_TYPE_CONTROL) 2 else 1
            }
        }

        albumAdapter = AlbumAdapter(recyclerView, this) { notifyContentLoaded() }

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = albumAdapter

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {

            val outerPx = dpToPx(22)   // same as button start/end margins
            val innerPx = dpToPx(8)    // same as button inner margins -> 16dp total gap

            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) return

                // Don't touch the master control row (it already has the padding via margins)
                if (albumAdapter.getItemViewType(position) == AlbumAdapter.VIEW_TYPE_CONTROL) {
                    outRect.set(0, 0, 0, 0)
                    return
                }

                val lp = view.layoutParams as GridLayoutManager.LayoutParams
                val column = lp.spanIndex // 0 or 1

                // Outer edges = 24dp. Inner edges = 8dp each => 16dp between columns.
                outRect.left = if (column == 0) outerPx else innerPx
                outRect.right = if (column == 0) innerPx else outerPx

                // Vertical spacing between rows
                outRect.top = dpToPx(12)
                outRect.bottom = dpToPx(4)
            }

            private fun dpToPx(dp: Int): Int {
                val density = recyclerView.resources.displayMetrics.density
                return (dp * density).toInt()
            }
        })

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
                val album = item.mediaMetadata.albumTitle?.toString() ?: ""
                val artist = item.mediaMetadata.artist?.toString() ?: ""
                val title = item.mediaMetadata.title?.toString() ?: ""
                
                album.contains(trimmedQuery, ignoreCase = true) ||
                artist.contains(trimmedQuery, ignoreCase = true) ||
                title.contains(trimmedQuery, ignoreCase = true)
            }
        }
        
        albumAdapter.submitFromSongs(filteredList)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }
}
