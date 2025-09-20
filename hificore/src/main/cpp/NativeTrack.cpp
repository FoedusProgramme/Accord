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

#define LOG_TAG "NativeTrack"

#include <jni.h>
#include <dlfcn.h>
#include <cstring>
#include <android/log_macros.h>
#include <bits/timespec.h>
#include <unistd.h>
#include <cstdlib>
#include <vector>
#include <map>
#include <pthread.h>
#include "helpers.h"
#include "audio-legacy.h"

extern void *libaudioclient_handle;
extern void *libpermission_handle;
extern void *libandroid_runtime_handle;
extern void *libutils_handle;
extern void *libavenhancements_handle;
extern void *libbinder_handle;
extern bool initLib(JNIEnv *env);

using DeviceIdVector = std::vector<int>;
typedef void*(*ZN7android7RefBaseC2Ev_t)(void* thisptr);
static ZN7android7RefBaseC2Ev_t ZN7android7RefBaseC2Ev = nullptr;
typedef void*(*ZN7android7RefBaseD2Ev_t)(void* thisptr);
static ZN7android7RefBaseD2Ev_t ZN7android7RefBaseD2Ev = nullptr;
typedef void(*ZNK7android7RefBase9incStrongEPKv_t)(void* thisptr, void* id);
static ZNK7android7RefBase9incStrongEPKv_t ZNK7android7RefBase9incStrongEPKv = nullptr;
typedef void(*ZNK7android7RefBase9decStrongEPKv_t)(void* thisptr, void* id);
static ZNK7android7RefBase9decStrongEPKv_t ZNK7android7RefBase9decStrongEPKv = nullptr;
typedef void*(*ZNK7android7RefBase10createWeakEPKv_t)(void* thisptr, void* id);
static ZNK7android7RefBase10createWeakEPKv_t ZNK7android7RefBase10createWeakEPKv = nullptr;
#include "aosp_stubs.h"
typedef void*(*ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject_t)(_JNIEnv*, _jobject*);
static ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject_t ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject = nullptr;
typedef int32_t(*ZN7android7content22AttributionSourceState14readFromParcelEPKNS_6ParcelE_t)(void* thisptr, void* parcel);
static ZN7android7content22AttributionSourceState14readFromParcelEPKNS_6ParcelE_t ZN7android7content22AttributionSourceState14readFromParcelEPKNS_6ParcelE = nullptr;
typedef void*(*ZN7android10AudioTrackC1ERKNS_7content22AttributionSourceStateE_t)(void* thisptr, void* attributionSourceState);
static ZN7android10AudioTrackC1ERKNS_7content22AttributionSourceStateE_t ZN7android10AudioTrackC1ERKNS_7content22AttributionSourceStateE = nullptr;
typedef void*(*ZN7android10AudioTrackC1Ev_t)(void* thisptr);
static ZN7android10AudioTrackC1Ev_t ZN7android10AudioTrackC1Ev = nullptr;
typedef void(*ZN7android7RefBase12weakref_type7decWeakEPKv_t)(void* thisptr, void* id);
static ZN7android7RefBase12weakref_type7decWeakEPKv_t ZN7android7RefBase12weakref_type7decWeakEPKv = nullptr;
typedef int32_t(*ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_t_t)
        (void* thisptr, int32_t streamType, uint32_t sampleRate, uint32_t format, uint32_t channelMask, size_t frameCount /* = 0 */, uint32_t flags /* = 0 */, legacy_callback_t callback /* = nullptr */, void* user /* = nullptr */, int32_t notificationFrames /* = 0 */, fake_sp& sharedMemory /* = nullptr */, bool threadCanCallJava /* = false */, int32_t audioSessionId /* = 0 */, int32_t transferType /* = TRANSFER_DEFAULT */, void* offloadInfo /* = nullptr */, int uid, pid_t pid, void* attributes /* = nullptr */);
static ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_t_t ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_t = nullptr;
typedef int32_t(*ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_t20audio_channel_mask_tm20audio_output_flags_tRKNS_2wpINS0_19IAudioTrackCallbackEEEiRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tRKNS_7content22AttributionSourceStateEPK18audio_attributes_tbfi_t)
        (void* thisptr, int32_t streamType, uint32_t sampleRate, uint32_t format, uint32_t channelMask, size_t frameCount /* = 0 */, uint32_t flags /* = 0 */, fake_wp& callback /* = nullptr */, int32_t notificationFrames /* = 0 */, fake_sp& sharedMemory /* = nullptr */, bool threadCanCallJava /* = false */, int32_t audioSessionId /* = 0 */, int32_t transferType /* = TRANSFER_DEFAULT */, void* offloadInfo /* = nullptr */, int& attributionSource, void* attributes /* = nullptr */, bool doNotReconnect /* = false */, float maxRequiredSpeed /* = 1.0f */, int selectedDeviceId /* = 0 */);
static ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_t20audio_channel_mask_tm20audio_output_flags_tRKNS_2wpINS0_19IAudioTrackCallbackEEEiRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tRKNS_7content22AttributionSourceStateEPK18audio_attributes_tbfi_t ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_t20audio_channel_mask_tm20audio_output_flags_tRKNS_2wpINS0_19IAudioTrackCallbackEEEiRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tRKNS_7content22AttributionSourceStateEPK18audio_attributes_tbfi = nullptr;
typedef int32_t(*ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbfi_t)
        (void* thisptr, int32_t streamType, uint32_t sampleRate, uint32_t format, uint32_t channelMask, size_t frameCount /* = 0 */, uint32_t flags /* = 0 */, legacy_callback_t callback /* = nullptr */, void* user /* = nullptr */, int32_t notificationFrames /* = 0 */, fake_sp& sharedMemory /* = nullptr */, bool threadCanCallJava /* = false */, int32_t audioSessionId /* = 0 */, int32_t transferType /* = TRANSFER_DEFAULT */, void* offloadInfo /* = nullptr */, uid_t uid, pid_t pid, void* attributes /* = nullptr */, bool doNotReconnect /* = false */, float maxRequiredSpeed /* = 1.0f */, int selectedDeviceId /* = 0 */);
static ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbfi_t ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbfi = nullptr;
typedef int32_t(*ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbf_t)
        (void* thisptr, int32_t streamType, uint32_t sampleRate, uint32_t format, uint32_t channelMask, size_t frameCount /* = 0 */, uint32_t flags /* = 0 */, legacy_callback_t callback /* = nullptr */, void* user /* = nullptr */, int32_t notificationFrames /* = 0 */, fake_sp& sharedMemory /* = nullptr */, bool threadCanCallJava /* = false */, int32_t audioSessionId /* = 0 */, int32_t transferType /* = TRANSFER_DEFAULT */, void* offloadInfo /* = nullptr */, uid_t uid, pid_t pid, void* attributes /* = nullptr */, bool doNotReconnect /* = false */, float maxRequiredSpeed /* = 1.0f */);
static ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbf_t ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbf = nullptr;
typedef int32_t(*ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tbf_t)
        (void* thisptr, int32_t streamType, uint32_t sampleRate, uint32_t format, uint32_t channelMask, size_t frameCount /* = 0 */, uint32_t flags /* = 0 */, legacy_callback_t callback /* = nullptr */, void* user /* = nullptr */, int32_t notificationFrames /* = 0 */, fake_sp& sharedMemory /* = nullptr */, bool threadCanCallJava /* = false */, int32_t audioSessionId /* = 0 */, int32_t transferType /* = TRANSFER_DEFAULT */, void* offloadInfo /* = nullptr */, int uid, pid_t pid, void* attributes /* = nullptr */, bool doNotReconnect /* = false */, float maxRequiredSpeed /* = 1.0f */);
static ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tbf_t ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tbf = nullptr;
typedef int32_t(*ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tb_t)
        (void* thisptr, int32_t streamType, uint32_t sampleRate, uint32_t format, uint32_t channelMask, size_t frameCount /* = 0 */, uint32_t flags /* = 0 */, legacy_callback_t callback /* = nullptr */, void* user /* = nullptr */, int32_t notificationFrames /* = 0 */, fake_sp& sharedMemory /* = nullptr */, bool threadCanCallJava /* = false */, int32_t audioSessionId /* = 0 */, int32_t transferType /* = TRANSFER_DEFAULT */, void* offloadInfo /* = nullptr */, uid_t uid, pid_t pid, void* attributes /* = nullptr */, bool doNotReconnect /* = false */);
static ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tb_t ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tb = nullptr;
typedef int32_t(*ZN7android11AudioSystem16getOutputForAttrEPK18audio_attributes_tPi15audio_session_tP19audio_stream_type_tjj14audio_format_tj20audio_output_flags_tiPK20audio_offload_info_t_t)(void *attr, int32_t *output, int32_t session, int32_t *stream, uid_t uid, uint32_t samplingRate, int32_t format, int32_t channelMask, int32_t flags, int32_t selectedDeviceId, void *offloadInfo);
static ZN7android11AudioSystem16getOutputForAttrEPK18audio_attributes_tPi15audio_session_tP19audio_stream_type_tjj14audio_format_tj20audio_output_flags_tiPK20audio_offload_info_t_t ZN7android11AudioSystem16getOutputForAttrEPK18audio_attributes_tPi15audio_session_tP19audio_stream_type_tjj14audio_format_tj20audio_output_flags_tiPK20audio_offload_info_t = nullptr;
typedef int32_t(*ZN7android11AudioSystem10getLatencyEiPj_t)(int32_t output, uint32_t* latency);
static ZN7android11AudioSystem10getLatencyEiPj_t ZN7android11AudioSystem10getLatencyEiPj = nullptr;
typedef int32_t(*ZN7android11AudioSystem13getFrameCountEiPm_t)(int32_t output, size_t* frameCount);
static ZN7android11AudioSystem13getFrameCountEiPm_t ZN7android11AudioSystem13getFrameCountEiPm = nullptr;
typedef int32_t(*ZN7android11AudioSystem15getSamplingRateEiPj_t)(int32_t output, uint32_t* sampleRate);
static ZN7android11AudioSystem15getSamplingRateEiPj_t ZN7android11AudioSystem15getSamplingRateEiPj = nullptr;
typedef void(*ZN7android11AudioSystem13releaseOutputEi19audio_stream_type_t15audio_session_t_t)(uint32_t output, int32_t stream, int32_t session);
static ZN7android11AudioSystem13releaseOutputEi19audio_stream_type_t15audio_session_t_t ZN7android11AudioSystem13releaseOutputEi19audio_stream_type_t15audio_session_t = nullptr;
typedef bool(*ZNK7android10AudioTrack19isOffloadedOrDirectEv_t)(void* thisptr);
static ZNK7android10AudioTrack19isOffloadedOrDirectEv_t ZNK7android10AudioTrack19isOffloadedOrDirectEv = nullptr;
typedef int32_t(*ZN7android10AudioTrack15setOutputDeviceEi_t)(void* thisptr, int32_t selectedDeviceId);
static ZN7android10AudioTrack15setOutputDeviceEi_t ZN7android10AudioTrack15setOutputDeviceEi = nullptr;
typedef int32_t(*ZN7android10AudioTrack15getOutputDeviceEv_t)(void* thisptr);
static ZN7android10AudioTrack15getOutputDeviceEv_t ZN7android10AudioTrack15getOutputDeviceEv = nullptr;
typedef bool(*ZN7android11AudioSystem18isOffloadSupportedERK20audio_offload_info_t_t)(audio_offload_info_t_v26 & offloadInfo);
static ZN7android11AudioSystem18isOffloadSupportedERK20audio_offload_info_t_t ZN7android11AudioSystem18isOffloadSupportedERK20audio_offload_info_t = nullptr;
typedef int32_t(*ZN7android11AudioSystem17getOffloadSupportERK20audio_offload_info_t_t)(audio_offload_info_t_v30 & offloadInfo);
static ZN7android11AudioSystem17getOffloadSupportERK20audio_offload_info_t_t ZN7android11AudioSystem17getOffloadSupportERK20audio_offload_info_t = nullptr;
typedef int32_t(*ZN7android10AudioTrack22addAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE_t)(void* thisptr, fake_sp& cb);
static ZN7android10AudioTrack22addAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE_t ZN7android10AudioTrack22addAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE = nullptr;
typedef int32_t(*ZN7android10AudioTrack25removeAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE_t)(void* thisptr, fake_sp& cb);
static ZN7android10AudioTrack25removeAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE_t ZN7android10AudioTrack25removeAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE = nullptr;
typedef DeviceIdVector(*ZN7android10AudioTrack18getRoutedDeviceIdsEv_t)(void* thisptr);
static ZN7android10AudioTrack18getRoutedDeviceIdsEv_t ZN7android10AudioTrack18getRoutedDeviceIdsEv = nullptr;
typedef int32_t(*ZN7android10AudioTrack17getRoutedDeviceIdEv_t)(void* thisptr);
static ZN7android10AudioTrack17getRoutedDeviceIdEv_t ZN7android10AudioTrack17getRoutedDeviceIdEv = nullptr;
typedef uint32_t(*ZNK7android10AudioTrack9getOutputEv_t)(void* thisptr);
static ZNK7android10AudioTrack9getOutputEv_t ZNK7android10AudioTrack9getOutputEv = nullptr;
typedef int32_t(*ZN7android10AudioTrack5startEv_t)(void* thisptr);
static ZN7android10AudioTrack5startEv_t ZN7android10AudioTrack5startEv = nullptr;
typedef void(*ZN7android10AudioTrack4stopEv_t)(void* thisptr);
static ZN7android10AudioTrack4stopEv_t ZN7android10AudioTrack4stopEv = nullptr;
typedef bool(*ZNK7android10AudioTrack7stoppedEv_t)(void* thisptr);
static ZNK7android10AudioTrack7stoppedEv_t ZNK7android10AudioTrack7stoppedEv = nullptr;
typedef void(*ZN7android10AudioTrack5flushEv_t)(void* thisptr);
static ZN7android10AudioTrack5flushEv_t ZN7android10AudioTrack5flushEv = nullptr;
typedef void(*ZN7android10AudioTrack5pauseEv_t)(void* thisptr);
static ZN7android10AudioTrack5pauseEv_t ZN7android10AudioTrack5pauseEv = nullptr;
typedef bool(*ZN7android10AudioTrack12pauseAndWaitERKNSt3__16chrono8durationIxNS1_5ratioILl1ELl1000EEEEE_t)(void* thisptr, const std::chrono::milliseconds& timeout);
static ZN7android10AudioTrack12pauseAndWaitERKNSt3__16chrono8durationIxNS1_5ratioILl1ELl1000EEEEE_t ZN7android10AudioTrack12pauseAndWaitERKNSt3__16chrono8durationIxNS1_5ratioILl1ELl1000EEEEE = nullptr;
typedef int32_t(*ZN7android10AudioTrack9setVolumeEf_t)(void* thisptr, float volume);
static ZN7android10AudioTrack9setVolumeEf_t ZN7android10AudioTrack9setVolumeEf = nullptr;
typedef int32_t(*ZN7android10AudioTrack21setAuxEffectSendLevelEf_t)(void* thisptr, float level);
static ZN7android10AudioTrack21setAuxEffectSendLevelEf_t ZN7android10AudioTrack21setAuxEffectSendLevelEf = nullptr;
typedef float(*ZNK7android10AudioTrack21getAuxEffectSendLevelEPf_t)(void* thisptr);
static ZNK7android10AudioTrack21getAuxEffectSendLevelEPf_t ZNK7android10AudioTrack21getAuxEffectSendLevelEPf = nullptr;
typedef int32_t(*ZN7android10AudioTrack13setSampleRateEj_t)(void* thisptr, int32_t rate);
static ZN7android10AudioTrack13setSampleRateEj_t ZN7android10AudioTrack13setSampleRateEj = nullptr;
typedef int32_t(*ZNK7android10AudioTrack13getSampleRateEv_t)(void* thisptr);
static ZNK7android10AudioTrack13getSampleRateEv_t ZNK7android10AudioTrack13getSampleRateEv = nullptr;
typedef int32_t(*ZNK7android10AudioTrack21getOriginalSampleRateEv_t)(void* thisptr);
static ZNK7android10AudioTrack21getOriginalSampleRateEv_t ZNK7android10AudioTrack21getOriginalSampleRateEv = nullptr;
typedef bool(*ZN7android10AudioTrack10hasStartedEv_t)(void* thisptr);
static ZN7android10AudioTrack10hasStartedEv_t ZN7android10AudioTrack10hasStartedEv = nullptr;
typedef int32_t(*ZN7android10AudioTrack7setLoopEjji_t)(void* thisptr, uint32_t start, uint32_t end, int32_t count);
static ZN7android10AudioTrack7setLoopEjji_t ZN7android10AudioTrack7setLoopEjji = nullptr;
typedef int32_t(*ZN7android10AudioTrack17setMarkerPositionEj_t)(void* thisptr, uint32_t pos);
static ZN7android10AudioTrack17setMarkerPositionEj_t ZN7android10AudioTrack17setMarkerPositionEj = nullptr;
typedef int32_t(*ZNK7android10AudioTrack17getMarkerPositionEPj_t)(void* thisptr, uint32_t* pos);
static ZNK7android10AudioTrack17getMarkerPositionEPj_t ZNK7android10AudioTrack17getMarkerPositionEPj = nullptr;
typedef int32_t(*ZN7android10AudioTrack23setPositionUpdatePeriodEj_t)(void* thisptr, uint32_t pos);
static ZN7android10AudioTrack23setPositionUpdatePeriodEj_t ZN7android10AudioTrack23setPositionUpdatePeriodEj = nullptr;
typedef int32_t(*ZNK7android10AudioTrack23getPositionUpdatePeriodEPj_t)(void* thisptr, uint32_t* pos);
static ZNK7android10AudioTrack23getPositionUpdatePeriodEPj_t ZNK7android10AudioTrack23getPositionUpdatePeriodEPj = nullptr;
typedef int32_t(*ZN7android10AudioTrack11setPositionEj_t)(void* thisptr, uint32_t pos);
static ZN7android10AudioTrack11setPositionEj_t ZN7android10AudioTrack11setPositionEj = nullptr;
typedef int32_t(*ZN7android10AudioTrack11getPositionEPj_t)(void* thisptr, uint32_t* pos);
static ZN7android10AudioTrack11getPositionEPj_t ZN7android10AudioTrack11getPositionEPj = nullptr;
typedef int32_t(*ZN7android10AudioTrack17getBufferPositionEPj_t)(void* thisptr, uint32_t* pos);
static ZN7android10AudioTrack17getBufferPositionEPj_t ZN7android10AudioTrack17getBufferPositionEPj = nullptr;
typedef int32_t(*ZN7android10AudioTrack21getBufferDurationInUsEPl_t)(void* thisptr, int64_t* pos);
static ZN7android10AudioTrack21getBufferDurationInUsEPl_t ZN7android10AudioTrack21getBufferDurationInUsEPl = nullptr;
typedef audio_playback_rate(*ZNK7android10AudioTrack15getPlaybackRateEv_t)(void* thisptr);
static ZNK7android10AudioTrack15getPlaybackRateEv_t ZNK7android10AudioTrack15getPlaybackRateEv = nullptr;
typedef int32_t(*ZN7android10AudioTrack15setPlaybackRateERKNS_17AudioPlaybackRateE_t)(void* thisptr, audio_playback_rate& rate);
static ZN7android10AudioTrack15setPlaybackRateERKNS_17AudioPlaybackRateE_t ZN7android10AudioTrack15setPlaybackRateERKNS_17AudioPlaybackRateE = nullptr;
typedef int32_t(*ZN7android10AudioTrack15pendingDurationEPiNS_17ExtendedTimestamp8LocationE_t)(void* thisptr, int32_t* out, int location);
static ZN7android10AudioTrack15pendingDurationEPiNS_17ExtendedTimestamp8LocationE_t ZN7android10AudioTrack15pendingDurationEPiNS_17ExtendedTimestamp8LocationE = nullptr;
typedef int32_t(*ZN7android10AudioTrack12getTimestampEPNS_17ExtendedTimestampE_t)(void* thisptr, ExtendedTimestamp* ts);
static ZN7android10AudioTrack12getTimestampEPNS_17ExtendedTimestampE_t ZN7android10AudioTrack12getTimestampEPNS_17ExtendedTimestampE = nullptr;
typedef int32_t(*ZN7android10AudioTrack16getMinFrameCountEPm19audio_stream_type_tj_t)(int32_t streamType, int32_t sampleRateHz);
static ZN7android10AudioTrack16getMinFrameCountEPm19audio_stream_type_tj_t ZN7android10AudioTrack16getMinFrameCountEPm19audio_stream_type_tj = nullptr;
typedef int32_t(*ZN7android10AudioTrack6reloadEv_t)(void* thisptr);
static ZN7android10AudioTrack6reloadEv_t ZN7android10AudioTrack6reloadEv = nullptr;
typedef int32_t(*ZN7android10AudioTrack15attachAuxEffectEi_t)(void* thisptr, int32_t effectId);
static ZN7android10AudioTrack15attachAuxEffectEi_t ZN7android10AudioTrack15attachAuxEffectEi = nullptr;
typedef uint32_t(*ZNK7android10AudioTrack17getUnderrunFramesEv_t)(void* thisptr);
static ZNK7android10AudioTrack17getUnderrunFramesEv_t ZNK7android10AudioTrack17getUnderrunFramesEv = nullptr;
typedef void*(*ZN7android7String8C1EPKc_t)(void* thisptr, const char* str);
static ZN7android7String8C1EPKc_t ZN7android7String8C1EPKc = nullptr;
typedef void(*ZN7android7String8D1Ev_t)(void* thisptr);
static ZN7android7String8D1Ev_t ZN7android7String8D1Ev = nullptr;
typedef int32_t(*ZN7android10AudioTrack13setParametersERKNS_7String8E_t)(void* thisptr, String8& s8);
static ZN7android10AudioTrack13setParametersERKNS_7String8E_t ZN7android10AudioTrack13setParametersERKNS_7String8E = nullptr;
typedef String8(*ZN7android10AudioTrack13getParametersERKNS_7String8E_t)(void* thisptr, String8& s8);
static ZN7android10AudioTrack13getParametersERKNS_7String8E_t ZN7android10AudioTrack13getParametersERKNS_7String8E = nullptr;
typedef int32_t(*ZN7android10AudioTrack12getTimestampERNS_14AudioTimestampE_t)(void* thisptr, android::AudioTimestamp& timestamp);
static ZN7android10AudioTrack12getTimestampERNS_14AudioTimestampE_t ZN7android10AudioTrack12getTimestampERNS_14AudioTimestampE = nullptr;
typedef ssize_t(*ZN7android10AudioTrack5writeEPKvjb_t)(void* thisptr, void* buf, uint32_t len, bool blocking);
static ZN7android10AudioTrack5writeEPKvjb_t ZN7android10AudioTrack5writeEPKvjb = nullptr;
typedef int32_t(*ZN7android10AudioTrack12obtainBufferEPNS0_6BufferEiPj_t)(void* thisptr, android::AudioTrack::Buffer* buf, int32_t waitCount, size_t* nonContig);
static ZN7android10AudioTrack12obtainBufferEPNS0_6BufferEiPj_t ZN7android10AudioTrack12obtainBufferEPNS0_6BufferEiPj = nullptr;
typedef void(*ZN7android10AudioTrack13releaseBufferEPKNS0_6BufferE_t)(void* thisptr, android::AudioTrack::Buffer* buf);
static ZN7android10AudioTrack13releaseBufferEPKNS0_6BufferE_t ZN7android10AudioTrack13releaseBufferEPKNS0_6BufferE = nullptr;

class MyCallback;
struct track_holder {
    explicit track_holder(JNIEnv* env) {
        int err = env->GetJavaVM(&vm);
        if (err != JNI_OK) {
            ALOGE("could not get JavaVM: %d. aborting!", err);
            abort();
        }
    }
    void* track = nullptr;
    MyCallback* callback = nullptr;
    void* deviceCallback = nullptr;
    jobject thiz = nullptr;
    jmethodID onAudioDeviceUpdate = nullptr;
    jobject sharedMemoryBuffer = nullptr;
    void* ats = nullptr;
    bool deathEmulation = false;
    bool died = false;
    JavaVM* vm = nullptr;
    std::map<void*, uint32_t> sequences = {};
};
static void myJniDetach(void* arg) {
    int ret = ((JavaVM*)arg)->DetachCurrentThread();
    if (ret != JNI_OK) {
        ALOGE("failed to detach thread: %d", ret);
    }
}
class MyCallback : public virtual android::AudioTrack::IAudioTrackCallback {
    track_holder* mHolder;
    jobject mCallback;
    jmethodID mOnUnderrun;
    jmethodID mOnMarker;
    jmethodID mOnNewPos;
    jmethodID mOnNewIAudioTrack;
    jmethodID mOnStreamEnd;
    jmethodID mOnNewTimestamp;
    jmethodID mOnLoopEnd;
    jmethodID mOnBufferEnd;
    jmethodID mOnMoreData;
    jmethodID mOnCanWriteMoreData;
public:
    MyCallback(track_holder& holder, JNIEnv* env, jobject jcallback) : RefBase(), mHolder(&holder) {
        static int idCounter = 0;
        mId = idCounter++;
        mCallback = env->NewGlobalRef(jcallback);
        jclass callbackClass = env->GetObjectClass(mCallback);
#define TRY_GET_JNI(FIELD, METHOD, SIGNATURE) \
        FIELD = env->GetMethodID(callbackClass, METHOD, SIGNATURE); \
        if (FIELD == nullptr) { \
            ALOGI("callback does not have matching " METHOD SIGNATURE " method, " \
                "assuming it does not care"); \
            env->ExceptionClear(); \
        }
        TRY_GET_JNI(mOnUnderrun, "onUnderrun", "()V")
        TRY_GET_JNI(mOnMarker, "onMarker", "(I)V")
        TRY_GET_JNI(mOnNewPos, "onNewPos", "(I)V")
        TRY_GET_JNI(mOnStreamEnd, "onStreamEnd", "()V")
        TRY_GET_JNI(mOnNewIAudioTrack, "onNewIAudioTrack", "()V")
        TRY_GET_JNI(mOnNewTimestamp, "onNewTimestamp", "(IJ)V")
        TRY_GET_JNI(mOnLoopEnd, "onLoopEnd", "(I)V")
        TRY_GET_JNI(mOnBufferEnd, "onBufferEnd", "()V")
        TRY_GET_JNI(mOnMoreData, "onMoreData", "(JLjava/nio/ByteBuffer;)J")
        TRY_GET_JNI(mOnCanWriteMoreData, "onCanWriteMoreData", "(JJ)V")
#undef TRY_GET_JNI
        env->DeleteLocalRef(callbackClass);
    };
    void onUnderrun() override {
        if (!mCallback || mHolder->died || !mOnUnderrun || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnUnderrun);
    }
    void onMarker(uint32_t markerPosition) override {
        if (!mCallback || mHolder->died || !mOnMarker || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnMarker, (jint) markerPosition);
    }
    void onNewPos(uint32_t newPos) override {
        if (!mCallback || mHolder->died || !mOnNewPos || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnNewPos, (jint) newPos);
    }
    // quirk: some ancient (before O) MTK versions don't call this unless track is offload
    void onNewIAudioTrack() override {
        if (!mCallback || mHolder->died) return;
        if (mHolder->deathEmulation) {
            // implies android_get_device_api_level() < 23
            // block any further callbacks, and access to track object other than dtor
            mHolder->died = true;
            ZN7android10AudioTrack5pauseEv(mHolder->track);
            ZN7android10AudioTrack4stopEv(mHolder->track);
        }
        if (mOnNewIAudioTrack && maybeAttachThread(__func__)) {
            mEnv->CallVoidMethod(mCallback, mOnNewIAudioTrack);
        }
    }
    void onStreamEnd() override {
        if (!mCallback || mHolder->died || !mOnStreamEnd || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnStreamEnd);
    }
    void onNewTimestamp(android::AudioTimestamp timestamp) override {
        if (!mCallback || mHolder->died || !mOnNewTimestamp || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnNewTimestamp,
                                     (jint) timestamp.mPosition,
                                     (jlong)((timestamp.mTime.tv_sec * 1000000000LL) + timestamp.mTime.tv_nsec));
    }
    void onLoopEnd(int32_t loopsRemaining) override {
        if (!mCallback || mHolder->died || !mOnLoopEnd || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnLoopEnd, (jint) loopsRemaining);
    }
    void onBufferEnd() override {
        if (!mCallback || mHolder->died || !mOnBufferEnd || !maybeAttachThread(__func__)) return;
        mEnv->CallVoidMethod(mCallback, mOnBufferEnd);
    }
    size_t onMoreData(const android::AudioTrack::Buffer &buffer) override {
        if (!mCallback || mHolder->died || !mOnMoreData || !maybeAttachThread(__func__)) return 0;
        jobject buf = mEnv->NewDirectByteBuffer(buffer.raw, (jlong) (uint64_t) buffer.mSize);
        auto ret = (size_t) mEnv->CallLongMethod(mCallback, mOnMoreData, buf,
                                                     (jlong) (uint64_t) buffer.frameCount);
        mEnv->DeleteLocalRef(buf);
        return ret;
    }
    size_t onCanWriteMoreData(const android::AudioTrack::Buffer &buffer) override {
        if (!mCallback || mHolder->died || !mOnCanWriteMoreData || !maybeAttachThread(__func__)) return 0;
        // this method is a bit of a misnomer, we're supposed to never write in the buffer and
        // always return 0. only the available write capacity is of interest.
        uint64_t size = buffer.mSize;
        uint64_t frames = buffer.frameCount;
        mEnv->CallVoidMethod(mCallback, mOnCanWriteMoreData, frames, size);
        return 0;
    }
    void onLastStrongRef(const void *id) override {
        mAttached = false; // we're probably _not_ on the callback thread anymore, invalidate mEnv
        if (maybeAttachThread(__func__)) {
            mEnv->DeleteGlobalRef(mCallback);
        } else {
            ALOGE("leaking callback reference %p because thread is not attached?!", mCallback);
        }
        // this will end up crashing if someone with a weak ref calls a callback method and the
        // method does not check if mCallback != null
        mEnv = nullptr;
        mCallback = nullptr;
        mHolder = nullptr;
    }
    bool onIncStrongAttempted(uint32_t flags, const void *id) override {
        return false; // never revive
    }
    private:
    bool mAttached = false;
    JNIEnv* mEnv = nullptr;
    int mId;
    bool maybeAttachThread(const char* caller) {
        // This relies on the fact that all callbacks will be called on the same thread.
        if (!mAttached) {
            JNIEnv* env;
            int ret = mHolder->vm->GetEnv((void**)&env, JNI_VERSION_1_6);
            if (ret == JNI_OK) {
                mEnv = env;
                mAttached = true;
            } else if (ret == JNI_EDETACHED) {
                char buf[50] = {'\0'};
                snprintf(buf, 50, "NativeTrack_cb%d", mId);
                struct {
                    jint version;
                    char *name;
                    jobject group;
                } attachArgs = {.version = JNI_VERSION_1_6, .name = &buf[0], .group = nullptr};
                ret = mHolder->vm->AttachCurrentThread(&env, &attachArgs);
                if (ret != JNI_OK) {
                    ALOGE("failed to attach jni thread %d", ret);
                    return false;
                }
                static pthread_once_t initialized = PTHREAD_ONCE_INIT;
                static pthread_key_t jni_env_key;
                pthread_once(&initialized, [] {
                    int err = pthread_key_create(&jni_env_key, myJniDetach);
                    if (err != 0) {
                        ALOGE("failed to create pthread key: %d, aborting!", err);
                        abort();
                    }
                });
                int err = pthread_setspecific(jni_env_key, mHolder->vm);
                if (err != 0) {
                    ALOGE("failed to set pthread key: %d, aborting!", err);
                    abort();
                }
                mEnv = env;
                mAttached = true;
            } else {
                ALOGE("failed to get jni env %d", ret);
            }
        }
        if (!mAttached) {
            ALOGE("tried to attach thread but failed (in %s)", caller);
            return false;
        }
        return true;
    }
};

static void callOnAudioDeviceUpdate(track_holder* holder, int audioIo, const DeviceIdVector& deviceIds) {
    if (!holder->thiz || holder->died || !holder->onAudioDeviceUpdate) return;
    JNIEnv* env;
    int ret = holder->vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED) {
        char buf[50] = {'\0'};
        snprintf(buf, 50, "audio_cb_tmp_thread");
        struct {
            jint version;
            char *name;
            jobject group;
        } attachArgs = {.version = JNI_VERSION_1_6, .name = &buf[0], .group = nullptr};
        int ret2 = holder->vm->AttachCurrentThread(&env, &attachArgs);
        if (ret2 != JNI_OK) {
            ALOGE("failed to attach jni thread %d, dropping callOnAudioDeviceUpdate", ret);
            return;
        }
    } else if (ret != JNI_OK) {
        ALOGE("failed to get jni env %d, dropping callOnAudioDeviceUpdate", ret);
        return;
    }
    jintArray deviceIdsJni;
    deviceIdsJni = env->NewIntArray((int32_t)deviceIds.size());
    if (deviceIdsJni == nullptr) {
        ALOGE("Out of memory, dropping onAudioDeviceUpdate");
        env->ExceptionClear();
        return;
    }
    env->SetIntArrayRegion(deviceIdsJni, 0, (int32_t)deviceIds.size(), &deviceIds[0]);
    env->CallVoidMethod(holder->thiz, holder->onAudioDeviceUpdate, audioIo, deviceIdsJni);
    env->DeleteLocalRef(deviceIdsJni);
    if (ret == JNI_EDETACHED) {
        myJniDetach(holder->vm);
    }
}

class DeviceCallbackV23 : public android::AudioSystem::AudioDeviceCallbackV23 {
public:
    track_holder* mHolder;

    explicit DeviceCallbackV23(track_holder& holder) : mHolder(&holder) {}

    void onAudioDeviceUpdate(int32_t audioIo, int32_t deviceId) override {
        if (mHolder != nullptr) {
            callOnAudioDeviceUpdate(mHolder, audioIo, {deviceId});
        } else {
            ALOGE("leaked device callback? drop onAudioDeviceUpdate io%d dev%d", audioIo, deviceId);
        }
    }

    bool onIncStrongAttempted(uint32_t flags, const void *id) override {
        return false; // never revive
    }

    void onLastStrongRef(const void *id) override {
        mHolder = nullptr;
    }
};

static void callbackAdapter(int event, void* userptr, void* info) {
    auto user = ((track_holder*) userptr)->callback;
    if (event == 9 && android_get_device_api_level() <= 23)
        event = 1001; // quirk: ancient CAF versions used code 9 for adsp failure event in case of
                      // LPA(Low Power Audio) playback, the proprietary predecessor of offload.
                      // while at it, LPA isn't supported here because the code is a royal mess
                      // and it got removed from CAF before L (only CM carried it a bit further).
    if (user == nullptr) {
        ALOGE("LEAKED callbackAdapter trying to call destroyed callback!!!");
        if (event == 0 || event == 9) {
            if (info) {
                ((android::AudioTrack::Buffer *) info)->mSize = 0;
            } else {
                ALOGE("event %d but info is nullptr? (while leaked?!)", event);
            }
        }
        return;
    }
    switch (event) {
        case 0 /* EVENT_MORE_DATA */:
            ((android::AudioTrack::Buffer*)info)->mSize =
                    user->onMoreData(*(android::AudioTrack::Buffer*)info);
            break;
        case 1 /* EVENT_UNDERRUN */:
            user->onUnderrun();
            break;
        case 2 /* EVENT_LOOP_END */:
            user->onLoopEnd(*(int32_t*)info);
            break;
        case 3 /* EVENT_MARKER */:
            user->onMarker(*(uint32_t*)info);
            break;
        case 4 /* EVENT_NEW_POS */:
            user->onNewPos(*(uint32_t*)info);
            break;
        case 5 /* EVENT_BUFFER_END */:
            user->onBufferEnd();
            break;
        case 6 /* EVENT_NEW_IAUDIOTRACK */:
            user->onNewIAudioTrack();
            break;
        case 7 /* EVENT_STREAM_END */:
            user->onStreamEnd();
            break;
        case 8 /* EVENT_NEW_TIMESTAMP */:
            user->onNewTimestamp(*(android::AudioTimestamp*)info);
            break;
        case 9 /* EVENT_CAN_WRITE_MORE_DATA */:
            ((android::AudioTrack::Buffer*)info)->mSize =
                    user->onCanWriteMoreData(*(android::AudioTrack::Buffer*)info);
            break;
        case 1001:
            ALOGE("unexpected event: ADSP failure"); // we don't use CM12.x LPA/Tunnel, can't happen
            break;
        default:
            ALOGE("unsupported event %d (user=%p info=%p infoVal=%d)", event, user, info,
                  info ? *(int32_t*)info : 0);
            break;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_00024Companion_initDlsym(JNIEnv* env, jobject) {
    if (!initLib(env))
        return false;
    if (android_get_device_api_level() >= 31) {
        DLSYM_OR_RETURN(libandroid_runtime, ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject, false)
        DLSYM_OR_RETURN(libpermission, ZN7android7content22AttributionSourceState14readFromParcelEPKNS_6ParcelE, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrackC1ERKNS_7content22AttributionSourceStateE, false)
    } else {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrackC1Ev, false)
    }
    DLSYM_OR_RETURN(libutils, ZN7android7RefBaseC2Ev, false)
    DLSYM_OR_RETURN(libutils, ZN7android7RefBaseD2Ev, false)
    DLSYM_OR_RETURN(libutils, ZNK7android7RefBase9incStrongEPKv, false)
    DLSYM_OR_RETURN(libutils, ZNK7android7RefBase9decStrongEPKv, false)
    DLSYM_OR_RETURN(libutils, ZNK7android7RefBase10createWeakEPKv, false)
    DLSYM_OR_RETURN(libutils, ZN7android7RefBase12weakref_type7decWeakEPKv, false)
    if (android_get_device_api_level() >= 31) {
	    DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem17getOffloadSupportERK20audio_offload_info_t, false)
    } else {
        DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem18isOffloadSupportedERK20audio_offload_info_t, false)
    }
    if (android_get_device_api_level() >= 36) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack18getRoutedDeviceIdsEv, false)
    } else if (android_get_device_api_level() == 35) {
        DLSYM_OR_ELSE(libaudioclient, ZN7android10AudioTrack18getRoutedDeviceIdsEv) {
            DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack17getRoutedDeviceIdEv, false)
        }
    } else if (android_get_device_api_level() >= 23) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack17getRoutedDeviceIdEv, false)
    }
    if (android_get_device_api_level() >= 23) {
	    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack15setOutputDeviceEi, false)
	    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack15getOutputDeviceEv, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack22addAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack25removeAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE, false)
        DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack21getOriginalSampleRateEv, false)
	    DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack15getPlaybackRateEv, false)
	    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack15setPlaybackRateERKNS_17AudioPlaybackRateE, false)
    } else {
        DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack19isOffloadedOrDirectEv, false)
    }
	if (android_get_device_api_level() >= 24) {
		DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack21getBufferDurationInUsEPl, false)
		DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack15pendingDurationEPiNS_17ExtendedTimestamp8LocationE, false)
		DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack12getTimestampEPNS_17ExtendedTimestampE, false)
	}
	DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack16getMinFrameCountEPm19audio_stream_type_tj, false)
    if (android_get_device_api_level() == 23) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem10getLatencyEiPj, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem13getFrameCountEiPm, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem15getSamplingRateEiPj, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem13releaseOutputEi19audio_stream_type_t15audio_session_t, false)
        DLSYM_OR_RETURN(libaudioclient, ZN7android11AudioSystem16getOutputForAttrEPK18audio_attributes_tPi15audio_session_tP19audio_stream_type_tjj14audio_format_tj20audio_output_flags_tiPK20audio_offload_info_t, false)
    }
    if (android_get_device_api_level() >= 31) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_t20audio_channel_mask_tm20audio_output_flags_tRKNS_2wpINS0_19IAudioTrackCallbackEEEiRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tRKNS_7content22AttributionSourceStateEPK18audio_attributes_tbfi, false)
    } else if (android_get_device_api_level() >= 28) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbfi, false)
    } else if (android_get_device_api_level() >= 26) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbf, false)
    } else if (android_get_device_api_level() >= 24) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tbf, false)
    } else if (android_get_device_api_level() >= 23) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tb, false)
    } else {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_t, false)
    }
    DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack9getOutputEv, false)
    if (android_get_device_api_level() >= 32) {
        DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack12pauseAndWaitERKNSt3__16chrono8durationIxNS1_5ratioILl1ELl1000EEEEE, false)
    }
	if (android_get_device_api_level() >= 26) {
		DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack10hasStartedEv, false)
	}
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack4stopEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack5pauseEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack5startEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack9setVolumeEf, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack21setAuxEffectSendLevelEf, false)
    DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack7stoppedEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack5flushEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack7setLoopEjji, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack17setMarkerPositionEj, false)
    DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack17getMarkerPositionEPj, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack23setPositionUpdatePeriodEj, false)
    DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack23getPositionUpdatePeriodEPj, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack11setPositionEj, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack11getPositionEPj, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack17getBufferPositionEPj, false)
    DLSYM_OR_RETURN(libaudioclient, ZNK7android10AudioTrack13getSampleRateEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack6reloadEv, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack15attachAuxEffectEi, false)
    DLSYM_OR_RETURN(libbinder, ZN7android7String8C1EPKc, false)
    DLSYM_OR_RETURN(libbinder, ZN7android7String8D1Ev, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack13setParametersERKNS_7String8E, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack12getTimestampERNS_14AudioTimestampE, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack5writeEPKvjb, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack12obtainBufferEPNS0_6BufferEiPj, false)
    DLSYM_OR_RETURN(libaudioclient, ZN7android10AudioTrack13releaseBufferEPKNS0_6BufferE, false)
    return true;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_create(
        JNIEnv *env, jobject thiz, jobject parcel) {
    auto theTrack = ::operator new(AUDIO_TRACK_SIZE);
    memset(theTrack, (unsigned char)0xde, AUDIO_TRACK_SIZE);
    auto holder = new track_holder(env);
    if (parcel != nullptr) { // implies SDK >= 31
        // I'm too cool to call AttributionSourceState ctor before using it.
        auto myParcel = ZN7android19parcelForJavaObjectEP7_JNIEnvP8_jobject(env, parcel);
        if (myParcel == nullptr) {
            ALOGE("myParcel is NULL");
            ::operator delete(theTrack);
            delete holder;
            return 0;
        }
        auto ats = ::operator new(ATTRIBUTION_SOURCE_SIZE);
        memset(ats, 0, ATTRIBUTION_SOURCE_SIZE);
        ZN7android7content22AttributionSourceState14readFromParcelEPKNS_6ParcelE(ats, myParcel);
        ZN7android10AudioTrackC1ERKNS_7content22AttributionSourceStateE(theTrack, ats);
        holder->ats = ats;
    } else {
        ZN7android10AudioTrackC1Ev(theTrack);
    }
    holder->thiz = env->NewGlobalRef(thiz);
    jclass clazz = env->GetObjectClass(thiz);
    holder->onAudioDeviceUpdate = env->GetMethodID(clazz, "onAudioDeviceUpdate", "(I[I)V");
    if (holder->onAudioDeviceUpdate == nullptr) {
        ALOGI("callback does not have onAudioDeviceUpdate(I[I)V, assuming it does not care");
        env->ExceptionClear();
    }
    env->DeleteLocalRef(clazz);
    auto callback = new MyCallback(*holder, env, thiz);
    callback->incStrong(holder);
    if (android_get_device_api_level() >= 33) {
        // virtual inheritance, let's have the compiler generate the vtable stuff
        ((android::AudioTrack *) theTrack)->incStrong(holder);
    } else {
        ZNK7android7RefBase9incStrongEPKv(theTrack, holder);
    }
    holder->track = theTrack;
    holder->callback = callback;
    /*if (android_get_device_api_level() >= 36 || ZN7android10AudioTrack18getRoutedDeviceIdsEv) {
        auto cb = new DeviceCallbackV35Qpr2(*holder);
        cb->incStrong(holder);
        holder->deviceCallback = cb;
    } else if (android_get_device_api_level() >= 33) {
        auto cb = new DeviceCallbackV33(*holder);
        cb->incStrong(holder);
        holder->deviceCallback = cb;
    } else*/ if (android_get_device_api_level() /*>=*/== 23) {
        auto cb = new DeviceCallbackV23(*holder);
        cb->incStrong(holder);
        holder->deviceCallback = cb;
    }
    if (holder->deviceCallback) {
        fake_sp cb = {.thePtr=holder->deviceCallback};
        int32_t ret = ZN7android10AudioTrack22addAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE(theTrack, cb);
        if (ret != 0) {
            ALOGE("failed to add device callback, error %d", ret);
        }
    }
    return (intptr_t)holder;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_set(
        JNIEnv* env, jobject, jlong ptr, jint streamType, jint sampleRate, jint format,
        jint channelMask, jint frameCount, jint trackFlags, jint sessionId, jfloat maxRequiredSpeed,
        jint selectedDeviceId, jint bitRate, jlong durationUs, jboolean hasVideo, jboolean smallBuf,
        jboolean isStreaming, jint bitWidth, jint offloadBufferSize, jint usage, jint contentType,
        jint attrFlags, jint notificationFrames, jboolean doNotReconnect, jint transferMode,
        jint contentId, jint syncId, jint encapsulationMode, jobject sharedMem) {
    if (android_get_device_api_level() < 23 && maxRequiredSpeed != 1.0f) {
        ALOGE("Android 5.x does not support speed adjustment, maxRequiredSpeed != 1f is wrong");
        return INT32_MIN;
    }
    if (android_get_device_api_level() < 23 && selectedDeviceId != 0) {
        ALOGE("Android 5.x does not support selected devices, selectedDeviceId != 0 is wrong");
        return INT32_MIN;
    }
    if (android_get_device_api_level() < 30 && (contentId != 0 || syncId != 0)) {
        ALOGE("Tuner supported since Android 11, (contentId != 0 || syncId != 0) is wrong");
        return INT32_MIN;
    }
    auto holder = (track_holder*) ptr;
    if (sharedMem) {
        holder->sharedMemoryBuffer = env->NewGlobalRef(sharedMem);
    }
    jint ret = 0;
    fake_sp sharedMemory = {.thePtr = sharedMem
                            ? env->GetDirectBufferAddress(sharedMem) : nullptr};
    union {
        audio_offload_info_t_v30 newInfo = {};
        audio_offload_info_t_v26 oldInfo;
    } offloadInfo;
    union {
        audio_attributes_v28 newAttrs = {};
        audio_attributes_legacy oldAttrs;
    } audioAttributes;
    // We must always provide offloadInfo to work around CAF being CAF.
    // without offloadInfo, CAF code might try setting it to stub when unsupported operation occurs
    //  or might not let us pretend to be MediaPlayer / legit track offload or might hardcode wrong
    //  bitWidth even if hal can do 8.24 offload.
    if (android_get_device_api_level() >= 28) {
        offloadInfo.newInfo = {
                .version = AUDIO_MAKE_OFFLOAD_INFO_VERSION(0, 2),
                .size = sizeof(audio_offload_info_t_v30),
                .sample_rate = (uint32_t)sampleRate,
                .channel_mask = (uint32_t)channelMask,
                .format = (uint32_t)format,
                .stream_type = streamType,
                .bit_rate = (uint32_t)bitRate,
                .duration_us = durationUs,
                .has_video = (bool)hasVideo,
                .is_streaming = (bool)isStreaming,
                .bit_width = (uint32_t)bitWidth,
                .offload_buffer_size = (uint32_t)offloadBufferSize,
                .usage = usage,
                .encapsulation_mode = encapsulationMode, // tuner
                .content_id = contentId, // tuner
                .sync_id = syncId, // tuner
        };
        audioAttributes.newAttrs = {
                .content_type = contentType,
                .usage = usage,
                .source = LEGACY_AUDIO_SOURCE_DEFAULT,
                .flags = (uint32_t)attrFlags,
        };
        memset(&audioAttributes.newAttrs.tags[0], '\0', 256);
    } else {
        offloadInfo.oldInfo = {
                .version = AUDIO_MAKE_OFFLOAD_INFO_VERSION(0, 1),
                .size = sizeof(audio_offload_info_t_v26),
                .sample_rate = (uint32_t)sampleRate,
                .channel_mask = (uint32_t)channelMask,
                .format = (uint32_t)format,
                .stream_type = streamType,
                .bit_rate = (uint32_t)bitRate,
                .duration_us = durationUs,
                .has_video = (bool)hasVideo,
                .is_streaming = (bool)isStreaming,
                // informative, since Android 8.0, earlier on CAF
                .bit_width = (uint32_t)bitWidth,
                .offload_buffer_size = (uint32_t)offloadBufferSize,
                .usage = usage,
                .use_small_bufs = (bool)smallBuf, // old CAF only
        };
        audioAttributes.oldAttrs = {
                .content_type = contentType,
                .usage = usage,
                .source = LEGACY_AUDIO_SOURCE_DEFAULT,
                .flags = (uint32_t)attrFlags,
        };
        memset(&audioAttributes.newAttrs.tags[0], '\0', 256);
    }
    if (android_get_device_api_level() >= 31) { // Android 12.0 (SDK 31) or later
        auto refs = holder->callback->createWeak(holder);
        fake_wp callback = {.thePtr = holder->callback, .refs = refs};
        ret = ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_t20audio_channel_mask_tm20audio_output_flags_tRKNS_2wpINS0_19IAudioTrackCallbackEEEiRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tRKNS_7content22AttributionSourceStateEPK18audio_attributes_tbfi(
                holder->track,
                /* streamType = */ streamType,
                /* sampleRate = */ sampleRate,
                /* format = */ format,
                /* channelMask = */ channelMask,
                /* frameCount = */ frameCount,
                /* flags = */ trackFlags,
                /* callback = */ callback,
                /* notificationFrames = */ notificationFrames,
                /* sharedBuffer = */ sharedMemory,
                /* threadCanCallJava = */ true,
                /* sessionId = */ sessionId,
                /* transferType = */ transferMode,
                /* offloadInfo = */ &offloadInfo,
                /* attributionSource = */ *((int *) holder->ats),
                /* pAttributes = */ &audioAttributes.newAttrs,
                /* doNotReconnect = */ doNotReconnect,
                /* maxRequiredSpeed = */ maxRequiredSpeed,
                /* selectedDeviceId = */ selectedDeviceId
        );
        // wp copy constructor increased weak with it's own id, so we're done here
        ZN7android7RefBase12weakref_type7decWeakEPKv(refs /* yes, refs */, holder);
        // we own attributionSource, so get rid of it
        ::operator delete(holder->ats);
        holder->ats = nullptr;
    } else if (android_get_device_api_level() >= 28) { // Android 9 (SDK 28) to Android 11 (SDK 30)
        ret = ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbfi(
                holder->track,
                /* streamType = */ streamType,
                /* sampleRate = */ sampleRate,
                /* format = */ format,
                /* channelMask = */ channelMask,
                /* frameCount = */ frameCount,
                /* flags = */ trackFlags,
                /* callback = */ callbackAdapter,
                /* user = */ holder->callback,
                /* notificationFrames = */ notificationFrames,
                /* sharedBuffer = */ sharedMemory,
                /* threadCanCallJava = */ true,
                /* sessionId = */ sessionId,
                /* transferType = */ transferMode,
                /* offloadInfo = */ &offloadInfo,
                /* uid = */ getuid(),
                /* pid = */ getpid(),
                /* pAttributes = */ &audioAttributes.newAttrs,
                /* doNotReconnect = */ doNotReconnect,
                /* maxRequiredSpeed = */ maxRequiredSpeed,
                /* selectedDeviceId = */ selectedDeviceId
        );
    } else if (android_get_device_api_level() >= 26) { // Android 8.x
        // This is safe to call before set() only in O!
        ZN7android10AudioTrack15setOutputDeviceEi(holder->track, selectedDeviceId);
        ret = ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tjiPK18audio_attributes_tbf(
                holder->track,
                /* streamType = */ streamType,
                /* sampleRate = */ sampleRate,
                /* format = */ format,
                /* channelMask = */ channelMask,
                /* frameCount = */ frameCount,
                /* flags = */ trackFlags,
                /* callback = */ callbackAdapter,
                /* user = */ holder->callback,
                /* notificationFrames = */ notificationFrames,
                /* sharedBuffer = */ sharedMemory,
                /* threadCanCallJava = */ true,
                /* sessionId = */ sessionId,
                /* transferType = */ transferMode,
                /* offloadInfo = */ &offloadInfo,
                /* uid = */ getuid(),
                /* pid = */ getpid(),
                /* pAttributes = */ &audioAttributes.newAttrs,
                /* doNotReconnect = */ doNotReconnect,
                /* maxRequiredSpeed = */ maxRequiredSpeed
        );
    } else if (android_get_device_api_level() >= 24) { // Android 7.x
#ifdef __LP64__
        *(int32_t*)((uintptr_t)holder->track + 0x300) = selectedDeviceId; // aarch64, x86_64
#elif defined(i386)
        *(int32_t*)((uintptr_t)holder->track + 0x270) = selectedDeviceId;
#else
        *(int32_t*)((uintptr_t)holder->track + 0x27c) = selectedDeviceId;
#endif
        ret = ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_iRKNS_2spINS_7IMemoryEEEb15audio_session_tNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tbf(
                holder->track,
                /* streamType = */ streamType,
                /* sampleRate = */ sampleRate,
                /* format = */ format,
                /* channelMask = */ channelMask,
                /* frameCount = */ frameCount,
                /* flags = */ trackFlags,
                /* callback = */ callbackAdapter,
                /* user = */ holder->callback,
                /* notificationFrames = */ notificationFrames,
                /* sharedBuffer = */ sharedMemory,
                /* threadCanCallJava = */ true,
                /* sessionId = */ sessionId,
                /* transferType = */ transferMode,
                /* offloadInfo = */ &offloadInfo,
                /* uid = */ (int)getuid(),
                /* pid = */ getpid(),
                /* pAttributes = */ &audioAttributes.newAttrs,
                /* doNotReconnect = */ doNotReconnect,
                /* maxRequiredSpeed = */ maxRequiredSpeed
        );
    } else if (android_get_device_api_level() >= 23) { // Android 6.0 (SDK 23)
#ifdef __LP64__
        *(int32_t*)((uintptr_t)holder->track + 0x2e0) = selectedDeviceId; // aarch64, x86_64
#elif defined(i386)
        *(int32_t*)((uintptr_t)holder->track + 0x24c) = selectedDeviceId;
#else
        *(int32_t*)((uintptr_t)holder->track + 0x254) = selectedDeviceId;
#endif
        if (maxRequiredSpeed > 1.0f) {
            if (trackFlags & 4 /* AUDIO_OUTPUT_FLAG_FAST */) {
                // if we're unlucky, the calculated frame count may be smaller than HAL frame count
                // and fast track will be rejected just because of this. but that's sort-of far-fetched
                // because our frame count calculation is quite conservative, and using fast tracks with
                // large buffer sizes is sort of counter productive as well, so let's just try our best
                ALOGI("fast requested with maxRequiredSpeed(%f) emulation, "
                      "trying to set frame count anyway", maxRequiredSpeed);
            }
            int32_t output = 0;
            int32_t myStreamType = streamType;
            int32_t status = ZN7android11AudioSystem16getOutputForAttrEPK18audio_attributes_tPi15audio_session_tP19audio_stream_type_tjj14audio_format_tj20audio_output_flags_tiPK20audio_offload_info_t(
                    &audioAttributes.oldAttrs, &output,sessionId, &myStreamType,
                    getuid(),sampleRate, format, channelMask,trackFlags,
                    selectedDeviceId,&offloadInfo.oldInfo);
            uint32_t afLatencyMs = 0;
            size_t afFrameCount = 0;
            uint32_t afSampleRate = 0;
            uint32_t minBufCount = 0;
            int32_t minFrameCount = 0;
            if (status != 0 || output == 0) {
                ALOGE("Could not get audio output for session %d, stream type %d, usage %d, "
                      "sample rate %u, format %#x, channel mask %#x, flags %#x",
                      sessionId, streamType, audioAttributes.oldAttrs.usage, sampleRate, format,
                      channelMask, trackFlags);
                goto fallback;
            }
            status = ZN7android11AudioSystem10getLatencyEiPj(output, &afLatencyMs);
            if (status != 0) {
                ALOGE("getLatency(%d) failed status %d", output, status);
                goto release;
            }
            status = ZN7android11AudioSystem13getFrameCountEiPm(output, &afFrameCount);
            if (status != 0) {
                ALOGE("getFrameCount(output=%d) status %d", output, status);
                goto release;
            }
            status = ZN7android11AudioSystem15getSamplingRateEiPj(output, &afSampleRate);
            if (status != 0) {
                ALOGE("getSamplingRate(output=%d) status %d", output, status);
                goto release;
            }
            // emulate maxRequiredSpeed feature in M, where speed adjustment was added, by
            // calculating the minimum frame count like AudioTrack would in N
            // (where maxRequiredSpeed was added)
            minBufCount = afLatencyMs / ((1000 * afFrameCount) / afSampleRate);
            minFrameCount = (int32_t)((double)(minBufCount < 2 ? 2 : minBufCount) *
                    (double)(sampleRate == afSampleRate ? afFrameCount : (size_t)((uint64_t)afFrameCount *
                    sampleRate / afSampleRate + 1 + 1))
                                    * (double)maxRequiredSpeed + 1 + 1);
            if (frameCount < minFrameCount) {
                ALOGI("corrected frameCount(%d) to minFrameCount(%d) for maxRequiredSpeed(%f) "
                      "emulation", frameCount, minFrameCount, maxRequiredSpeed);
                frameCount = minFrameCount;
            }
            release:
            ZN7android11AudioSystem13releaseOutputEi19audio_stream_type_t15audio_session_t(output, streamType, sessionId);
        }
        fallback:
        ret = ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_tb(
                holder->track,
                /* streamType = */ streamType,
                /* sampleRate = */ sampleRate,
                /* format = */ format,
                /* channelMask = */ channelMask,
                /* frameCount = */ frameCount,
                /* flags = */ trackFlags,
                /* callback = */ callbackAdapter,
                /* user = */ holder->callback,
                /* notificationFrames = */ notificationFrames,
                /* sharedBuffer = */ sharedMemory,
                /* threadCanCallJava = */ true,
                /* sessionId = */ sessionId,
                /* transferType = */ transferMode,
                /* offloadInfo = */ &offloadInfo,
                /* uid = */ getuid(),
                /* pid = */ getpid(),
                /* pAttributes = */ &audioAttributes.newAttrs,
                /* doNotReconnect = */ doNotReconnect
                );
    } else { // Android 5.0 / 5.1 (SDK 21 / 22)
        ret = ZN7android10AudioTrack3setE19audio_stream_type_tj14audio_format_tjm20audio_output_flags_tPFviPvS4_ES4_jRKNS_2spINS_7IMemoryEEEbiNS0_13transfer_typeEPK20audio_offload_info_tiiPK18audio_attributes_t(
                holder->track,
                /* streamType = */ streamType,
                /* sampleRate = */ sampleRate,
                /* format = */ format,
                /* channelMask = */ channelMask,
                /* frameCount = */ frameCount,
                /* flags = */ trackFlags,
                /* callback = */ callbackAdapter,
                /* user = */ holder->callback,
                /* notificationFrames = */ notificationFrames,
                /* sharedBuffer = */ sharedMemory,
                /* threadCanCallJava = */ true,
                /* sessionId = */ sessionId,
                /* transferType = */ transferMode,
                /* offloadInfo = */ &offloadInfo,
                /* uid = */ (int32_t)getuid(),
                /* pid = */ getpid(),
                /* pAttributes = */ &audioAttributes.newAttrs
                );
        if (ret == 0 && doNotReconnect) {
            // quirk: doNotReconnect will not work on some MTKs for non-offload (ie mixed or direct)
            // because onNewIAudioTrack is not called (on purpose). such is life.
            holder->deathEmulation = !ZNK7android10AudioTrack19isOffloadedOrDirectEv(holder->track);
        }
    }
    return ret;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getRealPtr(
        JNIEnv *, jobject, jlong ptr) {
    return (intptr_t)((track_holder*) ptr)->track;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_notificationFramesActFromOffset(
        JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    size_t extra;
    switch (android_get_device_api_level()) {
        case 27:
        case 26:
#ifdef __LP64__
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x228); // aarch64, x86_64
#elif defined(i386)
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1d8);
#else
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1dc);
#endif
        case 25:
        case 24:
#ifdef __LP64__
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x220); // aarch64, x86_64
#elif defined(i386)
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1cc);
#else
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1d4);
#endif
        case 23:
#ifdef __LP64__
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x214); // aarch64, x86_64
#elif defined(i386)
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1c0);
#else
            return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1c8);
#endif
        case 22:
            extra =
#ifdef __ARM_ARCH_7A__
                /* QCOM_DIRECTTRACK (BOARD_USES_LEGACY_ALSA_AUDIO), only for MSM8x60 in CM12.x */
                dlsym(libaudioclient_handle, "_ZN7android10AudioTrack6notifyEi") ? 0x1c /* 0x1c8 */ :
#endif
                (dlsym(libaudioclient_handle, "_ZN7android10AudioTrack28initializeTrackOffloadParamsEv")
#ifdef __LP64__
                 ? 0x20 /* 0x20c */ : 0x0);
            // edge case: couldn't find any CM12.1 x86_64 build
#elif defined(i386)
                 ? 0x18 /* 0x1bc */ : 0x0);
#else
                 ? 0x14 /* 0x1c0 */ : 0x0);
#endif
            break;
        case 21:
            extra =
#ifdef __ARM_ARCH_7A__
                /* QCOM_DIRECTTRACK (BOARD_USES_LEGACY_ALSA_AUDIO), only for MSM8x60 in CM12.x */
                dlsym(libaudioclient_handle, "_ZN7android10AudioTrack6notifyEi") ? 0x8 /* 0x1b4 */ :
#endif
                (0);
            break;
        default:
            return INT32_MAX;
    }
#ifdef __LP64__
    return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1ec + extra); // aarch64, x86_64
#elif defined(i386)
    return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1a4 + extra);
#else
    return (int32_t)*(uint32_t*)((uintptr_t)holder->track + 0x1ac + extra);
#endif
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getProxy(JNIEnv* env, jobject, jlong ptr, jint sessionId) {
    if (android_get_device_api_level() < 24) {
        ALOGE("getProxy should only be called on N+");
        return nullptr;
    }
    auto holder = (track_holder*)ptr;
    auto track = holder->track;
    jclass at = env->FindClass("android/media/AudioTrack");
    if (at == nullptr) {
        ALOGE("android/media/AudioTrack does not exist?!");
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(at, "<init>", "(J)V");
    if (ctor == nullptr) {
        ALOGE("getProxy: didn't find android/media/AudioTrack.<init>(J)V");
        return nullptr;
    }
    jmethodID setup = env->GetMethodID(at, "deferred_connect", "(J)V");
    if (setup == nullptr) {
        ALOGE("getProxy: didn't find android/media/AudioTrack.deferred_connect(J)V");
        return nullptr;
    }
    jmethodID regPb = env->GetMethodID(at, "baseRegisterPlayer", "(I)V");
    if (regPb == nullptr) {
        ALOGE("getProxy: didn't find android/media/AudioTrack.baseRegisterPlayer(J)V");
        return nullptr;
    }
    jmethodID setId = env->GetMethodID(at, "native_setPlayerIId", "(I)V");
    if (setId == nullptr) {
        ALOGW("getProxy: didn't find android/media/AudioTrack.native_setPlayerIId(I)V");
		if (android_get_device_api_level() >= 31) {
			return nullptr; // throw
		}
        env->ExceptionClear();
    }
    jfieldID id = env->GetFieldID(at, "mPlayerIId", "I");
    if (id == nullptr) {
        ALOGW("getProxy: didn't find android/media/AudioTrack.mPlayerIId int");
	    if (android_get_device_api_level() >= 31) {
		    return nullptr; // throw
	    }
        env->ExceptionClear();
    }
    // creating with 0 and then using deferred_connect() skips PlayerBase registration, which
    // allows us to do it ourselves, but with our real session ID (almost like a real AudioTrack).
    // before N, PlayerBase didn't exist, so we don't have to do that anywhere else.
    jobject proxy = env->NewObject(at, ctor, 0);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    env->CallVoidMethod(proxy, setup, track);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    env->CallVoidMethod(proxy, regPb, sessionId);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    if (setId != nullptr && id != nullptr) {
        // Let's be a nice citizen and contribute to MediaMetrics.
        jint playerId = env->GetIntField(proxy, id);
        if (env->ExceptionCheck()) {
            return nullptr;
        }
        env->CallVoidMethod(proxy, setId, playerId);
        if (env->ExceptionCheck()) {
            return nullptr;
        }
    }
    env->DeleteLocalRef(at);
    return proxy;
}

extern "C" JNIEXPORT void JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_dtor(
        JNIEnv * env, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    if (holder->deviceCallback) {
        fake_sp cb = {.thePtr=holder->deviceCallback};
        int ret = ZN7android10AudioTrack25removeAudioDeviceCallbackERKNS_2spINS_11AudioSystem19AudioDeviceCallbackEEE(holder->track, cb);
        if (ret != 0) {
            ALOGE("failed to remove audio device callback, error %d", ret);
        }
        ((DeviceCallbackV23*)holder->deviceCallback)->decStrong(holder);
        holder->deviceCallback = nullptr;
    }
    // RefBase will call the dtor; dtor will call stopAndJoinCallbacks where appropriate
    if (android_get_device_api_level() >= 33) {
        // virtual inheritance, let's have the compiler generate the vtable stuff
        ((android::AudioTrack *) holder->track)->decStrong(holder);
    } else {
        ZNK7android7RefBase9decStrongEPKv(holder->track, holder);
    }
    holder->callback->decStrong(holder);
    holder->track = nullptr;
    holder->callback = nullptr;
    if (holder->sharedMemoryBuffer) {
        env->DeleteGlobalRef(holder->sharedMemoryBuffer);
    }
    env->DeleteGlobalRef(holder->thiz);
    delete holder;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_00024Companion_isOffloadSupported(
        JNIEnv*, jobject, jint sampleRate, jint format,
        jint channelMask, jint bitRate, jint bitWidth, jint offloadBufferSize) {
    union {
        audio_offload_info_t_v30 newInfo = {};
        audio_offload_info_t_v26 oldInfo;
    } offloadInfo;
    if (android_get_device_api_level() >= 28) {
        offloadInfo.newInfo = {
                .version = AUDIO_MAKE_OFFLOAD_INFO_VERSION(0, 2),
                .size = sizeof(audio_offload_info_t_v30),
                .sample_rate = (uint32_t)sampleRate,
                .channel_mask = (uint32_t)channelMask,
                .format = (uint32_t)format,
                .stream_type = LEGACY_AUDIO_STREAM_MUSIC, // must be MUSIC
                .bit_rate = (uint32_t)bitRate,
                .duration_us = 2100 /* 3.5min * 60 */ * 1000 * 1000, // must be >60s
                .has_video = (bool)false,
                .is_streaming = (bool)false,
                .bit_width = (uint32_t)bitWidth,
                .offload_buffer_size = (uint32_t)offloadBufferSize,
                .usage = LEGACY_AUDIO_USAGE_MEDIA,
                .encapsulation_mode = 0,
                .content_id = 0,
                .sync_id = 0
        };
    } else {
        offloadInfo.oldInfo = {
                .version = AUDIO_MAKE_OFFLOAD_INFO_VERSION(0, 1),
                .size = sizeof(audio_offload_info_t_v26),
                .sample_rate = (uint32_t)sampleRate,
                .channel_mask = (uint32_t)channelMask,
                .format = (uint32_t)format,
                .stream_type = LEGACY_AUDIO_STREAM_MUSIC, // must be MUSIC
                .bit_rate = (uint32_t)bitRate,
                .duration_us = 2100 /* 3.5min * 60 */ * 1000 * 1000, // must be >60s
                .has_video = (bool)false,
                .is_streaming = (bool)false,
                .bit_width = (uint32_t)bitWidth,
                .offload_buffer_size = (uint32_t)offloadBufferSize,
                .usage = LEGACY_AUDIO_USAGE_MEDIA,
                .use_small_bufs = false,
        };
    }
	if (android_get_device_api_level() >= 31) {
		return ZN7android11AudioSystem17getOffloadSupportERK20audio_offload_info_t(offloadInfo.newInfo);
	} else {
		return ZN7android11AudioSystem18isOffloadSupportedERK20audio_offload_info_t(offloadInfo.oldInfo);
	}
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_startInternal(JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return -32; // DEAD_OBJECT
    return ZN7android10AudioTrack5startEv(holder->track);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_stopInternal(JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    ZN7android10AudioTrack4stopEv(holder->track);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_stoppedInternal(JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    return ZNK7android10AudioTrack7stoppedEv(holder->track);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_flushInternal(JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    ZN7android10AudioTrack5flushEv(holder->track);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_pauseInternal(JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    ZN7android10AudioTrack5pauseEv(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setVolumeInternal(JNIEnv *, jobject,
                                                                 jlong ptr, jfloat volume) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack9setVolumeEf(holder->track, volume);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setAuxEffectSendLevelInternal(JNIEnv *,
                                                                             jobject,
                                                                             jlong ptr,
                                                                             jfloat level) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack21setAuxEffectSendLevelEf(holder->track, level);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getAuxEffectSendLevelInternal(JNIEnv *,
                                                                             jobject,
                                                                             jlong ptr) {
    auto holder = (track_holder*) ptr;
    return ZNK7android10AudioTrack21getAuxEffectSendLevelEPf(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setSampleRateInternal(JNIEnv*, jobject,
                                                                     jlong ptr, jint rate) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack13setSampleRateEj(holder->track, rate);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getSampleRateInternal(JNIEnv *, jobject,
                                                                     jlong ptr) {
    auto holder = (track_holder*) ptr;
    return ZNK7android10AudioTrack13getSampleRateEv(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setLoopInternal(JNIEnv *, jobject, jlong ptr,
                                                               jint loop_start, jint loop_end,
                                                               jint loop_count) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack7setLoopEjji(holder->track, loop_start, loop_end, loop_count);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setMarkerPositionInternal(JNIEnv*, jobject,
                                                                         jlong ptr, jint pos) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack17setMarkerPositionEj(holder->track, pos);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getMarkerPositionInternal(JNIEnv*, jobject,
                                                                         jlong ptr) {
    auto holder = (track_holder*) ptr;
    uint32_t out = 0;
    return (int64_t)(((uint64_t)ZNK7android10AudioTrack17getMarkerPositionEPj(
            holder->track, &out) << 32) | out);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setPositionUpdatePeriodInternal(JNIEnv*, jobject,
                                                                         jlong ptr, jint pos) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack23setPositionUpdatePeriodEj(holder->track, pos);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getPositionUpdatePeriodInternal(JNIEnv*, jobject,
                                                                         jlong ptr) {
    auto holder = (track_holder*) ptr;
    uint32_t out = 0;
    return (int64_t)(((uint64_t)ZNK7android10AudioTrack23getPositionUpdatePeriodEPj(
            holder->track, &out) << 32) | out);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setPositionInternal(JNIEnv*, jobject,
                                                                               jlong ptr, jint pos) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack11setPositionEj(holder->track, pos);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getPositionInternal(JNIEnv*, jobject,
                                                                               jlong ptr) {
    auto holder = (track_holder*) ptr;
    uint32_t out = 0;
    return (int64_t)(((uint64_t)ZN7android10AudioTrack11getPositionEPj(
            holder->track, &out) << 32) | out);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getBufferPositionInternal(JNIEnv*, jobject,
                                                                   jlong ptr) {
    auto holder = (track_holder*) ptr;
    uint32_t out = 0;
    return (int64_t)(((uint64_t)ZN7android10AudioTrack17getBufferPositionEPj(
            holder->track, &out) << 32) | out);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_reloadInternal(JNIEnv *, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack6reloadEv(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getOutputInternal(JNIEnv*, jobject, jlong ptr) {
    auto holder = (track_holder*) ptr;
    return (int32_t)ZNK7android10AudioTrack9getOutputEv(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_attachAuxEffectInternal(JNIEnv*, jobject,
                                                                       jlong ptr, jint effect_id) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return -32; // DEAD_OBJECT
    return ZN7android10AudioTrack15attachAuxEffectEi(holder->track, effect_id);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getUnderrunFramesInternal(JNIEnv*, jobject,
                                                                         jlong ptr) {
    auto holder = (track_holder*) ptr;
    return (int32_t)ZNK7android10AudioTrack17getUnderrunFramesEv(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setParametersInternal(JNIEnv *env, jobject,
                                                                     jlong ptr, jstring params) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return -32; // DEAD_OBJECT
    const char* str = env->GetStringUTFChars(params, nullptr);
    String8 string8 = {};
    ZN7android7String8C1EPKc(&string8, str);
    env->ReleaseStringUTFChars(params, str);
    int32_t ret = ZN7android10AudioTrack13setParametersERKNS_7String8E(holder->track, string8);
    ZN7android7String8D1Ev(&string8);
    return ret;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getParametersInternal(JNIEnv *env, jobject,
                                                                     jlong ptr, jstring params) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return nullptr;
    const char* str = env->GetStringUTFChars(params, nullptr);
    String8 string8 = {};
    ZN7android7String8C1EPKc(&string8, str);
    env->ReleaseStringUTFChars(params, str);
    String8 ret = ZN7android10AudioTrack13getParametersERKNS_7String8E(holder->track, string8);
    ZN7android7String8D1Ev(&string8);
    jstring data = env->NewStringUTF(ret.data);
    ZN7android7String8D1Ev(&ret);
    return data;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getTimestampInternal(JNIEnv* env, jobject,
                                                                    jlong ptr, jlongArray out) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return -32; // DEAD_OBJECT
    android::AudioTimestamp ts;
    int32_t ret = ZN7android10AudioTrack12getTimestampERNS_14AudioTimestampE(holder->track, ts);
    if (ret == 0) {
        jlong* arr = env->GetLongArrayElements(out, nullptr);
        arr[0] = (jlong)ts.mPosition;
        arr[1] = (jlong)((ts.mTime.tv_sec * 1000000000LL) + ts.mTime.tv_nsec);
        env->ReleaseLongArrayElements(out, arr, 0);
    }
    return ret;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_writeInternal__JLjava_nio_ByteBuffer_2IIZ(JNIEnv *env,
                                                                                         jobject,
                                                                                         jlong ptr,
                                                                                         jobject buf,
                                                                                         jint offset,
                                                                                         jint size,
                                                                                         jboolean blocking) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return -32; // DEAD_OBJECT
    auto buffer = reinterpret_cast<uintptr_t>(env->GetDirectBufferAddress(buf));
    if (buffer == 0) {
        return INT32_MIN;
    }
    void* base = (void*)(buffer + offset);
    return ZN7android10AudioTrack5writeEPKvjb(holder->track, base, size, blocking);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_writeInternal__J_3BIIZ(JNIEnv *env, jobject,
                                                                    jlong ptr, jbyteArray buf,
																	jint offset, jint size,
                                                                    jboolean blocking) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return -32; // DEAD_OBJECT
    jbyte* buffer = env->GetByteArrayElements(buf, nullptr);
    if (buffer == nullptr) {
        return INT32_MIN;
    }
    void* base = buffer + offset;
    ssize_t ret = ZN7android10AudioTrack5writeEPKvjb(holder->track, base, size, blocking);
    env->ReleaseByteArrayElements(buf, buffer, JNI_ABORT);
    return ret;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_writeInternal__J_3FIIZ(JNIEnv *env, jobject,
                                                                      jlong ptr, jfloatArray buf,
                                                                      jint offset, jint size,
                                                                      jboolean blocking) {
	auto holder = (track_holder*) ptr;
	if (holder->died)
		return -32; // DEAD_OBJECT
	jfloat* buffer = env->GetFloatArrayElements(buf, nullptr);
	if (buffer == nullptr) {
		return INT32_MIN;
	}
	void* base = buffer + offset;
	ssize_t ret = ZN7android10AudioTrack5writeEPKvjb(holder->track, base, size * sizeof(jfloat), blocking);
	env->ReleaseFloatArrayElements(buf, buffer, JNI_ABORT);
	return ret;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_obtainBufferInternal(JNIEnv *env, jobject,
                                                                    jlong ptr, jint frame_size,
                                                                    jint waitCount, jlongArray nc,
                                                                    jlong requested_frame_count) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return nullptr;
    android::AudioTrack::Buffer temp;
    temp.frameCount = requested_frame_count;
    temp.mSize = requested_frame_count * frame_size; // technically not needed
    size_t nonContig = 0;
    int32_t ret = ZN7android10AudioTrack12obtainBufferEPNS0_6BufferEiPj(holder->track, &temp,
                                                                        waitCount, &nonContig);
    if (nc != nullptr) {
        jlong* arr = env->GetLongArrayElements(nc, nullptr);
        arr[0] = nonContig;
        env->ReleaseLongArrayElements(nc, arr, 0);
    }
    if (ret != 0) {
        ALOGE("obtainBuffer failed: ret %d", ret);
        return nullptr;
    }
    if (temp.frameCount * frame_size != temp.mSize) {
        ALOGE("obtainBuffer unexpected frame size frameCount(%zu) frameSize(%d) size(%zu)",
              temp.frameCount, frame_size, temp.mSize);
        temp.frameCount = 0;
        temp.mSize = 0;
        ZN7android10AudioTrack13releaseBufferEPKNS0_6BufferE(holder->track, &temp);
        return nullptr;
    }
    holder->sequences.emplace(temp.raw, temp.sequence);
    return env->NewDirectByteBuffer(temp.raw, temp.mSize);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_releaseBufferInternal(JNIEnv *env, jobject,
                                                                     jlong ptr, jint frame_size,
                                                                     jobject buf, jint limit) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return;
    android::AudioTrack::Buffer temp;
    temp.raw = env->GetDirectBufferAddress(buf);
    temp.mSize = limit;
    temp.frameCount = limit / frame_size;
    auto iter = holder->sequences.find(temp.raw);
    if (iter != holder->sequences.end()) {
        temp.sequence = iter->second;
        holder->sequences.erase(iter);
    } else {
        ALOGE("sequence number of %p not found, this should NEVER happen and is a bug", temp.raw);
    }
    ZN7android10AudioTrack13releaseBufferEPKNS0_6BufferE(holder->track, &temp);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_pauseAndWaitInternal(JNIEnv *, jobject, jlong ptr, jlong timeout) {
    auto holder = (track_holder*) ptr;
    if (holder->died)
        return true;
    std::chrono::milliseconds millis(timeout);
    return ZN7android10AudioTrack12pauseAndWaitERKNSt3__16chrono8durationIxNS1_5ratioILl1ELl1000EEEEE(holder->track, millis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getOriginalSampleRateInternal(JNIEnv *,
                                                                             jobject,
                                                                             jlong ptr) {
    auto holder = (track_holder*) ptr;
    return ZNK7android10AudioTrack21getOriginalSampleRateEv(holder->track);
}

extern "C"
JNIEXPORT jboolean JNICALL
	Java_org_nift4_gramophone_hificore_NativeTrack_hasStartedInternal(JNIEnv *, jobject,
                                                                  jlong ptr) {
	auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack10hasStartedEv(holder->track);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setSelectedDeviceInternal(JNIEnv*, jobject,
                                                                         jlong ptr, jint id) {
	auto holder = (track_holder*) ptr;
	if (holder->died)
		return -32; // DEAD_OBJECT
	return ZN7android10AudioTrack15setOutputDeviceEi(holder->track, id);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getSelectedDeviceInternal(JNIEnv *, jobject,
                                                                         jlong ptr) {
	auto holder = (track_holder*) ptr;
    return ZN7android10AudioTrack15getOutputDeviceEv(holder->track);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getRoutedDevicesInternal(JNIEnv *env, jobject,
                                                                        jlong ptr) {
	auto holder = (track_holder*) ptr;
	if (android_get_device_api_level() >= 34) {
		std::vector<int32_t> deviceIds = ZN7android10AudioTrack18getRoutedDeviceIdsEv(holder->track);
		jintArray deviceIdsJni;
		deviceIdsJni = env->NewIntArray((int32_t)deviceIds.size());
		if (deviceIdsJni == nullptr) {
			ALOGE("Out of memory, dropping getRoutedDevices");
			env->ExceptionClear();
			return nullptr;
		}
		env->SetIntArrayRegion(deviceIdsJni, 0, (int32_t)deviceIds.size(), &deviceIds[0]);
		return deviceIdsJni;
	} else {
		jintArray deviceIdsJni = env->NewIntArray(1);
		if (deviceIdsJni == nullptr) {
			ALOGE("Out of memory, dropping getRoutedDevices");
			env->ExceptionClear();
			return nullptr;
		}
		int id = ZN7android10AudioTrack17getRoutedDeviceIdEv(holder->track);
		env->SetIntArrayRegion(deviceIdsJni, 0, 1, &id);
		return deviceIdsJni;
	}
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getBufferDurationInUsInternal(JNIEnv *,
                                                                             jobject,
                                                                             jlong ptr) {
	auto holder = (track_holder*) ptr;
	int64_t out;
	// as long as speed >= 0 and sample rate >= 0, out will be >= 0, hence we can discard sign
    int32_t ret = ZN7android10AudioTrack21getBufferDurationInUsEPl(holder->track, &out);
	if (ret != 0) {
		if (ret > 0) {
			ALOGE("getBufferDurationInUs returned positive error code %d, this should never happen", ret);
			return -1;
		}
		return ret;
	}
	return out;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getPlaybackRateInternal(JNIEnv *env, jobject,
                                                                       jlong ptr,
                                                                       jfloatArray speed_pitch) {
	auto holder = (track_holder*) ptr;
	audio_playback_rate rate = ZNK7android10AudioTrack15getPlaybackRateEv(holder->track);
	env->SetFloatArrayRegion(speed_pitch, 0, 2, &rate.mSpeed);
	return (jlong)rate.mStretchMode << 32 | rate.mFallbackMode;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_setPlaybackRateInternal(JNIEnv *, jobject,
                                                                       jlong ptr, jfloat speed,
                                                                       jfloat pitch,
                                                                       jint stretch_mode,
                                                                       jint fallback) {
	audio_playback_rate rate = {
			.mSpeed = speed,
			.mPitch = pitch,
			.mStretchMode = stretch_mode,
			.mFallbackMode = fallback,
	};
	auto holder = (track_holder*) ptr;
	return ZN7android10AudioTrack15setPlaybackRateERKNS_17AudioPlaybackRateE(holder->track, rate);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_pendingDurationInternal(JNIEnv *, jobject,
                                                                       jlong ptr, jint location) {
	int32_t out;
	auto holder = (track_holder*) ptr;
	int ret = ZN7android10AudioTrack15pendingDurationEPiNS_17ExtendedTimestamp8LocationE(holder->track, &out, location);
	return (jlong)out << 32 | ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_getTimestamp2Internal(JNIEnv *env, jobject,
                                                                     jlong ptr,
																	 jlongArray mPosition,
																	 jlongArray mTimeNs,
																	 jlongArray mTimebaseOffset,
																	 jlongArray mFlushed) {
	ExtendedTimestamp ts;
	auto holder = (track_holder*) ptr;
	int ret = ZN7android10AudioTrack12getTimestampEPNS_17ExtendedTimestampE(holder->track, &ts);
	env->SetLongArrayRegion(mPosition, 0, sizeof(ts.mPosition) / sizeof(jlong), (jlong*)&ts.mPosition);
	env->SetLongArrayRegion(mTimeNs, 0, sizeof(ts.mTimeNs) / sizeof(jlong), (jlong*)&ts.mTimeNs);
	env->SetLongArrayRegion(mTimebaseOffset, 0, sizeof(ts.mTimebaseOffset) / sizeof(jlong), (jlong*)&ts.mTimebaseOffset);
	env->SetLongArrayRegion(mFlushed, 0, 1, (jlong*)&ts.mFlushed);
	return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_nift4_gramophone_hificore_NativeTrack_00024Companion_getMinFrameCountInternal(JNIEnv *,
                                                                                       jobject,
                                                                                       jint stream_type,
                                                                                       jint sample_rate_in_hz) {
	return ZN7android10AudioTrack16getMinFrameCountEPm19audio_stream_type_tj(stream_type, sample_rate_in_hz);
}