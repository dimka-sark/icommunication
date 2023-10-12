package com.dimkasark

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.dimkasark.models.PermissionCheckResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

object UsbCommandProcessor {
  private lateinit var filesDir: File
  private val mutex = Mutex()

  fun init(filesDir: File) {
    this.filesDir = filesDir
    MobileDeviceApi().setSocketPath(socketFile().absolutePath)
  }

  suspend fun obtainPermission(manager: UsbManager, device: UsbDevice): PermissionCheckResult {
    return execute(device, manager) {
      when (val permission = obtainPermission()) {
        0 -> PermissionCheckResult.Success(obtainDeviceInfo(filesDir))
        else -> PermissionCheckResult.Error(permission)
      }
    }
  }

  suspend fun installApp(
    manager: UsbManager,
    device: UsbDevice,
    path: String,
    progressCallback: (Long, Long) -> Unit
  ): Int {
    return execute(device, manager) {
      installNewApp(path, progressCallback)
    }
  }

  private suspend inline fun <T> execute(
    device: UsbDevice,
    manager: UsbManager,
    crossinline action: suspend MobileDeviceApi.() -> T
  ): T {
    return mutex.withLock {
      DeviceCommunicationApi(
        configPath = configFile().absolutePath,
        socketPath = socketFile().absolutePath,
        connection = manager.openDevice(device)
      ).runMobileDeviceAction {
        MobileDeviceApi().action()
      }
    }
  }

  private fun configFile(): File {
    return File(filesDir, "usb_lib")
  }

  private fun socketFile(): File {
    return File(configFile(), "usbmuxd")
  }
}