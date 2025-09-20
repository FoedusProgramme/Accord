package org.nift4.alacdecoder;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.decoder.DecoderException;

@OptIn(markerClass = UnstableApi.class)
public class AlacDecoderException extends DecoderException {

    public AlacDecoderException(String message) {
        super(message);
    }

    public AlacDecoderException(@Nullable Throwable cause) {
        super(cause);
    }
}
