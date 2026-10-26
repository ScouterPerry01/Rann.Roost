// File path and name: core/books/src/test/kotlin/ca/schippers/hfm/books/PropertyFactSheetTest.kt
// Modified On Timestamp: 2026-10-25 @ 21:13 EDT
// Created On Timestamp: 2026-10-25 @ 21:00 EDT
// File Description: Tests of the property fact sheet (AST-16): golden text of a fixed property in English,
//   the legal identifiers and owners left out, and French in metric units.
// Uses: PropertyFactSheet, RealPropertyService, a test household (SQLCipher JDBC driver).
// Used By: ./gradlew :core:books:test
// Purpose: Keep the printed fact sheet complete, in the right units and languages, and private when asked.
package ca.schippers.hfm.books

import ca.schippers.hfm.data.HouseholdStore
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.domain.MemberKind
import ca.schippers.hfm.i18n.Language
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyFactSheetTest {

    @TempDir
    lateinit var temp: Path

    private val store = HouseholdStore(SqlCipherJdbcDriverFactory(), KdfParams.TESTING)
    private lateinit var books: Books
    private lateinit var house: Asset
    private val day = LocalDate(2026, 10, 25)
    private val feet = FactSheetOptions(AreaUnit.SQUARE_FEET, LengthUnit.FEET)

    private fun ft(s: String) = LengthUnit.FEET.toMetres(BigDecimal(s))

    @BeforeEach
    fun setUp() {
        books = Books(store.create(temp.resolve("F.hfm"), "F", "perry", "Perry", "pw".toCharArray()).session)
        val group = books.groups().single().id
        house = books.assets.save(Asset("", group, AssetKind.HOME, "Maison"))
        val anne = books.members.create("Anne", MemberKind.ADULT).id
        val perry = books.members.create("Perry", MemberKind.ADULT).id
        books.realProperty.save(
            RealProperty(
                house.id,
                mapOf(
                    "property_type" to "DETACHED", "use" to "PRINCIPAL_RESIDENCE", "street_number" to "48", "street_name" to "Maple Crescent",
                    "municipality" to "Ottawa", "province" to "ON", "postal_code" to "K2B 4N7", "roll_number" to "0614 040 120 05300 0000",
                    "parcel_id" to "04123-0456 (LT)", "legal_description" to "Lot 27, Plan 4M-1182",
                    "lot_area" to AreaUnit.SQUARE_FEET.toSquareMetres(BigDecimal("5500")).toPlainString(), "lot_frontage" to ft("50").toPlainString(),
                    "year_built" to "2004", "exterior_finish" to "BRICK,OTHER", "exterior_finish.other" to "Board and batten", "backwater_valve" to "1",
                    "assessed_value" to "59100000",
                ),
                rooms = listOf(
                    PropertyRoom(type = "KITCHEN", level = "MAIN", length = ft("15"), width = ft("13"), flooring = "TILE"),
                    PropertyRoom(type = "BEDROOM", name = "Lily", level = "SECOND", length = ft("11"), width = ft("10")),
                ),
                systems = listOf(
                    PropertySystem(
                        kind = "WATER_HEATING", type = "TANK", fuel = "NATURAL_GAS", installYear = 2018, ownership = "RENTED", rentalProvider = "Enercare",
                        rentalMonthly = Money.parse("32.99", Currency.CAD),
                    ),
                ),
                structures = listOf(PropertyStructure(type = "SHED", area = AreaUnit.SQUARE_FEET.toSquareMetres(BigDecimal("100")), electricity = true)),
                owners = listOf(PropertyOwner(anne, 5000, "JOINT_TENANCY"), PropertyOwner(perry, 5000, "JOINT_TENANCY")),
                residenceYears = listOf(2021, 2022, 2023, 2025).map { ResidenceYear(perry, it) },
                custom = listOf(PropertyCustomField(label = "Paint colours", value = "Pale Oak OC-20"), PropertyCustomField(label = "Empty")),
            ),
        )
    }

    @AfterEach
    fun tearDown() = books.session.close()

    private fun sheet(language: Language, options: FactSheetOptions) =
        PropertyFactSheet(books, language, day).build(books.realProperty.get(house.id), house, options)

    @Test
    fun `the English fact sheet lists every filled-in detail in feet`() {
        val expected = """
            Property fact sheet: Maison
            Home
            48 Maple Crescent, Ottawa ON  K2B 4N7
            Prepared on 2026-10-25

            [Type and use]
            Property type: Detached
            Use: Principal residence

            [Address]
            Street number: 48
            Street: Maple Crescent
            Municipality: Ottawa
            Province or territory: Ontario
            Postal code: K2B 4N7

            [Legal and title]
            Assessment roll number: 0614 040 120 05300 0000
            Parcel identifier: 04123-0456 (LT)
            Legal description: Lot 27, Plan 4M-1182

            [Land]
            Lot area: 5,500 sq ft
            Frontage: 50 ft

            [Building]
            Year built: 2004
            Exterior finish: Brick, Board and batten

            [Basement]
            Backwater valve: Yes

            [Value and tax]
            Assessed value: $591,000.00

            [Rooms]
            Kitchen: Main floor · 15 ft × 13 ft · 195 sq ft · Tile
            Bedroom (Lily): Second floor · 11 ft × 10 ft · 110 sq ft
            Total area of the rooms: 305 sq ft

            [Systems]
            Water heating: Tank · Natural gas · Year installed: 2018 · Rented · Rented from: Enercare · $32.99 a month

            [Outbuildings]
            Shed: 100 sq ft · Electricity

            [Owners]
            Anne: 50% · Joint tenancy
            Perry: 50% · Joint tenancy

            [Principal residence]
            Perry: 2021-2023, 2025

            [Other details]
            Paint colours: Pale Oak OC-20
        """.trimIndent() + "\n"
        assertEquals(expected, sheet(Language.ENGLISH, feet).text())
    }

    @Test
    fun `legal identifiers and owners can be left out`() {
        val text = sheet(Language.ENGLISH, feet.copy(legal = false, owners = false)).text()
        for (gone in listOf("[Legal and title]", "0614 040 120", "04123-0456", "Plan 4M-1182", "[Owners]", "Anne", "[Principal residence]", "2021-2023")) {
            assertFalse(gone in text, "\"$gone\" should be left out")
        }
        assertTrue("[Address]" in text && "Lot area" in text && "[Rooms]" in text)
    }

    @Test
    fun `the French fact sheet uses French texts and metric units`() {
        val text = sheet(Language.FRENCH, FactSheetOptions(AreaUnit.SQUARE_METRES, LengthUnit.METRES)).text()
        assertTrue(text.startsWith("Fiche descriptive de la propriété : Maison\n"), text)
        assertTrue("Préparée le 2026-10-25" in text)
        // 15 x 13 ft = 4.572 x 3.9624 m; 195 sq ft = 18.116 m², shown to two decimals.
        assertTrue("4,57 m × 3,96 m" in text, text)
        assertTrue("18,12 m²" in text, text)
        assertTrue("Anne: 50 % · " in text, text)
        assertTrue("Année d’installation : 2018" in text, text)
    }
}
