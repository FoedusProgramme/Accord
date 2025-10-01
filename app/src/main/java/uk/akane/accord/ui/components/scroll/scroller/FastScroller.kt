package uk.akane.accord.ui.components.scroll.scroller

import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.springAnimator
import kotlin.math.roundToInt

class FastScroller<T>(private val view: T) where T : View, T : FastScrollerTargetView {
    private val horizontalPadding = 2.5.dp.px
    private val idleScrollerWidth = 3.dp.px
    private val activeScrollerWidth = 5.dp.px
    private val idleScrollerAlpha = 0.4f
    private val activeScrollerAlpha = 0.6f
    private val drawable = FastDrawable().apply {
        paint.color = Color.RED
        paint.alpha = (idleScrollerAlpha * 0xFF).roundToInt()
        paint.strokeWidth = idleScrollerWidth
        setCornerRadius(idleScrollerWidth)
    }

    private var shouldIntercept = false
    private var startY = 0f
    private var dy = 0f
    private var lastY = Float.NaN
    private var isDragging = false
    private var toolbarHasSnapBehavior = false
    private var dragDeltaMultiplier = 1f

    private val handler = Handler(Looper.getMainLooper())

    private val widthAnimator = springAnimator(
        idleScrollerWidth,
        stiffness = 150f,
        minValue = idleScrollerWidth,
        maxValue = activeScrollerWidth
    ) {
        val value = it.currentValue
        drawable.paint.strokeWidth = value
        drawable.setCornerRadius(value / 2)
        drawable.bounds.left = drawable.bounds.right - value
        val scrollState = view.viewScrollState
        if (scrollState == ScrollState.Idle) {
            view.postInvalidateOnAnimation()
        }
    }

    private val alphaAnimator = springAnimator(
        idleScrollerAlpha,
        stiffness = 150f,
        valueThreshold = 0.01f,
        minValue = 0f,
        maxValue = activeScrollerAlpha
    ) {
        drawable.paint.alpha = (it.currentValue * 0xFF).roundToInt()
        val scrollState = view.viewScrollState
        if (scrollState == ScrollState.Idle) {
            view.postInvalidateOnAnimation()
        }
    }

    fun show() {
        val targetValue = if (isDragging) activeScrollerAlpha else idleScrollerAlpha
        if (alphaAnimator.targetValue == targetValue) return
        handler.removeCallbacksAndMessages(null)
        alphaAnimator.animateTo(targetValue)
    }

    private fun showImmediately() {
        handler.removeCallbacksAndMessages(null)
        alphaAnimator.snapTo(activeScrollerAlpha)
    }

    fun hide() {
        if (alphaAnimator.targetValue == 0f) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!isDragging && view.viewScrollState == ScrollState.Idle) {
                alphaAnimator.animateTo(0f)
            }
        }, 1000)
    }

    fun hideScrollerImmediately() {
        if (alphaAnimator.currentValue == 0f) return
        handler.removeCallbacksAndMessages(null)
        alphaAnimator.snapTo(0f)
    }

    fun draw(c: Canvas) {
        drawable.draw(c)
    }

    fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        shouldIntercept = alphaAnimator.currentValue > 0f &&
                e.x >= view.width - horizontalPadding * 2 - activeScrollerWidth &&
                (e.y >= drawable.bounds.top && e.y <= drawable.bounds.bottom)
        if (shouldIntercept) {
            startY = e.rawY
        }
        return shouldIntercept
    }

    fun onTouchEvent(e: MotionEvent): Boolean {
        if (!shouldIntercept) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {}

            MotionEvent.ACTION_MOVE -> {
                if (lastY.isNaN()) {
                    lastY = e.rawY
                    isDragging = true

                    val paddingTop = view.paddingTop
                    val height: Float = (view.height.toFloat() - paddingTop - view.paddingBottom).coerceAtLeast(0f)
                    val range: Float = view.computeVerticalScrollRange().toFloat().coerceAtLeast(height)
                    val extent: Float = view.computeVerticalScrollExtent().toFloat().coerceAtLeast(height)
                    dragDeltaMultiplier = range / extent - 0.5f

                    showImmediately()
                    widthAnimator.animateTo(activeScrollerWidth)
                    view.setScrollState(ScrollState.Dragging)

                    val toolbarLayout =
                        ((view.parent as? CoordinatorLayout)
                            ?.getChildAt(0) as? AppBarLayout)
                            ?.getChildAt(0) as? CollapsingToolbarLayout
                    val params = toolbarLayout?.layoutParams as? AppBarLayout.LayoutParams
                    if (params != null) {
                        toolbarHasSnapBehavior = params.scrollFlags and AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP != 0
                        if (toolbarHasSnapBehavior) {
                            params.setScrollFlags(params.scrollFlags xor AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP)
                        }
                    }
                }

                dy = e.rawY - lastY
                lastY = e.rawY

                view.nestedScrollBy(0, (dy * dragDeltaMultiplier).roundToInt())
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                lastY = Float.NaN

                alphaAnimator.animateTo(idleScrollerAlpha)
                widthAnimator.animateTo(idleScrollerWidth)
                hide()
                view.setScrollState(ScrollState.Idle)

                if (toolbarHasSnapBehavior) {
                    val appBarLayout = (view.parent as? CoordinatorLayout)?.getChildAt(0) as? AppBarLayout
                    if (appBarLayout != null) {
                        val toolbarLayout = appBarLayout.getChildAt(0) as? CollapsingToolbarLayout
                        val params = toolbarLayout?.layoutParams as? AppBarLayout.LayoutParams
                        params?.setScrollFlags(params.scrollFlags or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP)
                        if (!view.canScrollVertically(-1)) {
                            val expanded = when {
                                dy < 0f -> true
                                dy > 0f -> false
                                else -> appBarLayout.height - appBarLayout.totalScrollRange > appBarLayout.height / 2
                            }
                            appBarLayout.setExpanded(expanded, true)
                        }
                    }
                }
            }
        }
        return isDragging
    }

    fun updateBounds() {
        val x = view.width - horizontalPadding
        val paddingTop = view.paddingTop
        val height: Float = (view.height.toFloat() - paddingTop - view.paddingBottom).coerceAtLeast(0f)
        val scrollY: Float = view.computeVerticalScrollOffset().toFloat()
        val scrollRange: Float = view.computeVerticalScrollRange().toFloat()

        val range: Float = scrollRange.coerceAtLeast(height)
        val offset: Float = scrollY.coerceIn(0f, scrollRange)
        val extent: Float = view.computeVerticalScrollExtent().toFloat().coerceAtLeast(height)

        val top = offset.coerceAtLeast(0f) / range * height + paddingTop
        val bottom = (offset.coerceAtMost(scrollRange) + extent) / range * height + paddingTop
        drawable.setBounds(x - drawable.paint.strokeWidth, top, x, bottom)
    }
}
