package uk.akane.accord.logic.player

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioTrack
import androidx.media3.common.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

@UnstableApi
class PostAmpAudioSink(
	val sink: DefaultAudioSink,
    val context: Context
) : ForwardingAudioSink(sink) {
	companion object {
		private const val TAG = "PostAmpAudioSink"
	}
	private val receiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action == "android.media.VOLUME_CHANGED_ACTION"
				|| intent?.action == "android.media.MASTER_VOLUME_CHANGED_ACTION"
				|| intent?.action == "android.media.MASTER_MUTE_CHANGED_ACTION"
				|| intent?.action == "android.media.STREAM_MUTE_CHANGED_ACTION") {
				myOnReceiveBroadcast(intent)
			}
		}
	}
	private var volumeEffect: Volume? = null
	private var format: Format? = null
	private var pendingFormat: Format? = null
	private var audioSessionId = 0

	init {
		ContextCompat.registerReceiver(
			context,
			receiver,
			IntentFilter().apply {
				addAction("android.media.VOLUME_CHANGED_ACTION")
				addAction("android.media.MASTER_VOLUME_CHANGED_ACTION")
				addAction("android.media.MASTER_MUTE_CHANGED_ACTION")
				addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
			},
			@SuppressLint("WrongConstant") // why is this needed?
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
	}

	override fun setListener(listener: AudioSink.Listener) {
		super.setListener(object : AudioSink.Listener by listener {
			override fun onPositionAdvancing(playoutStartSystemTimeMs: Long) {
				listener.onPositionAdvancing(playoutStartSystemTimeMs)
			}

			override fun onOffloadBufferEmptying() {
				listener.onOffloadBufferEmptying()
			}

			override fun onOffloadBufferFull() {
				listener.onOffloadBufferFull()
			}

			override fun onAudioSinkError(audioSinkError: Exception) {
				listener.onAudioSinkError(audioSinkError)
			}

			override fun onAudioCapabilitiesChanged() {
				listener.onAudioCapabilitiesChanged()
			}

			override fun onAudioTrackInitialized(audioTrackConfig: AudioSink.AudioTrackConfig) {
				myApplyPendingConfig()
				listener.onAudioTrackInitialized(audioTrackConfig)
			}

			override fun onAudioTrackReleased(audioTrackConfig: AudioSink.AudioTrackConfig) {
				listener.onAudioTrackReleased(audioTrackConfig)
			}

			override fun onSilenceSkipped() {
				listener.onSilenceSkipped()
			}

			override fun onAudioSessionIdChanged(audioSessionId: Int) {
				mySetAudioSessionId(audioSessionId)
				listener.onAudioSessionIdChanged(audioSessionId)
			}

			override fun onRoutingChanged(router: AudioTrack, routedDevice: AudioDeviceInfo?) {
				myOnRoutingChanged(router, routedDevice)
				listener.onRoutingChanged(router, routedDevice)
			}
		})
	}

	override fun configure(
		inputFormat: Format,
		specifiedBufferSize: Int,
		outputChannels: IntArray?
	) {
		pendingFormat = inputFormat
		super.configure(inputFormat, specifiedBufferSize, outputChannels)
	}

	private fun myOnReceiveBroadcast(intent: Intent) {
		// TODO
	}

	private fun myApplyPendingConfig() {
		format = pendingFormat
		Log.i(TAG, "set format to $format")
		// TODO
	}

	private fun mySetAudioSessionId(id: Int) {
		if (id != audioSessionId) {
			Log.i(TAG, "set session id to $id")
			if (audioSessionId != 0) {
				/*volumeEffect!!.let {
					CoroutineScope(Dispatchers.Default).launch {
						it.enabled = false
						it.release()
					}
				}
				volumeEffect = null*/
			}
			if (id != 0) {
				/*volumeEffect = Volume(99999, id)
				volumeEffect?.enabled = true*/
			}
		}
	}

	private fun myOnRoutingChanged(router: AudioTrack, routedDevice: AudioDeviceInfo?) {
		// TODO
	}

	private fun onAudioTrackPlayStateChanging() {
		// TODO
	}

	override fun play() {
		onAudioTrackPlayStateChanging()
		super.play()
	}

	override fun pause() {
		onAudioTrackPlayStateChanging()
		super.pause()
	}

	override fun flush() {
		onAudioTrackPlayStateChanging()
		super.flush()
	}

	override fun release() {
		context.unregisterReceiver(receiver)
		super.release()
	}

	override fun handleBuffer(
		buffer: ByteBuffer,
		presentationTimeUs: Long,
		encodedAccessUnitCount: Int
	): Boolean {
		val prev = sink.isAudioTrackStopped()
		val ret = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
		if (sink.isAudioTrackStopped() != prev) {
			onAudioTrackPlayStateChanging()
		}
		return ret
	}

	private val audioTrackField by lazy {
		DefaultAudioSink::class.java.getDeclaredField("audioTrack").apply {
			isAccessible = true
		}
	}

	private val audioTrackStoppedField by lazy {
		DefaultAudioSink::class.java.getDeclaredField("stoppedAudioTrack").apply {
			isAccessible = true
		}
	}

	private fun DefaultAudioSink.getAudioTrack(): AudioTrack? {
		return audioTrackField.get(this) as AudioTrack?
	}

	private fun DefaultAudioSink.isAudioTrackStopped(): Boolean {
		return audioTrackStoppedField.get(this) as Boolean
	}
}
