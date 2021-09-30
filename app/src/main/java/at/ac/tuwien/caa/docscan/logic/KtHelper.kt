package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import at.ac.tuwien.caa.docscan.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.net.URLEncoder

class KtHelper {

    companion object {
        fun copyFile(srcFile: File, dstFile: File): Boolean {

            return try {
//        Overwrite an existing file (true)
                srcFile.copyTo(dstFile, true)
                true
            } catch (exception: Exception) {
                FirebaseCrashlytics.getInstance().recordException(exception)
                false
            }
        }

        /**
         * Returns the directory in which pdf's are saved.
         */
        private fun getPdfDirectory(context: Context): String? {
            val dir: String?
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            dir = sharedPref.getString(context.getString(R.string.key_pdf_dir), null)

            return dir
        }

        /**
         * Returns true if a writable persistedUriPermission is given for a folder
         */
        fun isPdfFolderPermissionGiven(context: Context): Boolean {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                Permission is not given if folder is null:
                val folder = getPdfDirectory(context) ?: return false

                val file = DocumentFile.fromTreeUri(context, Uri.parse(folder))
                if (file == null || !file.exists())
                    return false

                val permissions = context?.contentResolver?.persistedUriPermissions
                if (permissions != null) {
                    for (permission in permissions) {
                        if (permission.uri.toString() == folder)
                            return true
                    }
                }
                return false
            } else
                return true

        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getDocScanPdfUri(context: Context): Uri {

            val sm = context!!.getSystemService(Context.STORAGE_SERVICE) as StorageManager

            val rootId =
                if (sm.primaryStorageVolume.isEmulated)
                    "primary"
                else
                    sm.primaryStorageVolume.uuid

            val rootUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", rootId)
            val documentsDir = Environment.DIRECTORY_DOCUMENTS
            val docScanDir = context.getString(R.string.app_name)
            val concatedDir = ":$documentsDir/$docScanDir"
            val encodedDir = URLEncoder.encode(concatedDir, "utf-8")
            val absoluteDir = "$rootUri$encodedDir"
            val absoluteUri = Uri.parse(absoluteDir)

            return absoluteUri
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getOpenDocumentDirIntent(context: Context): Intent? {
            if (context != null) {
                val docScanPdfUri = getDocScanPdfUri(context)

                return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, docScanPdfUri)
                    putExtra("android.provider.extra.SHOW_ADVANCED", true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
            }
            return null
        }

        fun saveDocumentDir(context: Context, uri: Uri) {
            if (uri == null) {
                return
            }
            context.contentResolver?.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putString(context.getString(R.string.key_pdf_dir), uri.toString())
            editor.commit()

        }
    }

}