package com.example.arsignal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position

class MainActivity : AppCompatActivity() {

    private lateinit var reader: SignalReader
    private lateinit var speedMonitor: NetworkSpeedMonitor

    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var modeSwitchButton: Button
    private lateinit var refreshButton: Button

    private lateinit var arContainer: FrameLayout
    private var sceneView: ArSceneView? = null
    private lateinit var arOverlayCard: LinearLayout
    private lateinit var signalText: TextView
    private lateinit var networkText: TextView
    private lateinit var qualityText: TextView
    private lateinit var speedText: TextView

    private lateinit var container2D: LinearLayout
    private lateinit var signalText2D: TextView
    private lateinit var networkText2D: TextView
    private lateinit var qualityText2D: TextView
    private lateinit var speedText2D: TextView

    private var is3DMode = false
    private var currentArNode: ArNode? = null
    private val handler = Handler(Looper.getMainLooper())

    private val requiredPermissions: Array<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return permissions.toTypedArray()
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updatePermissionState() }

    private val poller = object : Runnable {
        override fun run() {
            if (hasAllPermissions()) {
                updateSignal()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        reader = SignalReader(this)
        speedMonitor = NetworkSpeedMonitor()

        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        modeSwitchButton = findViewById(R.id.modeSwitchButton)
        refreshButton = findViewById(R.id.refreshButton)

        arContainer = findViewById(R.id.arContainer)
        arOverlayCard = findViewById(R.id.arOverlayCard)
        signalText = findViewById(R.id.signalText)
        networkText = findViewById(R.id.networkText)
        qualityText = findViewById(R.id.qualityText)
        speedText = findViewById(R.id.speedText)

        container2D = findViewById(R.id.container2D)
        signalText2D = findViewById(R.id.signalText2D)
        networkText2D = findViewById(R.id.networkText2D)
        qualityText2D = findViewById(R.id.qualityText2D)
        speedText2D = findViewById(R.id.speedText2D)

        permissionButton.setOnClickListener { requestPermissionsIfNeeded() }

        refreshButton.setOnClickListener {
            if (hasAllPermissions()) {
                updateSignal()
                Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissionsIfNeeded()
            }
        }

        modeSwitchButton.setOnClickListener {
            toggleMode(!is3DMode)
        }

        toggleMode(false)

        if (hasAllPermissions()) {
            updatePermissionState()
            startSpeedService()
        } else {
            requestPermissionsIfNeeded()
        }
    }

    private fun startSpeedService() {
        val serviceIntent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            R.id.action_about -> {
                showAboutReadmeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMessage("Configuration Details:\n\n• Notification Panel Tracker: Enabled\n• Refresh Interval: 1 Second\n• Speed Unit: Automatic (Kbps/Mbps)\n• AR Engine: Google ARCore")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Send Feedback") { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:skgdrive932@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback: AR Cellular Signal Visualizer")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showAboutReadmeDialog() {
        val aboutContent = """
            AR Cellular Signal Visualizer
            Developed by: SK Kaushal
            
            📧 Email: skgdrive932@gmail.com
            📱 Contact: +919779371866
            
            Key Features:
            • Real-time Cellular Signal & Speed Tracking
            • Live Notification Panel Speed Monitor
            • Dual Mode: 2D Dashboard & 3D AR
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About App")
            .setMessage(aboutContent)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onStart() {
        super.onStart()
        speedMonitor.start()
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

            if (!availability.isSupported || availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                showCustomArRequiredDialog()
            } else {
                val installStatus = ArCoreApk.getInstance().requestInstall(this, false)
                if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    showCustomArRequiredDialog()
                }
            }
        } catch (e: Exception) {
            showCustomArRequiredDialog()
        }
    }

    private fun showCustomArRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("AR Core Required")
            .setMessage("3D AR mode use karne ke liye Google Play Services for AR required hai.")
            .setPositiveButton("Install / Enable") { _, _ ->
                try {
                    ArCoreApk.getInstance().requestInstall(this, true)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to launch AR installer", Toast.LENGTH_SHORT).show()
                    toggleMode(false)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                toggleMode(false)
            }
            .setCancelable(false)
            .show()
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
            startSpeedService()
            updateSignal()
        } else {
            statusText.text = "Permissions required"
            permissionButton.visibility = View.VISIBLE
        }
    }

    private fun updateSignal() {
        if (!::reader.isInitialized) return

        val info = reader.read()
        val speedInfo = speedMonitor.getDownloadSpeed()

        val speedDisplay = "Speed: ${speedInfo.formattedSpeed}"

        if (info.dbm == null) {
            val unavail = "Signal unavailable"
            signalText.text = unavail
            signalText2D.text = unavail
            networkText.text = "Network: ${info.networkType}"
            networkText2D.text = "Network: ${info.networkType}"
            qualityText.text = "Quality: —"
            qualityText2D.text = "Quality: —"
            speedText.text = speedDisplay
            speedText2D.text = speedDisplay
            return
        }

        val dbmStr = "${info.dbm} dBm"
        val netStr = "Network: ${info.networkType}"
        val qualStr = "Quality: ${quality(info.dbm)}"

        signalText2D.text = dbmStr
        networkText2D.text = netStr
        qualityText2D.text = qualStr
        speedText2D.text = speedDisplay

        signalText.text = dbmStr
        networkText.text = netStr
        qualityText.text = qualStr
        speedText.text = speedDisplay

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
