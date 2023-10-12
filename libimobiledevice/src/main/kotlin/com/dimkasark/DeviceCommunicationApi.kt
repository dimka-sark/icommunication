package com.dimkasark

import android.hardware.usb.UsbDeviceConnection
import com.dimkasark.usbmux.UsbMuxDaemon
import java.io.File

class DeviceCommunicationApi(
  private val configPath: String,
  private val socketPath: String,
  private val connection: UsbDeviceConnection
) {
  suspend fun <T> runMobileDeviceAction(action: suspend () -> T): T {
    val configDirFile = File(configPath)

    configDirFile.apply {
      deleteRecursively()
      mkdirs()
    }

    val daemon = UsbMuxDaemon(configPath, socketPath, connection.fileDescriptor)
    daemon.waitForStart()
    val result = action()
    daemon.stop()
    connection.close()
    return result
  }
}