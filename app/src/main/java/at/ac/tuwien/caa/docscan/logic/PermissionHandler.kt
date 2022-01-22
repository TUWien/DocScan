package at.ac.tuwien.caa.docscan.logic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile


object PermissionHandler {

    val requiredMandatoryPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    fun checkMandatoryPermissions(context: Context) =
        requiredMandatoryPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * This function checks several conditions for [folder]:
     * - if it has write permission.
     * - if the folder exists (could have been deleted from outside the app's scope or if it's a SD card that could have been ejected)
     */
    fun isPermissionGiven(context: Context, folder: String?): Boolean {
        folder ?: return false
        return context.contentResolver?.persistedUriPermissions?.firstOrNull { uriPermission ->
            uriPermission.uri.toString() == folder && uriPermission.isWritePermission && DocumentFile.fromTreeUri(
                context,
                uriPermission.uri
            )?.exists() == true
        } != null
    }
}
