package org.nift4.audiofxfwd;

import android.media.audiofx.AudioEffect;

public interface OnParameterChangeListener {
	/**
	 * Called on the listener to notify it that a parameter value has changed.
	 * @param effect the effect on which the interface is registered.
	 * @param status status of the set parameter operation.
	 * @param param ID of the modified parameter.
	 * @param value the new parameter value.
	 */
	void onParameterChange(AudioEffect effect, int status, byte[] param,
	                       byte[] value);
}