package com.example.arcellular

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position
import io.github.sceneview.utils.Color

class MainActivity : AppCompatActivity() {

    private lateinit var reader: SignalReader
    private lateinit var statusText: TextView
    private lateinit var signalText: TextView
    private lateinit var networkText: TextView
    private lateinit var qualityText: TextView
    private lateinit var permissionButton: Button
    private lateinit var sceneView: ArSceneView

    private var currentArNode: ArNode? = null
    private val handler = Handler(Looper.getMainLooper())

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updatePermissionState() }

    private val poller = object : Runnable {
        override fun run() {
            if (hasAllPermissions()) {
                updateSignal()
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        reader = SignalReader(this)
        sceneView = findViewById(R.id.sceneView)
        statusText = findViewById(R.id.statusText)
        signalText = findViewById(R.id.signalText)
        networkText = findViewById(R.id.networkText)
        qualityText = findViewById(R.id.qualityText)
        permissionButton = findViewById(R.id.permissionButton)

        permissionButton.setOnClickListener { requestPermissionsIfNeeded() }

        if (hasAllPermissions()) {
            updatePermissionState()
        } else {
            requestPermissionsIfNeeded()
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(poller)
    }

    override fun onStop() {
        handler.removeCallbacks(poller)
        super.onStop()
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissionsIfNeeded() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun updatePermissionState() {
        if (hasAllPermissions()) {
            statusText.text = "Permissions ready • Live AR visualizer"
            permissionButton.visibility = View.GONE
            updateSignal()
        } else {
            statusText.text = "Permissions required for AR Visualizer"
            permissionButton.visibility = View.VISIBLE
        }
    }

    private fun updateSignal() {
        val info = reader.read()
        if (info.dbm == null) {
            signalText.text = "Signal unavailable"
            networkText.text = "Network: ${info.networkType}"
            qualityText.text = "Quality: —"
            return
        }

        signalText.text = "${info.dbm} dBm"
        networkText.text = "Network: ${info.networkType}"
        qualityText.text = "Quality: ${quality(info.dbm)}"

        // AR Space mein 3D Node render/update karein
        add3DSignalNode(info.dbm)
    }

    private fun add3DSignalNode(dbm: Int) {
        // Purana Node remove karein taaki stack na bane
        currentArNode?.let { sceneView.removeChild(it) }

        // Standard ArNode create karke Camera se 1 meter aage place karein
        val arNode = ArNode(sceneView.engine).apply {
            position = Position(x = 0.0f, y = 0.0f, z = -1.0f)
        }

        currentArNode = arNode
        sceneView.addChild(arNode)
    }

    private fun quality(dbm: Int): String = when {
        dbm >= -60 -> "Excellent"
        dbm >= -75 -> "Good"
        dbm >= -90 -> "Fair"
        dbm >= -105 -> "Poor"
        else -> "Very Poor"
    }
}
