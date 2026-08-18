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

    private lateinit var arContainer: FrameLayout
    private var sceneView: ArSceneView? = null
    private lateinit var arOverlayCard: LinearLayout
    private lateinit var signalText: TextView
    private lateinit var networkText: TextView
    private lateinit var qualityText: TextView

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
        setContentView(R.layout.activity_main)

        // 1. Permissions Check at the very beginning
        if (!hasAllPermissions()) {
            requestPermissionsIfNeeded()
        }

        // 2. Initialize UI Components
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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

        // Initialize state
        toggleMode(false)
        updatePermissionState()
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
            .setMessage("• Refresh Interval: 3 Seconds\n• Auto Mode Fallback: Enabled\n• AR Engine: Google ARCore")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAboutReadmeDialog() {
        val readmeContent = """
            # AR Cellular Signal Visualizer
            Developed by: SK Kaushal
            Features: Signal Tracking, 2D/3D Mode, Multi-Network Support.
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("About App")
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
        updatePermissionState()
    }

    private fun checkAndPromptARCore() {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (availability.isTransient) {
                handler.postDelayed({ checkAndPromptARCore() }, 200)
                return
            }
            if (!availability.isSupported) {
                Toast.makeText(this, "ARCore not supported", Toast.LENGTH_SHORT).show()
                toggleMode(false)
                return
            }
            val installStatus = ArCoreApk.getInstance().requestInstall(this, false)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                AlertDialog.Builder(this)
                    .setTitle("Google Play Services for AR")
                    .setMessage("This app requires ARCore.")
                    .setPositiveButton("CONTINUE") { _, _ -> ArCoreApk.getInstance().requestInstall(this, true) }
                    .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss(); toggleMode(false) }
                    .show()
            }
        } catch (e: Exception) {
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
            statusText.text = if (is3DMode) "3D AR Active" else "2D Active"
            permissionButton.visibility = View.GONE
            updateSignal()
        } else {
            statusText.text = "Permissions Required"
            permissionButton.visibility = View.VISIBLE
        }
    }

    private fun updateSignal() {
        // Safe check
        if (!::reader.isInitialized) return 
        
        val info = reader.read()
        if (info.dbm == null) {
            signalText.text = "Signal unavailable"
            signalText2D.text = "Signal unavailable"
            return
        }

        val dbmStr = "${info.dbm} dBm"
        signalText2D.text = dbmStr
        signalText.text = dbmStr
        networkText.text = "Network: ${info.networkType}"
        networkText2D.text = "Network: ${info.networkType}"
        
        if (is3DMode) add3DSignalNode(info.dbm)
    }

    private fun add3DSignalNode(dbm: Int) {
        val activeView = sceneView ?: return
        currentArNode?.let { activeView.removeChild(it) }
        val arNode = ArNode(activeView.engine).apply { position = Position(x = 0.0f, y = 0.0f, z = -1.0f) }
        currentArNode = arNode
        activeView.addChild(arNode)
    }
}
