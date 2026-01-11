package uk.akane.accord.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.ui.adapters.SearchAdapter
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.fadOutAnimation
import uk.akane.cupertino.widget.utils.AnimationUtils

class SearchFragment: Fragment() {
    private lateinit var navigationBar: NavigationBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var indicatorTitleTextView: TextView
    private lateinit var indicatorSubtitleTextView: TextView
    private lateinit var detailedSearchContainer: View
    private lateinit var searchBarNav: View
    private lateinit var searchBarDetail: View
    private lateinit var tabContainer: View
    private lateinit var tabIndicator: View
    private lateinit var tabAppleTextView: TextView
    private lateinit var tabLibraryTextView: TextView
    private lateinit var searchInputNav: EditText
    private lateinit var searchInputDetail: EditText

    private var indicatorTitleVisible = true
    private var indicatorSubtitleVisible = true
    private var isSearchExpanded = false
    private var isAppleTabSelected = true
    private var enterAnimator: AnimatorSet? = null
    private var exitAnimator: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_search, container, false)
        navigationBar = rootView.findViewById(R.id.navigation_bar)
        recyclerView = rootView.findViewById(R.id.rv)
        indicatorTitleTextView = rootView.findViewById(R.id.no_track_title)
        indicatorSubtitleTextView = rootView.findViewById(R.id.no_track_subtitle)
        detailedSearchContainer = rootView.findViewById(R.id.detailed_search_container)
        searchBarNav = rootView.findViewById(R.id.search_bar_nav)
        searchBarDetail = rootView.findViewById(R.id.search_bar_detail)
        tabContainer = rootView.findViewById(R.id.tab_container)
        tabIndicator = rootView.findViewById(R.id.tab_apple_music_container)
        tabAppleTextView = rootView.findViewById(R.id.tab_apple_music)
        tabLibraryTextView = rootView.findViewById(R.id.tab_library)
        searchInputNav = searchBarNav.findViewById(R.id.search_input)
        searchInputDetail = searchBarDetail.findViewById(R.id.search_input)

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                v.paddingBottom
            )
            detailedSearchContainer.setPadding(
                detailedSearchContainer.paddingLeft,
                systemBars.top,
                detailedSearchContainer.paddingRight,
                detailedSearchContainer.paddingBottom
            )
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                systemBars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
            )
            insets
        }

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = SearchAdapter(requireContext(), this) {
            indicatorSubtitleTextView.fadOutAnimation(interpolator = AnimationUtils.easingStandardInterpolator)
            indicatorTitleTextView.fadOutAnimation(interpolator = AnimationUtils.easingStandardInterpolator)
        }
        navigationBar.attach(recyclerView)

        tabAppleTextView.setOnClickListener { selectTab(true) }
        tabLibraryTextView.setOnClickListener { selectTab(false) }
        tabContainer.doOnLayout { updateTabIndicator(false) }
        updateTabSelection()

        detailedSearchContainer.visibility = View.GONE
        resetTabRevealState()

        searchBarNav.setOnClickListener { enterSearchMode() }
        searchInputNav.setOnClickListener { enterSearchMode() }
        searchInputNav.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) enterSearchMode()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isSearchExpanded) {
                        exitSearchMode()
                        return
                    }
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        )

        return rootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        navigationBar.onVisibilityChangedFromFragment(hidden)
    }

    private fun enterSearchMode() {
        if (isSearchExpanded) return
        isSearchExpanded = true
        cancelSearchAnimations()

        indicatorTitleVisible = indicatorTitleTextView.visibility == View.VISIBLE && indicatorTitleTextView.alpha > 0f
        indicatorSubtitleVisible = indicatorSubtitleTextView.visibility == View.VISIBLE && indicatorSubtitleTextView.alpha > 0f

        detailedSearchContainer.visibility = View.VISIBLE
        detailedSearchContainer.alpha = 0f
        detailedSearchContainer.translationY = 24.dp.px
        searchBarDetail.alpha = 1f
        resetTabRevealState()

        if (!detailedSearchContainer.isLaidOut) {
            detailedSearchContainer.doOnLayout { startEnterAnimation() }
        } else {
            startEnterAnimation()
        }
    }

    private fun startEnterAnimation() {
        val duration = (AnimationUtils.MID_DURATION * 1.15f).toLong()
        val fadeDuration = duration
        val pageOffsetY = -24.dp.px
        val detailOffsetY = 24.dp.px
        val mainViews = getMainPageViews()
        searchInputDetail.setText(searchInputNav.text)
        searchInputDetail.setSelection(searchInputDetail.text.length)

        mainViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.alpha = 1f
            view.translationY = 0f
        }
        detailedSearchContainer.alpha = 0f
        detailedSearchContainer.translationY = detailOffsetY

        val moveOut = mainViews.map { view ->
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, pageOffsetY).apply {
                this.duration = duration
                interpolator = AnimationUtils.easingStandardInterpolator
            }
        }
        val fadeOut = mainViews.map { view ->
            ObjectAnimator.ofFloat(view, View.ALPHA, 0f).apply {
                this.duration = fadeDuration
                interpolator = AnimationUtils.easingStandardInterpolator
            }
        }
        val moveInDetail = ObjectAnimator.ofFloat(detailedSearchContainer, View.TRANSLATION_Y, 0f).apply {
            this.duration = duration
            interpolator = AnimationUtils.easingStandardInterpolator
        }
        val fadeInDetail = ObjectAnimator.ofFloat(detailedSearchContainer, View.ALPHA, 1f).apply {
            this.duration = fadeDuration
            interpolator = AnimationUtils.easingStandardInterpolator
        }

        val enterSet = AnimatorSet().apply {
            playTogether(*(moveOut + fadeOut + listOf(moveInDetail, fadeInDetail)).toTypedArray())
            doOnEnd {
                mainViews.forEach { it.visibility = View.INVISIBLE }
                focusDetailedSearch()
            }
        }

        enterAnimator = enterSet
        enterSet.start()
    }

    private fun focusDetailedSearch() {
        searchInputNav.clearFocus()
        searchInputDetail.requestFocus()
        showKeyboard(searchInputDetail)
    }

    private fun exitSearchMode() {
        if (!isSearchExpanded) return
        isSearchExpanded = false
        cancelSearchAnimations()

        hideKeyboard(searchInputDetail)
        searchInputDetail.clearFocus()
        searchInputNav.setText(searchInputDetail.text)
        searchInputNav.setSelection(searchInputNav.text.length)

        if (!detailedSearchContainer.isLaidOut) {
            detailedSearchContainer.doOnLayout { startExitAnimation() }
        } else {
            startExitAnimation()
        }
    }

    private fun startExitAnimation() {
        val duration = (AnimationUtils.MID_DURATION * 1.15f).toLong()
        val fadeDuration = duration
        val pageOffsetY = -24.dp.px
        val detailOffsetY = 24.dp.px
        val mainViews = getMainPageViews()

        mainViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.translationY = pageOffsetY
        }
        detailedSearchContainer.alpha = 1f
        detailedSearchContainer.translationY = 0f

        val moveIn = mainViews.map { view ->
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f).apply {
                this.duration = duration
                interpolator = AnimationUtils.easingStandardInterpolator
            }
        }
        val fadeIn = mainViews.map { view ->
            ObjectAnimator.ofFloat(view, View.ALPHA, 1f).apply {
                this.duration = fadeDuration
                interpolator = AnimationUtils.easingStandardInterpolator
            }
        }
        val moveOutDetail = ObjectAnimator.ofFloat(detailedSearchContainer, View.TRANSLATION_Y, detailOffsetY).apply {
            this.duration = duration
            interpolator = AnimationUtils.easingStandardInterpolator
        }
        val fadeOutDetail = ObjectAnimator.ofFloat(detailedSearchContainer, View.ALPHA, 0f).apply {
            this.duration = fadeDuration
            interpolator = AnimationUtils.easingStandardInterpolator
        }

        val exitSet = AnimatorSet().apply {
            playTogether(*(moveIn + fadeIn + listOf(moveOutDetail, fadeOutDetail)).toTypedArray())
            doOnEnd {
                detailedSearchContainer.visibility = View.GONE
                resetTabRevealState()
            }
        }

        exitAnimator = exitSet
        exitSet.start()
    }

    private fun cancelSearchAnimations() {
        enterAnimator?.cancel()
        exitAnimator?.cancel()
        enterAnimator = null
        exitAnimator = null
    }

    private fun resetTabRevealState() {
        tabContainer.alpha = 1f
        tabContainer.translationY = 0f
        tabContainer.scaleX = 1f
        tabContainer.scaleY = 1f
    }

    private fun getMainPageViews(): List<View> {
        val views = mutableListOf<View>(navigationBar, recyclerView)
        if (indicatorTitleVisible) {
            indicatorTitleTextView.visibility = View.VISIBLE
            views.add(indicatorTitleTextView)
        } else {
            indicatorTitleTextView.visibility = View.GONE
        }
        if (indicatorSubtitleVisible) {
            indicatorSubtitleTextView.visibility = View.VISIBLE
            views.add(indicatorSubtitleTextView)
        } else {
            indicatorSubtitleTextView.visibility = View.GONE
        }
        return views
    }

    private fun selectTab(isAppleTab: Boolean) {
        if (isAppleTabSelected == isAppleTab) return
        isAppleTabSelected = isAppleTab
        updateTabSelection()
        updateTabIndicator(true)
    }

    private fun updateTabSelection() {
        val selectedColor = requireContext().getColor(R.color.onSurfaceColor)
        val inactiveColor = requireContext().getColor(R.color.onSurfaceColorInactive)
        tabAppleTextView.setTextColor(if (isAppleTabSelected) selectedColor else inactiveColor)
        tabLibraryTextView.setTextColor(if (isAppleTabSelected) inactiveColor else selectedColor)
    }

    private fun updateTabIndicator(animate: Boolean) {
        val container = tabIndicator.parent as View
        val containerWidth = container.width
        if (containerWidth == 0) return
        val marginParams = tabIndicator.layoutParams as ViewGroup.MarginLayoutParams
        val horizontalMargin = marginParams.leftMargin + marginParams.rightMargin
        val segmentWidth = (containerWidth - horizontalMargin) / 2f
        val targetWidth = segmentWidth.toInt()
        if (marginParams.width != targetWidth) {
            marginParams.width = targetWidth
            tabIndicator.layoutParams = marginParams
        }
        val targetTranslation = if (isAppleTabSelected) {
            0f
        } else {
            (containerWidth - targetWidth - horizontalMargin).toFloat()
        }
        if (animate) {
            tabIndicator.animate()
                .translationX(targetTranslation)
                .setDuration(AnimationUtils.MID_DURATION)
                .setInterpolator(AnimationUtils.easingStandardInterpolator)
                .start()
        } else {
            tabIndicator.translationX = targetTranslation
        }
    }

    private fun showKeyboard(target: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(target: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(target.windowToken, 0)
    }
}
