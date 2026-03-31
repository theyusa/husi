package fr.husi.di

import fr.husi.compose.material3.PlatformMaterialApi
import fr.husi.compose.theme.PlatformThemeApi
import fr.husi.ui.ImportLinkInteractor
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val commonUiModule = module {
    single<PlatformMaterialApi> { platformMaterialApi() }
    single<PlatformThemeApi> { platformThemeApi() }
    singleOf(::ImportLinkInteractor)
}

internal expect fun platformMaterialApi(): PlatformMaterialApi
internal expect fun platformThemeApi(): PlatformThemeApi
internal expect fun platformKoinModules(): List<Module>

fun initHusiKoin() {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        modules(listOf(commonUiModule, commonNavigationModule) + platformKoinModules())
    }
}
