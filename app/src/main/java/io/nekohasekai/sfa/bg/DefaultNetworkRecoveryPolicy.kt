package io.nekohasekai.sfa.bg

internal data class DefaultNetworkSignature(
    val networkId: Long?,
    val interfaceName: String,
    val interfaceIndex: Int,
)

internal class DefaultNetworkRecoveryPolicy(
    private val debounceMillis: Long = 250L,
    private val cooldownMillis: Long = 1_000L,
    private val maxRecoveryEvents: Int = 4,
) {
    private var latestSignature: DefaultNetworkSignature? = null
    private var latestObservedAtMillis = 0L
    private var appliedSignature: DefaultNetworkSignature? = null
    private var pendingSignature: DefaultNetworkSignature? = null
    private var inFlightSignature: DefaultNetworkSignature? = null
    private var lastRecoveryAtMillis: Long? = null
    private var recoveryEvents = 0

    init {
        require(debounceMillis >= 0L)
        require(cooldownMillis >= 0L)
        require(maxRecoveryEvents > 0)
    }

    fun observe(
        signature: DefaultNetworkSignature,
        nowMillis: Long,
    ): Boolean {
        require(nowMillis >= 0L)
        if (latestSignature == signature) return false

        latestSignature = signature
        latestObservedAtMillis = nowMillis
        pendingSignature = signature.takeUnless { it == appliedSignature }
        recoveryEvents = 0
        return true
    }

    fun nextRecoveryDelayMillis(nowMillis: Long): Long? {
        require(nowMillis >= 0L)
        if (pendingSignature == null || inFlightSignature != null || recoveryEvents >= maxRecoveryEvents) return null

        val debounceDelay = remainingDelay(latestObservedAtMillis, debounceMillis, nowMillis)
        val cooldownDelay =
            lastRecoveryAtMillis?.let { remainingDelay(it, cooldownMillis, nowMillis) } ?: 0L
        return maxOf(debounceDelay, cooldownDelay)
    }

    fun takeRecovery(nowMillis: Long): DefaultNetworkSignature? {
        if (nextRecoveryDelayMillis(nowMillis) != 0L) return null

        val signature = pendingSignature ?: return null
        inFlightSignature = signature
        lastRecoveryAtMillis = nowMillis
        recoveryEvents += 1
        return signature
    }

    fun recoverySucceeded(signature: DefaultNetworkSignature) {
        if (inFlightSignature != signature) return

        inFlightSignature = null
        appliedSignature = signature
        pendingSignature = latestSignature?.takeUnless { it == appliedSignature }
    }

    fun recoveryFailed(signature: DefaultNetworkSignature) {
        if (inFlightSignature != signature) return

        inFlightSignature = null
        pendingSignature = latestSignature?.takeUnless { it == appliedSignature }
    }

    fun reset() {
        latestSignature = null
        latestObservedAtMillis = 0L
        appliedSignature = null
        pendingSignature = null
        inFlightSignature = null
        lastRecoveryAtMillis = null
        recoveryEvents = 0
    }

    private fun remainingDelay(
        startedAtMillis: Long,
        durationMillis: Long,
        nowMillis: Long,
    ): Long = (durationMillis - (nowMillis - startedAtMillis)).coerceAtLeast(0L)
}
