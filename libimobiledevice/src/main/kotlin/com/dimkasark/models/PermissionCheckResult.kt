package com.dimkasark.models

sealed interface PermissionCheckResult {
  class Error(val code: Int) : PermissionCheckResult
  class Success(val info: DeviceInfo) : PermissionCheckResult
}

class DeviceInfo(val udid: String, val deviceVersion: String)