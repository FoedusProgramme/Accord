package uk.akane.accord.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.animation.LinearInterpolator
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
import uk.akane.accord.logic.sp
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.cupertino.widget.dpToPx
import uk.akane.cupertino.widget.image.SimpleImageView
import uk.akane.cupertino.widget.utils.AnimationUtils
import kotlin.math.absoluteValue
import androidx.core.graphics.withSave

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
    private var currentPopupEntries: PopupEntries? = null

    private val popupHeight: Float
        get() = currentPopupEntries?.entries?.sumOf { it.heightInPx } ?: 0F

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

    private var popupRenderNodeDirty = true
    private var lastPopupWidth = 0
    private var lastPopupHeight = 0

    private var previewCoverBoxMetrics: Int = 0
    private var previewCoverMargin: Int = 8.dpToPx(context)
    private var previewCoverHorizontalMargin: Int = 12.dpToPx(context)

    private var fullCoverX: Int = 0
    private var fullCoverY: Int = 0

    private var state: SlideStatus = SlideStatus.COLLAPSED

    private var onSlideListeners: MutableList<OnSlideListener> = mutableListOf()

    private var popupDismissAction: (() -> Unit)? = null

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

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.bottomNavigationPanelColor, null)
        style = Paint.Style.FILL
    }

    @JvmInline
    value class PopupEntries(val entries: List<PopupEntry>)

    class PopupMenuBuilder {
        private val menuEntryList: MutableList<PopupEntry> = mutableListOf()

        fun addMenuEntry(resources: Resources, iconResId: Int, iconDescriptionResId: Int): PopupMenuBuilder {
            menuEntryList.add(
                MenuEntry(
                    ResourcesCompat.getDrawable(resources, iconResId, null)!!,
                    resources.getString(iconDescriptionResId)
                )
            )
            return this
        }

        fun addDestructiveMenuEntry(resources: Resources, iconResId: Int, iconDescriptionResId: Int): PopupMenuBuilder {
            menuEntryList.add(
                MenuEntry(
                    ResourcesCompat.getDrawable(resources, iconResId, null)!!,
                    resources.getString(iconDescriptionResId),
                    true
                )
            )
            return this
        }

        fun addSpacer(): PopupMenuBuilder {
            menuEntryList.add(Spacer())
            return this
        }

        fun build() = PopupEntries(menuEntryList)
    }

    data class MenuEntry(
        val icon: Drawable,
        val string: String,
        val isDestructive: Boolean = false
    ) : PopupEntry(44)

    class Spacer() : PopupEntry(8)

    abstract class PopupEntry(val heightInDp: Int) {
        val heightInPx: Float
            get() = heightInDp.dp.px
    }

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

    private fun isInsidePopupMenu(x: Float, y: Float): Boolean {
        return x in popupLeft..popupRight && y in popupTop..popupBottom
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
        entryList: PopupEntries?,
        locationX: Int = 0,
        locationY: Int = 0,
        dismissAction: (() -> Unit)? = null
    ) {
        if (popupTransformFraction != 0F && popupTransformFraction != 1F) {
            dismissAction?.invoke()
            return
        }

        if (locationX != 0) {
            popupInitialLocationX = locationX + 16.dp.px.toInt()
        }

        if (locationY != 0) {
            popupInitialLocationY = locationY - 12.dp.px.toInt()
        }

        if (entryList != null) {
            currentPopupEntries = entryList
        }

        if (dismissAction != null) {
            popupDismissAction = dismissAction
        }

        AnimationUtils.createValAnimator<Float>(
            if (isRetract) 1F else 0F,
            if (isRetract) 0F else 1F,
            duration = 300L,
            interpolator = AnimationUtils.linearInterpolator
        ) {
            popupTransformFraction = it
            invalidate()
        }
    }

    private fun calculatePopupBounds() {
        popupLeft = lerp(
            popupInitialLocationX - popupWidth * 0.1F,
            (popupInitialLocationX - popupWidth),
            popupTransformFraction
        ) { f -> AnimationUtils.linearOutSlowInInterpolator.getInterpolation(f) }
        popupTop = lerp(
            popupInitialLocationY - popupHeight * 0.1F,
            (popupInitialLocationY - popupHeight),
            popupTransformFraction
        ) { f -> AnimationUtils.fastOutSlowInInterpolator.getInterpolation(f) }

        val newWidth = (popupRight - popupLeft).toInt()
        val newHeight = (popupBottom - popupTop).toInt()

        if (newWidth != lastPopupWidth || newHeight != lastPopupHeight) {
            popupRenderNodeDirty = true
            lastPopupWidth = newWidth
            lastPopupHeight = newHeight

            popupRenderNode.setOutline(0, 0, newWidth, newHeight, popupRadius)
            popupRenderNode.setPosition(popupLeft.toInt(), popupTop.toInt(), popupRight.toInt(), popupBottom.toInt())
        }
    }

    private fun drawBlurredBackground() {
        if (!popupRenderNodeDirty) {
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
        color = popupColorDodge
    }

    private val contentColor = resources.getColor(R.color.onSurfaceColorSolid, null)
    private val destructiveColor = resources.getColor(R.color.red, null)

    private val separatorColor = resources.getColor(R.color.popupMenuSeparator, null)
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = separatorColor
    }

    private val largeSeparatorColor = resources.getColor(R.color.popupMenuLargeSeparator, null)
    private val largeSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = largeSeparatorColor
    }

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        textSize = 17.sp.px
        typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
    }

    private val popupForegroundShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = popupColorPlain
    }

    private fun drawPopup(canvas: Canvas) {
        val alphaInt = lerp(
            0F,
            255F,
            popupTransformFraction,
            { t -> AnimationUtils.accelerateDecelerateInterpolator.getInterpolation(t) }
        ).toInt()

        val layer = canvas.saveLayerAlpha(
            popupLeft,
            popupTop,
            popupRight,
            popupBottom,
            alphaInt
        )

        drawPopupRenderNode(canvas)
        drawForegroundShade(canvas)
        drawContent(canvas)

        canvas.restoreToCount(layer)
    }


    private fun drawPopupRenderNode(canvas: Canvas) {
        canvas.drawRenderNode(popupRenderNode)
    }

    private val horizontalMargin = 18.dp.px
    private val iconCenterRightMargin = 27.dp.px

    private fun drawContent(canvas: Canvas) {
        canvas.withSave {
            var heightAccumulated = 0F

            val scale = (popupInitialLocationX - popupLeft) / popupWidth

            canvas.scale(scale, scale, popupLeft, popupTop)

            val distanceX = popupLeft - (popupInitialLocationX - popupWidth)
            val distanceY = popupTop - (popupInitialLocationY - popupHeight)

            canvas.translate(
                distanceX,
                distanceY,
            )

            currentPopupEntries!!.entries.forEachIndexed { index, entry ->
                // Calculate bounds
                val entryLeft = popupInitialLocationX - popupWidth
                val entryTop = popupInitialLocationY - popupHeight + heightAccumulated
                val entryRight = popupRight
                val entryBottom = entryTop + entry.heightInPx

                heightAccumulated += entry.heightInPx

                when (entry) {
                    is Spacer -> {
                        canvas.drawRect(
                            entryLeft,
                            entryTop,
                            entryRight,
                            entryBottom,
                            largeSeparatorPaint
                        )
                    }

                    is MenuEntry -> {
                        textPaint.color =
                            if (entry.isDestructive) destructiveColor else contentColor

                        entry.icon.let { drawable ->
                            val iconWidth = entry.icon.intrinsicWidth
                            val iconLeft = entryRight - iconWidth / 2f - iconCenterRightMargin
                            val iconTop = (entryTop + entryBottom - iconWidth) / 2f
                            val iconRight = iconLeft + iconWidth
                            val iconBottom = iconTop + iconWidth

                            drawable.setTint(
                                if (entry.isDestructive) destructiveColor else contentColor
                            )
                            drawable.setBounds(
                                iconLeft.toInt(),
                                iconTop.toInt(),
                                iconRight.toInt(),
                                iconBottom.toInt()
                            )
                            drawable.draw(canvas)
                        }

                        entry.string.let { str ->
                            val fm = textPaint.fontMetrics
                            val textHeight = fm.descent - fm.ascent
                            val offsetY =
                                entryTop + (entryBottom - entryTop - textHeight) / 2f - fm.ascent

                            val textLeft = entryLeft + horizontalMargin
                            canvas.drawText(str, textLeft, offsetY, textPaint)
                        }


                        if (index != currentPopupEntries!!.entries.size - 1 &&
                            currentPopupEntries!!.entries[index + 1] !is Spacer
                        ) {
                            canvas.drawRect(
                                entryLeft,
                                entryBottom + 0.5.dp.px,
                                entryRight,
                                entryBottom,
                                separatorPaint
                            )
                        }
                    }
                }
            }

        }
    }

    private fun drawForegroundShade(canvas: Canvas) {
        if (isDarkMode()) {
            drawShade(popupForegroundPaint, BlendMode.COLOR_DODGE, canvas)
            drawShade(popupForegroundShadePaint, null, canvas)
        } else {
            drawShade(popupForegroundShadePaint, null, canvas)
            drawShade(popupForegroundPaint, BlendMode.COLOR_DODGE, canvas)
        }
    }

    fun drawShade(paint: Paint, blendMode: BlendMode? = null, canvas: Canvas) {
        paint.blendMode = blendMode
        canvas.drawRoundRect(
            popupLeft,
            popupTop,
            popupRight,
            popupBottom,
            popupRadius,
            popupRadius,
            paint
        )
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return popupTransformFraction == 1F
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (popupTransformFraction == 1F && !isInsidePopupMenu(event.x, event.y)) {
            callUpPopup(true, null)
            popupDismissAction?.invoke()
            return true
        }
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

    fun addOnSlideListener(listener: OnSlideListener) {
        onSlideListeners.add(listener)
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

    @Suppress("CanBeParameter")
    @Parcelize
    private class SavedState(val superStateInternal: Parcelable?, val savedValue: Float) : BaseSavedState(superStateInternal)

    companion object {
        const val MINIMUM_ANIMATION_TIME = 220L
        const val MAXIMUM_ANIMATION_TIME = 320L
        const val SPEED_FACTOR = 2F
    }

    private fun isDarkMode(): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

}