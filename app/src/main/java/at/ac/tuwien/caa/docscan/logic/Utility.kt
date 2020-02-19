package at.ac.tuwien.caa.docscan.logic

import java.io.File

class Utility {

    companion object {

        /**
         * Copies a file and overwrites the target file if it is existing.
         */
        fun copyFile(file: File, target: File): Boolean {

            try {
//                Overwrite the current file:
                file.copyTo(target, true)
            }
            catch(e: Exception) {
                return false
            }

            return true
        }

    }
}