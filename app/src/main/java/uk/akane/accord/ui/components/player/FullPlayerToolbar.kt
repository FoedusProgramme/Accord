package uk.akane.accord.ui.components.player

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import uk.akane.accord.R
import uk.akane.accord.logic.getFile
import uk.akane.accord.logic.inverseLerp
import uk.akane.accord.logic.setTextAnimation
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.accord.ui.MainActivity
import uk.akane.cupertino.widget.OverlayTextView
import uk.akane.cupertino.widget.button.OverlayBackgroundButton
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

    init {
        inflate(context, R.layout.layout_full_player_tool_bar, this)

        coverSimpleImageView = findViewById(R.id.cover)
        titleTextView = findViewById(R.id.title)
        subtitleTextView = findViewById(R.id.subtitle)
        starTransformButton = findViewById(R.id.star)
        ellipsisBackgroundButton = findViewById(R.id.ellipsis)

        doOnLayout {
            maxTranslation = (height - subtitleTextView.bottom).toFloat()
        }
    }

    private var maxTranslation = 0F

    fun animateFade(fraction: Float) {
        val validFraction = inverseLerp(0.85F, 1F, fraction.coerceIn(0.85F, 1F))
        coverSimpleImageView.alpha = if (fraction == 1F) 1F else 0F

        titleTextView.translationY = lerp(maxTranslation, 0F, validFraction)
        subtitleTextView.translationY = lerp(maxTranslation, 0F, validFraction)
        starTransformButton.translationY = lerp(maxTranslation, 0F, validFraction)
        ellipsisBackgroundButton.translationY = lerp(maxTranslation, 0F, validFraction)

        titleTextView.alpha = validFraction
        subtitleTextView.alpha = validFraction
        starTransformButton.alpha = validFraction
        ellipsisBackgroundButton.alpha = validFraction
    }

    private var lastDisposable: Disposable? = null

    fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int
    ) {
        if (instance?.mediaItemCount != 0) {
            lastDisposable?.dispose()
            lastDisposable = null
            loadCoverForImageView()

            titleTextView.setTextAnimation(
                mediaItem?.mediaMetadata?.title
            )
            subtitleTextView.setTextAnimation(
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.default_artist)
            )
        } else {
            lastDisposable?.dispose()
            lastDisposable = null
        }
    }

    private fun loadCoverForImageView() {
        if (lastDisposable != null) {
            lastDisposable?.dispose()
            lastDisposable = null
            Log.e(FullPlayer.Companion.TAG, "raced while loading cover in onMediaItemTransition?")
        }
        val mediaItem = instance?.currentMediaItem
        if (coverSimpleImageView.width != 0 && coverSimpleImageView.height != 0) {
            val file = mediaItem?.getFile()
            lastDisposable = context.imageLoader.enqueue(
                ImageRequest.Builder(context).apply {
                    data(Pair(file, mediaItem?.mediaMetadata?.artworkUri))
                    size(coverSimpleImageView.width, coverSimpleImageView.height)
                    scale(Scale.FILL)
                    target(onSuccess = {
                        coverSimpleImageView.setImageDrawable(it.asDrawable(context.resources))
                    }, onError = {
                        coverSimpleImageView.setImageDrawable(it?.asDrawable(context.resources))
                    }) // do not react to onStart() which sets placeholder
                    allowHardware(coverSimpleImageView.isHardwareAccelerated)
                }.build()
            )
        }
    }

}