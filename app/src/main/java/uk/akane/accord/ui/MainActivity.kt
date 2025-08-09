package uk.akane.accord.ui

import android.content.Context
import android.content.res.Configuration
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import com.google.android.material.shape.CornerFamily
import uk.akane.accord.R
import uk.akane.accord.logic.enableEdgeToEdgeProperly
import uk.akane.accord.logic.isEssentialPermissionGranted
import uk.akane.accord.logic.utils.MediaUtils
import uk.akane.accord.logic.utils.UiUtils
import uk.akane.accord.setupwizard.fragments.SetupWizardFragment
import uk.akane.accord.ui.components.FloatingPanelLayout
import uk.akane.accord.ui.viewmodels.AccordViewModel
import uk.akane.cupertino.widget.utils.AnimationUtils

class MainActivity : AppCompatActivity() {

    private val accordViewModel: AccordViewModel by viewModels()

    companion object {
        const val DESIRED_BOTTOM_SHEET_OPEN_RATIO = 0.9f
        const val DESIRED_BOTTOM_SHEET_DISPLAY_RATIO = 0.85F
    }

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var floatingPanelLayout: FloatingPanelLayout
    private lateinit var shrinkContainerLayout: MaterialCardView
    private lateinit var screenCorners: UiUtils.ScreenCorners

    private var bottomInset: Int = 0
    private var bottomDefaultRadius: Int = 0

    private var bottomNavigationPanelColor: Int = 0

    private var isWindowColorSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiUtils.init(this)

        bottomDefaultRadius = resources.getDimensionPixelSize(R.dimen.bottom_panel_radius)
        bottomNavigationPanelColor = getColor(R.color.bottomNavigationPanelColor)

        installSplashScreen()
        enableEdgeToEdgeProperly()

        setContentView(R.layout.activity_main)

        if (isEssentialPermissionGranted()) {
            if (accordViewModel.mediaItemList.value?.isNotEmpty() != true) {
                MediaUtils.updateLibraryWithInCoroutine(accordViewModel, this)
            }
        } else {
            insertContainer(SetupWizardFragment {
                if (accordViewModel.mediaItemList.value?.isNotEmpty() != true) {
                    MediaUtils.updateLibraryWithInCoroutine(accordViewModel, this)
                }
            })
        }

        bottomNavigationView = findViewById(R.id.bottom_nav)
        floatingPanelLayout = findViewById(R.id.floating)
        shrinkContainerLayout = findViewById(R.id.shrink_container)

        floatingPanelLayout.addOnSlideListener(object : FloatingPanelLayout.OnSlideListener {
            override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
                when (status) {
                    FloatingPanelLayout.SlideStatus.EXPANDED -> {
                        if (!isDarkMode(this@MainActivity) &&
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
                        if (!isDarkMode(this@MainActivity) && ! floatingPanelLayout.insetController.isAppearanceLightStatusBars) {
                            floatingPanelLayout.insetController
                                .isAppearanceLightStatusBars = true
                        }
                    }
                    FloatingPanelLayout.SlideStatus.SLIDING -> {
                        if (!isDarkMode(this@MainActivity) && ! floatingPanelLayout.insetController.isAppearanceLightStatusBars) {
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
                Log.d("TAG", "screenCorners: ${screenCorners.getAvgRadius()}, cp: $cornerProgress")
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

    fun connectBottomNavigationView(listener : OnItemSelectedListener) {
        bottomNavigationView.setOnItemSelectedListener(listener)
    }

    private fun isDarkMode(context: Context): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun shrinkContainer(value: Float, ratio: Float) {
        shrinkContainerLayout.alpha = (1f - value).coerceIn(0.5f, 1f)
        shrinkContainerLayout.apply {
            scaleX = (1f - ratio) * (1f - value) + ratio
            scaleY = (1f - ratio) * (1f - value) + ratio
        }
    }

    private fun shrinkFloatingPanel(value: Float, ratio: Float) {
        floatingPanelLayout.alpha = (1f - value).coerceIn(0.5f, 1f)
        floatingPanelLayout.apply {
            scaleX = (1f - ratio) * (1f - value) + ratio
            scaleY = (1f - ratio) * (1f - value) + ratio
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

}