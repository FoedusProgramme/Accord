package uk.akane.accord.setupwizard.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import uk.akane.accord.setupwizard.fragments.PermissionPageFragment
import uk.akane.accord.setupwizard.fragments.WelcomePageFragment

class SetupWizardViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> WelcomePageFragment()
            1 -> PermissionPageFragment()
            else -> throw IllegalArgumentException("Didn't find desired fragment!")
        }
}