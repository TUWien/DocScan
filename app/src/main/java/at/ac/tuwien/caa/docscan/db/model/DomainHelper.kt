package at.ac.tuwien.caa.docscan.db.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

fun DocumentWithPages.sortByNumber(): DocumentWithPages {
    pages = pages.sortedBy { page -> page.index }
    return this
}

fun Flow<DocumentWithPages?>.sortByNumber(): Flow<DocumentWithPages?> {
    return transformLatest {
        emit(it?.sortByNumber())
    }
}
