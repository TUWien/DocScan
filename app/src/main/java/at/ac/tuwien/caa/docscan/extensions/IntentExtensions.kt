package at.ac.tuwien.caa.docscan.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.PageFileType
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
        Timber.e(exception, "No activity found to start intent!")
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
        intent.putExtra(
            DocumentsContract.EXTRA_INITIAL_URI, initialUri
                ?: getDocScanPdfUri(context)
        )
    }
    // TODO: This is a system setting which may not work
    intent.putExtra("android.provider.extra.SHOW_ADVANCED", true)
    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    return intent
}

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
    shareFile(fragmentActivity, pageFileType, listOf(uri))
}

fun shareFile(fragmentActivity: FragmentActivity, pageFileType: PageFileType, uris: List<Uri>) {
    fragmentActivity.safeStartActivity(getShareIntent(pageFileType, uris))
}

private fun getShareIntent(
    pageFileType: PageFileType,
    uris: List<Uri>
): Intent {
    val shareIntent = Intent()
    if (uris.size == 1) {
        shareIntent.action = Intent.ACTION_SEND
    } else {
        shareIntent.action = Intent.ACTION_SEND_MULTIPLE
    }
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (uris.size == 1) {
        shareIntent.putExtra(Intent.EXTRA_STREAM, uris[0])
    } else {
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    shareIntent.type = pageFileType.mimeType
    return shareIntent
}

fun shareFileAsEmailLog(
    fragmentActivity: FragmentActivity,
    pageFileType: PageFileType,
    uri: Uri
): Boolean {
    val intent = getShareIntent(pageFileType, listOf(uri))
    intent.run {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(fragmentActivity.getString(R.string.log_email_to)))
        putExtra(
            Intent.EXTRA_SUBJECT,
            fragmentActivity.getString(R.string.log_email_subject)
        )
        putExtra(Intent.EXTRA_TEXT, fragmentActivity.getString(R.string.log_email_text))
    }
    return fragmentActivity.safeStartActivity(intent)
}

fun showFile(
    fragmentActivity: FragmentActivity,
    pageFileType: PageFileType,
    docFile: DocumentFile
) {
    val uri = docFile.uri
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_VIEW
    shareIntent.setDataAndType(uri, pageFileType.mimeType)
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    fragmentActivity.safeStartActivity(shareIntent)
}

fun showAppSettings(fragmentActivity: FragmentActivity) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", fragmentActivity.baseContext.packageName, null)
    intent.data = uri
    fragmentActivity.safeStartActivity(intent)
}
