package com.dimkasark.icommunication

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.dimkasark.UsbCommandProcessor
import com.dimkasark.models.PermissionCheckResult
import com.dimkasark.icommunication.UsbCommunicationReceiver.Companion.USB_PERMISSION_REQUEST
import com.dimkasark.icommunication.external.data.API_VERSION
import com.dimkasark.icommunication.external.data.APP_PATH_KEY
import com.dimkasark.icommunication.external.data.DATA_KEY
import com.dimkasark.icommunication.external.data.DEVICE_GRAND_PERMISSION_COMMAND
import com.dimkasark.icommunication.external.data.DEVICE_INSTALL_COMMAND
import com.dimkasark.icommunication.external.data.DEVICE_INSTALL_RESPONSE_ERROR
import com.dimkasark.icommunication.external.data.DEVICE_INSTALL_RESPONSE_PROCESS
import com.dimkasark.icommunication.external.data.DEVICE_INSTALL_RESPONSE_SUCCESS
import com.dimkasark.icommunication.external.data.DEVICE_IPHONE_PERMISSION_COMMAND
import com.dimkasark.icommunication.external.data.DEVICE_LIST_CHANGE_COMMAND
import com.dimkasark.icommunication.external.data.DEVICE_VERSION_KEY
import com.dimkasark.icommunication.external.data.DeviceDto
import com.dimkasark.icommunication.external.data.DeviceListDto
import com.dimkasark.icommunication.external.data.LOADED_KEY
import com.dimkasark.icommunication.external.data.PERMISSION_KEY
import com.dimkasark.icommunication.external.data.PERMISSION_STATE_ALLOWED
import com.dimkasark.icommunication.external.data.PERMISSION_STATE_DENIED_BY_USER
import com.dimkasark.icommunication.external.data.PERMISSION_STATE_NEED_REQUEST
import com.dimkasark.icommunication.external.data.SERVICE_CONNECTION_COMMAND
import com.dimkasark.icommunication.external.data.SERVICE_CONNECTION_VERSION_MISS_MATCH
import com.dimkasark.icommunication.external.data.UDID_KEY
import com.dimkasark.icommunication.external.data.UPLOADED_KEY
import com.dimkasark.icommunication.external.mapper.toRaw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val UPLOAD_DEBOUNCE = 500L

class UsbCommunicationService : Service() {
  private var connectedApp: Messenger? = null
  private val messenger = Messenger(
    Handler(Looper.getMainLooper()) { msg ->
      handleMessage(msg)
      true
    }
  )
  private val receiver = UsbCommunicationReceiver(::connectedDevicesChanged)
  private val usbDeviceInfo = mutableListOf<DeviceDto>()
  private lateinit var scope: CoroutineScope

  override fun onBind(intent: Intent?): IBinder {
    return messenger.binder
  }

  override fun onCreate() {
    UsbCommandProcessor.init(filesDir)
    registerReceiver(
      receiver,
      IntentFilter().apply {
        addAction(UsbCommunicationReceiver.USB_PERMISSION)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
      }
    )
    refreshDeviceInfo(deviceId = 0, permissionGranted = false)
    scope = MainScope()
  }

  override fun onDestroy() {
    unregisterReceiver(receiver)
    scope.cancel()
  }

  private fun handleMessage(message: Message) {
    when (message.what) {
      SERVICE_CONNECTION_COMMAND -> {
        if (message.arg1 == API_VERSION) {
          connectedApp = message.replyTo
          sendDeviceStatusesForClient()
        } else {
          //Try to notify client for version miss match
          message.replyTo?.send(Message.obtain(null, SERVICE_CONNECTION_VERSION_MISS_MATCH))
        }
      }

      DEVICE_GRAND_PERMISSION_COMMAND -> requestDevicePermission(message.arg1)
      DEVICE_IPHONE_PERMISSION_COMMAND -> readIphonePermission(message.arg1)
      DEVICE_INSTALL_COMMAND -> installIphoneApp(message)
    }
  }

  private fun connectedDevicesChanged(deviceId: Int, permissionGranted: Boolean) {
    refreshDeviceInfo(deviceId = deviceId, permissionGranted = permissionGranted)
    sendDeviceStatusesForClient()
  }

  private fun refreshDeviceInfo(deviceId: Int, permissionGranted: Boolean) {
    val manager = getSystemService(UsbManager::class.java) ?: return

    //remove old devices
    val activeDevices = manager.deviceList.mapTo(HashSet()) { (_, device) -> device.deviceId }
    usbDeviceInfo.removeAll { it.deviceId !in activeDevices }

    manager.deviceList.forEach { (id, device) ->
      //add new devices
      val indexOfDevice = usbDeviceInfo.indexOfFirst { it.deviceId == device.deviceId }
      if (indexOfDevice < 0) {
        usbDeviceInfo.add(
          DeviceDto(
            id = id,
            deviceId = device.deviceId,
            vendorId = device.vendorId,
            productId = device.productId,
            permission = if (manager.hasPermission(device)) PERMISSION_STATE_ALLOWED else PERMISSION_STATE_NEED_REQUEST
          )
        )
      }
    }

    // handle device permission
    val indexOfDevice = usbDeviceInfo.indexOfFirst { it.deviceId == deviceId }
    if (indexOfDevice >= 0) {
      usbDeviceInfo[indexOfDevice] = usbDeviceInfo[indexOfDevice].copy(
        permission = if (!permissionGranted) PERMISSION_STATE_DENIED_BY_USER else PERMISSION_STATE_ALLOWED
      )
    }
  }

  private fun requestDevicePermission(deviceId: Int) {
    runWithDevice(deviceId = deviceId) { manager, device ->
      manager.requestPermission(
        device,
        PendingIntent.getBroadcast(
          this,
          USB_PERMISSION_REQUEST,
          Intent(UsbCommunicationReceiver.USB_PERMISSION),
          PendingIntent.FLAG_MUTABLE
        )
      )
    }
  }

  private fun readIphonePermission(deviceId: Int) {
    runWithDevice(deviceId = deviceId) { manager, device ->
      scope.launch {
        val result = UsbCommandProcessor.obtainPermission(manager, device)
        sendMessage(DEVICE_IPHONE_PERMISSION_COMMAND) {
          data = Bundle().apply {
            when (result) {
              is PermissionCheckResult.Success -> {
                putInt(PERMISSION_KEY, 0)
                putString(UDID_KEY, result.info.udid)
                putString(DEVICE_VERSION_KEY, result.info.deviceVersion)
              }

              is PermissionCheckResult.Error -> {
                putInt(PERMISSION_KEY, result.code)
              }
            }
          }
        }
      }
    }
  }

  private fun installIphoneApp(msg: Message) {
    val deviceId = msg.arg1

    val appUri = msg.data?.getParcelable<Uri>(APP_PATH_KEY)
    if (appUri == null) {
      sendInstallFailed()
      return
    }

    runWithDevice(deviceId = deviceId) { manager, device ->
      scope.launch {
        val tmpFile = try {
          withContext(Dispatchers.IO) {
            val file = File(filesDir, "temp_app.ipa")
            file.delete()
            // FIXME: try to migrate to contentResolver.openFileDescriptor
            contentResolver.openInputStream(appUri)?.use { input ->
              file.outputStream().use { output ->
                input.copyTo(output)
              }
            }
            file
          }
        } catch (ex: Exception) {
          sendInstallFailed()
          return@launch
        }

        val result = UsbCommandProcessor.installApp(
          manager,
          device,
          tmpFile.absolutePath,
          ProgressSender()::deviceIpaSend
        )

        withContext(Dispatchers.IO) {
          tmpFile.delete()
        }

        if (result == 0) {
          sendInstallSucceed()
        } else {
          sendInstallFailed(result)
        }
      }
    }
  }

  private fun sendInstallProgress(uploadedCount: Long, allCount: Long) = sendMessage(DEVICE_INSTALL_COMMAND) {
    arg1 = DEVICE_INSTALL_RESPONSE_PROCESS
    data = Bundle().apply {
      putLong(UPLOADED_KEY, uploadedCount)
      putLong(LOADED_KEY, allCount)
    }

  }

  private fun sendInstallSucceed() = sendMessage(DEVICE_INSTALL_COMMAND) {
    arg1 = DEVICE_INSTALL_RESPONSE_SUCCESS
  }

  private fun sendInstallFailed(errorCode: Int? = null) = sendMessage(DEVICE_INSTALL_COMMAND) {
    arg1 = DEVICE_INSTALL_RESPONSE_ERROR
    arg2 = errorCode ?: 0
  }

  private inline fun runWithDevice(deviceId: Int, action: (UsbManager, UsbDevice) -> Unit) {
    val manager = getSystemService(UsbManager::class.java) ?: return
    val device = manager.deviceList.values.find { it.deviceId == deviceId } ?: return
    action(manager, device)
  }

  private fun sendDeviceStatusesForClient() = sendMessage(DEVICE_LIST_CHANGE_COMMAND) {
    data = Bundle().apply {
      putByteArray(DATA_KEY, toRaw(DeviceListDto(devices = usbDeviceInfo.toList())))
    }
  }

  private inline fun sendMessage(messageId: Int, builder: Message.() -> Unit = {}) {
    val messenger = connectedApp ?: return
    messenger.send(Message.obtain(null, messageId).apply(builder))
  }

  inner class ProgressSender {
    private var lastReportedTime = System.currentTimeMillis()

    fun deviceIpaSend(sendCount: Long, allCount: Long) {
      val currentTime = System.currentTimeMillis()
      if (sendCount == allCount || (currentTime - lastReportedTime) > UPLOAD_DEBOUNCE) {
        sendInstallProgress(sendCount, allCount)
      }
    }
  }
}
