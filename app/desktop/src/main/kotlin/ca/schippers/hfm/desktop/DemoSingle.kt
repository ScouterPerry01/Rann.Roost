// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/DemoSingle.kt
// Modified On Timestamp: 2026-10-11 @ 13:05 EDT
// Created On Timestamp: 2026-10-11 @ 12:46 EDT
// File Description: The single-person sample household: a young, active renter on a modest salary,
//   learning to budget, paying down a credit card and a student loan, and starting to invest.
// Uses: DemoKit, DemoPlace, core/books services, core/calc loan projection.
// Used By: SampleHousehold.SINGLE (DemoKit.kt).
// Purpose: Show the app filled in for someone starting out, in any province or territory and language.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.Account
import ca.schippers.hfm.books.AccountDraft
import ca.schippers.hfm.books.AlertSettings
import ca.schippers.hfm.books.AmountKind
import ca.schippers.hfm.books.Asset
import ca.schippers.hfm.books.AssetKind
import ca.schippers.hfm.books.BillDraft
import ca.schippers.hfm.books.BillKind
import ca.schippers.hfm.books.BillType
import ca.schippers.hfm.books.Books
import ca.schippers.hfm.books.BudgetPeriod
import ca.schippers.hfm.books.CreditCardTerms
import ca.schippers.hfm.books.EventCategory
import ca.schippers.hfm.books.EventDraft
import ca.schippers.hfm.books.GatherDecision
import ca.schippers.hfm.books.HealthProvider
import ca.schippers.hfm.books.InsurancePolicy
import ca.schippers.hfm.books.Institution
import ca.schippers.hfm.books.InvestmentKind
import ca.schippers.hfm.books.InvestmentTxn
import ca.schippers.hfm.books.LoanDetails
import ca.schippers.hfm.books.MedCoverage
import ca.schippers.hfm.books.MedExpense
import ca.schippers.hfm.books.MedPlan
import ca.schippers.hfm.books.MedPlanKind
import ca.schippers.hfm.books.MedService
import ca.schippers.hfm.books.Member
import ca.schippers.hfm.books.PaymentMethod
import ca.schippers.hfm.books.PersonSchedule
import ca.schippers.hfm.books.PlanPerson
import ca.schippers.hfm.books.PolicyKind
import ca.schippers.hfm.books.PremiumFrequency
import ca.schippers.hfm.books.ProviderKind
import ca.schippers.hfm.books.RoomEntry
import ca.schippers.hfm.books.RoomPlan
import ca.schippers.hfm.books.SavingsGoal
import ca.schippers.hfm.books.ScheduleKind
import ca.schippers.hfm.books.ScheduleShift
import ca.schippers.hfm.books.SearchService
import ca.schippers.hfm.books.Security
import ca.schippers.hfm.books.SecurityKind
import ca.schippers.hfm.books.SplitDraft
import ca.schippers.hfm.books.TransactionDraft
import ca.schippers.hfm.books.TransferDraft
import ca.schippers.hfm.books.VolunteerEntry
import ca.schippers.hfm.books.VolunteerKind
import ca.schippers.hfm.books.AssetClass
import ca.schippers.hfm.books.Region
import ca.schippers.hfm.calc.loan.Compounding
import ca.schippers.hfm.calc.loan.LoanPlan
import ca.schippers.hfm.calc.loan.LoanProjection
import ca.schippers.hfm.calc.loan.LoanTerms
import ca.schippers.hfm.calc.loan.PaymentFrequency
import ca.schippers.hfm.calc.schedule.Recurrence
import ca.schippers.hfm.domain.AccountType
import ca.schippers.hfm.domain.MemberKind
import ca.schippers.hfm.money.Currency
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.math.BigDecimal

/**
 * The single person: [me], 26, a marketing coordinator earning $58,000, renting with a roommate.
 * Six months of activity in which restaurant spending comes down as the budget takes hold, the
 * credit card balance is paid down, and monthly purchases start a TFSA and an FHSA.
 */
internal class DemoSingle(books: Books, p: DemoPlace) : DemoKit(books, p) {
    private val group = books.groups().first().id
    private val start = monthsAgo(6)
    private lateinit var me: Member
    private lateinit var chequing: Account
    private lateinit var savings: Account
    private lateinit var card: Account

    private val employer get() = q("Agence Pixel inc.", "Brightside Marketing Ltd.")
    private val climbing get() = q("Délire Escalade", "Boulders Climbing Gym")
    private val runningClub get() = l("Club de course ${p.city}", "${p.city} Run Club")

    fun fill() {
        me = books.members.create(l("Camille", "Jordan"), MemberKind.ADULT, LocalDate(today.year - 26, 5, 3))
        addAccounts()
        addMonthlyLife()
        addBills()
        addInvesting()
        addActiveLife()
        addHealthAndInsurance()
        addGoalsBudgetsAndRules()
        books.contacts.let { c -> c.gather(group, c.proposals().flatMap { pr -> pr.sources.groupBy { SearchService.fold(it.name) }.values.map { GatherDecision(it) } }) }
    }

    private fun addAccounts() {
        val bank = books.institutions.create(Institution("", q("Desjardins", "Tangerine"), institutionNumber = q("815", "614")))
        chequing = books.accounts.create(AccountDraft(group, l("Compte chèques", "Chequing"), AccountType.CHEQUING, Currency.CAD, cad("1240.00"), start, bank.id, q("815-30455-0912233", "614-00152-3301984"), setOf(me.id)))
        savings = books.accounts.create(AccountDraft(group, l("Épargne", "Savings"), AccountType.HIGH_INTEREST_SAVINGS, Currency.CAD, cad("1500.00"), start, bank.id, ownerMemberIds = setOf(me.id)))
        // The card carried a balance from moving into the apartment; it comes down each month.
        card = books.accounts.create(AccountDraft(group, q("Visa Desjardins", "Tangerine Mastercard"), AccountType.CREDIT_CARD, Currency.CAD, cad("-3240.00"), start, bank.id, "5301776655443322", setOf(me.id)))
        books.creditCards.saveTerms(card.id, CreditCardTerms(cad("5000"), BigDecimal("0.1995"), statementDay = 12, dueDay = 3, minPaymentPercent = BigDecimal("0.03"), minPaymentFloor = cad("10")))
        books.accountAlerts.save(AlertSettings(card.id, nearLimitPercent = 60, overLimit = true))
        books.accountAlerts.save(AlertSettings(chequing.id, lowBalance = cad("300.00")))

        // The student loan: the federal part is interest-free; Quebec's loans are with a bank.
        val loanStart = LocalDate(today.year - 3, 11, 30)
        val terms = LoanTerms(cad("21500"), BigDecimal(q("0.0645", "0")), Compounding.MONTHLY, 114, PaymentFrequency.MONTHLY)
        val owed = LoanProjection.project(LoanPlan(terms, loanStart)).balanceOn(start.minus(DatePeriod(days = 1)))
        val loanName = q(l("Prêt étudiant (AFE)", "Student loan (AFE)"), l("Prêt d’études canadien", "Canada Student Loan"))
        val loan = books.accounts.create(AccountDraft(group, loanName, AccountType.LOAN, Currency.CAD, -owed, loanStart, bank.id, "NSLSC-44812207", setOf(me.id)))
        books.loans.save(LoanDetails(loan.id, terms.principal, terms.annualRate, compounding = terms.compounding, amortizationMonths = terms.amortizationMonths, firstPaymentDate = loanStart, paymentAccountId = chequing.id, lastPaidDate = start.minus(DatePeriod(days = 1))))
        while (true) {
            val payment = books.loans.nextPayment(loan.id, today)?.takeIf { it.date <= today } ?: break
            books.loans.recordPayment(loan.id, chequing.id, payment)
        }
    }

    /** Pay, rent, groceries, coffee and restaurants (coming down month by month), transit and the card's payments. */
    private fun addMonthlyLife() {
        val stub = payStub(employer, BigDecimal("2416.67"), group = cad("18.40"))
        val grocer = q("Maxi", if (p.province == ca.schippers.hfm.calc.Province.ON) "No Frills" else p.t.grocer)
        val transit = if (p.quebec) "RTC" else p.t.transit
        for (back in 6 downTo 0) {
            fun on(d: Int) = inMonth(back, d)
            for (d in listOf(1, 15)) add(books.payStubs.draft(chequing.id, stub.copy(payDate = on(d)), me.id))
            add(TransactionDraft(chequing.id, on(1), cad("-1150.00"), l("Loyer (Virement Interac)", "Rent (Interac e-Transfer)"), listOf(SplitDraft(cat("housing.rent"), cad("-1150.00"))), memo = l("Ma part; l’autre moitié est payée par Sam", "My half; my roommate Sam pays the rest")))
            for ((d, amount) in listOf(4 to "72.40", 11 to "58.15", 18 to "81.30", 25 to "64.75")) {
                add(TransactionDraft(card.id, on(d), cad("-$amount"), grocer, listOf(SplitDraft(cat("food.groceries"), cad("-$amount")))))
            }
            // The budget at work: fewer meals out each month.
            val fewer = (6 - back).coerceAtMost(4)
            for (i in 0 until 6 - fewer) {
                add(TransactionDraft(card.id, on(5 + i * 4), cad("-34.60"), listOf(q("Le Cercle", "The Local"), q("Pizzeria No.900", "Pizza Pizza"), "Sushi Shop")[i % 3], listOf(SplitDraft(cat("food.restaurants"), cad("-34.60")))))
            }
            for (d in listOf(3, 9, 16, 23)) add(TransactionDraft(card.id, on(d), cad("-6.85"), "Tim Hortons", listOf(SplitDraft(cat("food.coffee_snacks"), cad("-6.85")))))
            transit?.let { add(TransactionDraft(chequing.id, on(1), cad("-104.00"), it, listOf(SplitDraft(cat("transport.transit.pass"), cad("-104.00"))), memo = l("Laissez-passer mensuel", "Monthly pass"))) }
            add(TransactionDraft(card.id, on(13), cad("-89.99"), "Simons", listOf(SplitDraft(cat("personal.clothing"), cad("-89.99")))))
            add(TransferDraft(chequing.id, card.id, on(4), cad("680.00"), memo = l("Remboursement de la carte", "Paying down the card")))
            add(TransferDraft(chequing.id, savings.id, on(16), cad("250.00"), memo = l("Fonds d’urgence", "Emergency fund")))
        }
    }

    private fun addBills() {
        fun next(dayOfMonth: Int): LocalDate = LocalDate(today.year, today.month, dayOfMonth).let { if (it > today) it else it.plus(DatePeriod(months = 1)) }
        val bills = books.bills
        bills.create(
            BillDraft(BillKind.BILL, l("Loyer", "Rent"), cad("1150.00"), chequing.id, Recurrence.MONTHLY, next(1), l("Propriétaire", "Landlord"), categoryId = cat("housing.rent"),
                paymentMethod = PaymentMethod.ONLINE, type = BillType.HOME, categoryKey = "home.essential", subcategoryKey = "home.essential.rent"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Cellulaire", "Cell phone"), cad("45.00"), card.id, Recurrence.MONTHLY, next(20), q("Fizz", "Public Mobile"), "PM-2290 1144", AmountKind.VARIABLE,
                paymentMethod = PaymentMethod.CARD, categoryId = cat("utilities.mobile"), type = BillType.HOME, categoryKey = "home.essential", subcategoryKey = "home.essential.cell_phone"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Internet (ma moitié)", "Internet (my half)"), cad("40.00"), chequing.id, Recurrence.MONTHLY, next(18), q("Vidéotron", p.t.internet),
                paymentMethod = PaymentMethod.ONLINE, categoryId = cat("utilities.internet"), type = BillType.HOME, categoryKey = "home.essential", subcategoryKey = "home.essential.home_internet"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Musique en continu", "Music streaming"), cad("11.99"), card.id, Recurrence.MONTHLY, day(9), "Spotify", paymentMethod = PaymentMethod.CARD,
                categoryId = cat("leisure.subscriptions"), isSubscription = true, type = BillType.HOME, categoryKey = "home.lifestyle", subcategoryKey = "home.lifestyle.streaming"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Abonnement d’escalade", "Climbing gym membership"), cad("79.00"), card.id, Recurrence.MONTHLY, next(2), climbing, paymentMethod = PaymentMethod.CARD,
                categoryId = cat("personal.fitness"), isSubscription = true, type = BillType.HOME, categoryKey = "home.lifestyle", subcategoryKey = "home.lifestyle.gym"),
        )
        bills.create(BillDraft(BillKind.TRANSFER, l("Fonds d’urgence", "Emergency fund"), cad("250.00"), chequing.id, Recurrence.MONTHLY, next(16), transferAccountId = savings.id))
    }

    /** A TFSA and an FHSA opened this year, a little each month into all-in-one ETFs. */
    private fun addInvesting() {
        val broker = books.institutions.create(Institution("", "Wealthsimple"))
        val opened = monthsAgo(8)
        val tfsa = books.accounts.create(AccountDraft(group, l("CELI", "TFSA"), AccountType.TFSA, Currency.CAD, cad("0"), opened, broker.id, "WS-TFSA-7712", setOf(me.id)))
        val fhsa = books.accounts.create(AccountDraft(group, l("CELIAPP", "FHSA"), AccountType.FHSA, Currency.CAD, cad("0"), opened, broker.id, "WS-FHSA-7713", setOf(me.id)))
        val inv = books.investments
        val xeqt = inv.saveSecurity(Security("", "XEQT", "TSX", "iShares Core Equity ETF Portfolio", SecurityKind.ETF, Currency.CAD, AssetClass.EQUITY, Region.GLOBAL))
        val vbal = inv.saveSecurity(Security("", "VBAL", "TSX", "Vanguard Balanced ETF Portfolio", SecurityKind.ETF, Currency.CAD, AssetClass.EQUITY, Region.GLOBAL))
        for (back in 7 downTo 0) {
            val date = inMonth(back, 17)
            if (date > today) continue
            add(TransferDraft(chequing.id, tfsa.id, date, cad("200.00"), memo = l("Cotisation CELI", "TFSA contribution")))
            val price = BigDecimal("33.10") + BigDecimal(7 - back).multiply(BigDecimal("0.40"))
            inv.save(InvestmentTxn("", tfsa.id, date, InvestmentKind.BUY, xeqt.id, BigDecimal(6), price, cad(price * BigDecimal(6)), cad("0")))
            if (back <= 4) {
                add(TransferDraft(chequing.id, fhsa.id, date, cad("300.00"), memo = l("Cotisation CELIAPP", "FHSA contribution")))
                val balanced = BigDecimal("32.40") + BigDecimal(4 - back).multiply(BigDecimal("0.25"))
                inv.save(InvestmentTxn("", fhsa.id, date, InvestmentKind.BUY, vbal.id, BigDecimal(9), balanced, cad(balanced * BigDecimal(9)), cad("0")))
            }
        }
        inv.setPrice(xeqt.id, today, BigDecimal("36.20"))
        inv.setPrice(vbal.id, today, BigDecimal("33.70"))
        val plans = books.plans
        plans.saveRoom(RoomEntry("", me.id, RoomPlan.TFSA, today.year, cad("41500")), group)
        plans.saveRoom(RoomEntry("", me.id, RoomPlan.FHSA, today.year, cad("8000")), group)
        plans.saveRoom(RoomEntry("", me.id, RoomPlan.RRSP, today.year, cad("9850")), group)
    }

    /** The climbing gym, the run club, a rec soccer league, a half-marathon and the bike, with its tune-up. */
    private fun addActiveLife() {
        val calendar = books.calendar
        fun last(dayOfWeek: Int) = today.minus(DatePeriod(days = ((today.dayOfWeek.ordinal - dayOfWeek + 7) % 7).let { if (it == 0) 7 else it }))
        calendar.create(EventDraft(group, l("Escalade", "Climbing"), EventCategory.ACTIVITY, last(DayOfWeek.TUESDAY.ordinal), LocalTime(18, 30), 120, climbing, memberId = me.id, recurrence = Recurrence.WEEKLY, reminderMinutes = listOf(60)))
        calendar.create(EventDraft(group, l("Course en groupe", "Group run"), EventCategory.ACTIVITY, last(DayOfWeek.SATURDAY.ordinal), LocalTime(8, 0), 75, runningClub, memberId = me.id, recurrence = Recurrence.WEEKLY, reminderMinutes = listOf(60)))
        calendar.create(EventDraft(group, l("Soccer récréatif", "Rec soccer"), EventCategory.ACTIVITY, last(DayOfWeek.WEDNESDAY.ordinal), LocalTime(20, 0), 60, q("Parc Victoria", p.t.gamePark), memberId = me.id, recurrence = Recurrence.WEEKLY, endDate = day(40)))
        calendar.create(EventDraft(group, l("Demi-marathon", "Half-marathon"), EventCategory.ACTIVITY, day(34), LocalTime(7, 30), 150, l("Départ au centre-ville", "Downtown start line"), memberId = me.id, reminderMinutes = listOf(2 * 1440)))
        add(TransactionDraft(card.id, day(-40), cad("-95.00"), l("Demi-marathon de ${p.city}", "${p.city} Half-Marathon"), listOf(SplitDraft(cat("personal.fitness"), cad("-95.00"))), memo = l("Inscription", "Registration")))
        add(TransactionDraft(card.id, day(-120), cad("-140.00"), l("Ligue de soccer récréatif", "Rec soccer league"), listOf(SplitDraft(cat("personal.fitness"), cad("-140.00")))))
        add(TransactionDraft(card.id, day(-65), cad("-169.99"), q("La Cordée", "Running Room"), listOf(SplitDraft(cat("personal.fitness"), cad("-169.99"))), memo = l("Souliers de course", "Running shoes")))
        val bike = books.assets.save(Asset("", group, AssetKind.SPORTS, l("Vélo de route", "Road bike"), make = "Trek", model = "Domane AL 2", purchaseDate = LocalDate(today.year - 2, 5, 12), purchasePrice = cad("1349.99"), location = l("Rangement du sous-sol", "Basement storage")))
        books.assetMaintenance.addStarterTasks(bike.id, today) { ca.schippers.hfm.i18n.Messages.get(books.language, "assetTemplate.$it") }
        add(TransactionDraft(card.id, day(-30), cad("-89.00"), q("Vélo Ste-Foy", "Bike Shop Co."), listOf(SplitDraft(cat("personal.fitness"), cad("-89.00"))), memo = l("Mise au point", "Tune-up")))
        // Helping at the run club's charity race.
        books.volunteer.save(VolunteerEntry("", group, me.id, runningClub, VolunteerKind.OTHER, day(-48), 240, activity = l("Bénévole au ravitaillement", "Water station volunteer")))
        books.schedules.save(
            PersonSchedule("", group, me.id, ScheduleKind.WORK, employer, today.minus(DatePeriod(years = 1)), null, 1, true, null, (1..5).map { ScheduleShift(0, DayOfWeek(it), LocalTime(9, 0), LocalTime(17, 0)) }),
        )
    }

    private fun addHealthAndInsurance() {
        val physio = books.health.saveProvider(HealthProvider("", group, q("Physio Saint-Jean", "Active Life Physiotherapy"), ProviderKind.OTHER, p.phone("0170"), null, null, false))
        val plan = books.medical.savePlan(MedPlan("", group, MedPlanKind.GROUP_HEALTH, l("Assurance collective", "Group benefits"), q("Beneva", "Manulife"), "G-55120", "C-0991203", me.id, people = listOf(PlanPerson(me.id, 1))))
        books.medical.saveCoverage(MedCoverage("", plan.id, MedService.PHYSIOTHERAPY, BigDecimal("80"), annualMax = cad("500")))
        books.medical.saveCoverage(MedCoverage("", plan.id, MedService.DENTAL_PREVENTIVE, BigDecimal("80"), frequencyMonths = 9))
        val visit = books.medical.saveExpense(MedExpense("", group, me.id, MedService.PHYSIOTHERAPY, day(-21), cad("95.00"), physio.id, description = l("Genou du coureur", "Runner's knee")))
        books.medical.recordPayment(books.medical.submit(visit.id, plan.id, day(-20)).id, day(-14), cad("76.00"))
        books.medical.saveExpense(MedExpense("", group, me.id, MedService.PHYSIOTHERAPY, day(-7), cad("95.00"), physio.id, description = l("Genou du coureur, suivi", "Runner's knee, follow-up")))
        books.calendar.create(EventDraft(group, l("Physio (genou)", "Physio (knee)"), EventCategory.MEDICAL, day(7), LocalTime(17, 30), 45, memberId = me.id, providerId = physio.id))
        books.insurance.save(
            InsurancePolicy("", group, PolicyKind.TENANT, q("Desjardins Assurances", "Square One Insurance"), policyNumber = "T-2207718", insuredMemberId = me.id, premium = cad("19.50"), frequency = PremiumFrequency.MONTHLY,
                coverage = cad("30000"), deductible = cad("500"), renewalDate = day(95)),
        )
        add(TransactionDraft(card.id, inMonth(1, 7), cad("-19.50"), q("Desjardins Assurances", "Square One Insurance"), listOf(SplitDraft(cat("housing.insurance"), cad("-19.50")))))
    }

    /** The goals, a 50/30/20-style budget, and the rules that file the usual payees on their own. */
    private fun addGoalsBudgetsAndRules() {
        val goals = books.goals
        val emergency = goals.save(SavingsGoal("", savings.id, l("Fonds d’urgence (3 mois)", "Emergency fund (3 months)"), cad("6000"), LocalDate(today.year + 1, 6, 1), cad("250"), Recurrence.MONTHLY, start))
        goals.setAside(emergency.id, start, cad("1500"), l("Départ", "Starting amount"))
        goals.save(SavingsGoal("", savings.id, l("Voyage au Japon", "Trip to Japan"), cad("4000"), LocalDate(today.year + 1, 10, 1)))
        goals.save(SavingsGoal("", savings.id, l("Nouveau vélo", "New bike"), cad("1800"), LocalDate(today.year + 2, 4, 1)))
        goals.postScheduled(today)

        val firstMonth = LocalDate(start.year, start.month, 1)
        val budgets = books.budgets
        budgets.set(cat("housing.rent"), BudgetPeriod.MONTHLY, cad("1150.00"), startMonth = firstMonth)
        budgets.set(cat("food.groceries"), BudgetPeriod.MONTHLY, cad("300.00"), rollover = true, startMonth = firstMonth)
        budgets.set(cat("food.restaurants"), BudgetPeriod.MONTHLY, cad("120.00"), startMonth = firstMonth)
        budgets.set(cat("food.coffee_snacks"), BudgetPeriod.MONTHLY, cad("30.00"), startMonth = firstMonth)
        budgets.set(cat("personal.fitness"), BudgetPeriod.MONTHLY, cad("140.00"), rollover = true, startMonth = firstMonth)
        budgets.set(cat("personal.clothing"), BudgetPeriod.MONTHLY, cad("75.00"), rollover = true, startMonth = firstMonth)
        budgets.set(cat("transport"), BudgetPeriod.MONTHLY, cad("110.00"), startMonth = firstMonth)

        val rules = books.rules
        rules.create("Tim Hortons", cat("food.coffee_snacks"))
        rules.create("Spotify", cat("leisure.subscriptions"))
        rules.create(climbing, cat("personal.fitness"))
    }
}
