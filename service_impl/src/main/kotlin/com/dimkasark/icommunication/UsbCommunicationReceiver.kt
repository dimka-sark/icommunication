package com.dimkasark.icommunication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbCommunicationReceiver(private val deviceChangeCallback: (Int, Boolean) -> Unit) : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    intent ?: return

    when (intent.action) {
      USB_PERMISSION -> {
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

        // handle user not accepted local request
        deviceChangeCallback(device?.deviceId ?: 0, permissionGranted)
      }
      UsbManager.ACTION_USB_DEVICE_ATTACHED,
      UsbManager.ACTION_USB_DEVICE_DETACHED -> {
        deviceChangeCallback(0, false)
      }
    }
  }

  companion object {
    const val USB_PERMISSION = "com.dimkasark.icommunication.external.USB_PERMISSION"
    const val USB_PERMISSION_REQUEST = 101
  }
}