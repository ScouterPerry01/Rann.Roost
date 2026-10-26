// File path and name: core/books/src/test/kotlin/ca/schippers/hfm/books/DeterministicIdsTest.kt
// Modified On Timestamp: 2026-10-26 @ 03:10 EDT
// Created On Timestamp: 2026-10-26 @ 03:10 EDT
// File Description: Rows the app makes by itself get name-based ids (plan-multi-device 4.5, 4.6).
// Uses: An encrypted test household; CategoryService, GoalService, BillService; Ids.nameBased.
// Used By: ./gradlew :core:books:test.
// Purpose: Two computers of one household must make one built-in or automatic row, not two.
package ca.schippers.hfm.books

import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.calc.schedule.Frequency
import ca.schippers.hfm.calc.schedule.Recurrence
import ca.schippers.hfm.data.HouseholdStore
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.domain.AccountType
import ca.schippers.hfm.domain.CategoryKind
import ca.schippers.hfm.domain.Ids
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import ca.schippers.hfm.security.KdfParams
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicIdsTest {

    @TempDir
    lateinit var temp: Path

    private val store = HouseholdStore(SqlCipherJdbcDriverFactory(), KdfParams.TESTING)
    private lateinit var books: Books
    private lateinit var group: GroupInfo
    private lateinit var chequing: Account
    private fun d(m: Int, day: Int) = LocalDate(2026, m, day)
    private fun cad(s: String) = Money.parse(s, Currency.CAD)

    @BeforeEach
    fun setUp() {
        books = Books(store.create(temp.resolve("D.hfm"), "D", "perry", "Perry", "pw".toCharArray()).session)
        group = books.groups().single()
        chequing = books.accounts.create(AccountDraft(group.id, "Chequing", AccountType.CHEQUING, Currency.CAD, cad("1000"), d(1, 1)))
    }

    @AfterEach
    fun tearDown() = books.session.close()

    private fun assertBuiltInCategoriesNameBased() {
        val builtIn = books.categories.list(includeArchived = true).filter { it.systemKey != null }
        assertTrue(builtIn.size > 50)
        for (c in builtIn) assertEquals(Ids.nameBased(books.session.householdId, "category", c.systemKey!!), c.id, c.systemKey)
    }

    @Test
    fun `built-in categories, and those added for a new province, have name-based ids`() {
        assertBuiltInCategoriesNameBased()
        val before = books.categories.list(includeArchived = true).size
        books.setProvince(if (books.province == Province.ON) Province.QC else Province.ON)
        assertTrue(books.categories.list(includeArchived = true).size > before, "the new province adds its own categories")
        assertBuiltInCategoriesNameBased()
    }

    @Test
    fun `a user's own category still gets a new random id`() {
        val mine = books.categories.create(null, "Hobbies", "Loisirs", CategoryKind.EXPENSE)
        assertEquals(7, java.util.UUID.fromString(mine.id).version())
    }

    @Test
    fun `scheduled set-asides are named by goal and date`() {
        val goal = books.goals.save(SavingsGoal("", chequing.id, "Trip", cad("3000"), d(12, 31), cad("250"), Recurrence.MONTHLY, d(1, 15)))
        assertEquals(3, books.goals.postScheduled(d(3, 20)))
        val entries = books.goals.entries(goal.id).filter { it.kind == GoalEntryKind.SCHEDULED }
        assertEquals(listOf(d(1, 15), d(2, 15), d(3, 15)), entries.map { it.date }.sorted())
        for (e in entries) assertEquals(Ids.nameBased(goal.id, "scheduled", e.date.toString()), e.id)
    }

    @Test
    fun `a stored bill occurrence is named by bill and due date`() {
        val bill = books.bills.create(BillDraft(BillKind.BILL, "Hydro", cad("130"), chequing.id, Recurrence(Frequency.MONTHLY), d(1, 12)))
        books.bills.setAmount(bill.id, d(2, 12), cad("151.20"))
        books.bills.skip(bill.id, d(3, 12))
        val stored = books.ledger(group).ledgerQueries.occurrencesForBill(bill.id).executeAsList()
        assertEquals(listOf("2026-02-12", "2026-03-12"), stored.map { it.due_date }.sorted())
        for (o in stored) assertEquals(Ids.nameBased(bill.id, "occurrence", o.due_date), o.id)
    }
}
