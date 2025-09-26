package uk.akane.accord.ui

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.akane.accord.Accord
import uk.akane.accord.R
import uk.akane.accord.logic.enableEdgeToEdgeProperly
import uk.akane.accord.logic.isDarkMode
import uk.akane.accord.logic.isEssentialPermissionGranted
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.accord.logic.utils.UiUtils
import uk.akane.accord.setupwizard.fragments.SetupWizardFragment
import uk.akane.accord.ui.components.player.FloatingPanelLayout
import uk.akane.accord.ui.fragments.BrowseFragment
import uk.akane.accord.ui.fragments.HomeFragment
import uk.akane.accord.ui.fragments.LibraryFragment
import uk.akane.accord.ui.fragments.SearchFragment
import uk.akane.accord.ui.viewmodels.MediaControllerViewModel
import uk.akane.cupertino.widget.navigation.FragmentSwitcherView
import uk.akane.cupertino.widget.utils.AnimationUtils

class MainActivity : AppCompatActivity() {
    companion object {
        const val DESIRED_BOTTOM_SHEET_OPEN_RATIO = 0.9f
        const val DESIRED_BOTTOM_SHEET_DISPLAY_RATIO = 0.85F

        const val PLAYBACK_AUTO_START_FOR_FGS = "AutoStartFgs"
        const val PLAYBACK_AUTO_PLAY_ID = "AutoStartId"
        const val PLAYBACK_AUTO_PLAY_POSITION = "AutoStartPos"
    }

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var floatingPanelLayout: FloatingPanelLayout
    private lateinit var shrinkContainerLayout: MaterialCardView
    private lateinit var screenCorners: UiUtils.ScreenCorners
    lateinit var fragmentSwitcherView: FragmentSwitcherView

    private var bottomInset: Int = 0
    private var bottomDefaultRadius: Int = 0

    private var bottomNavigationPanelColor: Int = 0

    private var isWindowColorSet: Boolean = false

    private var ready: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiUtils.init(this)

        bottomDefaultRadius = resources.getDimensionPixelSize(R.dimen.bottom_panel_radius)
        bottomNavigationPanelColor = getColor(R.color.bottomNavigationPanelColor)

        installSplashScreen().setKeepOnScreenCondition { !ready }
        enableEdgeToEdgeProperly()

        lifecycle.addObserver(controllerViewModel)
        controllerViewModel.addControllerCallback(lifecycle) { controller, controllerLifecycle ->
            ready = true
        }

        setContentView(R.layout.activity_main)

        if (!isEssentialPermissionGranted()) {
            insertContainer(SetupWizardFragment {
                updateLibrary()
            })
        } else {
            Log.d("TAG", "yes we have permission")
            updateLibrary()
            lifecycleScope.launch {
                reader.songListFlow.collect { songs ->
                    Log.d("TAG", "collected song list: ${songs.size}")
                }
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_nav)
        floatingPanelLayout = findViewById(R.id.floating)
        shrinkContainerLayout = findViewById(R.id.shrink_container)
        fragmentSwitcherView = findViewById(R.id.switcher)

        fragmentSwitcherView.setup(
            this,
            listOf(
                HomeFragment(),
                BrowseFragment(),
                LibraryFragment(),
                SearchFragment()
            ),
            listOf(
                "Home",
                "Browse",
                "Library",
                "Search"
            )
        )

        bottomNavigationView.setOnItemSelectedListener { item ->
            fragmentSwitcherView.switchBaseFragment(
                when (item.itemId) {
                    R.id.home -> 0
                    R.id.browse -> 1
                    R.id.library -> 2
                    R.id.search -> 3
                    else -> throw IllegalArgumentException("Invalid itemId!")
                }
            )
            true
        }

        floatingPanelLayout.addOnSlideListener(object : FloatingPanelLayout.OnSlideListener {
            override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
                when (status) {
                    FloatingPanelLayout.SlideStatus.EXPANDED -> {
                        if (!isDarkMode() &&
                            floatingPanelLayout.insetController.isAppearanceLightStatusBars) {
                            floatingPanelLayout.insetController
                                .isAppearanceLightStatusBars = false
                        }
                    }
                    FloatingPanelLayout.SlideStatus.COLLAPSED -> {
                        shrinkContainerLayout.apply {
                            scaleX = 1f
                            scaleY = 1f
                        }
                        if (!isDarkMode() && ! floatingPanelLayout.insetController.isAppearanceLightStatusBars) {
                            floatingPanelLayout.insetController
                                .isAppearanceLightStatusBars = true
                        }
                    }
                    FloatingPanelLayout.SlideStatus.SLIDING -> {
                        if (!isDarkMode() && ! floatingPanelLayout.insetController.isAppearanceLightStatusBars) {
                            floatingPanelLayout.insetController
                                .isAppearanceLightStatusBars = true
                        }
                    }
                }
            }

            override fun onSlide(value: Float) {
                if (!isWindowColorSet) {
                    findViewById<View>(R.id.main).setBackgroundColor(
                        getColor(R.color.windowColor)
                    )
                    isWindowColorSet = true
                }
                shrinkContainer(value, DESIRED_BOTTOM_SHEET_OPEN_RATIO)
                val cornerProgress = (screenCorners.getAvgRadius() - bottomDefaultRadius) * value + bottomDefaultRadius
                floatingPanelLayout.panelCornerRadius = cornerProgress
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { v, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            val windowInsets = windowInsetsCompat.toWindowInsets()!!
            screenCorners = UiUtils.ScreenCorners(
                (windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0).toFloat(),
                (windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0).toFloat(),
                (windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0).toFloat(),
                (windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0).toFloat()
            )

            shrinkContainerLayout.shapeAppearanceModel =
                shrinkContainerLayout.shapeAppearanceModel
                    .toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, screenCorners.topLeft)
                    .setTopRightCorner(CornerFamily.ROUNDED, screenCorners.topRight)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, screenCorners.bottomLeft)
                    .setBottomRightCorner(CornerFamily.ROUNDED, screenCorners.bottomRight)
                    .build()
            bottomInset = insets.bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

    }

    val bottomHeight: Int
        get() = bottomNavigationView.paddingBottom +
                resources.getDimensionPixelSize(R.dimen.bottom_nav_height_raw) +
                resources.getDimensionPixelSize(R.dimen.preview_player_height)

    private fun shrinkContainer(value: Float, ratio: Float) {
        shrinkContainerLayout.alpha = lerp(1f, 0.5f, value)
        shrinkContainerLayout.apply {
            scaleX = lerp(1f, ratio, value)
            scaleY = lerp(1f, ratio, value)
        }
    }

    private fun shrinkFloatingPanel(value: Float, ratio: Float) {
        floatingPanelLayout.alpha = lerp(1f, 0.5f, value)
        floatingPanelLayout.apply {
            scaleX = lerp(1f, ratio, value)
            scaleY = lerp(1f, ratio, value)
        }
    }

    private var containerId: Int = View.NO_ID

    private fun insertContainer(fragment: Fragment) {
        val container = FragmentContainerView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        containerId = container.id

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(container)

        container.post {
            supportFragmentManager.beginTransaction()
                .replace(container.id, fragment)
                .runOnCommit {
                    // Round corner handling
                    val containerCardView: MaterialCardView = container.findViewById(R.id.root_card_view)
                    containerCardView.shapeAppearanceModel =
                        containerCardView.shapeAppearanceModel
                            .toBuilder()
                            .setTopLeftCorner(CornerFamily.ROUNDED, screenCorners.topLeft)
                            .setTopRightCorner(CornerFamily.ROUNDED, screenCorners.topRight)
                            .setBottomLeftCorner(CornerFamily.ROUNDED, screenCorners.bottomLeft)
                            .setBottomRightCorner(
                                CornerFamily.ROUNDED,
                                screenCorners.bottomRight
                            )
                            .build()
                    val screenHeight =
                        Resources.getSystem().displayMetrics.heightPixels.toFloat()

                    containerCardView.updateLayoutParams<MarginLayoutParams> {
                        topMargin =
                            ((1F - DESIRED_BOTTOM_SHEET_DISPLAY_RATIO + 0.05F) / 2 * screenHeight).toInt()
                    }

                    containerCardView.translationY = screenHeight
                    containerCardView.setCardBackgroundColor(resources.getColor(R.color.setupWizardSurfaceColor, null))

                    containerCardView.post {

                        containerCardView.visibility = View.VISIBLE

                        AnimationUtils.createValAnimator<Float>(
                            containerCardView.translationY,
                            0F,
                            duration = AnimationUtils.LONG_DURATION
                        ) { animatedValue ->
                            containerCardView.translationY = animatedValue
                            shrinkContainer(1f - animatedValue / screenHeight, DESIRED_BOTTOM_SHEET_DISPLAY_RATIO)
                            shrinkFloatingPanel(1f - animatedValue / screenHeight, DESIRED_BOTTOM_SHEET_DISPLAY_RATIO)
                        }
                    }
                }
                .commit()
        }
    }

    fun removeContainer() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)

        val container = rootView.findViewById<FragmentContainerView>(containerId)

        val containerCardView: MaterialCardView = container.findViewById(R.id.root_card_view)
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()

        AnimationUtils.createValAnimator<Float>(
            0F,
            screenHeight,
            duration = AnimationUtils.LONG_DURATION,
            doOnEnd = {
                supportFragmentManager.findFragmentById(container.id)?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
                rootView.removeView(container)
            }
        ) { animatedValue ->
            containerCardView.translationY = animatedValue
            shrinkContainer(1f - animatedValue / screenHeight, DESIRED_BOTTOM_SHEET_DISPLAY_RATIO)
            shrinkFloatingPanel(1f - animatedValue / screenHeight, DESIRED_BOTTOM_SHEET_DISPLAY_RATIO)
        }
    }

    fun updateLibrary(then: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            reader.refresh()
            withContext(Dispatchers.Main) {
                then?.let { it() }
            }
        }
    }

    inline val accord: Accord
        get() = application as Accord

    inline val reader
        get() = accord.reader

    /**
     * getPlayer:
     *   Returns a media controller.
     */
    fun getPlayer() = controllerViewModel.get()

    val controllerViewModel: MediaControllerViewModel by viewModels()

}