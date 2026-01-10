package uk.akane.accord.setupwizard.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import uk.akane.accord.R
import uk.akane.accord.logic.isEssentialPermissionGranted
import uk.akane.accord.logic.setCurrentItemInterpolated
import uk.akane.accord.setupwizard.adapters.SetupWizardViewPagerAdapter
import uk.akane.accord.ui.MainActivity
import uk.akane.cupertino.widget.utils.AnimationUtils

class SetupWizardFragment : Fragment() {

    private lateinit var viewPager2: ViewPager2
    private lateinit var viewPagerAdapter: SetupWizardViewPagerAdapter
    private lateinit var continueButton: MaterialButton

    private var inactiveBtnColor = 0
    private var activeBtnColor = 0

    private var onInactiveBtnColor = 0
    private var onActiveBtnColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_setup_wizard, container, false)

        inactiveBtnColor = requireContext().getColor(R.color.accentColorFainted)
        activeBtnColor = requireContext().getColor(R.color.accentColor)

        onInactiveBtnColor = requireContext().getColor(R.color.onAccentColorFainted)
        onActiveBtnColor = requireContext().getColor(R.color.onAccentColor)

        viewPager2 = rootView.findViewById(R.id.sw_viewpager)
        continueButton = rootView.findViewById(R.id.continue_btn)
        viewPagerAdapter = SetupWizardViewPagerAdapter(childFragmentManager, lifecycle)

        viewPager2.adapter = viewPagerAdapter
        viewPager2.isUserInputEnabled = false
        viewPager2.offscreenPageLimit = 9999

        continueButton.setOnClickListener {
            if (viewPager2.currentItem + 1 < viewPagerAdapter.itemCount) {
                if (viewPager2.currentItem + 1 == 1 && !requireContext().isEssentialPermissionGranted()) {
                    continueButton.isEnabled = false
                    AnimationUtils.createValAnimator(
                        activeBtnColor,
                        inactiveBtnColor,
                        isArgb = true
                    ) {
                        continueButton.backgroundTintList = ColorStateList.valueOf(it)
                    }
                    AnimationUtils.createValAnimator(
                        onActiveBtnColor,
                        onInactiveBtnColor,
                        isArgb = true
                    ) {
                        continueButton.setTextColor(it)
                    }
                }
                viewPager2.setCurrentItemInterpolated(viewPager2.currentItem + 1)
            } else {
                val mainActivity = requireActivity() as? MainActivity
                mainActivity?.updateLibrary() 
                mainActivity?.removeContainer()
            }
        }

        return rootView
    }

    fun releaseContinueButton() {
        AnimationUtils.createValAnimator(
            inactiveBtnColor,
            activeBtnColor,
            isArgb = true,
            doOnEnd = {
                continueButton.isEnabled = true
            }
        ) {
            continueButton.backgroundTintList = ColorStateList.valueOf(it)
        }
        AnimationUtils.createValAnimator(
            onInactiveBtnColor,
            onActiveBtnColor,
            isArgb = true
        ) {
            continueButton.setTextColor(it)
        }
    }
}