package com.example.arsignal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position

class MainActivity : AppCompatActivity() {

    private lateinit var reader: SignalReader
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var modeSwitchButton: Button

    // AR Dynamic Container
    private lateinit var arContainer: FrameLayout
    private var sceneView: ArSceneView? = null
    private lateinit var arOverlayCard: LinearLayout
    private lateinit var signalText: TextView
    private lateinit var networkText: TextView
    private lateinit var qualityText: TextView

    // 2D View Elements
    private lateinit var container2D: LinearLayout
    private lateinit var signalText2D: TextView
    private lateinit var networkText2D: TextView
    private lateinit var qualityText2D: TextView

    private var is3DMode = false
    private var currentArNode: ArNode? = null
    private var userRequestedInstall = true
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

        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        modeSwitchButton = findViewById(R.id.modeSwitchButton)

        arContainer = findViewById(R.id.arContainer)
        arOverlayCard = findViewById(R.id.arOverlayCard)
        signalText = findViewById(R.id.signalText)
        networkText = findViewById(R.id.networkText)
        qualityText = findViewById(R.id.qualityText)

        container2D = findViewById(R.id.container2D)
        signalText2D = findViewById(R.id.signalText2D)
        networkText2D = findViewById(R.id.networkText2D)
        qualityText2D = findViewById(R.id.qualityText2D)

        permissionButton.setOnClickListener { requestPermissionsIfNeeded() }

        modeSwitchButton.setOnClickListener {
            toggleMode(!is3DMode)
        }

        // Direct 2D setup on Start
        toggleMode(false)

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

    private fun toggleMode(enable3D: Boolean) {
        is3DMode = enable3D

        if (is3DMode) {
            // Switch to 3D AR Mode
            container2D.visibility = View.GONE
            arContainer.visibility = View.VISIBLE
            arOverlayCard.visibility = View.VISIBLE
            modeSwitchButton.text = "SWITCH TO 2D MODE"

            // Dynamic AR View Creation
            if (sceneView == null) {
                sceneView = ArSceneView(this)
                arContainer.addView(sceneView)
            }

            // Custom Dialog check trigger
            checkAndPromptARCore()
        } else {
            // Switch to 2D Mode
            arContainer.visibility = View.GONE
            arOverlayCard.visibility = View.GONE
            container2D.visibility = View.VISIBLE
            modeSwitchButton.text = "SWITCH TO 3D AR MODE"

            // Destroy AR View to save resources
            sceneView?.let {
                arContainer.removeView(it)
                sceneView = null
            }
        }

        if (hasAllPermissions()) {
            updatePermissionState()
        }
    }

    private fun checkAndPromptARCore() {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)

            if (availability.isTransient) {
                // Agar status check hone mein thoda time lag raha ho toh retry
                handler.postDelayed({ checkAndPromptARCore() }, 200)
                return
            }

            if (!availability.isSupported) {
                Toast.makeText(this, "ARCore is not supported on this device", Toast.LENGTH_SHORT).show()
                toggleMode(false)
                return
            }

            // Check if ARCore is installed or needs installation/update
            val installStatus = ArCoreApk.getInstance().requestInstall(this, false)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                
                // Custom Alert Dialog with CONTINUE and CANCEL buttons
                AlertDialog.Builder(this)
                    .setTitle("Google Play Services for AR")
                    .setMessage("This application requires the latest version of Google Play Services for AR to run 3D mode.")
                    .setPositiveButton("CONTINUE") { _, _ ->
                        try {
                            ArCoreApk.getInstance().requestInstall(this, true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .setNegativeButton("CANCEL") { dialog, _ ->
                        dialog.dismiss()
                        toggleMode(false) // Safe fallback to 2D Mode
                        Toast.makeText(this, "Switched back to 2D Mode", Toast.LENGTH_SHORT).show()
                    }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: UnavailableException) {
            Toast.makeText(this, "ARCore unavailable on this device", Toast.LENGTH_SHORT).show()
            toggleMode(false)
        }
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
            statusText.text = if (is3DMode) "3D AR Mode Active" else "2D Dashboard Mode Active"
            permissionButton.visibility = View.GONE
            updateSignal()
        } else {
            statusText.text = "Permissions required"
            permissionButton.visibility = View.VISIBLE
        }
    }

    private fun updateSignal() {
        val info = reader.read()

        if (info.dbm == null) {
            val unavail = "Signal unavailable"
            signalText.text = unavail
            signalText2D.text = unavail
            networkText.text = "Network: ${info.networkType}"
            networkText2D.text = "Network: ${info.networkType}"
            qualityText.text = "Quality: —"
            qualityText2D.text = "Quality: —"
            return
        }

        val dbmStr = "${info.dbm} dBm"
        val netStr = "Network: ${info.networkType}"
        val qualStr = "Quality: ${quality(info.dbm)}"

        signalText2D.text = dbmStr
        networkText2D.text = netStr
        qualityText2D.text = qualStr

        signalText.text = dbmStr
        networkText.text = netStr
        qualityText.text = qualStr

        if (is3DMode) {
            add3DSignalNode(info.dbm)
        }
    }

    private fun add3DSignalNode(dbm: Int) {
        val activeView = sceneView ?: return
        currentArNode?.let { activeView.removeChild(it) }

        val arNode = ArNode(activeView.engine).apply {
            position = Position(x = 0.0f, y = 0.0f, z = -1.0f)
        }

        currentArNode = arNode
        activeView.addChild(arNode)
    }

    private fun quality(dbm: Int): String = when {
        dbm >= -60 -> "Excellent"
        dbm >= -75 -> "Good"
        dbm >= -90 -> "Fair"
        dbm >= -105 -> "Poor"
        else -> "Very Poor"
    }
}
