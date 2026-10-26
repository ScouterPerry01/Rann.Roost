// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/DemoFamily.kt
// Modified On Timestamp: 2026-10-25 @ 19:55 EDT
// Created On Timestamp: 2026-10-11 @ 12:46 EDT
// File Description: The family sample household: two working parents with good salaries, three
//   young children (daycare, grade 1, grade 2), a house with a mortgage, an inherited cottage,
//   large portfolios, savings goals, activities and volunteering.
// Uses: DemoKit, DemoPlace, core/books services, core/calc loan projection.
// Used By: SampleHousehold.FAMILY (DemoKit.kt).
// Purpose: Show the app filled in for a busy family, in any province or territory and language.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.Account
import ca.schippers.hfm.books.AccountDraft
import ca.schippers.hfm.books.Allergy
import ca.schippers.hfm.books.Allowance
import ca.schippers.hfm.books.AllowanceFrequency
import ca.schippers.hfm.books.AllowanceKind
import ca.schippers.hfm.books.AmountKind
import ca.schippers.hfm.books.AreaUnit
import ca.schippers.hfm.books.Asset
import ca.schippers.hfm.books.AssetKind
import ca.schippers.hfm.books.Beneficiary
import ca.schippers.hfm.books.BeneficiaryKind
import ca.schippers.hfm.books.BillDraft
import ca.schippers.hfm.books.BillKind
import ca.schippers.hfm.books.BillType
import ca.schippers.hfm.books.Books
import ca.schippers.hfm.books.BudgetPeriod
import ca.schippers.hfm.books.CardHolder
import ca.schippers.hfm.books.Chore
import ca.schippers.hfm.books.ContactRole
import ca.schippers.hfm.books.Contractor
import ca.schippers.hfm.books.ContractorJob
import ca.schippers.hfm.books.CreditCardTerms
import ca.schippers.hfm.books.DonationReceipt
import ca.schippers.hfm.books.Driver
import ca.schippers.hfm.books.EstateContact
import ca.schippers.hfm.books.EstatePlan
import ca.schippers.hfm.books.EventCategory
import ca.schippers.hfm.books.EventDraft
import ca.schippers.hfm.books.FuelEntry
import ca.schippers.hfm.books.GatherDecision
import ca.schippers.hfm.books.GrantKind
import ca.schippers.hfm.books.HealthProvider
import ca.schippers.hfm.books.HomeProject
import ca.schippers.hfm.books.Immunization
import ca.schippers.hfm.books.InsurancePolicy
import ca.schippers.hfm.books.Institution
import ca.schippers.hfm.books.InvestmentKind
import ca.schippers.hfm.books.InvestmentTxn
import ca.schippers.hfm.books.IncomeType
import ca.schippers.hfm.books.LengthUnit
import ca.schippers.hfm.books.LoanDetails
import ca.schippers.hfm.books.MedCoverage
import ca.schippers.hfm.books.MedExpense
import ca.schippers.hfm.books.MedPlan
import ca.schippers.hfm.books.MedPlanKind
import ca.schippers.hfm.books.MedService
import ca.schippers.hfm.books.Member
import ca.schippers.hfm.books.PaymentDraft
import ca.schippers.hfm.books.PaymentMethod
import ca.schippers.hfm.books.PersonSchedule
import ca.schippers.hfm.books.Pet
import ca.schippers.hfm.books.PlanPerson
import ca.schippers.hfm.books.PolicyBeneficiary
import ca.schippers.hfm.books.PolicyKind
import ca.schippers.hfm.books.PremiumFrequency
import ca.schippers.hfm.books.ProjectStatus
import ca.schippers.hfm.books.PropertyCustomField
import ca.schippers.hfm.books.PropertyOwner
import ca.schippers.hfm.books.PropertyRoom
import ca.schippers.hfm.books.PropertyStructure
import ca.schippers.hfm.books.PropertySystem
import ca.schippers.hfm.books.ProviderKind
import ca.schippers.hfm.books.RealProperty
import ca.schippers.hfm.books.ResidenceYear
import ca.schippers.hfm.books.RoomEntry
import ca.schippers.hfm.books.RoomPlan
import ca.schippers.hfm.books.SavingsGoal
import ca.schippers.hfm.books.ScheduleException
import ca.schippers.hfm.books.ScheduleKind
import ca.schippers.hfm.books.ScheduleShift
import ca.schippers.hfm.books.SearchService
import ca.schippers.hfm.books.Security
import ca.schippers.hfm.books.SecurityKind
import ca.schippers.hfm.books.Severity
import ca.schippers.hfm.books.Sex
import ca.schippers.hfm.books.Species
import ca.schippers.hfm.books.SplitDraft
import ca.schippers.hfm.books.TransactionDraft
import ca.schippers.hfm.books.TransferDraft
import ca.schippers.hfm.books.ValueMethod
import ca.schippers.hfm.books.Vehicle
import ca.schippers.hfm.books.VolunteerEntry
import ca.schippers.hfm.books.VolunteerKind
import ca.schippers.hfm.calc.Province
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
 * The family: [parentA] (engineering, $118,000) and [parentB] (software, $104,000), [oldest] in
 * grade 2 (hockey), [middle] in grade 1 (dance and swimming) and [youngest] at daycare. Six months
 * of activity, the house's mortgage paid monthly, the cottage inherited from parent A's
 * grandmother, and portfolios bought over six years.
 */
internal class DemoFamily(books: Books, p: DemoPlace) : DemoKit(books, p) {
    private val group = books.groups().first().id
    private val start = monthsAgo(6)
    private lateinit var parentA: Member
    private lateinit var parentB: Member
    private lateinit var oldest: Member
    private lateinit var middle: Member
    private lateinit var youngest: Member
    private lateinit var chequing: Account
    private lateinit var savings: Account
    private lateinit var card: Account

    private val bankName get() = q("Banque Nationale", "RBC Royal Bank")
    private val daycare get() = q(l("CPE Les Petits Explorateurs", "Les Petits Explorateurs daycare"), l("Garderie Little Explorers", "Little Explorers Child Care Centre"))
    private val school get() = q("École primaire des Bâtisseurs", l("École ${p.t.neighbourhood}", "${p.t.neighbourhood} Elementary School"))
    private val hockey get() = q(l("Hockey mineur ${p.city}", "${p.city} Minor Hockey"), l("Hockey mineur ${p.t.city}", "${p.t.city} Minor Hockey Association"))
    private val breakfastClub get() = l("Club des petits déjeuners", "Breakfast Club of Canada")
    private val employerA get() = q("Ingénierie Boréal inc.", "Northwind Engineering Ltd.")
    private val employerB get() = q("Logiciels Cèdre inc.", "Cedarline Software Inc.")

    /**
     * What a month of daycare costs here: Quebec's reduced contribution, Ontario's cap of $22 a
     * day, Alberta's and Saskatchewan's flat fees, about $10 a day elsewhere (sample figures, not
     * the programs' rules).
     */
    private val daycareMonthly: String get() = when (p.province) {
        Province.QC -> "196.35"
        Province.ON -> "462.00"
        Province.AB -> "326.25"
        Province.SK -> "217.50"
        else -> "210.00"
    }

    fun fill() {
        addPeople()
        addAccounts()
        val mortgage = addHouseAndMortgage()
        addMonthlyLife()
        addBills()
        payMortgage(mortgage)
        addInvestments()
        addPlans()
        addVehiclesAndPet()
        addSchedulesAndActivities()
        addHealth()
        addInsuranceAndEstate()
        addVolunteering()
        addChores()
        addGoalsAndBudgets()
        addContacts()
    }

    private fun addPeople() {
        parentA = books.members.create(l("Mathieu", "Michael"), MemberKind.ADULT, LocalDate(1990, 3, 18))
        parentB = books.members.create(l("Julie", "Priya"), MemberKind.ADULT, LocalDate(1991, 8, 7))
        oldest = books.members.create(l("Félix", "Ethan"), MemberKind.CHILD, LocalDate(schoolYear - 7, 4, 22))
        middle = books.members.create(l("Florence", "Lily"), MemberKind.CHILD, LocalDate(schoolYear - 6, 2, 11))
        youngest = books.members.create(l("Arthur", "Leo"), MemberKind.CHILD, LocalDate(schoolYear - 3, 6, 30))
    }

    private fun addAccounts() {
        val bank = books.institutions.create(Institution("", bankName, institutionNumber = q("006", "003"), transitNumber = q("21045", "04512")))
        val both = setOf(parentA.id, parentB.id)
        chequing = books.accounts.create(AccountDraft(group, l("Compte familial", "Family chequing"), AccountType.CHEQUING, Currency.CAD, cad("6800.00"), start, bank.id, q("006-21045-4410227", "003-04512-5120847"), both))
        savings = books.accounts.create(AccountDraft(group, l("Épargne familiale", "Family savings"), AccountType.HIGH_INTEREST_SAVINGS, Currency.CAD, cad("31500.00"), start, bank.id, ownerMemberIds = both))
        card = books.accounts.create(AccountDraft(group, q("Mastercard Banque Nationale", "RBC Avion Visa"), AccountType.CREDIT_CARD, Currency.CAD, cad("0"), start, bank.id, "4510998877665544", both))
        books.creditCards.saveTerms(
            card.id,
            CreditCardTerms(cad("20000"), BigDecimal("0.2099"), statementDay = 3, dueDay = 24, minPaymentPercent = BigDecimal("0.03"), minPaymentFloor = cad("10"), annualFee = cad("120"), annualFeeDate = day(75).minus(DatePeriod(years = 1))),
        )
        books.creditCards.saveHolder(CardHolder("", card.id, parentA.displayName, parentA.id, "5544", isPrimary = true))
        books.creditCards.saveHolder(CardHolder("", card.id, parentB.displayName, parentB.id, "2201"))
    }

    /** The house, bought five years ago, and its mortgage: renewed last year for five years. Returns the mortgage. */
    private fun addHouseAndMortgage(): Account {
        val bank = books.institutions.list().first()
        val house = books.assets.save(
            Asset(
                "", group, AssetKind.HOME, l("Maison familiale", "Family home"), purchaseDate = today.minus(DatePeriod(years = 5, months = 2)), purchasePrice = cad("685000"),
                valueMethod = ValueMethod.MANUAL, value = cad("742000"), valueDate = day(-90), location = p.city,
            ),
        )
        val firstPayment = LocalDate(start.year, start.month, 1).minus(DatePeriod(months = 16))
        val terms = LoanTerms(cad("498000"), BigDecimal("0.0439"), Compounding.SEMI_ANNUAL, 21 * 12, PaymentFrequency.MONTHLY)
        val owedAtStart = LoanProjection.project(LoanPlan(terms, firstPayment)).balanceOn(start.minus(DatePeriod(days = 1)))
        val mortgage = books.accounts.create(AccountDraft(group, l("Hypothèque de la maison", "House mortgage"), AccountType.MORTGAGE, Currency.CAD, -owedAtStart, firstPayment, bank.id, "MTG-5520418"))
        books.loans.save(
            LoanDetails(
                mortgage.id, terms.principal, terms.annualRate, amortizationMonths = terms.amortizationMonths, firstPaymentDate = firstPayment,
                termEnd = firstPayment.plus(DatePeriod(years = 5)), paymentAccountId = chequing.id, lastPaidDate = start.minus(DatePeriod(days = 1)),
            ),
        )
        // The cottage parent A inherited from a grandmother: no mortgage; its cost for tax is its value at her death.
        val inherited = today.minus(DatePeriod(years = 3, months = 4))
        val cottage = books.assets.save(
            Asset(
                "", group, AssetKind.COTTAGE, p.cottage, purchaseDate = inherited, purchasePrice = cad("365000"), valueMethod = ValueMethod.MANUAL, value = cad("418000"),
                valueDate = day(-200), location = p.cottageTown,
                notes = l(
                    "Héritage de grand-maman Rose (${inherited.year}). Coût fiscal : la juste valeur marchande au décès, 365 000 $.",
                    "Inherited from Grandma Rose (${inherited.year}). Tax cost: fair market value at her death, $365,000.",
                ),
            ),
        )
        books.assets.save(Asset("", group, AssetKind.SPORTS, l("Canot et kayaks", "Canoe and kayaks"), purchaseDate = day(-500), purchasePrice = cad("2400"), location = p.cottage))
        upkeepFor(house)
        addPropertyDetails(house, cottage, mortgage)
        return mortgage
    }

    /**
     * AST-06 to AST-17: everything about the house and the cottage, so each tab of Property details has
     * something in it. Values are stored metric; roll and parcel numbers follow the province's own form.
     */
    private fun addPropertyDetails(house: Asset, cottage: Asset, mortgage: Account) {
        fun cents(s: String) = cad(s).minorUnits.toString()
        // Sizes as a Canadian listing gives them, in feet and square feet; stored exactly in metres.
        fun ft(s: String) = LengthUnit.FEET.toMetres(BigDecimal(s))
        fun sqft(s: String) = AreaUnit.SQUARE_FEET.toSquareMetres(BigDecimal(s))
        fun feet(s: String) = ft(s).toPlainString()
        fun squareFeet(s: String) = sqft(s).toPlainString()
        val province = p.province.name
        // The first letter of a postal code is the province's (Quebec's are G, H and J).
        val letter = when (p.province) {
            Province.NL -> "A"; Province.NS -> "B"; Province.PE -> "C"; Province.NB -> "E"; Province.QC -> "G"; Province.ON -> "K"
            Province.MB -> "R"; Province.SK -> "S"; Province.AB -> "T"; Province.BC -> "V"; Province.YT -> "Y"; else -> "X"
        }
        val (houseRoll, houseParcel, cottageRoll, cottageParcel) = when (p.province) {
            Province.ON -> listOf("0614 040 120 05300 0000", "04123-0456 (LT)", "4651 000 015 12400 0000", "53210-0118 (LT)")
            Province.QC -> listOf("1234-56-7890-1-000-0000", "1 234 567", "9821-44-1065-0-000-0000", "5 812 940")
            Province.BC -> listOf("200-04531-127", "012-345-678", "765-01842-033", "027-114-902")
            else -> listOf("R-104582-01", "123456789", "R-310877-04", "987654321")
        }
        val authority = when (p.province) {
            Province.ON -> "MPAC"
            Province.BC -> "BC Assessment"
            else -> p.municipality
        }
        val purchased = house.purchaseDate!!.year
        val a = parentA.id
        val b = parentB.id

        books.realProperty.save(
            RealProperty(
                house.id,
                fields = buildMap {
                    // Identification and address
                    put("property_type", "DETACHED"); put("building_style", "STOREY_2"); put("use", "PRINCIPAL_RESIDENCE")
                    put("occupancy", "OWNER"); put("acquired_how", "PURCHASE")
                    put("street_number", "48"); put("street_name", q("rue des Érables", "Maple Crescent")); put("municipality", p.city)
                    put("province", province); put("postal_code", "${letter}2B 4N7"); put("country", "CA")
                    put("latitude", q("46.80330", p.t.home.lat.toString())); put("longitude", q("-71.24280", p.t.home.lon.toString()))
                    // Legal
                    put("roll_number", houseRoll); put("parcel_id", houseParcel)
                    if (p.quebec) {
                        put("legal_description", "Lot 1 234 567 du cadastre du Québec, circonscription foncière de Québec.")
                    } else {
                        put("lot", "27"); put("plan", "4M-1182")
                        put("legal_description", l("Lot 27, plan 4M-1182, ${p.municipality}.", "Lot 27, Plan 4M-1182, ${p.municipality}."))
                    }
                    put("tenure", "FREEHOLD"); put("survey_date", "2004-06-14"); put("surveyor", q("Arpenteurs Rioux inc.", "Annis, O’Sullivan, Vollebekk Ltd."))
                    put("easements", l("Servitude d’Hydro de 3 m le long de la limite arrière.", "3 m hydro easement along the rear lot line."))
                    // Land: a 50 by 110 ft lot
                    put("lot_area", squareFeet("5500")); put("lot_frontage", feet("50")); put("lot_depth", feet("110")); put("lot_shape", "RECTANGULAR")
                    put("topography", "FLAT"); put("soil_type", "CLAY"); put("drainage", "GOOD"); put("vegetation", "LANDSCAPED"); put("waterfront", "NONE")
                    put("access", "MUNICIPAL_ROAD"); put("fencing", "FULL"); put("zoning", q("H-1", "R1"))
                    // Building: 2,150 sq ft above grade, 850 below
                    put("year_built", "2004"); put("builder", q("Construction Laval", "Minto")); put("storeys", "2"); put("floors_total", "3")
                    put("finished_area_above_grade", squareFeet("2150")); put("finished_area_below_grade", squareFeet("850"))
                    put("total_floor_area", squareFeet("3000")); put("footprint_area", squareFeet("1160"))
                    put("ceiling_height_main", feet("9")); put("bedrooms", "4"); put("bathrooms_full", "2"); put("bathrooms_half", "1"); put("kitchens", "1")
                    put("construction_type", "WOOD_FRAME"); put("foundation", "POURED_CONCRETE"); put("exterior_finish", "BRICK,VINYL")
                    put("roof_shape", "HIP"); put("roof_material", "ASPHALT_SHINGLE"); put("roof_year", "2019"); put("window_glazing", "DOUBLE")
                    put("window_frames", "VINYL"); put("windows_year", "2004"); put("insulation_attic_r", "50"); put("insulation_walls_r", "20")
                    put("energuide_rating", "78"); put("energuide_scale", "SCORE_0_100"); put("energy_audit_date", "2023-03-08"); put("interior_walls", "DRYWALL")
                    put("flooring_main", "HARDWOOD,TILE"); put("fireplaces", "1"); put("hazardous_materials", "RADON_TESTED")
                    put("radon_test_date", "2022-02-15"); put("radon_level_bq", "85")
                    put("major_renovations", l("2019 : toiture refaite. 2023 : isolation de l’entretoit portée à R-50.", "2019: new roof. 2023: attic insulation raised to R-50."))
                    // Basement, parking, features
                    put("basement_type", "FULL"); put("basement_finish", "FINISHED"); put("basement_height", feet("8")); put("waterproofing", "EXTERIOR")
                    put("sump_pump", "BATTERY_BACKUP"); put("backwater_valve", "1")
                    put("garage_type", "ATTACHED"); put("garage_spaces", "2"); put("garage_insulated", "1"); put("garage_door_opener", "1")
                    put("driveway", "ASPHALT"); put("parking_spaces", "2")
                    put("pool_type", "NONE"); put("deck_material", "COMPOSITE"); put("deck_area", squareFeet("300")); put("patio", "1"); put("porch", "1")
                    put("play_structure", "1"); put("central_vacuum", "1")
                    // Value and tax: Ontario still assesses at January 1, 2016 values.
                    put("assessed_value", cents(if (p.province == Province.ON) "591000" else "655000"))
                    put("assessment_year", if (p.province == Province.ON) "2016" else (today.year - 1).toString())
                    put("assessing_authority", authority); put("tax_account_number", "TX-0482-27"); put("annual_property_tax", cents("6850"))
                    if (p.province == Province.ON) put("school_support", "ENGLISH_PUBLIC")
                    put("insured_replacement_cost", cents("545000")); put("market_value_estimate", cents("742000")); put("estimate_date", day(-90).toString())
                    put("estimate_source", l("Ventes comparables de l’agente", "Agent’s comparable sales"))
                    put("mortgage_account", mortgage.id)
                },
                rooms = listOf(
                    PropertyRoom(type = "FOYER", level = "MAIN", length = ft("8"), width = ft("7"), flooring = "TILE"),
                    PropertyRoom(type = "LIVING", level = "MAIN", length = ft("17"), width = ft("14"), flooring = "HARDWOOD", features = l("Foyer au gaz", "Gas fireplace"), condition = "GOOD"),
                    PropertyRoom(type = "DINING", level = "MAIN", length = ft("12"), width = ft("11"), flooring = "HARDWOOD", condition = "GOOD"),
                    PropertyRoom(type = "KITCHEN", level = "MAIN", length = ft("15"), width = ft("13"), flooring = "TILE", features = l("Îlot, comptoirs de quartz (2021)", "Island, quartz counters (2021)"), condition = "EXCELLENT"),
                    PropertyRoom(type = "POWDER_ROOM", level = "MAIN", length = ft("6"), width = ft("5"), flooring = "TILE"),
                    PropertyRoom(type = "MUDROOM", level = "MAIN", length = ft("9"), width = ft("6"), flooring = "TILE"),
                    PropertyRoom(type = "PRIMARY_BEDROOM", level = "SECOND", length = ft("16"), width = ft("13"), flooring = "HARDWOOD", features = l("Garde-robe de plain-pied", "Walk-in closet"), condition = "GOOD"),
                    PropertyRoom(type = "ENSUITE", level = "SECOND", length = ft("10"), width = ft("9"), flooring = "TILE", features = l("Douche vitrée, deux lavabos", "Glass shower, double sink")),
                    PropertyRoom(type = "BEDROOM", name = oldest.displayName, level = "SECOND", length = ft("12"), width = ft("10.5"), flooring = "HARDWOOD"),
                    PropertyRoom(type = "BEDROOM", name = middle.displayName, level = "SECOND", length = ft("11"), width = ft("10"), flooring = "HARDWOOD"),
                    PropertyRoom(type = "NURSERY", name = youngest.displayName, level = "SECOND", length = ft("10"), width = ft("9.5"), flooring = "CARPET"),
                    PropertyRoom(type = "BATHROOM", level = "SECOND", length = ft("9"), width = ft("5.5"), flooring = "TILE"),
                    PropertyRoom(type = "LAUNDRY", level = "SECOND", length = ft("7"), width = ft("6"), flooring = "TILE"),
                    PropertyRoom(type = "RECREATION", level = "BASEMENT", length = ft("24"), width = ft("18"), flooring = "VINYL", condition = "GOOD"),
                    PropertyRoom(type = "DEN_OFFICE", level = "BASEMENT", length = ft("11"), width = ft("10"), flooring = "VINYL", features = l("Bureau de télétravail", "Work-from-home office")),
                    PropertyRoom(type = "UTILITY", level = "BASEMENT", length = ft("14"), width = ft("10"), flooring = "CONCRETE"),
                ),
                systems = listOf(
                    if (p.quebec) {
                        PropertySystem(kind = "HEATING", type = "HEAT_PUMP_AIR", fuel = "ELECTRICITY", makeModel = "Mitsubishi Zuba", capacity = "36 000 BTU/h", installYear = 2021, lastInspection = day(-150), ownership = "OWNED")
                    } else {
                        PropertySystem(kind = "HEATING", type = "FORCED_AIR", fuel = "NATURAL_GAS", makeModel = "Lennox EL296V", capacity = "80,000 BTU/h", installYear = 2016, lastInspection = day(-150), ownership = "OWNED")
                    },
                    PropertySystem(kind = "COOLING", type = "CENTRAL_AIR", fuel = "ELECTRICITY", makeModel = "Lennox ML14XC1", capacity = l("3 tonnes", "3 ton"), installYear = 2016, ownership = "OWNED"),
                    PropertySystem(
                        kind = "WATER_HEATING", type = "TANK", fuel = q("ELECTRICITY", "NATURAL_GAS"), capacity = q("227 L", "40 US gal"), installYear = 2018, ownership = "RENTED",
                        rentalProvider = q("HydroSolution", "Enercare"), rentalMonthly = cad("32.99"),
                    ),
                    PropertySystem(kind = "VENTILATION", type = "HRV", makeModel = "Venmar AVS", installYear = 2004, notes = l("Filtres changés chaque trimestre", "Filters changed every quarter")),
                    PropertySystem(kind = "ELECTRICAL", type = "BREAKERS", capacity = "200 A", installYear = 2004, notes = l("Câblage en cuivre", "Copper wiring")),
                    PropertySystem(kind = "PLUMBING", type = "PEX", installYear = 2004),
                    PropertySystem(kind = "WATER_SUPPLY", type = "MUNICIPAL"),
                    PropertySystem(kind = "SEWAGE", type = "MUNICIPAL"),
                    PropertySystem(kind = "FIREPLACE", type = "GAS", fuel = q("PROPANE", "NATURAL_GAS"), makeModel = "Napoleon B36", installYear = 2004),
                    PropertySystem(kind = "EV_CHARGER", makeModel = "Grizzl-E Classic", capacity = "40 A", installYear = 2023, ownership = "OWNED"),
                    PropertySystem(kind = "SECURITY", type = "MONITORED_ALARM", rentalProvider = q("Alarme Sentinelle", "Alarm Force"), rentalMonthly = cad("29.95"), ownership = "RENTED"),
                    PropertySystem(kind = "FIRE_PROTECTION", type = "SMOKE_ALARMS", notes = l("Avertisseurs de fumée et de CO à chaque étage", "Smoke and CO alarms on every floor")),
                ),
                structures = listOf(
                    PropertyStructure(type = "SHED", name = l("Remise du jardin", "Garden shed"), area = sqft("100"), yearBuilt = 2020, construction = l("Bois sur blocs", "Wood on blocks"), electricity = true),
                ),
                owners = listOf(PropertyOwner(a, 5000, "JOINT_TENANCY"), PropertyOwner(b, 5000, "JOINT_TENANCY")),
                residenceYears = (purchased..today.year).map { ResidenceYear(a, it) },
                custom = listOf(
                    PropertyCustomField(label = l("Couleurs de peinture", "Paint colours"), value = l("Salon : Benjamin Moore Pale Oak OC-20. Cuisine : Chantilly Lace OC-65.", "Living room: Benjamin Moore Pale Oak OC-20. Kitchen: Chantilly Lace OC-65.")),
                    PropertyCustomField(label = l("Robinet d’arrêt principal", "Main water shut-off"), value = l("Sous-sol, salle mécanique, derrière le chauffe-eau", "Basement utility room, behind the water heater")),
                ),
            ),
        )

        books.realProperty.save(
            RealProperty(
                cottage.id,
                fields = buildMap {
                    put("property_type", "COTTAGE_CABIN"); put("building_style", "CHALET"); put("use", "SECONDARY_RESIDENCE")
                    put("occupancy", "OWNER"); put("acquired_how", "INHERITANCE")
                    put("street_number", "1187"); put("street_name", q("chemin du Lac-Édouard", "Lakeshore Road")); put("municipality", p.cottageTown)
                    put("province", province); put("postal_code", "${letter}0K 1R0"); put("country", "CA"); put("emergency_civic_number", "1187")
                    put(
                        "directions",
                        l(
                            "Prendre le chemin privé à gauche après le dépanneur; troisième entrée après le pont. Panneau bleu « Rose ».",
                            "Take the private road on the left past the general store; third driveway after the bridge. Blue \"Rose\" sign.",
                        ),
                    )
                    put("roll_number", cottageRoll); put("parcel_id", cottageParcel); put("tenure", "FREEHOLD")
                    if (!p.quebec) { put("lot", "14"); put("concession", "3"); put("geographic_township", l("Canton de ${p.cottageTown}", "Township of ${p.cottageTown}")) }
                    put("easements", l("Droit de passage partagé sur le chemin privé.", "Shared right-of-way over the private road."))
                    // Land: about an acre, 150 ft on the lake
                    put("lot_area", squareFeet("43560")); put("lot_frontage", feet("150")); put("lot_depth", feet("290")); put("lot_shape", "IRREGULAR")
                    put("topography", "GENTLE_SLOPE"); put("soil_type", "SAND"); put("drainage", "GOOD"); put("vegetation", "PARTLY_WOODED")
                    put("waterfront", "LAKE"); put("water_frontage", feet("150")); put("shoreline", "MIXED"); put("water_depth_at_dock", feet("6"))
                    put("view", l("Ouest sur le lac, couchers de soleil", "West over the lake, sunsets"))
                    put("access", "PRIVATE_ROAD"); put("road_maintained_by", l("Association du chemin (350 $ par année)", "Road association ($350 a year)"))
                    put("fencing", "NONE"); put("zoning", q("Villégiature", "Shoreline Residential (SR)"))
                    // Building: about 1,100 sq ft
                    put("year_built", "1978"); put("year_built_estimated", "1"); put("storeys", "1.5"); put("floors_total", "2"); put("finished_area_above_grade", squareFeet("1100"))
                    put("bedrooms", "2"); put("bathrooms_full", "1"); put("kitchens", "1"); put("construction_type", "WOOD_FRAME"); put("foundation", "PIERS")
                    put("exterior_finish", "WOOD"); put("roof_shape", "GABLE"); put("roof_material", "METAL"); put("roof_year", "2016"); put("window_glazing", "DOUBLE")
                    put("window_frames", "WOOD"); put("interior_walls", "PANELLING"); put("flooring_main", "HARDWOOD"); put("fireplaces", "0")
                    put("major_renovations", l("2016 : toit de tôle. 2021 : nouvelle installation septique.", "2016: metal roof. 2021: new septic system."))
                    put("basement_type", "CRAWL_SPACE"); put("garage_type", "NONE"); put("driveway", "GRAVEL"); put("parking_spaces", "3")
                    put("deck_material", "CEDAR"); put("deck_area", squareFeet("400")); put("porch", "1"); put("fire_pit", "1")
                    put("assessed_value", cents(if (p.province == Province.ON) "289000" else "341000"))
                    put("assessment_year", if (p.province == Province.ON) "2016" else (today.year - 1).toString())
                    put("assessing_authority", if (p.province == Province.ON) "MPAC" else q("Municipalité de ${p.cottageTown}", "${p.t.cottageTown}"))
                    put("annual_property_tax", cents("3480")); put("insured_replacement_cost", cents("310000"))
                    put("market_value_estimate", cents("418000")); put("estimate_date", day(-200).toString())
                    put("estimate_source", l("Évaluation à la succession", "Estate appraisal"))
                },
                rooms = listOf(
                    PropertyRoom(type = "GREAT_ROOM", level = "MAIN", length = ft("22"), width = ft("17"), flooring = "HARDWOOD", features = l("Poêle à bois, plafond cathédrale", "Wood stove, cathedral ceiling"), condition = "GOOD"),
                    PropertyRoom(type = "KITCHEN", level = "MAIN", length = ft("12"), width = ft("10"), flooring = "VINYL", condition = "FAIR"),
                    PropertyRoom(type = "PRIMARY_BEDROOM", level = "MAIN", length = ft("12"), width = ft("11"), flooring = "HARDWOOD"),
                    PropertyRoom(type = "BEDROOM", level = "MAIN", length = ft("10"), width = ft("9"), flooring = "HARDWOOD", features = l("Lits superposés", "Bunk beds")),
                    PropertyRoom(type = "BATHROOM", level = "MAIN", length = ft("8"), width = ft("5"), flooring = "VINYL"),
                    PropertyRoom(type = "SCREENED_PORCH", level = "MAIN", length = ft("16"), width = ft("8"), flooring = "OTHER", features = l("Côté lac", "Lake side")),
                    PropertyRoom(type = "LOFT", level = "LOFT", length = ft("16"), width = ft("12"), flooring = "HARDWOOD", features = l("Deux lits doubles", "Two double beds")),
                ),
                systems = listOf(
                    PropertySystem(kind = "HEATING", type = "WOOD_STOVE", fuel = "WOOD", makeModel = "Jøtul F 500", installYear = 2012, lastInspection = day(-400), ownership = "OWNED", notes = l("Certifié WETT; ramonage chaque automne", "WETT certified; chimney swept every fall")),
                    PropertySystem(kind = "HEATING", type = "ELECTRIC_BASEBOARD", fuel = "ELECTRICITY", installYear = 1978),
                    PropertySystem(kind = "WATER_HEATING", type = "TANK", fuel = "ELECTRICITY", capacity = q("151 L", "40 US gal"), installYear = 2017, ownership = "OWNED"),
                    PropertySystem(kind = "ELECTRICAL", type = "BREAKERS", capacity = "100 A", installYear = 1995),
                    PropertySystem(kind = "WATER_SUPPLY", type = "LAKE_INTAKE", notes = l("Pompe retirée et purgée à la fermeture", "Pump pulled and drained at closing")),
                    PropertySystem(kind = "WATER_TREATMENT", type = "UV", makeModel = "Viqua VH410", installYear = 2019, notes = l("Lampe changée chaque printemps", "Lamp replaced every spring")),
                    PropertySystem(kind = "SEWAGE", type = "SEPTIC", capacity = q("3 400 L", "3,600 L"), installYear = 2021, lastInspection = day(-120), notes = l("Vidange aux deux ans", "Pumped every two years")),
                ),
                structures = listOf(
                    PropertyStructure(type = "BUNKIE", name = l("Camp des enfants", "Kids’ bunkie"), area = sqft("120"), yearBuilt = 1992, construction = l("Bois", "Wood frame"), electricity = true),
                    PropertyStructure(type = "DOCK_FLOATING", name = l("Quai", "Dock"), area = sqft("240"), yearBuilt = 2018, construction = l("Cèdre sur flotteurs", "Cedar on floats"), notes = l("Retiré à l’automne", "Pulled out in the fall")),
                    PropertyStructure(type = "SAUNA", area = sqft("60"), yearBuilt = 2005, construction = l("Cèdre", "Cedar"), heated = true, electricity = true),
                    PropertyStructure(type = "SHED", name = l("Remise à bois", "Woodshed"), area = sqft("80"), yearBuilt = 1985),
                ),
                owners = listOf(PropertyOwner(a, 10000, "SOLE")),
                custom = listOf(
                    PropertyCustomField(label = l("Gardien des clés", "Key holder"), value = l("Voisins au 1191 (Lise et Paul), ${q("819", p.area)}-555-0147", "Neighbours at 1191 (Lise and Paul), ${q("819", p.area)}-555-0147")),
                    PropertyCustomField(label = l("Ouverture et fermeture", "Opening and closing"), value = l("Ouverture à la mi-mai, fermeture à l’Action de grâce.", "Open mid-May, close at Thanksgiving.")),
                ),
            ),
        )
    }

    private fun upkeepFor(house: Asset) {
        fun task(key: String) = ca.schippers.hfm.i18n.Messages.get(books.language, "assetTemplate.$key")
        books.assetMaintenance.addStarterTasks(house.id, today, ::task)
        books.assets.list().firstOrNull { it.kind == AssetKind.COTTAGE }?.let { books.assetMaintenance.addStarterTasks(it.id, today, ::task) }
    }

    /** Six months of pay, child benefit, groceries, daycare, after-school care, restaurants, outings and gifts. */
    private fun addMonthlyLife() {
        val stubA = payStub(employerA, BigDecimal("4916.67"), group = cad("38.50"), pension = cad("245.83"))
        val stubB = payStub(employerB, BigDecimal("4333.33"), group = cad("31.20"), rrsp = cad("216.67"))
        val ccb = childBenefitPerMonth(today.year - 1, BigDecimal("118000"), BigDecimal("104000"), 3)
        val grocer = q("IGA", p.t.grocer)
        val restaurant = q("Restaurant Le Cartier", "The Keg")
        for (back in 6 downTo 0) {
            fun on(d: Int) = inMonth(back, d)
            for (d in listOf(1, 15)) {
                add(books.payStubs.draft(chequing.id, stubA.copy(payDate = on(d)), parentA.id))
                add(books.payStubs.draft(chequing.id, stubB.copy(payDate = on(d)), parentB.id))
            }
            if (!ccb.isZero) add(TransactionDraft(chequing.id, on(20), ccb, l("Allocation canadienne pour enfants", "Canada Child Benefit"), listOf(SplitDraft(cat("income.benefits.ccb"), ccb))))
            for ((d, amount) in listOf(3 to "318.42", 10 to "276.15", 17 to "341.88", 24 to "295.60")) {
                add(TransactionDraft(card.id, on(d), cad("-$amount"), grocer, listOf(SplitDraft(cat("food.groceries"), cad("-$amount")))))
            }
            add(TransactionDraft(card.id, on(12), cad("-412.37"), "Costco", listOf(SplitDraft(cat("food.groceries"), cad("-288.50")), SplitDraft(cat("children.clothing"), cad("-74.99")), SplitDraft(cat("health.otc"), cad("-48.88")))))
            add(TransactionDraft(chequing.id, on(1), cad("-$daycareMonthly"), daycare, listOf(SplitDraft(cat("children.childcare"), cad("-$daycareMonthly"))), memberId = youngest.id))
            add(TransactionDraft(chequing.id, on(1), cad("-385.00"), l("Service de garde - $school", "$school before and after school"), listOf(SplitDraft(cat("children.childcare"), cad("-385.00")))))
            add(TransactionDraft(card.id, on(8), cad("-96.40"), restaurant, listOf(SplitDraft(cat("food.restaurants"), cad("-96.40")))))
            add(TransactionDraft(card.id, on(22), cad("-54.85"), q("Benny & Co.", "Boston Pizza"), listOf(SplitDraft(cat("food.restaurants"), cad("-54.85")))))
            add(TransactionDraft(card.id, on(14), cad("-38.97"), q("Cinéplex", "Cineplex"), listOf(SplitDraft(cat("leisure.entertainment"), cad("-38.97")))))
            add(TransactionDraft(card.id, on(19), cad("-45.00"), l("Cadeau de fête d’un ami", "Friend's birthday present"), listOf(SplitDraft(cat("gifts.gifts"), cad("-45.00"))), memberId = middle.id))
            // Monthly gifts to the food bank and the Breakfast Club, on one receipt a year.
            add(TransactionDraft(chequing.id, on(5), cad("-50.00"), breakfastClub, listOf(SplitDraft(cat("gifts.charity"), cad("-50.00"))), memberId = parentB.id))
            add(TransactionDraft(chequing.id, on(5), cad("-40.00"), q("Moisson Québec", p.t.foodBank), listOf(SplitDraft(cat("gifts.charity"), cad("-40.00"))), memberId = parentA.id))
            add(TransferDraft(chequing.id, card.id, on(24), cad("2150.00")))
            add(TransferDraft(chequing.id, savings.id, on(16), cad("1200.00"), memo = l("Épargne du mois", "Monthly savings")))
        }
        // A fundraising gala: the receipt counts only the part above the dinner's value.
        if (LocalDate(today.year, 4, 26) <= today) {
            val gala = books.transactions.create(
                TransactionDraft(card.id, LocalDate(today.year, 4, 26), cad("-300.00"), q("Fondation du CHU de Québec", p.t.foundation), listOf(SplitDraft(cat("gifts.charity"), cad("-300.00"))), memberId = parentA.id),
            )
            books.donations.setReceipt(gala.id, DonationReceipt(q("Fondation du CHU de Québec", p.t.foundation), null, "G-${today.year}-2210", cad("180.00"), received = true))
        }
    }

    private fun addBills() {
        fun next(dayOfMonth: Int): LocalDate = LocalDate(today.year, today.month, dayOfMonth).let { if (it > today) it else it.plus(DatePeriod(months = 1)) }
        val bills = books.bills
        val home = BillType.HOME
        bills.create(
            BillDraft(BillKind.BILL, p.power, cad("164.20"), chequing.id, Recurrence.MONTHLY, next(14), p.power, "7 2210 4418 3", AmountKind.VARIABLE, paymentMethod = PaymentMethod.PAD,
                categoryId = cat("utilities.electricity"), type = home, categoryKey = "home.essential", subcategoryKey = "home.essential.electricity"),
        )
        (if (p.quebec) "Énergir" else p.t.gas)?.let { gas ->
            bills.create(
                BillDraft(BillKind.BILL, gas, cad("118.60"), chequing.id, Recurrence.MONTHLY, next(9), gas, "9120 5531 0087", AmountKind.VARIABLE, paymentMethod = PaymentMethod.PAD,
                    categoryId = cat("utilities.heating"), type = home, categoryKey = "home.essential", subcategoryKey = "home.essential.natural_gas"),
            )
        }
        val internet = q("Vidéotron", p.t.internet)
        bills.create(
            BillDraft(BillKind.BILL, l("Internet et télé", "Internet and TV"), cad("128.00"), chequing.id, Recurrence.MONTHLY, next(18), internet, "8 3301 2210 9984",
                paymentMethod = PaymentMethod.PAD, categoryId = cat("utilities.internet"), type = home, categoryKey = "home.essential", subcategoryKey = "home.essential.home_internet"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Cellulaires (2)", "Cell phones (2)"), cad("118.00"), card.id, Recurrence.MONTHLY, next(21), q("Fizz", "Koodo"), "4410 2291 87",
                AmountKind.VARIABLE, paymentMethod = PaymentMethod.CARD, categoryId = cat("utilities.mobile"), type = home, categoryKey = "home.essential", subcategoryKey = "home.essential.cell_phone"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Taxes municipales", "Property taxes"), cad("6120.00"), chequing.id, Recurrence.SEMI_ANNUAL, LocalDate(today.year, 3, 1).let { if (it > today) it else LocalDate(today.year, 9, 1).let { s -> if (s > today) s else LocalDate(today.year + 1, 3, 1) } },
                p.municipality, q("Matricule 4471-2203-1180", p.t.roll), paymentMethod = PaymentMethod.ONLINE, categoryId = cat("housing.municipal_tax"),
                type = home, categoryKey = "home.essential", subcategoryKey = "home.essential.property_taxes"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Taxes du chalet", "Cottage property taxes"), cad("3480.00"), chequing.id, Recurrence.ANNUAL, LocalDate(today.year + 1, 6, 30),
                q("Municipalité de ${p.cottageTown}", "${p.t.cottageTown} property taxes"), paymentMethod = PaymentMethod.ONLINE, categoryId = cat("leisure.cottage_rv"),
                type = home, categoryKey = "home.essential", subcategoryKey = "home.essential.property_taxes"),
        )
        bills.create(
            BillDraft(BillKind.BILL, l("Diffusion en continu", "Streaming"), cad("26.99"), card.id, Recurrence.MONTHLY, day(6), "StreamCo", paymentMethod = PaymentMethod.CARD,
                categoryId = cat("utilities.tv_streaming"), isSubscription = true, type = home, categoryKey = "home.lifestyle", subcategoryKey = "home.lifestyle.streaming"),
        )
        bills.create(BillDraft(BillKind.BILL, l("Garderie", "Daycare"), cad(daycareMonthly), chequing.id, Recurrence.MONTHLY, next(1), daycare, categoryId = cat("children.childcare"), paymentMethod = PaymentMethod.PAD))
        bills.create(BillDraft(BillKind.TRANSFER, l("Épargne du mois", "Monthly savings"), cad("1200.00"), chequing.id, Recurrence.MONTHLY, next(16), transferAccountId = savings.id))
    }

    private fun payMortgage(mortgage: Account) {
        while (true) {
            val payment = books.loans.nextPayment(mortgage.id, today)?.takeIf { it.date <= today } ?: break
            books.loans.recordPayment(mortgage.id, chequing.id, payment)
        }
    }

    /**
     * Each parent's RRSP and TFSA and their joint account, bought over six years in index ETFs and
     * a few Canadian stocks, with dividends and current prices: about $350,000 each.
     */
    private fun addInvestments() {
        val inv = books.investments
        val broker = books.institutions.create(Institution("", q(l("Courtage direct Banque Nationale", "National Bank Direct Brokerage"), "RBC Direct Investing"), institutionNumber = q("006", "003")))
        val opened = today.minus(DatePeriod(years = 6))
        fun account(name: String, type: AccountType, owners: Set<String>, number: String, cash: String) =
            books.accounts.create(AccountDraft(group, name, type, Currency.CAD, cad(cash), opened, broker.id, number, owners))
        fun n(s: String) = BigDecimal(s)
        fun security(symbol: String, name: String, kind: SecurityKind, assetClass: AssetClass = AssetClass.EQUITY, region: Region = Region.CANADA) =
            inv.saveSecurity(Security("", symbol, "TSX", name, kind, Currency.CAD, assetClass, region))
        val xeqt = security("XEQT", "iShares Core Equity ETF Portfolio", SecurityKind.ETF, region = Region.GLOBAL)
        val vfv = security("VFV", "Vanguard S&P 500 Index ETF", SecurityKind.ETF, region = Region.US)
        val xic = security("XIC", "iShares Core S&P/TSX Capped Composite", SecurityKind.ETF)
        val zag = security("ZAG", "BMO Aggregate Bond Index ETF", SecurityKind.ETF, AssetClass.FIXED_INCOME)
        val xef = security("XEF", "iShares Core MSCI EAFE IMI", SecurityKind.ETF, region = Region.INTERNATIONAL)
        val td = security("TD", "Toronto-Dominion Bank", SecurityKind.STOCK)
        val enb = security("ENB", "Enbridge Inc.", SecurityKind.STOCK)
        val cnr = security("CNR", "Canadian National Railway", SecurityKind.STOCK)
        fun buy(a: Account, s: Security, monthsBack: Int, qty: String, price: String, fees: String = "0") = inv.save(
            InvestmentTxn("", a.id, monthsAgo(monthsBack), InvestmentKind.BUY, s.id, n(qty), n(price), cad(n(qty) * n(price)), cad(fees)),
        )
        fun income(a: Account, s: Security, monthsBack: Int, amount: String, type: IncomeType = IncomeType.DIVIDEND) =
            inv.save(InvestmentTxn("", a.id, monthsAgo(monthsBack), InvestmentKind.INCOME, s.id, amount = cad(amount), incomeType = type))

        val a = parentA.displayName
        val b = parentB.displayName
        val rrspA = account(l("REER $a", "RRSP $a"), AccountType.RRSP, setOf(parentA.id), "R-77120", "112000")
        val tfsaA = account(l("CELI $a", "TFSA $a"), AccountType.TFSA, setOf(parentA.id), "T-77121", "64000")
        val rrspB = account(l("REER $b", "RRSP $b"), AccountType.RRSP, setOf(parentB.id), "R-88340", "98000")
        val tfsaB = account(l("CELI $b", "TFSA $b"), AccountType.TFSA, setOf(parentB.id), "T-88341", "64000")
        val joint = account(l("Placements conjoints", "Joint investments"), AccountType.BROKERAGE, setOf(parentA.id, parentB.id), "J-55017", "150000")

        buy(rrspA, xeqt, 70, "1800", "24.10"); buy(rrspA, zag, 60, "1500", "15.20"); buy(rrspA, xeqt, 30, "1200", "27.40")
        buy(tfsaA, vfv, 66, "300", "78.30"); buy(tfsaA, xic, 48, "900", "29.90"); buy(tfsaA, vfv, 20, "120", "118.40")
        buy(rrspB, xeqt, 68, "1700", "24.60"); buy(rrspB, xef, 54, "900", "29.80"); buy(rrspB, zag, 26, "1100", "13.70")
        buy(tfsaB, xeqt, 64, "1400", "25.30"); buy(tfsaB, xeqt, 18, "700", "31.10")
        buy(joint, td, 62, "600", "72.40", "9.95"); buy(joint, enb, 58, "800", "48.10", "9.95"); buy(joint, cnr, 50, "200", "146.20", "9.95"); buy(joint, vfv, 40, "250", "92.70", "9.95")
        for (m in listOf(15, 12, 9, 6, 3)) {
            income(joint, td, m, "612.00"); income(joint, enb, m, "754.40"); income(joint, cnr, m, "177.50")
            income(rrspA, zag, m, "270.00", IncomeType.DISTRIBUTION); income(rrspB, zag, m, "198.00", IncomeType.DISTRIBUTION)
        }
        for ((s, price) in listOf(xeqt to "36.20", vfv to "158.40", xic to "42.10", zag to "14.05", xef to "41.30", td to "96.80", enb to "66.40", cnr to "151.90")) inv.setPrice(s.id, today, n(price))
        listOf(rrspA, tfsaA, rrspB, tfsaB).forEach { acc ->
            books.plans.saveBeneficiary(
                Beneficiary("", acc.id, if (acc.type == AccountType.TFSA) BeneficiaryKind.SUCCESSOR_HOLDER else BeneficiaryKind.BENEFICIARY,
                    if (acc in listOf(rrspA, tfsaA)) parentB.displayName else parentA.displayName, if (acc in listOf(rrspA, tfsaA)) parentB.id else parentA.id, l("Conjoint", "Spouse"),
                    if (acc.type == AccountType.TFSA) null else BigDecimal(100)),
            )
        }
    }

    /** Contribution room, and the family RESP for the three children with the grants received this year. */
    private fun addPlans() {
        val plans = books.plans
        val year = today.year
        plans.saveRoom(RoomEntry("", parentA.id, RoomPlan.RRSP, year, cad("21240")), group)
        plans.saveRoom(RoomEntry("", parentB.id, RoomPlan.RRSP, year, cad("17830")), group)
        plans.saveRoom(RoomEntry("", parentA.id, RoomPlan.TFSA, year, cad("14000")), group)
        plans.saveRoom(RoomEntry("", parentB.id, RoomPlan.TFSA, year, cad("21000")), group)
        val bank = books.institutions.list().first()
        val resp = books.accounts.create(
            AccountDraft(group, l("REEE familial", "Family RESP"), AccountType.RESP, Currency.CAD, cad("38600"), today.minus(DatePeriod(years = 7)), bank.id, "E-30217", setOf(parentA.id, parentB.id)),
        )
        val children = listOf(oldest, middle, youngest)
        children.forEach { plans.saveBeneficiary(Beneficiary("", resp.id, BeneficiaryKind.RESP_BENEFICIARY, it.displayName, it.id)) }
        val thisYear = LocalDate(year, 1, 1)
        add(TransferDraft(savings.id, resp.id, maxOf(monthsAgo(3), thisYear), cad("7500.00"), memo = l("Cotisation REEE (2 500 $ par enfant)", "RESP contribution ($2,500 per child)")))
        for (child in children) {
            plans.recordGrant(resp.id, child.id, maxOf(monthsAgo(2), thisYear), GrantKind.CESG, cad("500"))
            if (p.quebec) plans.recordGrant(resp.id, child.id, maxOf(monthsAgo(2), thisYear), GrantKind.QESI, cad("250"))
        }
        // British Columbia's one-time grant, for a child aged 6 to 9.
        if (p.province == Province.BC) plans.recordGrant(resp.id, oldest.id, maxOf(monthsAgo(4), thisYear), GrantKind.BCTESG, cad("1200"))
    }

    private fun addVehiclesAndPet() {
        val dealer = q("Honda de Sainte-Foy", p.t.honda)
        val van = books.vehicles.save(
            Vehicle("", group, "Odyssey", "Honda", "Odyssey", 2022, "EX-L", l("Blanc", "White"), "5FNRL6H71NB012345", q("G18 TRB", p.t.plate1), driverMemberId = parentB.id,
                purchaseDate = LocalDate(2022, 5, 20), purchasePrice = cad("51200"), seller = dealer, purchaseOdometer = 20, insurer = p.insurer, policyNumber = "AUT-3310228",
                insuranceRenewal = day(48), registrationRenewal = day(130)),
        )
        val suv = books.vehicles.save(
            Vehicle("", group, "Outback", "Subaru", "Outback", 2020, "Touring", l("Gris", "Grey"), "4S4BTANC5L3123456", q("K42 MLD", p.t.plate2), driverMemberId = parentA.id,
                purchaseDate = LocalDate(2021, 3, 2), purchasePrice = cad("31900"), seller = q("Subaru de Québec", "${p.t.city} Subaru"), purchaseOdometer = 41_000, insurer = p.insurer,
                policyNumber = "AUT-3310229", insuranceRenewal = day(48), registrationRenewal = day(260)),
        )
        val price = BigDecimal(q("1.689", p.t.fuelPrice))
        for ((v, base) in listOf(van to 61_000, suv to 104_000)) {
            var odo = base
            for (back in 5 downTo 0) {
                for (d in listOf(6, 20)) {
                    odo += 520
                    val date = inMonth(back, d)
                    if (date > today) continue
                    val litres = BigDecimal(if (v == van) "52.4" else "47.8")
                    books.vehicles.saveFuel(FuelEntry("", v.id, date, odo, litres, cad(litres * price), station = "Petro-Canada"), PaymentDraft(card.id, cat("transport.fuel"), "Petro-Canada"))
                }
            }
            val tasks = books.vehicles.addStarterTasks(v.id, today) { ca.schippers.hfm.i18n.Messages.get(books.language, "task.$it") }
            tasks.firstOrNull { it.templateKey == "oil" }?.let { oil ->
                books.vehicles.saveService(
                    ca.schippers.hfm.books.ServiceRecord("", v.id, day(-75), odo - 2600, q("Garage Tremblay", "Main Street Auto"), cost = cad("118.40"), taskIds = setOf(oil.id)),
                    PaymentDraft(card.id, cat("transport.maintenance"), q("Garage Tremblay", "Main Street Auto")),
                )
            }
        }
        val cat = books.pets.save(
            Pet("", l("Mistigri", "Pepper"), Species.CAT, l("Domestique à poil court", "Domestic shorthair"), Sex.FEMALE, LocalDate(2021, 9, 1), birthDateEstimated = true, colour = l("Tigrée", "Tabby"),
                neutered = true, microchip = "985141000556677", ownerMemberId = middle.id),
        )
        val vet = books.health.saveProvider(HealthProvider("", group, q("Clinique vétérinaire des Laurentides", "Riverside Animal Hospital"), ProviderKind.VET, p.phone("0190"), null, null, false))
        add(TransactionDraft(card.id, day(-52), cad("-212.30"), vet.name, listOf(SplitDraft(cat("pets.vet"), cad("-212.30"))), memberId = cat.id))
        books.health.saveImmunization(Immunization("", group, cat.id, l("Rage", "Rabies"), day(-52), vet.id, day(-52).plus(DatePeriod(years = 1)), null))
    }

    private fun addSchedulesAndActivities() {
        fun t(h: Int, m: Int = 0) = LocalTime(h, m)
        fun dow(n: Int) = DayOfWeek(n)
        val weekdays = 1..5
        val schedules = books.schedules
        schedules.save(PersonSchedule("", group, parentA.id, ScheduleKind.WORK, employerA, today.minus(DatePeriod(years = 3)), null, 1, true, null, weekdays.map { ScheduleShift(0, dow(it), t(7, 30), t(15, 30)) }))
        // Parent B works from home Monday, Wednesday and Friday; at the office the other days.
        schedules.save(PersonSchedule("", group, parentB.id, ScheduleKind.WORK, employerB, today.minus(DatePeriod(years = 2)), null, 1, true, null, weekdays.map { ScheduleShift(0, dow(it), t(9), t(17)) }))
        val pdDay = day(12).let { d -> if (d.dayOfWeek.ordinal >= 5) d.plus(DatePeriod(days = 7 - d.dayOfWeek.ordinal)) else d }
        for (child in listOf(oldest, middle)) {
            schedules.save(
                PersonSchedule("", group, child.id, ScheduleKind.SCHOOL, school, LocalDate(schoolYear, 9, 2), LocalDate(schoolYear + 1, 6, 24), 1, true, null,
                    weekdays.map { ScheduleShift(0, dow(it), t(8, 10), t(15, 0)) }, listOf(ScheduleException(pdDay, true, reason = l("Journée pédagogique", "PD day")))),
            )
        }
        schedules.save(PersonSchedule("", group, youngest.id, ScheduleKind.OTHER, daycare, LocalDate(schoolYear, 8, 25), null, 1, true, null, weekdays.map { ScheduleShift(0, dow(it), t(7, 45), t(16, 45)) }))

        // The older two's activities, with carpools and costs; hockey registration paid in September.
        val calendar = books.calendar
        val lastTuesday = today.minus(DatePeriod(days = ((today.dayOfWeek.ordinal - 1 + 7) % 7).let { if (it == 0) 7 else it }))
        val practice = calendar.create(
            EventDraft(group, l("Pratique de hockey", "Hockey practice"), EventCategory.ACTIVITY, lastTuesday, t(17, 30), 60, q("Aréna Bardy", "${p.t.city} Memorial Arena"), memberId = oldest.id,
                recurrence = Recurrence.WEEKLY, endDate = day(150), reminderMinutes = listOf(90), driverThere = Driver(memberId = parentA.id), driverBack = Driver(memberId = parentA.id)),
        )
        val saturday = today.plus(DatePeriod(days = (5 - today.dayOfWeek.ordinal + 7) % 7))
        calendar.create(
            EventDraft(group, l("Match de hockey", "Hockey game"), EventCategory.ACTIVITY, saturday, t(8, 0), 75, q("Centre sportif", "${p.t.city} Sportsplex"), memberId = oldest.id,
                recurrence = Recurrence.WEEKLY, endDate = day(150), reminderMinutes = listOf(1440), driverThere = Driver(memberId = parentA.id), driverBack = Driver(name = l("Karine (maman de Zoé)", "Karen (Zoe's mom)"))),
        )
        val lastThursday = today.minus(DatePeriod(days = ((today.dayOfWeek.ordinal - 3 + 7) % 7).let { if (it == 0) 7 else it }))
        val dance = calendar.create(
            EventDraft(group, l("Cours de ballet", "Ballet class"), EventCategory.ACTIVITY, lastThursday, t(17, 45), 45, q("École de danse Arabesque", "Arabesque Dance Studio"), memberId = middle.id,
                recurrence = Recurrence.WEEKLY, endDate = day(200), reminderMinutes = listOf(120), driverThere = Driver(memberId = parentB.id), driverBack = Driver(memberId = parentB.id), cost = cad("18.00")),
        )
        calendar.recordCost(dance.id, lastThursday, chequing.id, cat("children.activities"))
        calendar.create(
            EventDraft(group, l("Cours de natation", "Swimming lessons"), EventCategory.ACTIVITY, saturday, t(10, 30), 45, q("Centre aquatique", p.t.swimming), memberId = middle.id,
                recurrence = Recurrence.WEEKLY, endDate = day(70), reminderMinutes = listOf(60), driverThere = Driver(memberId = parentB.id), driverBack = Driver(memberId = parentB.id)),
        )
        val registration = LocalDate(schoolYear, 9, 8).takeIf { it <= today } ?: monthsAgo(1)
        add(TransactionDraft(chequing.id, registration, cad("-695.00"), hockey, listOf(SplitDraft(cat("children.activities"), cad("-695.00"))), memberId = oldest.id, memo = l("Inscription de la saison", "Season registration")))
        add(TransactionDraft(card.id, registration.plus(DatePeriod(days = 3)), cad("-389.97"), q("Sports Experts", "Sport Chek"), listOf(SplitDraft(cat("children.activities"), cad("-389.97"))), memberId = oldest.id, memo = l("Patins et équipement", "Skates and gear")))
        add(TransactionDraft(chequing.id, registration.plus(DatePeriod(days = 1)), cad("-540.00"), q("École de danse Arabesque", "Arabesque Dance Studio"), listOf(SplitDraft(cat("children.activities"), cad("-540.00"))), memberId = middle.id, memo = l("Session d’automne", "Fall session")))
        calendar.create(EventDraft(group, l("Rencontre de parents", "Parent-teacher interviews"), EventCategory.OTHER, day(9), t(18, 30), 30, school, memberId = oldest.id))
        calendar.create(EventDraft(group, l("Fête de Florence", "Lily's birthday party"), EventCategory.PERSONAL, day(17), t(13, 0), 180, l("À la maison", "At home"), memberId = middle.id))
        // In two weeks a teammate's mother drives to practice.
        calendar.setDrivers(practice.id, lastTuesday.plus(DatePeriod(days = 14)), Driver(name = l("Karine (maman de Zoé)", "Karen (Zoe's mom)")), Driver(memberId = parentA.id))
    }

    private fun addHealth() {
        val health = books.health
        val med = books.medical
        val pediatrician = health.saveProvider(HealthProvider("", group, q("Dre Isabelle Roy, pédiatre", l("Dre Anita Shah, pédiatre", "Dr. Anita Shah, pediatrician")), ProviderKind.DOCTOR, p.phone("0144"), null, null, false))
        val dentist = health.saveProvider(HealthProvider("", group, q("Clinique dentaire des Bâtisseurs", "${p.t.street2} Family Dental"), ProviderKind.DENTIST, p.phone("0178"), null, null, false))
        val pharmacy = health.saveProvider(HealthProvider("", group, q("Pharmacie Familiprix", "Shoppers Drug Mart"), ProviderKind.PHARMACY, p.phone("0101"), null, null, false))
        health.saveAllergy(Allergy("", group, middle.id, l("Noix", "Tree nuts"), l("Urticaire", "Hives"), Severity.MODERATE, l("Épipen à l’école et à la garderie", "EpiPen at school and in the dance bag")))
        health.saveImmunization(Immunization("", group, youngest.id, l("RRO-varicelle (2e dose)", "MMRV (2nd dose)"), day(-200), pediatrician.id, null, null))
        health.saveImmunization(Immunization("", group, oldest.id, "Influenza", day(-330), pharmacy.id, day(30), null))
        books.calendar.create(EventDraft(group, l("Examen annuel des enfants", "Children's yearly check-up"), EventCategory.MEDICAL, day(22), LocalTime(9, 0), 60, memberId = youngest.id, providerId = pediatrician.id))
        books.calendar.create(EventDraft(group, l("Nettoyage dentaire (3 enfants)", "Dental cleaning (all 3 kids)"), EventCategory.MEDICAL, day(11), LocalTime(15, 30), 90, memberId = oldest.id, providerId = dentist.id))

        val everyone = listOf(parentA, parentB, oldest, middle, youngest)
        val planA = med.savePlan(MedPlan("", group, MedPlanKind.GROUP_HEALTH, l("Assurance collective ($employerA)", "Group benefits ($employerA)"), q("Beneva", "Sun Life"), "G-20441", "C-7781020", parentA.id,
            people = everyone.map { PlanPerson(it.id, if (it == parentB) 2 else 1) }))
        val planB = med.savePlan(MedPlan("", group, MedPlanKind.GROUP_HEALTH, l("Assurance collective ($employerB)", "Group benefits ($employerB)"), q("Desjardins Assurances", "Manulife"), "77810", "M-44102", parentB.id,
            people = everyone.map { PlanPerson(it.id, if (it == parentB) 1 else 2) }))
        for ((plan, rate) in listOf(planA to "80", planB to "70")) {
            med.saveCoverage(MedCoverage("", plan.id, MedService.PRESCRIPTION, BigDecimal(rate)))
            med.saveCoverage(MedCoverage("", plan.id, MedService.DENTAL_PREVENTIVE, BigDecimal("90"), frequencyMonths = 9))
            med.saveCoverage(MedCoverage("", plan.id, MedService.EYE_EXAM, BigDecimal("100"), perVisitMax = cad("100"), frequencyMonths = 24))
        }
        fun expense(who: Member, service: MedService, days: Int, amount: String, what: String) =
            med.saveExpense(MedExpense("", group, who.id, service, day(days), cad(amount), description = what))
        val cleaning = expense(oldest, MedService.DENTAL_PREVENTIVE, -150, "210.00", l("Nettoyage et radiographies", "Cleaning and X-rays"))
        med.recordPayment(med.submit(cleaning.id, planA.id, day(-149)).id, day(-140), cad("189.00"))
        val eyes = expense(middle, MedService.EYE_EXAM, -60, "110.00", l("Examen de la vue", "Eye exam"))
        med.submit(eyes.id, planA.id, day(-59))
        expense(parentB, MedService.PRESCRIPTION, -12, "36.80", l("Antibiotique", "Antibiotic"))
    }

    private fun addInsuranceAndEstate() {
        val insurance = books.insurance
        val house = books.assets.list().first { it.kind == AssetKind.HOME }
        val cottage = books.assets.list().first { it.kind == AssetKind.COTTAGE }
        insurance.save(InsurancePolicy("", group, PolicyKind.HOME, p.insurer, q("Courtier Morin", "Wilson Insurance Brokers"), "H-6620117", parentA.id, cad("2140"), deductible = cad("1000"), coverage = cad("760000"), startDate = day(-200), renewalDate = day(165), assetIds = setOf(house.id)))
        insurance.save(InsurancePolicy("", group, PolicyKind.HOME, p.insurer, q("Courtier Morin", "Wilson Insurance Brokers"), "H-6620118", parentA.id, cad("1260"), deductible = cad("2500"), coverage = cad("420000"), startDate = day(-310), renewalDate = day(55), assetIds = setOf(cottage.id)))
        insurance.save(InsurancePolicy("", group, PolicyKind.AUTO, p.insurer, policyNumber = "AUT-3310228", premium = cad("212.40"), frequency = PremiumFrequency.MONTHLY, deductible = cad("500"), renewalDate = day(48), assetIds = books.vehicles.list().map { it.id }.toSet()))
        for ((who, other, number) in listOf(Triple(parentA, parentB, "L-904122"), Triple(parentB, parentA, "L-904123"))) {
            val life = insurance.save(InsurancePolicy("", group, PolicyKind.LIFE, q("Industrielle Alliance", "Canada Life"), policyNumber = number, insuredMemberId = who.id, premium = cad("58.40"), frequency = PremiumFrequency.MONTHLY, coverage = cad("1000000"), coverageNotes = l("Temporaire 20 ans", "20-year term")))
            insurance.saveBeneficiary(PolicyBeneficiary("", life.id, other.displayName, other.id, l("Conjoint", "Spouse"), BigDecimal(100)))
        }
        insurance.save(InsurancePolicy("", group, PolicyKind.DISABILITY, q("Beneva", "Sun Life"), policyNumber = "LTD-20441", insuredMemberId = parentA.id, premium = cad("0"), frequency = PremiumFrequency.MONTHLY, coverageNotes = l("Invalidité de longue durée, 66,7 % du salaire (employeur)", "Long-term disability, 66.7% of salary (through work)")))

        val guardians = l("Marie et Luc Gagnon (sœur de Julie)", "Anjali and Sam Rao (Priya's sister)")
        for ((who, other) in listOf(parentA to parentB, parentB to parentA)) {
            books.estate.save(
                who.id, group,
                EstatePlan(
                    willLocation = if (p.quebec) l("Testament notarié, chez Me Lavoie", "Notarial will, at Me Lavoie's") else l("Bureau de l’avocat (original); copie dans le classeur", "Lawyer's office (original); copy in the filing cabinet"),
                    willDate = "${today.year - 2}-11-14",
                    powerOfAttorneyLocation = l("Classeur, chemise « Succession »", "Filing cabinet, \"Estate\" folder"),
                    digitalAccounts = l("Gestionnaire de mots de passe familial; trousse d’urgence dans le coffre", "Family password manager; emergency kit in the fireproof box"),
                    contacts = listOf(
                        EstateContact(if (p.quebec) ContactRole.LIQUIDATOR else ContactRole.EXECUTOR, other.displayName),
                        EstateContact(ContactRole.OTHER, guardians, notes = l("Tuteurs des enfants", "Guardians for the children")),
                        EstateContact(if (p.quebec) ContactRole.NOTARY else ContactRole.LAWYER, q("Me Chantal Lavoie", l("Me David Kim", "David Kim")), q("Lavoie notaires", "Kim & Associates"), p.phone("0130")),
                    ),
                ),
            )
        }
    }

    /** Parent A coaches with minor hockey; parent B with the Breakfast Club and on the school council. */
    private fun addVolunteering() {
        val volunteer = books.volunteer
        var week = today.minus(DatePeriod(days = 7 * 10))
        while (week <= today) {
            volunteer.save(VolunteerEntry("", group, parentA.id, hockey, VolunteerKind.OTHER, week, 150, activity = l("Entraîneur adjoint", "Assistant coach")))
            week = week.plus(DatePeriod(days = 7))
        }
        for (n in listOf(-84, -56, -28, -7)) volunteer.save(VolunteerEntry("", group, parentB.id, breakfastClub, VolunteerKind.OTHER, day(n), 120, activity = l("Service des déjeuners", "Serving breakfast")))
        for (n in listOf(-35, -5)) volunteer.save(VolunteerEntry("", group, parentB.id, l("Conseil d’établissement - $school", "$school council"), VolunteerKind.SCHOOL, day(n), 90, activity = l("Réunion", "Meeting")))
    }

    private fun addChores() {
        for (child in listOf(oldest, middle)) {
            val allowance = books.allowances.save(Allowance("", group, child.id, cad("5.00"), AllowanceFrequency.WEEKLY, day(-35), null, null, emptyList()))
            for (w in 0..4) books.allowances.addEntry(allowance, day(-35 + 7 * w), cad("5.00"), AllowanceKind.PAID)
            val tidy = books.chores.save(Chore("", group, child.id, l("Ranger sa chambre", "Tidy the bedroom"), Currency.CAD, cad("1.00"), 2))
            val pet = books.chores.save(Chore("", group, child.id, l("Nourrir Mistigri", "Feed Pepper"), Currency.CAD, null, 1))
            for (n in listOf(-12, -9, -5, -2)) books.chores.tick(tidy.id, day(n))
            for (n in -6..0) books.chores.tick(pet.id, day(n))
            books.chores.pay(allowance, day(-7))
        }
    }

    private fun addGoalsAndBudgets() {
        val goals = books.goals
        val trip = goals.save(SavingsGoal("", savings.id, l("Voyage en famille", "Family trip"), cad("9000"), LocalDate(today.year + 1, 7, 1), cad("400"), Recurrence.MONTHLY, start))
        goals.setAside(trip.id, start, cad("2500"), l("Départ", "Starting amount"))
        val kitchen = goals.save(SavingsGoal("", savings.id, l("Rénovation de la cuisine", "Kitchen renovation"), cad("40000"), LocalDate(today.year + 3, 5, 1), cad("500"), Recurrence.MONTHLY, start))
        goals.setAside(kitchen.id, start, cad("8000"))
        val emergency = goals.save(SavingsGoal("", savings.id, l("Fonds d’urgence", "Emergency fund"), cad("30000")))
        goals.setAside(emergency.id, start, cad("18000"))
        val dock = goals.save(SavingsGoal("", savings.id, l("Nouveau quai au chalet", "New dock at the cottage"), cad("6500"), LocalDate(today.year + 1, 5, 15), cad("300"), Recurrence.MONTHLY, start))
        goals.setAside(dock.id, start, cad("1200"))
        goals.save(SavingsGoal("", savings.id, l("Activités des enfants", "Kids' activities"), cad("2500"), LocalDate(today.year + 1, 8, 31)))
        goals.postScheduled(today)
        books.homeProjects.save(HomeProject("", group, l("Rénovation de la cuisine", "Kitchen renovation"), ProjectStatus.PLANNED, Currency.CAD, books.assets.list().first { it.kind == AssetKind.HOME }.id, budget = cad("40000")))
        val contractor = books.contractors.save(Contractor("", group, q("Rénovations Bouchard", "Maple Leaf Renovations"), l("Entrepreneur général", "General contractor"), p.phone("0166")))
        books.contractors.saveJob(contractor, ContractorJob("", day(-400), l("Salle de bain du sous-sol", "Basement bathroom"), cad("18400.00"), 4))

        val firstMonth = LocalDate(start.year, start.month, 1)
        val budgets = books.budgets
        budgets.set(cat("food.groceries"), BudgetPeriod.MONTHLY, cad("1600.00"), rollover = true, startMonth = firstMonth)
        budgets.set(cat("food.restaurants"), BudgetPeriod.MONTHLY, cad("200.00"), startMonth = firstMonth)
        budgets.set(cat("children.childcare"), BudgetPeriod.MONTHLY, cad(BigDecimal(daycareMonthly) + BigDecimal("385.00")), startMonth = firstMonth)
        budgets.set(cat("children.activities"), BudgetPeriod.MONTHLY, cad("250.00"), rollover = true, startMonth = firstMonth)
        budgets.set(cat("transport"), BudgetPeriod.MONTHLY, cad("650.00"), startMonth = firstMonth)
        budgets.set(cat("utilities"), BudgetPeriod.MONTHLY, cad("560.00"), startMonth = firstMonth)
        budgets.set(cat("leisure.entertainment"), BudgetPeriod.MONTHLY, cad("80.00"), startMonth = firstMonth)
        budgets.set(cat("gifts"), BudgetPeriod.MONTHLY, cad("150.00"), rollover = true, startMonth = firstMonth)
    }

    /** CON-01: the contacts gathered from the institutions, providers, contractors and estate contacts above. */
    private fun addContacts() {
        val contacts = books.contacts
        contacts.gather(group, contacts.proposals().flatMap { proposal -> proposal.sources.groupBy { SearchService.fold(it.name) }.values.map { GatherDecision(it) } })
    }
}
