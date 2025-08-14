package uk.akane.accord.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.AppBarLayout
import uk.akane.accord.R
import uk.akane.accord.logic.applyOffsetListener
import uk.akane.accord.logic.enableEdgeToEdgeListener
import uk.akane.accord.logic.enableEdgeToEdgePaddingListener
import uk.akane.accord.ui.viewmodels.AccordViewModel

class HomeFragment: Fragment() {
    private val accordViewModel: AccordViewModel by activityViewModels()

    private lateinit var collapsedTitleTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        collapsedTitleTextView = rootView.findViewById(R.id.collapsed_title)
        collapsedTitleTextView.enableEdgeToEdgeListener(top = true)

        return rootView
    }
}