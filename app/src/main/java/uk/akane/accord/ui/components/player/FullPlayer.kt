package uk.akane.accord.ui.components.player

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.media.MediaRouter2
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import com.google.android.material.slider.Slider
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.playOrPause
import uk.akane.accord.logic.setTextAnimation
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.components.FadingVerticalEdgeLayout
import uk.akane.accord.ui.components.PopupHelper
import uk.akane.accord.ui.components.lyrics.LyricsViewModel
import uk.akane.cupertino.widget.OverlayTextView
import uk.akane.cupertino.widget.button.AnimatedVectorButton
import uk.akane.cupertino.widget.button.OverlayBackgroundButton
import uk.akane.cupertino.widget.button.OverlayButton
import uk.akane.cupertino.widget.button.StarTransformButton
import uk.akane.cupertino.widget.button.StateAnimatedVectorButton
import uk.akane.cupertino.widget.divider.OverlayDivider
import uk.akane.cupertino.widget.image.OverlayHintView
import uk.akane.cupertino.widget.image.SimpleImageView
import uk.akane.cupertino.widget.slider.OverlaySlider
import uk.akane.cupertino.widget.special.BlendView
import uk.akane.cupertino.widget.utils.AnimationUtils
import uk.akane.cupertino.widget.utils.AnimationUtils.MID_DURATION

class FullPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    FloatingPanelLayout.OnSlideListener, Player.Listener {

    private val activity
        get() = context as MainActivity
    private val instance: MediaController?
        get() = activity.getPlayer()

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
    private var titleTextView: OverlayTextView
    private var subtitleTextView: OverlayTextView
    private var listOverlayButton: OverlayButton
    private var airplayOverlayButton: OverlayButton
    private var captionOverlayButton: OverlayButton
    private var starTransformButton: StarTransformButton
    private var controllerButton: StateAnimatedVectorButton
    private var previousButton: AnimatedVectorButton
    private var nextButton: AnimatedVectorButton
    private var ellipsisButton: OverlayBackgroundButton

    private var fullPlayerToolbar: FullPlayerToolbar
    private var testSlider: Slider

    private var lyricsViewModel: LyricsViewModel? = null
    private val floatingPanelLayout: FloatingPanelLayout
        get() = parent as FloatingPanelLayout

    private var firstTime = false

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
        titleTextView = findViewById(R.id.title)
        subtitleTextView = findViewById(R.id.subtitle)
        listOverlayButton = findViewById(R.id.list)
        airplayOverlayButton = findViewById(R.id.airplay)
        captionOverlayButton = findViewById(R.id.caption)
        starTransformButton = findViewById(R.id.star)
        ellipsisButton = findViewById(R.id.ellipsis)
        controllerButton = findViewById(R.id.main_control_btn)
        previousButton = findViewById(R.id.backward_btn)
        nextButton = findViewById(R.id.forward_btn)
        fullPlayerToolbar = findViewById(R.id.full_player_tool_bar)
        testSlider = findViewById(R.id.test_slider)

        ellipsisButton.setOnCheckedChangeListener { v, checked ->
            callUpPlayerPopupMenu(v)
        }

        ellipsisButton.setOnLongClickListener {
            // TODO tell floating panel to intercept gesture
            callUpPlayerPopupMenu(it)
            true
        }

        clipToOutline = true

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

            override fun onEmphasizeStartLeft() {
                speakerHintView.playAnim()
            }

            override fun onEmphasizeStartRight() {
                speakerFullHintView.playAnim()
            }
        })

        progressOverlaySlider.addEmphasizeListener(object : OverlaySlider.EmphasizeListener {
            override fun onEmphasizeVertical(translationX: Float, translationY: Float) {
                currentTimestampTextView.translationY = translationY
                currentTimestampTextView.translationX = -translationX
                leftTimestampTextView.translationY = translationY
                leftTimestampTextView.translationX = translationX
            }
        })

        coverSimpleImageView.doOnLayout {
            Log.d(TAG, "csi: ${coverSimpleImageView.left}, ${coverSimpleImageView.top}")
            floatingPanelLayout.setupTransitionImageView(
                coverSimpleImageView.width,
                coverSimpleImageView.height,
                coverSimpleImageView.left,
                coverSimpleImageView.top,
                AppCompatResources.getDrawable(context, R.drawable.default_cover)!!.toBitmap()
            )
        }

        testSlider.addOnChangeListener { slider, progress, isUser ->
            animateCoverChange(progress)
        }

        listOverlayButton.setOnClickListener {
            Log.d(
                TAG,
                "ContentType: ${if (listOverlayButton.isChecked) ContentType.NORMAL else ContentType.PLAYLIST}"
            )
            contentType =
                if (listOverlayButton.isChecked) ContentType.NORMAL else ContentType.PLAYLIST
            listOverlayButton.toggle()
        }

        airplayOverlayButton.setOnClickListener {
            startSystemMediaControl()
        }

        activity.controllerViewModel.addControllerCallback(activity.lifecycle) { _, _ ->
            firstTime = true
            instance?.addListener(this@FullPlayer)
            onRepeatModeChanged(instance?.repeatMode ?: Player.REPEAT_MODE_OFF)
            onShuffleModeEnabledChanged(instance?.shuffleModeEnabled == true)
            onPlaybackStateChanged(instance?.playbackState ?: Player.STATE_IDLE)
            instance?.currentTimeline?.let {
                onTimelineChanged(it, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            }
            onMediaItemTransition(
                instance?.currentMediaItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            )
            onMediaMetadataChanged(instance?.mediaMetadata ?: MediaMetadata.EMPTY)
            firstTime = false
        }

        controllerButton.setOnClickListener {
            instance?.playOrPause()
        }

        previousButton.setOnClickListener {
            instance?.seekToPrevious()
        }
        nextButton.setOnClickListener {
            instance?.seekToNext()
        }

        doOnLayout {
            floatingPanelLayout.addOnSlideListener(this)

            finalTranslationX = 32.dp.px - coverSimpleImageView.left
            finalTranslationY = (20 - 18).dp.px
            finalScale = 74.dp.px / coverSimpleImageView.height
        }
    }

    private fun startSystemMediaControl() {
        if (Build.VERSION.SDK_INT >= 34) {
            val mediaRouter2 = MediaRouter2.getInstance(context)
            val tag = mediaRouter2.showSystemOutputSwitcher()
            if (!tag) {
                Toast.makeText(context, R.string.media_control_text_error, Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            val intent = Intent().apply {
                action = "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG"
                setPackage("com.android.systemui")
                putExtra("package_name", context.packageName)
            }
            val tag = startNativeMediaDialog(intent)
            if (!tag) {
                Toast.makeText(context, R.string.media_control_text_error, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun startNativeMediaDialog(intent: Intent): Boolean {
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val applicationInfo: ApplicationInfo? = activityInfo?.applicationInfo
            if (applicationInfo != null && (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                context.startActivity(intent)
                return true
            }
        }
        return false
    }

    private var finalTranslationX = 0F
    private var finalTranslationY = 0F
    private var initialCoverRadius =
        resources.getDimensionPixelSize(R.dimen.full_cover_radius).toFloat()
    private var endCoverRadius = 22.dp.px
    private var initialElevation = 24.dp.px
    private var finalScale = 0F

    private fun animateCoverChange(fraction: Float) {
        coverSimpleImageView.translationX = lerp(0f, finalTranslationX, fraction)
        coverSimpleImageView.translationY = lerp(0f, finalTranslationY, fraction)
        coverSimpleImageView.pivotX = 0F
        coverSimpleImageView.pivotY = 0F
        coverSimpleImageView.scaleX = lerp(1f, finalScale, fraction)
        coverSimpleImageView.scaleY = lerp(1f, finalScale, fraction)
        coverSimpleImageView.elevation = lerp(initialElevation, 5f.dp.px, fraction)
        coverSimpleImageView.updateCornerRadius(
            lerp(
                initialCoverRadius,
                endCoverRadius,
                fraction
            ).toInt()
        )

        coverSimpleImageView.visibility = if (fraction == 1F) INVISIBLE else VISIBLE

        titleTextView.translationY =
            coverSimpleImageView.translationY - coverSimpleImageView.height * (1f - coverSimpleImageView.scaleX)
        starTransformButton.translationY =
            coverSimpleImageView.translationY - coverSimpleImageView.height * (1f - coverSimpleImageView.scaleX)
        ellipsisButton.translationY =
            coverSimpleImageView.translationY - coverSimpleImageView.height * (1f - coverSimpleImageView.scaleX)
        subtitleTextView.translationY =
            coverSimpleImageView.translationY - coverSimpleImageView.height * (1f - coverSimpleImageView.scaleX)

        val quickFraction = (fraction * 1.2f).coerceIn(0F, 1F)
        titleTextView.alpha = lerp(1F, 0F, quickFraction)
        subtitleTextView.alpha = lerp(1F, 0F, quickFraction)
        starTransformButton.alpha = lerp(1F, 0F, quickFraction)
        ellipsisButton.alpha = lerp(1F, 0F, quickFraction)

        fullPlayerToolbar.animateFade(fraction)
    }

    private fun callUpPlayerPopupMenu(v: View) {
        floatingPanelLayout.callUpPopup(
            PopupHelper.PopupMenuBuilder()
                .addMenuEntry(resources, R.drawable.ic_info, R.string.popup_view_credits)
                .addSpacer()
                .addDestructiveMenuEntry(
                    resources,
                    R.drawable.ic_trash,
                    R.string.popup_delete_from_library
                )
                .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_add_to_a_playlist)
                .addSpacer()
                .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_share_song)
                .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_share_lyrics)
                .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_go_to_album)
                .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_create_station)
                .addSpacer()
                .addMenuEntry(resources, R.drawable.ic_square, R.string.popup_undo_favorite)
                .build(),
            v.left + v.width,
            v.top
        ) {
            ellipsisButton.isChecked = false
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
        Log.d(
            TAG,
            "marginBottom: ${marginBottom}, InsetsBottom: ${floatingInsets.bottom}, marginTop: ${floatingInsets.top}"
        )
        return super.dispatchApplyWindowInsets(platformInsets)
    }

    override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
        when (status) {
            FloatingPanelLayout.SlideStatus.EXPANDED -> {
                coverSimpleImageView.alpha = 1F
            }

            else -> {
                coverSimpleImageView.alpha = 0F
            }
        }
    }

    var previousState = false
    fun freeze() {
        previousState = blendView.isRunning
        blendView.stopRotationAnimation()
    }

    fun unfreeze() {
        // TODO: Make it on demand
        if (previousState) {
            blendView.startRotationAnimation()
        } else {
            blendView.stopRotationAnimation()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG, "onMeasured")
    }

    override fun onSlide(value: Float) {
        // PLACEHOLDER TODO
    }

    private var transformationFraction = 0F

    private var contentType = ContentType.NORMAL
        set(value) {
            when (value) {
                ContentType.LYRICS -> {
                    // TODO
                }

                ContentType.NORMAL -> {
                    AnimationUtils.createValAnimator(
                        transformationFraction, 0F,
                        duration = MID_DURATION
                    ) {
                        transformationFraction = it
                        animateCoverChange(it)
                    }
                }

                ContentType.PLAYLIST -> {
                    captionOverlayButton.isChecked = false
                    AnimationUtils.createValAnimator(
                        transformationFraction, 1F,
                        duration = MID_DURATION
                    ) {
                        transformationFraction = it
                        animateCoverChange(it)
                    }
                }
            }
            field = value
        }

    private var lastDisposable: Disposable? = null

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int
    ) {
        fullPlayerToolbar.onMediaItemTransition(mediaItem, reason)
        if (instance?.mediaItemCount != 0) {
            lastDisposable?.dispose()
            lastDisposable = null
            loadCoverForImageView()

            titleTextView.setTextAnimation(
                mediaItem?.mediaMetadata?.title ?: "",
                skipAnimation = firstTime
            )
            subtitleTextView.setTextAnimation(
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.default_artist),
                skipAnimation = firstTime
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
            Log.e(TAG, "raced while loading cover in onMediaItemTransition?")
        }
        val mediaItem = instance?.currentMediaItem
        Log.d(TAG, "load cover for ${mediaItem?.mediaMetadata?.title} considered")
        if (coverSimpleImageView.width != 0 && coverSimpleImageView.height != 0) {
            Log.d(
                TAG,
                "load cover for ${mediaItem?.mediaMetadata?.title} at ${coverSimpleImageView.width} ${coverSimpleImageView.height}"
            )
            lastDisposable = context.imageLoader.enqueue(
                ImageRequest.Builder(context).apply {
                    data(mediaItem?.mediaMetadata?.artworkUri)
                    size(coverSimpleImageView.width, coverSimpleImageView.height)
                    scale(Scale.FILL)
                    target(onSuccess = {
                        blendView.setImageBitmap(it.toBitmap())
                        coverSimpleImageView.setImageDrawable(it.asDrawable(context.resources))
                        fullPlayerToolbar.setImageViewCover(it.asDrawable(context.resources))
                        (parent as FloatingPanelLayout).transitionImageView!!.setImageDrawable(
                            it.asDrawable(context.resources)
                        )
                    }, onError = {
                        blendView.setImageBitmap(it?.toBitmap())
                        coverSimpleImageView.setImageDrawable(it?.asDrawable(context.resources))
                        fullPlayerToolbar.setImageViewCover(it?.asDrawable(context.resources))
                        (parent as FloatingPanelLayout).transitionImageView!!.setImageDrawable(
                            it?.asDrawable(context.resources)
                        )
                    }) // do not react to onStart() which sets placeholder
                    allowHardware(coverSimpleImageView.isHardwareAccelerated)
                }.build()
            )
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        onPlaybackStateChanged(instance?.playbackState ?: Player.STATE_IDLE)
    }

    override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
        Log.d("FullPlayer", "onPlaybackStateChanged: $playbackState")
        if (instance?.isPlaying == true) {
            controllerButton.playAnimation(false)
        } else if (playbackState != Player.STATE_BUFFERING) {
            controllerButton.playAnimation(true)
        }
        /*
        if (instance?.isPlaying == true) {
            if (bottomSheetFullControllerButton.getTag(R.id.play_next) as Int? != 1) {
                bottomSheetFullControllerButton.icon =
                    AppCompatResources.getDrawable(
                        wrappedContext ?: context,
                        R.drawable.play_anim
                    )
                bottomSheetFullControllerButton.background =
                    AppCompatResources.getDrawable(context, R.drawable.bg_play_anim)
                bottomSheetFullControllerButton.icon.startAnimation()
                bottomSheetFullControllerButton.background.startAnimation()
                bottomSheetFullControllerButton.setTag(R.id.play_next, 1)
            }
            if (!isUserTracking) {
                progressDrawable.animate = true
            }
            if (!runnableRunning) {
                runnableRunning = true
                handler.postDelayed(positionRunnable, SLIDER_UPDATE_INTERVAL)
            }
            bottomSheetFullCover.startRotation()
        } else if (playbackState != Player.STATE_BUFFERING) {
            if (bottomSheetFullControllerButton.getTag(R.id.play_next) as Int? != 2) {
                bottomSheetFullControllerButton.icon =
                    AppCompatResources.getDrawable(
                        wrappedContext ?: context,
                        R.drawable.pause_anim
                    )
                bottomSheetFullControllerButton.background =
                    AppCompatResources.getDrawable(context, R.drawable.bg_pause_anim)
                bottomSheetFullControllerButton.icon.startAnimation()
                bottomSheetFullControllerButton.background.startAnimation()
                bottomSheetFullControllerButton.setTag(R.id.play_next, 2)
                bottomSheetFullCover.stopRotation()
            }
            if (!isUserTracking) {
                progressDrawable.animate = false
            }
        }

         */
    }

    /*
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        val isHeart = (mediaMetadata.userRating as? HeartRating)?.isHeart == true
        if (bottomSheetFavoriteButton.isChecked != isHeart) {
            bottomSheetFavoriteButton.removeOnCheckedChangeListener(this)
            bottomSheetFavoriteButton.isChecked =
                (mediaMetadata.userRating as? HeartRating)?.isHeart == true
            bottomSheetFavoriteButton.addOnCheckedChangeListener(this)
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: @Player.TimelineChangeReason Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
            updateDuration()
        }
    }

    private fun updateDuration() {
        val duration = instance?.contentDuration?.let { if (it == C.TIME_UNSET) null else it }
            ?: instance?.currentMediaItem?.mediaMetadata?.durationMs
        if (duration != null && duration.toInt() != bottomSheetFullSeekBar.max) {
            bottomSheetFullDuration.setTextAnimation(
                CalculationUtils.convertDurationToTimeStamp(duration)
            )
            val position =
                CalculationUtils.convertDurationToTimeStamp(instance?.currentPosition ?: 0)
            if (!isUserTracking) {
                bottomSheetFullSeekBar.max = duration.toInt()
                bottomSheetFullSeekBar.progress = instance?.currentPosition?.toInt() ?: 0
                bottomSheetFullSlider.valueTo = duration.toFloat().coerceAtLeast(1f)
                bottomSheetFullSlider.value =
                    min(instance?.currentPosition?.toFloat() ?: 0f, bottomSheetFullSlider.valueTo)
                bottomSheetFullPosition.text = position
            }
            bottomSheetFullLyricView.updateLyricPositionFromPlaybackPos()
        }
    }
     */

    enum class ContentType {
        LYRICS, NORMAL, PLAYLIST
    }

    companion object {
        const val TAG = "FullPlayer"
    }
}