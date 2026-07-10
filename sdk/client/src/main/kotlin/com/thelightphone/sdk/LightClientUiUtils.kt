package com.thelightphone.sdk

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.thelightphone.sdk.shared.LightServiceMethod
import com.thelightphone.sdk.shared.asKotlinResult
import com.thelightphone.sdk.ui.LightQrCodeScanner

/**
 * Wrapper for the UI library's LightQrCodeScanner that include the client library's functions for
 * checking and requesting the camera permission for the SDK server.
 *
 * When LightOS is unavailable (e.g. sideloaded dev builds), falls back to standard Android
 * camera permission checks so scanning still works if the user granted CAMERA in settings.
 */
@Composable
fun LightQrCodeScanner(
    onScanned: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Scan QR Code",
) {
    val context = LocalContext.current
    val permissionLauncher = rememberPermissionRequestLauncher(Manifest.permission.CAMERA)
    val androidPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* LightQrCodeScanner re-checks on resume */ }

    LightQrCodeScanner(
        title = title,
        onScanned = onScanned,
        onBack = onBack,
        modifier = modifier,
        checkCameraPermission = {
            val viaLight = checkPermission(Manifest.permission.CAMERA).asKotlinResult
                .map { it.permissionResult == LightServiceMethod.GetPermission.Result.Granted }
            if (viaLight.isSuccess) {
                viaLight
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                Result.success(granted)
            }
        },
        launchCameraPermissionRequest = {
            val lightBridgeWorks = checkPermission(Manifest.permission.CAMERA).asKotlinResult.isSuccess
            if (lightBridgeWorks) {
                permissionLauncher?.launch()
            } else {
                androidPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
    )
}
