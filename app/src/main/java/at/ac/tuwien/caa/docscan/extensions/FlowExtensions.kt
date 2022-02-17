package at.ac.tuwien.caa.docscan.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel

/**
 * A safe call for [SendChannel.offer] which is only called if [SendChannel.isClosedForSend] == false
 * and surrounded with a try/catch in case a [CancellationException] is thrown.
 *
 * @return true if the value has been successfully added to the [SendChannel]'s queue else false
 */
@ExperimentalCoroutinesApi
fun <E> SendChannel<E>.safeOffer(value: E) = !isClosedForSend && try {
    val result = trySend(value)
    result.isSuccess
} catch (e: CancellationException) {
    false
}
