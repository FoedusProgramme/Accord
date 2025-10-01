package uk.akane.accord.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.sp
import uk.akane.cupertino.widget.continuousRoundRect

class BannerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var mediaPlayer: MediaPlayer? = null
    private val textureView: TextureView = TextureView(context)
    private var videoResId: Int = 0
    private var placeholder: Drawable? = null

    init {
        addView(textureView, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ))

        context.theme.obtainStyledAttributes(attrs, R.styleable.BannerView, 0, 0).apply {
            try {
                videoResId = getResourceId(R.styleable.BannerView_videoSrc, 0)
                placeholder = ResourcesCompat.getDrawable(resources, getResourceId(R.styleable.BannerView_placeholder, 0), null)
            } finally {
                recycle()
            }
        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                if (videoResId != 0) {
                    playVideo(videoResId, Surface(surface))
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                releasePlayer()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun playVideo(resId: Int, surface: Surface) {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            val uri = "android.resource://${context.packageName}/$resId".toUri()
            setDataSource(context, uri)
            setSurface(surface)
            isLooping = true
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private val clipPath = Path()
    private val roundCornerSize = 18.dp.px
    private lateinit var linearGradient: LinearGradient

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val width = measuredWidth
        val size = width

        val childLeft = 0
        val childTop = 0
        val childRight = childLeft + size
        val childBottom = childTop + size

        textureView.layout(childLeft, childTop, childRight, childBottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipPath.reset()
        clipPath.continuousRoundRect(
            0F, 0F,
            w.toFloat(), h.toFloat(),
            roundCornerSize
        )
        linearGradient = LinearGradient(
            0F, 0F,
            measuredWidth.toFloat(), 0F,
            intArrayOf(
                resources.getColor(R.color.breezeGradientStart, null),
                resources.getColor(R.color.breezeGradientEnd, null)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        gradientPaint.shader = linearGradient
        val topOffset = 26.dp.px
        val left = 26.dp.px
        val top = topOffset
        val right = left + bannerWidth
        val bottom = top + bannerHeight

        bannerDrawable.setBounds(
            left.toInt(), top.toInt(),
            right.toInt(), bottom.toInt()
        )

        placeholder?.setBounds(
            0, 0,
            w, w,
        )

        bannerDrawable.setTint(
            resources.getColor(R.color.systemWhite, null)
        )
    }

    private val gradientPaint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
    }

    private val bannerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_banner_logo, null)!!
    private val bannerHeight = 14.dp.px
    private val bannerWidth = (bannerDrawable.intrinsicWidth.toFloat() / bannerDrawable.intrinsicHeight.toFloat() * bannerHeight).toInt()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.systemWhite, null)
        typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
        textSize = 40.sp.px
        textAlign = Paint.Align.LEFT
    }

    private val textString = context.getString(R.string.waves_at_max)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releasePlayer()
    }

    override fun dispatchDraw(canvas: Canvas) {
        clipCanvas(canvas)
        if (mediaPlayer?.isPlaying != true) {
            drawPreview(canvas)
        }
        drawPreview(canvas)
        super.dispatchDraw(canvas)
        drawDrawable(canvas)
        drawTitleText(canvas)
        drawGradientText(canvas)
    }

    private fun clipCanvas(canvas: Canvas) {
        canvas.clipPath(clipPath)
        canvas.drawRect(
            0F, measuredWidth.toFloat(),
            measuredWidth.toFloat(), measuredHeight.toFloat(),
            gradientPaint
        )
    }

    private fun drawPreview(canvas: Canvas) {
        placeholder?.draw(canvas)
    }

    private fun drawDrawable(canvas: Canvas) {
        bannerDrawable.draw(canvas)
    }

    private val gradientTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.systemWhite, null)
        typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        textSize = 16.sp.px
        textAlign = Paint.Align.LEFT
    }

    private val gradientText = "sasakure.UK、lasah、Sound Horizon、Mili、Toripiyo、Yunosuke、Powerless、…"

    private fun drawTitleText(canvas: Canvas) {
        val bounds = bannerDrawable.bounds
        val x = bounds.left.toFloat()
        val yTop = bounds.bottom + 12.dp.px
        val fm = textPaint.fontMetrics
        val baseline = yTop - fm.ascent

        canvas.drawText(textString, x, baseline, textPaint)
    }

    private fun drawGradientText(canvas: Canvas) {
        if (gradientText.isEmpty()) return

        val rectTop = measuredWidth
        val rectBottom = measuredHeight
        val rectHeight = rectBottom - rectTop

        val maxWidth = measuredWidth - 32.dp.px

        val staticLayout = StaticLayout.Builder.obtain(
            gradientText, 0, gradientText.length, gradientTextPaint, maxWidth.toInt()
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(3)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .setLineSpacing(2.5.sp.px, 1.0f)
            .build()

        val textHeight = staticLayout.height
        val x = (measuredWidth - staticLayout.width) / 2f
        val y = rectTop + (rectHeight - textHeight) / 2f

        canvas.withTranslation(x, y) {
            staticLayout.draw(this)
        }
    }
}
