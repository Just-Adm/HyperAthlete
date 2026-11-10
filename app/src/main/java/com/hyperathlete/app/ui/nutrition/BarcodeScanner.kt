package com.hyperathlete.app.ui.nutrition

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.hyperathlete.app.R
import com.hyperathlete.app.core.util.findActivity

/**
 * Barcode scanning for packaged food (brief §11).
 *
 * 1. Google's code scanner (a ready camera screen from Play services, no camera permission). Its module is
 *    downloaded by Play services on demand, which doesn't happen by itself for an app installed from an APK,
 *    so the module is checked and, if missing, installed first.
 * 2. If Play services can't provide it (no Google services, download failed, scanner error), the app's own
 *    camera scanner ([CameraBarcodeScreen], on-device ML Kit, needs the camera permission) takes over.
 */
object BarcodeScanner {
    private const val TAG = "BarcodeScanner"

    val formats = intArrayOf(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)

    fun scan(context: Context, onCode: (String) -> Unit, onFallback: () -> Unit) {
        val host = context.findActivity() ?: context
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(formats[0], *formats.drop(1).toIntArray())
            .enableAutoZoom()
            .build()
        val client = runCatching { GmsBarcodeScanning.getClient(host, options) }.getOrNull()
        if (client == null) { onFallback(); return }

        fun start() {
            client.startScan()
                .addOnSuccessListener { b -> b.rawValue?.let(onCode) }
                .addOnFailureListener { e -> Log.w(TAG, "Google scanner failed, using the in-app camera", e); onFallback() }
        }

        val modules = runCatching { ModuleInstall.getClient(host) }.getOrNull()
        if (modules == null) { start(); return }
        modules.areModulesAvailable(client)
            .addOnSuccessListener { r ->
                if (r.areModulesAvailable()) start()
                else {
                    Toast.makeText(context, R.string.barcode_downloading, Toast.LENGTH_SHORT).show()
                    var listener: InstallStatusListener? = null
                    listener = InstallStatusListener { u ->
                        when (u.installState) {
                            ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> { listener?.let { modules.unregisterListener(it) }; start() }
                            ModuleInstallStatusUpdate.InstallState.STATE_FAILED,
                            ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> { listener?.let { modules.unregisterListener(it) }; onFallback() }
                            else -> Unit
                        }
                    }
                    modules.installModules(ModuleInstallRequest.newBuilder().addApi(client).setListener(listener).build())
                        .addOnSuccessListener { resp -> if (resp.areModulesAlreadyInstalled()) { modules.unregisterListener(listener); start() } }
                        .addOnFailureListener { e -> Log.w(TAG, "scanner module install failed", e); modules.unregisterListener(listener); onFallback() }
                }
            }
            .addOnFailureListener { e -> Log.w(TAG, "module check failed", e); onFallback() }
    }
}
