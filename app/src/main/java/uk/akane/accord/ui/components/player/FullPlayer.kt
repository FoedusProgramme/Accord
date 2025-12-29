package uk.akane.accord.ui.components.player

import android.content.Context
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.media.AudioManager
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import uk.akane.accord.logic.inverseLerp
import uk.akane.accord.logic.playOrPause
import uk.akane.accord.logic.setTextAnimation
import uk.akane.accord.logic.utils.CalculationUtils.convertDurationToTimeStamp
import uk.akane.accord.logic.utils.CalculationUtils.lerp
import uk.akane.accord.ui.adapters.QueueItemTouchHelperCallback
import uk.akane.accord.ui.adapters.QueuePreviewAdapter
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.components.FadingVerticalEdgeLayout
import uk.akane.accord.ui.components.lyrics.LyricsViewModel
import uk.akane.cupertino.widget.OverlayTextView
import uk.akane.cupertino.widget.button.AnimatedVectorButton
import uk.akane.cupertino.widget.button.OverlayBackgroundButton
import uk.akane.cupertino.widget.button.OverlayButton
import uk.akane.cupertino.widget.button.OverlayPillButton
import uk.akane.cupertino.widget.button.StarTransformButton
import uk.akane.cupertino.widget.button.StateAnimatedVectorButton
import uk.akane.cupertino.widget.divider.OverlayDivider
import uk.akane.cupertino.widget.image.OverlayHintView
import uk.akane.cupertino.widget.image.SimpleImageView
import uk.akane.cupertino.widget.slider.OverlaySlider
import uk.akane.cupertino.widget.special.BlendView
import uk.akane.cupertino.widget.utils.AnimationUtils
import uk.akane.cupertino.widget.utils.AnimationUtils.LONG_DURATION
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
    private var queueContainer: View
    private var queueShuffleButton: OverlayPillButton
    private var queueRepeatButton: OverlayPillButton
    private var queueAutoplayButton: OverlayPillButton
    private var queueTextView: OverlayTextView
    private var queueRecyclerView: RecyclerView
    private var queueItemTouchHelper: ItemTouchHelper? = null

    private var lyricsViewModel: LyricsViewModel? = null
    private val floatingPanelLayout: FloatingPanelLayout
        get() = parent as FloatingPanelLayout

    private var firstTime = false
    private var positionUpdateRunning = false
    private var isUserScrubbing = false
    private var isUserVolumeScrubbing = false
    private var maxDeviceVolume = 0
    private var volumeUpdateAnimator: ValueAnimator? = null
    private val audioManager by lazy {
        ContextCompat.getSystemService(context, AudioManager::class.java)
    }
    private var isVolumeReceiverRegistered = false
    private val volumeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "android.media.VOLUME_CHANGED_ACTION",
                "android.media.MASTER_VOLUME_CHANGED_ACTION",
                "android.media.MASTER_MUTE_CHANGED_ACTION",
                "android.media.STREAM_MUTE_CHANGED_ACTION" -> updateVolumeSlider()
            }
        }
    }
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgressDisplay()
            if (positionUpdateRunning) {
                postDelayed(this, POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

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
        queueContainer = findViewById(R.id.queue_container)
        queueShuffleButton = findViewById(R.id.btnShuffle)
        queueRepeatButton = findViewById(R.id.btnRepeat)
        queueAutoplayButton = findViewById(R.id.btnAutoplay)
        queueTextView = findViewById(R.id.queue)
        queueRecyclerView = findViewById(R.id.queue_list)
        queueRecyclerView.layoutManager = LinearLayoutManager(context)
        val queueAdapter = QueuePreviewAdapter(
            mutableListOf(
                QueuePreviewAdapter.Item("Hollowlight", "Moonshade"),
                QueuePreviewAdapter.Item("Paper Trails", "The Vellum Set"),
                QueuePreviewAdapter.Item("Daybreak Avenue", "Arlo Winters"),
                QueuePreviewAdapter.Item("Slow Orbit", "Nocturne Park"),
                QueuePreviewAdapter.Item("Midnight Bloom", "City Flora"),
                QueuePreviewAdapter.Item("So easy", "Olivia Dean"),
                QueuePreviewAdapter.Item("Blinding Lights", "The Weeknd")
            ),
            object : QueuePreviewAdapter.DragStartListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                    queueItemTouchHelper?.startDrag(viewHolder)
                }
            }
        )
        queueRecyclerView.adapter = queueAdapter
        queueItemTouchHelper = ItemTouchHelper(QueueItemTouchHelperCallback(queueAdapter)).apply {
            attachToRecyclerView(queueRecyclerView)
        }
        queueContainer.doOnLayout {
            queueEnterOffset = resolveQueueEnterOffset()
        }

        ellipsisButton.setOnCheckedChangeListener { v, checked ->
            callUpPlayerPopupMenu(v)
        }

        ellipsisButton.setOnLongClickListener {
            // TODO tell floating panel to intercept gesture
            callUpPlayerPopupMenu(it)
            true
        }

        fullPlayerToolbar.setOnEllipsisCheckedChangeListener(
            OverlayBackgroundButton.OnCheckedChangeListener { button, _ ->
                callUpPlayerPopupMenu(button)
            }
        )

        clipToOutline = true

        fadingEdgeLayout.visibility = GONE
        queueContainer.visibility = INVISIBLE
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
        volumeOverlaySlider.addValueChangeListener(object : OverlaySlider.ValueChangeListener {
            override fun onStartTracking(slider: OverlaySlider) {
                isUserVolumeScrubbing = true
                volumeUpdateAnimator?.cancel()
            }

            override fun onValueChanged(slider: OverlaySlider, value: Float, fromUser: Boolean) {
                if (!fromUser) return
                setDeviceVolume(value.toInt())
            }

            override fun onStopTracking(slider: OverlaySlider) {
                setDeviceVolume(slider.value.toInt())
                isUserVolumeScrubbing = false
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

        progressOverlaySlider.addValueChangeListener(object : OverlaySlider.ValueChangeListener {
            override fun onStartTracking(slider: OverlaySlider) {
                isUserScrubbing = true
                stopPositionUpdates()
            }

            override fun onValueChanged(slider: OverlaySlider, value: Float, fromUser: Boolean) {
                if (!fromUser) return
                val duration = resolveDurationMs() ?: return
                updateProgressTexts(value.toLong(), duration)
            }

            override fun onStopTracking(slider: OverlaySlider) {
                val duration = resolveDurationMs()
                if (duration != null) {
                    val position = slider.value.toLong().coerceIn(0L, duration)
                    instance?.seekTo(position)
                    updateProgressTexts(position, duration)
                }
                isUserScrubbing = false
                if (instance?.isPlaying == true) {
                    startPositionUpdates()
                } else {
                    updateProgressDisplay()
                }
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

            updateTransitionTargetForContentType(contentType)
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

        queueRepeatButton.setOnClickListener {
            val controller = instance ?: return@setOnClickListener
            val nextRepeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextRepeatMode
            updateRepeatButton(nextRepeatMode)
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
            updateVolumeSlider()
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

        updateVolumeSlider()
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
    private val queueCoverRadius = 5.dp.px
    private var queueEnterOffset = 0f
    private val queueStartFraction = 1F / 1.2F

    private fun updateTransitionTargetForContentType(value: ContentType) {
        val targetView = if (value == ContentType.PLAYLIST) {
            fullPlayerToolbar.getCoverView()
        } else {
            coverSimpleImageView
        }
        val lockCornerRadius = value == ContentType.PLAYLIST
        val targetRadius = if (lockCornerRadius) queueCoverRadius else 0F
        val targetElevation = if (value == ContentType.PLAYLIST) null else initialElevation

        targetView.doOnLayout {
            floatingPanelLayout.updateTransitionTarget(
                targetView,
                targetRadius,
                lockCornerRadius,
                targetElevation
            )
        }
    }

    private fun updateRepeatButton(repeatMode: Int) {
        val iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
            R.drawable.ic_nowplaying_repeat_one
        } else {
            R.drawable.ic_nowplaying_repeat
        }
        queueRepeatButton.setIconResource(iconRes)
        queueRepeatButton.setChecked(repeatMode != Player.REPEAT_MODE_OFF)
    }

    private fun resolveDurationMs(): Long? {
        val duration = instance?.contentDuration
        if (duration != null && duration != C.TIME_UNSET) {
            return duration
        }
        return instance?.currentMediaItem?.mediaMetadata?.durationMs?.takeIf { it > 0L }
    }

    private fun resolveMaxDeviceVolume(): Int {
        if (maxDeviceVolume <= 0) {
            maxDeviceVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
        }
        return maxDeviceVolume
    }

    private fun resolveDeviceVolume(): Int {
        val controller = instance
        return if (controller != null && controller.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME)) {
            controller.deviceVolume
        } else {
            audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        }
    }

    private fun updateVolumeSlider(volume: Int? = null) {
        if (isUserVolumeScrubbing) return
        val maxVolume = resolveMaxDeviceVolume()
        if (maxVolume <= 0) return

        volumeOverlaySlider.valueFrom = 0f
        volumeOverlaySlider.valueTo = maxVolume.toFloat()
        val currentVolume = (volume ?: resolveDeviceVolume()).coerceIn(0, maxVolume).toFloat()
        animateVolumeSliderTo(currentVolume)
    }

    private fun setDeviceVolume(volume: Int) {
        val maxVolume = resolveMaxDeviceVolume()
        if (maxVolume <= 0) return
        val boundedVolume = volume.coerceIn(0, maxVolume)
        val controller = instance
        if (controller != null && controller.isCommandAvailable(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS )) {
            controller.setDeviceVolume(boundedVolume, 0)
        } else {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, boundedVolume, 0)
        }
    }

    private fun animateVolumeSliderTo(targetValue: Float) {
        val startValue = volumeOverlaySlider.value
        if (startValue == targetValue) return
        volumeUpdateAnimator?.cancel()
        volumeUpdateAnimator = ValueAnimator.ofFloat(startValue, targetValue).apply {
            duration = LONG_DURATION
            interpolator = AnimationUtils.easingStandardInterpolator
            addUpdateListener {
                volumeOverlaySlider.value = it.animatedValue as Float
                volumeOverlaySlider.invalidate()
            }
            start()
        }
    }

    private fun updateProgressTexts(positionMs: Long, durationMs: Long) {
        val safeDuration = durationMs.coerceAtLeast(1L)
        val boundedPosition = positionMs.coerceIn(0L, safeDuration)
        val remaining = (safeDuration - boundedPosition).coerceAtLeast(0L)

        currentTimestampTextView.text = convertDurationToTimeStamp(boundedPosition)
        leftTimestampTextView.text =
            context.getString(R.string.time_remaining, convertDurationToTimeStamp(remaining))
    }

    private fun updateProgressDisplay() {
        val mediaDuration = resolveDurationMs()
        val currentPosition = instance?.currentPosition ?: 0L
        if (mediaDuration == null || instance?.mediaItemCount == 0) {
            val placeholder = context.getString(R.string.default_duration)
            currentTimestampTextView.text = placeholder
            leftTimestampTextView.text = placeholder
            progressOverlaySlider.valueTo = 1f
            progressOverlaySlider.value = 0f
            progressOverlaySlider.invalidate()
            return
        }

        if (isUserScrubbing) return

        val safeDuration = mediaDuration.coerceAtLeast(1L)
        val boundedPosition = currentPosition.coerceIn(0L, safeDuration)

        updateProgressTexts(boundedPosition, safeDuration)
        progressOverlaySlider.valueTo = safeDuration.toFloat()
        progressOverlaySlider.value = boundedPosition.toFloat()
        progressOverlaySlider.invalidate()
    }

    private fun startPositionUpdates() {
        if (positionUpdateRunning) return
        positionUpdateRunning = true
        removeCallbacks(positionUpdateRunnable)
        post(positionUpdateRunnable)
    }

    private fun stopPositionUpdates() {
        positionUpdateRunning = false
        removeCallbacks(positionUpdateRunnable)
    }

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
        animateQueuePanel(fraction)
    }

    private fun resolveQueueEnterOffset(): Float {
        // Animate from just above the seekbar, not from title area
        val progressBarTop = progressOverlaySlider.top.toFloat()
        val queueContainerBottom = queueContainer.bottom.toFloat()

        // Offset to move queue container so its top aligns with progressBar top
        return progressBarTop - queueContainerBottom
    }

    private fun animateQueuePanel(fraction: Float) {
        val queueFraction = inverseLerp(queueStartFraction, 1F, fraction, clamp = true)
        if (queueFraction <= 0F) {
            queueEnterOffset = resolveQueueEnterOffset()
            queueContainer.translationY = queueEnterOffset
            queueContainer.visibility = INVISIBLE
            setQueueChildrenAlpha(0F)
            return
        }

        queueContainer.visibility = VISIBLE
        // Simply animate from start offset to 0 (final position)
        queueContainer.translationY = lerp(queueEnterOffset, 0F, queueFraction)
        setQueueChildrenAlpha(queueFraction)
    }

    private fun setQueueChildrenAlpha(alpha: Float) {
        queueShuffleButton.alpha = alpha
        queueRepeatButton.alpha = alpha
        queueAutoplayButton.alpha = alpha
        queueTextView.alpha = alpha
        queueRecyclerView.alpha = alpha
    }

    private fun callUpPlayerPopupMenu(v: View) {
        val popupEntries = PlayerPopupMenu.build(resources)

        val anchorView = if (contentType == ContentType.PLAYLIST) {
            fullPlayerToolbar.getEllipsisView()
        } else {
            v
        }

        val anchor = IntArray(2)
        val container = IntArray(2)
        anchorView.getLocationOnScreen(anchor)
        floatingPanelLayout.getLocationOnScreen(container)
        val localX = anchor[0] - container[0]
        val localY = anchor[1] - container[1]

        val popupAnchorOffset = 12.dp.px
        val popupBelowGap = 8.dp.px
        val showBelow = contentType == ContentType.PLAYLIST
        val locationY = if (showBelow) {
            (localY + anchorView.height + popupBelowGap + popupAnchorOffset).toInt()
        } else {
            localY
        }

        floatingPanelLayout.callUpPopup(
            popupEntries,
            localX + anchorView.width,
            locationY,
            showBelow
        ) {
            ellipsisButton.isChecked = false
            fullPlayerToolbar.setEllipsisChecked(false)
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
        if (contentType == ContentType.PLAYLIST) {
            fullPlayerToolbar.getCoverView().alpha = if (value >= 1F) 1F else 0F
        }
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
                    queueContainer.visibility = VISIBLE
                    queueContainer.translationY = queueEnterOffset
                    setQueueChildrenAlpha(0F)
                    queueContainer.bringToFront()
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
            updateTransitionTargetForContentType(value)
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
            updateProgressDisplay()
        } else {
            lastDisposable?.dispose()
            lastDisposable = null
            updateProgressDisplay()
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
                        val drawable = it.asDrawable(context.resources)
                        blendView.setImageBitmap(it.toBitmap())
                        coverSimpleImageView.setImageDrawable(drawable)
                        fullPlayerToolbar.setImageViewCover(drawable)
                        floatingPanelLayout.transitionImageView?.setImageDrawable(drawable)
                        floatingPanelLayout.setPreviewCover(drawable)
                    }, onError = {
                        val drawable = it?.asDrawable(context.resources)
                        blendView.setImageBitmap(it?.toBitmap())
                        coverSimpleImageView.setImageDrawable(drawable)
                        fullPlayerToolbar.setImageViewCover(drawable)
                        floatingPanelLayout.transitionImageView?.setImageDrawable(drawable)
                        floatingPanelLayout.setPreviewCover(drawable)
                    }) // do not react to onStart() which sets placeholder
                    allowHardware(coverSimpleImageView.isHardwareAccelerated)
                }.build()
            )
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        onPlaybackStateChanged(instance?.playbackState ?: Player.STATE_IDLE)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateRepeatButton(repeatMode)
    }

    override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
        Log.d("FullPlayer", "onPlaybackStateChanged: $playbackState")
        val isPlaying = instance?.isPlaying == true
        if (isPlaying) {
            controllerButton.playAnimation(false)
        } else if (playbackState != Player.STATE_BUFFERING) {
            controllerButton.playAnimation(true)
        }
        if (isPlaying) {
            startPositionUpdates()
        } else {
            stopPositionUpdates()
            updateProgressDisplay()
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

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        updateVolumeSlider(volume)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: @Player.TimelineChangeReason Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
            updateProgressDisplay()
        }
    }

    override fun onDetachedFromWindow() {
        stopPositionUpdates()
        if (isVolumeReceiverRegistered) {
            runCatching { context.unregisterReceiver(volumeChangeReceiver) }
            isVolumeReceiverRegistered = false
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isVolumeReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction("android.media.MASTER_VOLUME_CHANGED_ACTION")
                addAction("android.media.MASTER_MUTE_CHANGED_ACTION")
                addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(volumeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(volumeChangeReceiver, filter)
            }
            isVolumeReceiverRegistered = true
        }
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
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}
