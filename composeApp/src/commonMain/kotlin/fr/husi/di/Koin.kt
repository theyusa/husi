package fr.husi.di

import fr.husi.compose.material3.PlatformMaterialApi
import fr.husi.compose.theme.PlatformThemeApi
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val commonUiModule = module {
    single<PlatformMaterialApi> { platformMaterialApi() }
    single<PlatformThemeApi> { platformThemeApi() }
}

internal expect fun platformMaterialApi(): PlatformMaterialApi
internal expect fun platformThemeApi(): PlatformThemeApi

fun initHusiKoin() {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        modules(commonUiModule)
    }
}
