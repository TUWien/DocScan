package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.isLocked
import at.ac.tuwien.caa.docscan.db.model.state.LockState
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.*
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent
import java.util.*
import java.util.concurrent.Executors

private val docDao: DocumentDao by KoinJavaComponent.inject(DocumentDao::class.java)
private val pageDao: PageDao by KoinJavaComponent.inject(PageDao::class.java)

/**
 * An own dispatcher for the locking mechanisms, this is a single threaded executor, in order to
 * not override lock states accidentally.
 */
val dbLockDispatcher by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

/**
 * Unlocks the document.
 */
suspend fun unLockDocAfterLongRunningOperation(documentId: UUID): Resource<Unit> {
    return withContext(dbLockDispatcher) {
        tryToUnlockDoc(documentId, null)
        return@withContext Success(Unit)
    }
}

/**
 * Locks the doc entirely until it is unlocked. The caller is responsible for unlocking it.
 */
suspend fun lockDocForLongRunningOperation(documentId: UUID): Resource<Unit> {
    return withContext(dbLockDispatcher) {
        val doc = docDao.getDocument(documentId)
            ?: return@withContext DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()

        when (val checkLockResult = checkLock(doc, null)) {
            is Failure -> {
                // return the error if the check has failed
                return@withContext Failure(checkLockResult.exception)
            }
            is Success -> {
                // lock the doc
                lockDoc(documentId)
                return@withContext Success(Unit)
            }
        }
    }
}

/**
 * Performs an operation on the doc itself, which meanwhile locks the doc, so that other write
 * access will fail.
 */
suspend fun <T> performDocOperation(
    documentId: UUID,
    operation: (document: Document) -> Resource<T>
): Resource<T> {
    return lockForOperation(documentId, null) { document, _ ->
        return@lockForOperation operation(document)
    }
}

/**
 * Performs an operation on the page itself, which meanwhile locks the doc partially, so that other write
 * access will fail on that particular page.
 */
suspend fun <T> performPageOperation(
    documentId: UUID,
    pageId: UUID,
    operation: (document: Document, page: Page) -> Resource<T>
): Resource<T> {
    return lockForOperation(documentId, pageId) { document, page ->
        if (page != null) {
            return@lockForOperation operation(document, page)
        }
        return@lockForOperation DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
    }
}

private suspend fun <T> lockForOperation(
    documentId: UUID,
    pageId: UUID?,
    operation: (document: Document, page: Page?) -> Resource<T>
): Resource<T> {
    return withContext(dbLockDispatcher) {
        val doc = docDao.getDocument(documentId)
            ?: return@withContext DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        val page = if (pageId != null) {
            // if the page is requested, but not available, then an error should be returned.
            pageDao.getPageByIdNonSuspendable(pageId)
                ?: return@withContext DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        } else {
            null
        }

        when (val checkLockResult = checkLock(doc, page)) {
            is Failure -> {
                // return the error if the check has failed
                return@withContext Failure(checkLockResult.exception)
            }
            is Success -> {
                // ignore, check for lock is OK.
            }
        }

        lockDoc(documentId, pageId)

        var result: Resource<T> = DBErrorCode.GENERIC.asFailure()
        try {
            withContext(Dispatchers.IO) {
                result = operation(doc, page)
            }
        } catch (cancellationException: CancellationException) {
            // in case the operation is cancelled, this exception is caught, so that the document
            // will be unlocked.
        } finally {
            withContext(NonCancellable) {
                tryToUnlockDoc(documentId, pageId)
            }
        }
        return@withContext result
    }
}

/**
 * Checks for the lock state of a document and page
 */
private fun checkLock(doc: Document, page: Page?): Resource<Unit> {
    when (doc.lockState) {
        LockState.NONE -> {
            // ignore - no, lock state
        }
        LockState.PARTIAL_LOCK -> {
            when {
                page != null -> {
                    when (page.postProcessingState) {
                        PostProcessingState.DRAFT, PostProcessingState.DONE -> {
                            // partial doc lock, but page is not locked, so operation on page can be performed.
                        }
                        PostProcessingState.PROCESSING -> {
                            return DBErrorCode.DOCUMENT_PARTIALLY_LOCKED.asFailure()
                        }
                    }
                }
                else -> {
                    return DBErrorCode.DOCUMENT_PARTIALLY_LOCKED.asFailure()
                }
            }
        }
        LockState.FULL_LOCK -> {
            return DBErrorCode.DOCUMENT_LOCKED.asFailure()
        }
    }
    return Success(Unit)
}

private fun lockDoc(documentId: UUID, pageId: UUID? = null) {
    docDao.setDocumentLock(
        documentId,
        if (pageId == null) LockState.FULL_LOCK else LockState.PARTIAL_LOCK
    )
}

private suspend fun tryToUnlockDoc(documentId: UUID, pageId: UUID?) {
    // it might happen that this won't return anything, this is because some operations like deleting
    // a document would remove this.
    val docWithPages = docDao.getDocumentWithPages(documentId) ?: return
    val unLockEntireDocument = when (docWithPages.document.lockState) {
        LockState.NONE -> {
            true // if no lock was set, then unlock document
        }
        LockState.FULL_LOCK -> {
            true // if full lock was set by this operation, then full lock can be unset
        }
        LockState.PARTIAL_LOCK -> {
            // check if partial lock constraints are still set
            var lockedBecauseOfOtherPage = false
            run lit@{
                docWithPages.pages.forEach {
                    if (it.isLocked() && it.id != pageId) {
                        lockedBecauseOfOtherPage = true
                        return@lit
                    }
                }
            }
            !lockedBecauseOfOtherPage
        }
    }
    if (unLockEntireDocument) {
        docDao.setDocumentLock(
            documentId,
            LockState.NONE
        )
    }
}
