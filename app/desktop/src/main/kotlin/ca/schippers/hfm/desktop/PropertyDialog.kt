// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/PropertyDialog.kt
// Modified On Timestamp: 2026-10-25 @ 19:10 EDT
// Created On Timestamp: 2026-10-25 @ 19:10 EDT
// File Description: The Property details dialog of a house, cottage, condo, land or building: one tab per
//   catalogue section, then rooms, systems, outbuildings, owners, principal-residence years and custom fields.
// Uses: RealPropertyService and RealPropertyCatalogue (core/books); Components.kt inputs; core/i18n texts.
// Used By: AssetDialog (AssetsScreen.kt).
// Purpose: AST-06 to AST-17 on the desktop: record everything about a property in one place.
package ca.schippers.hfm.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.schippers.hfm.books.Asset
import ca.schippers.hfm.books.AssetKind
import ca.schippers.hfm.books.AreaUnit
import ca.schippers.hfm.books.LengthUnit
import ca.schippers.hfm.books.PropertyCustomField
import ca.schippers.hfm.books.PropertyField
import ca.schippers.hfm.books.PropertyFieldType
import ca.schippers.hfm.books.PropertyOwner
import ca.schippers.hfm.books.PropertyRoom
import ca.schippers.hfm.books.PropertySection
import ca.schippers.hfm.books.PropertyStructure
import ca.schippers.hfm.books.PropertySystem
import ca.schippers.hfm.books.RealProperty
import ca.schippers.hfm.books.RealPropertyCatalogue
import ca.schippers.hfm.books.RealPropertyService
import ca.schippers.hfm.books.ResidenceYear
import ca.schippers.hfm.books.ValidationException
import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import ca.schippers.hfm.money.MoneyFormat
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

/** The tabs after the catalogue sections: the lists of a property. */
private enum class PropertyList { ROOMS, SYSTEMS, STRUCTURES, OWNERS, RESIDENCE, CUSTOM }

private const val AREA_UNIT_SETTING = "property.areaUnit"
private const val LENGTH_UNIT_SETTING = "property.lengthUnit"
private const val OTHER = RealPropertyCatalogue.OTHER

/** How amounts, decimals, areas and lengths are shown and read back in this dialog. */
private class PropertyUnits(val locale: Locale, val currency: Currency, val area: AreaUnit, val length: LengthUnit) {
    fun show(v: BigDecimal?): String = v?.let { MoneyFormat.formatDecimal(it, locale) }.orEmpty()

    fun showArea(m2: BigDecimal?): String = show(m2?.let { area.fromSquareMetres(it) })

    fun showLength(m: BigDecimal?): String = show(m?.let { length.fromMetres(it) })

    fun decimal(text: String): BigDecimal? = text.trim().ifEmpty { null }?.let {
        runCatching { MoneyFormat.parseDecimal(it, locale) }.getOrElse { throw ValidationException("error.invalidNumber") }
    }

    fun area(text: String): BigDecimal? = decimal(text)?.let { area.toSquareMetres(it) }

    fun length(text: String): BigDecimal? = decimal(text)?.let { length.toMetres(it) }

    fun money(text: String): Money? = parseAmount(text, currency, locale)
}

// The rows being edited, as typed: numbers stay text until Save.

private data class RoomRow(
    val id: String = "", val type: String = "BEDROOM", val name: String = "", val level: String? = null, val length: String = "", val width: String = "",
    val area: String = "", val flooring: String? = null, val ceiling: String = "", val features: String = "", val condition: String? = null,
    val key: String = UUID.randomUUID().toString(),
)

private data class SystemRow(
    val id: String = "", val kind: String = "HEATING", val type: String? = null, val fuel: String? = null, val makeModel: String = "",
    val capacity: String = "", val installYear: String = "", val lastInspection: String = "", val ownership: String? = null,
    val rentalProvider: String = "", val rentalMonthly: String = "", val childAssetId: String? = null, val notes: String = "",
    val key: String = UUID.randomUUID().toString(),
)

private data class StructureRow(
    val id: String = "", val type: String = "SHED", val name: String = "", val area: String = "", val yearBuilt: String = "",
    val construction: String = "", val heated: Boolean = false, val electricity: Boolean = false, val water: Boolean = false,
    val childAssetId: String? = null, val notes: String = "", val key: String = UUID.randomUUID().toString(),
)

private data class OwnerRow(val memberId: String, val share: String = "", val tenancy: String? = null, val key: String = UUID.randomUUID().toString())

private data class CustomRow(val id: String = "", val label: String = "", val value: String = "", val key: String = UUID.randomUUID().toString())

private fun String.orNull(): String? = trim().ifEmpty { null }

private fun date(text: String): LocalDate? = text.orNull()?.let { runCatching { LocalDate.parse(it) }.getOrElse { throw ValidationException("error.invalidDate") } }

private fun year(text: String): Int? = text.orNull()?.let { it.toIntOrNull() ?: throw ValidationException("error.invalidYear") }

/** "2015-2019, 2023" as years; the service checks each year. */
internal fun parseYears(text: String): List<Int> = text.split(',', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }.flatMap { part ->
    val ends = part.split('-', '–').map { it.trim() }
    val from = ends.first().toIntOrNull() ?: throw ValidationException("error.invalidYear")
    val to = if (ends.size == 2) ends[1].toIntOrNull() ?: throw ValidationException("error.invalidYear") else from
    if (ends.size > 2 || to < from || to - from > 200) throw ValidationException("error.invalidYear")
    (from..to).toList()
}.distinct().sorted()

/** Years written back as ranges: 2015-2019, 2023. */
internal fun formatYears(years: List<Int>): String {
    val out = ArrayList<String>()
    var start: Int? = null
    var last = 0
    for (y in years.sorted().distinct()) {
        if (start != null && y == last + 1) {
            last = y
            continue
        }
        start?.let { out += if (it == last) "$it" else "$it-$last" }
        start = y
        last = y
    }
    start?.let { out += if (it == last) "$it" else "$it-$last" }
    return out.joinToString(", ")
}

/** AST-06 to AST-17: everything about a house, cottage, condo, land or building. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyDialog(model: BooksModel, asset: Asset, onClose: () -> Unit) {
    val books = model.books
    val locale = model.language.locale
    var area by remember { mutableStateOf(runCatching { AreaUnit.valueOf(books.setting(AREA_UNIT_SETTING).orEmpty()) }.getOrDefault(AreaUnit.SQUARE_FEET)) }
    var length by remember { mutableStateOf(runCatching { LengthUnit.valueOf(books.setting(LENGTH_UNIT_SETTING).orEmpty()) }.getOrDefault(LengthUnit.FEET)) }
    val units = PropertyUnits(locale, asset.currency, area, length)
    val start = remember { books.realProperty.get(asset.id) }
    val members = remember { books.members.list() }
    val names = remember { books.members.list(includeArchived = true).associate { it.id to it.displayName } }
    val children = remember { books.assets.list().filter { it.id != asset.id } }

    // The single-value fields, as typed.
    val fields = remember {
        mutableStateMapOf<String, String>().apply {
            for ((k, v) in start.fields) {
                val f = RealPropertyCatalogue.field(k)
                put(
                    k,
                    when (f?.type) {
                        PropertyFieldType.AREA -> units.showArea(BigDecimal(v))
                        PropertyFieldType.LENGTH -> units.showLength(BigDecimal(v))
                        PropertyFieldType.DECIMAL -> units.show(BigDecimal(v))
                        PropertyFieldType.MONEY -> MoneyFormat.formatAmount(Money.ofMinor(v.toLong(), asset.currency), locale)
                        else -> v
                    },
                )
            }
        }
    }
    val rooms = remember {
        mutableStateListOf<RoomRow>().apply {
            start.rooms.forEach { r ->
                add(RoomRow(r.id, r.type, r.name.orEmpty(), r.level, units.showLength(r.length), units.showLength(r.width), units.showArea(r.area), r.flooring, units.showLength(r.ceilingHeight), r.features.orEmpty(), r.condition))
            }
        }
    }
    val systems = remember {
        mutableStateListOf<SystemRow>().apply {
            start.systems.forEach { s ->
                add(
                    SystemRow(
                        s.id, s.kind, s.type, s.fuel, s.makeModel.orEmpty(), s.capacity.orEmpty(), s.installYear?.toString().orEmpty(), s.lastInspection?.toString().orEmpty(),
                        s.ownership, s.rentalProvider.orEmpty(), s.rentalMonthly?.let { MoneyFormat.formatAmount(it, locale) }.orEmpty(), s.childAssetId, s.notes.orEmpty(),
                    ),
                )
            }
        }
    }
    val structures = remember {
        mutableStateListOf<StructureRow>().apply {
            start.structures.forEach { s ->
                add(StructureRow(s.id, s.type, s.name.orEmpty(), units.showArea(s.area), s.yearBuilt?.toString().orEmpty(), s.construction.orEmpty(), s.heated, s.electricity, s.water, s.childAssetId, s.notes.orEmpty()))
            }
        }
    }
    val owners = remember {
        mutableStateListOf<OwnerRow>().apply {
            start.owners.forEach { o -> add(OwnerRow(o.memberId, o.shareBp?.let { units.show(BigDecimal(it).movePointLeft(2)) }.orEmpty(), o.tenancy)) }
        }
    }
    val residence = remember {
        mutableStateMapOf<String, String>().apply {
            start.residenceYears.groupBy { it.memberId }.forEach { (m, ys) -> put(m, formatYears(ys.map { it.year })) }
        }
    }
    val custom = remember { mutableStateListOf<CustomRow>().apply { start.custom.forEach { add(CustomRow(it.id, it.label, it.value.orEmpty())) } } }

    // Changing a unit converts what is already typed.
    fun reunit(newArea: AreaUnit, newLength: LengthUnit) {
        val next = PropertyUnits(locale, asset.currency, newArea, newLength)
        fun a(t: String) = runCatching { units.area(t) }.getOrNull()?.let(next::showArea) ?: t
        fun l(t: String) = runCatching { units.length(t) }.getOrNull()?.let(next::showLength) ?: t
        for (k in fields.keys.toList()) {
            when (RealPropertyCatalogue.field(k)?.type) {
                PropertyFieldType.AREA -> fields[k] = a(fields.getValue(k))
                PropertyFieldType.LENGTH -> fields[k] = l(fields.getValue(k))
                else -> Unit
            }
        }
        rooms.indices.forEach { i -> rooms[i].let { r -> rooms[i] = r.copy(length = l(r.length), width = l(r.width), area = a(r.area), ceiling = l(r.ceiling)) } }
        structures.indices.forEach { i -> structures[i] = structures[i].let { it.copy(area = a(it.area)) } }
        area = newArea
        length = newLength
        runCatching {
            books.putSetting(AREA_UNIT_SETTING, newArea.name)
            books.putSetting(LENGTH_UNIT_SETTING, newLength.name)
        }
    }

    fun build(): RealProperty {
        val stored = LinkedHashMap<String, String>()
        for ((k, text) in fields) {
            val v = text.trim()
            if (v.isEmpty()) continue
            stored[k] = when (RealPropertyCatalogue.field(k)?.type) {
                PropertyFieldType.AREA -> units.area(v)!!.toPlainString()
                PropertyFieldType.LENGTH -> units.length(v)!!.toPlainString()
                PropertyFieldType.DECIMAL -> units.decimal(v)!!.toPlainString()
                PropertyFieldType.MONEY -> units.money(v)!!.minorUnits.toString()
                else -> v
            }
        }
        return RealProperty(
            asset.id, stored,
            rooms = rooms.map { r ->
                PropertyRoom(r.id, r.type, r.name.orNull(), r.level, units.length(r.length), units.length(r.width), units.area(r.area), r.flooring, units.length(r.ceiling), r.features.orNull(), r.condition)
            },
            systems = systems.map { s ->
                val rented = s.ownership == "RENTED" || s.ownership == "LEASED"
                PropertySystem(
                    s.id, s.kind, s.type, s.fuel, s.makeModel.orNull(), s.capacity.orNull(), year(s.installYear), date(s.lastInspection), s.ownership,
                    s.rentalProvider.orNull().takeIf { rented }, units.money(s.rentalMonthly).takeIf { rented }, s.childAssetId, s.notes.orNull(),
                )
            },
            structures = structures.map { s ->
                PropertyStructure(s.id, s.type, s.name.orNull(), units.area(s.area), year(s.yearBuilt), s.construction.orNull(), s.heated, s.electricity, s.water, s.childAssetId, s.notes.orNull())
            },
            owners = owners.map { o ->
                PropertyOwner(o.memberId, units.decimal(o.share)?.movePointRight(2)?.let { runCatching { it.intValueExact() }.getOrElse { throw ValidationException("error.ownerShares") } }, o.tenancy)
            },
            residenceYears = residence.flatMap { (m, text) -> parseYears(text).map { ResidenceYear(m, it) } },
            custom = custom.map { PropertyCustomField(it.id, it.label.trim(), it.value.orNull()) },
        )
    }

    val sections = PropertySection.entries.filter { s ->
        when (s) {
            PropertySection.CONDO -> asset.kind == AssetKind.CONDO
            PropertySection.BUILDING, PropertySection.BASEMENT -> asset.kind != AssetKind.LAND
            else -> true
        }
    }
    val lists = PropertyList.entries.filter { it != PropertyList.ROOMS || asset.kind != AssetKind.LAND }
    val tabs: List<Any> = sections + lists
    var tab by remember { mutableStateOf(0) }

    WideDialog(model.t("property.title", asset.name), model.t("common.close"), onClose) {
        Column(Modifier.width(820.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Picker(model.t("property.areaUnit"), AreaUnit.entries, area, { model.t("areaUnit.$it") }, Modifier.width(200.dp)) { reunit(it, length) }
                Picker(model.t("property.lengthUnit"), LengthUnit.entries, length, { model.t("lengthUnit.$it") }, Modifier.width(200.dp)) { reunit(area, it) }
            }
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(if (t is PropertySection) model.t("property.section.$t") else model.t("property.tab.$t")) })
                }
            }
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (val t = tabs[tab.coerceIn(tabs.indices)]) {
                    is PropertySection -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (f in RealPropertyCatalogue.inSection(t)) FieldEditor(model, f, fields, units)
                    }
                    PropertyList.ROOMS -> RoomsEditor(model, rooms, units)
                    PropertyList.SYSTEMS -> SystemsEditor(model, systems, units, children)
                    PropertyList.STRUCTURES -> StructuresEditor(model, structures, units, children)
                    PropertyList.OWNERS -> OwnersEditor(model, owners, members)
                    PropertyList.RESIDENCE -> ResidenceEditor(model, asset, residence, members)
                    PropertyList.CUSTOM -> CustomEditor(model, custom)
                }
            }
            Button(onClick = { model.act { books.realProperty.save(build()) } }) { Text(model.t("common.save")) }
        }
    }
}

/** One catalogue field, entered the way its type asks (labels from property.field.*). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldEditor(model: BooksModel, f: PropertyField, values: SnapshotStateMap<String, String>, units: PropertyUnits) {
    val value = values[f.key].orEmpty()
    fun set(v: String) {
        values[f.key] = v
    }
    val label = model.t("property.field.${f.key}") + when (f.type) {
        PropertyFieldType.AREA -> " (${model.t("areaUnit.short.${units.area}")})"
        PropertyFieldType.LENGTH -> " (${model.t("lengthUnit.short.${units.length}")})"
        else -> ""
    }
    val list = f.choices.orEmpty()
    val wide = f.type == PropertyFieldType.LONG_TEXT || f.type == PropertyFieldType.MULTI_CHOICE
    Column(if (wide) Modifier.fillMaxWidth() else Modifier.width(260.dp)) {
        val full = Modifier.fillMaxWidth()
        when (f.type) {
            PropertyFieldType.LONG_TEXT -> TextInput(label, value, full, singleLine = false) { set(it) }
            PropertyFieldType.MONEY -> AmountInput(label, value, units.currency, units.locale, full, model::money) { set(it) }
            PropertyFieldType.DATE -> DateInput(label, value, full) { set(it) }
            PropertyFieldType.YES_NO -> Picker(label, listOf(null, "1", "0"), value.ifEmpty { null }, { yesNo(model, it) }, full) { set(it.orEmpty()) }
            PropertyFieldType.PROVINCE -> {
                val provinces = listOf<Province?>(null) + Province.entries.sortedBy { model.t("province.$it") }
                Picker(label, provinces, Province.of(value), { it?.let { p -> model.t("province.$p") } ?: model.t("common.none") }, full) { set(it?.name.orEmpty()) }
            }
            PropertyFieldType.CHOICE -> {
                Picker(label, listOf<String?>(null) + RealPropertyCatalogue.choiceList(list), value.ifEmpty { null }, { choice(model, list, it) }, full) { set(it.orEmpty()) }
            }
            PropertyFieldType.MULTI_CHOICE -> {
                val keys = RealPropertyCatalogue.choiceList(list)
                val picked = value.split(',').filter { it.isNotEmpty() }.toSet()
                Text(label, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in keys) {
                        FilterChip(c in picked, { set((if (c in picked) picked - c else picked + c).sortedBy { keys.indexOf(it) }.joinToString(",")) }, { Text(choice(model, list, c)) })
                    }
                }
            }
            PropertyFieldType.BILL -> RefPicker(model, label, value, remember { model.books.bills.list().associate { it.id to it.name } }) { set(it) }
            PropertyFieldType.ACCOUNT -> RefPicker(model, label, value, remember { model.books.accounts.list().associate { it.account.id to it.account.name } }) { set(it) }
            PropertyFieldType.CONTACT -> RefPicker(model, label, value, remember { model.books.contacts.list().associate { it.id to it.name } }) { set(it) }
            else -> TextInput(label, value, full) { set(it) }
        }
        if (f.choices != null && OTHER in value.split(',')) {
            val otherKey = f.key + RealPropertyCatalogue.OTHER_SUFFIX
            TextInput(model.t("property.other"), values[otherKey].orEmpty(), full) { values[otherKey] = it }
        }
        if (f.hint) Text(model.t("property.hint.${f.key}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun yesNo(model: BooksModel, v: String?): String = when (v) {
    "1" -> model.t("common.yes")
    "0" -> model.t("common.no")
    else -> model.t("common.none")
}

private fun choice(model: BooksModel, list: String, key: String?): String = key?.let { model.t("property.choice.$list.$it") } ?: model.t("common.none")

/** A choice among the household's bills, accounts or contacts, kept as the id. */
@Composable
private fun RefPicker(model: BooksModel, label: String, value: String, names: Map<String, String>, onSelect: (String) -> Unit) {
    Picker(label, listOf<String?>(null) + names.keys.sortedBy { names.getValue(it).lowercase() }, value.ifEmpty { null }, { it?.let(names::get) ?: model.t("common.none") }, Modifier.fillMaxWidth()) {
        onSelect(it.orEmpty())
    }
}

@Composable
private fun ChoicePicker(model: BooksModel, label: String, list: String, value: String?, modifier: Modifier, optional: Boolean = true, onSelect: (String?) -> Unit) {
    val keys = RealPropertyCatalogue.choiceList(list)
    Picker(label, if (optional) listOf<String?>(null) + keys else keys, value, { choice(model, list, it) }, modifier) { onSelect(it) }
}

@Composable
private fun ChildAssetPicker(model: BooksModel, value: String?, children: List<Asset>, modifier: Modifier, onSelect: (String?) -> Unit) {
    Picker(model.t("property.childAsset"), listOf<Asset?>(null) + children, children.firstOrNull { it.id == value }, { it?.name ?: model.t("common.none") }, modifier) { onSelect(it?.id) }
}

// --- AST-11: rooms -------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomsEditor(model: BooksModel, rooms: MutableList<RoomRow>, units: PropertyUnits) {
    val a = model.t("areaUnit.short.${units.area}")
    val l = model.t("lengthUnit.short.${units.length}")
    val total = rooms.mapNotNull { r -> runCatching { roomArea(r, units) }.getOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)
    Text(model.t("property.rooms.total", rooms.size, units.showArea(total), a), style = MaterialTheme.typography.bodySmall)
    rooms.toList().forEachIndexed { i, r ->
        key(r.key) {
            fun up(change: (RoomRow) -> RoomRow) {
                rooms[i] = change(rooms[i])
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                ChoicePicker(model, model.t("property.room.type"), "roomType", r.type, Modifier.width(200.dp), optional = false) { v -> up { it.copy(type = v ?: it.type) } }
                TextInput(model.t("property.room.name"), r.name, Modifier.width(180.dp)) { v -> up { it.copy(name = v) } }
                ChoicePicker(model, model.t("property.room.level"), "roomLevel", r.level, Modifier.width(170.dp)) { v -> up { it.copy(level = v) } }
                ChoicePicker(model, model.t("property.room.condition"), "roomCondition", r.condition, Modifier.width(170.dp)) { v -> up { it.copy(condition = v) } }
                TextInput("${model.t("property.room.length")} ($l)", r.length, Modifier.width(120.dp)) { v -> up { it.copy(length = v) } }
                TextInput("${model.t("property.room.width")} ($l)", r.width, Modifier.width(120.dp)) { v -> up { it.copy(width = v) } }
                val computed = if (r.area.isBlank()) runCatching { roomArea(r, units) }.getOrNull() else null
                TextInput("${model.t("property.room.area")} ($a)", r.area, Modifier.width(150.dp), supporting = computed?.let { units.showArea(it) }) { v -> up { it.copy(area = v) } }
                TextInput("${model.t("property.room.ceiling")} ($l)", r.ceiling, Modifier.width(150.dp)) { v -> up { it.copy(ceiling = v) } }
                ChoicePicker(model, model.t("property.room.flooring"), "flooring", r.flooring, Modifier.width(190.dp)) { v -> up { it.copy(flooring = v) } }
                TextInput(model.t("property.room.features"), r.features, Modifier.width(300.dp)) { v -> up { it.copy(features = v) } }
                TextButton(onClick = { rooms.removeAt(i) }) { Text(model.t("common.remove")) }
            }
            HorizontalDivider()
        }
    }
    OutlinedButton(onClick = { rooms += RoomRow() }) { Text(model.t("property.room.add")) }
}

/** The room's area in square metres: as entered, or length times width. */
private fun roomArea(r: RoomRow, units: PropertyUnits): BigDecimal? =
    PropertyRoom(type = r.type, length = units.length(r.length), width = units.length(r.width), area = units.area(r.area)).areaOrComputed

// --- AST-12: systems -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemsEditor(model: BooksModel, systems: MutableList<SystemRow>, units: PropertyUnits, children: List<Asset>) {
    systems.toList().forEachIndexed { i, s ->
        key(s.key) {
            fun up(change: (SystemRow) -> SystemRow) {
                systems[i] = change(systems[i])
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                ChoicePicker(model, model.t("property.system.kind"), "systemKind", s.kind, Modifier.width(200.dp), optional = false) { v ->
                    up { if (v == null || v == it.kind) it else it.copy(kind = v, type = null) }
                }
                RealPropertyCatalogue.systemTypes[s.kind]?.let { list ->
                    ChoicePicker(model, model.t("property.system.type"), list, s.type, Modifier.width(220.dp)) { v -> up { it.copy(type = v) } }
                }
                ChoicePicker(model, model.t("property.system.fuel"), "fuel", s.fuel, Modifier.width(180.dp)) { v -> up { it.copy(fuel = v) } }
                TextInput(model.t("property.system.makeModel"), s.makeModel, Modifier.width(220.dp)) { v -> up { it.copy(makeModel = v) } }
                TextInput(model.t("property.system.capacity"), s.capacity, Modifier.width(160.dp)) { v -> up { it.copy(capacity = v) } }
                TextInput(model.t("property.system.installYear"), s.installYear, Modifier.width(130.dp)) { v -> up { it.copy(installYear = v) } }
                DateInput(model.t("property.system.lastInspection"), s.lastInspection, Modifier.width(170.dp)) { v -> up { it.copy(lastInspection = v) } }
                ChoicePicker(model, model.t("property.system.ownership"), "systemOwnership", s.ownership, Modifier.width(170.dp)) { v -> up { it.copy(ownership = v) } }
                if (s.ownership == "RENTED" || s.ownership == "LEASED") {
                    TextInput(model.t("property.system.rentalProvider"), s.rentalProvider, Modifier.width(200.dp)) { v -> up { it.copy(rentalProvider = v) } }
                    AmountInput(model.t("property.system.rentalMonthly"), s.rentalMonthly, units.currency, units.locale, Modifier.width(160.dp), model::money) { v -> up { it.copy(rentalMonthly = v) } }
                }
                ChildAssetPicker(model, s.childAssetId, children, Modifier.width(220.dp)) { v -> up { it.copy(childAssetId = v) } }
                TextInput(model.t("account.notes"), s.notes, Modifier.width(300.dp)) { v -> up { it.copy(notes = v) } }
                TextButton(onClick = { systems.removeAt(i) }) { Text(model.t("common.remove")) }
            }
            HorizontalDivider()
        }
    }
    OutlinedButton(onClick = { systems += SystemRow() }) { Text(model.t("property.system.add")) }
}

// --- AST-13: outbuildings and fixtures -----------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StructuresEditor(model: BooksModel, structures: MutableList<StructureRow>, units: PropertyUnits, children: List<Asset>) {
    val a = model.t("areaUnit.short.${units.area}")
    structures.toList().forEachIndexed { i, s ->
        key(s.key) {
            fun up(change: (StructureRow) -> StructureRow) {
                structures[i] = change(structures[i])
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                ChoicePicker(model, model.t("property.structure.type"), "structureType", s.type, Modifier.width(200.dp), optional = false) { v -> up { it.copy(type = v ?: it.type) } }
                TextInput(model.t("property.structure.name"), s.name, Modifier.width(180.dp)) { v -> up { it.copy(name = v) } }
                TextInput("${model.t("property.structure.area")} ($a)", s.area, Modifier.width(140.dp)) { v -> up { it.copy(area = v) } }
                TextInput(model.t("property.structure.yearBuilt"), s.yearBuilt, Modifier.width(130.dp)) { v -> up { it.copy(yearBuilt = v) } }
                TextInput(model.t("property.structure.construction"), s.construction, Modifier.width(200.dp)) { v -> up { it.copy(construction = v) } }
                LabeledCheckbox(model.t("property.structure.heated"), s.heated) { v -> up { it.copy(heated = v) } }
                LabeledCheckbox(model.t("property.structure.electricity"), s.electricity) { v -> up { it.copy(electricity = v) } }
                LabeledCheckbox(model.t("property.structure.water"), s.water) { v -> up { it.copy(water = v) } }
                ChildAssetPicker(model, s.childAssetId, children, Modifier.width(220.dp)) { v -> up { it.copy(childAssetId = v) } }
                TextInput(model.t("account.notes"), s.notes, Modifier.width(300.dp)) { v -> up { it.copy(notes = v) } }
                TextButton(onClick = { structures.removeAt(i) }) { Text(model.t("common.remove")) }
            }
            HorizontalDivider()
        }
    }
    OutlinedButton(onClick = { structures += StructureRow() }) { Text(model.t("property.structure.add")) }
}

// --- AST-15: owners on title ---------------------------------------------------------------------

@Composable
private fun OwnersEditor(model: BooksModel, owners: MutableList<OwnerRow>, members: List<ca.schippers.hfm.books.Member>) {
    Text(model.t("property.owner.hint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    owners.toList().forEachIndexed { i, o ->
        key(o.key) {
            fun up(change: (OwnerRow) -> OwnerRow) {
                owners[i] = change(owners[i])
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Picker(model.t("property.owner.member"), members, members.firstOrNull { it.id == o.memberId }, { it.displayName }, Modifier.width(220.dp)) { m -> up { it.copy(memberId = m.id) } }
                TextInput(model.t("property.owner.share"), o.share, Modifier.width(130.dp)) { v -> up { it.copy(share = v) } }
                ChoicePicker(model, model.t("property.owner.tenancy"), "tenancy", o.tenancy, Modifier.width(220.dp)) { v -> up { it.copy(tenancy = v) } }
                TextButton(onClick = { owners.removeAt(i) }) { Text(model.t("common.remove")) }
            }
        }
    }
    val free = members.filter { m -> owners.none { it.memberId == m.id } }
    if (free.isNotEmpty()) OutlinedButton(onClick = { owners += OwnerRow(free.first().id) }) { Text(model.t("property.owner.add")) }
}

// --- AST-17: principal-residence years -----------------------------------------------------------

@Composable
private fun ResidenceEditor(model: BooksModel, asset: Asset, years: SnapshotStateMap<String, String>, members: List<ca.schippers.hfm.books.Member>) {
    Text(model.t("property.residence.hint", RealPropertyService.FAMILY_UNIT_FROM.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    for (m in members) {
        TextInput(model.t("property.residence.years", m.displayName), years[m.id].orEmpty(), Modifier.width(420.dp), supporting = model.t("property.residence.example")) { years[m.id] = it }
    }
    // The years another property was also designated (saved values; the check runs again after Save).
    val conflicts = remember(model.revision) { runCatching { model.books.realProperty.residenceConflicts() }.getOrDefault(emptyList()).filter { asset.id in it.assetIds } }
    val others = remember(conflicts) { conflicts.associate { c -> c.year to c.assetIds.filter { it != asset.id }.mapNotNull { id -> runCatching { model.books.assets.get(id).name }.getOrNull() } } }
    for (c in conflicts) {
        Text(model.t("property.residence.conflict", c.year.toString(), others[c.year].orEmpty().joinToString(", ")), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

// --- AST-06: custom fields -----------------------------------------------------------------------

@Composable
private fun CustomEditor(model: BooksModel, custom: MutableList<CustomRow>) {
    custom.toList().forEachIndexed { i, c ->
        key(c.key) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextInput(model.t("property.custom.label"), c.label, Modifier.width(240.dp)) { v -> custom[i] = custom[i].copy(label = v) }
                TextInput(model.t("property.custom.value"), c.value, Modifier.width(420.dp), singleLine = false) { v -> custom[i] = custom[i].copy(value = v) }
                TextButton(onClick = { custom.removeAt(i) }) { Text(model.t("common.remove")) }
            }
        }
    }
    OutlinedButton(onClick = { custom += CustomRow() }) { Text(model.t("property.custom.add")) }
}
