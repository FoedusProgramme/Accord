package uk.akane.accord.logic.utils.exoplayer

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import uk.akane.accord.BuildConfig
import uk.akane.accord.logic.utils.CircularShuffleOrder


/**
 * If player in STATE_ENDED is resumed, state will be STATE_READY, on play button press it will
 * update to STATE_ENDED and only then media3 will wrap around playlist for us. This is a workaround
 * to restore STATE_ENDED as well and fake it for media3 until it indeed wraps around playlist.
 */
@OptIn(UnstableApi::class)
class EndedWorkaroundPlayer(exoPlayer: ExoPlayer) : ForwardingSimpleBasePlayer(exoPlayer), Player.Listener {

    companion object {
        private const val TAG = "EndedWorkaroundPlayer"
    }

    private val remoteDeviceInfo = DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE).build()
    init {
        player.addListener(this)
    }
    val exoPlayer
        get() = player as ExoPlayer
    // TODO: can't we do this in a cleaner way?
    var nextShuffleOrder:
            ((firstIndex: Int, mediaItemCount: Int, EndedWorkaroundPlayer) -> CircularShuffleOrder)?
            = null
    var isEnded = false
        set(value) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "isEnded set to $value (was $field)")
            }
            field = value
        }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == DISCONTINUITY_REASON_SEEK) {
            isEnded = false
        }
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
    }

    override fun getState(): State {
        if (isEnded) {
            val superState = super.state
            if (superState.playerError != null) {
                isEnded = false
                return superState
            }
            return superState.buildUpon().setPlaybackState(STATE_ENDED).setIsLoading(false).build()
        }
        if (player.currentTimeline.isEmpty) {
            return super.state.buildUpon().setDeviceInfo(remoteDeviceInfo).build()
        }
        return super.getState()
    }
}