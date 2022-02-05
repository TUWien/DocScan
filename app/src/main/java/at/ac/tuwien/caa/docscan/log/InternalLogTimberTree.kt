package at.ac.tuwien.caa.docscan.log

import android.util.Log
import at.ac.tuwien.caa.docscan.extensions.getTimeStamp
import at.ac.tuwien.caa.docscan.log.InternalLogTimberTree.Companion.LOGGABLE_PRIORITIES
import at.ac.tuwien.caa.docscan.logic.FileHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * A timber tree which records all [LOGGABLE_PRIORITIES] calls and writes them into internal storage.
 */
class InternalLogTimberTree(
    private val fileHandler: FileHandler,
    private val coroutineScope: CoroutineScope
) : Timber.Tree() {

    companion object {
        val LOGGABLE_PRIORITIES = listOf(Log.ERROR, Log.WARN, Log.ASSERT, Log.INFO)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return LOGGABLE_PRIORITIES.contains(priority)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val firstLine = "${Date().getTimeStamp()} ${getPriorityPrefix(priority)}${tag ?: ""}: $message"
        coroutineScope.launch {
            fileHandler.appendToLog(firstLine, t)
        }
    }

    private fun getPriorityPrefix(priority: Int): String {
        val prefix = when (priority) {
            Log.VERBOSE -> {
                "V"
            }
            Log.DEBUG -> {
                "D"
            }
            Log.INFO -> {
                "I"
            }
            Log.WARN -> {
                "W"
            }
            Log.ERROR -> {
                "E"
            }
            Log.ASSERT -> {
                "A"
            }
            else -> {
                "UKN(${priority})" // unknown priority
            }
        }
        return "$prefix/"
    }
}
