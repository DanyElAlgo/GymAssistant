package com.example.gym_assistant

import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.EventChannel

object PoseEventStream : EventChannel.StreamHandler {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sink: EventChannel.EventSink? = null

    /** Latest payload, so Flutter can fetch it when it resumes after the camera closes. */
    @Volatile
    var lastPayload: String? = null
        private set

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        sink = events
        lastPayload?.let { events?.success(it) }
    }

    override fun onCancel(arguments: Any?) {
        sink = null
    }

    fun emit(payload: String) {
        lastPayload = payload
        mainHandler.post { sink?.success(payload) }
    }
}
