/*
 *     Copyright (C) 2025 nift4
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.nift4.gramophone.hificore

import android.annotation.SuppressLint
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.annotation.RequiresApi

object AudioTrackHiddenApi {
    private const val TAG = "AudioTrackHiddenApi"
    private const val TRACE_TAG = "GpNativeTrace"
    var libLoaded = false
        private set

    data class MixPort(val id: Int, val ioHandle: Int, val name: String?, val flags: Int?, val channelMask: Int?, val format: UInt?, val sampleRate: UInt?, val hwModule: Int?, val fast: Boolean?)
    init {
        if (canLoadLib()) {
            try {
                Log.d(TRACE_TAG, "Loading libhificore.so")
                System.loadLibrary("hificore")
                libLoaded = true
                Log.d(TRACE_TAG, "Done loading libhificore.so")
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
    }

    /**
     * A device may be banned from loading `libhificore.so` because it has crashed with a
     * memory-related problem in Play Console. In order to rule out this being related to hacks in
     * `libhificore.so`, these devices are banned from loading it. If further crashes appear, the
     * ban can be lifted again because it's not our fault. If there are no further crashes, it may
     * indicate an incompatibility of `libhificore.so` with the device's firmware.
     */
    fun canLoadLib(): Boolean {
        return !(Build.VERSION.SDK_INT == 33 && Build.BRAND == "TECNO" &&
                Build.PRODUCT.startsWith("BG6-")) && // Tecno SPARK Go 2024
            !(Build.VERSION.SDK_INT == 34 && Build.BRAND == "samsung" &&
                    Build.DEVICE == "dm1q") // Samsung Galaxy S23
    }

    @SuppressLint("PrivateApi")
    private fun getAudioTrackPtr(audioTrack: AudioTrack): Long {
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot get pointer for released AudioTrack")
        val cls = audioTrack.javaClass
        val field = cls.getDeclaredField("mNativeTrackInJavaObj")
        field.isAccessible = true
        return field.get(audioTrack) as Long
    }

    fun getHalSampleRate(audioTrack: AudioTrack): UInt? {
        if (!libLoaded)
            return null
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot get hal sample rate for released AudioTrack")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // getHalSampleRate() exists since below commit which first appeared in Android U
            // https://cs.android.com/android/_/android/platform/frameworks/av/+/310037a32d56e361d5b5156b74f8846f92bc245e
            val ret = try {
                getHalSampleRateInternal(getAudioTrackPtr(audioTrack))
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
                null
            }
            if (ret != null && ret != 0)
                return ret.toUInt()
            return null
        }
        val output = getOutput(audioTrack)
        if (output == null)
            return null
        val af = getAfService()
        if (af == null)
            return null
        val inParcel = obtainParcel(af)
        val outParcel = obtainParcel(af)
        try {
            inParcel.writeInterfaceToken(af.interfaceDescriptor!!)
            inParcel.writeInt(output)
            // IAudioFlingerService.sampleRate(audio_io_handle_t)
            Log.d(TRACE_TAG, "trying to call sampleRate() via binder")
            try {
                af.transact(3, inParcel, outParcel, 0)
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
            Log.d(TRACE_TAG, "done calling format() via binder")
            if (!readStatus(outParcel))
                return null
            return outParcel.readInt().toUInt()
        } finally {
            inParcel.recycle()
            outParcel.recycle()
        }
    }
    private external fun getHalSampleRateInternal(@Suppress("unused") audioTrackPtr: Long): Int

    private fun obtainParcel(binder: IBinder) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Parcel.obtain(binder) else Parcel.obtain()

    fun getHalChannelCount(audioTrack: AudioTrack): Int? {
        if (!libLoaded)
            return null
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot get hal channel count for released AudioTrack")
        // before U, caller should query channel mask from audio_port/audio_port_v7
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) null else
        // getHalChannelCount() exists since below commit which first appeared in Android U
        // https://cs.android.com/android/_/android/platform/frameworks/av/+/310037a32d56e361d5b5156b74f8846f92bc245e
            try {
                Log.d(TRACE_TAG, "calling native getHalChannelCountInternal/getAudioTrackPtr")
                getHalChannelCountInternal(getAudioTrackPtr(audioTrack))
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
                null
            }.also { Log.d(TRACE_TAG, "native getHalChannelCountInternal/getAudioTrackPtr is done: $it") }
    }
    private external fun getHalChannelCountInternal(@Suppress("unused") audioTrackPtr: Long): Int

    fun getHalFormat(audioTrack: AudioTrack): UInt? {
        if (!libLoaded)
            return null
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot get hal format for released AudioTrack")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // getHalFormat() exists since below commit which first appeared in Android U
            // https://cs.android.com/android/_/android/platform/frameworks/av/+/310037a32d56e361d5b5156b74f8846f92bc245e
            val ret = try {
                Log.d(TRACE_TAG, "calling native getHalFormatInternal/getAudioTrackPtr")
                getHalFormatInternal(getAudioTrackPtr(audioTrack))
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
                null
            }.also { Log.d(TRACE_TAG, "native getHalChannelCountInternal/getAudioTrackPtr is done: $it") }
            if (ret != null && ret != 0)
                return ret.toUInt()
            return null
        }
        val output = getOutput(audioTrack)
        if (output == null)
            return null
        val af = getAfService()
        if (af == null)
            return null
        val inParcel = obtainParcel(af)
        val outParcel = obtainParcel(af)
        try {
            inParcel.writeInterfaceToken(af.interfaceDescriptor!!)
            inParcel.writeInt(output)
            // IAudioFlingerService.format(audio_io_handle_t)
            Log.d(TRACE_TAG, "trying to call format() via binder")
            try {
                af.transact(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 4 else 5,
                    inParcel, outParcel, 0
                )
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
            Log.d(TRACE_TAG, "done calling format() via binder")
            if (!readStatus(outParcel))
                return null
            // In T, return value changed from legacy audio_format_t to AudioFormatDescription
            // https://cs.android.com/android/_/android/platform/frameworks/av/+/b60bd1b586b74ddf375257c4d07323e271d84ff3
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (outParcel.readInt() != 1 /* kNonNullParcelableFlag */) {
                    Log.e(TAG, "got a null parcelable unexpectedly")
                    return null
                }
                return simplifyAudioFormatDescription(outParcel)?.toUInt()
            } else
                return outParcel.readInt().toUInt()
        } finally {
            inParcel.recycle()
            outParcel.recycle()
        }
    }
    private external fun getHalFormatInternal(@Suppress("unused") audioTrackPtr: Long): Int

    @SuppressLint("PrivateApi") // sorry, not sorry...
    private fun listAudioPorts(): Pair<List<Any>, Int>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null // while listAudioPorts exists in L, it just returns an error
        val ports = ArrayList<Any?>()
        val generation = IntArray(1)
        try {
            Class.forName("android.media.AudioSystem").getMethod(
                "listAudioPorts", ArrayList::class.java, IntArray::class.java
            ).invoke(null, ports, generation) as Int
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
        if (ports.contains(null))
            Log.e(TAG, "why does listAudioPorts() return a null port?!")
        return ports.filterNotNull() to generation[0]
    }

    @SuppressLint("PrivateApi") // only Android T, private API stability
    private fun simplifyAudioFormatDescription(out: Parcel): Int? {
        return try {
            Class.forName("android.media.audio.common.AidlConversion").getDeclaredMethod(
                "aidl2legacy_AudioFormatDescription_Parcel_audio_format_t", Parcel::class.java
            ).also {
                it.isAccessible = true
            }.invoke(null, out) as Int
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            null
        }
    }

    private fun getMixPortMetadata(id: Int, io: Int): IntArray? {
        if (!libLoaded)
            return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null // need listAudioPorts or getAudioPort
        return try {
            Log.d(TRACE_TAG, "calling native findAfFlagsForPortInternal")
            val result = findAfFlagsForPortInternal(id, io)
                .also { Log.d(TRACE_TAG, "native findAfFlagsForPortInternal is done: $it") }
            if (result == null) return null // something went wrong. native layer logged reason to logcat
            return result
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            null
        }
    }
    @Suppress("unused") // for parameters
    private external fun findAfFlagsForPortInternal(id: Int, sr: Int): IntArray?

    fun findAfTrackFlags(dump: String?, latency: Int?, track: AudioTrack, grantedFlags: Int?): Int? {
        if (!libLoaded)
            return null
        // First exposure to client process was below commit, which first appeared in U QPR2.
        // https://cs.android.com/android/_/android/platform/frameworks/av/+/94ed47c6b6ca5a69b90238f6ae97af2ce7df9be0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            return null
        try {
            val dump = dump ?: throw NullPointerException("af track dump is null, check prior logs")
            val latency = latency ?: throw NullPointerException("af track latency is null, check prior logs")
            val theLine = dump.split('\n').first { it.contains("AF SampleRate") }
            val theLine2 = dump.split('\n').first { it.contains("format(0x") }
            val regex = Regex(".*AF latency \\(([0-9]+)\\) AF frame count\\(([0-9]+)\\) AF SampleRate\\(([0-9]+)\\).*")
            val regex2 = Regex(".*format\\(0x([0-9a-f]+)\\), .*")
            val match = regex.matchEntire(theLine) ?: throw NullPointerException("failed match of $theLine")
            val match2 = regex2.matchEntire(theLine2) ?: throw NullPointerException("failed match2 of $theLine2")
            val afLatency = match.groupValues.getOrNull(1)?.toIntOrNull()
                ?: throw NullPointerException("failed parsing afLatency in: $theLine")
            val afFrameCount = match.groupValues.getOrNull(2)?.toLongOrNull()
                ?: throw NullPointerException("failed parsing afFrameCount in: $theLine")
            val afSampleRate = match.groupValues.getOrNull(3)?.toIntOrNull()
                ?: throw NullPointerException("failed parsing afSampleRate in: $theLine")
            val format = match2.groupValues.getOrNull(1)?.toUIntOrNull(radix = 16)?.toInt()
                ?: throw NullPointerException("failed parsing format in: $theLine2")
            val ptr = getAudioTrackPtr(track)
            Log.d(TRACE_TAG, "calling native findAfTrackFlagsInternal")
            return findAfTrackFlagsInternal(ptr, afLatency, afFrameCount, afSampleRate, latency, format).let {
                if (it == Int.MAX_VALUE || it == Int.MIN_VALUE)
                    null // something went wrong, this was logged to logcat
                else if (grantedFlags != null && (it or grantedFlags) != it) {
                    // should never happen
                    Log.e(TAG, "af track flags($it) are nonsense, |$grantedFlags = ${it or grantedFlags}")
                    null
                } else it
            }.also { Log.d(TRACE_TAG, "native findAfTrackFlagsInternal is done: $it") }
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }
    @Suppress("unused") // for parameters
    private external fun findAfTrackFlagsInternal(pointer: Long, afLatency: Int, afFrameCount: Long,
                                                  afSampleRate: Int, latency: Int, format: Int): Int

    fun getOutput(audioTrack: AudioTrack): Int? {
        if (!libLoaded)
            return null
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot get hal output for released AudioTrack")
        Log.d(TRACE_TAG, "calling native getOutputInternal/getAudioTrackPtr")
        return try {
            getOutputInternal(getAudioTrackPtr(audioTrack))
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            null
        }.also { Log.d(TRACE_TAG, "native getOutputInternal/getAudioTrackPtr is done: $it") }
    }
    private external fun getOutputInternal(@Suppress("unused") audioTrackPtr: Long): Int

    fun getGrantedFlags(audioTrack: AudioTrack): Int? {
        if (!libLoaded)
            return null
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot get hal output for released AudioTrack")
        Log.d(TRACE_TAG, "calling native getOutputInternal/getAudioTrackPtr")
        return try {
            getFlagsInternal(audioTrack, getAudioTrackPtr(audioTrack)).let {
                if (it == Int.MAX_VALUE || it == Int.MIN_VALUE)
                    null // something went wrong, native side logged reason to logcat
                else it
            }
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            null
        }.also { Log.d(TRACE_TAG, "native getOutputInternal/getAudioTrackPtr is done: $it") }
    }
    @Suppress("unused") // for parameters
    /*private*/ external fun getFlagsInternal(audioTrack: AudioTrack?, audioTrackPtr: Long): Int

    fun dump(audioTrack: AudioTrack): String? {
        if (!libLoaded)
            return null
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED)
            throw IllegalArgumentException("cannot dump released AudioTrack")
        Log.d(TRACE_TAG, "calling native dump/getAudioTrackPtr")
        return try {
            dumpInternal(getAudioTrackPtr(audioTrack))
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            null
        }.also { Log.d(TRACE_TAG, "native dump/getAudioTrackPtr is done: $it") }
    }
    /*private*/ external fun dumpInternal(@Suppress("unused") audioTrackPtr: Long): String

    @SuppressLint("PrivateApi") // only used below U, stable private API
    private fun getAfService(): IBinder? {
        return try {
            Class.forName("android.os.ServiceManager").getMethod(
                "getService", String::class.java
            ).invoke(null, "media.audio_flinger") as IBinder?
        } catch (e: Throwable) {
            Log.e(TAG, Log.getStackTraceString(e))
            null
        }
    }

    private fun readStatus(parcel: Parcel): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val status = parcel.readInt()
        if (status == 0) return true
        Log.e(TAG, "binder transaction failed with status $status")
        return false
    }

    fun getMixPortForThread(oid: Int?): MixPort? {
        if (oid == null)
            return null
        val ports = listAudioPorts()
        if (ports != null)
            for (port in ports.first) {
                try {
                    if (port.javaClass.canonicalName != "android.media.AudioMixPort") continue
                    val ioHandle = port.javaClass.getMethod("ioHandle").invoke(port) as Int
                    if (ioHandle != oid) continue
                    return getMixPort(port)
                } catch (t: Throwable) {
                    Log.e(TAG, Log.getStackTraceString(t))
                }
            }
        return null
    }

    fun getPrimaryMixPort(): MixPort? {
        val ports = listAudioPorts()
        if (ports != null)
            for (port in ports.first) {
                try {
                    if (port.javaClass.canonicalName != "android.media.AudioMixPort") continue
                    val mixPort = getMixPort(port)
                    // TODO: support android below T where flags is null
                    if (mixPort.flags != null && (mixPort.flags and 2 /* AUDIO_OUTPUT_FLAG_PRIMARY */) != 0)
                        return mixPort
                } catch (t: Throwable) {
                    Log.e(TAG, Log.getStackTraceString(t))
                }
            }
        return null
    }

    private fun getMixPort(port: Any): MixPort {
        val ioHandle = port.javaClass.getMethod("ioHandle").invoke(port) as Int
        val id = port.javaClass.getMethod("id").invoke(port) as Int
        val name = port.javaClass.getMethod("name").invoke(port) as String?
        val mixPortData = getMixPortMetadata(id, ioHandle)
        // flags exposed to app process since below commit which first appeared in T release.
        // https://cs.android.com/android/_/android/platform/frameworks/av/+/99809024b36b243ad162c780c1191bb503a8df47
        return MixPort(id, ioHandle, name, flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            mixPortData?.get(3) else null, channelMask = mixPortData?.get(2),
            format = mixPortData?.get(1)?.toUInt(), sampleRate = mixPortData?.get(0)?.toUInt(),
            hwModule = mixPortData?.get(4), fast = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                mixPortData?.let { it[5] == 0 } else null)
    }

    private val idRegex = Regex(".*id\\((.*)\\) .*")
    fun getPortIdFromDump(dump: String?): Int? {
        if (dump == null)
            return null
        // ID is in dump output since below commit which first appeared in Q.
        // https://cs.android.com/android/_/android/platform/frameworks/av/+/fb8ede2a020e741cb892ee024fcfba7e689183f2
        // https://cs.android.com/android/_/android/platform/frameworks/av/+/973db02ac18fa1de9ce6221f47b01af1bdc4bec2
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return null
        val dt = dump.trim().split('\n').map { it.trim() }
        if (dt.size < 2) {
            Log.e(
                TAG,
                "getIdFromDump() failure: not enough lines, DUMP:\n$dump"
            )
            return null
        }
        if (dt[0] != "AudioTrack::dump") {
            Log.e(
                TAG,
                "getIdFromDump() failure: L0 isn't AudioTrack::dump, DUMP:\n$dump"
            )
            return null
        }
        if (!dt[1].contains("id(")) {
            Log.e(
                TAG,
                "getIdFromDump() failure: L1 didn't contain id(, DUMP:\n$dump"
            )
            return null
        }
        val idText = idRegex.matchEntire(dt[1])?.groupValues[1]
        if (idText == null) {
            Log.e(
                TAG,
                "getIdFromDump() failure: L1 didn't match regex, DUMP:\n$dump"
            )
            return null
        }
        idText.toIntOrNull()?.let { return it }
        Log.e(
            TAG,
            "getIdFromDump() failure: $idText didn't convert to int from base 10, DUMP:\n$dump"
        )
        return null
    }

    private val notificationRegex = Regex(".*notif. frame count\\((.*)\\), req\\. notif\\. frame .*")
    fun getNotificationFramesActFromDump(dump: String?): Int? {
        if (dump == null)
            return null
        // ID is in dump output since below commit which first appeared in P.
        // https://cs.android.com/android/_/android/platform/frameworks/av/+/d114b624ea2ec5c51779b74132a60b4a46f6cdba
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            return null
        val dt = dump.trim().split('\n').map { it.trim() }
        if (dt.size < 7) {
            Log.e(
                TAG,
                "getNotificationFramesActFromDump() failure: not enough lines, DUMP:\n$dump"
            )
            return null
        }
        if (dt[0] != "AudioTrack::dump") {
            Log.e(
                TAG,
                "getNotificationFramesActFromDump() failure: L0 isn't AudioTrack::dump, DUMP:\n$dump"
            )
            return null
        }
        if (!dt[6].contains("notif. frame count(")) {
            Log.e(
                TAG,
                "getNotificationFramesActFromDump() failure: L6 didn't contain notif. frame count(, DUMP:\n$dump"
            )
            return null
        }
        val notificationText = notificationRegex.matchEntire(dt[6])?.groupValues[1]
        if (notificationText == null) {
            Log.e(
                TAG,
                "getNotificationFramesActFromDump() failure: L6 didn't match regex, DUMP:\n$dump"
            )
            return null
        }
        notificationText.toIntOrNull()?.let { return it }
        Log.e(
            TAG,
            "getNotificationFramesActFromDump() failure: $notificationText didn't convert to int, DUMP:\n$dump"
        )
        return null
    }

    private val latencyRegex = Regex(".*\\), latency \\((.*)\\).*")
    fun getLatencyFromDump(dump: String?): Int? {
        if (dump == null)
            return null
        // use AudioTrack.getLatency() on N+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return null
        val dt = dump.trim().split('\n').map { it.trim() }
        if (dt.size < 5) {
            Log.e(
                TAG,
                "getLatencyFromDump() failure: not enough lines, DUMP:\n$dump"
            )
            return null
        }
        if (dt[0] != "AudioTrack::dump") {
            Log.e(
                TAG,
                "getLatencyFromDump() failure: L0 isn't AudioTrack::dump, DUMP:\n$dump"
            )
            return null
        }
        if (!dt[4].contains("latency (")) {
            Log.e(
                TAG,
                "getLatencyFromDump() failure: L4 didn't contain latency (, DUMP:\n$dump"
            )
            return null
        }
        val text = latencyRegex.matchEntire(dt[4])?.groupValues[1]
        if (text == null) {
            Log.e(
                TAG,
                "getLatencyFromDump() failure: L4 didn't match regex, DUMP:\n$dump"
            )
            return null
        }
        text.toIntOrNull()?.let { return it }
        Log.e(
            TAG,
            "getLatencyFromDump() failure: $text didn't convert to int, DUMP:\n$dump"
        )
        return null
    }

    private val frameCountRegex = Regex(".*\\), frame count \\((.*)\\).*")
    fun getFrameCountFromDump(dump: String?): Long? {
        if (dump == null)
            return null
        val dt = dump.trim().split('\n').map { it.trim() }
        if (dt.size < 3) {
            Log.e(
                TAG,
                "getFrameCountFromDump() failure: not enough lines, DUMP:\n$dump"
            )
            return null
        }
        if (dt[0] != "AudioTrack::dump") {
            Log.e(
                TAG,
                "getFrameCountFromDump() failure: L0 isn't AudioTrack::dump, DUMP:\n$dump"
            )
            return null
        }
        if (!dt[2].contains("latency (")) {
            Log.e(
                TAG,
                "getFrameCountFromDump() failure: L2 didn't contain latency (, DUMP:\n$dump"
            )
            return null
        }
        val text = frameCountRegex.matchEntire(dt[2])?.groupValues[1]
        if (text == null) {
            Log.e(
                TAG,
                "getFrameCountFromDump() failure: L2 didn't match regex, DUMP:\n$dump"
            )
            return null
        }
        text.toLongOrNull()?.let { return it }
        Log.e(
            TAG,
            "getFrameCountFromDump() failure: $text didn't convert to int, DUMP:\n$dump"
        )
        return null
    }

    private val stateRegex = Regex(".*state\\((.*)\\), latency.*")
    fun getStateFromDump(dump: String?): Int? {
        if (dump == null)
            return null
        val dt = dump.trim().split('\n').map { it.trim() }
        if (dt.size < 5) {
            Log.e(
                TAG,
                "getStateFromDump() failure: not enough lines, DUMP:\n$dump"
            )
            return null
        }
        if (dt[0] != "AudioTrack::dump") {
            Log.e(
                TAG,
                "getStateFromDump() failure: L0 isn't AudioTrack::dump, DUMP:\n$dump"
            )
            return null
        }
        if (!dt[4].contains("state(")) {
            Log.e(
                TAG,
                "getStateFromDump() failure: L4 didn't contain state(, DUMP:\n$dump"
            )
            return null
        }
        val text = stateRegex.matchEntire(dt[2])?.groupValues[1]
        if (text == null) {
            Log.e(
                TAG,
                "getStateFromDump() failure: L4 didn't match regex, DUMP:\n$dump"
            )
            return null
        }
        text.toIntOrNull()?.let { return it }
        Log.e(
            TAG,
            "getStateFromDump() failure: $text didn't convert to int, DUMP:\n$dump"
        )
        return null
    }

    data class AudioConfigBase(val sampleRate: Int, val channelMask: Int, val format: Int)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun getEffectConfigs(ptr: Long): Pair<AudioConfigBase, AudioConfigBase> {
        val out = IntArray(6)
        val ret = getEffectConfigs(ptr, out)
        if (ret != 0) {
            throw IllegalStateException("getEffectConfigs() failed: $ret")
        }
        return AudioConfigBase(out[0], out[1], out[2]) to
                AudioConfigBase(out[3], out[4], out[5])
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private external fun getEffectConfigs(ptr: Long, out: IntArray): Int
}