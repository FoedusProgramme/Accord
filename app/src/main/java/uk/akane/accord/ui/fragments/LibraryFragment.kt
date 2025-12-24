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
import uk.akane.accord.ui.adapters.LibraryHeadAdapter
import uk.akane.accord.ui.components.NavigationBar

class LibraryFragment: Fragment() {
    private lateinit var navigationBar: NavigationBar
    private lateinit var libraryRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library, container, false)

        navigationBar = rootView.findViewById(R.id.navigation_bar)
        libraryRecyclerView = rootView.findViewById(R.id.library_rv)

        libraryRecyclerView.layoutManager = LinearLayoutManager(context)
        libraryRecyclerView.adapter = LibraryHeadAdapter(requireContext())

        navigationBar.attach(libraryRecyclerView)

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