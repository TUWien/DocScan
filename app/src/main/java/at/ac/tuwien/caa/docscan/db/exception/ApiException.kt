package at.ac.tuwien.caa.docscan.db.exception

/**
 * An exception which indicates API issues.
 */
open class ApiException(httpCode: Int) : Exception()
