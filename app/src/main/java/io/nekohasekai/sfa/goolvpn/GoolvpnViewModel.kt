package io.nekohasekai.sfa.goolvpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GoolvpnUiState(
    val activated: Boolean = false,
    val loading: Boolean = false,
    val active: Boolean = false,
    val profileReady: Boolean = false,
    val planTitle: String = "GOOLVPN",
    val expiresAt: String? = null,
    val isLifetime: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val diagnosticsLoading: Boolean = false,
    val diagnosticsSending: Boolean = false,
    val diagnosticsReport: String? = null,
    val diagnosticsSnapshot: GoolvpnDiagnosticSnapshot? = null,
    val diagnosticsTicketId: Int? = null,
    val diagnosticsSendError: String? = null,
    val devicesLoading: Boolean = false,
    val devices: List<GoolvpnDevice> = emptyList(),
    val deviceLimit: Int = 0,
    val deviceUsed: Int = 0,
    val devicesError: String? = null,
    val onboardingStep: Int = 0,
    val accountUrl: String = "https://goolv.site/connect.html",
    val supportUrl: String = "https://goolv.site/support.html",
    val feedbackUrl: String = "https://goolv.site/feedback",
    val connectionMode: GoolvpnConnectionMode = GoolvpnConnectionMode.Automatic,
    val smartBypassGroups: List<GoolvpnSmartBypassGroup> = emptyList(),
    val smartBypassEnabled: Boolean = false,
    val smartBypassEnabledGroupIds: Set<String> = emptySet(),
)

class GoolvpnViewModel : ViewModel() {
    private val api = GoolvpnApi()
    private val tokenStore = DeviceTokenStore(Application.application)
    private val profileStore = ManagedProfileStore(Application.application)
    private val preferences =
        Application.application.getSharedPreferences("goolvpn_settings", android.content.Context.MODE_PRIVATE)
    private var serviceStatus = Status.Stopped
    private var serverConfig: String? = null
    private val activationRequests = Channel<String>(Channel.CONFLATED)
    private val _uiState =
        MutableStateFlow(GoolvpnUiState(connectionMode = savedConnectionMode()))
    val uiState: StateFlow<GoolvpnUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            for (token in activationRequests) {
                uiState.first { !it.loading }
                performActivation(token)
            }
        }
        refresh()
    }

    fun activate(token: String) {
        val normalized = token.trim()
        if (normalized.isBlank()) return
        activationRequests.trySend(normalized)
    }

    fun refresh() {
        if (_uiState.value.loading) return
        val deviceToken = tokenStore.load()
        if (deviceToken == null) {
            _uiState.value = GoolvpnUiState(connectionMode = savedConnectionMode())
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(activated = true, loading = true, error = null) }
            loadProfile(deviceToken)
        }
    }

    fun deactivate() {
        val deviceToken = tokenStore.load()
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            withContext(Dispatchers.IO) {
                if (deviceToken != null) {
                    runCatching { api.revoke(deviceToken) }
                }
                profileStore.clear()
            }
            tokenStore.clear()
            _uiState.value = GoolvpnUiState(connectionMode = savedConnectionMode())
        }
    }

    fun refreshDevices() {
        val deviceToken = tokenStore.load() ?: return
        if (_uiState.value.devicesLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(devicesLoading = true, devicesError = null) }
            try {
                val devices = withContext(Dispatchers.IO) { api.getDevices(deviceToken) }
                _uiState.update {
                    it.copy(
                        devicesLoading = false,
                        devices = devices.devices,
                        deviceLimit = devices.limit,
                        deviceUsed = devices.used,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(devicesLoading = false, devicesError = "Не удалось загрузить устройства.") }
            }
        }
    }

    fun revokeDevice(deviceId: Int) {
        val deviceToken = tokenStore.load() ?: return
        if (_uiState.value.devicesLoading || _uiState.value.devices.any { it.id == deviceId && it.isCurrent }) return
        viewModelScope.launch {
            _uiState.update { it.copy(devicesLoading = true, devicesError = null) }
            try {
                withContext(Dispatchers.IO) { api.revokeDevice(deviceToken, deviceId) }
                val devices = withContext(Dispatchers.IO) { api.getDevices(deviceToken) }
                _uiState.update {
                    it.copy(
                        devicesLoading = false,
                        devices = devices.devices,
                        deviceLimit = devices.limit,
                        deviceUsed = devices.used,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(devicesLoading = false, devicesError = "Не удалось отозвать устройство.") }
            }
        }
    }

    fun runDiagnostics() {
        if (_uiState.value.diagnosticsLoading || _uiState.value.diagnosticsSending) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    diagnosticsLoading = true,
                    diagnosticsReport = null,
                    diagnosticsSnapshot = null,
                    diagnosticsTicketId = null,
                    diagnosticsSendError = null,
                )
            }
            val snapshot = withContext(Dispatchers.IO) { buildDiagnosticsSnapshot() }
            _uiState.update {
                it.copy(
                    diagnosticsLoading = false,
                    diagnosticsSnapshot = snapshot,
                    diagnosticsReport = snapshot.render(),
                )
            }
        }
    }

    fun submitDiagnostics() {
        val deviceToken = tokenStore.load() ?: return
        val report = _uiState.value.diagnosticsReport ?: return
        if (_uiState.value.diagnosticsSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(diagnosticsSending = true, diagnosticsSendError = null) }
            try {
                val ticketId = withContext(Dispatchers.IO) { api.submitDiagnostic(deviceToken, report) }
                _uiState.update { it.copy(diagnosticsSending = false, diagnosticsTicketId = ticketId) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        diagnosticsSending = false,
                        diagnosticsSendError = diagnosticSubmitError(error),
                    )
                }
            }
        }
    }

    fun dismissDiagnostics() {
        _uiState.update {
            it.copy(
                diagnosticsLoading = false,
                diagnosticsSending = false,
                diagnosticsReport = null,
                diagnosticsSnapshot = null,
                diagnosticsTicketId = null,
                diagnosticsSendError = null,
            )
        }
    }

    fun advanceOnboarding() {
        val nextStep = when (_uiState.value.onboardingStep) {
            ONBOARDING_AUTOMATIC -> ONBOARDING_FAST
            ONBOARDING_FAST -> ONBOARDING_STABLE
            ONBOARDING_STABLE -> ONBOARDING_SMART_BYPASS
            ONBOARDING_SMART_BYPASS -> ONBOARDING_APP_ROUTING
            ONBOARDING_APP_ROUTING -> ONBOARDING_DIAGNOSTICS
            else -> 0
        }
        if (nextStep == 0) markOnboardingCompleted()
        _uiState.update { it.copy(onboardingStep = nextStep) }
    }

    fun dismissOnboarding() {
        markOnboardingCompleted()
        _uiState.update { it.copy(onboardingStep = 0) }
    }

    fun restartOnboarding() {
        preferences.edit().remove(KEY_ONBOARDING_COMPLETED).apply()
        _uiState.update { it.copy(onboardingStep = ONBOARDING_AUTOMATIC) }
    }

    fun setConnectionMode(mode: GoolvpnConnectionMode) {
        if (_uiState.value.connectionMode == mode) return
        preferences.edit().putString(KEY_CONNECTION_MODE, mode.preferenceValue).apply()
        _uiState.update { it.copy(connectionMode = mode, error = null) }
        if (serviceStatus == Status.Started) {
            viewModelScope.launch {
                if (!applyConnectionMode(mode, closeExistingConnections = true)) {
                    _uiState.update {
                        it.copy(error = "Не удалось сменить режим подключения. Переподключите VPN.")
                    }
                }
            }
        }
    }

    fun setSmartBypassEnabled(enabled: Boolean) {
        val ids = if (enabled) _uiState.value.smartBypassGroups.map { it.id }.toSet() else emptySet()
        updateSmartBypassGroups(ids)
    }

    fun setSmartBypassGroupEnabled(groupId: String, enabled: Boolean) {
        val ids = _uiState.value.smartBypassEnabledGroupIds.toMutableSet()
        if (enabled) ids += groupId else ids -= groupId
        updateSmartBypassGroups(ids)
    }

    private fun updateSmartBypassGroups(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_SMART_BYPASS_GROUPS, ids).apply()
        val enabled = ids.isNotEmpty()
        _uiState.update { it.copy(smartBypassEnabled = enabled, smartBypassEnabledGroupIds = ids, error = null) }
        val config = serverConfig ?: return
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val changed = withContext(Dispatchers.IO) {
                    profileStore.apply(
                        applySmartBypassRules(
                            config,
                            state.smartBypassGroups,
                            ids,
                        ),
                        "${state.smartBypassGroups.firstOrNull()?.catalogVersion ?: "none"}:${ids.sorted()}",
                    )
                }
                if (changed && serviceStatus == Status.Started) {
                    withContext(Dispatchers.IO) { Libbox.newStandaloneCommandClient().serviceReload() }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Не удалось применить Умный режим.") }
            }
        }
    }

    fun updateServiceStatus(status: Status) {
        val previousStatus = serviceStatus
        serviceStatus = status
        if (status == Status.Started && previousStatus != Status.Started) {
            viewModelScope.launch {
                delay(250)
                applyConnectionMode(_uiState.value.connectionMode, closeExistingConnections = false)
            }
        }
    }

    private suspend fun loadProfile(deviceToken: String) {
        try {
            val profile = withContext(Dispatchers.IO) { api.getProfile(deviceToken) }
            var profileChanged = false
            val smartBypassIds = preferences.getStringSet(KEY_SMART_BYPASS_GROUPS, emptySet()).orEmpty()
            val profileReady = if (profile.active && profile.config != null) {
                serverConfig = profile.config
                withContext(Dispatchers.IO) {
                    profileChanged = profileStore.apply(
                        applySmartBypassRules(
                            profile.config,
                            profile.smartBypassGroups,
                            smartBypassIds,
                        ),
                        "${profile.profileVersion}:${profile.smartBypassGroups.firstOrNull()?.catalogVersion ?: "none"}:${smartBypassIds.sorted()}",
                    )
                }
                true
            } else {
                withContext(Dispatchers.IO) { profileStore.clear() }
                false
            }
            if (profileChanged && serviceStatus == Status.Started) {
                withContext(Dispatchers.IO) {
                    Libbox.newStandaloneCommandClient().serviceReload()
                }
                delay(250)
                applyConnectionMode(_uiState.value.connectionMode, closeExistingConnections = false)
            }
            _uiState.value = GoolvpnUiState(
                activated = true,
                loading = false,
                active = profile.active,
                profileReady = profileReady,
                planTitle = profile.planTitle,
                expiresAt = profile.expiresAt,
                isLifetime = profile.isLifetime,
                message = profile.message,
                accountUrl = profile.accountUrl,
                supportUrl = profile.supportUrl,
                connectionMode = savedConnectionMode(),
                smartBypassGroups = profile.smartBypassGroups,
                smartBypassEnabled = smartBypassIds.isNotEmpty(),
                smartBypassEnabledGroupIds = smartBypassIds,
                onboardingStep = if (isOnboardingCompleted()) {
                    0
                } else {
                    _uiState.value.onboardingStep.takeIf { it in 1..6 } ?: ONBOARDING_AUTOMATIC
                },
            )
        } catch (error: GoolvpnApiException) {
            if (error.statusCode == 401) {
                tokenStore.clear()
                withContext(Dispatchers.IO) { profileStore.clear() }
                _uiState.value = GoolvpnUiState(error = "Активация устройства истекла. Получите новый код.")
            } else {
                _uiState.update { it.copy(loading = false, error = profileError(error)) }
            }
        } catch (error: Exception) {
            _uiState.update { it.copy(loading = false, error = profileError(error)) }
        }
    }

    private suspend fun performActivation(token: String) {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val deviceToken = withContext(Dispatchers.IO) { api.activate(token) }
            tokenStore.save(deviceToken)
            loadProfile(deviceToken)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    loading = false,
                    error = activationError(error),
                )
            }
        }
    }

    private fun activationError(error: Exception): String = when (error) {
        is GoolvpnApiException -> when (error.statusCode) {
            401 -> "Код недействителен, уже использован или истек."
            429 -> "Слишком много попыток. Подождите минуту."
            else -> "Не удалось активировать приложение. Попробуйте позже."
        }
        else -> "Нет связи с GOOLVPN. Проверьте интернет и повторите."
    }

    private fun profileError(error: Exception): String = when (error) {
        is GoolvpnApiException -> "Сервер временно не отдал профиль. Повторите обновление."
        else -> "Не удалось обновить профиль. Проверьте интернет."
    }

    private fun diagnosticSubmitError(error: Exception): String = when (error) {
        is GoolvpnApiException -> when (error.statusCode) {
            401 -> "Устройство больше не привязано. Получите новый код активации."
            429 -> "Отчёт уже недавно отправлялся. Попробуйте через несколько минут."
            else -> "Не удалось отправить отчёт. Его можно скопировать и отправить вручную."
        }
        else -> "Нет связи с GOOLVPN. Его можно скопировать и отправить вручную."
    }

    private fun savedConnectionMode(): GoolvpnConnectionMode =
        GoolvpnConnectionMode.fromPreference(preferences.getString(KEY_CONNECTION_MODE, null))

    private fun isOnboardingCompleted(): Boolean =
        preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    private fun markOnboardingCompleted() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    private fun buildDiagnosticsSnapshot(): GoolvpnDiagnosticSnapshot {
        val connectivity =
            Application.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        val capabilities = network?.let(connectivity::getNetworkCapabilities)
        val transport = when {
            capabilities == null -> "none"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
        val current = _uiState.value
        val backendReachable = runCatching { api.healthCheck() }.getOrDefault(false)
        return GoolvpnDiagnosticSnapshot(
            appVersion = BuildConfig.VERSION_NAME,
            androidVersion = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
            vpnStatus = serviceStatus.name.lowercase(),
            networkTransport = transport,
            networkValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            backendReachable = backendReachable,
            activated = current.activated,
            accessActive = current.active,
            profileReady = current.profileReady,
            connectionMode = current.connectionMode.preferenceValue,
            smartBypassVersion = current.smartBypassGroups.firstOrNull()?.catalogVersion,
            smartBypassGroups = current.smartBypassEnabledGroupIds,
            lastError = current.error,
        )
    }

    private suspend fun applyConnectionMode(
        mode: GoolvpnConnectionMode,
        closeExistingConnections: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        repeat(5) { attempt ->
            try {
                val client = Libbox.newStandaloneCommandClient()
                client.selectOutbound(GOOLVPN_SELECTOR_TAG, mode.outboundTag)
                if (closeExistingConnections) client.closeConnections()
                return@withContext true
            } catch (_: Exception) {
                if (attempt < 4) delay(200)
            }
        }
        false
    }

    private companion object {
        const val KEY_CONNECTION_MODE = "connection_mode"
        const val KEY_SMART_BYPASS_GROUPS = "smart_bypass_groups"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val GOOLVPN_SELECTOR_TAG = "GOOLVPN"
        const val ONBOARDING_AUTOMATIC = 1
        const val ONBOARDING_FAST = 2
        const val ONBOARDING_STABLE = 3
        const val ONBOARDING_SMART_BYPASS = 4
        const val ONBOARDING_APP_ROUTING = 5
        const val ONBOARDING_DIAGNOSTICS = 6
    }
}
