package at.ac.tuwien.caa.docscan.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class DocumentWithPages(
    @Embedded
    val document: Document,

    @Relation(
        parentColumn = Document.KEY_ID,
        entityColumn = Page.KEY_DOC_ID
    )
    var pages: List<Page> = listOf()
)

fun DocumentWithPages.isCropped(): Boolean {
    //TODO: Implement this, it's currently wrong, this should be part of the Page domain
    return false
}

fun DocumentWithPages.isuploaded(): Boolean {
    //TODO: Implement this, this is basically false and should be represented with a XOR enum in the Document
    return false
}

// TODO: Is document cropped is currently encoded in the exif, but this does not work very well, adding to DB?
//public static boolean isDocumentCropped(DocumentWithPages document) {
//
//    if (document != null) {
//        ArrayList<File> files = document.getPages();
//        if (files != null && !files.isEmpty()) {
//            for (File file : files) {
//                if (!PageDetector.isCropped(file.getAbsolutePath()))
//                    return false;
//            }
//        }
//    }
//
//    return true;
//}