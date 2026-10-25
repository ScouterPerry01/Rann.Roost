// File path and name: core/books/src/main/kotlin/ca/schippers/hfm/books/RealPropertyService.kt
// Modified On Timestamp: 2026-10-25 @ 16:45 EDT
// Created On Timestamp: 2026-10-25 @ 16:45 EDT
// File Description: Real-property details of a house, cottage, condo, land or building (AST-06 to AST-17):
//   fields from RealPropertyCatalogue, rooms, systems, outbuildings, owners, residence years and custom fields.
// Uses: RealPropertyCatalogue; AssetService (the asset and its group); the ledger's RealProperty.sq queries.
// Used By: The desktop Property tabs and fact sheet; the estate summary and home inventory; tests.
// Purpose: Validate and keep every property detail in the asset's group ledger (HH-11).
package ca.schippers.hfm.books

import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.domain.Ids
import ca.schippers.hfm.domain.PermissionLevel
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode

/** AST-11: one room. Lengths in metres, [area] in square metres; [areaOrComputed] multiplies when only the sides are given. */
data class PropertyRoom(
    val id: String = "",
    val type: String,
    val name: String? = null,
    val level: String? = null,
    val length: BigDecimal? = null,
    val width: BigDecimal? = null,
    val area: BigDecimal? = null,
    val flooring: String? = null,
    val ceilingHeight: BigDecimal? = null,
    val features: String? = null,
    val condition: String? = null,
) {
    val areaOrComputed: BigDecimal? get() = area ?: if (length != null && width != null) length.multiply(width).setScale(2, RoundingMode.HALF_UP) else null
}

/** AST-12: one system; [type] comes from the list for its [kind] (RealPropertyCatalogue.systemTypes). */
data class PropertySystem(
    val id: String = "",
    val kind: String,
    val type: String? = null,
    val fuel: String? = null,
    val makeModel: String? = null,
    val capacity: String? = null,
    val installYear: Int? = null,
    val lastInspection: LocalDate? = null,
    val ownership: String? = null,
    val rentalProvider: String? = null,
    val rentalMonthly: Money? = null,
    val childAssetId: String? = null,
    val notes: String? = null,
)

/** AST-13: a building or fixture on the land other than the main building. [area] in square metres. */
data class PropertyStructure(
    val id: String = "",
    val type: String,
    val name: String? = null,
    val area: BigDecimal? = null,
    val yearBuilt: Int? = null,
    val construction: String? = null,
    val heated: Boolean = false,
    val electricity: Boolean = false,
    val water: Boolean = false,
    val childAssetId: String? = null,
    val notes: String? = null,
)

/** AST-15: a title holder; [shareBp] in basis points (10,000 = 100 %). */
data class PropertyOwner(val memberId: String, val shareBp: Int? = null, val tenancy: String? = null)

/** AST-17: a year [memberId] designated the property as principal residence. */
data class ResidenceYear(val memberId: String, val year: Int)

/** AST-06: a detail the catalogue does not cover, in the user's words. */
data class PropertyCustomField(val id: String = "", val label: String, val value: String? = null)

/**
 * AST-06 to AST-17: everything recorded about one property. [fields] holds the catalogue's single
 * values by key, as stored (see [PropertyFieldType]); the typed readers below interpret them.
 */
data class RealProperty(
    val assetId: String,
    val fields: Map<String, String> = emptyMap(),
    val rooms: List<PropertyRoom> = emptyList(),
    val systems: List<PropertySystem> = emptyList(),
    val structures: List<PropertyStructure> = emptyList(),
    val owners: List<PropertyOwner> = emptyList(),
    val residenceYears: List<ResidenceYear> = emptyList(),
    val custom: List<PropertyCustomField> = emptyList(),
) {
    fun text(key: String): String? = fields[key]

    fun decimal(key: String): BigDecimal? = fields[key]?.let(::BigDecimal)

    fun integer(key: String): Int? = fields[key]?.toInt()

    fun yes(key: String): Boolean = fields[key] == "1"

    fun date(key: String): LocalDate? = fields[key]?.let(LocalDate::parse)

    fun money(key: String, currency: Currency): Money? = fields[key]?.toLong()?.let { Money.ofMinor(it, currency) }

    fun choices(key: String): List<String> = fields[key]?.split(',')?.filter { it.isNotEmpty() }.orEmpty()

    /** AST-07: the civic address on one or two lines, as written on an envelope; empty when none is given. */
    fun addressLines(): List<String> {
        val street = listOfNotNull(text("street_number"), text("street_name")).joinToString(" ")
        val first = listOfNotNull(text("unit")?.let { "$it-" }, street.ifBlank { null }).joinToString("")
        val town = listOfNotNull(text("municipality"), text("province")).joinToString(" ")
        val second = listOfNotNull(town.ifBlank { null }, text("postal_code")).joinToString("  ")
        return listOfNotNull(first.ifBlank { null }, text("address_line_2"), second.ifBlank { null })
    }
}

/** AST-17: a year designated as principal residence on more than one property. */
data class ResidenceConflict(val year: Int, val assetIds: List<String>)

/** Area and length units for entry and display; the values are stored in square metres and metres. */
enum class AreaUnit(val squareMetres: BigDecimal) {
    SQUARE_METRES(BigDecimal.ONE),
    SQUARE_FEET(BigDecimal("0.09290304")),
    ACRES(BigDecimal("4046.8564224")),
    HECTARES(BigDecimal("10000")),
    ;

    fun toSquareMetres(value: BigDecimal): BigDecimal = value.multiply(squareMetres).stripTrailingZeros()

    fun fromSquareMetres(value: BigDecimal, scale: Int = 2): BigDecimal = value.divide(squareMetres, scale, RoundingMode.HALF_UP)
}

enum class LengthUnit(val metres: BigDecimal) {
    METRES(BigDecimal.ONE),
    FEET(BigDecimal("0.3048")),
    ;

    fun toMetres(value: BigDecimal): BigDecimal = value.multiply(metres).stripTrailingZeros()

    fun fromMetres(value: BigDecimal, scale: Int = 2): BigDecimal = value.divide(metres, scale, RoundingMode.HALF_UP)
}

/**
 * AST-06 to AST-17: the real-property details of houses, cottages, condos, land and buildings,
 * kept in the ledger of the asset's group. Saving replaces what is recorded for the property.
 */
class RealPropertyService internal constructor(private val books: Books) {

    /** What is recorded for the property [assetId] (empty for a property with no details yet). */
    fun get(assetId: String): RealProperty {
        val (group, asset) = books.assets.locate(assetId)
        val q = books.ledger(group).realPropertyQueries
        val c = asset.currency
        return RealProperty(
            assetId,
            q.propertyFields(assetId).executeAsList().associate { it.field_ to it.value_ },
            q.propertyRooms(assetId).executeAsList().map {
                PropertyRoom(
                    it.id, it.room_type, it.name, it.level, it.length_m?.let(::BigDecimal), it.width_m?.let(::BigDecimal), it.area_m2?.let(::BigDecimal),
                    it.flooring, it.ceiling_height_m?.let(::BigDecimal), it.features, it.condition,
                )
            },
            q.propertySystems(assetId).executeAsList().map {
                PropertySystem(
                    it.id, it.system_kind, it.system_type, it.fuel, it.make_model, it.capacity, it.install_year?.toInt(), it.last_inspection_date?.let(LocalDate::parse),
                    it.ownership, it.rental_provider, it.rental_monthly_minor?.let { m -> Money.ofMinor(m, c) }, it.child_asset_id, it.notes,
                )
            },
            q.propertyStructures(assetId).executeAsList().map {
                PropertyStructure(
                    it.id, it.structure_type, it.name, it.area_m2?.let(::BigDecimal), it.year_built?.toInt(), it.construction, it.heated == 1L,
                    it.electricity == 1L, it.water == 1L, it.child_asset_id, it.notes,
                )
            },
            q.propertyOwners(assetId).executeAsList().map { PropertyOwner(it.member_id, it.share_bp?.toInt(), it.tenancy) },
            q.propertyResidenceYears(assetId).executeAsList().map { ResidenceYear(it.member_id, it.year.toInt()) },
            q.propertyCustom(assetId).executeAsList().map { PropertyCustomField(it.id, it.label, it.value_) },
        )
    }

    /** Saves [p] as the whole record of its property: fields, rows and years not in [p] are removed. */
    fun save(p: RealProperty): RealProperty {
        val (group, asset) = books.assets.locate(p.assetId)
        validate(asset.kind in REAL_PROPERTY_KINDS, "error.propertyKind")
        books.require(group, PermissionLevel.EDIT)
        val fields = normalize(p.fields)
        validateRows(p, asset.currency)
        val ledger = books.ledger(group)
        val q = ledger.realPropertyQueries
        val id = p.assetId
        ledger.transaction {
            val old = q.propertyFields(id).executeAsList().associate { it.field_ to it.value_ }
            (old.keys - fields.keys).forEach { q.deletePropertyField(id, it) }
            fields.filter { (k, v) -> old[k] != v }.forEach { (k, v) -> q.upsertPropertyField(id, k, v) }

            val rooms = p.rooms.map { it.copy(id = it.id.ifBlank { Ids.newId() }) }
            q.propertyRooms(id).executeAsList().map { it.id }.filter { r -> rooms.none { it.id == r } }.forEach(q::deletePropertyRoom)
            rooms.forEachIndexed { i, r ->
                q.upsertPropertyRoom(
                    r.id, id, r.type, r.name.blankToNull(), r.level, r.length?.plain(), r.width?.plain(), r.area?.plain(), r.flooring, r.ceilingHeight?.plain(),
                    r.features.blankToNull(), r.condition, i.toLong(),
                )
            }

            val systems = p.systems.map { it.copy(id = it.id.ifBlank { Ids.newId() }) }
            q.propertySystems(id).executeAsList().map { it.id }.filter { s -> systems.none { it.id == s } }.forEach(q::deletePropertySystem)
            systems.forEachIndexed { i, s ->
                q.upsertPropertySystem(
                    s.id, id, s.kind, s.type, s.fuel, s.makeModel.blankToNull(), s.capacity.blankToNull(), s.installYear?.toLong(), s.lastInspection?.toString(),
                    s.ownership, s.rentalProvider.blankToNull(), s.rentalMonthly?.minorUnits, s.childAssetId, s.notes.blankToNull(), i.toLong(),
                )
            }

            val structures = p.structures.map { it.copy(id = it.id.ifBlank { Ids.newId() }) }
            q.propertyStructures(id).executeAsList().map { it.id }.filter { s -> structures.none { it.id == s } }.forEach(q::deletePropertyStructure)
            structures.forEachIndexed { i, s ->
                q.upsertPropertyStructure(
                    s.id, id, s.type, s.name.blankToNull(), s.area?.plain(), s.yearBuilt?.toLong(), s.construction.blankToNull(), s.heated.bit(),
                    s.electricity.bit(), s.water.bit(), s.childAssetId, s.notes.blankToNull(), i.toLong(),
                )
            }

            q.propertyOwners(id).executeAsList().map { it.member_id }.filter { m -> p.owners.none { it.memberId == m } }.forEach { q.deletePropertyOwner(id, it) }
            p.owners.forEach { q.upsertPropertyOwner(id, it.memberId, it.shareBp?.toLong(), it.tenancy) }

            val years = p.residenceYears.toSet()
            q.propertyResidenceYears(id).executeAsList().map { ResidenceYear(it.member_id, it.year.toInt()) }.filter { it !in years }
                .forEach { q.deleteResidenceYear(id, it.memberId, it.year.toLong()) }
            years.forEach { q.insertResidenceYear(id, it.memberId, it.year.toLong()) }

            val custom = p.custom.map { it.copy(id = it.id.ifBlank { Ids.newId() }) }
            q.propertyCustom(id).executeAsList().map { it.id }.filter { c -> custom.none { it.id == c } }.forEach(q::deletePropertyCustom)
            custom.forEachIndexed { i, c -> q.upsertPropertyCustom(c.id, id, c.label.trim(), c.value.blankToNull(), i.toLong()) }
        }
        books.session.audit("UPDATE", ENTITY, id)
        return get(id)
    }

    /**
     * AST-17: years designated as principal residence on more than one property. From 1982 a family
     * unit designates one property a year; before that, each person could designate their own.
     */
    fun residenceConflicts(): List<ResidenceConflict> {
        val rows = books.groups().flatMap { g -> books.ledger(g).realPropertyQueries.allResidenceYears().executeAsList() }
        return rows.groupBy { it.year.toInt() }.flatMap { (year, list) ->
            if (year >= FAMILY_UNIT_FROM) {
                val assets = list.map { it.asset_id }.distinct()
                if (assets.size > 1) listOf(ResidenceConflict(year, assets.sorted())) else emptyList()
            } else {
                list.groupBy { it.member_id }.values.map { it.map { r -> r.asset_id }.distinct() }.filter { it.size > 1 }
                    .map { ResidenceConflict(year, it.sorted()) }
            }
        }.sortedBy { it.year }
    }

    // --- Validation -------------------------------------------------------------------------------

    /**
     * Checks each field against its type and returns the values as stored; blank values are dropped.
     * Amounts are minor units of the asset's currency.
     */
    private fun normalize(raw: Map<String, String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for ((key, value) in raw) {
            val v = value.trim()
            if (v.isEmpty()) continue
            if (key.endsWith(RealPropertyCatalogue.OTHER_SUFFIX)) {
                val base = key.removeSuffix(RealPropertyCatalogue.OTHER_SUFFIX)
                val field = RealPropertyCatalogue.field(base) ?: throw ValidationException("error.notFound")
                validate(field.choices != null && RealPropertyCatalogue.OTHER in RealPropertyCatalogue.choiceList(field.choices), "error.invalidChoice", key)
                validate(v.length <= TEXT_LIMIT, "error.textTooLong", TEXT_LIMIT)
                out[key] = v
                continue
            }
            val field = RealPropertyCatalogue.field(key) ?: throw ValidationException("error.notFound")
            out[key] = when (field.type) {
                PropertyFieldType.TEXT, PropertyFieldType.BILL, PropertyFieldType.ACCOUNT, PropertyFieldType.CONTACT ->
                    v.also { validate(it.length <= TEXT_LIMIT, "error.textTooLong", TEXT_LIMIT) }
                PropertyFieldType.LONG_TEXT -> v.also { validate(it.length <= LONG_TEXT_LIMIT, "error.textTooLong", LONG_TEXT_LIMIT) }
                PropertyFieldType.INTEGER -> wholeNumber(v, 0, 1_000_000).toString()
                PropertyFieldType.YEAR -> year(v).toString()
                PropertyFieldType.DECIMAL, PropertyFieldType.AREA, PropertyFieldType.LENGTH -> nonNegative(v).plain()
                PropertyFieldType.MONEY -> v.toLongOrNull()?.takeIf { it >= 0 }?.toString() ?: throw ValidationException("error.invalidNumber")
                PropertyFieldType.DATE -> runCatching { LocalDate.parse(v) }.getOrElse { throw ValidationException("error.invalidDate") }.toString()
                PropertyFieldType.YES_NO -> v.also { validate(it == "1" || it == "0", "error.invalidChoice", key) }
                PropertyFieldType.CHOICE -> v.also { validate(it in RealPropertyCatalogue.choiceList(field.choices!!), "error.invalidChoice", key) }
                PropertyFieldType.MULTI_CHOICE -> {
                    val list = RealPropertyCatalogue.choiceList(field.choices!!)
                    val picked = v.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    validate(picked.all { it in list }, "error.invalidChoice", key)
                    picked.sortedBy { list.indexOf(it) }.joinToString(",")
                }
                PropertyFieldType.PROVINCE -> (Province.of(v) ?: throw ValidationException("error.invalidChoice", key)).name
                PropertyFieldType.COUNTRY -> v.uppercase().also { validate(it.matches(COUNTRY), "error.countryCode") }
                PropertyFieldType.POSTAL_CODE -> v.uppercase()
                PropertyFieldType.LATITUDE -> coordinate(v, 90)
                PropertyFieldType.LONGITUDE -> coordinate(v, 180)
            }
        }
        // A Canadian postal code is written A1A 1A1 (the country is Canada unless another is given).
        out["postal_code"]?.let { code ->
            if ((out["country"] ?: "CA") == "CA") {
                val compact = code.replace(" ", "")
                validate(compact.matches(POSTAL_CODE), "error.postalCode")
                out["postal_code"] = compact.substring(0, 3) + " " + compact.substring(3)
            }
        }
        // The words of an Other choice are kept only while Other is chosen.
        out.keys.filter { it.endsWith(RealPropertyCatalogue.OTHER_SUFFIX) }.forEach { k ->
            val base = k.removeSuffix(RealPropertyCatalogue.OTHER_SUFFIX)
            if (out[base]?.split(',')?.contains(RealPropertyCatalogue.OTHER) != true) out.remove(k)
        }
        return out
    }

    private fun validateRows(p: RealProperty, currency: Currency) {
        val roomTypes = RealPropertyCatalogue.choiceList("roomType")
        p.rooms.forEach { r ->
            validate(r.type in roomTypes, "error.invalidChoice", "roomType")
            r.level?.let { validate(it in RealPropertyCatalogue.choiceList("roomLevel"), "error.invalidChoice", "roomLevel") }
            r.flooring?.let { validate(it in RealPropertyCatalogue.choiceList("flooring"), "error.invalidChoice", "flooring") }
            r.condition?.let { validate(it in RealPropertyCatalogue.choiceList("roomCondition"), "error.invalidChoice", "roomCondition") }
            listOfNotNull(r.length, r.width, r.area, r.ceilingHeight).forEach { validate(it.signum() >= 0, "error.invalidNumber") }
        }
        p.systems.forEach { s ->
            validate(s.kind in RealPropertyCatalogue.choiceList("systemKind"), "error.invalidChoice", "systemKind")
            val types = RealPropertyCatalogue.systemTypes[s.kind]
            s.type?.let { t -> validate(types != null && t in RealPropertyCatalogue.choiceList(types), "error.invalidChoice", "systemType") }
            s.fuel?.let { validate(it in RealPropertyCatalogue.choiceList("fuel"), "error.invalidChoice", "fuel") }
            s.ownership?.let { validate(it in RealPropertyCatalogue.choiceList("systemOwnership"), "error.invalidChoice", "systemOwnership") }
            s.installYear?.let { year(it.toString()) }
            s.rentalMonthly?.let {
                validate(!it.isNegative, "error.invalidNumber")
                validate(it.currency == currency, "error.currencyMismatch", currency.code)
            }
            s.childAssetId?.let { books.assets.get(it) }
        }
        p.structures.forEach { s ->
            validate(s.type in RealPropertyCatalogue.choiceList("structureType"), "error.invalidChoice", "structureType")
            s.yearBuilt?.let { year(it.toString()) }
            s.area?.let { validate(it.signum() >= 0, "error.invalidNumber") }
            s.childAssetId?.let { books.assets.get(it) }
        }
        val members = books.members.list(includeArchived = true).map { it.id }.toSet()
        validate(p.owners.map { it.memberId }.distinct().size == p.owners.size, "error.ownerShares")
        p.owners.forEach { o ->
            validate(o.memberId in members, "error.memberRequired")
            o.tenancy?.let { validate(it in RealPropertyCatalogue.choiceList("tenancy"), "error.invalidChoice", "tenancy") }
            o.shareBp?.let { validate(it in 1..FULL_SHARE, "error.ownerShares") }
        }
        // Shares, when given, are given for every owner and add up to 100 %.
        if (p.owners.any { it.shareBp != null }) {
            validate(p.owners.all { it.shareBp != null } && p.owners.sumOf { it.shareBp!! } == FULL_SHARE, "error.ownerShares")
        }
        p.residenceYears.forEach { y ->
            validate(y.memberId in members, "error.memberRequired")
            year(y.year.toString())
        }
        p.custom.forEach { c ->
            validate(c.label.isNotBlank(), "error.labelRequired")
            validate(c.label.length <= TEXT_LIMIT && (c.value?.length ?: 0) <= LONG_TEXT_LIMIT, "error.textTooLong", LONG_TEXT_LIMIT)
        }
    }

    private fun wholeNumber(v: String, min: Int, max: Int): Int =
        v.toIntOrNull()?.takeIf { it in min..max } ?: throw ValidationException("error.invalidNumber")

    private fun year(v: String): Int {
        val y = v.toIntOrNull()
        validate(y != null && y in FIRST_YEAR..(books.today().year + 5), "error.invalidYear")
        return y!!
    }

    private fun nonNegative(v: String): BigDecimal =
        runCatching { BigDecimal(v.replace(',', '.')) }.getOrNull()?.takeIf { it.signum() >= 0 } ?: throw ValidationException("error.invalidNumber")

    private fun coordinate(v: String, limit: Int): String {
        val d = runCatching { BigDecimal(v.replace(',', '.')) }.getOrNull()
        validate(d != null && d.abs() <= BigDecimal(limit), "error.coordinate")
        return d!!.plain()
    }

    private fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()

    private fun Boolean.bit(): Long = if (this) 1 else 0

    companion object {
        const val ENTITY = "real_property"
        const val TEXT_LIMIT = 200
        const val LONG_TEXT_LIMIT = 4000
        const val FULL_SHARE = 10_000

        /** The first year a family unit could designate only one principal residence (Income Tax Act s. 54). */
        const val FAMILY_UNIT_FROM = 1982

        /** The earliest year accepted for construction and residence years. */
        const val FIRST_YEAR = 1600

        private val COUNTRY = Regex("[A-Z]{2}")
        private val POSTAL_CODE = Regex("[ABCEGHJ-NPRSTVXY][0-9][ABCEGHJ-NPRSTV-Z][0-9][ABCEGHJ-NPRSTV-Z][0-9]")
    }
}
