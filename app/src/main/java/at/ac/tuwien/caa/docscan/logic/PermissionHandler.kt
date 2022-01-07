package at.ac.tuwien.caa.docscan.logic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

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
     * @return true if a writable persistedUriPermission is given for a folder
     */
    fun isPermissionGiven(context: Context, folder: String): Boolean {
        return context.contentResolver?.persistedUriPermissions?.firstOrNull { uriPermission ->
            uriPermission.uri.toString() == folder
        } != null
    }
}
