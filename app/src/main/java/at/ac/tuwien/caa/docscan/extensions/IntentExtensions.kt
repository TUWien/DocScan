package at.ac.tuwien.caa.docscan.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.PageFileType
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.net.URLEncoder

/**
 * Safely handles cases in which no activities are installed to handle the given intent.
 * If no activity can handle the intent, the intent is not started, but an error dialog is shown instead.
 *
 * @return returns true if the intent was started; false otherwise
 */
fun FragmentActivity.safeStartActivity(intent: Intent): Boolean {
    return try {
        startActivity(intent)
        true
    } catch (exception: ActivityNotFoundException) {
        FirebaseCrashlytics.getInstance().recordException(exception)
        Timber.d(exception, "No activity found to start intent.")
        false
    }
}

fun getImageImportIntent(): Intent {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "image/*"
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    return intent
}

/**
 * @return an intent to ask for access of an entire public folder with [Intent.ACTION_OPEN_DOCUMENT_TREE]
 * @param initialUri if set, then the initial folder will be set, fallbacks to [getDocScanPdfUri]
 */
fun getExportFolderPermissionIntent(context: Context, initialUri: Uri?): Intent {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri
                ?: getDocScanPdfUri(context))
    }
    // TODO: This is a system setting which may not work
    intent.putExtra("android.provider.extra.SHOW_ADVANCED", true)
    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    return intent
}

// TODO: EXPORT_CONSTRAINT: SAF is used for all platforms
@RequiresApi(Build.VERSION_CODES.N)
private fun getDocScanPdfUri(context: Context): Uri {

    val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    val rootId =
            if (sm.primaryStorageVolume.isEmulated)
                "primary"
            else
                sm.primaryStorageVolume.uuid

    val rootUri =
            DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", rootId)
    val documentsDir = Environment.DIRECTORY_DOCUMENTS
    val docScanDir = context.getString(R.string.app_name)
    val concatedDir = ":$documentsDir/$docScanDir"
    val encodedDir = URLEncoder.encode(concatedDir, "utf-8")
    val absoluteDir = "$rootUri$encodedDir"

    return Uri.parse(absoluteDir)
}

fun shareFile(fragmentActivity: FragmentActivity, pageFileType: PageFileType, uri: Uri) {
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_SEND
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
    shareIntent.type = pageFileType.mimeType
    fragmentActivity.safeStartActivity(shareIntent)
}

fun showFile(fragmentActivity: FragmentActivity, pageFileType: PageFileType, docFile: DocumentFile) {
    val uri = docFile.uri
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_VIEW
    shareIntent.setDataAndType(uri, pageFileType.mimeType)
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    fragmentActivity.safeStartActivity(shareIntent)
}
