package fr.husi.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.ArraySet
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import fr.husi.compose.extraBottomPadding
import fr.husi.compose.paddingExceptBottom
import fr.husi.ktx.blankAsNull
import fr.husi.repository.androidRepo
import fr.husi.resources.*
import fr.husi.utils.PackageCache
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
internal data class ProxiedApp(
    private val appInfo: ApplicationInfo,
    val packageName: String,
    var isProxied: Boolean,
    val icon: Drawable,
    val name: String, // cached for sorting
) {
    val uid get() = appInfo.uid
}

@Stable
internal abstract class BaseAppListViewModel : ViewModel() {
    val textFieldState = TextFieldState()

    protected lateinit var packageManager: PackageManager
    protected val proxiedUids = ArraySet<Int>()
    protected val singleThreadContext = Dispatchers.IO.limitedParallelism(1)
    protected val cachedApps by lazy {
        PackageCache.installedPackages.toMutableMap().apply {
            remove(androidRepo.context.packageName)
        }
    }

    private val iconCache = mutableMapOf<String, Drawable>()
    private fun loadIcon(packageInfo: PackageInfo): Drawable {
        return iconCache.getOrPut(packageInfo.packageName) {
            packageInfo.applicationInfo!!.loadIcon(packageManager)
        }
    }

    protected abstract fun updateApps(apps: List<ProxiedApp>, isLoading: Boolean)
    protected abstract fun updateSnackbar(message: StringOrRes?)
    protected abstract suspend fun afterMutation()
    protected abstract suspend fun afterItemClick(app: ProxiedApp, newIsProxied: Boolean)
    abstract fun export(): String

    protected fun collectSearchText() {
        viewModelScope.launch {
            snapshotFlow { textFieldState.text.toString() }
                .drop(1)
                .distinctUntilChanged()
                .collect { withContext(singleThreadContext) { reload() } }
        }
    }

    suspend fun reload(cachedApps: Map<String, PackageInfo> = this.cachedApps) {
        val apps = mutableListOf<ProxiedApp>()
        val query = textFieldState.text.toString()
        for ((packageName, packageInfo) in cachedApps) {
            currentCoroutineContext()[Job]!!.ensureActive()

            val applicationInfo = packageInfo.applicationInfo!!
            val name = applicationInfo.loadLabel(packageManager).toString()
            query.blankAsNull()?.lowercase()?.let {
                val hit = packageName.lowercase().contains(it)
                        || name.lowercase().contains(it)
                        || applicationInfo.uid.toString().contains(it)
                if (!hit) continue
            }

            apps.add(
                ProxiedApp(
                    appInfo = applicationInfo,
                    packageName = packageName,
                    isProxied = proxiedUids.contains(applicationInfo.uid),
                    icon = loadIcon(packageInfo),
                    name = name,
                )
            )
        }
        apps.sortWith(compareBy({ !it.isProxied }, { it.name }))
        updateApps(apps, false)
    }

    fun invertSections() = viewModelScope.launch(singleThreadContext) {
        val current = ArraySet(proxiedUids)
        val cachedApps = cachedApps
        val allUids = ArraySet<Int>()
        for ((_, packageInfo) in cachedApps) {
            allUids.add(packageInfo.applicationInfo!!.uid)
        }
        proxiedUids.clear()
        proxiedUids.ensureCapacity(cachedApps.size - current.size)
        allUids.filter { it !in current }.forEach { proxiedUids.add(it) }
        afterMutation()
    }

    fun clearSections() = viewModelScope.launch(singleThreadContext) {
        proxiedUids.clear()
        afterMutation()
    }

    fun import(raw: String?) = viewModelScope.launch(singleThreadContext) {
        if (raw?.blankAsNull() == null) {
            updateSnackbar(StringOrRes.Res(Res.string.action_import_err))
            return@launch
        }
        var bypass = false
        val apps = raw.lineSequence().let {
            when (it.firstOrNull()) {
                "false" -> {
                    bypass = false
                    it.drop(1)
                }

                "true" -> {
                    bypass = true
                    it.drop(1)
                }

                else -> it
            }
        }
        applyImport(bypass, apps)
        updateSnackbar(StringOrRes.Res(Res.string.action_import_msg))
        afterMutation()
    }

    protected open suspend fun applyImport(bypass: Boolean, apps: Sequence<String>) {
        proxiedUids.clear()
        val cachedApps = cachedApps
        if (bypass) {
            val bypassSet = apps.toSet()
            for ((packageName, packageInfo) in cachedApps) {
                if (packageName !in bypassSet) {
                    proxiedUids.add(packageInfo.applicationInfo!!.uid)
                }
            }
        } else {
            for (packageName in apps) {
                val info = cachedApps[packageName] ?: continue
                proxiedUids.add(info.applicationInfo!!.uid)
            }
        }
    }

    fun onItemClick(app: ProxiedApp) = viewModelScope.launch(singleThreadContext) {
        val newIsProxied = !proxiedUids.remove(app.uid)
        if (newIsProxied) {
            proxiedUids.add(app.uid)
        }
        afterItemClick(app, newIsProxied)
    }
}

@Composable
internal fun AppListContent(
    apps: List<ProxiedApp>,
    innerPadding: PaddingValues,
    onClick: (ProxiedApp) -> Unit,
    scrollState: LazyListState = rememberLazyListState(),
) {
    Row(
        modifier = Modifier.paddingExceptBottom(innerPadding),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            state = scrollState,
            contentPadding = extraBottomPadding(),
        ) {
            items(
                items = apps,
                key = { it.packageName },
                contentType = { 0 },
            ) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = rememberDrawablePainter(app.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 12.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .width(0.dp),
                        ) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${app.packageName} (${app.uid})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = app.isProxied,
                            onCheckedChange = { onClick(app) },
                        )
                    }
                }
            }
        }

        Box {
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = scrollState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 16.dp,
                ),
            )
        }
    }
}
