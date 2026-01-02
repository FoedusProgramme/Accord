package uk.akane.accord.ui.components.player

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.isDarkMode
import uk.akane.accord.logic.playOrPause
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.components.NavigationBar
import uk.akane.cupertino.widget.image.SimpleImageView

class PreviewPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    FloatingPanelLayout.OnSlideListener {
    private var controlMaterialButton: MaterialButton
    private var nextMaterialButton: MaterialButton
    private var coverSimpleImageView: SimpleImageView
    private var titleTextView: TextView
    private val floatingPanelLayout: FloatingPanelLayout
        get() = parent as FloatingPanelLayout
    private val activity: MainActivity
        get() = context as MainActivity

    private val expandedBackgroundColor = resources.getColor(R.color.navigationBarExpandedBackground, null)
    private val blurAppendColor = resources.getColor(R.color.navigationBarBlurAppendColor, null)
    private val blurAppendColorDark = resources.getColor(R.color.navigationBarBlurAppendDarkModeColor, null)
    private val backgroundLocation = IntArray(2)
    private val selfLocation = IntArray(2)
    private val backgroundRenderNode = RenderNode("PreviewPlayerBlur").apply {
        setRenderEffect(
            RenderEffect.createBlurEffect(
                NavigationBar.BLUR_STRENGTH.dp.px,
                NavigationBar.BLUR_STRENGTH.dp.px,
                Shader.TileMode.MIRROR
            )
        )
    }

    private var lastControlIconRes: Int? = null
    private var lastTitleText: String? = null
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackControls()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackControls(playbackState)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateTitle()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateTitle()
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            updatePlaybackControls()
        }
    }

    override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
        when (status) {
            FloatingPanelLayout.SlideStatus.COLLAPSED -> {
                coverSimpleImageView.alpha = 1F
            }
            else -> {
                coverSimpleImageView.alpha = 0F
            }
        }
    }

    override fun onSlide(value: Float) {
    }

    init {
        inflate(context, R.layout.layout_preview_player, this)
        controlMaterialButton = findViewById(R.id.control_btn)
        nextMaterialButton = findViewById(R.id.next_btn)
        coverSimpleImageView = findViewById(R.id.preview_cover)
        titleTextView = findViewById(R.id.title)

        coverSimpleImageView.doOnLayout {
            floatingPanelLayout.setupMetrics(
                coverSimpleImageView.width
            )
        }

        doOnLayout {
            floatingPanelLayout.addOnSlideListener(this)
        }

        controlMaterialButton.setOnClickListener {
            activity.getPlayer()?.playOrPause()
        }

        nextMaterialButton.setOnClickListener {
            activity.getPlayer()?.seekToNext()
        }

        activity.controllerViewModel.addControllerCallback(activity.lifecycle) { controller, _ ->
            controller.addListener(playerListener)
            updatePlaybackControls(controller.playbackState)
            updateTitle(controller)
        }

        updatePlaybackControls()
        updateTitle()
    }

    override fun dispatchDraw(canvas: Canvas) {
        drawBlurredBackground(canvas)
        super.dispatchDraw(canvas)
    }

    fun setCover(drawable: Drawable?) {
        coverSimpleImageView.setImageDrawable(drawable)
    }

    private fun updatePlaybackControls(playbackState: Int? = null) {
        val player = activity.getPlayer()
        if (player == null) {
            controlMaterialButton.isEnabled = false
            nextMaterialButton.isEnabled = false
            return
        }

        controlMaterialButton.isEnabled =
            player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
        nextMaterialButton.isEnabled =
            player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)

        val resolvedPlaybackState = playbackState ?: player.playbackState
        if (resolvedPlaybackState == Player.STATE_BUFFERING) return

        val iconRes = if (player.isPlaying) {
            R.drawable.ic_prop_pause
        } else {
            R.drawable.ic_prop_play
        }
        if (iconRes != lastControlIconRes) {
            controlMaterialButton.setIconResource(iconRes)
            lastControlIconRes = iconRes
        }
    }

    private fun updateTitle(playerOverride: Player? = null) {
        val player = playerOverride ?: activity.getPlayer()
        val title = player?.mediaMetadata?.title?.toString()?.trim().orEmpty()
        val resolved = if (title.isNotEmpty()) {
            title
        } else {
            resources.getString(R.string.default_track)
        }
        if (resolved != lastTitleText) {
            titleTextView.text = resolved
            lastTitleText = resolved
        }
    }

    private fun drawBlurredBackground(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val backgroundView = activity.findViewById<android.view.View>(R.id.shrink_container)
            ?: return
        if (backgroundView.width == 0 || backgroundView.height == 0) return

        backgroundRenderNode.setPosition(0, 0, width, height)
        val recordingCanvas = backgroundRenderNode.beginRecording(width, height)
        recordingCanvas.drawColor(expandedBackgroundColor)

        backgroundView.getLocationOnScreen(backgroundLocation)
        getLocationOnScreen(selfLocation)
        val offsetX = backgroundLocation[0] - selfLocation[0]
        val offsetY = backgroundLocation[1] - selfLocation[1]
        recordingCanvas.save()
        recordingCanvas.translate(offsetX.toFloat(), offsetY.toFloat())
        backgroundView.draw(recordingCanvas)
        recordingCanvas.restore()
        backgroundRenderNode.endRecording()

        canvas.drawRenderNode(backgroundRenderNode)
        if (context.isDarkMode()) {
            canvas.drawColor(blurAppendColor, BlendMode.OVERLAY)
            canvas.drawColor(blurAppendColorDark)
        } else {
            canvas.drawColor(blurAppendColor, BlendMode.HARD_LIGHT)
        }
    }
}
