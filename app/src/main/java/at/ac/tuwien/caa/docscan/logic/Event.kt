package at.ac.tuwien.caa.docscan.logic

import androidx.lifecycle.Observer

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 *
 * @author matejbart
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content if it has not already been handled,
     * and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content

    /**
     * Returns the content if it has not already been handled, but
     * does NOT automatically mark it as "handled"
     */
    fun peekContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            content
        }
    }

    /**
     * Marks the content as "handled" in a separate step; this is useful if multiple components
     * have to check if they are responsible for handling the event by using [peekContent] and
     * later marking it as "handled".
     */
    fun markAsHandled() {
        hasBeenHandled = true
    }
}

class ConsumableEvent<T>(private val onEventUnhandledAction: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let { value ->
            onEventUnhandledAction(value)
        }
    }
}