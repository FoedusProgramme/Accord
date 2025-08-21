package uk.akane.accord.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
    GestureDetector.OnGestureListener {

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

    private var transitionImageView: SimpleImageView? = null

    var panelCornerRadius = 0F

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.bottomNavigationPanelColor, null)
        style = Paint.Style.FILL
    }

    private var popupPath = Path()
    private val popupEntry = mutableListOf<PopupEntry>()
    private val popupHeight: Float
        get() = popupEntry.sumOf { it -> (it.heightInDp.dp.px) }

    inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
        var sum = 0f
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

    private val popupRenderNode = RenderNode("popupBlur").apply {
        clipToOutline = true
        setRenderEffect(
            RenderEffect.createBlurEffect(
                50.dp.px,
                50.dp.px,
                Shader.TileMode.MIRROR
            )
        )
    }

    private val popupWidth: Float
    private val popupRadius: Float

    private val popupColorDodge = resources.getColor(R.color.popupMenuColorDodge, null)
    private val popupColorPlain = resources.getColor(R.color.popupMenuPlain, null)

    private var popupLeft = 0F
    private var popupTop = 0F
    private val popupRight: Float
        get() = popupInitialLocationX.toFloat()
    private val popupBottom: Float
        get() = popupInitialLocationY.toFloat()

    private var popupTransformFraction = 0F
    private var popupInitialLocationX = 0
    private var popupInitialLocationY = 0
    private var popupCurrentRadius = 0F

    private var popupRenderNodeDirty = true
    private var lastPopupWidth = 0
    private var lastPopupHeight = 0


    data class MenuEntry(
        val iconRes: Int,
        val entryDescRes: Int
    ) : PopupEntry(44)

    class Spacer() : PopupEntry(8)

    abstract class PopupEntry(val heightInDp: Int)

    init {
        inflate(context, R.layout.layout_floating_panel, this)

        popupWidth = 250.dp.px
        popupRadius = 12.dp.px

        fullScreenView = findViewById(R.id.full_player)
        previewView = findViewById(R.id.preview_player)

        doOnLayout {
            panelCornerRadius = resources.getDimensionPixelSize(R.dimen.bottom_panel_radius).toFloat()
            // First init to sync the view location
            updateTransform(fraction)
        }
    }

    private var previewCoverBoxMetrics: Int = 0
    private var previewCoverMargin: Int = 8.dpToPx(context)
    private var previewCoverHorizontalMargin: Int = 12.dpToPx(context)

    private var fullCoverX: Int = 0
    private var fullCoverY: Int = 0

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

        transitionImageView?.let { it ->
            val constraintSet = ConstraintSet()
            constraintSet.clone(this)
            constraintSet.connect(it.id, ConstraintSet.START, previewView.id, ConstraintSet.START, 0)
            constraintSet.connect(it.id, ConstraintSet.TOP, previewView.id, ConstraintSet.TOP, 0)
            constraintSet.applyTo(this)

            it.pivotX = 0F
            it.pivotY = 0F

            it.visibility = INVISIBLE

            it.doOnLayout {
                updateTransitionFraction(fraction)
            }
        }
    }

    private val startPadding = 5F.dp.px
    private val endElevation = 24F.dp.px
    private val startRadius = 36F.dp.px
    private val endRadius = 10F.dp.px

    private fun updateTransitionFraction(fraction: Float) {
        transitionImageView?.let {
            if (it.width != 0) {
                val rawDelta = fullScreenView.height - previewView.height - previewView.marginBottom
                val initialScale = previewCoverBoxMetrics / it.width.toFloat() * previewView.scaleX
                val initialTranslationX = + previewCoverMargin.toFloat() * previewView.scaleX - previewCoverHorizontalMargin * fraction

                it.scaleX = lerp(initialScale, 1f, fraction)
                it.scaleY = lerp(initialScale, 1f, fraction)
                it.translationX = lerp(initialTranslationX, fullCoverX - previewCoverHorizontalMargin.toFloat(), fraction)
                it.translationY = lerp(previewCoverMargin.toFloat(), -rawDelta.toFloat() + fullCoverY, fraction)

                it.setPadding(lerp(startPadding, 0F, fraction).toInt())
                it.elevation = lerp(0F, endElevation, fraction)
                it.updateCornerRadius(lerp(startRadius, endRadius, fraction).toInt())
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

        previewView.alpha = lerp(1f, 0f, fraction)
        fullScreenView.alpha = lerp(0f, 1f, fraction)

        invalidate()
        triggerSlide(fraction)
    }

    private fun isInsideBoundingBox(x: Float, y: Float): Boolean {
        return x in boundLeft..boundRight && y in boundTop..boundBottom
    }

    private val contentRenderNode = RenderNode("content").apply {
        clipToOutline = true
    }

    override fun dispatchDraw(canvas: Canvas) {
        contentRenderNode.setPosition(0, 0, width, height)
        val recordingCanvas = contentRenderNode.beginRecording(width, height)
        recordingCanvas.drawPath(path, shadowPaint)
        super.dispatchDraw(recordingCanvas)
        contentRenderNode.endRecording()

        canvas.drawRenderNode(contentRenderNode)

        if (popupTransformFraction != 0f) {
            calculatePopupBounds()
            drawBlurredBackground()
            drawPopup(canvas)
        }
    }

    fun callUpPopup(
        isRetract: Boolean,
        entryList: List<PopupEntry>,
        locationX: Int = 0,
        locationY: Int = 0
    ) {
        popupInitialLocationX = locationX
        popupInitialLocationY = locationY

        popupEntry.clear()
        popupEntry.addAll(entryList)

        AnimationUtils.createValAnimator<Float>(
            if (isRetract) 1F else 0F,
            if (isRetract) 0F else 1F,
        ) {
            popupTransformFraction = it
            @Suppress("DEPRECATION")
            invalidate(
                popupLeft.toInt(),
                popupTop.toInt(),
                popupRight.toInt(),
                popupBottom.toInt()
            )
        }
    }

    private fun calculatePopupBounds() {
        popupLeft = lerp(
            popupInitialLocationX.toFloat(),
            (popupInitialLocationX - popupWidth),
            popupTransformFraction
        )
        popupTop = lerp(
            popupInitialLocationY.toFloat(),
            (popupInitialLocationY - popupHeight),
            popupTransformFraction
        )

        popupCurrentRadius = lerp(0F, popupRadius, popupTransformFraction)

        val newWidth = (popupRight - popupLeft).toInt()
        val newHeight = (popupBottom - popupTop).toInt()

        if (newWidth != lastPopupWidth || newHeight != lastPopupHeight) {
            popupRenderNodeDirty = true
            lastPopupWidth = newWidth
            lastPopupHeight = newHeight

            popupRenderNode.setOutline(0, 0, newWidth, newHeight, popupCurrentRadius)
            popupRenderNode.setPosition(popupLeft.toInt(), popupTop.toInt(), popupRight.toInt(), popupBottom.toInt())
        }
    }

    private fun drawBlurredBackground() {
        if (!popupRenderNodeDirty) {
            popupRenderNode.setPosition(
                popupLeft.toInt(),
                popupTop.toInt(),
                popupRight.toInt(),
                popupBottom.toInt()
            )
            return
        }

        popupRenderNode.setPosition(
            popupLeft.toInt(),
            popupTop.toInt(),
            popupRight.toInt(),
            popupBottom.toInt()
        )

        val recordingCanvas = popupRenderNode.beginRecording(lastPopupWidth, lastPopupHeight)
        recordingCanvas.translate(-popupLeft, -popupTop)
        recordingCanvas.drawRenderNode(contentRenderNode)
        recordingCanvas.translate(popupLeft, popupTop)
        popupRenderNode.endRecording()

        popupRenderNodeDirty = false
    }


    private val popupForegroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private fun drawPopup(canvas: Canvas) {
        canvas.drawRenderNode(popupRenderNode)

        popupForegroundPaint.color = popupColorDodge
        popupForegroundPaint.blendMode = BlendMode.COLOR_DODGE
        canvas.drawRoundRect(
            popupLeft,
            popupTop,
            popupRight,
            popupBottom,
            popupCurrentRadius,
            popupCurrentRadius,
            popupForegroundPaint
        )

        popupForegroundPaint.color = popupColorPlain
        popupForegroundPaint.blendMode = null
        canvas.drawRoundRect(
            popupLeft,
            popupTop,
            popupRight,
            popupBottom,
            popupCurrentRadius,
            popupCurrentRadius,
            popupForegroundPaint
        )
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
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
        if (fraction + deltaY < 0F || fraction + deltaY > 1F) { return true }

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

    private var state: SlideStatus = SlideStatus.COLLAPSED

    fun addOnSlideListener(listener: OnSlideListener) {
        onSlideListeners.add(listener)
    }

    override fun onDetachedFromWindow() {
        onSlideListeners.clear()
        removeView(transitionImageView)
        transitionImageView = null
        super.onDetachedFromWindow()
    }

    private var onSlideListeners: MutableList<OnSlideListener> = mutableListOf()

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

    @Suppress("CanBeParameter")
    @Parcelize
    private class SavedState(val superStateInternal: Parcelable?, val savedValue: Float) : BaseSavedState(superStateInternal)

    companion object {
        const val MINIMUM_ANIMATION_TIME = 220L
        const val MAXIMUM_ANIMATION_TIME = 320L
        const val SPEED_FACTOR = 2F
    }

}