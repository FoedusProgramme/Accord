package org.nift4.alacdecoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.decoder.SimpleDecoder;
import androidx.media3.decoder.SimpleDecoderOutputBuffer;

import com.beatofthedrum.alacdecoder.AlacDecodeUtils;
import com.beatofthedrum.alacdecoder.AlacFile;

import java.nio.ByteBuffer;

@OptIn(markerClass = UnstableApi.class)
public class AlacDecoder extends SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, AlacDecoderException> {
    private static final int ALAC_MAX_PACKET_SIZE = 16384;

    private final Format inputFormat;
    private final AlacFile file;

    public AlacDecoder(Format inputFormat, int numInputBuffers, int numOutputBuffers) throws AlacDecoderException {
        super(new DecoderInputBuffer[numInputBuffers], new SimpleDecoderOutputBuffer[numOutputBuffers]);
        this.inputFormat = inputFormat;
        int bitDepth = inputFormat.initializationData.get(0)[5];
        this.file = AlacDecodeUtils.create_alac(bitDepth, inputFormat.channelCount);
        this.file.channel_map = getChannelMapping();
        AlacDecodeUtils.alac_set_info(file, ByteBuffer.wrap(inputFormat.initializationData.get(0)));
        int mp4MaxSize = inputFormat.maxInputSize != Format.NO_VALUE ? inputFormat.maxInputSize
                : ALAC_MAX_PACKET_SIZE;
        setInitialInputBufferSize(Math.min(file.max_frame_bytes, mp4MaxSize));
    }

    public Format getInputFormat() {
        return inputFormat;
    }

    @NonNull
    @Override
    public String getName() {
        return "AlacDecoder";
    }

    @NonNull
    @Override
    protected DecoderInputBuffer createInputBuffer() {
        return new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
    }

    @NonNull
    @Override
    protected SimpleDecoderOutputBuffer createOutputBuffer() {
        return new SimpleDecoderOutputBuffer(this::releaseOutputBuffer);
    }

    @NonNull
    @Override
    protected AlacDecoderException createUnexpectedDecodeException(@NonNull Throwable error) {
        return new AlacDecoderException(error);
    }

    @Nullable
    @Override
    protected AlacDecoderException decode(@NonNull DecoderInputBuffer inputBuffer,
                                          @NonNull SimpleDecoderOutputBuffer outputBuffer,
                                          boolean reset) {
        if (!Util.castNonNull(inputBuffer.data).hasArray())
            return new AlacDecoderException("input has no array");
        if (inputBuffer.hasSupplementalData())
            return new AlacDecoderException("input has extra data, why?");
        try {
            int limit = AlacDecodeUtils.decode_frame(file, inputBuffer, outputBuffer);
            Util.castNonNull(outputBuffer.data).position(0);
            outputBuffer.data.limit(limit);
            return null;
        } catch (AlacDecoderException e) {
            return e;
        } finally {
            file.input_buffer = null;
        }
    }

    private int[] getChannelMapping() {
        return switch (file.numchannels) {
            case 1 -> new int[] { 0 };
            case 2 -> new int[] { 0, 1 };
            case 3 -> new int[] { 2, 0, 1 };
            case 4 -> new int[] { 2, 0, 1, 3 }; // must be CHANNEL_OUT_QUAD_SURROUND (non-canonical)!
            case 5 -> new int[] { 2, 0, 1, 3, 4 };
            case 6 -> new int[] { 2, 0, 1, 4, 5, 3 };
            case 7 -> new int[] { 2, 0, 1, 4, 5, 6, 3 };
            case 8 -> new int[] { 2, 6, 7, 0, 1, 4, 5, 3 };
            default -> throw new UnsupportedOperationException("invalid channel mask " + file.numchannels);
        };
    }
}
