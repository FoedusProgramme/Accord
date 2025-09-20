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


#ifndef ANDROID_AUDIO_CORE_H
#define ANDROID_AUDIO_CORE_H

/*
 * Stripped down audio.h for audio_port structs of Android 5-7 and 8.
 */

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/cdefs.h>
#include <sys/types.h>

typedef enum {
	LEGACY_AUDIO_STREAM_DEFAULT = -1, // (-1)
	LEGACY_AUDIO_STREAM_MIN = 0,
	LEGACY_AUDIO_STREAM_VOICE_CALL = 0,
	LEGACY_AUDIO_STREAM_SYSTEM = 1,
	LEGACY_AUDIO_STREAM_RING = 2,
	LEGACY_AUDIO_STREAM_MUSIC = 3,
	LEGACY_AUDIO_STREAM_ALARM = 4,
	LEGACY_AUDIO_STREAM_NOTIFICATION = 5,
	LEGACY_AUDIO_STREAM_BLUETOOTH_SCO = 6,
	LEGACY_AUDIO_STREAM_ENFORCED_AUDIBLE = 7,
	LEGACY_AUDIO_STREAM_DTMF = 8,
	LEGACY_AUDIO_STREAM_TTS = 9,
	LEGACY_AUDIO_STREAM_ACCESSIBILITY = 10,
	LEGACY_AUDIO_STREAM_REROUTING = 11,
	LEGACY_AUDIO_STREAM_PATCH = 12,
	LEGACY_AUDIO_STREAM_PUBLIC_CNT = 11, // (ACCESSIBILITY + 1)
	LEGACY_AUDIO_STREAM_FOR_POLICY_CNT = 12, // PATCH
	LEGACY_AUDIO_STREAM_CNT = 13, // (PATCH + 1)
} LEGACY_audio_stream_type_t;

typedef enum {
	LEGACY_AUDIO_SOURCE_DEFAULT = 0,
	LEGACY_AUDIO_SOURCE_MIC = 1,
	LEGACY_AUDIO_SOURCE_VOICE_UPLINK = 2,
	LEGACY_AUDIO_SOURCE_VOICE_DOWNLINK = 3,
	LEGACY_AUDIO_SOURCE_VOICE_CALL = 4,
	LEGACY_AUDIO_SOURCE_CAMCORDER = 5,
	LEGACY_AUDIO_SOURCE_VOICE_RECOGNITION = 6,
	LEGACY_AUDIO_SOURCE_VOICE_COMMUNICATION = 7,
	LEGACY_AUDIO_SOURCE_REMOTE_SUBMIX = 8,
	LEGACY_AUDIO_SOURCE_UNPROCESSED = 9,
	LEGACY_AUDIO_SOURCE_CNT = 10,
	LEGACY_AUDIO_SOURCE_MAX = 9, // (CNT - 1)
	LEGACY_AUDIO_SOURCE_FM_TUNER = 1998,
	LEGACY_AUDIO_SOURCE_HOTWORD = 1999,
} LEGACY_audio_source_t;

typedef enum {
	LEGACY_AUDIO_SESSION_OUTPUT_STAGE = -1, // (-1)
	LEGACY_AUDIO_SESSION_OUTPUT_MIX = 0,
	LEGACY_AUDIO_SESSION_ALLOCATE = 0,
	LEGACY_AUDIO_SESSION_NONE = 0,
} LEGACY_audio_session_t;

typedef enum {
	LEGACY_AUDIO_FORMAT_INVALID = 4294967295u, // 0xFFFFFFFFUL
	LEGACY_AUDIO_FORMAT_DEFAULT = 0u, // 0
	LEGACY_AUDIO_FORMAT_PCM = 0u, // 0x00000000UL
	LEGACY_AUDIO_FORMAT_MP3 = 16777216u, // 0x01000000UL
	LEGACY_AUDIO_FORMAT_AMR_NB = 33554432u, // 0x02000000UL
	LEGACY_AUDIO_FORMAT_AMR_WB = 50331648u, // 0x03000000UL
	LEGACY_AUDIO_FORMAT_AAC = 67108864u, // 0x04000000UL
	LEGACY_AUDIO_FORMAT_HE_AAC_V1 = 83886080u, // 0x05000000UL
	LEGACY_AUDIO_FORMAT_HE_AAC_V2 = 100663296u, // 0x06000000UL
	LEGACY_AUDIO_FORMAT_VORBIS = 117440512u, // 0x07000000UL
	LEGACY_AUDIO_FORMAT_OPUS = 134217728u, // 0x08000000UL
	LEGACY_AUDIO_FORMAT_AC3 = 150994944u, // 0x09000000UL
	LEGACY_AUDIO_FORMAT_E_AC3 = 167772160u, // 0x0A000000UL
	LEGACY_AUDIO_FORMAT_DTS = 184549376u, // 0x0B000000UL
	LEGACY_AUDIO_FORMAT_DTS_HD = 201326592u, // 0x0C000000UL
	LEGACY_AUDIO_FORMAT_IEC61937 = 218103808u, // 0x0D000000UL
	LEGACY_AUDIO_FORMAT_DOLBY_TRUEHD = 234881024u, // 0x0E000000UL
	LEGACY_AUDIO_FORMAT_EVRC = 268435456u, // 0x10000000UL
	LEGACY_AUDIO_FORMAT_EVRCB = 285212672u, // 0x11000000UL
	LEGACY_AUDIO_FORMAT_EVRCWB = 301989888u, // 0x12000000UL
	LEGACY_AUDIO_FORMAT_EVRCNW = 318767104u, // 0x13000000UL
	LEGACY_AUDIO_FORMAT_AAC_ADIF = 335544320u, // 0x14000000UL
	LEGACY_AUDIO_FORMAT_WMA = 352321536u, // 0x15000000UL
	LEGACY_AUDIO_FORMAT_WMA_PRO = 369098752u, // 0x16000000UL
	LEGACY_AUDIO_FORMAT_AMR_WB_PLUS = 385875968u, // 0x17000000UL
	LEGACY_AUDIO_FORMAT_MP2 = 402653184u, // 0x18000000UL
	LEGACY_AUDIO_FORMAT_QCELP = 419430400u, // 0x19000000UL
	LEGACY_AUDIO_FORMAT_DSD = 436207616u, // 0x1A000000UL
	LEGACY_AUDIO_FORMAT_FLAC = 452984832u, // 0x1B000000UL
	LEGACY_AUDIO_FORMAT_ALAC = 469762048u, // 0x1C000000UL
	LEGACY_AUDIO_FORMAT_APE = 486539264u, // 0x1D000000UL
	LEGACY_AUDIO_FORMAT_AAC_ADTS = 503316480u, // 0x1E000000UL
	LEGACY_AUDIO_FORMAT_SBC = 520093696u, // 0x1F000000UL
	LEGACY_AUDIO_FORMAT_APTX = 536870912u, // 0x20000000UL
	LEGACY_AUDIO_FORMAT_APTX_HD = 553648128u, // 0x21000000UL
	LEGACY_AUDIO_FORMAT_AC4 = 570425344u, // 0x22000000UL
	LEGACY_AUDIO_FORMAT_LDAC = 587202560u, // 0x23000000UL
	LEGACY_AUDIO_FORMAT_MAIN_MASK = 4278190080u, // 0xFF000000UL
	LEGACY_AUDIO_FORMAT_SUB_MASK = 16777215u, // 0x00FFFFFFUL
	LEGACY_AUDIO_FORMAT_PCM_SUB_16_BIT = 1u, // 0x1
	LEGACY_AUDIO_FORMAT_PCM_SUB_8_BIT = 2u, // 0x2
	LEGACY_AUDIO_FORMAT_PCM_SUB_32_BIT = 3u, // 0x3
	LEGACY_AUDIO_FORMAT_PCM_SUB_8_24_BIT = 4u, // 0x4
	LEGACY_AUDIO_FORMAT_PCM_SUB_FLOAT = 5u, // 0x5
	LEGACY_AUDIO_FORMAT_PCM_SUB_24_BIT_PACKED = 6u, // 0x6
	LEGACY_AUDIO_FORMAT_MP3_SUB_NONE = 0u, // 0x0
	LEGACY_AUDIO_FORMAT_AMR_SUB_NONE = 0u, // 0x0
	LEGACY_AUDIO_FORMAT_AAC_SUB_MAIN = 1u, // 0x1
	LEGACY_AUDIO_FORMAT_AAC_SUB_LC = 2u, // 0x2
	LEGACY_AUDIO_FORMAT_AAC_SUB_SSR = 4u, // 0x4
	LEGACY_AUDIO_FORMAT_AAC_SUB_LTP = 8u, // 0x8
	LEGACY_AUDIO_FORMAT_AAC_SUB_HE_V1 = 16u, // 0x10
	LEGACY_AUDIO_FORMAT_AAC_SUB_SCALABLE = 32u, // 0x20
	LEGACY_AUDIO_FORMAT_AAC_SUB_ERLC = 64u, // 0x40
	LEGACY_AUDIO_FORMAT_AAC_SUB_LD = 128u, // 0x80
	LEGACY_AUDIO_FORMAT_AAC_SUB_HE_V2 = 256u, // 0x100
	LEGACY_AUDIO_FORMAT_AAC_SUB_ELD = 512u, // 0x200
	LEGACY_AUDIO_FORMAT_VORBIS_SUB_NONE = 0u, // 0x0
	LEGACY_AUDIO_FORMAT_PCM_16_BIT = 1u, // (PCM | PCM_SUB_16_BIT)
	LEGACY_AUDIO_FORMAT_PCM_8_BIT = 2u, // (PCM | PCM_SUB_8_BIT)
	LEGACY_AUDIO_FORMAT_PCM_32_BIT = 3u, // (PCM | PCM_SUB_32_BIT)
	LEGACY_AUDIO_FORMAT_PCM_8_24_BIT = 4u, // (PCM | PCM_SUB_8_24_BIT)
	LEGACY_AUDIO_FORMAT_PCM_FLOAT = 5u, // (PCM | PCM_SUB_FLOAT)
	LEGACY_AUDIO_FORMAT_PCM_24_BIT_PACKED = 6u, // (PCM | PCM_SUB_24_BIT_PACKED)
	LEGACY_AUDIO_FORMAT_AAC_MAIN = 67108865u, // (AAC | AAC_SUB_MAIN)
	LEGACY_AUDIO_FORMAT_AAC_LC = 67108866u, // (AAC | AAC_SUB_LC)
	LEGACY_AUDIO_FORMAT_AAC_SSR = 67108868u, // (AAC | AAC_SUB_SSR)
	LEGACY_AUDIO_FORMAT_AAC_LTP = 67108872u, // (AAC | AAC_SUB_LTP)
	LEGACY_AUDIO_FORMAT_AAC_HE_V1 = 67108880u, // (AAC | AAC_SUB_HE_V1)
	LEGACY_AUDIO_FORMAT_AAC_SCALABLE = 67108896u, // (AAC | AAC_SUB_SCALABLE)
	LEGACY_AUDIO_FORMAT_AAC_ERLC = 67108928u, // (AAC | AAC_SUB_ERLC)
	LEGACY_AUDIO_FORMAT_AAC_LD = 67108992u, // (AAC | AAC_SUB_LD)
	LEGACY_AUDIO_FORMAT_AAC_HE_V2 = 67109120u, // (AAC | AAC_SUB_HE_V2)
	LEGACY_AUDIO_FORMAT_AAC_ELD = 67109376u, // (AAC | AAC_SUB_ELD)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_MAIN = 503316481u, // (AAC_ADTS | AAC_SUB_MAIN)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_LC = 503316482u, // (AAC_ADTS | AAC_SUB_LC)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_SSR = 503316484u, // (AAC_ADTS | AAC_SUB_SSR)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_LTP = 503316488u, // (AAC_ADTS | AAC_SUB_LTP)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_HE_V1 = 503316496u, // (AAC_ADTS | AAC_SUB_HE_V1)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_SCALABLE = 503316512u, // (AAC_ADTS | AAC_SUB_SCALABLE)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_ERLC = 503316544u, // (AAC_ADTS | AAC_SUB_ERLC)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_LD = 503316608u, // (AAC_ADTS | AAC_SUB_LD)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_HE_V2 = 503316736u, // (AAC_ADTS | AAC_SUB_HE_V2)
	LEGACY_AUDIO_FORMAT_AAC_ADTS_ELD = 503316992u, // (AAC_ADTS | AAC_SUB_ELD)
} LEGACY_audio_format_t;

typedef enum {
	LEGACY_AUDIO_MODE_INVALID = -2, // (-2)
	LEGACY_AUDIO_MODE_CURRENT = -1, // (-1)
	LEGACY_AUDIO_MODE_NORMAL = 0,
	LEGACY_AUDIO_MODE_RINGTONE = 1,
	LEGACY_AUDIO_MODE_IN_CALL = 2,
	LEGACY_AUDIO_MODE_IN_COMMUNICATION = 3,
	LEGACY_AUDIO_MODE_CNT = 4,
	LEGACY_AUDIO_MODE_MAX = 3, // (CNT - 1)
} LEGACY_audio_mode_t;

typedef enum {
	LEGACY_AUDIO_USAGE_UNKNOWN = 0,
	LEGACY_AUDIO_USAGE_MEDIA = 1,
	LEGACY_AUDIO_USAGE_VOICE_COMMUNICATION = 2,
	LEGACY_AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING = 3,
	LEGACY_AUDIO_USAGE_ALARM = 4,
	LEGACY_AUDIO_USAGE_NOTIFICATION = 5,
	LEGACY_AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE = 6,
	LEGACY_AUDIO_USAGE_NOTIFICATION_COMMUNICATION_REQUEST = 7,
	LEGACY_AUDIO_USAGE_NOTIFICATION_COMMUNICATION_INSTANT = 8,
	LEGACY_AUDIO_USAGE_NOTIFICATION_COMMUNICATION_DELAYED = 9,
	LEGACY_AUDIO_USAGE_NOTIFICATION_EVENT = 10,
	LEGACY_AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY = 11,
	LEGACY_AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12,
	LEGACY_AUDIO_USAGE_ASSISTANCE_SONIFICATION = 13,
	LEGACY_AUDIO_USAGE_GAME = 14,
	LEGACY_AUDIO_USAGE_VIRTUAL_SOURCE = 15,
	LEGACY_AUDIO_USAGE_ASSISTANT = 16,
	LEGACY_AUDIO_USAGE_CNT = 17,
	LEGACY_AUDIO_USAGE_MAX = 16, // (CNT - 1)
} LEGACY_audio_usage_t;

typedef enum {
	LEGACY_AUDIO_PORT_ROLE_NONE = 0,
	LEGACY_AUDIO_PORT_ROLE_SOURCE = 1,
	LEGACY_AUDIO_PORT_ROLE_SINK = 2,
} LEGACY_audio_port_role_t;

typedef enum {
	LEGACY_AUDIO_PORT_TYPE_NONE = 0,
	LEGACY_AUDIO_PORT_TYPE_DEVICE = 1,
	LEGACY_AUDIO_PORT_TYPE_MIX = 2,
	LEGACY_AUDIO_PORT_TYPE_SESSION = 3,
} LEGACY_audio_port_type_t;

typedef enum {
	LEGACY_AUDIO_LATENCY_LOW = 0,
	LEGACY_AUDIO_LATENCY_NORMAL = 1,
} LEGACY_audio_mix_latency_class_t;

#define audio_port_config { \
	int id; \
	LEGACY_audio_port_role_t role; \
	LEGACY_audio_port_type_t type; \
	unsigned int config_mask; \
	unsigned int sample_rate; \
	uint32_t channel_mask; \
	LEGACY_audio_format_t format; \
	struct { \
		int index; \
		uint32_t mode; \
		uint32_t channel_mask; \
		int values[sizeof(uint32_t) * 8]; \
		unsigned int ramp_duration_ms; \
	} gain; \
	union { \
		struct { \
			int hw_module; \
			uint32_t type; \
			char address[32]; \
		} device; \
		struct { \
			int hw_module; \
			int handle; \
			union { \
				LEGACY_audio_stream_type_t stream; \
				LEGACY_audio_source_t      source; \
			} usecase; \
		} mix; \
		struct { \
			LEGACY_audio_session_t session; \
		} session; \
	} ext; \
}

#define audio_gain { \
	uint32_t mode; \
	uint32_t channel_mask; \
	int min_value; \
	int max_value; \
	int default_value; \
	unsigned int step_value; \
	unsigned int min_ramp_ms; \
	unsigned int max_ramp_ms; \
}
#define audio_port_device_ext { \
	int hw_module; \
	uint32_t type; \
	char address[32]; \
}
#define audio_port_mix_ext { \
	int hw_module; \
	int handle; \
	LEGACY_audio_mix_latency_class_t latency_class; \
}
#define audio_port_session_ext { \
	LEGACY_audio_session_t session; \
}

struct audio_port_legacy {
	int id;
	LEGACY_audio_port_role_t role;
	LEGACY_audio_port_type_t type;
	char name[128];
	unsigned int num_sample_rates;
	unsigned int sample_rates[16];
	unsigned int num_channel_masks;
	uint32_t channel_masks[16];
	unsigned int num_formats;
	LEGACY_audio_format_t formats[16];
	unsigned int num_gains;
	struct audio_gain gains[16];
	struct audio_port_config active_config;
	union {
		struct audio_port_device_ext device;
		struct audio_port_mix_ext mix;
		struct audio_port_session_ext session;
	} ext;
};

struct audio_port_oreo {
	int id;
	LEGACY_audio_port_role_t role;
	LEGACY_audio_port_type_t type;
	char name[128];
	unsigned int num_sample_rates;
	unsigned int sample_rates[32];
	unsigned int num_channel_masks;
	uint32_t channel_masks[32];
	unsigned int num_formats;
	LEGACY_audio_format_t formats[32];
	unsigned int num_gains;
	struct audio_gain gains[16];
	struct audio_port_config active_config;
	union {
		struct audio_port_device_ext device;
		struct audio_port_mix_ext mix;
		struct audio_port_session_ext session;
	} ext;
};
#undef audio_port_config
#undef audio_gain
#undef audio_port_device_ext
#undef audio_port_mix_ext
#undef audio_port_session_ext

#ifdef __cplusplus
}
#endif

#endif  // ANDROID_AUDIO_CORE_H