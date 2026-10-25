// File path and name: app/desktop/src/test/kotlin/ca/schippers/hfm/desktop/ResidenceYearsTest.kt
// Modified On Timestamp: 2026-10-25 @ 19:30 EDT
// Created On Timestamp: 2026-10-25 @ 19:30 EDT
// File Description: Tests of how principal-residence years are typed and shown in the Property dialog.
// Uses: parseYears and formatYears (PropertyDialog.kt).
// Used By: ./gradlew :app:desktop:test.
// Purpose: "2015-2019, 2023" reads as the right years and is written back the same way (AST-17).
package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResidenceYearsTest {

    @Test
    fun `ranges and single years are read and written back`() {
        val years = parseYears("2023, 2015-2019; 2018")
        assertEquals(listOf(2015, 2016, 2017, 2018, 2019, 2023), years)
        assertEquals("2015-2019, 2023", formatYears(years))
        assertEquals("", formatYears(emptyList()))
        assertEquals(emptyList(), parseYears("  "))
        assertEquals("2001, 2003", formatYears(listOf(2003, 2001)))
    }

    @Test
    fun `nonsense is refused`() {
        for (text in listOf("20x5", "2019-2015", "2010-2011-2012", "1000-1500")) {
            assertFailsWith<ValidationException>(text) { parseYears(text) }
        }
    }
}
