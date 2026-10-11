// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/DemoPlace.kt
// Modified On Timestamp: 2026-10-11 @ 13:05 EDT
// Created On Timestamp: 2026-10-11 @ 12:26 EDT
// File Description: Where a sample household lives: the local names, prices and places of each
//   province and territory, kept apart from the language its text is written in.
// Uses: ca.schippers.hfm.calc.Province, IncomeTax, SalesTaxes, core/i18n province names.
// Used By: DemoHousehold, DemoTrips, DemoSeasonal.
// Purpose: Let any sample household be opened in English or French in any province or territory.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.calc.salestax.SalesTaxes
import ca.schippers.hfm.calc.tax.IncomeTax
import ca.schippers.hfm.calc.tax.TaxInput
import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.i18n.Messages
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode

/** A point on the map, for the saved places of the trip log. */
internal data class LatLon(val lat: Double, val lon: Double) {
    fun shift(dLat: Double, dLon: Double) = LatLon(lat + dLat, lon + dLon)
}

/**
 * Where a sample household lives, apart from the language of its text. [l] picks the text in the
 * household's language; [q] picks a name as it would be in Quebec or elsewhere in Canada (a bank,
 * a garage, a store's receipt), whatever the language; the other values are the province's or
 * territory's own (its city, utilities, hospitals, prices). Quebec's own names are written where
 * they are used, as the first argument of [q]; the rest of Canada's come from [Local].
 */
internal class DemoPlace(val province: Province, val english: Boolean) {
    val quebec: Boolean get() = province.isQuebec
    val language: Language get() = if (english) Language.ENGLISH else Language.FRENCH

    /** The French or English version of a text. */
    fun l(fr: String, en: String) = if (english) en else fr

    /** The Quebec name, or the one used elsewhere in Canada. */
    fun q(qc: String, other: String) = if (quebec) qc else other

    /** The local values; in Quebec, Ontario's stand in only where no Quebec name is given. */
    val t: Local = LOCAL.getValue(if (quebec) Province.ON else province)

    val city: String get() = q(l("Québec", "Quebec City"), t.city)
    val power: String get() = q("Hydro-Québec", l(t.powerFr, t.power))
    val bank: String get() = q("Desjardins", "TD")
    val insurer: String get() = q("Desjardins Assurances", "Intact Insurance")
    val specialist: String get() = q("Institut de cardiologie de Montréal", t.specialist)
    val cottage: String get() = l("Chalet", "Cottage") + " (" + q("Lac-Édouard", t.cottage) + ")"
    val cottageTown: String get() = q("Lac-Édouard", t.cottageTown)

    // Sam's two clients' towns as the trip log names them, and how far each is.
    val client1: String get() = q("Lévis", t.client1).let { l("Client à $it", "Client in $it") }
    val client1Km: String get() = q("28", t.client1Km.toString())
    val client2: String get() = q("Montmagny", t.client2).let { l("Client à $it", "Client in $it") }
    val client2Km: String get() = q("78", t.client2Km.toString())
    val area: String get() = q("418", t.area)
    fun phone(last: String) = "$area-555-$last"
    val municipality: String get() = q("Ville de Québec", l("Ville de ${t.city}", "City of ${t.city}"))
    val provinceName: String get() = Messages.get(language, "province.${province.name}")

    /** The main street in short ("Bank St"), as on a sign or a pharmacy's address. */
    val mainStreetShort: String get() = t.mainStreet.replace(" Street", " St").replace(" Avenue", " Ave").replace(" Road", " Rd")

    /** The sales taxes on [on], each as (label, rate), and [base] with them added. */
    fun salesTaxes(on: LocalDate) = SalesTaxes.ratesOn(on, province)
    fun withSalesTax(base: BigDecimal, on: LocalDate): BigDecimal =
        base.add(salesTaxes(on).fold(BigDecimal.ZERO) { sum, r -> sum + base * r.rate }).setScale(2, RoundingMode.HALF_UP)

    /**
     * The provincial or territorial income tax taken from each of 24 pays of [gross], from the
     * app's own tax rules (Ontario's and Quebec's pay stubs keep the figures they always had).
     */
    fun provincialTaxPerPay(gross: BigDecimal, year: Int): BigDecimal =
        IncomeTax.estimate(year, province, mapOf(TaxInput.EMPLOYMENT to gross * BigDecimal(24)), false).provincialTax
            .divide(BigDecimal(24), 0, RoundingMode.HALF_UP).setScale(2)

    /**
     * One province's or territory's local names, places and prices. Coordinates are approximate
     * and the businesses mostly invented; the public bodies (utilities, transit, hospitals) are the
     * real ones. [gas] is null where homes are not heated with natural gas.
     */
    data class Local(
        val city: String,
        val area: String,
        val home: LatLon,
        val neighbourhood: String,
        val mainStreet: String,
        val street2: String,
        val street3: String,
        val bankStreet: String,
        val school: String,
        val power: String,
        val powerFr: String = power,
        val powerPhone: String,
        val powerWeb: String,
        val timeOfUse: Boolean = false,
        val gas: String?,
        val internet: String,
        val grocer: String,
        val foodBank: String,
        val transit: String?,
        val foundation: String,
        val hospital: String,
        val specialist: String,
        val specialistKm: Int,
        val specialistAt: LatLon,
        val client1: String,
        val client1Km: Int,
        val client2: String,
        val client2Km: Int,
        val clientAddress: String,
        val clientAt: LatLon,
        val cottage: String,
        val cottageTown: String,
        val cottageAt: LatLon,
        val toCottage: Int,
        val fireDept: String,
        val propane: String,
        val assessor: String,
        val pension: String,
        val pensionPrefix: String,
        val plumber: String,
        val swimming: String,
        val tournamentPark: String,
        val gamePark: String,
        val lunch: String,
        val poolStore: String,
        val energyRebate: String?,
        val energyRebateRef: String = "ER-77102",
        val cityRebate: String? = null,
        val waterHeaterRebate: String? = null,
        val plate1: String,
        val plate2: String,
        val fuelPrice: String,
        val kwhPrice: String,
        val propertyTax: String,
        val roll: String,
        val work: LatLon = home.shift(0.0236, 0.0079),
        val station: LatLon = home.shift(0.0062, 0.0167),
        val garage: LatLon = home.shift(0.0155, 0.0319),
        val charger: String = "FLO ($cottage)",
        val honda: String = "$city Honda",
        val toyota: String = "$city Toyota",
    )

    companion object {
        /** Sample households open in Quebec in French and in Ontario in English unless told otherwise. */
        fun defaultFor(language: Language): Province = if (language == Language.FRENCH) Province.QC else Province.ON

        private val LOCAL: Map<Province, Local> = mapOf(
            Province.ON to Local(
                city = "Ottawa", area = "613", home = LatLon(45.39530, -75.70600), neighbourhood = "Glebe", mainStreet = "Bank Street", street2 = "Elgin Street",
                street3 = "Carling Ave", bankStreet = "Sparks St.", school = "Hopewell Public School",
                power = "Hydro Ottawa", powerPhone = "613-738-6400", powerWeb = "hydroottawa.com", timeOfUse = true, gas = "Enbridge Gas", internet = "Rogers",
                grocer = "Loblaws", foodBank = "Ottawa Food Bank", transit = "OC Transpo", foundation = "CHEO Foundation", hospital = "Civic Hospital",
                specialist = "Kingston Health Sciences Centre", specialistKm = 196, specialistAt = LatLon(44.22460, -76.49340),
                client1 = "Kanata", client1Km = 28, client2 = "Smiths Falls", client2Km = 78, clientAddress = "300 Terry Fox Drive, Kanata", clientAt = LatLon(45.30880, -75.89870),
                cottage = "Bon Echo", cottageTown = "Cloyne", cottageAt = LatLon(44.89790, -77.20940), toCottage = 182, fireDept = "Addington Highlands Fire Department",
                propane = "Rideau Propane", assessor = "MPAC", pension = "OMERS Primary Pension Plan", pensionPrefix = "OM", plumber = "Rideau Plumbing",
                swimming = "Brewer Pool", tournamentPark = "Mooney's Bay", gamePark = "Lansdowne Park", lunch = "The Wellington", poolStore = "Pioneer Family Pools",
                energyRebate = "Home Renovation Savings Program (Enbridge Gas)", energyRebateRef = "HRSP-77102", cityRebate = "City of Ottawa Better Homes rebate",
                waterHeaterRebate = "Home Renovation Savings Program (IESO)", plate1 = "CKMR 482", plate2 = "CPXM 219", fuelPrice = "1.629", kwhPrice = "0.098",
                propertyTax = "4812.00", roll = "Roll 0614 123 4567 8900",
                work = LatLon(45.41890, -75.69810), station = LatLon(45.40150, -75.68930), garage = LatLon(45.41080, -75.67410), charger = "FLO (Bon Echo Road)",
                honda = "Ottawa Honda", toyota = "Ottawa Toyota",
            ),
            Province.AB to Local(
                city = "Calgary", area = "403", home = LatLon(51.03000, -114.08500), neighbourhood = "Mission", mainStreet = "17th Avenue SW", street2 = "4th Street SW",
                street3 = "Macleod Trail", bankStreet = "Stephen Ave", school = "Elbow Park School",
                power = "ENMAX", powerPhone = "1 800 555-0164", powerWeb = "enmax.com", gas = "ATCO Gas", internet = "Telus",
                grocer = "Calgary Co-op", foodBank = "Calgary Food Bank", transit = "Calgary Transit", foundation = "Alberta Children's Hospital Foundation",
                hospital = "Foothills Medical Centre", specialist = "University of Alberta Hospital", specialistKm = 300, specialistAt = LatLon(53.52100, -113.52400),
                client1 = "Airdrie", client1Km = 33, client2 = "Strathmore", client2Km = 55, clientAddress = "200 Main Street N, Airdrie", clientAt = LatLon(51.29200, -114.01400),
                cottage = "Sylvan Lake", cottageTown = "Sylvan Lake", cottageAt = LatLon(52.30900, -114.09600), toCottage = 160, fireDept = "Sylvan Lake Fire Department",
                propane = "Superior Propane", assessor = "City of Calgary", pension = "Local Authorities Pension Plan", pensionPrefix = "LAPP", plumber = "Bow River Plumbing",
                swimming = "Southland Leisure Centre", tournamentPark = "Prince's Island Park", gamePark = "Shouldice Park", lunch = "Cactus Club Cafe", poolStore = "Calgary Pool & Spa",
                energyRebate = null, plate1 = "CKM-4821", plate2 = "CPX-2190", fuelPrice = "1.399", kwhPrice = "0.180", propertyTax = "4310.00", roll = "Roll 201 456 789",
            ),
            Province.BC to Local(
                city = "Vancouver", area = "604", home = LatLon(49.26200, -123.11500), neighbourhood = "Kitsilano", mainStreet = "West Broadway", street2 = "West 4th Avenue",
                street3 = "Granville Street", bankStreet = "Granville St", school = "Henry Hudson Elementary",
                power = "BC Hydro", powerPhone = "1 800 555-0164", powerWeb = "bchydro.com", gas = "FortisBC", internet = "Telus",
                grocer = "Save-On-Foods", foodBank = "Greater Vancouver Food Bank", transit = "TransLink", foundation = "BC Children's Hospital Foundation",
                hospital = "Vancouver General Hospital", specialist = "Kelowna General Hospital", specialistKm = 390, specialistAt = LatLon(49.87300, -119.49500),
                client1 = "Surrey", client1Km = 30, client2 = "Abbotsford", client2Km = 72, clientAddress = "10355 King George Boulevard, Surrey", clientAt = LatLon(49.19000, -122.84900),
                cottage = "Harrison Lake", cottageTown = "Harrison Hot Springs", cottageAt = LatLon(49.30200, -121.78500), toCottage = 130,
                fireDept = "Harrison Hot Springs Fire Department", propane = "Superior Propane", assessor = "BC Assessment", pension = "Municipal Pension Plan", pensionPrefix = "MPP",
                plumber = "Coastal Plumbing", swimming = "Kitsilano Pool", tournamentPark = "Vanier Park", gamePark = "Kitsilano Beach Park", lunch = "The Naam",
                poolStore = "Coast Pool & Spa", energyRebate = "CleanBC Better Homes", energyRebateRef = "CBH-77102", plate1 = "LK8 21P", plate2 = "PX2 19T",
                fuelPrice = "1.759", kwhPrice = "0.110", propertyTax = "3920.00", roll = "Folio 012-345-67-8900",
            ),
            Province.SK to Local(
                city = "Saskatoon", area = "306", home = LatLon(52.13000, -106.66000), neighbourhood = "Nutana", mainStreet = "Broadway Avenue", street2 = "8th Street East",
                street3 = "Idylwyld Drive", bankStreet = "2nd Ave", school = "Victoria School",
                power = "SaskPower", powerPhone = "1 800 555-0164", powerWeb = "saskpower.com", gas = "SaskEnergy", internet = "SaskTel",
                grocer = "Saskatoon Co-op", foodBank = "Saskatoon Food Bank & Learning Centre", transit = "Saskatoon Transit",
                foundation = "Jim Pattison Children's Hospital Foundation", hospital = "Royal University Hospital", specialist = "Regina General Hospital", specialistKm = 240,
                specialistAt = LatLon(50.45000, -104.59000), client1 = "Warman", client1Km = 28, client2 = "Humboldt", client2Km = 112,
                clientAddress = "107 Central Street, Warman", clientAt = LatLon(52.32100, -106.58400), cottage = "Waskesiu Lake", cottageTown = "Waskesiu",
                cottageAt = LatLon(53.92000, -106.08000), toCottage = 230, fireDept = "Lakeland District Fire Department", propane = "Superior Propane",
                assessor = "City of Saskatoon", pension = "Municipal Employees' Pension Plan", pensionPrefix = "MEPP", plumber = "Prairie Plumbing", swimming = "Shaw Centre",
                tournamentPark = "Kinsmen Park", gamePark = "Gordie Howe Sports Complex", lunch = "Broadway Roastery", poolStore = "Prairie Pool & Spa", energyRebate = null,
                plate1 = "482 KMR", plate2 = "219 PXM", fuelPrice = "1.459", kwhPrice = "0.170", propertyTax = "4120.00", roll = "Roll 512 334 210",
            ),
            Province.MB to Local(
                city = "Winnipeg", area = "204", home = LatLon(49.88000, -97.14500), neighbourhood = "Wolseley", mainStreet = "Portage Avenue", street2 = "Osborne Street",
                street3 = "Pembina Highway", bankStreet = "Portage Ave", school = "Laura Secord School",
                power = "Manitoba Hydro", powerPhone = "1 800 555-0164", powerWeb = "hydro.mb.ca", gas = null, internet = "Bell MTS",
                grocer = "Safeway", foodBank = "Harvest Manitoba", transit = "Winnipeg Transit", foundation = "Children's Hospital Foundation of Manitoba",
                hospital = "Health Sciences Centre", specialist = "Brandon Regional Health Centre", specialistKm = 215, specialistAt = LatLon(49.84400, -99.94500),
                client1 = "Selkirk", client1Km = 34, client2 = "Steinbach", client2Km = 60, clientAddress = "200 Eaton Avenue, Selkirk", clientAt = LatLon(50.14400, -96.88400),
                cottage = "Gimli", cottageTown = "Gimli", cottageAt = LatLon(50.63300, -96.99000), toCottage = 90, fireDept = "Gimli Fire Department",
                propane = "Superior Propane", assessor = "City of Winnipeg", pension = "Winnipeg Civic Employees' Pension Plan", pensionPrefix = "WCEP",
                plumber = "Red River Plumbing", swimming = "Pan Am Pool", tournamentPark = "Assiniboine Park", gamePark = "The Forks", lunch = "Stella's",
                poolStore = "Prairie Pool & Spa", energyRebate = "Efficiency Manitoba insulation rebate", energyRebateRef = "EM-77102", plate1 = "KMR 482", plate2 = "PXM 219",
                fuelPrice = "1.399", kwhPrice = "0.100", propertyTax = "4290.00", roll = "Roll 03010245600",
            ),
            Province.NB to Local(
                city = "Moncton", area = "506", home = LatLon(46.08800, -64.77800), neighbourhood = "Bessborough", mainStreet = "Main Street", street2 = "St. George Street",
                street3 = "Mountain Road", bankStreet = "Main St", school = "Bessborough School",
                power = "NB Power", powerFr = "Énergie NB", powerPhone = "1 800 555-0164", powerWeb = "nbpower.com", gas = "Liberty Utilities (Gas New Brunswick)", internet = "Bell Aliant",
                grocer = "Sobeys", foodBank = "Peter McKee Community Food Centre", transit = "Codiac Transpo", foundation = "Friends of The Moncton Hospital",
                hospital = "The Moncton Hospital", specialist = "Saint John Regional Hospital", specialistKm = 155, specialistAt = LatLon(45.30300, -66.08700),
                client1 = "Shediac", client1Km = 27, client2 = "Sussex", client2Km = 77, clientAddress = "290 Main Street, Shediac", clientAt = LatLon(46.21900, -64.54100),
                cottage = "Bouctouche", cottageTown = "Bouctouche", cottageAt = LatLon(46.47000, -64.73000), toCottage = 45, fireDept = "Bouctouche Fire Department",
                propane = "Superior Propane", assessor = "Service New Brunswick", pension = "Public Service Shared Risk Plan", pensionPrefix = "PSSRP",
                plumber = "Petitcodiac Plumbing", swimming = "YMCA of Greater Moncton", tournamentPark = "Centennial Park", gamePark = "CN Sportplex", lunch = "Tide & Boar",
                poolStore = "Maritime Pool & Spa", energyRebate = "NB Power Total Home Energy Savings Program", energyRebateRef = "THES-77102",
                plate1 = "KMR 482", plate2 = "PXM 219", fuelPrice = "1.559", kwhPrice = "0.130", propertyTax = "4480.00", roll = "PAN 01234567",
            ),
            Province.NS to Local(
                city = "Halifax", area = "902", home = LatLon(44.64200, -63.58500), neighbourhood = "South End", mainStreet = "Spring Garden Road", street2 = "Quinpool Road",
                street3 = "Robie Street", bankStreet = "Barrington St", school = "Inglis Street Elementary",
                power = "Nova Scotia Power", powerPhone = "1 800 555-0164", powerWeb = "nspower.ca", gas = "Eastward Energy", internet = "Eastlink",
                grocer = "Sobeys", foodBank = "Feed Nova Scotia", transit = "Halifax Transit", foundation = "IWK Foundation", hospital = "QEII Health Sciences Centre",
                specialist = "Cape Breton Regional Hospital", specialistKm = 400, specialistAt = LatLon(46.12700, -60.18400),
                client1 = "Lower Sackville", client1Km = 28, client2 = "Windsor", client2Km = 70, clientAddress = "752 Sackville Drive, Lower Sackville",
                clientAt = LatLon(44.77600, -63.67800), cottage = "Chester", cottageTown = "Chester", cottageAt = LatLon(44.54200, -64.23900), toCottage = 65,
                fireDept = "Chester Volunteer Fire Department", propane = "Superior Propane", assessor = "PVSC", pension = "Halifax Regional Municipality Pension Plan",
                pensionPrefix = "HRM", plumber = "Harbour Plumbing", swimming = "Centennial Pool", tournamentPark = "Point Pleasant Park", gamePark = "Wanderers Grounds",
                lunch = "The Bicycle Thief", poolStore = "Maritime Pool & Spa", energyRebate = "Efficiency Nova Scotia home energy rebate", energyRebateRef = "ENS-77102",
                plate1 = "FKM 482", plate2 = "FPX 219", fuelPrice = "1.579", kwhPrice = "0.190", propertyTax = "4610.00", roll = "AAN 01234567",
            ),
            Province.PE to Local(
                city = "Charlottetown", area = "902", home = LatLon(46.23800, -63.13100), neighbourhood = "Brighton", mainStreet = "University Avenue", street2 = "Grafton Street",
                street3 = "Kent Street", bankStreet = "Queen St", school = "Prince Street Elementary",
                power = "Maritime Electric", powerPhone = "1 800 555-0164", powerWeb = "maritimeelectric.com", gas = null, internet = "Eastlink",
                grocer = "Sobeys", foodBank = "Upper Room Hospitality Ministry", transit = "T3 Transit", foundation = "QEH Foundation", hospital = "Queen Elizabeth Hospital",
                specialist = "Prince County Hospital", specialistKm = 70, specialistAt = LatLon(46.39500, -63.79000),
                client1 = "Cornwall", client1Km = 12, client2 = "Summerside", client2Km = 70, clientAddress = "39 Lowther Drive, Cornwall", clientAt = LatLon(46.22700, -63.21800),
                cottage = "Cavendish", cottageTown = "Cavendish", cottageAt = LatLon(46.49200, -63.38100), toCottage = 40, fireDept = "North Rustico Fire Department",
                propane = "Superior Propane", assessor = "Province of PEI", pension = "City of Charlottetown Pension Plan", pensionPrefix = "CTP", plumber = "Island Plumbing",
                swimming = "Bell Aliant Centre", tournamentPark = "Victoria Park", gamePark = "Memorial Field", lunch = "Receiver Coffee", poolStore = "Island Pool & Spa",
                energyRebate = "efficiencyPEI insulation rebate", energyRebateRef = "EPEI-77102", plate1 = "K4821", plate2 = "P2190",
                fuelPrice = "1.599", kwhPrice = "0.170", propertyTax = "3240.00", roll = "PID 345678",
            ),
            Province.NL to Local(
                city = "St. John's", area = "709", home = LatLon(47.56200, -52.71300), neighbourhood = "Georgestown", mainStreet = "Water Street", street2 = "Duckworth Street",
                street3 = "Topsail Road", bankStreet = "Water St", school = "Bishop Feild Elementary",
                power = "Newfoundland Power", powerPhone = "1 800 555-0164", powerWeb = "newfoundlandpower.com", gas = null, internet = "Bell Aliant",
                grocer = "Dominion", foodBank = "Community Food Sharing Association", transit = "Metrobus", foundation = "Janeway Children's Hospital Foundation",
                hospital = "Health Sciences Centre", specialist = "Western Memorial Regional Hospital", specialistKm = 690, specialistAt = LatLon(48.95100, -57.95200),
                client1 = "Paradise", client1Km = 15, client2 = "Carbonear", client2Km = 105, clientAddress = "1505 Topsail Road, Paradise", clientAt = LatLon(47.52900, -52.87100),
                cottage = "Holyrood", cottageTown = "Holyrood", cottageAt = LatLon(47.38800, -53.13600), toCottage = 45, fireDept = "Holyrood Fire Department",
                propane = "Superior Propane", assessor = "City of St. John's", pension = "City of St. John's Pension Plan", pensionPrefix = "SJP", plumber = "Avalon Plumbing",
                swimming = "The Aquarena", tournamentPark = "Bannerman Park", gamePark = "King George V Park", lunch = "Rocket Bakery", poolStore = "Avalon Pool & Spa",
                energyRebate = "takeCHARGE insulation rebate", energyRebateRef = "TC-77102", plate1 = "KMR 482", plate2 = "PXM 219",
                fuelPrice = "1.629", kwhPrice = "0.140", propertyTax = "3610.00", roll = "Account 0123456",
            ),
            Province.YT to Local(
                city = "Whitehorse", area = "867", home = LatLon(60.72100, -135.05700), neighbourhood = "Riverdale", mainStreet = "Main Street", street2 = "Fourth Avenue",
                street3 = "Second Avenue", bankStreet = "Main St", school = "Selkirk Elementary School",
                power = "ATCO Electric Yukon", powerPhone = "1 800 555-0164", powerWeb = "atcoelectricyukon.com", gas = null, internet = "Northwestel",
                grocer = "Real Canadian Superstore", foodBank = "Whitehorse Food Bank", transit = "Whitehorse Transit", foundation = "Yukon Hospital Foundation",
                hospital = "Whitehorse General Hospital", specialist = "Dawson City Community Hospital", specialistKm = 530, specialistAt = LatLon(64.06000, -139.43000),
                client1 = "Mount Lorne", client1Km = 40, client2 = "Carcross", client2Km = 74, clientAddress = "Annie Lake Road, Mount Lorne", clientAt = LatLon(60.45000, -134.85000),
                cottage = "Marsh Lake", cottageTown = "Marsh Lake", cottageAt = LatLon(60.53000, -134.32000), toCottage = 50, fireDept = "Marsh Lake Fire Department",
                propane = "Superior Propane", assessor = "Government of Yukon", pension = "City of Whitehorse Pension Plan", pensionPrefix = "WHP", plumber = "Yukon Plumbing",
                swimming = "Canada Games Centre", tournamentPark = "Rotary Peace Park", gamePark = "F.H. Collins fields", lunch = "Baked Café", poolStore = "Northern Pool & Spa",
                energyRebate = "Good Energy rebate (Yukon)", energyRebateRef = "GE-77102", plate1 = "BKM48", plate2 = "BPX21",
                fuelPrice = "1.789", kwhPrice = "0.190", propertyTax = "2940.00", roll = "Roll 104-2234",
            ),
            Province.NT to Local(
                city = "Yellowknife", area = "867", home = LatLon(62.45400, -114.37200), neighbourhood = "Old Town", mainStreet = "Franklin Avenue", street2 = "50th Street",
                street3 = "Old Airport Road", bankStreet = "Franklin Ave", school = "N.J. Macpherson School",
                power = "Northland Utilities", powerPhone = "1 800 555-0164", powerWeb = "northlandutilities.com", gas = null, internet = "Northwestel",
                grocer = "Independent Grocer", foodBank = "Food Rescue Yellowknife", transit = "Yellowknife Transit", foundation = "Stanton Territorial Hospital Foundation",
                hospital = "Stanton Territorial Hospital", specialist = "Hay River Regional Health Centre", specialistKm = 480, specialistAt = LatLon(60.81600, -115.78500),
                client1 = "Dettah", client1Km = 25, client2 = "Behchoko", client2Km = 100, clientAddress = "Dettah Road, Dettah", clientAt = LatLon(62.41200, -114.30500),
                cottage = "Prelude Lake", cottageTown = "Ingraham Trail", cottageAt = LatLon(62.56000, -113.99000), toCottage = 30,
                fireDept = "Ingraham Trail Volunteer Fire Department", propane = "Superior Propane", assessor = "City of Yellowknife",
                pension = "City of Yellowknife Pension Plan", pensionPrefix = "YKP", plumber = "Northern Plumbing", swimming = "Ruth Inch Memorial Pool",
                tournamentPark = "Somba K'e Park", gamePark = "Fritz Theil Park", lunch = "Wildcat Cafe", poolStore = "Northern Pool & Spa",
                energyRebate = "Arctic Energy Alliance rebate", energyRebateRef = "AEA-77102", plate1 = "KMR482", plate2 = "PXM219",
                fuelPrice = "1.799", kwhPrice = "0.380", propertyTax = "3420.00", roll = "Roll 3021-0045",
            ),
            Province.NU to Local(
                city = "Iqaluit", area = "867", home = LatLon(63.74700, -68.51700), neighbourhood = "Happy Valley", mainStreet = "Federal Road", street2 = "Queen Elizabeth Way",
                street3 = "Ring Road", bankStreet = "Queen Elizabeth Way", school = "Nakasuk School",
                power = "Qulliq Energy", powerPhone = "1 800 555-0164", powerWeb = "qec.nu.ca", gas = null, internet = "Northwestel",
                grocer = "Northmart", foodBank = "Qajuqturvik Food Centre", transit = null, foundation = "Arctic Children and Youth Foundation",
                hospital = "Qikiqtani General Hospital", specialist = "Qikiqtani General Hospital", specialistKm = 4, specialistAt = LatLon(63.75100, -68.52200),
                client1 = "Apex", client1Km = 5, client2 = "Iqaluit", client2Km = 3, clientAddress = "Apex Road, Apex", clientAt = LatLon(63.73000, -68.45000),
                cottage = "Sylvia Grinnell", cottageTown = "Iqaluit", cottageAt = LatLon(63.72700, -68.59500), toCottage = 8, fireDept = "Iqaluit Fire Department",
                propane = "Uqsuq", assessor = "Government of Nunavut", pension = "City of Iqaluit Pension Plan", pensionPrefix = "IQP", plumber = "Arctic Plumbing",
                swimming = "Arnaitok Aquatic Centre", tournamentPark = "Sylvia Grinnell Territorial Park", gamePark = "Nakasuk field", lunch = "Grind & Brew",
                poolStore = "Northmart", energyRebate = null, plate1 = "KM482", plate2 = "PX219", fuelPrice = "1.759", kwhPrice = "0.300",
                propertyTax = "3010.00", roll = "Roll 0088-114",
            ),
        )
    }
}
