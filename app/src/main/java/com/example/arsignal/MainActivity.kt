package com.example.arsignal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
        
        // Auto-detect layout ID using packageName to avoid 'Unresolved reference: R'
        val layoutId = resources.getIdentifier("activity_main", "layout", packageName)
        setContentView(layoutId)

        // Setup Toolbar dynamically
        val toolbarId = resources.getIdentifier("toolbar", "id", packageName)
        val toolbar: Toolbar = findViewById(toolbarId)
        setSupportActionBar(toolbar)

        reader = SignalReader(this)

        statusText = findViewById(resources.getIdentifier("statusText", "id", packageName))
        permissionButton = findViewById(resources.getIdentifier("permissionButton", "id", packageName))
        modeSwitchButton = findViewById(resources.getIdentifier("modeSwitchButton", "id", packageName))

        arContainer = findViewById(resources.getIdentifier("arContainer", "id", packageName))
        arOverlayCard = findViewById(resources.getIdentifier("arOverlayCard", "id", packageName))
        signalText = findViewById(resources.getIdentifier("signalText", "id", packageName))
        networkText = findViewById(resources.getIdentifier("networkText", "id", packageName))
        qualityText = findViewById(resources.getIdentifier("qualityText", "id", packageName))

        container2D = findViewById(resources.getIdentifier("container2D", "id", packageName))
        signalText2D = findViewById(resources.getIdentifier("signalText2D", "id", packageName))
        networkText2D = findViewById(resources.getIdentifier("networkText2D", "id", packageName))
        qualityText2D = findViewById(resources.getIdentifier("qualityText2D", "id", packageName))

        permissionButton.setOnClickListener { requestPermissionsIfNeeded() }

        modeSwitchButton.setOnClickListener {
            toggleMode(!is3DMode)
        }

        toggleMode(false)

        if (hasAllPermissions()) {
            updatePermissionState()
        } else {
            requestPermissionsIfNeeded()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuId = resources.getIdentifier("main_menu", "menu", packageName)
        if (menuId != 0) {
            menuInflater.inflate(menuId, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val settingsId = resources.getIdentifier("action_settings", "id", packageName)
        val aboutId = resources.getIdentifier("action_about", "id", packageName)

        return when (item.itemId) {
            settingsId -> {
                showSettingsDialog()
                true
            }
            aboutId -> {
                showAboutReadmeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMessage("• Refresh Interval: 3 Seconds\n• Auto Mode Fallback: Enabled\n• AR Engine: Google ARCore")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAboutReadmeDialog() {
        val readmeContent = """
            # AR Cellular Signal Visualizer
            
            Developed by: SK Kaushal
            
            Key Features:
            • Real-time Cellular Signal Tracking (dBm & Quality)
            • Dual Mode: 2D Performance Dashboard & 3D AR Camera Visualization
            • Battery Optimized Dynamic AR Loading
            • Multi-Network Support (4G/5G/LTE)
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About App (README)")
            .setMessage(readmeContent)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
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
            container2D.visibility = View.GONE
            arContainer.visibility = View.VISIBLE
            arOverlayCard.visibility = View.VISIBLE
            modeSwitchButton.text = "SWITCH TO 2D MODE"

            if (sceneView == null) {
                sceneView = ArSceneView(this)
                arContainer.addView(sceneView)
            }

            checkAndPromptARCore()
        } else {
            arContainer.visibility = View.GONE
            arOverlayCard.visibility = View.GONE
            container2D.visibility = View.VISIBLE
            modeSwitchButton.text = "SWITCH TO 3D AR MODE"

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
                handler.postDelayed({ checkAndPromptARCore() }, 200)
                return
            }

            if (!availability.isSupported) {
                Toast.makeText(this, "ARCore is not supported on this device", Toast.LENGTH_SHORT).show()
                toggleMode(false)
                return
            }

            val installStatus = ArCoreApk.getInstance().requestInstall(this, false)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
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
                        toggleMode(false)
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
