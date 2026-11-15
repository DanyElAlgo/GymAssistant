package com.example.gym_assistant

import android.content.Context
import android.util.Log
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

/** Kotlin -> Python bridge. Sends landmarks to backend.py (Chaquopy) for exercise analysis. */
object PoseAnalyzer {
    private const val TAG = "GymPose"

    @Volatile
    private var module: PyObject? = null

    @Synchronized
    fun init(context: Context) {
        if (module != null) return
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
        module = Python.getInstance().getModule("backend")
        Log.d(TAG, "Python backend loaded")
    }

    fun reset() {
        try {
            module?.callAttr("reset")
        } catch (exception: PyException) {
            Log.e(TAG, "Python reset failed", exception)
        }
    }

    /** Returns the analysis JSON object, or an object with "error" if Python failed. */
    fun analyze(landmarksJson: String): JSONObject {
        val backend = module ?: return JSONObject().put("error", "Python backend not initialized")
        return try {
            JSONObject(backend.callAttr("analyze", landmarksJson).toString())
        } catch (exception: PyException) {
            Log.e(TAG, "Python analysis failed", exception)
            JSONObject().put("error", exception.message ?: "Python error")
        }
    }
}
