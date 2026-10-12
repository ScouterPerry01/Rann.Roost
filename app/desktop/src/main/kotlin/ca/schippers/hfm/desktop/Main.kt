// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/Main.kt
// Modified On Timestamp: 2026-10-12 @ 09:17 EDT
// Created On Timestamp: 2026-10-01 @ 15:53 EDT
// File Description: The desktop app's entry point: the window, the tray, auto-lock and demo mode
//   (runDemo: a sample household in any province with -Pprovince); a start-up error is reported.
// Uses: AppState, App.kt, DemoHousehold, DemoPlace, StartupFailure.
// Used By: The packaged app and ./gradlew :app:desktop:run / runDemo.
// Purpose: Start the app.

package ca.schippers.hfm.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ca.schippers.hfm.books.StatementStatus
import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.i18n.Messages
import kotlinx.coroutines.delay
import java.util.prefs.Preferences
import kotlin.system.exitProcess

fun main() {
    PackagedSelfCheck.runIfRequested()
    // An error before or while the window opens would otherwise end in the launcher's bare
    // "Failed to launch JVM" (the Microsoft Store's first review): say what it was instead. When our
    // own message cannot show either, the error goes on to the launcher, whose box is better than none.
    try {
        desktopApp()
    } catch (e: Throwable) {
        if (StartupFailure.report(e)) exitProcess(0) else throw e
    }
}

private fun desktopApp() = application {
    val state = remember {
        // The demo keeps its own per-computer settings, so trying it never changes the real app's.
        val demo = System.getProperty("hfm.demo") == "true"
        val prefs = Preferences.userRoot().node(if (demo) "ca/schippers/hfm-demo" else "ca/schippers/hfm")
        AppState(prefs = prefs).also { app ->
            // Demo mode (./gradlew :app:desktop:runDemo): a throw-away sample household.
            if (demo) {
                System.getProperty("hfm.demo.lang")?.let { tag ->
                    Language.entries.firstOrNull { it.tag == tag }?.let { app.switchLanguage(it, remember = false) }
                }
                // -Psample=family|single: another sample household than Alex and Sam's; -Pprovince=AB: in any province or
                // territory (by default Quebec in French, Ontario in English).
                val sample = SampleHousehold.entries.firstOrNull { it.name.equals(System.getProperty("hfm.demo.sample"), ignoreCase = true) } ?: SampleHousehold.COUPLE
                val province = ca.schippers.hfm.calc.Province.of(System.getProperty("hfm.demo.province")) ?: DemoPlace.defaultFor(app.language)
                val model = BooksModel(sample.create(app.store, app.language, province), app)
                model.selectedAccountId = model.books.accounts.list().firstOrNull { it.account.name == System.getProperty("hfm.demo.account") }?.account?.id
                System.getProperty("hfm.demo.section")?.let { name -> Section.entries.firstOrNull { it.name == name }?.let { model.section = it } }
                System.getProperty("hfm.demo.report")?.let { name -> ReportKind.entries.firstOrNull { it.name == name }?.let { model.reportState.kind = it } }
                if (System.getProperty("hfm.demo.reconcile") == "true") {
                    model.selectedAccountId?.let { id ->
                        model.reconcilingStatementId = model.books.statements.statements(id).firstOrNull { it.status == StatementStatus.OPEN }?.id
                    }
                }
                System.getProperty("hfm.demo.search")?.let { q -> model.search = q to model.books.search.search(q, model.language.locale) }
                app.screen = Screen.Main(model)
                // -Pmanual=<chapter or chapter#section> (or "screen"): the manual open beside the app.
                System.getProperty("hfm.demo.manual")?.let { page -> app.openManual(page.takeUnless { it == "screen" }) }
            }
        }
    }
    // BILL-04: a system notification with today's bill reminders, once per household and day.
    val trayState = rememberTrayState()
    Tray(icon = AppIcon, state = trayState, tooltip = Messages.get(state.language, "app.name"))
    val main = state.screen as? Screen.Main
    LaunchedEffect(main?.model) {
        val model = main?.model ?: return@LaunchedEffect
        // Checked every few minutes so timed appointments are announced on time; each reminder once a
        // day for this household, remembered on this computer so reopening the app does not repeat it.
        while (true) {
            val lines = model.reminderLines()
            val keys = state.notified.fresh(model.session.householdId, today(), lines.map { it.key })
            val fresh = lines.filter { it.key in keys }
            if (fresh.isNotEmpty()) {
                val body = notificationBody(fresh, state.notificationDetails) { model.t("nav.${it.name.lowercase()}") }
                trayState.sendNotification(Notification(model.t("reminder.banner", fresh.size), body))
            }
            delay(5 * 60_000L)
        }
    }
    // SEC-02: lock after the chosen time without keyboard or mouse activity.
    LaunchedEffect(state) {
        while (true) {
            delay(15_000)
            state.lockIfIdle()
        }
    }
    Window(
        onCloseRequest = {
            state.lock()
            exitApplication()
        },
        onPreviewKeyEvent = { event ->
            state.touch()
            // Ctrl+F: go to the search box (OTH-03).
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.F && state.screen is Screen.Main) {
                runCatching { state.searchFocus.requestFocus() }
                true
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.F1 && event.isShiftPressed) {
                // NFR-12: Shift+F1 opens the manual, in its own window, on the chapter for the screen shown.
                state.openManual()
                true
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.F1 && state.helpTopic == null) {
                // NFR-12: F1 opens the help on the topic for the screen shown.
                state.openHelp()
                true
            } else {
                false
            }
        },
        title = Messages.get(state.language, "app.name"),
        icon = AppIcon,
        state = rememberWindowState(width = 1440.dp, height = 900.dp),
    ) {
        App(state)
    }
    if (state.manualPage != null) ManualWindow(state)
}

/**
 * The RANN's Roost head, for the window, taskbar and tray (branding/, not covered by the GPL).
 * Loaded on first use, so the packaged self-check runs without the graphics libraries.
 */
internal val AppIcon by lazy {
    BitmapPainter(
        org.jetbrains.skia.Image.makeFromEncoded(
            checkNotNull(object {}.javaClass.getResourceAsStream("/hfm/branding/icon.png")).use { it.readBytes() },
        ).toComposeImageBitmap(),
    )
}
