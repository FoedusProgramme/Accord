package uk.akane.accord.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import uk.akane.accord.R
import uk.akane.accord.ui.components.NavigationBar

class BrowseFragment: Fragment() {
    private lateinit var navigationBar: NavigationBar
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_browse, container, false)
        navigationBar = rootView.findViewById(R.id.navigation_bar)

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