package fr.husi.di

import fr.husi.compose.material3.PlatformMaterialApi
import fr.husi.compose.material3.TvPlatformMaterialApi
import fr.husi.compose.material3.standardPlatformMaterialApi
import fr.husi.compose.theme.PlatformThemeApi
import fr.husi.compose.theme.TvPlatformThemeApi
import fr.husi.compose.theme.standardPlatformThemeApi
import fr.husi.repository.repo
import org.koin.core.module.Module

internal actual fun platformMaterialApi(): PlatformMaterialApi {
    return if (repo.isTv) {
        TvPlatformMaterialApi
    } else {
        standardPlatformMaterialApi()
    }
}

internal actual fun platformThemeApi(): PlatformThemeApi {
    return if (repo.isTv) {
        TvPlatformThemeApi
    } else {
        standardPlatformThemeApi()
    }
}

internal actual fun platformKoinModules(): List<Module> = listOf(androidNavigationModule)
