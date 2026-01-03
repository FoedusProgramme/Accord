package uk.akane.accord.ui.fragments.browse

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.adapters.browse.ArtistAdapter
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.navigation.SwitcherPostponeFragment

class ArtistsFragment : SwitcherPostponeFragment() {

    private val activity get() = requireActivity() as MainActivity

    private lateinit var recyclerView: RecyclerView
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var navigationBar: NavigationBar

    init {
        postponeSwitcherAnimation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_browse_artists, container, false)

        navigationBar = rootView.findViewById(R.id.navigation_bar)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        recyclerView = rootView.findViewById(R.id.rv)
        layoutManager = LinearLayoutManager(requireContext())

        artistAdapter = ArtistAdapter(
            recyclerView = recyclerView,
            fragment = this
        ) {
            notifyContentLoaded()
        }

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = artistAdapter

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            // Side padding
            private val sidePx = dpToPx(24)

            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) return

                outRect.left = sidePx
                outRect.right = 0
            }

            private fun dpToPx(dp: Int): Int =
                (dp * recyclerView.resources.displayMetrics.density).toInt()
        })

        navigationBar.attach(recyclerView)
        navigationBar.setOnReturnClickListener {
            activity.fragmentSwitcherView.popBackTopFragmentIfExists()
        }

        return rootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }
}
