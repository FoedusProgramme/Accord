/*
 *     Copyright (C) 2011 The Android Open Source Project
 *                   2025 nift4
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

// last observed AOSP size (arm64) was 1312

#define AUDIO_TRACK_SIZE 5000
// last observed AOSP size (arm64) was 152
#define ATTRIBUTION_SOURCE_SIZE 500

// make sure to call RefBase::incStrong on thePtr before giving fake_sp away, and call
// decStrong when you don't need it anymore :)
struct fake_sp {
    void* /* MUST be RefBase* (or compatible contract, but ymmv) */ thePtr;
};
// make sure to call RefBase::createWeak(thePtr, <unique id>) and save returned pointer to refs,
// then call RefBase::weakref_type::decWeak(refs, <unique id>) when you don't need it anymore
struct fake_wp {
    void* /* MUST be RefBase* */ thePtr;
    void* /* RefBase::weakref_type* */ refs;
};
struct String8 {
    const char* data;
};

extern "C" {
#define AUDIO_MAKE_OFFLOAD_INFO_VERSION(maj,min) \
            ((((maj) & 0xff) << 8) | ((min) & 0xff))
typedef struct {
    uint16_t version;                   // version of the info structure
    uint16_t size;                      // total size of the structure including version and size
    uint32_t sample_rate;               // sample rate in Hz
    uint32_t channel_mask;  // channel mask
    uint32_t format;              // audio format
    int32_t stream_type;    // stream type
    uint32_t bit_rate;                  // bit rate in bits per second
    int64_t duration_us;                // duration in microseconds, -1 if unknown
    bool has_video;                     // true if stream is tied to a video stream
    bool is_streaming;                  // true if streaming, false if local playback
    uint32_t bit_width;                 // version 0.1b:
    uint32_t offload_buffer_size;       // version 0.1b: offload fragment size
    int32_t usage;                // version 0.1b:
    bool use_small_bufs; // caf
} audio_offload_info_t_v26;
typedef struct {
    uint16_t version;                   // version of the info structure
    uint16_t size;                      // total size of the structure including version and size
    uint32_t sample_rate;               // sample rate in Hz
    uint32_t channel_mask;  // channel mask
    uint32_t format;              // audio format
    int32_t stream_type;    // stream type
    uint32_t bit_rate;                  // bit rate in bits per second
    int64_t duration_us;                // duration in microseconds, -1 if unknown
    bool has_video;                     // true if stream is tied to a video stream
    bool is_streaming;                  // true if streaming, false if local playback
    uint32_t bit_width;                 // version 0.1b:
    uint32_t offload_buffer_size;       // version 0.1b: offload fragment size
    int32_t usage;                // version 0.1b:
    int32_t encapsulation_mode;  // version 0.2:
    int32_t content_id;                 // version 0.2: content id from tuner hal (0 if none)
    int32_t sync_id;                    // version 0.2: sync id from tuner hal (0 if none)
} __attribute__((aligned(8))) audio_offload_info_t_v30;

/* Audio attributes */
typedef struct {
    int32_t content_type;
    int32_t usage;
    int32_t source;
    uint32_t flags;
    char tags[256]; /* UTF8 */
} audio_attributes_legacy; // before P
typedef struct {
    int32_t content_type;
    int32_t usage;
    int32_t source;
    uint32_t flags;
    char tags[256]; /* UTF8 */
} __attribute__((packed)) audio_attributes_v28;
}

struct audio_playback_rate {
	float mSpeed;
	float mPitch;
	int32_t  mStretchMode;
	int32_t mFallbackMode;
};

struct alignas(8) ExtendedTimestamp {
	ExtendedTimestamp() {
		memset(mPosition, 0, sizeof(mPosition));
		for (int i = 0; i < 5; ++i) {
			mTimeNs[i] = -1;
		}
		memset(mTimebaseOffset, 0, sizeof(mTimebaseOffset));
		mFlushed = 0;
	}

	int64_t mPosition[5];
	int64_t mTimeNs[5];
	int64_t mTimebaseOffset[2];
	int64_t mFlushed;
};

typedef void (*legacy_callback_t)(int event, void* user, void *info);
namespace android {
    // NOLINTBEGIN
    class RefBase {
    public:
        inline RefBase() {
            ZN7android7RefBaseC2Ev(this);
        }

        virtual inline ~RefBase() {
            ZN7android7RefBaseD2Ev(this);
        }

        inline void incStrong(void* id) {
            ZNK7android7RefBase9incStrongEPKv(this, id);
        }

        inline void decStrong(void* id) {
            ZNK7android7RefBase9decStrongEPKv(this, id);
        }

        inline void* createWeak(void* id) {
            return ZNK7android7RefBase10createWeakEPKv(this, id);
        }

        virtual void onFirstRef();

        virtual void onLastStrongRef(const void *id);

        virtual bool onIncStrongAttempted(uint32_t flags, const void *id);

        virtual void onLastWeakRef(const void *id);

        void *mRefs;
    };
    // NOLINTEND
    class AudioTimestamp {
    public:
        AudioTimestamp() : mPosition(0), mTime({ .tv_sec = 0, .tv_nsec = 0 }) {
            ALOGE("if you see this, expect a segfault. this class AudioTimestamp never was supposed to be instantiated");
        }
        uint32_t mPosition;
        struct timespec mTime;
    };
    class AudioTrack : public virtual RefBase {
    public:
        AudioTrack() {
            ALOGE("if you see this, expect a segfault. this class AudioTrack never was supposed to be instantiated");
        }
        virtual ~AudioTrack() {
            ALOGE("if you see this, expect a segfault. this class AudioTrack never was supposed to be dtor'ed");
        };
        class Buffer
        {
        public:
            Buffer() {
            }
            // [[nodiscard]] size_t size() const { return mSize; }
            // [[nodiscard]] size_t getFrameCount() const { return frameCount; }
            // [[nodiscard]] uint8_t * data() const { return ui8; }
            size_t      frameCount = 0;
            size_t      mSize = 0;
            union {
                void*       raw = nullptr;
                int16_t*    i16;
                uint8_t*    ui8;
            };
            uint32_t    sequence = 0;
        };
        class IAudioTrackCallback : public virtual RefBase {
            friend AudioTrack;
        public:
            IAudioTrackCallback() {
                // TODO do we need to call the aosp ctor here?
            }
            // This event only occurs for TRANSFER_CALLBACK.
            virtual inline size_t
            onMoreData([[maybe_unused]] const AudioTrack::Buffer &buffer) {
                ALOGE("stub IAudioTrackCallback::onMoreData called");
                return 0;
            }

            virtual inline void onUnderrun() {
                ALOGE("stub IAudioTrackCallback::onUnderrun called");
            }

            virtual inline void onLoopEnd([[maybe_unused]] int32_t loopsRemaining) {
                ALOGE("stub IAudioTrackCallback::onLoopEnd called");
            }

            virtual inline void onMarker([[maybe_unused]] uint32_t markerPosition) {
                ALOGE("stub IAudioTrackCallback::onMarker called");
            }

            virtual inline void onNewPos([[maybe_unused]] uint32_t newPos) {
                ALOGE("stub IAudioTrackCallback::onNewPos called");
            }

            virtual inline void onBufferEnd() {
                ALOGE("stub IAudioTrackCallback::onBufferEnd called");
            }

            virtual inline void onNewIAudioTrack() {
                ALOGE("stub IAudioTrackCallback::onNewIAudioTrack called");
            }

            virtual inline void onStreamEnd() {
                ALOGE("stub IAudioTrackCallback::onStreamEnd called");
            }

            virtual inline void onNewTimestamp([[maybe_unused]] AudioTimestamp timestamp) {
                ALOGE("stub IAudioTrackCallback::onNewTimestamp called");
            }

            // This event only occurs for TRANSFER_SYNC_NOTIF_CALLBACK.
            virtual inline size_t onCanWriteMoreData([[maybe_unused]] const AudioTrack::Buffer &buffer) {
                ALOGE("stub IAudioTrackCallback::onCanWriteMoreData called");
                return 0;
            }
        };
    };
    class AudioSystem {
    public:
        class AudioDeviceCallbackV23 : public RefBase
        {
        public:

            AudioDeviceCallbackV23() {}
            virtual ~AudioDeviceCallbackV23() {}

            virtual void onAudioDeviceUpdate(int32_t audioIo,
                                             int32_t deviceId) = 0;
        };
        class AudioDeviceCallbackV33 : public virtual RefBase
        {
        public:

            AudioDeviceCallbackV33() {}
            virtual ~AudioDeviceCallbackV33() {}

            virtual void onAudioDeviceUpdate(int32_t audioIo,
                                             int32_t deviceId) = 0;
        };
        class AudioDeviceCallbackV35Qpr2 : public virtual RefBase
        {
        public:

            AudioDeviceCallbackV35Qpr2() {}
            virtual ~AudioDeviceCallbackV35Qpr2() {}

            virtual void onAudioDeviceUpdate(int32_t audioIo,
                                             const DeviceIdVector& deviceIds) = 0;
        };
    };
}

inline void android::RefBase::onFirstRef()
{
}

inline void android::RefBase::onLastStrongRef(const void*)
{
}

inline bool android::RefBase::onIncStrongAttempted(uint32_t flags, const void*)
{
    return flags & 1;
}

inline void android::RefBase::onLastWeakRef(const void*)
{
}