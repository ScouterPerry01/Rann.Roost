// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/SampleScreen.kt
// Modified On Timestamp: 2026-10-11 @ 13:05 EDT
// Created On Timestamp: 2026-10-11 @ 12:49 EDT
// File Description: The "Open a sample household" screen: choose one of the three sample
//   households, its language and its province or territory, then open a throw-away copy.
// Uses: SampleHousehold, DemoPlace, AppState.openedSample, Picker.
// Used By: App.kt (Screen.Samples, from the welcome screen).
// Purpose: Let anyone try the app on a realistic household of their own province and language.

package ca.schippers.hfm.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.i18n.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Choose a sample household and where it lives. The language is the app's own (switching it here
 * switches the app); the province starts as Quebec in French and Ontario in English until chosen.
 * Building one takes a little while, so the screen says so until it opens.
 */
@Composable
internal fun SampleScreen(state: AppState) {
    var kind by remember { mutableStateOf(SampleHousehold.FAMILY) }
    var province by remember { mutableStateOf<Province?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val where = province ?: DemoPlace.defaultFor(state.language)

    Text(state.t("sample.title"), style = MaterialTheme.typography.headlineMedium)
    Text(state.t("sample.intro"))
    for (option in SampleHousehold.entries) {
        val key = "sample.${option.name.lowercase()}"
        OutlinedCard(Modifier.fillMaxWidth().clickable(enabled = !busy) { kind = option }) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(selected = kind == option, onClick = { kind = option }, enabled = !busy)
                Column {
                    Text(state.t(key), style = MaterialTheme.typography.titleMedium)
                    Text(state.t("$key.hint"), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Text(state.t("sample.language"), style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (language in Language.entries) {
            val name = if (language == Language.ENGLISH) "English" else "Français"
            if (language == state.language) Button(onClick = {}, enabled = !busy) { Text(name) }
            else OutlinedButton(onClick = { state.switchLanguage(language) }, enabled = !busy) { Text(name) }
        }
    }
    Picker(state.t("sample.province"), Province.entries.sortedBy { state.t("province.$it") }, where, { state.t("province.$it") }, Modifier.width(400.dp), enabled = !busy) { province = it }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (busy) Text(state.t("sample.preparing"), style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { state.screen = Screen.Welcome }, enabled = !busy) { Text(state.t("common.back")) }
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                error = null
                val language = state.language
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { kind.create(state.store, language, where) } }
                    busy = false
                    result.onSuccess { state.openedSample(it) }.onFailure { error = state.t("error.generic", it.message ?: it.javaClass.simpleName) }
                }
            },
        ) { Text(state.t("sample.open")) }
    }
}
