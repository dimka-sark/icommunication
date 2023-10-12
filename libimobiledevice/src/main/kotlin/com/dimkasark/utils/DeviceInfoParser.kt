package com.dimkasark.utils

import com.dimkasark.models.DeviceInfo

fun extractDeviceInfo(xmlContent: String): DeviceInfo {
  val map = asParametersMap(xmlContent)
  return DeviceInfo(
    udid = map["UniqueDeviceID"] ?: "",
    deviceVersion = map["ProductVersion"] ?: ""
  )
}
