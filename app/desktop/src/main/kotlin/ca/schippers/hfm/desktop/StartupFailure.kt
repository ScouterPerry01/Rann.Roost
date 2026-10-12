// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/StartupFailure.kt
// Modified On Timestamp: 2026-10-12 @ 09:17 EDT
// Created On Timestamp: 2026-10-12 @ 09:17 EDT
// File Description: What happens when the app cannot start: the error and its details are written
//   to startup-error.log and shown in a message, instead of the launcher's bare "Failed to launch JVM".
// Uses: core/i18n Messages, Swing JOptionPane.
// Used By: Main.kt.
// Purpose: Make a start-up failure on someone else's computer (a user's, the Microsoft Store's testers')
//   say what went wrong.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.i18n.Messages
import java.awt.GraphicsEnvironment
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.Locale
import javax.swing.JOptionPane

internal object StartupFailure {

    /**
     * Where the log goes: the user's local application data on Windows (inside the package's own
     * folder when installed from the Microsoft Store), ~/.local/state elsewhere.
     */
    val logFile: Path
        get() {
            val local = System.getenv("LOCALAPPDATA")?.let(Path::of)
            val base = local?.resolve("RANN's Roost")
                ?: (System.getenv("XDG_STATE_HOME")?.let(Path::of) ?: Path.of(System.getProperty("user.home"), ".local", "state")).resolve("ranns-roost")
            return base.resolve("startup-error.log")
        }

    /**
     * Writes [error] to [logFile] and shows it, as far as this computer still allows; never throws.
     * True when the message was shown (the app then ends quietly instead of adding the launcher's box).
     */
    fun report(error: Throwable): Boolean {
        val details = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        System.err.println(details)
        val written = runCatching {
            val file = logFile
            Files.createDirectories(file.parent)
            val version = System.getProperty("jpackage.app-version") ?: "development"
            Files.writeString(
                file,
                "${LocalDateTime.now()}  RANN's Roost $version  Java ${System.getProperty("java.version")}  " +
                    "${System.getProperty("os.name")} ${System.getProperty("os.version")}\n\n$details",
            )
            file
        }.getOrNull()
        // The message window itself may be what failed (the error was in the windowing toolkit): then the log is all there is.
        return runCatching {
            check(!GraphicsEnvironment.isHeadless())
            val language = Language.of(Locale.getDefault())
            val text = Messages.get(language, "startup.failed.body", error.toString(), written?.toString() ?: "-")
            JOptionPane.showMessageDialog(null, text, Messages.get(language, "startup.failed.title"), JOptionPane.ERROR_MESSAGE)
        }.isSuccess
    }
}
