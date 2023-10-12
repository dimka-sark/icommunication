package com.dimkasark.icommunication.external.data

data class DeviceListDto(
  val devices: List<DeviceDto>
)

data class DeviceDto(
  val id: String,
  val deviceId: Int,
  val vendorId: Int,
  val productId: Int,
  val permission: Int
)


const val PERMISSION_STATE_ALLOWED = 0
const val PERMISSION_STATE_NEED_REQUEST = 1
const val PERMISSION_STATE_DENIED_BY_USER = 2

const val API_VERSION = 2

const val DATA_KEY = "DATA"

const val PERMISSION_KEY = "PERMISSION_KEY"
const val UDID_KEY = "UDID_KEY"
const val DEVICE_VERSION_KEY = "DEVICE_VERSION_KEY"
const val APP_PATH_KEY = "APP_PATH_KEY"

const val UPLOADED_KEY = "UPLOADED_KEY"
const val LOADED_KEY = "LOADED_KEY"

const val DEVICE_INSTALL_RESPONSE_SUCCESS = 0
const val DEVICE_INSTALL_RESPONSE_PROCESS = 1
const val DEVICE_INSTALL_RESPONSE_ERROR = -1

const val SERVICE_CONNECTION_VERSION_MISS_MATCH = -1
const val SERVICE_CONNECTION_COMMAND = 1
const val DEVICE_LIST_CHANGE_COMMAND = 2
const val DEVICE_GRAND_PERMISSION_COMMAND = 3
const val DEVICE_IPHONE_PERMISSION_COMMAND = 4
const val DEVICE_INSTALL_COMMAND = 5
