package com.example.arsignal

import android.app.Activity
import android.content.pm.PackageManager
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableException

class ARSessionManager(private val activity: Activity) {
    var session: Session? = null
        private set

    fun isCameraAvailable(): Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    fun start(): Boolean {
        if (session == null) {
            try {
                session = Session(activity)
            } catch (_: UnavailableException) {
                return false
            }
        }

        val config = Config(session).apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            focusMode = Config.FocusMode.AUTO
        }

        session?.configure(config)
        session?.resume()
        return true
    }

    fun update(): Frame? = try {
        session?.update()
    } catch (_: Exception) {
        null
    }

    fun findPlaneHit(frame: Frame, x: Float, y: Float) =
        frame.hitTest(x, y).firstOrNull { hit ->
            val plane = hit.trackable as? Plane
            plane != null &&
                plane.trackingState == TrackingState.TRACKING &&
                plane.isPoseInPolygon(hit.hitPose)
        }

    fun pause() { session?.pause() }

    fun close() {
        session?.close()
        session = null
    }
}
