package fr.husi.di

import fr.husi.compose.material3.DesktopPlatformMaterialApi
import fr.husi.compose.material3.PlatformMaterialApi
import fr.husi.compose.theme.PlatformThemeApi
import fr.husi.compose.theme.standardPlatformThemeApi

internal actual fun platformMaterialApi(): PlatformMaterialApi = DesktopPlatformMaterialApi

internal actual fun platformThemeApi(): PlatformThemeApi = standardPlatformThemeApi()
