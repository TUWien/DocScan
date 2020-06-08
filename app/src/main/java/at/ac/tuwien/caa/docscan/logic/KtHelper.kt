package at.ac.tuwien.caa.docscan.logic

import com.crashlytics.android.Crashlytics
import java.io.File

class KtHelper {

    companion object {
        fun copyFile(srcFile: File, dstFile: File): Boolean {

            return try {
//        Overwrite an existing file (true)
                srcFile.copyTo(dstFile, true)
                true
            } catch (exception: Exception) {
                Crashlytics.logException(exception)
                false
            }

        }
    }

}