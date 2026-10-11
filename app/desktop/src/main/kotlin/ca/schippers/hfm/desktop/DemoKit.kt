// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/DemoKit.kt
// Modified On Timestamp: 2026-10-11 @ 13:05 EDT
// Created On Timestamp: 2026-10-11 @ 12:41 EDT
// File Description: What the family and single-person sample households share: the three sample
//   households to choose from, creating one in a temporary folder, and small helpers (amounts,
//   categories, dates, pay stubs whose income tax comes from the app's own tax rules).
// Uses: DemoPlace, DemoHousehold, core/books, core/calc IncomeTax, core/data HouseholdStore.
// Used By: DemoFamily, DemoSingle, Main.kt, GettingStarted (Open a sample household).
// Purpose: Let anyone try the app on a realistic household of their choice, then throw it away.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.Books
import ca.schippers.hfm.books.DeductionKind
import ca.schippers.hfm.books.PayDeduction
import ca.schippers.hfm.books.PayEarning
import ca.schippers.hfm.books.PayStub
import ca.schippers.hfm.books.TransactionDraft
import ca.schippers.hfm.books.TransferDraft
import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.calc.tax.IncomeTax
import ca.schippers.hfm.calc.tax.TaxInput
import ca.schippers.hfm.data.HouseholdSession
import ca.schippers.hfm.data.HouseholdStore
import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files

/** The sample households to try the app on, each in any province or territory and either language. */
enum class SampleHousehold {
    /** Alex and Sam: a couple with a child, a rental, a side business (the manual's household). */
    COUPLE,

    /** A couple with three young children, a mortgage, an inherited cottage and large portfolios. */
    FAMILY,

    /** A young active person on a modest salary, learning to budget and starting to invest. */
    SINGLE;

    /** Creates this sample household in a temporary folder, signed in as its owner. */
    fun create(store: HouseholdStore, language: Language, province: Province = DemoPlace.defaultFor(language)): HouseholdSession {
        val p = DemoPlace(province, language == Language.ENGLISH)
        return when (this) {
            COUPLE -> DemoHousehold.create(store, language, province)
            FAMILY -> DemoKit.newHousehold(store, p, "Famille Tremblay", "The Mitchell family", "Mathieu", "Michael") { DemoFamily(it, p).fill() }
            SINGLE -> DemoKit.newHousehold(store, p, "Budget de Camille", "Jordan's budget", "Camille", "Jordan") { DemoSingle(it, p).fill() }
        }
    }
}

/**
 * Helpers for building a sample household: [l] and [q] as in [DemoPlace], amounts in dollars,
 * categories by key, dates counted from [today], and only past activity entered.
 */
internal abstract class DemoKit(protected val books: Books, protected val p: DemoPlace) {
    protected val today: LocalDate = today()
    protected val language: Language get() = p.language

    protected fun l(fr: String, en: String) = p.l(fr, en)
    protected fun q(qc: String, other: String) = p.q(qc, other)
    protected fun cad(s: String): Money = Money.parse(s, Currency.CAD)
    protected fun cad(v: BigDecimal): Money = Money.of(v.setScale(2, RoundingMode.HALF_UP), Currency.CAD)
    protected fun cat(key: String): String = books.categories.list().first { it.systemKey == key }.id
    protected fun day(n: Int): LocalDate = today.plus(DatePeriod(days = n))
    protected fun monthsAgo(n: Int): LocalDate = today.minus(DatePeriod(months = n))

    /** The [dayOfMonth] of the month [back] months before this one (day 28 at most). */
    protected fun inMonth(back: Int, dayOfMonth: Int): LocalDate =
        LocalDate(today.year, today.month, 1).minus(DatePeriod(months = back)).let { LocalDate(it.year, it.month, minOf(dayOfMonth, 28)) }

    /** The year the current school year started (it starts in September). */
    protected val schoolYear: Int get() = if (today.month.ordinal >= 7) today.year else today.year - 1

    // Only past activity: anything that would fall after today is skipped.
    protected fun add(draft: TransactionDraft) { if (draft.date <= today) books.transactions.create(draft) }
    protected fun add(draft: TransferDraft) { if (draft.date <= today) books.transactions.transfer(draft) }

    /**
     * A pay stub of [gross] paid twice a month. The income tax withheld is the year's estimate from
     * the app's tax rules (Rates and rules) for [p]'s province, spread over 24 pays; the pension
     * plan and employment insurance amounts are round sample figures, not calculated.
     */
    protected fun payStub(employer: String, gross: BigDecimal, group: Money? = null, rrsp: Money? = null, pension: Money? = null, union: Money? = null): PayStub {
        val year = today.year
        val annual = gross * BigDecimal(24)
        val estimate = IncomeTax.estimate(year, p.province, mapOf(TaxInput.EMPLOYMENT to annual), false)
        fun perPay(v: BigDecimal) = cad(v.divide(BigDecimal(24), 2, RoundingMode.HALF_UP))
        val territory = p.province in setOf(Province.YT, Province.NT, Province.NU)
        val deductions = buildList {
            add(PayDeduction(DeductionKind.INCOME_TAX, l("Impôt fédéral", "Federal tax"), perPay(estimate.federalTax)))
            val provincial = when {
                p.quebec -> l("Impôt du Québec", "Quebec tax")
                else -> l(if (territory) "Impôt territorial" else "Impôt provincial", "${p.provinceName} tax")
            }
            add(PayDeduction(DeductionKind.INCOME_TAX, provincial, perPay(estimate.provincialTax)))
            if (p.quebec) {
                add(PayDeduction(DeductionKind.CPP_QPP, l("RRQ", "QPP"), cad("225.00")))
                add(PayDeduction(DeductionKind.EI_QPIP, l("AE", "EI"), cad("55.00")))
                add(PayDeduction(DeductionKind.EI_QPIP, l("RQAP", "QPIP"), cad("24.00")))
            } else {
                add(PayDeduction(DeductionKind.CPP_QPP, l("RPC", "CPP"), cad("210.00")))
                add(PayDeduction(DeductionKind.EI_QPIP, l("AE", "EI"), cad("70.00")))
            }
            pension?.let { add(PayDeduction(DeductionKind.PENSION, l("Régime de retraite", "Pension plan"), it)) }
            rrsp?.let { add(PayDeduction(DeductionKind.RRSP, l("REER collectif", "Group RRSP"), it)) }
            group?.let { add(PayDeduction(DeductionKind.GROUP_INSURANCE, l("Assurance collective", "Group insurance"), it)) }
            union?.let { add(PayDeduction(DeductionKind.UNION_DUES, l("Cotisation syndicale", "Union dues"), it)) }
        }
        return PayStub(employer, LocalDate(year, 1, 1), listOf(PayEarning(l("Salaire", "Regular pay"), cad(gross))), deductions)
    }

    /** The household's child benefit for [year]'s July-to-June payments, a month's worth, from the app's tax rules. */
    protected fun childBenefitPerMonth(year: Int, ownIncome: BigDecimal, spouseIncome: BigDecimal, children: Int): Money {
        val estimate = IncomeTax.estimate(
            year, p.province,
            mapOf(TaxInput.EMPLOYMENT to ownIncome, TaxInput.SPOUSE_NET_INCOME to spouseIncome, TaxInput.SPOUSE_WORKING_INCOME to spouseIncome, TaxInput.CHILDREN to BigDecimal(children)),
            false,
        )
        val annual = estimate.lines.filter { it.kind == ca.schippers.hfm.calc.tax.TaxLineKind.CHILD_BENEFIT }.fold(BigDecimal.ZERO) { s, line -> s + line.amount }
        return cad(annual.divide(BigDecimal(12), 2, RoundingMode.HALF_UP))
    }

    companion object {
        /**
         * A new household in a temporary folder, in [p]'s province and language, owned by [ownerFr]
         * or [ownerEn], filled by [fill]; the Getting started guide is hidden, as for Alex and Sam's.
         */
        fun newHousehold(store: HouseholdStore, p: DemoPlace, nameFr: String, nameEn: String, ownerFr: String, ownerEn: String, fill: (Books) -> Unit): HouseholdSession {
            val dir = Files.createTempDirectory("hfm-demo").resolve("Demo.hfm")
            val created = store.create(
                dir, p.l(nameFr, nameEn), DemoHousehold.LOGIN, p.l(ownerFr, ownerEn), DemoHousehold.PASSWORD.toCharArray(),
                locale = p.l("fr-CA", "en-CA"), province = p.province.name,
                sharedGroupName = ca.schippers.hfm.i18n.Messages.get(p.language, "group.sharedName"),
            )
            val books = Books(created.session).also { it.language = p.language }
            fill(books)
            books.putSetting(gettingStartedSetting(books.userId), "1")
            return created.session
        }
    }
}
