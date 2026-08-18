private fun checkAndPromptARCore() {
    try {
        val availability = ArCoreApk.getInstance().checkAvailability(this)

        if (availability.isTransient) {
            handler.postDelayed({ checkAndPromptARCore() }, 200)
            return
        }

        // Agar AR Supported nahi hai ya ARCore installed nahi hai
        if (!availability.isSupported || availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            showCustomArRequiredDialog()
        } else {
            // UserRequested false karke check karenge taaki Google ka internal dialog na aaye
            val installStatus = ArCoreApk.getInstance().requestInstall(this, false)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                showCustomArRequiredDialog()
            }
        }
    } catch (e: Exception) {
        showCustomArRequiredDialog()
    }
}

// Aapka Custom Popup Dialog
private fun showCustomArRequiredDialog() {
    AlertDialog.Builder(this)
        .setTitle("AR Core Required")
        .setMessage("3D AR mode use karne ke liye Google Play Services for AR required hai. Kya aap ise setup karna chahte hain?")
        .setPositiveButton("Install / Enable") { _, _ ->
            try {
                // User ke click karne par hi Google install request trigger hogi
                ArCoreApk.getInstance().requestInstall(this, true)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to launch AR installer", Toast.LENGTH_SHORT).show()
                toggleMode(false)
            }
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            toggleMode(false)
            Toast.makeText(this, "Switched back to 2D Mode", Toast.LENGTH_SHORT).show()
        }
        .setCancelable(false)
        .show()
}
