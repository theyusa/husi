@file:OptIn(KoinDelicateAPI::class, KoinExperimentalAPI::class)

package fr.husi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import fr.husi.Key
import fr.husi.bg.DeepLinkDispatcher
import fr.husi.compose.theme.AppTheme
import fr.husi.database.DataStore
import fr.husi.permission.LocalPermissionPlatform
import fr.husi.permission.rememberAndroidPermissionPlatform
import fr.husi.repository.repo
import fr.husi.service.ServiceConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityRetainedScope
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.annotation.KoinDelicateAPI
import org.koin.core.annotation.KoinExperimentalAPI

class MainActivity : ComposeActivity(), AndroidScopeComponent {

    override val scope by activityRetainedScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ServiceConnector.connect()
        lifecycleScope.launch(Dispatchers.IO) {
            DataStore.configurationStore
                .stringFlow(Key.SERVICE_MODE, Key.MODE_VPN)
                .drop(1)
                .collect {
                    if (DataStore.serviceState.started) {
                        repo.reloadService()
                        ServiceConnector.reconnect()
                    }
                }
        }

        when (intent.action) {
            Intent.ACTION_VIEW -> onNewIntent(intent)
            else -> {}
        }

        setContent {
            val permissionPlatform = rememberAndroidPermissionPlatform()
            UnboundKoinScope(scope) {
                CompositionLocalProvider(
                    LocalPermissionPlatform provides permissionPlatform,
                ) {
                    AppTheme {
                        MainScreen(
                            moveToBackground = { moveTaskToBack(true) },
                            initialProcessText = intent
                                .takeIf { it.action == Intent.ACTION_PROCESS_TEXT }
                                ?.getStringExtra(Intent.EXTRA_PROCESS_TEXT),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val uri = intent.data ?: return
        DeepLinkDispatcher.emit(uri.toString())
    }

    override fun onStart() {
        ServiceConnector.updateConnectionId(ServiceConnector.connectionIdMainActivityForeground)
        super.onStart()
    }

    override fun onStop() {
        ServiceConnector.updateConnectionId(ServiceConnector.connectionIdMainActivityBackground)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceConnector.disconnect()
    }

}
