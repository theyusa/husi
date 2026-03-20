package fr.husi.ui

import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import fr.husi.Key
import fr.husi.database.DataStore
import fr.husi.utils.AppScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
internal data class AppManagerUiState(
    val mode: ProxyMode = ProxyMode.DISABLED,
    val isLoading: Boolean = false,
    val apps: List<ProxiedApp> = emptyList(), // sorted, full
    val filteredApps: List<ProxiedApp> = emptyList(), // sorted, filtered by search
    val scanned: List<String>? = null,
    val scanProcess: Float? = null,
    val snackbarMessage: StringOrRes? = null,
    val shouldFinish: Boolean = false,
)

@Immutable
internal enum class ProxyMode {
    DISABLED,
    PROXY,
    BYPASS,
}

@Stable
internal class AppManagerViewModel : BaseAppListViewModel() {
    private val _uiState = MutableStateFlow(AppManagerUiState())
    val uiState = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun initialize(pm: PackageManager) {
        if (!DataStore.proxyApps) {
            DataStore.proxyApps = true
        }
        _uiState.update { it.copy(mode = currentProxyMode()) }
        packageManager = pm
        _uiState.update {
            it.copy(isLoading = true, apps = emptyList())
        }
        viewModelScope.launch(singleThreadContext) {
            DataStore.configurationStore.stringSetFlow(Key.PACKAGES).collect { packages ->
                proxiedUids.clear()
                val cachedApps = cachedApps
                for ((packageName, packageInfo) in cachedApps) {
                    if (packages.contains(packageName)) {
                        proxiedUids.add(packageInfo.applicationInfo!!.uid)
                    }
                }
                reload(cachedApps)
            }
        }
        collectSearchText()
    }

    override fun updateApps(apps: List<ProxiedApp>, filteredApps: List<ProxiedApp>, isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading, apps = apps, filteredApps = filteredApps) }
    }

    override fun updateSnackbar(message: StringOrRes?) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    override suspend fun afterMutation() = writeToDataStore()

    override suspend fun applyImport(bypass: Boolean, apps: Sequence<String>) {
        DataStore.bypassMode = bypass
        super.applyImport(bypass, apps)
    }

    override suspend fun afterItemClick(app: ProxiedApp, newIsProxied: Boolean) = writeToDataStore()

    override fun export(): String {
        val body = DataStore.packages.joinToString("\n")
        return "${DataStore.bypassMode}\n${body}"
    }

    fun scanChinaApps() {
        scanJob = viewModelScope.launch(singleThreadContext) {
            val cachedApps = cachedApps
            val bypass = DataStore.bypassMode
            _uiState.update {
                it.copy(
                    scanned = emptyList(),
                    scanProcess = null,
                )
            }
            for ((packageName, packageInfo) in cachedApps) {
                if (!isActive) {
                    _uiState.update {
                        it.copy(
                            scanned = null,
                            scanProcess = null,
                        )
                    }
                    return@launch
                }

                val old = _uiState.value
                val scanned = old.scanned!! + packageName
                _uiState.update {
                    it.copy(
                        scanned = scanned,
                        scanProcess = (scanned.size.toDouble() / cachedApps.size.toDouble()).toFloat(),
                    )
                }

                val appInfo = packageInfo.applicationInfo!!
                if (AppScanner.isChinaApp(packageName, packageManager)) {
                    if (bypass) {
                        proxiedUids.add(appInfo.uid)
                    } else {
                        proxiedUids.remove(appInfo.uid)
                    }
                } else {
                    if (!bypass) {
                        proxiedUids.add(appInfo.uid)
                    } else {
                        proxiedUids.remove(appInfo.uid)
                    }
                }
            }
            writeToDataStore()
            _uiState.emit(_uiState.value.copy(scanned = null, scanProcess = null))
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
    }

    private suspend fun writeToDataStore() {
        DataStore.packages = cachedApps.values.asSequence()
            .filter { it.applicationInfo!!.uid in proxiedUids }
            .map { it.packageName }
            .toCollection(LinkedHashSet())
    }

    private fun currentProxyMode(): ProxyMode {
        return if (!DataStore.proxyApps) {
            ProxyMode.DISABLED
        } else if (DataStore.bypassMode) {
            ProxyMode.BYPASS
        } else {
            ProxyMode.PROXY
        }
    }

    fun setProxyMode(mode: ProxyMode) = viewModelScope.launch {
        when (mode) {
            ProxyMode.DISABLED -> {
                DataStore.proxyApps = false
                _uiState.update { state ->
                    state.copy(
                        shouldFinish = true,
                    )
                }
            }

            ProxyMode.BYPASS -> {
                DataStore.proxyApps = true
                DataStore.bypassMode = true
            }

            ProxyMode.PROXY -> {
                DataStore.proxyApps = true
                DataStore.bypassMode = false
            }
        }
        _uiState.update { it.copy(mode = mode) }
    }
}
