package uk.akane.accord.ui.fragments.browse

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

        rootView.findViewById<View>(R.id.popback).setOnClickListener {
            activity.fragmentSwitcherView.popBackTopFragmentIfExists()
        }

        return rootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }

}