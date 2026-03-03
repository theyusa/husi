package fr.husi.ui

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed class NavRoutes : NavKey {

    companion object {
        val savedStateConfiguration
            get() = SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(Configuration::class, Configuration.serializer())
                        subclass(Groups::class, Groups.serializer())
                        subclass(Route::class, Route.serializer())
                        subclass(Settings::class, Settings.serializer())
                        subclass(Plugin::class, Plugin.serializer())
                        subclass(Log::class, Log.serializer())
                        subclass(Dashboard::class, Dashboard.serializer())
                        subclass(Tools::class, Tools.serializer())
                        subclass(ToolsPage.Stun::class, ToolsPage.Stun.serializer())
                        subclass(ToolsPage.GetCert::class, ToolsPage.GetCert.serializer())
                        subclass(ToolsPage.VPNScanner::class, ToolsPage.VPNScanner.serializer())
                        subclass(ToolsPage.SpeedTest::class, ToolsPage.SpeedTest.serializer())
                        subclass(ToolsPage.RuleSetMatch::class, ToolsPage.RuleSetMatch.serializer())
                        subclass(About::class, About.serializer())
                        subclass(Libraries::class, Libraries.serializer())
                        subclass(ProfileEditor::class, ProfileEditor.serializer())
                        subclass(ConnectionsDetail::class, ConnectionsDetail.serializer())
                        subclass(AppManager::class, AppManager.serializer())
                        subclass(Assets::class, Assets.serializer())
                        subclass(AppList::class, AppList.serializer())
                        subclass(ConfigEditor::class, ConfigEditor.serializer())
                        subclass(AssetEdit::class, AssetEdit.serializer())
                        subclass(GroupSettings::class, GroupSettings.serializer())
                        subclass(RouteSettings::class, RouteSettings.serializer())
                        subclass(ProfileSelect::class, ProfileSelect.serializer())
                    }
                }
            }
    }

    @Serializable
    data object Configuration : NavRoutes()

    @Serializable
    data object Groups : NavRoutes()

    @Serializable
    data object Route : NavRoutes()

    @Serializable
    data object Settings : NavRoutes()

    @Serializable
    data object Plugin : NavRoutes()

    @Serializable
    data object Log : NavRoutes()

    @Serializable
    data object Dashboard : NavRoutes()

    @Serializable
    data object Tools : NavRoutes()

    @Serializable
    sealed class ToolsPage : NavRoutes() {
        @Serializable
        data object Stun : ToolsPage()

        @Serializable
        data object GetCert : ToolsPage()

        @Serializable
        data object VPNScanner : ToolsPage()

        @Serializable
        data object SpeedTest : ToolsPage()

        @Serializable
        data object RuleSetMatch : ToolsPage()

    }

    @Serializable
    data object About : NavRoutes()

    @Serializable
    data object Libraries : NavRoutes()

    @Serializable
    data class ProfileEditor(
        val type: Int,
        val id: Long = -1L,
        val subscription: Boolean = false,
    ) : NavRoutes() {
        @Transient
        var onResult: ((Boolean) -> Unit)? = null
    }

    @Serializable
    data class ConnectionsDetail(
        val uuid: String,
    ) : NavRoutes()

    @Serializable
    data object AppManager : NavRoutes()

    @Serializable
    data object Assets : NavRoutes()

    @Serializable
    data class AppList(
        val initialPackages: Set<String> = emptySet(),
    ) : NavRoutes() {
        @Transient
        var onResult: ((Set<String>) -> Unit)? = null
    }

    @Serializable
    data class ConfigEditor(
        val initialText: String = "",
    ) : NavRoutes() {
        @Transient
        var onResult: ((String) -> Unit)? = null
    }

    @Serializable
    data class AssetEdit(
        val assetName: String = "",
    ) : NavRoutes() {
        @Transient
        var onFinished: ((AssetEditResult) -> Unit)? = null
    }

    @Serializable
    data class GroupSettings(
        val groupId: Long = 0L,
    ) : NavRoutes()

    @Serializable
    data class RouteSettings(
        val routeId: Long = -1L,
        val useDraft: Boolean = false,
    ) : NavRoutes() {
        @Transient
        var initialState: RouteSettingsUiState? = null

        @Transient
        var onSaved: (() -> Unit)? = null
    }

    @Serializable
    data class ProfileSelect(
        val preSelected: Long? = null,
    ) : NavRoutes() {
        @Transient
        var onSelected: ((Long) -> Unit)? = null
    }
}
