package at.ac.tuwien.caa.docscan.db.exception

sealed class DBException : Exception()

/**
 * An exception which indicates that a document is in a state but the action can be still proceeded.
 */
sealed class DBWarning() : DBException()

/**
 * An exception which indicates that a document is in a state but the action can be still proceeded.
 */
sealed class DBError() : DBException()

/**
 * An exception which indicates an already existing sample in the DB.
 */
class DBDocumentDuplicate : DBError()

/**
 * An exception which indicates that a document is currently locked, and no modifications to the
 * pages are possible
 */
class DBDocumentLocked(status: DocumentLockStatus) : DBError()

/**
 * An exception which indicates that the operation to the document has been already performed.
 */
class DBDuplicateAction() : DBError()

/**
 * Represents the lock status of the document.
 */
enum class DocumentLockStatus {
    /**
     * Document is being uploaded.
     */
    UPLOAD,

    /**
     * Document is being processed.
     */
    PROCESSING,

    /**
     * Document is being exported.
     */
    EXPORTING
}
