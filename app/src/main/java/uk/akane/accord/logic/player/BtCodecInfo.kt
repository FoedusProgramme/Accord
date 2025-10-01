package uk.akane.accord.logic.player

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class BtCodecInfo(
    val codec: String?, val sampleRateHz: Int?, val channelConfig: Int?,
    val bitsPerSample: Int?, val quality: String?
) : Parcelable {
    companion object {
        private const val TAG = "BtCodecInfo"

        @RequiresApi(Build.VERSION_CODES.O)
        fun fromCodecConfig(codecConfig: BluetoothCodecConfig?): BtCodecInfo? {
            if (codecConfig == null) return null
            try {
                val codec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                    codecConfig.extendedCodecType?.codecName
                else {
                    val ct = @Suppress("deprecation") @SuppressLint("NewApi") codecConfig.codecType
                    val name = getCodecNameReflection(codecConfig)?.trim() ?: when {
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> "SBC"
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> "AAC"
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> "aptX"
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> "aptX HD"
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> "LDAC"
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3 -> "LC3"
                        ct == @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS -> "OPUS"
                        @Suppress("deprecation") @SuppressLint("NewApi") codecConfig.codecType ==
                                @Suppress("deprecation") BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID -> "INVALID CODEC"

                        else -> "UNKNOWN CODEC(${@Suppress("deprecation") @SuppressLint("NewApi") codecConfig.codecType})"
                    }
                    when (name) {
                        // P returns without space, some newer versions added space
                        "LHDCV2" -> "LHDC V2"
                        "LHDCV3" -> "LHDC V3"
                        "LHDCV5" -> "LHDC V5"
                        // other known ones: aptX Adaptive, aptX TWS+
                        else -> name
                    }
                }
                val sr = when ((@SuppressLint("NewApi") codecConfig.sampleRate)) {
                    BluetoothCodecConfig.SAMPLE_RATE_44100 -> 44100
                    BluetoothCodecConfig.SAMPLE_RATE_48000 -> 48000
                    BluetoothCodecConfig.SAMPLE_RATE_88200 -> 88200
                    BluetoothCodecConfig.SAMPLE_RATE_96000 -> 96000
                    BluetoothCodecConfig.SAMPLE_RATE_176400 -> 176400
                    BluetoothCodecConfig.SAMPLE_RATE_192000 -> 192000
                    else -> {
                        Log.e(
                            TAG, "unknown sample rate flag " +
                                    (@SuppressLint("NewApi") codecConfig.sampleRate)
                        ); null
                    }
                }
                return BtCodecInfo(
                    codec, sr, when ((@SuppressLint("NewApi")
                    codecConfig.channelMode)) {
                        BluetoothCodecConfig.CHANNEL_MODE_NONE -> AudioFormat.CHANNEL_INVALID
                        BluetoothCodecConfig.CHANNEL_MODE_MONO -> AudioFormat.CHANNEL_OUT_MONO
                        BluetoothCodecConfig.CHANNEL_MODE_STEREO -> AudioFormat.CHANNEL_OUT_STEREO
                        else -> {
                            Log.e(
                                TAG, "unknown channel mode flag " +
                                        (@SuppressLint("NewApi") codecConfig.channelMode)
                            ); null
                        }
                    }, when ((@SuppressLint("NewApi") codecConfig.bitsPerSample)) {
                        BluetoothCodecConfig.BITS_PER_SAMPLE_16 -> 16
                        BluetoothCodecConfig.BITS_PER_SAMPLE_24 -> 24
                        BluetoothCodecConfig.BITS_PER_SAMPLE_32 -> 32
                        else -> {
                            Log.e(
                                TAG, "unknown bit per sample flag " +
                                        (@SuppressLint("NewApi") codecConfig.bitsPerSample)
                            ); null
                        }
                    }, (@SuppressLint("NewApi") if (codec == "LDAC") when {
                        codecConfig.codecSpecific1 == 1000L || codecConfig.codecSpecific1 == 0L -> "Auto"
                        codecConfig.codecSpecific1 == 1002L && ((sr ?: 1) % 48000) == 0 -> "330kbps"
                        codecConfig.codecSpecific1 == 1001L && ((sr ?: 1) % 48000) == 0 -> "660kbps"
                        codecConfig.codecSpecific1 == 1003L && ((sr ?: 1) % 48000) == 0 -> "990kbps"
                        codecConfig.codecSpecific1 == 1002L && ((sr ?: 1) % 44100) == 0 -> "303kbps"
                        codecConfig.codecSpecific1 == 1001L && ((sr ?: 1) % 44100) == 0 -> "606kbps"
                        codecConfig.codecSpecific1 == 1003L && ((sr ?: 1) % 44100) == 0 -> "909kbps"
                        else -> "ERROR (${codecConfig.codecSpecific1})"
                    } else null)
                )
            } catch (t: Throwable) {
                Log.e(TAG, Log.getStackTraceString(t))
                return null
            }
        }

        private fun getCodecNameReflection(codecConfig: BluetoothCodecConfig): String? {
            return try {
                @SuppressLint("NewApi")
                BluetoothCodecConfig::class.java.getMethod("getCodecName")
                    .invoke(codecConfig) as String
            } catch (_: Throwable) {
                try {
                    @SuppressLint("NewApi")
                    BluetoothCodecConfig::class.java.getMethod("getCodecName", Int::class.java)
                        .invoke(
                            null,
                            @Suppress("deprecation") codecConfig.codecType
                        ) as String
                } catch (t: Throwable) {
                    Log.e(TAG, Log.getStackTraceString(t))
                    return null
                }
            }.takeIf { !it.startsWith("UNKNOWN CODEC") }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getCodec(context: Context, callback: (BtCodecInfo?) -> Unit): Proxy? {
            val adapter =
                ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter
                    ?: return null
            val sl = Proxy(adapter, callback, context)
            if (!adapter.getProfileProxy(context, sl, BluetoothProfile.A2DP)) {
                Log.e(TAG, "getProfileProxy error")
                callback(null)
                return null
            }
            return sl
        }

        class Proxy(
            val adapter: BluetoothAdapter, private val callback: (BtCodecInfo?) -> Unit,
            private val context: Context
        ) : BluetoothProfile.ServiceListener {
            var a2dp: BluetoothA2dp? = null
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile != BluetoothProfile.A2DP) {
                    Log.e(TAG, "wrong profile $profile connected")
                    return
                }
                a2dp = proxy as BluetoothA2dp
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "missing bluetooth permission")
                    callback(null)
                    return
                }
                val cd = a2dp!!.connectedDevices
                val device = if (cd.size <= 1) cd.firstOrNull() else try {
                    BluetoothA2dp::class.java.getMethod("getActiveDevice")
                        .invoke(a2dp) as BluetoothDevice?
                } catch (t: Throwable) {
                    Log.e(TAG, Log.getStackTraceString(t))
                    callback(null)
                    return
                }
                if (device == null) return
                @SuppressLint("NewApi")
                val codec = try {
                    BluetoothA2dp::class.java.getMethod(
                        "getCodecStatus",
                        BluetoothDevice::class.java
                    ).invoke(a2dp, device) as BluetoothCodecStatus?
                } catch (t: Throwable) {
                    Log.e(TAG, Log.getStackTraceString(t))
                    null
                }?.codecConfig
                callback(fromCodecConfig(codec))
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile != BluetoothProfile.A2DP) {
                    Log.e(TAG, "wrong profile $profile disconnected")
                    return
                }
                a2dp = null
            }
        }
    }
}
