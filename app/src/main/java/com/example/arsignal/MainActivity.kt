package com.example.arcellular

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var modeSwitchButton: Button
    private lateinit var refreshButton: Button // 1. Variable Declare kiya

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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        reader = SignalReader(this)

        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        modeSwitchButton = findViewById(R.id.modeSwitchButton)
        refreshButton = findViewById(R.id.refreshButton) // 2. Initialise kiya

        // 3. Listener set kiya
        refreshButton.setOnClickListener {
            if (hasAllPermissions()) {
                updateSignal()
                Toast.makeText(this, "Signal Refreshed", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissionsIfNeeded()
            }
        }

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

        toggleMode(false)

        if (hasAllPermissions()) {
            updatePermissionState()
        } else {
            requestPermissionsIfNeeded()
        }
    }

    // --- Baki code waisa hi rahega ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> { showSettingsDialog(); true }
            R.id.action_about -> { showAboutReadmeDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMessage("Configuration Details:\n\n• Refresh Interval: 3 Seconds\n• AR Engine: Google ARCore\n\nHave feedback?")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Send Feedback") { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:skgdrive932@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback: AR Cellular Signal Visualizer")
                }
                startActivity(intent)
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
            • Real-time Cellular Signal Tracking
            • Dual Mode: 2D Dashboard & 3D AR
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About App")
            .setMessage(aboutContent)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun toggleMode(enable3D: Boolean) {
        is3DMode = enable3D
        if (is3DMode) {
            container2D.visibility = View.GONE
            arContainer.visibility = View.VISIBLE
            modeSwitchButton.text = "SWITCH TO 2D MODE"
            if (sceneView == null) {
                sceneView = ArSceneView(this)
                arContainer.addView(sceneView)
            }
        } else {
            arContainer.visibility = View.GONE
            container2D.visibility = View.VISIBLE
            modeSwitchButton.text = "SWITCH TO 3D AR MODE"
        }
    }

    private fun updateSignal() {
        val info = reader.read()
        val dbmStr = "${info.dbm ?: "N/A"} dBm"
        signalText2D.text = dbmStr
        signalText.text = dbmStr
        // ... (Baaki signal update logic)
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsIfNeeded() = permissionLauncher.launch(requiredPermissions)

    private fun updatePermissionState() {
        if (hasAllPermissions()) {
            permissionButton.visibility = View.GONE
            updateSignal()
        } else {
            permissionButton.visibility = View.VISIBLE
        }
    }
}
