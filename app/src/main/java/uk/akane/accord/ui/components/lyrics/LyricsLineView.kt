package uk.akane.accord.ui.components.lyrics

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.res.ResourcesCompat
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.floatAnimator
import uk.akane.accord.logic.sp
import uk.akane.cupertino.widget.utils.AnimationUtils
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("ViewConstructor")
class LyricsLineView internal constructor(
    context: Context,
    private val line: LyricsLine
) : View(context) {
    private val horizontalPadding = 32.dp.px
    private val verticalPadding = 14.dp.px

    lateinit var animations: Animations
        private set

    private lateinit var staticLayout: StaticLayout
    private val paint = TextPaint().apply {
        textSize = 34.sp.px
        color = Color.WHITE
        typeface = ResourcesCompat.getFont(context, R.font.inter_bold)
    }
    private val contentPaint = Paint().apply {
        xfermode = AnimationUtils.addXfermode
    }

    private val blurRenderNode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RenderNode(BLUR_NODE_NAME)
    } else {
        null
    }

    var textOffset: Float = 0f
        set(value) {
            if (field == value) return
            field = value
            translationY = value
        }

    var textAlpha: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blurRenderNode?.alpha = textAlpha
            } else {
                alpha = value
            }
        }

    var textScale: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var blurRadius: Float = 0f
        set(value) {
            if (field == value) return
            field = value
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            val roundedValue = value.roundToInt()
            val renderEffect =
                if (roundedValue == 0) null
                else blurs?.get(roundedValue)
            blurRenderNode?.setRenderEffect(renderEffect)
        }

    init {
        isClickable = true
        isFocusable = true
        foreground = RippleDrawable(rippleColorStateList, null, null)
        contentDescription = line.text
    }

    fun setAnimations(index: Int, globalOffset: Float, deviceHeight: Float) {
        animations = Animations(index, globalOffset, deviceHeight)
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blurRenderNode?.discardDisplayList()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (height < 0 || width < 0) return
        val hP = horizontalPadding.roundToInt()
        val vP = verticalPadding.roundToInt()

        val text = line.text
        val layoutWidth = MeasureSpec.getSize(widthMeasureSpec)
        val textWidth = layoutWidth - hP * 2

        Log.d("TAG", "tl: ${text.length}, $textWidth")

        staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, textWidth)
            .build()

        val layoutHeight = staticLayout.height + vP * 2
        setMeasuredDimension(layoutWidth, layoutHeight)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blurRenderNode?.apply {
                setPosition(hP, vP, layoutWidth - hP, layoutHeight - vP)
                pivotY = layoutHeight / 2f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val count = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), contentPaint)
        val staticLayout = staticLayout
        val scale = textScale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (canvas.isHardwareAccelerated && blurRenderNode != null) {
                with(blurRenderNode) {
                    if (!hasDisplayList()) {
                        val recordingCanvas = beginRecording()
                        staticLayout.draw(recordingCanvas)
                        endRecording()
                    }
                    scaleX = scale
                    scaleY = scale
                    canvas.drawRenderNode(this)
                }
            } else {
                canvas.translate(horizontalPadding, verticalPadding)
                canvas.scale(scale, scale)
                paint.alpha = (textAlpha * 255).roundToInt()
                staticLayout.draw(canvas)
            }
        } else {
            canvas.translate(horizontalPadding, verticalPadding)
            canvas.scale(scale, scale)
            staticLayout.draw(canvas)
        }
        canvas.restoreToCount(count)
    }

    override fun performClick(): Boolean {
        // GlobalPlayer.seekTo(line.timestamp)
        super.performClick()
        return true
    }

    inner class Animations(
        private val index: Int,
        private var globalOffset: Float,
        private val deviceHeight: Float
    ) {
        private var targetOffset = 0f

        private val offsetFractionAnimator = floatAnimator(700L, interpolator = offsetFractionInterpolator) {
            textOffset = targetOffset * (1f - it.currentValue)
        }

        private val alphaAnimator = floatAnimator(500L, interpolator = AnimationUtils.decelerateInterpolator) {
            textAlpha = it.currentValue
        }

        private val scaleAnimator = floatAnimator(500L, interpolator = AnimationUtils.decelerateInterpolator) {
            textScale = it.currentValue
        }

        private val blurRadiusAnimator = floatAnimator(100L) {
            blurRadius = it.currentValue
        }

        fun getGlobalOffset() = globalOffset

        fun setGlobalOffset(offset: Float) {
            globalOffset = offset
        }

        fun cancelBlur() {
            blurRadiusAnimator.snapTo(0f)
        }

        fun checkIsInScreen(scrollOffset: Float, targetOffset: Float): Boolean {
            val previousScrollOffset = scrollOffset - targetOffset
            val height = height
            return if (previousScrollOffset < scrollOffset) {
                globalOffset + height > previousScrollOffset && globalOffset < scrollOffset + deviceHeight
            } else {
                globalOffset + height > scrollOffset && globalOffset < previousScrollOffset + deviceHeight
            }
        }

        fun updateImmediately(targetIndex: Int) {
            val isActivated = index == targetIndex
            val targetAlpha = if (isActivated) ACTIVE_ALPHA else INACTIVE_ALPHA
            val targetScale = if (isActivated) ACTIVE_SCALE else INACTIVE_SCALE
            val targetBlurRadius = (abs(index - targetIndex) * blurRadiusStep).coerceAtMost(maxBlurRadius)

            offsetFractionAnimator.snapTo(1f)
            alphaAnimator.snapTo(targetAlpha)
            scaleAnimator.snapTo(targetScale)
            blurRadiusAnimator.snapTo(targetBlurRadius)
        }

        fun update(targetIndex: Int, preventBlurUpdate: Boolean = false) {
            val isActivated = index == targetIndex
            val targetAlpha = if (isActivated) ACTIVE_ALPHA else INACTIVE_ALPHA
            val targetScale = if (isActivated) ACTIVE_SCALE else INACTIVE_SCALE
            val targetBlurRadius = (abs(index - targetIndex) * blurRadiusStep).coerceAtMost(maxBlurRadius)

            val delay = if (index < targetIndex) {
                0L
            } else {
                ((index - targetIndex) * 20L + 10L).coerceAtMost(190L)
            }
            val secondaryDelay = delay + 250L

            targetOffset = textOffset
            offsetFractionAnimator.startDelay = delay
            offsetFractionAnimator.start()

            if (alphaAnimator.targetValue != targetAlpha) {
                alphaAnimator.startDelay = secondaryDelay
                alphaAnimator.animateTo(targetAlpha)
            }

            if (scaleAnimator.targetValue != targetScale) {
                scaleAnimator.startDelay = secondaryDelay
                scaleAnimator.animateTo(targetScale)
            }

            if (!preventBlurUpdate) {
                if (blurRadiusAnimator.targetValue != targetBlurRadius) {
                    blurRadiusAnimator.startDelay = secondaryDelay
                    blurRadiusAnimator.animateTo(targetBlurRadius)
                }
            }
        }
    }

    private companion object {
        const val BLUR_NODE_NAME = "LyricsLineViewBlurNode"

        const val ACTIVE_ALPHA = 0.9f
        const val INACTIVE_ALPHA = 0.2f
        const val ACTIVE_SCALE = 1f
        const val INACTIVE_SCALE = 0.96f
        val maxBlurRadius = 8.dp.px
        val blurRadiusStep = 2.dp.px

        val offsetFractionInterpolator = PathInterpolator(0.6f, 0f, 0.2f, 1f)

        val blurs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (1..maxBlurRadius.roundToInt()).associateWith {
                val radius = it.toFloat()
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL)
            }
        } else {
            null
        }

        val rippleColorStateList = ColorStateList.valueOf(0x60FFFFFF)
    }
}