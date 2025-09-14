package uk.akane.accord.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.AppBarLayout
import uk.akane.accord.R
import uk.akane.accord.logic.applyOffsetListener
import uk.akane.accord.logic.enableEdgeToEdgeListener
import uk.akane.accord.logic.enableEdgeToEdgePaddingListener
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.accord.ui.viewmodels.AccordViewModel

class HomeFragment: Fragment() {
    private val accordViewModel: AccordViewModel by activityViewModels()
    private lateinit var navigationBar: NavigationBar
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
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