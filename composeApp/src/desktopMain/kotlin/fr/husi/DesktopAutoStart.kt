package fr.husi

import fr.husi.ktx.Logs
import fr.husi.ktx.blankAsNull
import fr.husi.platform.PlatformInfo
import java.io.File

internal object DesktopAutoStart {
    private lateinit var manager: DesktopAutoStartManager

    fun initialize() {
        manager = DesktopAutoStartManager(buildStartupCommand())
    }

    fun setEnabled(enabled: Boolean): Boolean {
        return manager.setEnabled(enabled)
    }
}

private class DesktopAutoStartManager(
    private val startupCommand: List<String>,
) {
    companion object {
        private const val DESKTOP_ENTRY_NAME = "fr.husi.desktop"
        private const val LAUNCHER_AGENT_NAME = "fr.husi.desktop.autostart"
        private const val WINDOWS_RUN_KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""
        private const val WINDOWS_VALUE_NAME = "Husi"
    }

    fun setEnabled(enabled: Boolean): Boolean {
        return runCatching {
            if (enabled) {
                enable()
            } else {
                disable()
            }
        }.onFailure {
            Logs.e("update desktop auto-start", it)
        }.isSuccess
    }

    private fun enable() {
        when {
            PlatformInfo.isLinux -> writeLinuxDesktopEntry()
            PlatformInfo.isMacOs -> writeMacLaunchAgent()
            PlatformInfo.isWindows -> writeWindowsRunKey()
            else -> error("Unsupported desktop platform")
        }
    }

    private fun disable() {
        when {
            PlatformInfo.isLinux -> deleteFileIfPresent(linuxDesktopEntryFile())
            PlatformInfo.isMacOs -> deleteFileIfPresent(macLaunchAgentFile())
            PlatformInfo.isWindows -> deleteWindowsRunKey()
            else -> error("Unsupported desktop platform")
        }
    }

    private fun linuxDesktopEntryFile(): File {
        val xdgConfigHome = System.getenv("XDG_CONFIG_HOME")
            ?.blankAsNull()
            ?.let(::File)
            ?: File(System.getProperty("user.home"), ".config")
        return File(File(xdgConfigHome, "autostart"), DESKTOP_ENTRY_NAME)
    }

    private fun writeLinuxDesktopEntry() {
        val entryFile = linuxDesktopEntryFile()
        entryFile.parentFile.mkdirs()
        val execLine = startupCommand.joinToString(" ", transform = ::quoteDesktopEntryArgument)
        entryFile.writeText(
            """
            [Desktop Entry]
            Type=Application
            Version=1.0
            Name=Husi
            Comment=Launch Husi at login and connect automatically
            Exec=$execLine
            Terminal=false
            StartupNotify=false
            X-GNOME-Autostart-enabled=true
            """.trimIndent() + "\n",
        )
    }

    private fun quoteDesktopEntryArgument(argument: String): String {
        val escaped = buildString(argument.length) {
            for (char in argument) {
                when (char) {
                    '\\', '"', '$', '`' -> {
                        append('\\')
                        append(char)
                    }

                    else -> append(char)
                }
            }
        }
        return "\"$escaped\""
    }

    private fun macLaunchAgentFile(): File {
        return File(
            File(System.getProperty("user.home"), "Library/LaunchAgents"),
            "$LAUNCHER_AGENT_NAME.plist",
        )
    }

    private fun writeMacLaunchAgent() {
        val agentFile = macLaunchAgentFile()
        agentFile.parentFile.mkdirs()
        val arguments = startupCommand.joinToString(separator = "\n") {
            "    <string>${xmlEscape(it)}</string>"
        }
        agentFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>$LAUNCHER_AGENT_NAME</string>
                <key>ProgramArguments</key>
                <array>
            $arguments
                </array>
                <key>RunAtLoad</key>
                <true/>
            </dict>
            </plist>
            """.trimIndent() + "\n",
        )
    }

    private fun xmlEscape(value: String): String {
        return buildString(value.length) {
            for (char in value) {
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(char)
                }
            }
        }
    }

    private fun writeWindowsRunKey() {
        val commandLine = startupCommand.joinToString(" ", transform = ::quoteWindowsArgument)
        runWindowsRegistryCommand(
            "add",
            WINDOWS_RUN_KEY,
            "/v",
            WINDOWS_VALUE_NAME,
            "/t",
            "REG_SZ",
            "/d",
            commandLine,
            "/f",
        )
    }

    private fun deleteFileIfPresent(file: File) {
        if (!file.exists()) return
        check(file.delete()) { "failed to delete ${file.absolutePath}" }
    }

    private fun deleteWindowsRunKey() {
        if (!windowsRunKeyExists()) return
        runWindowsRegistryCommand(
            "delete",
            WINDOWS_RUN_KEY,
            "/v",
            WINDOWS_VALUE_NAME,
            "/f",
        )
    }

    private fun windowsRunKeyExists(): Boolean {
        return runCatching {
            runWindowsRegistryCommand("query", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE_NAME)
        }.isSuccess
    }

    private fun runWindowsRegistryCommand(vararg args: String): String {
        val process = ProcessBuilder(
            buildList {
                add("reg")
                addAll(args)
            },
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            output.ifBlank {
                "reg ${args.joinToString(" ")} failed with exit code $exitCode"
            }
        }
        return output
    }

    private fun quoteWindowsArgument(argument: String): String {
        if (argument.isEmpty()) return "\"\""
        if (argument.none { it == ' ' || it == '\t' || it == '"' }) return argument

        val quoted = StringBuilder(argument.length + 2)
        quoted.append('"')
        var slashCount = 0
        for (char in argument) {
            when (char) {
                '\\' -> slashCount++
                '"' -> {
                    quoted.append("\\".repeat(slashCount * 2 + 1))
                    quoted.append('"')
                    slashCount = 0
                }

                else -> {
                    if (slashCount > 0) {
                        quoted.append("\\".repeat(slashCount))
                        slashCount = 0
                    }
                    quoted.append(char)
                }
            }
        }
        if (slashCount > 0) {
            quoted.append("\\".repeat(slashCount * 2))
        }
        quoted.append('"')
        return quoted.toString()
    }
}

private fun buildStartupCommand(): List<String> {
    return buildList {
        addAll(resolveLauncherCommand())
        add("--autostart")
        add("--background")
    }
}

/**
 * Resolve based on package path -> try resolving jpackage -> try getting from process
 */
private fun resolveLauncherCommand(): List<String> {
    resolvePackagedDesktopLauncher()
        ?.let { return listOf(it.absolutePath) }

    System.getProperty("jpackage.app-path")
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf(File::isFile)
        ?.let { return listOf(it.absolutePath) }

    ProcessHandle.current().info().command().orElse(null)
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf(File::isFile)
        ?.takeIf { !it.name.lowercase().startsWith("java") }
        ?.let { return listOf(it.absolutePath) }

    error("Desktop launcher not found")
}

private fun resolvePackagedDesktopLauncher(): File? {
    val codeSource = DesktopAutoStart::class.java.protectionDomain?.codeSource?.location ?: return null
    val runtimePath = runCatching {
        File(codeSource.toURI())
    }.getOrElse {
        File(codeSource.path)
    }
    val appDir = runtimePath.parentFile
        ?.takeIf { runtimePath.isFile && it.name == "app" }
        ?: return null
    val appRoot = appDir.parentFile ?: return null
    return when {
        PlatformInfo.isLinux -> resolveSingleDesktopLauncher(File(appRoot, "bin")) {
            it.canExecute()
        }

        PlatformInfo.isMacOs -> resolveSingleDesktopLauncher(File(appRoot, "MacOS")) {
            it.canExecute()
        }

        PlatformInfo.isWindows -> resolveSingleDesktopLauncher(appRoot) {
            it.extension.equals("exe", ignoreCase = true)
        }

        else -> null
    }
}

private fun resolveSingleDesktopLauncher(directory: File, predicate: (File) -> Boolean): File? {
    val files = directory.listFiles() ?: return null
    return files.singleOrNull { it.isFile && predicate(it) }
}
