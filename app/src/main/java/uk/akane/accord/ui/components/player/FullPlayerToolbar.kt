package uk.akane.accord.ui.components.player

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import coil3.request.Disposable
import uk.akane.accord.R
import uk.akane.accord.logic.inverseLerp
import uk.akane.accord.logic.setTextAnimation
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.accord.ui.MainActivity
import uk.akane.cupertino.widget.OverlayTextView
import uk.akane.cupertino.widget.button.OverlayBackgroundButton
import uk.akane.cupertino.widget.button.OverlayPillButton
import uk.akane.cupertino.widget.button.StarTransformButton
import uk.akane.cupertino.widget.image.SimpleImageView

class FullPlayerToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val activity
        get() = context as MainActivity
    private val instance: MediaController?
        get() = activity.getPlayer()

    private var coverSimpleImageView: SimpleImageView
    private var titleTextView: OverlayTextView
    private var subtitleTextView: OverlayTextView
    private var starTransformButton: StarTransformButton
    private var ellipsisBackgroundButton: OverlayBackgroundButton
    private var shuffleButton: OverlayPillButton
    private var repeatButton: OverlayPillButton
    private var autoplayButton: OverlayPillButton

    init {
        inflate(context, R.layout.layout_full_player_tool_bar, this)

        coverSimpleImageView = findViewById(R.id.cover)
        titleTextView = findViewById(R.id.title)
        subtitleTextView = findViewById(R.id.subtitle)
        starTransformButton = findViewById(R.id.star)
        ellipsisBackgroundButton = findViewById(R.id.ellipsis)
        shuffleButton = findViewById(R.id.btnShuffle)
        repeatButton = findViewById(R.id.btnRepeat)
        autoplayButton = findViewById(R.id.btnAutoplay)

        doOnLayout {
            maxTranslation = (height - subtitleTextView.bottom).toFloat()
            animateFade(0F)
        }
    }

    private var maxTranslation = 0F

    fun animateFade(fraction: Float) {
        val validFraction = inverseLerp(0.85F, 1F, fraction.coerceIn(0.85F, 1F))
        Log.d("TAG2333", "validFraction: $validFraction, fraction: $fraction")
        coverSimpleImageView.alpha = if (fraction == 1F) 1F else 0F

        titleTextView.translationY = lerp(maxTranslation, 0F, validFraction)
        subtitleTextView.translationY = lerp(maxTranslation, 0F, validFraction)
        starTransformButton.translationY = lerp(maxTranslation, 0F, validFraction)
        ellipsisBackgroundButton.translationY = lerp(maxTranslation, 0F, validFraction)
        shuffleButton.translationY = lerp(maxTranslation, 0F, validFraction)
        repeatButton.translationY = lerp(maxTranslation, 0F, validFraction)
        autoplayButton.translationY = lerp(maxTranslation, 0F, validFraction)

        titleTextView.alpha = validFraction
        subtitleTextView.alpha = validFraction
        starTransformButton.alpha = validFraction
        ellipsisBackgroundButton.alpha = validFraction
        shuffleButton.alpha = validFraction
        repeatButton.alpha = validFraction
        autoplayButton.alpha = validFraction
    }

    private var lastDisposable: Disposable? = null

    fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int
    ) {
        if (instance?.mediaItemCount != 0) {
            lastDisposable?.dispose()
            lastDisposable = null

            titleTextView.setTextAnimation(
                mediaItem?.mediaMetadata?.title ?: ""
            )
            subtitleTextView.setTextAnimation(
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.default_artist)
            )
        } else {
            lastDisposable?.dispose()
            lastDisposable = null
        }
    }

    fun setImageViewCover(drawable: Drawable?) =
        coverSimpleImageView.setImageDrawable(drawable)

    fun getCoverView(): SimpleImageView = coverSimpleImageView

    fun setOnRepeatClickListener(listener: View.OnClickListener) {
        repeatButton.setOnClickListener(listener)
    }

    fun setRepeatMode(repeatMode: Int) {
        val iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
            R.drawable.ic_nowplaying_repeat_one
        } else {
            R.drawable.ic_nowplaying_repeat
        }
        repeatButton.setIconResource(iconRes)
        repeatButton.setChecked(repeatMode != Player.REPEAT_MODE_OFF)
    }

}
