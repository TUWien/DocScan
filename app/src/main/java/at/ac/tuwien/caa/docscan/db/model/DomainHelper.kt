package at.ac.tuwien.caa.docscan.db.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

fun DocumentWithPages.sortByNumber(): DocumentWithPages {
    pages = pages.sortedBy { page -> page.number }
    return this
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<DocumentWithPages?>.sortByNumber(): Flow<DocumentWithPages?> {
    return transformLatest {
        emit(it?.sortByNumber())
    }
}
