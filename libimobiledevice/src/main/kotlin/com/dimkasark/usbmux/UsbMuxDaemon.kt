package com.dimkasark.usbmux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UsbMuxDaemon(
  private val configPath: String,
  private val socketPath: String,
  private val descriptor: Int
) {
  private val stateFlow = MutableStateFlow(DaemonState.NotStarted)

  //private val job: Job
  init {
    GlobalScope.launch(Dispatchers.IO + NonCancellable) {
      val serviceId = prepareDaemon(descriptor, configPath, socketPath)

      stateFlow.value = DaemonState.Started

      startDaemon(serviceId)

      freeDaemonResources()

      stateFlow.value = DaemonState.Terminated
    }
  }

  suspend fun waitForStart() {
    stateFlow.first { it == DaemonState.Started }
    //Wait for daemon really starts
    delay(300)
  }

  suspend fun stop() {
    stopDaemon(socketPath)
    stateFlow.first { it == DaemonState.Terminated }
  }

  private external fun prepareDaemon(usbDescriptor: Int, configPath: String, socketPath: String): Int

  private external fun startDaemon(descriptor: Int)

  private external fun stopDaemon(socketPath: String)

  private external fun freeDaemonResources()

  companion object {
    init {
      System.loadLibrary("usbmuxd-android")
    }
  }

  enum class DaemonState {
    NotStarted, Started, Terminated
  }
}