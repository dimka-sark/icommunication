package com.dimkasark.icommunication.external.mapper

import com.dimkasark.icommunication.external.data.DeviceDto
import com.dimkasark.icommunication.external.data.DeviceListDto
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun toRaw(list: DeviceListDto): ByteArray {
  return ByteArrayOutputStream().use { output ->
    ObjectOutputStream(output).use { stream ->
      val devices = list.devices
      stream.writeInt(devices.size)
      devices.forEach { device ->
        stream.writeUTF(device.id)
        stream.writeInt(device.deviceId)
        stream.writeInt(device.vendorId)
        stream.writeInt(device.productId)
        stream.writeInt(device.permission)
      }
    }
    output.toByteArray()
  }
}

fun fromRaw(raw: ByteArray): DeviceListDto {
  return ByteArrayInputStream(raw).use { input ->
    ObjectInputStream(input).use { stream ->
      DeviceListDto(
        devices = MutableList(stream.readInt()) {
          DeviceDto(
            id = stream.readUTF(),
            deviceId = stream.readInt(),
            vendorId = stream.readInt(),
            productId = stream.readInt(),
            permission = stream.readInt()
          )
        }
      )
    }
  }
}