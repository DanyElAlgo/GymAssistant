package com.example.gym_assistant

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channel = "com.example.gym_assistant/pose"
    private val landmarkChannel = "com.example.gym_assistant/landmarks"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channel).setMethodCallHandler { call, result ->
            when (call.method) {
                "openCamera" -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    result.success(null)
                }
                "getLastResult" -> result.success(PoseEventStream.lastPayload)
                else -> result.notImplemented()
            }
        }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, landmarkChannel)
            .setStreamHandler(PoseEventStream)
    }
}
