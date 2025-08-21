package uk.akane.accord.logic

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.AnyRes
import androidx.annotation.FloatRange
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import uk.akane.accord.R
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.accord.logic.utils.UiUtils
import uk.akane.cupertino.widget.utils.AnimationUtils
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.max

fun View.enableEdgeToEdgePaddingListener(
    ime: Boolean = false, top: Boolean = false,
    extra: ((Insets) -> Unit)? = null
) {
    if (fitsSystemWindows) throw IllegalArgumentException("must have fitsSystemWindows disabled")
    if (this is AppBarLayout) {
        if (ime) throw IllegalArgumentException("AppBarLayout must have ime flag disabled")
        // AppBarLayout fitsSystemWindows does not handle left/right for a good reason, it has
        // to be applied to children to look good; we rewrite fitsSystemWindows in a way mostly specific
        // to Gramophone to support shortEdges displayCutout
        val collapsingToolbarLayout =
            children.find { it is CollapsingToolbarLayout } as CollapsingToolbarLayout?
        collapsingToolbarLayout?.let {
            // The CollapsingToolbarLayout mustn't consume insets, we handle padding here anyway
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets -> insets }
        }
        collapsingToolbarLayout?.let{
            it.setCollapsedTitleTypeface(ResourcesCompat.getFont(context, R.font.inter_semibold))
            it.setExpandedTitleTypeface(ResourcesCompat.getFont(context, R.font.inter_bold))
        }
        val expandedTitleMarginStart = collapsingToolbarLayout?.expandedTitleMarginStart
        val expandedTitleMarginEnd = collapsingToolbarLayout?.expandedTitleMarginEnd
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val cutoutAndBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            (v as AppBarLayout).children.forEach {
                if (it is CollapsingToolbarLayout) {
                    val es = expandedTitleMarginStart!! + if (it.layoutDirection
                        == View.LAYOUT_DIRECTION_LTR
                    ) cutoutAndBars.left else cutoutAndBars.right
                    if (es != it.expandedTitleMarginStart) it.expandedTitleMarginStart = es
                    val ee = expandedTitleMarginEnd!! + if (it.layoutDirection
                        == View.LAYOUT_DIRECTION_RTL
                    ) cutoutAndBars.left else cutoutAndBars.right
                    if (ee != it.expandedTitleMarginEnd) it.expandedTitleMarginEnd = ee
                }
                it.setPadding(cutoutAndBars.left, 0, cutoutAndBars.right, 0)
            }
            v.setPadding(0, cutoutAndBars.top, 0, 0)
            val i = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            extra?.invoke(cutoutAndBars)
            return@setOnApplyWindowInsetsListener WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout(),
                    Insets.of(cutoutAndBars.left, 0, cutoutAndBars.right, cutoutAndBars.bottom)
                )
                .setInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout(),
                    Insets.of(i.left, 0, i.right, i.bottom)
                )
                .build()
        }
    } else {
        val pl = paddingLeft
        val pt = paddingTop
        val pr = paddingRight
        val pb = paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val mask = WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    if (ime) WindowInsetsCompat.Type.ime() else 0
            val i = insets.getInsets(mask)
            v.setPadding(
                pl + i.left, pt + (if (top) i.top else 0), pr + i.right,
                pb + i.bottom
            )
            extra?.invoke(i)
            return@setOnApplyWindowInsetsListener WindowInsetsCompat.Builder(insets)
                .setInsets(mask, Insets.NONE)
                .setInsetsIgnoringVisibility(mask, Insets.NONE)
                .build()
        }
    }
}

fun TextView.enableEdgeToEdgeListener(
    ime: Boolean = false,
    top: Boolean = false,
    extra: ((Insets) -> Unit)? = null
) {
    if (fitsSystemWindows) throw IllegalArgumentException("must have fitsSystemWindows disabled")

    val originalLp = layoutParams as ViewGroup.MarginLayoutParams
    val ml = originalLp.leftMargin
    val mt = originalLp.topMargin
    val mr = originalLp.rightMargin
    val mb = originalLp.bottomMargin

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val mask = WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                if (ime) WindowInsetsCompat.Type.ime() else 0

        val i = insets.getInsets(mask)

        val lp = v.layoutParams as ViewGroup.MarginLayoutParams
        lp.leftMargin = ml + i.left
        lp.topMargin = mt + if (top) i.top else 0
        lp.rightMargin = mr + i.right
        lp.bottomMargin = mb + i.bottom
        v.layoutParams = lp

        extra?.invoke(i)

        WindowInsetsCompat.Builder(insets)
            .setInsets(mask, Insets.NONE)
            .setInsetsIgnoringVisibility(mask, Insets.NONE)
            .build()
    }
}

// enableEdgeToEdge() without enforcing contrast, magic based on androidx EdgeToEdge.kt
fun ComponentActivity.enableEdgeToEdgeProperly() {
    if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
    } else {
        val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, darkScrim))
    }
}

fun ViewPager2.setCurrentItemInterpolated(
    item: Int,
    duration: Long = AnimationUtils.FAST_DURATION,
    interpolator: TimeInterpolator = AnimationUtils.easingStandardInterpolator,
    pagePxWidth: Int = width
) {
    val pxToDrag: Int = pagePxWidth * (item - currentItem)
    val animator = ValueAnimator.ofInt(0, pxToDrag)
    var previousValue = 0
    animator.addUpdateListener { valueAnimator ->
        val currentValue = valueAnimator.animatedValue as Int
        val currentPxToDrag = (currentValue - previousValue).toFloat()
        fakeDragBy(-currentPxToDrag *
                if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR) 1 else -1)
        previousValue = currentValue
    }
    animator.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) { beginFakeDrag() }
        override fun onAnimationEnd(animation: Animator) { endFakeDrag() }
        override fun onAnimationCancel(animation: Animator) { /* Ignored */ }
        override fun onAnimationRepeat(animation: Animator) { /* Ignored */ }
    })
    animator.interpolator = interpolator
    animator.duration = duration
    animator.start()
}

@JvmInline
value class Dp(val value: Float) {
    inline val px: Float
        get() = value * UiUtils.density

    companion object {
        val Zero = Dp(0f)
    }
}

inline val Int.dp: Dp
    get() = Dp(this.toFloat())

inline val Float.dp: Dp
    get() = Dp(this)

inline val Double.dp: Dp
    get() = Dp(this.toFloat())


@JvmInline
value class Sp(val value: Float) {
    inline val px: Float
        get() = value * UiUtils.scaledDensity

    companion object {
        val Zero = Sp(0f)
    }
}

inline val Int.sp: Sp
    get() = Sp(this.toFloat())

inline val Float.sp: Sp
    get() = Sp(this)

inline val Double.sp: Sp
    get() = Sp(this.toFloat())

fun floatAnimator(
    duration: Long,
    initialValue: Float = 0f,
    targetValue: Float = 1f,
    startDelay: Long = 0L,
    interpolator: TimeInterpolator? = null,
    listener: AnimationUtils.Animator.ValueUpdateListener<Float>
) = AnimationUtils.LinearAnimator(
    initialValue = initialValue,
    targetValue = targetValue,
    startDelay = startDelay,
    duration = duration,
    interpolator = interpolator,
    listener = listener,
    lerp = FloatLerp
)

private val FloatLerp = { from: Float, to: Float, fraction: Float -> lerp(from, to, fraction) }

fun Rect.scale(
    @FloatRange(from = -1.0, to = 1.0) scaleX: Float,
    @FloatRange(from = -1.0, to = 1.0) scaleY: Float
) {
    val newWidth = width() * scaleX
    val newHeight = height() * scaleY
    val deltaX = (width() - newWidth) / 2
    val deltaY = (height() - newHeight) / 2

    set((left + deltaX).toInt(), (top + deltaY).toInt(), (right - deltaX).toInt(), (bottom - deltaY).toInt())
}

fun AppBarLayout.applyOffsetListener() =
    this.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
        private val collapsingToolbarLayout: CollapsingToolbarLayout = children.find { it is CollapsingToolbarLayout } as CollapsingToolbarLayout
        private val materialToolbar: MaterialToolbar = collapsingToolbarLayout.children.find { it is MaterialToolbar } as MaterialToolbar
        private var defaultTitleMarginBottom = 0
        init {
            defaultTitleMarginBottom = materialToolbar.titleMarginBottom
            materialToolbar.isTitleCentered = true
        }

        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            val progress = verticalOffset.absoluteValue / (appBarLayout.height - materialToolbar.height - appBarLayout.paddingTop).toFloat()
            val progressTitle = 1f - max(0f, (progress - 0.5f) / 0.5f)

            materialToolbar.background = AppCompatResources.getDrawable(appBarLayout.context, R.drawable.top_app_bar_divider)?.apply {
                alpha = (progress * 255).toInt()
            }

            val destinationOffset = (-resources.getDimensionPixelSize(R.dimen.toolbar_margin_bottom_offset) * progressTitle + defaultTitleMarginBottom).toInt()
            // Use a flag or condition to ensure this does not repeatedly cause layout changes
            if (materialToolbar.titleMarginBottom != destinationOffset) {
                materialToolbar.titleMarginBottom = destinationOffset
            }
        }
    })

/**
 * get uri to drawable or any other resource type if u wish
 * @param drawableId - drawable res id
 * @return - uri
 */
fun Context.getUriToDrawable(
    @AnyRes drawableId: Int
): Uri {
    val imageUri = (ContentResolver.SCHEME_ANDROID_RESOURCE
            + "://" + this.resources.getResourcePackageName(drawableId)
            + '/' + this.resources.getResourceTypeName(drawableId)
            + '/' + this.resources.getResourceEntryName(drawableId)).toUri()
    return imageUri
}

fun Context.hasMediaPermissionSeparation() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun Context.isAlbumPermissionGranted() =
    (hasMediaPermissionSeparation() && (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)) ||
            (!hasMediaPermissionSeparation() && isEssentialPermissionGranted())

fun Context.isEssentialPermissionGranted() =
    (!hasMediaPermissionSeparation() && (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) ||
            (hasMediaPermissionSeparation() && (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED))