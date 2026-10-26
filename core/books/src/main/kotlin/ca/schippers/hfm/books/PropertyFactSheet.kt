// File path and name: core/books/src/main/kotlin/ca/schippers/hfm/books/PropertyFactSheet.kt
// Modified On Timestamp: 2026-10-25 @ 21:13 EDT
// Created On Timestamp: 2026-10-25 @ 21:00 EDT
// File Description: The property fact sheet (AST-16): every filled-in detail of a property as headed
//   label / value lines in the chosen language and units, with the legal identifiers and owners optional.
// Uses: RealPropertyService, RealPropertyCatalogue, core/i18n texts (property.*), MoneyFormat.
// Used By: RealPropertyService.factSheet; the desktop Property dialog (PDF through PdfPages.factSheet).
// Purpose: One printable page or two about a property, for an insurer, a sale, an executor or a contractor.
package ca.schippers.hfm.books

import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.i18n.Messages
import ca.schippers.hfm.money.Money
import ca.schippers.hfm.money.MoneyFormat
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

/** AST-16: one heading of a fact sheet and its label / value lines, in order. */
data class FactSection(val title: String, val rows: List<Pair<String, String>>)

/** AST-16: a property's fact sheet: its title, the lines under it, then each section that has something in it. */
data class FactSheet(val title: String, val subtitle: List<String>, val sections: List<FactSection>) {
    /** The sheet as plain text, one line per row (what the golden-text test compares). */
    fun text(): String = buildString {
        appendLine(title)
        subtitle.forEach { appendLine(it) }
        for (s in sections) {
            appendLine()
            appendLine("[${s.title}]")
            s.rows.forEach { (label, value) -> appendLine("$label: $value") }
        }
    }
}

/**
 * What a fact sheet shows: sizes in [area] and [length] units; [legal] keeps the roll number, parcel
 * and the rest of the legal section; [owners] keeps the owners and the principal-residence years.
 */
data class FactSheetOptions(val area: AreaUnit, val length: LengthUnit, val legal: Boolean = true, val owners: Boolean = true)

/** "2015-2019, 2023": years written as ranges. */
fun yearRanges(years: List<Int>): String {
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

/** Builds the fact sheet of one property from what is saved, in [language]. */
internal class PropertyFactSheet(private val books: Books, private val language: Language, private val today: LocalDate) {
    private val locale = language.locale

    private fun t(key: String, vararg args: Any) = Messages.get(language, key, *args)

    private fun pair(label: String, value: String) = t("property.factSheet.pair", label, value)

    private fun choice(list: String, key: String, other: String? = null): String =
        if (key == RealPropertyCatalogue.OTHER && !other.isNullOrBlank()) other else t("property.choice.$list.$key")

    private fun decimal(v: BigDecimal) = MoneyFormat.formatDecimal(v, locale)

    private fun area(m2: BigDecimal, o: FactSheetOptions) = "${decimal(o.area.fromSquareMetres(m2))} ${t("areaUnit.short.${o.area}")}"

    private fun length(m: BigDecimal, o: FactSheetOptions) = "${decimal(o.length.fromMetres(m))} ${t("lengthUnit.short.${o.length}")}"

    fun build(p: RealProperty, asset: Asset, o: FactSheetOptions): FactSheet {
        val members by lazy { books.members.list(includeArchived = true).associate { it.id to it.displayName } }
        val assets by lazy { books.assets.list(includeDisposed = true).associate { it.id to it.name } }
        val bills by lazy { books.bills.list(includeInactive = true).associate { it.id to it.name } }
        val accounts by lazy { books.accounts.list().associate { it.account.id to it.account.name } }
        val contacts by lazy { books.contacts.list().associate { it.id to it.name } }

        fun show(f: PropertyField, v: String): String? = when (f.type) {
            PropertyFieldType.TEXT, PropertyFieldType.LONG_TEXT, PropertyFieldType.INTEGER, PropertyFieldType.YEAR, PropertyFieldType.DATE,
            PropertyFieldType.POSTAL_CODE, PropertyFieldType.COUNTRY, PropertyFieldType.LATITUDE, PropertyFieldType.LONGITUDE,
            -> v
            PropertyFieldType.DECIMAL -> decimal(BigDecimal(v))
            PropertyFieldType.AREA -> area(BigDecimal(v), o)
            PropertyFieldType.LENGTH -> length(BigDecimal(v), o)
            PropertyFieldType.MONEY -> MoneyFormat.format(Money.ofMinor(v.toLong(), asset.currency), locale)
            PropertyFieldType.YES_NO -> t(if (v == "1") "common.yes" else "common.no")
            PropertyFieldType.CHOICE, PropertyFieldType.MULTI_CHOICE ->
                p.choices(f.key).joinToString(", ") { choice(f.choices!!, it, p.text(f.key + RealPropertyCatalogue.OTHER_SUFFIX)) }
            PropertyFieldType.PROVINCE -> t("province.$v")
            // A linked record that was deleted since is left out.
            PropertyFieldType.BILL -> bills[v]
            PropertyFieldType.ACCOUNT -> accounts[v]
            PropertyFieldType.CONTACT -> contacts[v]
        }

        val sections = ArrayList<FactSection>()
        fun add(title: String, rows: List<Pair<String, String>>) {
            if (rows.isNotEmpty()) sections += FactSection(title, rows)
        }
        for (s in PropertySection.entries) {
            if (s == PropertySection.LEGAL && !o.legal) continue
            add(
                t("property.section.$s"),
                RealPropertyCatalogue.inSection(s).mapNotNull { f -> p.fields[f.key]?.let { v -> show(f, v) }?.let { t("property.field.${f.key}") to it } },
            )
        }

        // Rooms: "Kitchen (Name)": level · 15 × 13 ft · 195 sq ft · flooring · condition · features; then the total.
        add(
            t("property.tab.ROOMS"),
            p.rooms.map { r ->
                val label = choice("roomType", r.type) + (r.name?.let { " ($it)" }.orEmpty())
                label to listOfNotNull(
                    r.level?.let { choice("roomLevel", it) },
                    if (r.length != null && r.width != null) t("property.factSheet.size", length(r.length, o), length(r.width, o)) else null,
                    r.areaOrComputed?.let { area(it, o) },
                    r.ceilingHeight?.let { pair(t("property.room.ceiling"), length(it, o)) },
                    r.flooring?.let { choice("flooring", it) },
                    r.condition?.let { choice("roomCondition", it) },
                    r.features,
                ).joinToString(" · ")
            } + p.rooms.mapNotNull { it.areaOrComputed }.takeIf { it.size > 1 }
                ?.let { areas -> listOf(t("property.factSheet.roomsTotal") to area(areas.fold(BigDecimal.ZERO, BigDecimal::add), o)) }.orEmpty(),
        )

        add(
            t("property.tab.SYSTEMS"),
            p.systems.map { s ->
                val types = RealPropertyCatalogue.systemTypes[s.kind]
                choice("systemKind", s.kind) to listOfNotNull(
                    s.type?.let { if (types != null) choice(types, it) else it },
                    s.fuel?.let { choice("fuel", it) },
                    s.makeModel,
                    s.capacity,
                    s.installYear?.let { pair(t("property.system.installYear"), it.toString()) },
                    s.lastInspection?.let { pair(t("property.system.lastInspection"), it.toString()) },
                    s.ownership?.let { choice("systemOwnership", it) },
                    s.rentalProvider?.let { pair(t("property.system.rentalProvider"), it) },
                    s.rentalMonthly?.let { t("property.factSheet.perMonth", MoneyFormat.format(it, locale)) },
                    s.childAssetId?.let(assets::get)?.let { pair(t("property.childAsset"), it) },
                    s.notes,
                ).joinToString(" · ")
            },
        )

        add(
            t("property.tab.STRUCTURES"),
            p.structures.map { s ->
                choice("structureType", s.type) + (s.name?.let { " ($it)" }.orEmpty()) to listOfNotNull(
                    s.area?.let { area(it, o) },
                    s.yearBuilt?.let { pair(t("property.structure.yearBuilt"), it.toString()) },
                    s.construction,
                    t("property.structure.heated").takeIf { s.heated },
                    t("property.structure.electricity").takeIf { s.electricity },
                    t("property.structure.water").takeIf { s.water },
                    s.childAssetId?.let(assets::get)?.let { pair(t("property.childAsset"), it) },
                    s.notes,
                ).joinToString(" · ")
            },
        )

        if (o.owners) {
            add(
                t("property.tab.OWNERS"),
                p.owners.map { ow ->
                    (members[ow.memberId] ?: ow.memberId) to listOfNotNull(
                        ow.shareBp?.let { t("property.factSheet.share", decimal(BigDecimal(it).movePointLeft(2))) },
                        ow.tenancy?.let { choice("tenancy", it) },
                    ).joinToString(" · ")
                },
            )
            add(
                t("property.tab.RESIDENCE"),
                p.residenceYears.groupBy { it.memberId }.map { (m, ys) -> (members[m] ?: m) to yearRanges(ys.map { it.year }) },
            )
        }

        add(t("property.tab.CUSTOM"), p.custom.mapNotNull { c -> c.value?.let { c.label to it } })

        val subtitle = listOfNotNull(
            t("assetKind.${asset.kind}"),
            p.addressLines().joinToString(", ").ifBlank { null },
            t("property.factSheet.prepared", today.toString()),
        )
        return FactSheet(t("property.factSheet.title", asset.name), subtitle, sections)
    }
}
