package fr.husi.di

import fr.husi.compose.material3.DesktopPlatformMaterialApi
import fr.husi.compose.material3.PlatformMaterialApi
import fr.husi.compose.theme.PlatformThemeApi
import fr.husi.compose.theme.standardPlatformThemeApi
import org.koin.core.module.Module

internal actual fun platformMaterialApi(): PlatformMaterialApi = DesktopPlatformMaterialApi

internal actual fun platformThemeApi(): PlatformThemeApi = standardPlatformThemeApi()

internal actual fun platformKoinModules(): List<Module> = emptyList()
