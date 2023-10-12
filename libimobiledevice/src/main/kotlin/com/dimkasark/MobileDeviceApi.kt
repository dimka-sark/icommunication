package com.dimkasark

import android.util.Log
import androidx.annotation.Keep
import com.dimkasark.models.DeviceInfo
import com.dimkasark.utils.extractDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class MobileDeviceApi {
  @Volatile
  private var installCallback: ((Long, Long) -> Unit)? = null

  fun obtainDeviceInfo(tmpFilesDir: File): DeviceInfo {
    val path = File.createTempFile("result", ".json", tmpFilesDir)
    phoneDetails(path.absolutePath)
    val content = path.readText()
    path.delete()
    return extractDeviceInfo(content)
  }

  suspend fun installNewApp(path: String, progressCallback: (Long, Long) -> Unit): Int {
    return withContext(Dispatchers.IO) {
      this@MobileDeviceApi.installCallback = progressCallback
      val result = installApp(path)
      this@MobileDeviceApi.installCallback = null
      result
    }
  }

  suspend fun obtainPermission(): Int {
    while (true) {
      when (val permissionResult = phoneConnectionPermission()) {
        0 -> {
          Log.d("LIB_PERMISSION", "Permission Granted ")
          return 0
        }

        -17 -> {
          // user block phone, wait and recheck
          Log.d("LIB_PERMISSION", "Phone Blocked")
          delay(500L)
        }

        -19 -> {
          //user show alert for request
          Log.d("LIB_PERMISSION", "Permission Alert Shown")
          delay(2000L)
        }

        -18 -> {
          //user force denied request
          Log.d("LIB_PERMISSION", "Permission Not Granted ")
          return permissionResult
        }

        -100 -> {
          //user force denied request
          Log.d("LIB_PERMISSION", "Device mismatch ")
          return permissionResult
        }

        else -> {
          Log.d("LIB_PERMISSION", "Unknown error $permissionResult")
          return permissionResult
        }
      }
    }
  }

  external fun setSocketPath(socketPath: String)

  private external fun phoneConnectionPermission(): Int

  private external fun phoneDetails(outputFile: String)

  private external fun installApp(path: String): Int

  @Keep
  protected fun installAppSend(sendCount: Long, allCount: Long) {
    installCallback?.invoke(sendCount, allCount)
  }

  companion object {
    init {
      System.loadLibrary("icommunication")
    }
  }
}