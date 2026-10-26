// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/DemoSeasonal.kt
// Modified On Timestamp: 2026-10-26 @ 00:10 EDT
// Created On Timestamp: 2026-10-06 @ 08:32 EDT
// File Description: The sample household's pool, yard and cottage with their seasonal tasks, and an
//   energy upgrade with the rebates of where it lives.
// Uses: DemoPlace, core/books assets, seasonal checklist and project rebates.
// Used By: DemoHousehold.
// Purpose: Show the seasonal checklist and energy rebates on realistic data.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.Asset
import ca.schippers.hfm.books.AssetKind
import ca.schippers.hfm.books.Books
import ca.schippers.hfm.books.EnergyUpgrade
import ca.schippers.hfm.books.HomeProject
import ca.schippers.hfm.books.ProjectRebate
import ca.schippers.hfm.books.ProjectStatus
import ca.schippers.hfm.books.RebateStatus
import ca.schippers.hfm.books.ValueMethod
import ca.schippers.hfm.calc.schedule.Seasons
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import java.math.BigDecimal

/**
 * SEA-01 to SEA-05 in the sample household: a pool and a yard at the house, the cottage the
 * mortgage is for, their usual tasks with some of this season's already ticked, and an energy
 * upgrade with the rebates of where the household lives ([p]); [skip] are tasks left as they are
 * (the furnace filter is overdue on purpose).
 */
internal fun addSeasonalDemo(books: Books, group: String, house: Asset, skip: Set<String>, today: LocalDate, p: DemoPlace) {
    val l = p::l
    val q = p::q
    fun cad(s: String) = Money.parse(s, Currency.CAD)
    fun day(n: Int) = today.plus(DatePeriod(days = n))
    fun task(key: String) = ca.schippers.hfm.i18n.Messages.get(books.language, "assetTemplate.$key")
    val assets = books.assets
    val upkeep = books.assetMaintenance
    val pool = assets.save(
        Asset(
            "", group, AssetKind.POOL, l("Piscine hors terre", "Above-ground pool"), house.id, "Trevi", "Prestige 24", purchaseDate = LocalDate(2021, 6, 5),
            seller = q("Club Piscine", p.t.poolStore), purchasePrice = cad("7900"), location = l("Cour arrière", "Backyard"),
        ),
    )
    val yard = assets.save(Asset("", group, AssetKind.YARD, l("Terrain et potager", "Yard and vegetable garden"), house.id, location = q("Rue des Érables", l("Rue Maple", "Maple Street"))))
    val cottage = assets.save(
        Asset(
            "", group, AssetKind.COTTAGE, p.cottage, purchaseDate = LocalDate(2019, 8, 15), purchasePrice = cad("265000"),
            valueMethod = ValueMethod.MANUAL, value = cad("340000"), valueDate = day(-200), inNetWorth = true, location = p.cottageTown,
        ),
    )
    for (a in listOf(pool, yard, cottage)) upkeep.addStarterTasks(a.id, today, ::task)

    // A few of this season's tasks already done, so the checklist shows its progress.
    val season = Seasons.windowOf(today)
    val list = books.seasonal.checklist(season, today)
    val doable = list.items.filter { it.taskId !in skip && it.dueDate?.let { d -> d <= today.plus(DatePeriod(days = 10)) } == true }
    doable.take(4).forEachIndexed { i, item ->
        val on = listOf(season.start, today.plus(DatePeriod(days = -(i + 1) * 3))).max().coerceAtMost(today)
        books.seasonal.tick(item, on, null, if (i == 0) BigDecimal("85.00") else null, null)
    }

    // SEA-05: attic insulation done last year with its rebates, and a heat pump water heater planned.
    val attic = books.homeProjects.save(
        HomeProject(
            "", group, l("Isolation de l’entretoit", "Attic insulation and air sealing"), ProjectStatus.DONE, Currency.CAD, house.id, day(-230), day(-222), cad("5000"),
            energyKind = EnergyUpgrade.INSULATION,
        ),
    )
    books.homeProjects.addCost(attic, day(-240), l("Évaluation énergétique", "Pre-retrofit energy evaluation"), cad("450.00"))
    books.homeProjects.addCost(attic, day(-222), l("Isolation R-60 et étanchéité", "R-60 blown-in insulation and air sealing"), cad("4380.00"))
    val project = books.homeProjects.list().first { it.id == attic.id }
    val rebates = books.projectRebates
    // Each rebate only where the place has one: Quebec's and Ontario's are the manual's.
    if (p.quebec || p.t.energyRebate != null) {
        rebates.save(
            project,
            ProjectRebate(
                "", project.id, q("Rénoclimat (Gouvernement du Québec)", p.t.energyRebate ?: ""), RebateStatus.RECEIVED, q("RC-2025-118274", p.t.energyRebateRef),
                cad("1850"), day(-215), day(-170), day(-150), cad("1850"),
            ),
        )
    }
    if (p.quebec || p.t.cityRebate != null) {
        rebates.save(project, ProjectRebate("", project.id, q(l("Remise municipale (Ville de Québec)", "Municipal rebate (Ville de Québec)"), p.t.cityRebate ?: ""), RebateStatus.APPLIED, amount = cad("500"), applied = day(-140)))
    }
    val water = books.homeProjects.save(
        HomeProject(
            "", group, l("Chauffe-eau thermopompe", "Heat pump water heater"), ProjectStatus.PLANNED, Currency.CAD, house.id, budget = cad("3800"),
            energyKind = EnergyUpgrade.WATER_HEATER,
        ),
    )
    if (p.quebec || p.t.waterHeaterRebate != null) {
        rebates.save(water, ProjectRebate("", water.id, q("LogisVert (Hydro-Québec)", p.t.waterHeaterRebate ?: ""), RebateStatus.PLANNED, amount = cad("1000")))
    }
}
