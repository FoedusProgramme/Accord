package android.media.audiofx;

@SuppressWarnings("unused")
public class AudioEffect {
	public AudioEffect() {
		throw new UnsupportedOperationException("Stub!");
	}

	public void setParameterListener(OnParameterChangeListener listener) {
		throw new UnsupportedOperationException("Stub!");
	}

	public interface OnParameterChangeListener {
		void onParameterChange(AudioEffect effect, int status, byte[] param,
		                       byte[] value);
	}
}
