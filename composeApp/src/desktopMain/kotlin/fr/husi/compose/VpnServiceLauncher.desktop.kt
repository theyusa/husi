package fr.husi.compose

import androidx.compose.runtime.Composable
import fr.husi.repository.resolveRepository

@Composable
actual fun rememberVpnServiceLauncher(onFailed: () -> Unit): () -> Unit {
    return { resolveRepository().startService() }
}
