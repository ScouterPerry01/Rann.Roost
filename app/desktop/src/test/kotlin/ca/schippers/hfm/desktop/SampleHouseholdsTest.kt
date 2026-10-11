// File path and name: app/desktop/src/test/kotlin/ca/schippers/hfm/desktop/SampleHouseholdsTest.kt
// Modified On Timestamp: 2026-10-11 @ 13:05 EDT
// Created On Timestamp: 2026-10-11 @ 12:47 EDT
// File Description: Tests that the family and single-person sample households build, in a few
//   provinces in the normal build and in every province and territory with sampleHouseholdsTest,
//   and hold what their stories promise.
// Uses: SampleHousehold, DemoPlace, core/books, SQLCipher driver.
// Used By: ./gradlew :app:desktop:test, and :app:desktop:sampleHouseholdsTest for every province.
// Purpose: Keep the "Open a sample household" choices working everywhere.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.AssetKind
import ca.schippers.hfm.books.Books
import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.data.HouseholdStore
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.domain.AccountType
import ca.schippers.hfm.domain.MemberKind
import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.security.KdfParams
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleHouseholdsTest {

    @Test
    fun `the family builds in Alberta in English and in Quebec in French`() {
        family(Province.AB, Language.ENGLISH)
        family(Province.QC, Language.FRENCH)
    }

    @Test
    fun `the single person builds in Ontario in English and in Nunavut in French`() {
        single(Province.ON, Language.ENGLISH)
        single(Province.NU, Language.FRENCH)
    }

    /** Each province or territory once for each household, the languages taking turns (several minutes). */
    @Test
    @Tag("samples")
    fun `both build in every province and territory`() {
        Province.entries.forEachIndexed { i, province ->
            family(province, Language.entries[i % 2])
            single(province, Language.entries[(i + 1) % 2])
        }
    }

    private fun build(kind: SampleHousehold, province: Province, language: Language, check: (Books) -> Unit) {
        kind.create(HouseholdStore(SqlCipherJdbcDriverFactory(), KdfParams.TESTING), language, province).use { session ->
            val books = Books(session).also { it.language = language }
            assertEquals(province, books.province, "$kind $province $language")
            check(books)
        }
    }

    private fun family(province: Province, language: Language) = build(SampleHousehold.FAMILY, province, language) { books ->
        val where = "$province $language"
        val members = books.members.list()
        assertEquals(2, members.count { it.kind == MemberKind.ADULT }, where)
        assertEquals(3, members.count { it.kind == MemberKind.CHILD }, where)
        val accounts = books.accounts.list().map { it.account }
        assertTrue(accounts.any { it.type == AccountType.MORTGAGE }, "$where: the house's mortgage")
        val resp = accounts.single { it.type == AccountType.RESP }
        assertEquals(3, books.plans.beneficiaries(resp.id).size, "$where: the RESP is for the three children")
        assertTrue(books.assets.list().any { it.kind == AssetKind.COTTAGE && it.notes.orEmpty().isNotBlank() }, "$where: the inherited cottage")
        assertEquals(4, accounts.count { it.type == AccountType.RRSP || it.type == AccountType.TFSA }, "$where: each parent's RRSP and TFSA")
        assertTrue(books.goals.list().size >= 5, "$where: several savings goals")
        assertTrue(books.volunteer.list().map { it.memberId }.toSet().size == 2, "$where: both parents volunteer")
        assertTrue(books.budgets.list().isNotEmpty(), where)
        assertTrue(books.contacts.proposals().isEmpty(), "$where: every institution, provider and insurer has its contact")
    }

    private fun single(province: Province, language: Language) = build(SampleHousehold.SINGLE, province, language) { books ->
        val where = "$province $language"
        assertEquals(1, books.members.list().size, where)
        val accounts = books.accounts.list()
        assertTrue(accounts.any { it.account.type == AccountType.TFSA } && accounts.any { it.account.type == AccountType.FHSA }, "$where: a TFSA and an FHSA")
        val card = accounts.single { it.account.type == AccountType.CREDIT_CARD }
        assertTrue(card.balance > card.account.openingBalance, "$where: the card balance has come down: ${card.balance}")
        assertTrue(accounts.any { it.account.type == AccountType.LOAN }, "$where: the student loan")
        assertTrue(books.rules.list().size >= 3, where)
        assertTrue(books.goals.list().size >= 3, where)
        assertTrue(books.contacts.proposals().isEmpty(), "$where: every institution, provider and insurer has its contact")
    }
}
