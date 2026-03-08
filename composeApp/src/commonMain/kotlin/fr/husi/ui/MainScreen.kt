package fr.husi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import fr.husi.AlertType
import fr.husi.bg.Alert
import fr.husi.bg.BackendState
import fr.husi.bg.Executable
import fr.husi.bg.ServiceState
import fr.husi.compose.BackHandler
import fr.husi.compose.BoxedVerticalScrollbar
import fr.husi.compose.DrawerCompat
import fr.husi.compose.TextButton
import fr.husi.compose.drawerIsCollapsible
import fr.husi.database.SagerDatabase
import fr.husi.fmt.PluginEntry
import fr.husi.ktx.restartApplication
import fr.husi.ktx.runOnDefaultDispatcher
import fr.husi.permission.AppPermission
import fr.husi.permission.LocalPermissionPlatform
import fr.husi.repository.repo
import fr.husi.resources.Res
import fr.husi.resources.action_download
import fr.husi.resources.bug_report
import fr.husi.resources.cancel
import fr.husi.resources.close
import fr.husi.resources.construction
import fr.husi.resources.data_usage
import fr.husi.resources.description
import fr.husi.resources.directions
import fr.husi.resources.document
import fr.husi.resources.error
import fr.husi.resources.fast_rewind
import fr.husi.resources.have_a_nice_day
import fr.husi.resources.info
import fr.husi.resources.location_permission_description
import fr.husi.resources.location_permission_title
import fr.husi.resources.menu_about
import fr.husi.resources.menu_configuration
import fr.husi.resources.menu_dashboard
import fr.husi.resources.menu_group
import fr.husi.resources.menu_log
import fr.husi.resources.menu_route
import fr.husi.resources.menu_tools
import fr.husi.resources.missing_plugin
import fr.husi.resources.nfc
import fr.husi.resources.no_thanks
import fr.husi.resources.ok
import fr.husi.resources.permission_denied
import fr.husi.resources.plugin
import fr.husi.resources.plugin_unknown
import fr.husi.resources.query_package_denied
import fr.husi.resources.question_mark
import fr.husi.resources.settings
import fr.husi.resources.transform
import fr.husi.resources.view_list
import fr.husi.resources.warning_amber
import fr.husi.results.LocalResultEventBus
import fr.husi.results.ResultEventBus
import fr.husi.ui.configuration.ConfigurationScreen
import fr.husi.ui.configuration.ProfileSelectSheet
import fr.husi.ui.dashboard.ConnectionDetailScreen
import fr.husi.ui.dashboard.DashboardScreen
import fr.husi.ui.profile.ConfigEditScreen
import fr.husi.ui.profile.ProfileEditorScreen
import fr.husi.ui.tools.GetCertScreen
import fr.husi.ui.tools.RuleSetMatchScreen
import fr.husi.ui.tools.SpeedtestScreen
import fr.husi.ui.tools.StunScreen
import fr.husi.ui.tools.ToolsScreen
import fr.husi.ui.tools.VPNScannerScreen
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    moveToBackground: () -> Unit,
) {
    val permission = LocalPermissionPlatform.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val savedStateConfiguration = remember { NavRoutes.savedStateConfiguration }
    val backStack = rememberNavBackStack(savedStateConfiguration, NavRoutes.Configuration)
    val resultBus = remember { ResultEventBus() }
    val canCollapseDrawer = drawerIsCollapsible()
    val drawerState = rememberDrawerState(
        if (canCollapseDrawer) {
            DrawerValue.Closed
        } else {
            DrawerValue.Open
        },
    )
    val currentRoute = backStack.lastOrNull() as? NavRoutes
    val isAtStartDestination = currentRoute == NavRoutes.Configuration
    val serviceStatus by BackendState.status.collectAsStateWithLifecycle()

    fun closeDrawer() {
        if (canCollapseDrawer) {
            scope.launch { drawerState.close() }
        }
    }

    fun popBackStack(): Boolean {
        if (backStack.size <= 1) {
            return false
        }
        backStack.removeLastOrNull()
        return true
    }

    fun navigateTo(route: NavRoutes) {
        backStack.add(route)
    }

    fun navigateToDrawerRoute(route: NavRoutes) {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    /**
     * Check query packages permission for rogue vendors.
     * If we don't query for `com.android.permission.GET_INSTALLED_APPS` permission,
     * only when we query all packages in foreground will pop the permission window for query permission.
     * @see <a href="https://www.taf.org.cn/upload/AssociationStandard/TTAF%20108-2022%20%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E5%BA%94%E7%94%A8%E8%BD%AF%E4%BB%B6%E5%88%97%E8%A1%A8%E6%9D%83%E9%99%90%E5%AE%9E%E6%96%BD%E6%8C%87%E5%8D%97.pdf">移动终端应用软件列表权限实施指南</a>
     */
    var showQueryPackageDeniedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (permission.canRequestPermission(AppPermission.QueryInstalledApps) &&
            !permission.hasPermission(AppPermission.QueryInstalledApps)
        ) {
            permission.requestPermission(AppPermission.QueryInstalledApps) { granted ->
                if (granted) runOnDefaultDispatcher {
                    repo.stopService()
                    delay(500)
                    SagerDatabase.instance.close()
                    Executable.killAll(true)
                    restartApplication()
                } else {
                    showQueryPackageDeniedDialog = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasPostNotification =
            permission.hasPermission(AppPermission.PostNotifications)
        if (!hasPostNotification) {
            permission.requestPermission(AppPermission.PostNotifications)
        }
    }

    BackHandler(enabled = true) {
        when {
            canCollapseDrawer && drawerState.isOpen -> scope.launch { drawerState.close() }

            !isAtStartDestination -> {
                val popped = popBackStack()
                if (!popped) {
                    navigateToDrawerRoute(NavRoutes.Configuration)
                }
            }

            else -> moveToBackground()
        }
    }

    LaunchedEffect(serviceStatus.state) {
        if (serviceStatus.state != ServiceState.Connected) {
            viewModel.resetUrlTestStatus()
        }
    }

    var showServiceAlert by remember { mutableStateOf<Alert?>(null) }

    LaunchedEffect(Unit) {
        BackendState.alerts.collect { alert ->
            if (alert.type == AlertType.COMMON) {
                if (alert.message.isNotBlank()) {
                    viewModel.showSnackbar(StringOrRes.Direct(alert.message))
                }
            } else {
                showServiceAlert = alert
            }
        }
    }

    DrawerCompat(
        drawerState = drawerState,
        drawerContent = {
            @Composable
            fun BuildDrawerItem(info: DrawerItemInfo) {
                DrawerItem(
                    info = info,
                    closeDrawer = ::closeDrawer,
                    currentRoute = currentRoute,
                    onNavigate = ::navigateToDrawerRoute,
                )
            }

            val dividerPadding = 4.dp
            val items0 = remember {
                persistentListOf(
                    DrawerItemInfo(
                        Res.string.menu_configuration,
                        Res.drawable.description,
                        NavRoutes.Configuration,
                    ),
                    DrawerItemInfo(
                        Res.string.menu_group,
                        Res.drawable.view_list,
                        NavRoutes.Groups,
                    ),
                    DrawerItemInfo(
                        Res.string.menu_route,
                        Res.drawable.directions,
                        NavRoutes.Route,
                    ),
                    DrawerItemInfo(
                        Res.string.settings,
                        Res.drawable.settings,
                        NavRoutes.Settings,
                    ),
                    DrawerItemInfo(
                        Res.string.plugin,
                        Res.drawable.nfc,
                        NavRoutes.Plugin,
                    ),
                )
            }
            for (info in items0) BuildDrawerItem(info)
            HorizontalDivider(modifier = Modifier.padding(vertical = dividerPadding))
            val items1 = remember {
                persistentListOf(
                    DrawerItemInfo(Res.string.menu_log, Res.drawable.bug_report, NavRoutes.Log),
                    DrawerItemInfo(
                        Res.string.menu_dashboard,
                        Res.drawable.transform,
                        NavRoutes.Dashboard,
                    ),
                    DrawerItemInfo(
                        Res.string.menu_tools,
                        Res.drawable.construction,
                        NavRoutes.Tools,
                    ),
                )
            }
            for (info in items1) BuildDrawerItem(info)
            HorizontalDivider(modifier = Modifier.padding(vertical = dividerPadding))
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.document)) },
                selected = false,
                onClick = {
                    closeDrawer()
                    uriHandler.openUri("https://codeberg.org/xchacha20-poly1305/husi/wiki")
                },
                modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = {
                    Icon(vectorResource(Res.drawable.data_usage), null)
                },
            )
            BuildDrawerItem(
                DrawerItemInfo(
                    Res.string.menu_about,
                    Res.drawable.info,
                    NavRoutes.About,
                ),
            )
            if (canCollapseDrawer) {
                HorizontalDivider(modifier = Modifier.padding(vertical = dividerPadding))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NavigationDrawerItemDefaults.ItemPadding),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val tooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above,
                        ),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(Res.string.close))
                            }
                        },
                        state = tooltipState,
                    ) {
                        IconButton(
                            onClick = ::closeDrawer,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.fast_rewind),
                                contentDescription = stringResource(Res.string.close),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        fun onDrawerClick() {
            if (!canCollapseDrawer) {
                return
            }
            scope.launch {
                if (drawerState.isOpen) {
                    drawerState.close()
                } else {
                    drawerState.open()
                }
            }
        }

        CompositionLocalProvider(
            LocalResultEventBus provides resultBus,
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = ::popBackStack,
                entryProvider = entryProvider(
                    fallback = { key ->
                        error("Unknown route: $key")
                    },
                ) {
                    entry<NavRoutes.Configuration> {
                        ConfigurationScreen(
                            mainViewModel = viewModel,
                            onNavigationClick = ::onDrawerClick,
                            selectCallback = null,
                            preSelected = null,
                            onOpenProfileEditor = { navigateTo(it) },
                        )
                    }

                    entry<NavRoutes.Groups> {
                        GroupScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                            openGroupSettings = { groupId ->
                                navigateTo(NavRoutes.GroupSettings(groupId = groupId))
                            },
                        )
                    }

                    entry<NavRoutes.Route> {
                        RouteScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                            openRouteSettings = { routeId ->
                                navigateTo(NavRoutes.RouteSettings(routeId = routeId))
                            },
                            openAssets = {
                                navigateTo(NavRoutes.Assets)
                            },
                        )
                    }

                    entry<NavRoutes.Settings> {
                        SettingsScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                            openAppManager = { navigateTo(NavRoutes.AppManager) },
                        )
                    }

                    entry<NavRoutes.Plugin> {
                        PluginScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                        )
                    }

                    entry<NavRoutes.Log> {
                        LogcatScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                        )
                    }

                    entry<NavRoutes.Dashboard> {
                        DashboardScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                            openConnectionDetail = { uuid ->
                                navigateTo(NavRoutes.ConnectionsDetail(uuid = uuid))
                            },
                        )
                    }

                    entry<NavRoutes.ConnectionsDetail> { key ->
                        ConnectionDetailScreen(
                            uuid = key.uuid,
                            popup = ::popBackStack,
                            openRouteSettings = { initialState ->
                                navigateTo(
                                    NavRoutes.RouteSettings(
                                        routeId = -1L,
                                        useDraft = true,
                                        initialState = initialState,
                                    ),
                                )
                            },
                        )
                    }

                    entry<NavRoutes.ProfileEditor> { route ->
                        ProfileEditorScreen(
                            type = route.type,
                            profileId = route.id,
                            isSubscription = route.subscription,
                            onOpenProfileSelect = { navigateTo(it) },
                            onOpenConfigEditor = { navigateTo(it) },
                            onResult = { updated ->
                                resultBus.sendResult(route.resultKey, updated)
                                popBackStack()
                            },
                        )
                    }

                    entry<NavRoutes.ProfileSelect> { route ->
                        ProfileSelectSheet(
                            mainViewModel = viewModel,
                            preSelected = route.preSelected,
                            onDismiss = ::popBackStack,
                            onSelected = { id ->
                                resultBus.sendResult<Long?>(route.resultKey, id)
                                popBackStack()
                            },
                        )
                    }

                    entry<NavRoutes.AppManager> {
                        AppManagerScreen(
                            onBackPress = { popBackStack() },
                        )
                    }

                    entry<NavRoutes.GroupSettings> { key ->
                        GroupSettingsScreen(
                            groupId = key.groupId,
                            onBackPress = { popBackStack() },
                            onOpenProfileSelect = { navigateTo(it) },
                        )
                    }

                    entry<NavRoutes.RouteSettings> { key ->
                        val initialState = if (key.useDraft) key.initialState else null
                        RouteSettingsScreen(
                            routeId = key.routeId,
                            initialState = initialState,
                            onBackPress = { popBackStack() },
                            onSaved = ::popBackStack,
                            onOpenProfileSelect = { navigateTo(it) },
                            onOpenAppList = { navigateTo(it) },
                            onOpenConfigEditor = { navigateTo(it) },
                        )
                    }

                    entry<NavRoutes.AppList> { route ->
                        AppListScreen(
                            initialPackages = route.initialPackages,
                            resultKey = route.resultKey,
                            onBack = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.ConfigEditor> { route ->
                        ConfigEditScreen(
                            initialText = route.initialText,
                            resultKey = route.resultKey,
                            onBack = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.Assets> {
                        AssetsScreen(
                            onBackPress = { popBackStack() },
                            onOpenAssetEditor = { route ->
                                navigateTo(route)
                            },
                        )
                    }

                    entry<NavRoutes.AssetEdit> { key ->
                        AssetEditScreen(
                            assetName = key.assetName,
                            resultKey = key.resultKey,
                            onBack = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.Tools> {
                        ToolsScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                            onOpenTool = { toolsRoute ->
                                navigateTo(toolsRoute)
                            },
                        )
                    }

                    entry<NavRoutes.ToolsPage.Stun> {
                        StunScreen(
                            onBackPress = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.ToolsPage.GetCert> {
                        GetCertScreen(
                            onBack = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.ToolsPage.VPNScanner> {
                        VPNScannerScreen(
                            onBackPress = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.ToolsPage.SpeedTest> {
                        SpeedtestScreen(
                            onBackPress = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.ToolsPage.RuleSetMatch> {
                        RuleSetMatchScreen(
                            onBackPress = ::popBackStack,
                        )
                    }

                    entry<NavRoutes.About> {
                        AboutScreen(
                            mainViewModel = viewModel,
                            onDrawerClick = ::onDrawerClick,
                            onNavigateToLibraries = { navigateTo(NavRoutes.Libraries) },
                        )
                    }

                    entry<NavRoutes.Libraries> {
                        LibrariesScreen(
                            onBackPress = ::popBackStack,
                        )
                    }
                },
            )

        }
    }

    if (showQueryPackageDeniedDialog) AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                permission.openPermissionSettings()
                showQueryPackageDeniedDialog = false
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.no_thanks)) {
                showQueryPackageDeniedDialog = false
                viewModel.showSnackbar(StringOrRes.Res(Res.string.have_a_nice_day))
            }
        },
        icon = {
            Icon(vectorResource(Res.drawable.warning_amber), null)
        },
        title = { Text(stringResource(Res.string.permission_denied)) },
        text = { Text(stringResource(Res.string.query_package_denied)) },
    )

    if (showServiceAlert != null) {
        val alert = showServiceAlert!!
        when (alert.type) {
            AlertType.MISSING_PLUGIN -> {
                val pluginName = alert.message
                val plugin = PluginEntry.find(pluginName)
                if (plugin == null) {
                    showServiceAlert = null
                    viewModel.showSnackbar(
                        StringOrRes.ResWithParams(Res.string.plugin_unknown, pluginName),
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { showServiceAlert = null },
                        confirmButton = {
                            TextButton(stringResource(Res.string.action_download)) {
                                showServiceAlert = null
                                uriHandler.openUri(
                                    if (repo.isAndroid) {
                                        plugin.downloadSource.apk
                                    } else {
                                        plugin.downloadSource.binary
                                    },
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(stringResource(Res.string.cancel)) {
                                showServiceAlert = null
                            }
                        },
                        icon = { Icon(vectorResource(Res.drawable.error), null) },
                        title = { Text(stringResource(plugin.displayName)) },
                        text = { Text(stringResource(Res.string.missing_plugin)) },
                    )
                }
            }

            AlertType.NEED_WIFI_PERMISSION -> {
                AlertDialog(
                    onDismissRequest = { showServiceAlert = null },
                    confirmButton = {
                        TextButton(stringResource(Res.string.ok)) {
                            showServiceAlert = null
                            permission.requestPermission(AppPermission.WifiInfo)
                        }
                    },
                    dismissButton = {
                        TextButton(stringResource(Res.string.no_thanks)) {
                            showServiceAlert = null
                        }
                    },
                    icon = { Icon(vectorResource(Res.drawable.warning_amber), null) },
                    title = { Text(stringResource(Res.string.location_permission_title)) },
                    text = { Text(stringResource(Res.string.location_permission_description)) },
                )
            }
        }
    }

    var showAlertDialog by remember { mutableStateOf<MainViewModelUiEvent.AlertDialog?>(null) }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is MainViewModelUiEvent.AlertDialog -> showAlertDialog = event

                else -> {}
            }
        }
    }
    if (showAlertDialog != null) {
        val dialog = showAlertDialog!!
        AlertDialog(
            onDismissRequest = { showAlertDialog = null },
            confirmButton = {
                TextButton(stringOrRes(dialog.confirmButton.label)) {
                    dialog.confirmButton.onClick()
                    showAlertDialog = null
                }
            },
            dismissButton = dialog.dismissButton?.let { button ->
                {
                    TextButton(stringOrRes(button.label)) {
                        button.onClick()
                        showAlertDialog = null
                    }
                }
            },
            icon = {
                Icon(
                    vectorResource(
                        if (dialog.dismissButton != null) {
                            Res.drawable.question_mark
                        } else {
                            Res.drawable.error
                        },
                    ),
                    null,
                )
            },
            title = { Text(stringOrRes(dialog.title)) },
            text = {
                val scrollState = rememberScrollState()
                Row {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState),
                    ) {
                        Text(stringOrRes(dialog.message))
                    }

                    BoxedVerticalScrollbar(
                        modifier = Modifier.fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                        style = defaultMaterialScrollbarStyle().copy(
                            thickness = 12.dp,
                        ),
                    )
                }
            },
        )
    }
}

@Immutable
private data class DrawerItemInfo(
    val label: StringResource,
    val icon: DrawableResource,
    val route: NavRoutes,
)

@Composable
private fun DrawerItem(
    modifier: Modifier = Modifier,
    info: DrawerItemInfo,
    closeDrawer: () -> Unit,
    currentRoute: NavRoutes?,
    onNavigate: (NavRoutes) -> Unit,
) {
    val selected = currentRoute.matchesRoute(info.route)
    NavigationDrawerItem(
        label = { Text(stringResource(info.label)) },
        selected = selected,
        onClick = {
            closeDrawer()
            if (!selected) {
                onNavigate(info.route)
            }
        },
        modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        icon = {
            Icon(vectorResource(info.icon), null)
        },
    )
}

private fun NavRoutes?.matchesRoute(
    route: NavRoutes,
): Boolean {
    val current = this ?: return false
    return current::class == route::class
}
