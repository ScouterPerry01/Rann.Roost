// File path and name: app/desktop/src/main/kotlin/ca/schippers/hfm/desktop/DemoTrips.kt
// Modified On Timestamp: 2026-10-11 @ 13:05 EDT
// Created On Timestamp: 2026-10-06 @ 09:14 EDT
// File Description: The sample household's trip log: saved places, the Civic's details and three
//   months of driving a plug-in hybrid RAV4.
// Uses: DemoPlace (local places, prices and coordinates), core/books trips and vehicles.
// Used By: DemoHousehold.
// Purpose: Show the trip log, fuel and charging features on realistic data.

package ca.schippers.hfm.desktop

import ca.schippers.hfm.books.Account
import ca.schippers.hfm.books.Asset
import ca.schippers.hfm.books.AssetKind
import ca.schippers.hfm.books.Books
import ca.schippers.hfm.books.Charging
import ca.schippers.hfm.books.DriveKind
import ca.schippers.hfm.books.Energy
import ca.schippers.hfm.books.FuelEntry
import ca.schippers.hfm.books.FuelType
import ca.schippers.hfm.books.Member
import ca.schippers.hfm.books.PaymentDraft
import ca.schippers.hfm.books.Place
import ca.schippers.hfm.books.PlaceCategory
import ca.schippers.hfm.books.Transmission
import ca.schippers.hfm.books.Trip
import ca.schippers.hfm.books.TripLoad
import ca.schippers.hfm.books.TripPurpose
import ca.schippers.hfm.books.TripStop
import ca.schippers.hfm.books.TripStopKind
import ca.schippers.hfm.books.Vehicle
import ca.schippers.hfm.books.VehicleDetails
import ca.schippers.hfm.books.VehicleUsage
import ca.schippers.hfm.money.Currency
import ca.schippers.hfm.money.Money
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * TRP-01 to TRP-10 in the sample household: saved places, the Civic's details, and a plug-in hybrid
 * RAV4 driven for three months: commutes, a weekend at the cottage towing the utility trailer, home
 * and public charging, and fill-ups before and after the towing. The medical trip gets its times.
 */
internal class DemoTrips(private val books: Books, private val p: DemoPlace) {

    private fun l(fr: String, en: String) = p.l(fr, en)

    private fun q(qc: String, other: String) = p.q(qc, other)

    private fun cad(s: String) = Money.parse(s, Currency.CAD)

    private fun cat(key: String) = books.categories.list().first { it.systemKey == key }.id

    fun add(group: String, visa: Account, alex: Member, sam: Member, lea: Member, civic: Vehicle, today: LocalDate) {
        val province = p.province.name
        val t = p.t
        fun place(name: String, category: PlaceCategory, qc: LatLon, other: LatLon, address: String? = null, radius: Int = 150): Place {
            val at = if (p.quebec) qc else other
            return books.places.save(Place("", group, name, category, address, at.lat, at.lon, radius, province))
        }
        val street = q("rue des Érables", l("rue Maple", "Maple Street"))
        val home = place(
            l("Maison", "Home") + " ($street)", PlaceCategory.HOME, LatLon(46.80400, -71.24200), t.home,
            q("118, rue des Érables, Québec", "48 $street, ${p.city}"),
        )
        val work = place(l("Bureau", "Office") + " (" + q("boul. Laurier", t.mainStreet) + ")", PlaceCategory.WORK, LatLon(46.77700, -71.28100), t.work, radius = 200)
        val cottage = place(p.cottage, PlaceCategory.OTHER, LatLon(47.65800, -72.27000), t.cottageAt, radius = 300)
        place(p.specialist, PlaceCategory.MEDICAL, LatLon(45.57450, -73.57950), t.specialistAt)
        val station = place("Petro-Canada (" + q("ch. Sainte-Foy", t.mainStreet) + ")", PlaceCategory.FUEL, LatLon(46.78800, -71.26200), t.station)
        place(q("Garage Tremblay", "Main Street Auto"), PlaceCategory.GARAGE, LatLon(46.81000, -71.23000), t.garage)
        val client = place(p.client1, PlaceCategory.CLIENT, LatLon(46.80300, -71.17800), t.clientAt, q("5500, boul. Guillaume-Couture, Lévis", t.clientAddress))
        // The phone's user is Alex, proposed as the driver on the phone.
        runCatching { books.users.list().firstOrNull { it.isMe }?.let { books.users.update(it.id, it.displayName, alex.id) } }

        // TRP-07: the Civic's details; Sam also drives it for clients.
        books.vehicles.save(
            books.vehicles.get(civic.id).copy(
                details = VehicleDetails(l("2,0 L 4 cylindres", "2.0 L 4-cylinder"), Transmission.CVT, DriveKind.FWD, BigDecimal("47"), null, "215/50R17", "205/55R16", "0W-20", BigDecimal("4.4")),
                usage = VehicleUsage.MIXED,
            ),
        )
        val trailer = books.assets.save(
            Asset("", group, AssetKind.TRAILER, l("Remorque utilitaire 5 x 8", "Utility trailer (5 x 8)"), make = "Canadian Tire", purchaseDate = LocalDate(2022, 5, 14), purchasePrice = cad("1299.99"), location = l("Maison", "Home")),
        )
        val start = today.minus(DatePeriod(days = 90))
        val rav4 = books.vehicles.save(
            Vehicle(
                "", group, "RAV4", "Toyota", "RAV4 Prime", 2023, "SE", l("Bleu", "Blue"), "JTMEB3FV7PD123456", q("H27 PRV", t.plate2), FuelType.PLUG_IN_HYBRID, alex.id,
                LocalDate(2023, 9, 20), cad("52890"), q("Toyota Ste-Foy", t.toyota), 12, Currency.CAD, today.plus(DatePeriod(months = 4)),
                p.insurer, "AUT-5521874", today.plus(DatePeriod(days = 12)),
                details = VehicleDetails(
                    l("2,5 L hybride rechargeable", "2.5 L plug-in hybrid"), Transmission.CVT, DriveKind.AWD, BigDecimal("55"), BigDecimal("18.1"),
                    "225/60R18", "225/65R17", "0W-16", BigDecimal("4.5"), 1_134, 2_495,
                ),
            ),
        )
        books.vehicles.addReading(rav4.id, start, 18_400)

        // Three months of driving: about 30 km a day not logged, commutes on the last working days,
        // and the weekend at the cottage with the trailer, three weeks ago.
        val price = BigDecimal(q("1.689", t.fuelPrice))
        val kwhPrice = BigDecimal(q("0.078", t.kwhPrice))
        var odo = 18_400
        var sinceFill = 0.0
        val towOut = today.minus(DatePeriod(days = 23)).let { d -> d.minus(DatePeriod(days = (d.dayOfWeek.ordinal - DayOfWeek.FRIDAY.ordinal + 7) % 7)) }
        val towBack = towOut.plus(DatePeriod(days = 2))
        val commuteDays = (1..7).map { today.minus(DatePeriod(days = it)) }.filter { it.dayOfWeek.ordinal < 5 }.take(3).toSet()
        val toCottage = if (p.quebec) 178 else t.toCottage
        fun at(d: LocalDate, h: Int, m: Int) = LocalDateTime(d.year, d.month, d.day, h, m)
        fun fill(d: LocalDate) {
            val litres = BigDecimal(sinceFill).setScale(1, RoundingMode.HALF_UP).takeIf { it >= BigDecimal(5) } ?: return
            books.vehicles.saveFuel(
                FuelEntry("", rav4.id, d, odo, litres, Money.of(litres * price, Currency.CAD), station = station.name, placeId = station.id, energy = Energy.FUEL),
                PaymentDraft(visa.id, cat("transport.fuel"), "Petro-Canada"),
            )
            sinceFill = 0.0
        }
        fun trip(d: LocalDate, from: Place, to: Place, km: Int, leave: Pair<Int, Int>, minutes: Int, purpose: TripPurpose, load: TripLoad = TripLoad.NONE, riders: String? = null) {
            val begin = at(d, leave.first, leave.second)
            val endMinutes = leave.first * 60 + leave.second + minutes
            books.trips.save(
                Trip(
                    "", group, d, to.name, BigDecimal.ZERO, false, purpose, rav4.id, alex.id, from.name, null,
                    begin, at(d, endMinutes / 60, endMinutes % 60), odo, odo + km, from.id, to.id, load, trailer.id.takeIf { load == TripLoad.TOWING }, riders, province,
                ),
            )
            odo += km
            // Plug-in hybrid: towing burns about 9.5 L/100 km; otherwise the battery covers most of the driving.
            sinceFill += km * (if (load == TripLoad.TOWING) 9.5 else 3.4) / 100.0
        }
        var day = start.plus(DatePeriod(days = 1))
        var lastFill = start
        while (day < today) {
            if (day == towOut) {
                fill(day)
                lastFill = day
                trip(day, home, cottage, toCottage, 16 to 30, 135, TripPurpose.PERSONAL, TripLoad.TOWING, l("Sam, Léa", "Sam, Maya"))
            } else if (day == towBack) {
                trip(day, cottage, home, toCottage, 14 to 45, 140, TripPurpose.PERSONAL, TripLoad.TOWING, l("Sam, Léa", "Sam, Maya"))
                fill(day)
                lastFill = day
            } else if (day == commuteDays.maxOrNull()) {
                // TRP-12, TRP-15: on the way home, a stop at the client (business) and a coffee break.
                trip(day, home, work, 22, 7 to 50, 28, TripPurpose.PERSONAL)
                books.trips.save(
                    Trip(
                        "", group, day, home.name, BigDecimal.ZERO, false, TripPurpose.PERSONAL, rav4.id, alex.id, work.name, null,
                        at(day, 15, 30), at(day, 17, 50), odo, odo + 39, work.id, home.id, province = province, deviceId = null,
                        startAddress = work.address, endAddress = home.address, startLatitude = work.latitude, startLongitude = work.longitude,
                        endLatitude = home.latitude, endLongitude = home.longitude,
                        stops = listOf(
                            TripStop("", TripStopKind.STOP, at(day, 15, 58), odometer = odo + 18, placeId = client.id, place = client.name, address = client.address, purpose = TripPurpose.BUSINESS),
                            TripStop("", TripStopKind.BREAK, at(day, 17, 5), at(day, 17, 20)),
                        ),
                    ),
                )
                odo += 39
                sinceFill += 39 * 3.4 / 100.0
            } else if (day in commuteDays) {
                trip(day, home, work, 22, 7 to 50, 28, TripPurpose.PERSONAL)
                trip(day, work, home, 22, 16 to 55, 34, TripPurpose.PERSONAL)
            } else if (day != towOut.plus(DatePeriod(days = 1))) {
                odo += 30
                sinceFill += 30 * 3.4 / 100.0
            }
            // A full tank every three weeks, a home charge every Sunday night, and one public charge.
            if (day.toEpochDays() - lastFill.toEpochDays() >= 21) {
                fill(day)
                lastFill = day
            }
            if (day.dayOfWeek == DayOfWeek.SUNDAY && day != towBack) {
                books.vehicles.saveFuel(
                    FuelEntry("", rav4.id, day, odo, BigDecimal("16.8"), Money.of(BigDecimal("16.8") * kwhPrice, Currency.CAD), energy = Energy.ELECTRICITY, charging = Charging.HOME),
                    PaymentDraft(visa.id, cat("transport.ev_charging"), p.power),
                )
            }
            if (day == towBack.minus(DatePeriod(days = 1))) {
                books.vehicles.saveFuel(
                    FuelEntry("", rav4.id, day, odo, BigDecimal("12.4"), cad("6.20"), fullTank = false, station = q("Circuit électrique (Saint-Tite)", t.charger), energy = Energy.ELECTRICITY, charging = Charging.PUBLIC),
                )
            }
            day = day.plus(DatePeriod(days = 1))
        }
        // TRP-08: its usual tasks, with an oil change and tire rotation two months ago, so the forecast prices them.
        val tasks = books.vehicles.addStarterTasks(rav4.id, start) { ca.schippers.hfm.i18n.Messages.get(books.language, "task.$it") }.associateBy { it.templateKey }
        books.vehicles.saveService(
            ca.schippers.hfm.books.ServiceRecord(
                "", rav4.id, today.minus(DatePeriod(days = 60)), 19_280, q("Toyota Ste-Foy", t.toyota), cost = cad("189.90"),
                taskIds = setOfNotNull(tasks["oil"]?.id, tasks["tire_rotation"]?.id),
            ),
            PaymentDraft(visa.id, cat("transport.maintenance"), q("Toyota Ste-Foy", t.toyota)),
        )
        // The Civic's trips entered on the computer leave from home, to the saved place of the same name;
        // MED-11: the medical trip gets its times.
        val byName = books.places.list().associateBy { it.name }
        for (t in books.trips.list(today.year) + books.trips.list(today.year - 1)) {
            if (t.vehicleId != civic.id || t.origin != null) continue
            val medical = t.purpose == TripPurpose.MEDICAL
            books.trips.save(
                t.copy(
                    origin = home.name, startPlaceId = home.id, endPlaceId = byName[t.destination]?.id,
                    startAt = if (medical) at(t.date, 7, 15) else t.startAt, endAt = if (medical) at(t.date, 16, 40) else t.endAt, passengers = if (medical) "Sam" else t.passengers,
                ),
            )
        }
    }
}
