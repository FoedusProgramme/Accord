package uk.akane.accord.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withSave
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.isDarkMode
import uk.akane.accord.logic.setOutline
import uk.akane.accord.logic.sp
import uk.akane.accord.logic.sumOf
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.cupertino.widget.utils.AnimationUtils

class PopupHelper(
    private val context: Context,
    private val contentRenderNode: RenderNode
) {

    private var currentPopupEntries: PopupEntries? = null

    // Popup properties
    private val popupWidth: Float = 270.dp.px
    private val popupHeight: Float
        get() = currentPopupEntries?.entries?.sumOf { it.heightInPx } ?: 0F
    private val popupRadius: Float = 14.dp.px
    private val popupItemHorizontalMargin = 18.dp.px
    private val popupIconCenterToRightMargin = 27.dp.px

    // Popup colors
    private val popupColorDodge = context.getColor(R.color.popupMenuColorDodge)
    private val popupColorPlain = context.getColor(R.color.popupMenuPlain)
    private val contentColor = context.getColor(R.color.onSurfaceColorSolid)
    private val destructiveColor = context.getColor(R.color.red)
    private val separatorColor = context.getColor(R.color.popupMenuSeparator)
    private val largeSeparatorColor = context.getColor(R.color.popupMenuLargeSeparator)

    // Popup locations

    // Initial
    private var popupInitialLocationX = 0
    private var popupInitialLocationY = 0

    // Motion
    private var popupLeft = 0F
    private var popupTop = 0F
    private var popupRight = 0F
    private var popupBottom = 0F

    // Last popup
    private var lastPopupWidth = 0
    private var lastPopupHeight = 0

    // Progress
    private var popupTransformFraction = 0F
    private var popupRenderNodeDirty = true
    val transformFraction: Float
        get() = popupTransformFraction
    private var popupAnchorFromTop = false
    private var popupBackgroundRenderNode: RenderNode? = null

    // Called upon dismiss
    private var popupDismissAction: (() -> Unit)? = null

    // Animator
    private var popupAnimator: ValueAnimator? = null

    // Popup paint
    private val popupForegroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = popupColorDodge }
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = separatorColor }
    private val largeSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = largeSeparatorColor }
    private val popupForegroundShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = popupColorPlain }

    // Content paint
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        textSize = 18.sp.px
        typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
    }
    val textPaintFontMetrics: Paint.FontMetrics by lazy { textPaint.fontMetrics }

    // RenderNode
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

    // Exposed methods
    fun isInsidePopupMenu(x: Float, y: Float): Boolean {
        return x in popupLeft..popupRight && y in popupTop..popupBottom
    }

    fun drawPopup(canvas: Canvas) {
        if (popupTransformFraction != 0f) {
            calculatePopupBounds()
            if (!popupRenderNode.hasDisplayList()) {
                drawBlurredBackground()
            }
            drawPopupContent(canvas)
        }
    }


    fun callUpPopup(
        isRetract: Boolean,
        entryList: PopupEntries?,
        locationX: Int = 0,
        locationY: Int = 0,
        anchorFromTop: Boolean = false,
        backgroundRenderNode: RenderNode? = null,
        dismissAction: (() -> Unit)? = null,
        invalidate: (() -> Unit),
        doOnStart: (() -> Unit),
        doOnEnd: (() -> Unit)
    ) {
        if (popupTransformFraction != 0F && popupTransformFraction != 1F) {
            dismissAction?.invoke()
            return
        }

        if (!isRetract) {
            popupInitialLocationX = locationX + 16.dp.px.toInt()
            popupInitialLocationY = locationY - 12.dp.px.toInt()
            popupAnchorFromTop = anchorFromTop
            popupBackgroundRenderNode = backgroundRenderNode
        } else {
            popupBackgroundRenderNode = null
        }

        if (entryList != null) {
            currentPopupEntries = entryList
        }

        if (dismissAction != null) {
            popupDismissAction = dismissAction
        }

        if (isRetract) {
            popupDismissAction?.invoke()
        }

        popupAnimator?.cancel()
        popupAnimator = null

        if (!isRetract) {
            popupRenderNode.discardDisplayList()
        }

        val renderTop = if (popupAnchorFromTop) {
            popupInitialLocationY.toFloat()
        } else {
            popupInitialLocationY - popupHeight
        }
        val renderBottom = if (popupAnchorFromTop) {
            popupInitialLocationY + popupHeight
        } else {
            popupInitialLocationY.toFloat()
        }

        popupRenderNode.setPosition(
            (popupInitialLocationX - popupWidth).toInt(),
            renderTop.toInt(),
            popupInitialLocationX,
            renderBottom.toInt()
        )

        doOnStart.invoke()

        popupAnimator = AnimationUtils.createValAnimator<Float>(
            if (isRetract) 1F else 0F,
            if (isRetract) 0F else 1F,
            duration = 300L,
            interpolator = AnimationUtils.linearInterpolator,
            doOnEnd = {
                doOnEnd.invoke()
            },
            doOnCancel = {
                doOnEnd.invoke()
            }
        ) {
            popupTransformFraction = it
            invalidate()
        }
    }

    // Internal method for call up drawing
    private fun drawPopupContent(canvas: Canvas) {
        val alphaInt = lerp(
            0F,
            255F,
            popupTransformFraction
        ) { t -> AnimationUtils.accelerateDecelerateInterpolator.getInterpolation(t) }.toInt()

        val layer = canvas.saveLayerAlpha(
            popupLeft,
            popupTop,
            popupRight,
            popupBottom,
            alphaInt
        )

        canvas.drawRenderNode(popupRenderNode)
        drawForegroundShade(canvas)
        drawItemContent(canvas)

        canvas.restoreToCount(layer)
    }

    // Internal calculations
    private fun calculatePopupBounds() {
        popupLeft = lerp(
            popupInitialLocationX - popupWidth * 0.1F,
            (popupInitialLocationX - popupWidth),
            popupTransformFraction
        ) { f -> AnimationUtils.linearOutSlowInInterpolator.getInterpolation(f) }
        popupRight = popupInitialLocationX.toFloat()

        if (popupAnchorFromTop) {
            popupTop = popupInitialLocationY.toFloat()
            popupBottom = lerp(
                popupInitialLocationY + popupHeight * 0.1F,
                popupInitialLocationY + popupHeight,
                popupTransformFraction
            ) { f -> AnimationUtils.fastOutSlowInInterpolator.getInterpolation(f) }
        } else {
            popupTop = lerp(
                popupInitialLocationY - popupHeight * 0.1F,
                (popupInitialLocationY - popupHeight),
                popupTransformFraction
            ) { f -> AnimationUtils.fastOutSlowInInterpolator.getInterpolation(f) }
            popupBottom = popupInitialLocationY.toFloat()
        }

        val newWidth = (popupRight - popupLeft).toInt()
        val newHeight = (popupBottom - popupTop).toInt()

        if (newWidth != lastPopupWidth || newHeight != lastPopupHeight) {
            popupRenderNodeDirty = true
            lastPopupWidth = newWidth
            lastPopupHeight = newHeight

            popupRenderNode.setOutline(
                (popupLeft - popupInitialLocationX + popupWidth).toInt(),
                if (popupAnchorFromTop) {
                    (popupTop - popupInitialLocationY).toInt()
                } else {
                    (popupTop - popupInitialLocationY + popupHeight).toInt()
                },
                popupWidth.toInt(),
                popupHeight.toInt(),
                popupRadius)
        }
    }

    private fun drawBlurredBackground() {
        if (!popupRenderNodeDirty) return

        val recordingCanvas = popupRenderNode.beginRecording(popupWidth.toInt(), popupHeight.toInt())
        val translateY = if (popupAnchorFromTop) {
            -popupInitialLocationY.toFloat()
        } else {
            -popupInitialLocationY + popupHeight
        }
        recordingCanvas.translate(-popupInitialLocationX + popupWidth, translateY)
        recordingCanvas.drawRenderNode(popupBackgroundRenderNode ?: contentRenderNode)

        popupRenderNode.endRecording()
        popupRenderNodeDirty = false
    }


    private fun drawItemContent(canvas: Canvas) {
        canvas.withSave {
            var heightAccumulated = 0F

            val scale = (popupInitialLocationX - popupLeft) / popupWidth

            canvas.scale(scale, scale, popupLeft, popupTop)

            val distanceX = popupLeft - (popupInitialLocationX - popupWidth)
            val distanceY = popupTop - if (popupAnchorFromTop) {
                popupInitialLocationY.toFloat()
            } else {
                (popupInitialLocationY - popupHeight)
            }

            canvas.translate(
                distanceX,
                distanceY,
            )

            currentPopupEntries!!.entries.forEachIndexed { index, entry ->
                // Calculate bounds
                val entryLeft = popupInitialLocationX - popupWidth
                val entryTop = (if (popupAnchorFromTop) {
                    popupInitialLocationY.toFloat()
                } else {
                    popupInitialLocationY - popupHeight
                }) + heightAccumulated
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
                            val iconLeft = entryRight - iconWidth / 2f - popupIconCenterToRightMargin
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
                            val fm = textPaintFontMetrics
                            val textHeight = fm.descent - fm.ascent
                            val offsetY =
                                entryTop + (entryBottom - entryTop - textHeight) / 2f - fm.ascent

                            val textLeft = entryLeft + popupItemHorizontalMargin
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
        if (context.isDarkMode()) {
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

    // Helper classes
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
    ) : PopupEntry(48)

    class Spacer() : PopupEntry(10)

    abstract class PopupEntry(val heightInDp: Int) {
        val heightInPx by lazy { heightInDp.dp.px }
    }

}
