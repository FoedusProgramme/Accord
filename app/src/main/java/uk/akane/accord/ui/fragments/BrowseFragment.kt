package uk.akane.accord.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.accord.ui.adapters.BrowseAdapter
import uk.akane.accord.ui.components.NavigationBar

class BrowseFragment: Fragment() {

    private lateinit var navigationBar: NavigationBar
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_browse, container, false)

        navigationBar = rootView.findViewById(R.id.navigation_bar)
        recyclerView = rootView.findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = BrowseAdapter(requireContext())

        navigationBar.attach(recyclerView)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            navigationBar.setPadding(
                navigationBar.paddingLeft,
                systemBars.top,
                navigationBar.paddingRight,
                navigationBar.paddingBottom
            )
            insets
        }
        return rootView
    }
}