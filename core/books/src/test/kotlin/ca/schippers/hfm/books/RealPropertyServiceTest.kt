// File path and name: core/books/src/test/kotlin/ca/schippers/hfm/books/RealPropertyServiceTest.kt
// Modified On Timestamp: 2026-10-25 @ 20:40 EDT
// Created On Timestamp: 2026-10-25 @ 17:05 EDT
// File Description: Tests of RealPropertyService and RealPropertyCatalogue (AST-06 to AST-17).
// Uses: RealPropertyService, RealPropertyCatalogue, AssetService, an encrypted test household.
// Used By: ./gradlew :core:books:test.
// Purpose: Every field type is checked and stored as specified; rows, owners and years are kept and replaced.
package ca.schippers.hfm.books

import ca.schippers.hfm.data.HouseholdStore
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.domain.MemberKind
import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.i18n.Messages
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import ca.schippers.hfm.security.KdfParams
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RealPropertyServiceTest {

    @TempDir
    lateinit var temp: Path

    private val store = HouseholdStore(SqlCipherJdbcDriverFactory(), KdfParams.TESTING)
    private lateinit var books: Books
    private lateinit var group: String
    private lateinit var house: Asset
    private lateinit var cottage: Asset
    private lateinit var anne: String
    private lateinit var perry: String
    private val property get() = books.realProperty
    private fun cad(s: String) = Money.parse(s, Currency.CAD)

    @BeforeEach
    fun setUp() {
        books = Books(store.create(temp.resolve("P.hfm"), "P", "perry", "Perry", "pw".toCharArray()).session)
        group = books.groups().single().id
        house = books.assets.save(Asset("", group, AssetKind.HOME, "Maison"))
        cottage = books.assets.save(Asset("", group, AssetKind.COTTAGE, "Chalet"))
        anne = books.members.create("Anne", MemberKind.ADULT).id
        perry = books.members.create("Perry", MemberKind.ADULT).id
    }

    @AfterEach
    fun tearDown() = books.session.close()

    private fun fail(key: String, block: () -> Unit) = assertEquals(key, assertFailsWith<ValidationException> { block() }.key)

    @Test
    fun `every field type is checked and stored in its plain form`() {
        val saved = property.save(
            RealProperty(
                house.id,
                mapOf(
                    "street_number" to " 123A ", "street_name" to "Main St. W", "municipality" to "Ottawa", "province" to "on",
                    "postal_code" to "k1a0b1", "roll_number" to "0614 012 345 67890 0000", "year_built" to "1987", "storeys" to "1.50",
                    "lot_area" to "650,25", "assessed_value" to "61200000", "energy_audit_date" to "2024-05-01", "backwater_valve" to "1",
                    "exterior_finish" to "VINYL,BRICK,OTHER", "exterior_finish.other" to "Board and batten", "property_type" to "DETACHED",
                    "latitude" to "45.4215", "longitude" to "-75.6972", "unit" to "  ",
                ),
            ),
        )
        val f = saved.fields
        assertEquals("123A", f["street_number"])
        assertEquals("ON", f["province"])
        assertEquals("K1A 0B1", f["postal_code"])
        assertEquals("1.5", f["storeys"])
        assertEquals("650.25", f["lot_area"])
        assertEquals("BRICK,VINYL,OTHER", f["exterior_finish"], "multiple choices keep the list's order")
        assertEquals("Board and batten", f["exterior_finish.other"])
        assertTrue("unit" !in f, "blank values are not kept")
        assertEquals(cad("612000"), saved.money("assessed_value", Currency.CAD))
        assertEquals(LocalDate(2024, 5, 1), saved.date("energy_audit_date"))
        assertTrue(saved.yes("backwater_valve"))
        assertEquals(listOf("123A Main St. W", "Ottawa ON  K1A 0B1"), saved.addressLines())
    }

    @Test
    fun `wrong values are refused with the reason`() {
        fun one(key: String, value: String) = property.save(RealProperty(house.id, mapOf(key to value)))
        fail("error.postalCode") { one("postal_code", "12345") }
        fail("error.invalidYear") { one("year_built", "1200") }
        fail("error.invalidNumber") { one("lot_area", "-3") }
        fail("error.invalidChoice") { one("roof_material", "STRAW") }
        fail("error.invalidDate") { one("survey_date", "2024-13-01") }
        fail("error.coordinate") { one("latitude", "91") }
        fail("error.countryCode") { one("country", "Canada") }
        fail("error.textTooLong") { one("lot", "x".repeat(201)) }
        fail("error.notFound") { one("no_such_field", "1") }
        // A postal code outside Canada is kept as written.
        assertEquals("90210", property.save(RealProperty(house.id, mapOf("country" to "us", "postal_code" to "90210"))).fields["postal_code"])
    }

    @Test
    fun `the words of Other are dropped when Other is no longer chosen`() {
        property.save(RealProperty(house.id, mapOf("roof_material" to "OTHER", "roof_material.other" to "Thatch")))
        assertEquals("Thatch", property.get(house.id).fields["roof_material.other"])
        val now = property.save(RealProperty(house.id, mapOf("roof_material" to "METAL", "roof_material.other" to "Thatch")))
        assertEquals(mapOf("roof_material" to "METAL"), now.fields)
        fail("error.invalidChoice") { property.save(RealProperty(house.id, mapOf("year_built.other" to "1900"))) }
    }

    @Test
    fun `rooms, systems, outbuildings and custom fields are kept in order and replaced`() {
        val first = property.save(
            RealProperty(
                house.id,
                rooms = listOf(
                    PropertyRoom(type = "KITCHEN", level = "MAIN", length = BigDecimal("4.2"), width = BigDecimal("3.5"), flooring = "TILE"),
                    PropertyRoom(type = "PRIMARY_BEDROOM", name = "Notre chambre", level = "SECOND", area = BigDecimal("16")),
                ),
                systems = listOf(
                    PropertySystem(kind = "HEATING", type = "HEAT_PUMP_AIR", fuel = "ELECTRICITY", installYear = 2021, ownership = "OWNED"),
                    PropertySystem(kind = "WATER_HEATING", type = "TANK", fuel = "NATURAL_GAS", ownership = "RENTED", rentalProvider = "Enercare", rentalMonthly = cad("32.99")),
                    PropertySystem(kind = "SOLAR", makeModel = "8 kW array"),
                ),
                structures = listOf(PropertyStructure(type = "SHED", area = BigDecimal("9.3"), electricity = true)),
                custom = listOf(PropertyCustomField(label = "Hydro pole number", value = "HP-4471")),
            ),
        )
        assertEquals(listOf("KITCHEN", "PRIMARY_BEDROOM"), first.rooms.map { it.type })
        assertEquals(BigDecimal("14.70"), first.rooms[0].areaOrComputed)
        assertEquals(cad("32.99"), first.systems[1].rentalMonthly)
        assertTrue(first.structures.single().electricity)
        val kitchen = first.rooms[0]
        val again = property.save(first.copy(rooms = listOf(first.rooms[1], kitchen.copy(flooring = "HARDWOOD")), systems = first.systems.drop(1), custom = emptyList()))
        assertEquals(listOf(first.rooms[1].id, kitchen.id), again.rooms.map { it.id }, "rows keep their ids and take the new order")
        assertEquals("HARDWOOD", again.rooms[1].flooring)
        assertEquals(2, again.systems.size)
        assertTrue(again.custom.isEmpty())
        fail("error.invalidChoice") { property.save(RealProperty(house.id, systems = listOf(PropertySystem(kind = "COOLING", type = "BOILER")))) }
        fail("error.invalidChoice") { property.save(RealProperty(house.id, rooms = listOf(PropertyRoom(type = "BALLROOM")))) }
        fail("error.labelRequired") { property.save(RealProperty(house.id, custom = listOf(PropertyCustomField(label = " ")))) }
    }

    @Test
    fun `owners' shares add up to the whole`() {
        val joint = listOf(PropertyOwner(anne, 5_000, "JOINT_TENANCY"), PropertyOwner(perry, 5_000, "JOINT_TENANCY"))
        assertEquals(joint, property.save(RealProperty(house.id, owners = joint)).owners)
        fail("error.ownerShares") { property.save(RealProperty(house.id, owners = listOf(PropertyOwner(anne, 6_000), PropertyOwner(perry, 5_000)))) }
        fail("error.ownerShares") { property.save(RealProperty(house.id, owners = listOf(PropertyOwner(anne, 10_000), PropertyOwner(perry)))) }
        fail("error.memberRequired") { property.save(RealProperty(house.id, owners = listOf(PropertyOwner("nobody")))) }
        // Owners without shares are fine (the deed does not always say).
        assertEquals(2, property.save(RealProperty(house.id, owners = joint.map { it.copy(shareBp = null) })).owners.size)
    }

    @Test
    fun `principal residence years on two properties are flagged from 1982, per person before`() {
        property.save(RealProperty(house.id, residenceYears = (1979..1984).map { ResidenceYear(anne, it) }))
        property.save(RealProperty(cottage.id, residenceYears = listOf(ResidenceYear(perry, 1980), ResidenceYear(anne, 1981), ResidenceYear(perry, 1983))))
        val conflicts = property.residenceConflicts()
        assertEquals(listOf(1981, 1983), conflicts.map { it.year }, "1980: two people, two properties, allowed before 1982")
        assertEquals(setOf(house.id, cottage.id), conflicts.first().assetIds.toSet())
        // Removing a year removes the conflict.
        property.save(RealProperty(cottage.id, residenceYears = listOf(ResidenceYear(perry, 1980))))
        assertTrue(property.residenceConflicts().isEmpty())
    }

    @Test
    fun `only real property has property details, and they go with the asset`() {
        val boat = books.assets.save(Asset("", group, AssetKind.BOAT, "Chaloupe"))
        fail("error.propertyKind") { property.save(RealProperty(boat.id, mapOf("lot" to "12"))) }
        val land = books.assets.save(Asset("", group, AssetKind.LAND, "Terre à bois"))
        property.save(RealProperty(land.id, mapOf("lot_area" to "404685.64"), owners = listOf(PropertyOwner(anne))))
        books.assets.delete(land.id)
        assertFailsWith<ValidationException> { property.get(land.id) }
        assertEquals(REAL_PROPERTY_KINDS, setOf(AssetKind.HOME, AssetKind.COTTAGE, AssetKind.CONDO, AssetKind.LAND, AssetKind.BUILDING))
    }

    @Test
    fun `units convert exactly`() {
        assertEquals(BigDecimal("0.09290304"), AreaUnit.SQUARE_FEET.toSquareMetres(BigDecimal.ONE))
        assertEquals(BigDecimal("2000.00"), AreaUnit.SQUARE_FEET.fromSquareMetres(AreaUnit.SQUARE_FEET.toSquareMetres(BigDecimal(2000))))
        assertEquals(BigDecimal("1.00"), AreaUnit.ACRES.fromSquareMetres(BigDecimal("4046.8564224")))
        assertEquals(BigDecimal("30.48"), LengthUnit.FEET.toMetres(BigDecimal(100)).setScale(2))
    }

    @Test
    fun `the catalogue is consistent and every text exists in English and French`() {
        val keys = RealPropertyCatalogue.fields.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "field keys are unique")
        RealPropertyCatalogue.fields.filter { it.type == PropertyFieldType.CHOICE || it.type == PropertyFieldType.MULTI_CHOICE }
            .forEach { RealPropertyCatalogue.choiceList(it.choices!!) }
        RealPropertyCatalogue.systemTypes.values.forEach { RealPropertyCatalogue.choiceList(it) }
        val missing = (RealPropertyCatalogue.textKeys() + AssetKind.entries.map { "assetKind.$it" })
            .filter { key -> Language.entries.any { key !in Messages.keys(it) } }
        assertTrue(missing.isEmpty(), "missing texts: $missing")
    }

    @Test
    fun `a room measured in feet shows its area in whole square feet`() {
        // 8 x 7 ft and 17 x 14 ft: rounding the square metres first gave 55.97 and 237.99 sq ft.
        for ((l, w) in listOf(8 to 7, 17 to 14, 24 to 18)) {
            val room = PropertyRoom(type = "LIVING", length = LengthUnit.FEET.toMetres(BigDecimal(l)), width = LengthUnit.FEET.toMetres(BigDecimal(w)))
            assertEquals(BigDecimal(l * w).setScale(2), AreaUnit.SQUARE_FEET.fromSquareMetres(room.areaOrComputed!!))
        }
        assertEquals(0, BigDecimal("14.7").compareTo(PropertyRoom(type = "KITCHEN", length = BigDecimal("4.2"), width = BigDecimal("3.5")).areaOrComputed))
    }
}
