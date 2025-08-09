package uk.akane.accord.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.WindowInsets
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import uk.akane.accord.R
import uk.akane.accord.logic.getUriToDrawable
import uk.akane.accord.ui.components.lyrics.LyricsViewModel
import uk.akane.cupertino.widget.OverlayTextView
import uk.akane.cupertino.widget.divider.OverlayDivider
import uk.akane.cupertino.widget.image.OverlayHintView
import uk.akane.cupertino.widget.image.SimpleImageView
import uk.akane.cupertino.widget.slider.OverlaySlider
import uk.akane.cupertino.widget.special.BlendView

class FullPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    FloatingPanelLayout.OnSlideListener {

    private var initialMargin = IntArray(4)

    private var blendView: BlendView
    private var overlayDivider: OverlayDivider
    private var fadingEdgeLayout: FadingVerticalEdgeLayout
    private var lyricsBtn: Button
    private var volumeOverlaySlider: OverlaySlider
    private var progressOverlaySlider: OverlaySlider
    private var speakerHintView: OverlayHintView
    private var speakerFullHintView: OverlayHintView
    private var currentTimestampTextView: OverlayTextView
    private var leftTimestampTextView: OverlayTextView
    private var coverSimpleImageView: SimpleImageView

    private var lyricsViewModel: LyricsViewModel? = null
    private val floatingPanelLayout: FloatingPanelLayout?
        get() = parent as FloatingPanelLayout?

    init {
        inflate(context, R.layout.layout_full_player, this)

        blendView = findViewById(R.id.blend_view)
        overlayDivider = findViewById(R.id.divider)
        fadingEdgeLayout = findViewById(R.id.fading)
        lyricsBtn = findViewById(R.id.lyrics)
        volumeOverlaySlider = findViewById(R.id.volume_slider)
        progressOverlaySlider = findViewById(R.id.progressBar)
        speakerHintView = findViewById(R.id.speaker_hint)
        speakerFullHintView = findViewById(R.id.speaker_full_hint)
        currentTimestampTextView = findViewById(R.id.current_timestamp)
        leftTimestampTextView = findViewById(R.id.left_timeStamp)
        coverSimpleImageView = findViewById(R.id.cover)

        blendView.setImageUri(context.getUriToDrawable(R.drawable.eg))
        blendView.startRotationAnimation()
        clipToOutline = true

        floatingPanelLayout?.addOnSlideListener(this)

        fadingEdgeLayout.visibility = GONE
        lyricsViewModel = LyricsViewModel(context)

        lyricsBtn.setOnClickListener {
            fadingEdgeLayout.visibility = VISIBLE
            lyricsViewModel?.onViewCreated(fadingEdgeLayout)
            lyricsBtn.visibility = GONE
        }

        volumeOverlaySlider.addEmphasizeListener(object : OverlaySlider.EmphasizeListener {
            override fun onEmphasizeProgressLeft(translationX: Float) {
                speakerHintView.translationX = -translationX
            }

            override fun onEmphasizeProgressRight(translationX: Float) {
                speakerFullHintView.translationX = translationX
            }

            override fun onEmphasizeAll(fraction: Float) {
                speakerHintView.transformValue = fraction
                speakerFullHintView.transformValue = fraction
            }
        })

        progressOverlaySlider.addEmphasizeListener(object : OverlaySlider.EmphasizeListener {
            override fun onEmphasizeVertical(translationX: Float, translationY: Float) {
                currentTimestampTextView.translationY = translationY
                currentTimestampTextView.translationX = - translationX
                leftTimestampTextView.translationY = translationY
                leftTimestampTextView.translationX = translationX
            }
        })
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
                marginLeft,
                marginTop + floatingInsets.top,
                marginRight,
                marginBottom + floatingInsets.bottom
            )
            Log.d(TAG, "initTop: ${initialMargin[1]}")
            overlayDivider.updateLayoutParams<MarginLayoutParams> {
                topMargin = initialMargin[1] + overlayDivider.marginTop
            }
        }
        Log.d(TAG, "marginBottom: ${marginBottom}, InsetsBottom: ${floatingInsets.bottom}, marginTop: ${floatingInsets.top}")
        return super.dispatchApplyWindowInsets(platformInsets)
    }

    override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
        when (status) {
            else -> {}
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d("TAG", "onMeasured")
    }

    override fun onSlide(value: Float) {
        // PLACEHOLDER TODO
    }

    companion object {
        const val TAG = "FullPlayer"
    }
}