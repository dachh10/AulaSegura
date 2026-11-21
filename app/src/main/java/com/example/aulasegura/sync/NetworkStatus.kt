package com.example.aulasegura.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Single source of truth for "can we reach Firestore right now?". A fun
 * interface so a ViewModel test can answer without a device.
 */
fun interface NetworkStatus {
    fun isAvailable(): Boolean

    companion object {
        fun from(context: Context): NetworkStatus {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            return NetworkStatus {
                val network = manager?.activeNetwork
                if (network == null) {
                    false
                } else {
                    manager.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ||
                        manager.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                        manager.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
                }
            }
        }
    }
}
