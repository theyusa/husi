package fr.husi.fmt

import fr.husi.database.DataStore
import fr.husi.platform.PlatformInfo

internal actual fun SingBoxOptions.Inbound_TunOptions.applyPlatformConfig() {
    auto_route = true
    if (DataStore.tunStrictRoute) {
        strict_route = true
    }
    if (PlatformInfo.isLinux && DataStore.tunAutoRedirect) {
        auto_redirect = true
    }
}
