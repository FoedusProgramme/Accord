package uk.akane.accord.ui.components

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.inverseLerp
import uk.akane.accord.logic.isDarkMode
import uk.akane.accord.logic.sp
import uk.akane.accord.ui.MainActivity

class NavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes),
    DefaultLifecycleObserver {

    private val activity: MainActivity
        get() = context as MainActivity

    private val accentColor = resources.getColor(R.color.accentColor, null)

    private val bottomDividerColor = resources.getColor(R.color.navigationBarDivider, null)
    private val bottomDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bottomDividerColor }

    private val avatarColor = accentColor
    private val avatarDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_person_navigation_bar, null)!!

    private val ellipsisBackgroundColor = resources.getColor(R.color.navigationBarEllipsisBackground, null)
    private val ellipsisBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ellipsisBackgroundColor }

    private val ellipsisColor = accentColor
    private val ellipsisDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_ellipsis_navigation_bar, null)!!

    private val chevronColor = accentColor
    private val chevronDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_chevron_left, null)!!

    private val expandedNavigationBarBackgroundColor = resources.getColor(R.color.navigationBarExpandedBackground, null)

    private val blurAppendColor = resources.getColor(R.color.navigationBarBlurAppendColor, null)
    private val blurAppendColorDark = resources.getColor(R.color.navigationBarBlurAppendDarkModeColor, null)

    private var titleText = ""
    private var returnButtonText = ""

    private var lifecycle: Lifecycle? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.NavigationBar, 0, 0) {
            titleText = getString(R.styleable.NavigationBar_title) ?: ""
            returnButtonText = getString(R.styleable.NavigationBar_returnButtonText) ?: ""
            shouldDrawLargeMenuItem = getBoolean(R.styleable.NavigationBar_hasLargeMenuItems, true)
            shouldDrawReturnButton = getBoolean(R.styleable.NavigationBar_hasReturnButton, false)
            Log.d("TAG", "should: $shouldDrawLargeMenuItem")
        }
        setWillNotDraw(false)
        isClickable = true
    }

    private var collapseProgress = 0F
    private var renderShowProgress = 0F
    private var scrollOffsetPx = 0

    var blurRadius: Float = 0F
        set(value) {
            field = value
            renderNode?.setRenderEffect(
                RenderEffect.createBlurEffect(
                    value,
                    value,
                    Shader.TileMode.MIRROR
                )
            )
        }

    var shouldDrawLargeMenuItem: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var shouldDrawReturnButton: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var renderNode: RenderNode? = null

    private val expandedTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.navigationBarTitle, null)
        textSize = 34.sp.px
        textAlign = Paint.Align.LEFT
        typeface = ResourcesCompat.getFont(context, R.font.inter_bold)
    }
    private val expandedTitleFontMetrics by lazy { expandedTitlePaint.fontMetrics }
    private val collapsedTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.navigationBarTitle, null)
        textSize = 18.sp.px
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
    }

    private var appendHeight = 0
    private var childTopMargin = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val child = getChildAt(0)
        if (child != null) {
            val lp = child.layoutParams as MarginLayoutParams
            childTopMargin = lp.topMargin
            val childWidth = widthSize - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin
            child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(child.measuredHeight, MeasureSpec.EXACTLY)
            )
            appendHeight = child.measuredHeight + lp.bottomMargin + lp.topMargin
        }

        val desiredHeight = EXPANDED_STATE_HEIGHT.dp.px.toInt() +
                calculateExpandedHeightPadding().toInt() +
                (if (shouldDrawReturnButton) EXPANDED_PADDED_HEIGHT_RETURN else 0).dp.px.toInt() +
                paddingTop + paddingBottom +
                appendHeight

        val measuredHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            MeasureSpec.UNSPECIFIED -> desiredHeight
            else -> desiredHeight
        }

        val measuredWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize
            MeasureSpec.UNSPECIFIED -> widthSize
            else -> widthSize
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (renderShowProgress < 1F) {
            drawExpandedBackground(canvas)
        }

        val layer = canvas.saveLayerAlpha(
            0F,
            0F,
            width.toFloat(),
            height.toFloat(),
            ((1f - collapseProgress) * 255).toInt()
        )

        drawExpandedTitle(canvas)

        Log.d("TAG", "invalidated! $shouldDrawLargeMenuItem")
        if (shouldDrawLargeMenuItem) {
            drawMenuItems(canvas)
        }

        canvas.restoreToCount(layer)

        if (renderShowProgress > 0F) {

            val layerRender = canvas.saveLayerAlpha(
                0F,
                0F,
                width.toFloat(),
                height.toFloat(),
                (renderShowProgress * 255).toInt()
            )

            canvas.withTranslation(0F, -translationY) {
                renderNode?.let {
                    canvas.drawRenderNode(it)
                }
            }

            drawRenderNodeOverlay(canvas)
            drawBottomDivider(canvas)

            canvas.restoreToCount(layerRender)

        }

        drawCollapsedTitle(canvas)

        if (shouldDrawReturnButton) {
            canvas.withTranslation(y = -translationY) {
                drawReturnButton(canvas)
            }
        }
    }

    private fun drawRenderNode() {
        renderNode?.beginRecording(renderNodeWidth, renderNodeHeight)?.apply {
            withSave {
                drawColor(expandedNavigationBarBackgroundColor)
                targetView?.draw(this)
            }
        }
        renderNode?.endRecording()
    }

    private fun drawRenderNodeOverlay(canvas: Canvas) {
        if (context.isDarkMode()) {
            canvas.drawColor(blurAppendColor, BlendMode.OVERLAY)
            canvas.drawColor(blurAppendColorDark)
        } else {
            canvas.drawColor(blurAppendColor, BlendMode.HARD_LIGHT)
        }
    }

    private fun drawExpandedBackground(canvas: Canvas) {
        canvas.drawColor(expandedNavigationBarBackgroundColor)
    }

    private fun drawBottomDivider(canvas: Canvas) {
        canvas.drawRect(
            0F,
            height - DIVIDER_SIZE.dp.px,
            width.toFloat(),
            height.toFloat(),
            bottomDividerPaint
        )
    }

    private fun drawExpandedTitle(canvas: Canvas) {
        val topY = paddingTop + calculateExpandedHeightPadding() + 3f.dp.px + (if (shouldDrawReturnButton) EXPANDED_PADDED_HEIGHT_RETURN else 0).dp.px.toInt()

        val baseline = topY - expandedTitleFontMetrics.ascent

        canvas.drawText(
            titleText,
            EXPANDED_SIDE_PADDING.dp.px,
            baseline,
            expandedTitlePaint
        )
    }

    private val returnTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = chevronColor
        textSize = 18.sp.px
        textAlign = Paint.Align.LEFT
        typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        isSubpixelText = true
    }

    private fun drawCollapsedTitle(canvas: Canvas) {
        val paint = collapsedTitlePaint
        val text = titleText

        val x = width / 2f

        val fm = paint.fontMetrics

        val collapseExpandDiff = (EXPANDED_STATE_HEIGHT - COLLAPSED_STATE_HEIGHT).dp.px
        val returnButtonTopTranslation = (if (shouldDrawReturnButton) (EXPANDED_PADDED_HEIGHT_RETURN) else 0).dp.px.toInt()

        val collapsedTop = paddingTop + calculateExpandedHeightPadding() + collapseExpandDiff + returnButtonTopTranslation
        val collapsedBottom = collapsedTop + COLLAPSED_STATE_HEIGHT.dp.px + childTopMargin
        val centerY = (collapsedTop + collapsedBottom) / 2f + COLLAPSED_TITLE_Y_OFFSET.dp.px

        val baseline = centerY - (fm.ascent + fm.descent) / 2f

        paint.alpha = (255 * collapseProgress).toInt()

        canvas.drawText(text, x, baseline, paint)

        /*

         */
    }

    private fun drawReturnButton(canvas: Canvas) {
        val chevronWidth = chevronDrawable.intrinsicWidth
        val chevronHeight = (chevronWidth * (chevronDrawable.intrinsicHeight.toFloat() /
                chevronDrawable.intrinsicWidth.toFloat())).toInt()

        val centerY = paddingTop + (EXPANDED_PADDED_HEIGHT_RETURN.dp.px / 2f) + returnRowYOffset

        val chevronLeft = EXPANDED_PADDED_HEIGHT_RETURN_START_PADDING.dp.px.toInt()
        val chevronTop = (centerY - chevronHeight / 2f).toInt()

        chevronDrawable.setBounds(
            chevronLeft,
            chevronTop,
            chevronLeft + chevronWidth,
            chevronTop + chevronHeight
        )
        chevronDrawable.setTint(chevronColor)
        chevronDrawable.draw(canvas)

        val fm = returnTextPaint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        val textX = chevronLeft + chevronWidth + EXPANDED_PADDED_HEIGHT_RETURN_START_PADDING.dp.px
        canvas.drawText(returnButtonText, textX, baseline, returnTextPaint)
    }

    private fun drawMenuItems(canvas: Canvas) {
        val size = EXPANDED_MENU_ITEM_SIZE.dp.px
        val ellipsisSize = 18.dp.px

        val textCenterY = if (shouldDrawReturnButton) {
            paddingTop + (EXPANDED_PADDED_HEIGHT_RETURN.dp.px / 2f) + returnRowYOffset
        } else {
            val topY = paddingTop + calculateExpandedHeightPadding() + 3f.dp.px
            val baseline = topY - expandedTitleFontMetrics.ascent
            baseline + (expandedTitleFontMetrics.ascent + expandedTitleFontMetrics.descent) / 2f
        }

        val drawableLeft = width - EXPANDED_SIDE_PADDING.dp.px - size
        val drawableTop = textCenterY - size / 2f
        val drawableRight = drawableLeft + size
        val drawableBottom = drawableTop + size

        val shouldDrawAvatar = !shouldDrawReturnButton
        val ellipsisLeft = if (shouldDrawAvatar) {
            drawableLeft - size - EXPANDED_ELLIPSIS_MARGIN.dp.px
        } else {
            drawableLeft
        }
        val ellipsisTop = drawableTop
        val ellipsisRight = ellipsisLeft + size
        val ellipsisBottom = ellipsisTop + size

        val ellipsisCenterX = (ellipsisLeft + ellipsisRight) / 2f
        val ellipsisCenterY = (ellipsisTop + ellipsisBottom) / 2f

        val ellipsisDrawableLeft = (ellipsisCenterX - ellipsisSize / 2f).toInt()
        val ellipsisDrawableTop = (ellipsisCenterY - ellipsisSize / 2f).toInt()
        val ellipsisDrawableRight = ellipsisDrawableLeft + ellipsisSize
        val ellipsisDrawableBottom = ellipsisDrawableTop + ellipsisSize

        ellipsisDrawable.setBounds(
            ellipsisDrawableLeft,
            ellipsisDrawableTop,
            ellipsisDrawableRight.toInt(),
            ellipsisDrawableBottom.toInt()
        )

        if (shouldDrawAvatar) {
            avatarDrawable.setBounds(
                drawableLeft.toInt(),
                drawableTop.toInt(),
                drawableRight.toInt(),
                drawableBottom.toInt()
            )

            avatarDrawable.setTint(avatarColor)
            avatarDrawable.draw(canvas)
        }

        canvas.drawRoundRect(
            ellipsisLeft,
            ellipsisTop,
            ellipsisRight,
            ellipsisBottom,
            size / 2f,
            size / 2f,
            ellipsisBackgroundPaint
        )

        ellipsisDrawable.setTint(ellipsisColor)
        ellipsisDrawable.draw(canvas)
    }

    private var renderNodeWidth = 0
    private var renderNodeHeight = 0

    private var targetView: View? = null
    private var returnClickListener: (() -> Unit)? = null
    private var returnButtonPressed = false
    private val returnButtonBounds = RectF()
    private val returnRowYOffset = RETURN_ROW_Y_OFFSET.dp.px

    fun setOnReturnClickListener(listener: (() -> Unit)?) {
        returnClickListener = listener
    }

    fun attach(view: RecyclerView) {
        targetView = view
        renderNode = RenderNode("BlurredTarget")
        blurRadius = BLUR_STRENGTH.dp.px

        doOnLayout {
            lifecycle = findViewTreeLifecycleOwner()?.lifecycle
            lifecycle?.addObserver(this)

            view.clipToPadding = false
            view.setPadding(
                view.paddingLeft,
                view.paddingTop + height,
                view.paddingEnd,
                activity.bottomHeight
            )

            drawRenderNode()

            view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    var shouldInvalidate = false

                    scrollOffsetPx = (scrollOffsetPx + dy).coerceAtLeast(0)
                    val offset = scrollOffsetPx
                    val maxOffset = height - paddingTop - COLLAPSED_STATE_HEIGHT.dp.px - appendHeight
                    val dstTranslationY = (-offset.toFloat()).coerceAtLeast(-maxOffset)

                    val secondStageOffsetEnd = EXPANDED_STATE_HEIGHT.dp.px + calculateExpandedHeightPadding() + (if (shouldDrawReturnButton) EXPANDED_PADDED_HEIGHT_RETURN else 0).dp.px.toInt() -
                            COLLAPSED_STATE_HEIGHT.dp.px + paddingTop + COLLAPSED_STATE_HEIGHT.dp.px + appendHeight * 2
                    val secondStageProgress = inverseLerp(-maxOffset, -secondStageOffsetEnd, -offset.toFloat()).coerceIn(0F, 1F)

                    drawRenderNode()

                    if (dstTranslationY != translationY) {
                        shouldInvalidate = true
                        translationY = dstTranslationY
                        collapseProgress = if (maxOffset > 0) {
                            (offset / maxOffset).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    }

                    if (secondStageProgress != renderShowProgress) {
                        shouldInvalidate = true
                        renderShowProgress = secondStageProgress
                    }

                    if (shouldInvalidate) {
                        invalidate()
                    }
                }
            })

            if (view.computeVerticalScrollOffset() == height) {
                view.scrollBy(0, -height)
            }

            renderNodeWidth = width.takeIf { it > 0 } ?: view.width
            renderNodeHeight = (paddingTop + COLLAPSED_STATE_HEIGHT.dp.px + appendHeight).takeIf { it > 0 }?.toInt() ?: view.height

            renderNode?.setPosition(0, 0, renderNodeWidth, renderNodeHeight)

            scrollOffsetPx = view.computeVerticalScrollOffset()
        }
    }

    private fun updateReturnButtonBounds(): RectF {
        val chevronWidth = chevronDrawable.intrinsicWidth
        val chevronHeight = (chevronWidth * (chevronDrawable.intrinsicHeight.toFloat() /
                chevronDrawable.intrinsicWidth.toFloat())).toInt()

        val translationYOffset = -translationY
        val centerY = paddingTop + (EXPANDED_PADDED_HEIGHT_RETURN.dp.px / 2f) +
            returnRowYOffset + translationYOffset
        val chevronLeft = EXPANDED_PADDED_HEIGHT_RETURN_START_PADDING.dp.px
        val chevronTop = centerY - chevronHeight / 2f
        val textX = chevronLeft + chevronWidth + EXPANDED_PADDED_HEIGHT_RETURN_START_PADDING.dp.px
        val textWidth = returnTextPaint.measureText(returnButtonText)
        val hitPadding = 8.dp.px

        returnButtonBounds.set(
            (chevronLeft - hitPadding).coerceAtLeast(0f),
            (chevronTop - hitPadding).coerceAtLeast(0f),
            (textX + textWidth + hitPadding).coerceAtMost(width.toFloat()),
            (chevronTop + chevronHeight + hitPadding).coerceAtMost(height.toFloat())
        )
        return returnButtonBounds
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!shouldDrawReturnButton) return super.onInterceptTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_DOWN &&
            updateReturnButtonBounds().contains(ev.x, ev.y)
        ) {
            returnButtonPressed = true
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!shouldDrawReturnButton) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                returnButtonPressed = updateReturnButtonBounds().contains(event.x, event.y)
                return returnButtonPressed
            }
            MotionEvent.ACTION_UP -> {
                if (returnButtonPressed && updateReturnButtonBounds().contains(event.x, event.y)) {
                    returnClickListener?.invoke()
                    performClick()
                }
                returnButtonPressed = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                returnButtonPressed = false
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount != 1) return

        val child = getChildAt(0)
        val lp = child.layoutParams as? MarginLayoutParams

        val marginStart = lp?.marginStart ?: 0
        val marginEnd = lp?.marginEnd ?: 0
        val marginTop = lp?.topMargin ?: 0

        val childLeft = paddingLeft + marginStart
        val childRight = width - paddingRight - marginEnd
        val childTop = (paddingTop + EXPANDED_STATE_HEIGHT.dp.px + calculateExpandedHeightPadding() +
            (if (shouldDrawReturnButton) EXPANDED_PADDED_HEIGHT_RETURN else 0).dp.px.toInt()
            ).toInt() + marginTop + COLLAPSED_CHILD_TOP_PADDING.dp.px.toInt()
        val childBottom = childTop + child.measuredHeight

        child.layout(childLeft, childTop, childRight, childBottom)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        refreshRenderNode()
    }

    fun onVisibilityChangedFromFragment(isHidden: Boolean) {
        if (!isHidden) {
            refreshRenderNode()
        }
    }

    private fun refreshRenderNode() {
        if (collapseProgress > 0F) {
            post {
                drawRenderNode()
                invalidate()
            }
        }
    }

    private fun calculateExpandedHeightPadding() =
        EXPANDED_PADDED_HEIGHT.dp.px + EXPANDED_PADDED_HEIGHT_APPEND_RETURN.dp.px

    companion object {
        const val EXPANDED_PADDED_HEIGHT = 22
        const val EXPANDED_PADDED_HEIGHT_APPEND_RETURN = -10
        const val EXPANDED_PADDED_HEIGHT_RETURN = 44
        const val EXPANDED_PADDED_HEIGHT_RETURN_START_PADDING = 10
        const val EXPANDED_STATE_HEIGHT = 52
        const val EXPANDED_SIDE_PADDING = 22

        const val EXPANDED_MENU_ITEM_SIZE = 30
        const val EXPANDED_ELLIPSIS_MARGIN = 18
        const val RETURN_ROW_Y_OFFSET = 8

        const val COLLAPSED_STATE_HEIGHT = 44
        const val COLLAPSED_TITLE_Y_OFFSET = 8
        const val COLLAPSED_CHILD_TOP_PADDING = 8
        const val DIVIDER_SIZE = 0.5F
        const val BLUR_STRENGTH = 50F
    }
}
