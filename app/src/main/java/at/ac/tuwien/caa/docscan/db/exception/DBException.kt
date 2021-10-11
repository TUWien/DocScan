package at.ac.tuwien.caa.docscan.db.exception

open class DBException : Exception()

/**
 * An exception which indicates an already existing sample in the DB.
 */
class DBDocumentDuplicate : DBException()
