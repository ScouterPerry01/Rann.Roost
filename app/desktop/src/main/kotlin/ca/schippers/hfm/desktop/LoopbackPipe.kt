// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/LoopbackPipe.kt
// Modified On Timestamp: 2026-10-20 @ 10:05 EDT
// Created On Timestamp: 2026-10-20 @ 10:05 EDT
// File Description: Makes Java's internal wake-up pipe on Windows use a local TCP connection instead
//   of a socket file in %TEMP%.
// Uses: The JDK's jdk.net.unixdomain.tmpdir setting.
// Used By: Main.kt (the first thing main() does).
// Purpose: The Microsoft Store's third rejection: inside the MSIX package Windows redirects files
//   written under AppData, so the socket file is made in one place and looked for in another; every
//   HttpClient (updates, AI reading, rates) then failed with "Invalid argument: connect".

package ca.schippers.hfm.desktop

import java.nio.file.Path

internal object LoopbackPipe {

    private const val SETTING = "jdk.net.unixdomain.tmpdir"

    /**
     * On Windows, gives Java a socket-file folder that cannot be used, so its selectors fall back to the
     * loopback TCP connection Java used before version 16. Must run before anything opens a network
     * connection: Java reads the setting once. A value given on the command line is kept.
     */
    fun useTcp() {
        folder(System.getProperty("os.name"), System.getProperty("java.home"), System.getProperty(SETTING))
            ?.let { System.setProperty(SETTING, it) }
    }

    /**
     * The folder to set, or null to leave Java as it is: a folder that does not exist inside the app's
     * own Java runtime (read-only once installed), so binding a socket file there always fails.
     */
    internal fun folder(osName: String?, javaHome: String?, current: String?): String? {
        if (current != null || javaHome == null) return null
        if (osName?.lowercase()?.startsWith("windows") != true) return null
        return Path.of(javaHome, "no-socket-files").toString()
    }
}
