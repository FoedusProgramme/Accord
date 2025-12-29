package uk.akane.accord.ui.components.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RenderNode
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import kotlinx.parcelize.Parcelize
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.setOutline
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.cupertino.widget.popup.PopupHelper
import uk.akane.cupertino.widget.popup.PopupMenuHost
import uk.akane.cupertino.widget.dpToPx
import uk.akane.cupertino.widget.image.SimpleImageView
import uk.akane.cupertino.widget.utils.AnimationUtils
import kotlin.math.absoluteValue

class FloatingPanelLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    GestureDetector.OnGestureListener,
    PopupMenuHost {

    private val activity: Activity
        get() = context as Activity

    private val gestureDetector = GestureDetector(context, this)
    val insetController = WindowCompat.getInsetsController(activity.window, this)

    private var fraction: Float = 0F
    private var initialMargin = IntArray(4)

    private var fullScreenView: FullPlayer
    private var previewView: View

    private var flingValueAnimator: ValueAnimator? = null
    private var penultimateMotionTime = 0L
    private var penultimateMotionY = 0F
    private var lastMotionTime = 0L
    private var lastMotionY = 0F

    private val path = Path()

    private var boundLeft = 0F
    private var boundTop = 0F
    private var boundRight = 0F
    private var boundBottom = 0F

    private var fullLeft = 0F
    private var fullTop = 0F
    private var fullRight = 0F
    private var fullBottom = 0F

    private var previewLeft = 0F
    private var previewTop = 0F
    private var previewRight = 0F
    private var previewBottom = 0F

    private var isDragging = false

    var transitionImageView: SimpleImageView? = null

    var panelCornerRadius = 0F

    private var previewCoverBoxMetrics: Int = 0
    private var previewCoverMargin: Int = 8.dpToPx(context)
    private var previewCoverHorizontalMargin: Int = 12.dpToPx(context)

    private var fullCoverX: Int = 0
    private var fullCoverY: Int = 0
    private var fullCoverScale = 1F
    private var lockTransitionCornerRadius = false

    private var state: SlideStatus = SlideStatus.COLLAPSED

    private var onSlideListeners: MutableList<OnSlideListener> = mutableListOf()

    private val contentRenderNode = RenderNode("content").apply {
        clipToOutline = true
    }

    private val popupHelper = PopupHelper(
        context,
        contentRenderNode,
        ResourcesCompat.getFont(context, R.font.inter_regular)
    )

    override val popupHostView: View
        get() = this
    private val popupBackgroundRenderNode = RenderNode("popupBackground")

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.bottomNavigationPanelColor, null)
        style = Paint.Style.FILL
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        inflate(context, R.layout.layout_floating_panel, this)

        fullScreenView = findViewById(R.id.full_player)
        previewView = findViewById(R.id.preview_player)

        doOnLayout {
            panelCornerRadius = resources.getDimensionPixelSize(R.dimen.bottom_panel_radius).toFloat()
            // First init to sync the view location
            updateTransform(fraction)
        }
    }

    fun setupMetrics(metrics: Int) {
        previewCoverBoxMetrics = metrics
    }

    fun setupTransitionImageView(w: Int, h: Int, mx: Int, mh: Int, bitmap: Bitmap) {
        if (transitionImageView != null) return

        fullCoverX = mx
        fullCoverY = mh

        transitionImageView = SimpleImageView(context).apply {
            id = generateViewId()
            layoutParams = LayoutParams(w, h)
            setImageBitmap(bitmap)
            updateCornerRadius(startRadius.toInt())
        }

        addView(transitionImageView)

        transitionImageView?.let {
            val constraintSet = ConstraintSet()
            constraintSet.clone(this)
            constraintSet.connect(it.id, ConstraintSet.START, previewView.id, ConstraintSet.START, 0)
            constraintSet.connect(it.id, ConstraintSet.TOP, previewView.id, ConstraintSet.TOP, 0)
            constraintSet.applyTo(this)

            it.pivotX = 0F
            it.pivotY = 0F

            it.doOnLayout {
                updateTransitionFraction(fraction)
            }
        }
    }

    fun updateTransitionTarget(targetView: View, targetRadius: Float, lockCornerRadius: Boolean,
                               targetElevation: Float? = null) {
        val imageView = transitionImageView ?: return
        if (imageView.width == 0) {
            imageView.doOnLayout {
                updateTransitionTarget(targetView, targetRadius, lockCornerRadius, targetElevation)
            }
            return
        }
        if (targetView.width == 0 || targetView.height == 0) {
            targetView.doOnLayout {
                updateTransitionTarget(targetView, targetRadius, lockCornerRadius, targetElevation)
            }
            return
        }

        var x = 0
        var y = 0
        var current: View = targetView
        while (current !== fullScreenView && current.parent is View) {
            x += current.left
            y += current.top
            current = current.parent as View
        }

        fullCoverX = x
        fullCoverY = y
        fullCoverScale = targetView.width.toFloat() / imageView.width.toFloat()

        lockTransitionCornerRadius = lockCornerRadius
        if (lockCornerRadius) {
            transitionStartRadius = targetRadius
            transitionEndRadius = targetRadius
        } else {
            transitionStartRadius = startRadius
            transitionEndRadius = endRadius
        }

        transitionEndElevation = targetElevation ?: targetView.elevation
        updateTransitionFraction(fraction)
    }

    private val startPadding = 5F.dp.px
    private val endElevation = 24F.dp.px
    private val startRadius = 36F.dp.px
    private val endRadius = 10F.dp.px
    private var transitionStartRadius = startRadius
    private var transitionEndRadius = endRadius
    private var transitionEndElevation = endElevation

    private fun updateTransitionFraction(fraction: Float) {
        transitionImageView?.let {
            if (it.width != 0) {
                val rawDelta = fullScreenView.height - previewView.height - previewView.marginBottom
                val initialScale = previewCoverBoxMetrics / it.width.toFloat() * previewView.scaleX
                val initialTranslationX = + previewCoverMargin.toFloat() * previewView.scaleX - previewCoverHorizontalMargin * fraction
                val scale = lerp(initialScale, fullCoverScale, fraction)

                it.scaleX = scale
                it.scaleY = scale
                it.translationX = lerp(initialTranslationX, fullCoverX - previewCoverHorizontalMargin.toFloat(), fraction)
                it.translationY = lerp(previewCoverMargin.toFloat(), -rawDelta.toFloat() + fullCoverY, fraction)

                val targetVisualPadding = 0.5F.dp.px
                val dynamicStartPadding = if (initialScale > 0) targetVisualPadding / initialScale else 0F
                it.setPadding(lerp(dynamicStartPadding, 0F, fraction).toInt())

                it.elevation = lerp(0F, transitionEndElevation, fraction)
                val cornerRadius = if (lockTransitionCornerRadius && scale != 0F) {
                    transitionStartRadius / scale
                } else {
                    lerp(transitionStartRadius, transitionEndRadius, fraction)
                }
                it.updateCornerRadius(cornerRadius.toInt())
            }
        }
    }

    private fun updateTransform(newFraction: Float) {
        if (newFraction == fraction && fraction != 0F && fraction != 1F) return
        fraction = newFraction

        val deltaY = lerp(0f, (fullScreenView.height - previewView.height - previewView.marginBottom).toFloat(), fraction)

        // Preview
        previewView.scaleX = lerp(1f, fullScreenView.width.toFloat() / previewView.width, fraction)
        previewView.scaleY = previewView.scaleX
        previewView.translationY = -deltaY

        updateTransitionFraction(fraction)

        // Full
        fullScreenView.scaleX = (previewView.width * previewView.scaleX) / fullScreenView.width
        fullScreenView.scaleY = fullScreenView.scaleX
        fullScreenView.translationY = (fullScreenView.height - previewView.marginBottom - previewView.height - deltaY)
        fullScreenView.pivotY = 0f
        fullScreenView.pivotX = fullScreenView.width / 2f

        previewLeft = previewView.marginStart.toFloat()
        previewTop = (fullScreenView.height - previewView.height - previewView.marginBottom).toFloat()
        previewRight = fullScreenView.width.toFloat() - previewView.marginEnd
        previewBottom = fullScreenView.height.toFloat() - previewView.marginBottom

        fullLeft = 0f
        fullTop = 0f
        fullRight = fullScreenView.width.toFloat()
        fullBottom = fullScreenView.height.toFloat()

        boundLeft = lerp(previewLeft, fullLeft, fraction)
        boundTop = lerp(previewTop, fullTop, fraction)
        boundRight = lerp(previewRight, fullRight, fraction)
        boundBottom = lerp(previewBottom, fullBottom, fraction)

        path.reset()
        path.addRoundRect(
            boundLeft, boundTop, boundRight, boundBottom,
            panelCornerRadius,
            panelCornerRadius,
            Path.Direction.CW
        )

        contentRenderNode.setOutline(
            boundLeft.toInt(), boundTop.toInt(), boundRight.toInt(), boundBottom.toInt(),
            panelCornerRadius
        )

        previewView.alpha = lerp(1f, 0f, fraction * 2f)
        fullScreenView.alpha = lerp(0f, 1f, fraction * 2f)

        invalidate()
        triggerSlide(fraction)
    }

    private fun isInsideBoundingBox(x: Float, y: Float): Boolean {
        return x in boundLeft..boundRight && y in boundTop..boundBottom
    }

    override fun dispatchDraw(canvas: Canvas) {
        contentRenderNode.setPosition(0, 0, width, height)
        val recordingCanvas = contentRenderNode.beginRecording(width, height)
        recordingCanvas.drawPath(path, shadowPaint)
        super.dispatchDraw(recordingCanvas)
        contentRenderNode.endRecording()

        canvas.drawRenderNode(contentRenderNode)

        popupHelper.drawPopup(canvas)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return popupHelper.transformFraction == 1F
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (popupHelper.transformFraction == 1F &&
            !popupHelper.isInsidePopupMenu(event.x, event.y)) {
            popupHelper.callUpPopup(
                true,
                null,
                invalidate = {
                    invalidate()
                },
                doOnStart = {
                    fullScreenView.freeze()
                },
                doOnEnd = {
                    fullScreenView.unfreeze()
                }
            )
            return true
        }
        if (popupHelper.transformFraction != 0F) return true
        return if (isInsideBoundingBox(event.x, event.y) || isDragging) {
            if (gestureDetector.onTouchEvent(event)) {
                true
            } else if (event.action == MotionEvent.ACTION_UP) {
                onUp()
                true
            } else {
                super.onTouchEvent(event)
            }
        } else {
            false
        }
    }

    override fun dispatchApplyWindowInsets(platformInsets: WindowInsets): WindowInsets {
        if (initialMargin[3] != 0) return super.dispatchApplyWindowInsets(platformInsets)
        val insets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
        val floatingInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )
        if (floatingInsets.bottom != 0) {
            initialMargin = intArrayOf(
                previewView.marginLeft,
                previewView.marginTop,
                previewView.marginRight,
                previewView.marginBottom + floatingInsets.bottom
            )
            previewView.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = initialMargin[3]
            }
        }
        return super.dispatchApplyWindowInsets(platformInsets)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        isDragging = false
        val isSlidingUp = (penultimateMotionY - lastMotionY) > 0
        val lastVelocity = -(lastMotionY - penultimateMotionY) / (lastMotionTime - penultimateMotionTime) * SPEED_FACTOR
        val supposedDuration =
            ((fullTop - previewTop) / lastVelocity)
                .toLong()
                .absoluteValue
                .coerceIn(MINIMUM_ANIMATION_TIME, MAXIMUM_ANIMATION_TIME)

        if (state == SlideStatus.SLIDING) {
            flingValueAnimator?.cancel()
            flingValueAnimator = null

            ValueAnimator.ofFloat(
                fraction,
                if (isSlidingUp) 1.0F else 0F
            ).apply {
                flingValueAnimator = this
                interpolator = AnimationUtils.easingStandardInterpolator
                duration = supposedDuration

                addUpdateListener {
                    updateTransform(animatedValue as Float)
                }

                start()
            }
        }
        return true
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        isDragging = true

        flingValueAnimator?.cancel()
        flingValueAnimator = null

        val deltaY = - distanceY / (fullTop - previewTop)
        if (fraction + deltaY !in 0F..1F) { return true }

        updateTransform(fraction + deltaY)

        penultimateMotionY = lastMotionY
        penultimateMotionTime = lastMotionTime
        lastMotionY = e2.y
        lastMotionTime = e2.eventTime

        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (state == SlideStatus.COLLAPSED) {
            flingValueAnimator?.cancel()
            flingValueAnimator = null

            ValueAnimator.ofFloat(
                fraction,
                1F
            ).apply {
                flingValueAnimator = this
                duration = (MAXIMUM_ANIMATION_TIME + MINIMUM_ANIMATION_TIME) / 2
                interpolator = AnimationUtils.easingStandardInterpolator

                addUpdateListener {
                    updateTransform(animatedValue as Float)
                }

                start()
            }
        }
        return true
    }

    private fun onUp() {
        if (isDragging) {
            flingValueAnimator?.cancel()
            flingValueAnimator = null

            ValueAnimator.ofFloat(
                fraction,
                if (penultimateMotionY - lastMotionY > 0) 1.0F else 0F
            ).apply {
                flingValueAnimator = this
                duration = (MAXIMUM_ANIMATION_TIME + MINIMUM_ANIMATION_TIME) / 2
                interpolator = AnimationUtils.easingStandardInterpolator

                addUpdateListener {
                    updateTransform(animatedValue as Float)
                }

                start()
            }
        }

        isDragging = false
    }

    private fun triggerSlide(progress: Float) {
        onSlideListeners.forEach {
            it.onSlide(progress)
        }
        val prevState = state
        state = when (progress) {
            1.0F -> {
                transitionImageView?.visibility = INVISIBLE
                SlideStatus.EXPANDED
            }
            0.0F -> {
                transitionImageView?.visibility = INVISIBLE
                SlideStatus.COLLAPSED
            }
            else -> {
                transitionImageView?.visibility = VISIBLE
                SlideStatus.SLIDING
            }
        }
        if (prevState != state) {
            onSlideListeners.forEach { it.onSlideStatusChanged(state) }
        }
    }

    enum class SlideStatus {
        COLLAPSED, EXPANDED, SLIDING
    }

    interface OnSlideListener {
        fun onSlideStatusChanged(status: SlideStatus)
        fun onSlide(value: Float)
    }

    fun addOnSlideListener(listener: OnSlideListener) {
        onSlideListeners.add(listener)
    }

    fun setPreviewCover(drawable: Drawable?) {
        (previewView as? PreviewPlayer)?.setCover(drawable)
    }

    override fun onDetachedFromWindow() {
        onSlideListeners.clear()
        removeView(transitionImageView)
        transitionImageView = null
        super.onDetachedFromWindow()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState, fraction)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            fraction = state.savedValue
            doOnLayout {
                updateTransform(fraction)
            }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun recordPopupBackground(backgroundView: View): RenderNode? {
        if (width == 0 || height == 0 || backgroundView.width == 0 || backgroundView.height == 0) {
            return null
        }

        popupBackgroundRenderNode.setPosition(0, 0, width, height)
        val recordingCanvas = popupBackgroundRenderNode.beginRecording(width, height)

        val backgroundLocation = IntArray(2)
        val containerLocation = IntArray(2)
        backgroundView.getLocationOnScreen(backgroundLocation)
        getLocationOnScreen(containerLocation)

        val offsetX = backgroundLocation[0] - containerLocation[0]
        val offsetY = backgroundLocation[1] - containerLocation[1]

        recordingCanvas.translate(offsetX.toFloat(), offsetY.toFloat())
        backgroundView.draw(recordingCanvas)

        popupBackgroundRenderNode.endRecording()
        return popupBackgroundRenderNode
    }

    fun callUpPopup(
        entryList: PopupHelper.PopupEntries,
        locationX: Int,
        locationY: Int,
        anchorFromTop: Boolean = false,
        backgroundView: View? = null,
        dismissAction: (() -> Unit)? = null
    ) {
        val backgroundRenderNode = backgroundView?.let { recordPopupBackground(it) }
        popupHelper.callUpPopup(
            false,
            entryList,
            locationX,
            locationY,
            anchorFromTop,
            backgroundRenderNode = backgroundRenderNode,
            dismissAction = dismissAction,
            invalidate = {
                invalidate()
            },
            doOnStart = {
                fullScreenView.freeze()
            },
            doOnEnd = {
                fullScreenView.unfreeze()
            }
        )
    }

    override fun showPopupMenu(
        entries: PopupHelper.PopupEntries,
        locationX: Int,
        locationY: Int,
        anchorFromTop: Boolean,
        backgroundView: View?,
        onDismiss: (() -> Unit)?
    ) {
        callUpPopup(entries, locationX, locationY, anchorFromTop, backgroundView, onDismiss)
    }

    @Suppress("CanBeParameter")
    @Parcelize
    private class SavedState(val superStateInternal: Parcelable?, val savedValue: Float) : BaseSavedState(superStateInternal)

    companion object {
        const val MINIMUM_ANIMATION_TIME = 220L
        const val MAXIMUM_ANIMATION_TIME = 320L
        const val SPEED_FACTOR = 2F
    }

}
