package at.ac.tuwien.caa.docscan.logic

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import at.ac.tuwien.caa.docscan.extensions.safeOffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkUtil
@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
constructor(
    private val connectivityManager: ConnectivityManager
) {

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private val unmeteredCapabilities = listOf(
        NetworkCapabilities.NET_CAPABILITY_INTERNET,
        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
    )

    private val connectedCapabilities = listOf(
        NetworkCapabilities.NET_CAPABILITY_INTERNET
    )

    fun getNetworkStatus(): NetworkStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?.getNetworkStatus() ?: NetworkStatus.DISCONNECTED
        } else {
            if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                if (connectivityManager.isActiveNetworkMetered) {
                    NetworkStatus.CONNECTED_METERED
                } else {
                    NetworkStatus.CONNECTED_UNMETERED
                }
            } else {
                NetworkStatus.DISCONNECTED
            }
        }
    }

    /**
     * @return a flow which emits the network status as described in the states in [NetworkStatus].
     */
    fun watchNetworkAvailability() =
        callbackFlow {

            val callback = object : ConnectivityManager.NetworkCallback() {

                override fun onLost(network: Network) {
                    super.onLost(network)
                    offerStatus(NetworkStatus.DISCONNECTED)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    offerStatus(NetworkStatus.DISCONNECTED)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    offerStatus(networkCapabilities.getNetworkStatus())
                }

                private fun offerStatus(networkStatus: NetworkStatus) {
                    mainThreadHandler.post {
                        safeOffer(networkStatus)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                val builder = NetworkRequest.Builder()
                connectedCapabilities.forEach {
                    builder.addCapability(it)
                }
                connectivityManager.registerNetworkCallback(builder.build(), callback)
            }
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()

    private fun NetworkCapabilities.getNetworkStatus(): NetworkStatus {
        return when {
            hasCapabilities(unmeteredCapabilities) -> {
                NetworkStatus.CONNECTED_UNMETERED
            }
            hasCapabilities(connectedCapabilities) -> {
                NetworkStatus.CONNECTED_METERED
            }
            else -> NetworkStatus.DISCONNECTED
        }
    }
}

private fun NetworkCapabilities.hasCapabilities(capabilities: List<Int>): Boolean {
    capabilities.forEach {
        if (!hasCapability(it)) {
            return false
        }
    }
    return true
}

/**
 * The network status represents for most android versions the default network which is currently used,
 * it doesn't matter which transport type it is, it rather uses metered/unmetered flags to determine
 * if the network may cause charges for the user.
 */
enum class NetworkStatus {
    CONNECTED_UNMETERED,
    CONNECTED_METERED,
    DISCONNECTED
}
