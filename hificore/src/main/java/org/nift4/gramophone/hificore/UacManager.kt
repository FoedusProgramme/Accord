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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService

class UacManager(private val context: Context) {
	companion object {
		private const val TAG = "Uac"
		private const val UAC_PERMISSION_ACTION =
			"org.nift4.gramophone.action.UAC_PERMISSION_GRANTED"
	}

	private val usbManager = context.getSystemService<UsbManager>()
	private val openDevices = mutableSetOf<Pair<UsbDevice, UsbDeviceConnection>>()
	private val attachDetachReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val isAttach = intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED
			if (isAttach || intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
				val device = IntentCompat.getParcelableExtra(intent,
					UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
				if (device == null) return
				if (isAttach)
					dispatchDeviceAddedCallbackIfNeeded(device)
				else
					dispatchDeviceDetachedCallbackIfNeeded(device)
			}
		}
	}

	init {
		context.registerReceiver(attachDetachReceiver, IntentFilter().apply {
			addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
			addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		})
	}

	private fun dispatchDeviceAddedCallbackIfNeeded(device: UsbDevice) {
		if (!isDeviceAudioEligible(device, true))
			return
		// TODO: do something.
	}

	private fun dispatchDeviceDetachedCallbackIfNeeded(device: UsbDevice) {
		if (!isDeviceAudioEligible(device, false))
			return
		// TODO: do something.
	}

	fun enumerateSoundcards() {
		/*eligibleDevices.forEach {
			if (!usbManager.hasPermission(it)) {
				val i = Intent(UAC_PERMISSION_ACTION)
				i.setPackage(context.packageName)
				val pi = PendingIntentCompat.getBroadcast(context, 0x4ac2, i,
					PendingIntent.FLAG_ONE_SHOT, false)
				usbManager.requestPermission(it, pi)
			}
			val deviceHandle = usbManager.openDevice(it)
			if (deviceHandle == null) {
				Log.e(TAG, "failed to open $it")
				return@forEach
			}

		}*/
	}

	private fun isDeviceAudioEligible(device: UsbDevice, allowLog: Boolean): Boolean {
		// we don't care about device class. as long as we have audio function, we can use it.
		val supportedConfigurations = mutableSetOf<Int>()
		for (configurationIndex in 0..<device.configurationCount) {
			val configuration = device.getConfiguration(configurationIndex)
			var hasAudioControl = false
			var hasAudioStreaming = false
			var hasMidiStreaming = false
			for (interfaceIndex in 0..<configuration.interfaceCount) {
				val iface = configuration.getInterface(interfaceIndex)
				if (iface.interfaceClass != UsbConstants.USB_CLASS_AUDIO) {
					continue
				}
				if (iface.interfaceProtocol != 0x20 /* IP_VERSION_02_00 */) {
					if (allowLog)
						Log.e(TAG, "$device/$configuration has unsupported interface version $iface")
					continue
				}
				when (iface.interfaceSubclass) {
					0x01 /* AUDIOCONTROL */ -> hasAudioControl = true
					0x02 /* AUDIOSTREAMING */ -> hasAudioStreaming = true
					0x03 /* MIDISTREAMING */ -> hasMidiStreaming = true
					else -> {
						if (allowLog)
							Log.e(TAG, "$device/$configuration has unsupported interface subclass $iface")
					}
				}
			}
			if (!hasAudioControl) {
				continue
			}
			if (!hasAudioStreaming) {
				if (allowLog) {
					if (hasMidiStreaming) {
						Log.i(TAG, "$device/$configuration has no audio streaming " +
								"class, is MIDI device")
					} else {
						Log.w(TAG, "$device/$configuration has no streaming class")
					}
				}
				continue
			}
			supportedConfigurations.add(configurationIndex) // TODO: exit early here.
		}
		if (supportedConfigurations.isEmpty()) {
			return false
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
			device.deviceClass == UsbConstants.USB_CLASS_VIDEO
		) {
			Log.w(TAG, "eligible audio device is UVC device, missing camera " +
						"permission to access, hence ignoring")
			return false
		}
		// TODO: check that there is at least 1 audio sink streaming interface declared.
		return true
	}

	private external fun getDetailsForUsbDevice(fd: Int)
}