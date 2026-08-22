package io.nekohasekai.sfa.bg

import android.net.Network
import android.os.Build
import android.os.SystemClock
import android.util.Log
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.sfa.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.NetworkInterface

object DefaultNetworkMonitor {

    private const val TAG = "DefaultNetworkMonitor"
    private const val INTERFACE_LOOKUP_ATTEMPTS = 10
    private const val INTERFACE_LOOKUP_DELAY_MILLIS = 100L

    @Volatile
    var defaultNetwork: Network? = null
    private var listener: InterfaceUpdateListener? = null
    private var started = false
    private val recoveryLock = Any()
    private val recoveryPolicy = DefaultNetworkRecoveryPolicy()
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var interfaceLookupJob: Job? = null
    private var recoveryJob: Job? = null

    suspend fun start() {
        synchronized(recoveryLock) {
            started = true
            resetRecoveryLocked()
        }
        DefaultNetworkListener.start(this) {
            defaultNetwork = it
            checkDefaultInterfaceUpdate(it)
        }
        defaultNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Application.connectivity.activeNetwork
        } else {
            DefaultNetworkListener.get()
        }
    }

    suspend fun stop() {
        synchronized(recoveryLock) {
            started = false
            resetRecoveryLocked()
        }
        DefaultNetworkListener.stop(this)
    }

    suspend fun require(): Network {
        val network = defaultNetwork
        if (network != null) {
            return network
        }
        return DefaultNetworkListener.get()
    }

    fun setListener(listener: InterfaceUpdateListener?) {
        val shouldCheck =
            synchronized(recoveryLock) {
                if (this.listener !== listener) {
                    this.listener = listener
                    resetRecoveryLocked()
                }
                started && listener != null
            }
        if (shouldCheck) checkDefaultInterfaceUpdate(defaultNetwork)
    }

    private fun checkDefaultInterfaceUpdate(newNetwork: Network?) {
        val lookupJob =
            synchronized(recoveryLock) {
                if (!started || listener == null) return@synchronized null

                interfaceLookupJob?.cancel()
                recoveryScope.launch(start = CoroutineStart.LAZY) {
                    val signature = resolveSignature(newNetwork) ?: return@launch
                    observeSignature(newNetwork, signature)
                }.also { interfaceLookupJob = it }
            } ?: return
        lookupJob.start()
    }

    private suspend fun resolveSignature(network: Network?): DefaultNetworkSignature? {
        if (network == null) {
            return DefaultNetworkSignature(networkId = null, interfaceName = "", interfaceIndex = -1)
        }

        repeat(INTERFACE_LOOKUP_ATTEMPTS) { attempt ->
            val interfaceName =
                Application.connectivity.getLinkProperties(network)?.interfaceName?.takeIf { it.isNotBlank() }
            val interfaceIndex =
                interfaceName?.let {
                    runCatching { NetworkInterface.getByName(it)?.index }.getOrNull()
                }
            if (interfaceName != null && interfaceIndex != null) {
                return DefaultNetworkSignature(
                    networkId = networkId(network),
                    interfaceName = interfaceName,
                    interfaceIndex = interfaceIndex,
                )
            }
            if (attempt + 1 < INTERFACE_LOOKUP_ATTEMPTS) delay(INTERFACE_LOOKUP_DELAY_MILLIS)
        }
        return null
    }

    private fun observeSignature(
        network: Network?,
        signature: DefaultNetworkSignature,
    ) {
        synchronized(recoveryLock) {
            if (!started || listener == null || defaultNetwork != network) return

            recoveryPolicy.observe(signature, SystemClock.elapsedRealtime())
            ensureRecoveryJobLocked()
        }
    }

    private fun ensureRecoveryJobLocked() {
        if (listener == null || recoveryPolicy.nextRecoveryDelayMillis(SystemClock.elapsedRealtime()) == null) return
        if (recoveryJob?.isActive == true) return

        val job =
            recoveryScope.launch(start = CoroutineStart.LAZY) {
                processRecoveries()
            }
        recoveryJob = job
        job.start()
    }

    private suspend fun processRecoveries() {
        val currentJob = currentCoroutineContext()[Job]
        try {
            while (currentCoroutineContext().isActive) {
                val waitMillis =
                    synchronized(recoveryLock) {
                        if (listener == null) null else recoveryPolicy.nextRecoveryDelayMillis(SystemClock.elapsedRealtime())
                    } ?: return
                if (waitMillis > 0L) {
                    delay(waitMillis)
                    continue
                }

                val request =
                    synchronized(recoveryLock) {
                        val currentListener = listener
                        val signature = recoveryPolicy.takeRecovery(SystemClock.elapsedRealtime())
                        if (currentListener == null || signature == null) null else RecoveryRequest(currentListener, signature)
                    } ?: continue
                try {
                    request.listener.updateDefaultInterface(
                        request.signature.interfaceName,
                        request.signature.interfaceIndex,
                        false,
                        false,
                    )
                    synchronized(recoveryLock) {
                        recoveryPolicy.recoverySucceeded(request.signature)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Failed to update default interface", error)
                    synchronized(recoveryLock) {
                        recoveryPolicy.recoveryFailed(request.signature)
                    }
                }
            }
        } finally {
            synchronized(recoveryLock) {
                if (recoveryJob === currentJob) {
                    recoveryJob = null
                    ensureRecoveryJobLocked()
                }
            }
        }
    }

    private fun resetRecoveryLocked() {
        interfaceLookupJob?.cancel()
        interfaceLookupJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        recoveryPolicy.reset()
    }

    private fun networkId(network: Network): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        network.networkHandle
    } else {
        network.hashCode().toLong()
    }

    private data class RecoveryRequest(
        val listener: InterfaceUpdateListener,
        val signature: DefaultNetworkSignature,
    )
}
