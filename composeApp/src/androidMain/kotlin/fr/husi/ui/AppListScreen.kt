@file:OptIn(ExperimentalLayoutApi::class)

package fr.husi.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.husi.compose.BackHandler
import fr.husi.compose.SimpleIconButton
import fr.husi.compose.setPlainText
import fr.husi.repository.FakeRepository
import fr.husi.repository.repo
import fr.husi.resources.Res
import fr.husi.resources.action_copy
import fr.husi.resources.action_import
import fr.husi.resources.cleaning_services
import fr.husi.resources.clear_selections
import fr.husi.resources.close
import fr.husi.resources.content_paste
import fr.husi.resources.copy_all
import fr.husi.resources.copy_success
import fr.husi.resources.fiber_smart_record
import fr.husi.resources.invert_selections
import fr.husi.resources.more
import fr.husi.resources.more_vert
import fr.husi.resources.ok
import fr.husi.resources.search
import fr.husi.resources.select_apps
import fr.husi.results.LocalResultEventBus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.random.Random

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal actual fun AppListScreen(
    initialPackages: Set<String>,
    resultKey: String,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val resultBus = LocalResultEventBus.current
    val viewModel: AppListViewModel = viewModel(key = resultKey) { AppListViewModel() }
    val context = LocalContext.current
    LaunchedEffect(viewModel, initialPackages) {
        viewModel.initialize(context.packageManager, initialPackages)
    }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = getStringOrRes(message),
                    actionLabel = repo.getString(Res.string.ok),
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    fun saveAndExit() {
        resultBus.sendResult<Set<String>>(resultKey, viewModel.allPackages().toSet())
        onBack()
    }
    BackHandler(enabled = true) {
        saveAndExit()
    }

    val searchBarState = rememberSearchBarState()
    val textFieldState = viewModel.textFieldState

    var showMoreActions by remember { mutableStateOf(false) }

    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val colors = TopAppBarDefaults.topAppBarColors()
            val isScrolled = scrollBehavior.state.overlappedFraction > 0
            val containerColor = if (isScrolled) {
                colors.scrolledContainerColor
            } else {
                colors.containerColor
            }

            Surface(
                color = containerColor,
            ) {
                Column {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.select_apps)) },
                        navigationIcon = {
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.close),
                                contentDescription = stringResource(Res.string.close),
                                onClick = ::saveAndExit,
                            )
                        },
                        actions = {
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.copy_all),
                                contentDescription = stringResource(Res.string.action_copy),
                                onClick = {
                                    val toExport = viewModel.export()
                                    scope.launch {
                                        clipboard.setPlainText(toExport)
                                        snackbarHostState.showSnackbar(
                                            message = repo.getString(Res.string.copy_success),
                                            actionLabel = repo.getString(Res.string.ok),
                                            duration = SnackbarDuration.Short,
                                        )
                                    }
                                },
                            )
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.content_paste),
                                contentDescription = stringResource(Res.string.action_import),
                                onClick = {
                                    scope.launch {
                                        val text = clipboard.getClipEntry()?.clipData
                                            ?.getItemAt(0)?.text
                                            ?.toString()
                                        viewModel.import(text)
                                    }
                                },
                            )
                            Box {
                                SimpleIconButton(
                                    imageVector = vectorResource(Res.drawable.more_vert),
                                    contentDescription = stringResource(Res.string.more),
                                    onClick = { showMoreActions = true },
                                )

                                DropdownMenu(
                                    expanded = showMoreActions,
                                    onDismissRequest = { showMoreActions = false },
                                    shape = MenuDefaults.standaloneGroupShape,
                                    containerColor = MenuDefaults.groupStandardContainerColor,
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.invert_selections)) },
                                        onClick = {
                                            viewModel.invertSections()
                                            showMoreActions = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.fiber_smart_record),
                                                contentDescription = null,
                                            )
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.clear_selections)) },
                                        onClick = {
                                            viewModel.clearSections()
                                            showMoreActions = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.cleaning_services),
                                                contentDescription = null,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                        windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                        scrollBehavior = scrollBehavior,
                    )

                    SearchBar(
                        state = searchBarState,
                        inputField = {
                            SearchBarDefaults.InputField(
                                textFieldState = textFieldState,
                                searchBarState = searchBarState,
                                onSearch = {
                                    scope.launch {
                                        searchBarState.animateToCollapsed()
                                    }
                                },
                                leadingIcon = {
                                    Icon(vectorResource(Res.drawable.search), null)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.isLoading,
            animationSpec = tween(durationMillis = 300),
        ) { isLoading ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            } else {
                AppListContent(
                    apps = uiState.apps,
                    innerPadding = innerPadding,
                    onClick = { viewModel.onItemClick(it) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewAppListScreen() {
    LaunchedEffect(Unit) {
        repo = FakeRepository()
    }

    AppListScreen(
        initialPackages = emptySet(),
        resultKey = "app-list-${Random.nextLong()}",
        onBack = {},
    )
}
