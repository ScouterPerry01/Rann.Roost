<!--
File path and name: docs/requirements-additions.md
Modified On Timestamp: 2026-10-25 @ 12:05 EDT
Created On Timestamp: 2026-10-02 @ 00:43 EDT
File Description: Requirements added after the SRS, by area, with the SRS's priority definitions.
Uses: The SRS ("docs/Household Finance Manager — Software Requirements Specification.md").
Used By: The development plan, ADRs, docs/plan-multi-device.md, docs/spec-real-property.md, tests and the manual.
Purpose: Keep the owner's later requests traceable by id.
-->

# Requirements added after the SRS

Requests from the owner after Phase 1 (2026-10-02). They use the SRS's priority definitions and
are delivered before Phase 2.

## Exchange rates

| ID | Requirement | Priority |
| --- | --- | --- |
| FX-07 | The user can follow currencies that no account uses (for travel, comparisons); their daily rates are downloaded too. | Should |
| FX-08 | An optional second rate source, off by default, for currencies the Bank of Canada does not publish (about 160 currencies, today's rate only), with the source's attribution shown. Bank of Canada rates are always preferred, and manual rates are never replaced. | Could |

## Calendar and events

| ID | Requirement | Priority |
| --- | --- | --- |
| CAL-01 | Appointments and events of any kind (medical, bank, garage, home, personal, other): title, date, start time or all day, duration, location, notes. | Should |
| CAL-02 | Events can repeat (same patterns as bills, BILL-02) and can be linked to a household member, an account and a health provider. | Should |
| CAL-03 | Reminders at chosen lead times (for example 1 day and 1 hour before), on the desktop now and on the phone in Phase 2. | Should |
| CAL-04 | One calendar showing events, bills, medication refills and health follow-ups, by month and as an agenda. | Should |
| CAL-05 | An occurrence can be marked done or cancelled without changing the series. | Could |
| CAL-06 | Events and health records are stored in an account group chosen by the user, so private ones are encrypted from other household users (HH-11). | Should |

## Health records

| ID | Requirement | Priority |
| --- | --- | --- |
| HLT-01 | Per household member: medications with dose, instructions, prescriber, pharmacy, prescription number, start and end dates. | Should |
| HLT-02 | Refills: last fill date, days of supply and refills remaining give the next refill date; recording a refill updates them. | Should |
| HLT-03 | Refill reminders a chosen number of days before the next refill, and a prompt to renew when no refills remain. | Should |
| HLT-04 | Conditions (diagnosed date, status: active, managed, resolved), allergies (reaction, severity) and immunizations (date, next dose due). | Should |
| HLT-05 | Medical tests with date, result, units, reference range and a follow-up date that appears in the calendar. | Should |
| HLT-06 | Medical appointments are calendar events (CAL-01) linked to the person and the provider. | Should |
| HLT-07 | A directory of health providers: doctors, dentists, pharmacies, clinics, labs, specialists. | Should |
| HLT-08 | Health records will link to medical expenses and claims (MED-06 to MED-10) when those are built in Phase 4. | Could |
| HLT-09 | A printable health summary per person (medications, conditions, allergies) for appointments and emergencies (EST-01). | Could |

Health information is an organizational aid, not medical advice (as TAX-04 for tax figures).

## Savings goals in one account

Brings forward BUD-02 (sinking funds) and BUD-03 (savings goals) from Phase 5.

| ID | Requirement | Priority |
| --- | --- | --- |
| GOAL-01 | Any number of goals per account, each with a name, target amount, optional target date and notes. | Should |
| GOAL-02 | A planned set-aside per goal (amount and schedule, such as each pay or monthly), posted automatically on schedule or entered by hand. Set-asides earmark part of the balance; no money moves. | Should |
| GOAL-03 | The account shows its balance divided into goals and an unassigned remainder, with a warning when the goals add up to more than the balance. | Should |
| GOAL-04 | Progress per goal: amount set aside, percentage, amount still needed, the set-aside needed per period to reach the target date, and whether the goal is on track. | Should |
| GOAL-05 | Spending from a goal (optionally linked to the purchase), moving money between goals, and marking a goal reached or archived. | Should |
| GOAL-06 | A history of each goal's set-asides, spending and moves. | Could |

## Pets

| ID | Requirement | Priority |
| --- | --- | --- |
| PET-01 | Pet records: name, species, breed, sex, date of birth, colour, microchip number, spayed or neutered, photo (Phase 2 vault), notes, archived when no longer in the household. | Should |
| PET-02 | Municipal licence: number, issuing municipality, expiry date, with a renewal reminder; pet insurance: insurer, policy number, renewal date. | Should |
| PET-03 | Pets have the same health records as people (HLT-01 to HLT-05): vaccines with next dose due, medications and refills, conditions, allergies, tests. | Should |
| PET-04 | Veterinarians, groomers and kennels in the provider directory (HLT-07); vet, grooming and boarding appointments in the calendar (CAL-01). | Should |
| PET-05 | Transactions can be recorded for a pet, as for a person; cost per pet per year by category (food, vet, grooming, licence, insurance, boarding, supplies). | Should |

## Vehicles

The vehicle part of section 11 (AST, WAR, INS, MNT), brought forward from Phase 4.

| ID | Requirement | Priority |
| --- | --- | --- |
| VEH-01 | Vehicle records: name, make, model, year, trim, colour, VIN, licence plate, fuel type, main driver, purchase date, price, seller and odometer; status active, sold or retired, with the disposal date and price (AST-01, AST-05). | Should |
| VEH-02 | Registration (SAAQ) renewal date and insurance (insurer, policy number, renewal date) with reminders (INS-03). | Should |
| VEH-03 | Warranties per vehicle: kind, provider, end date and kilometre limit, with an expiry reminder 60 days before (WAR-01, WAR-02). | Should |
| VEH-04 | Odometer readings entered directly or taken from fuel and service entries; the average distance per day is used to forecast kilometre-based maintenance (MNT-03). | Should |
| VEH-05 | Maintenance tasks scheduled by time, by distance or both, from starter templates (oil, tire rotation, seasonal tire swap, brakes, air filters, inspection) or custom (MNT-01, MNT-02). | Should |
| VEH-06 | Service log: date, odometer, tasks done, provider or do-it-yourself, cost, notes; completing a task restarts its schedule (MNT-04). | Should |
| VEH-07 | Fuel and charging log: date, odometer, litres or kWh, cost, full tank; consumption in L/100 km (or kWh/100 km) and cost per km (MNT-07). | Should |
| VEH-08 | Fuel and service entries can create the matching transaction in an account, linked to the vehicle. | Should |
| VEH-09 | Any transaction can be linked to a vehicle. | Should |
| VEH-10 | Cost of ownership per vehicle and year by category, and cost per km (MNT-06). Logbook entries without a linked transaction are included so nothing is missed or counted twice. | Should |
| VEH-11 | Due maintenance, renewals and warranty expiries appear in the reminders and the calendar (MNT-05). | Should |

## Maintenance for every asset

Details of section 11's maintenance requirements (MNT-01 to MNT-06) as built in Phase 4c
(2026-10-03), beyond what the SRS states.

| ID | Requirement | Priority |
| --- | --- | --- |
| MNT-10 | An asset other than a vehicle may have a meter that counts engine hours or kilometres (an RV); its tasks can then repeat by use as well as by months, whichever comes first. | Should |
| MNT-11 | Seasonal tasks (spring opening, winterization, tire swaps) come back every year on their date. | Should |
| MNT-12 | The phone's meter form lists metered assets beside vehicles, in their own unit; the reading goes straight to the asset (MNT-03). | Should |
| MNT-13 | Cost of ownership (MNT-06) adds each asset's share of the premiums of the policies that name it, split evenly between the things a policy covers; the purchase price is shown apart, not as a running cost. | Could |
| MNT-14 | Maintenance due this month or overdue, on vehicles and other assets, is one list: on the desktop, in the reminders and calendar, and on the phone with a notification when a task becomes due soon or due (MNT-05). | Should |

## Categories

| ID | Requirement | Priority |
| --- | --- | --- |
| CAT-06 | Public transit is split into Transit passes and Fares and tickets; Pets gains Licence, Insurance and Boarding. Existing households receive the new categories when upgraded. | Should |

## Provinces and territories

Requested on 2026-10-02, during Phase 3: full support for every province and territory, not only
Quebec. Built before Phase 3d.

| ID | Requirement | Priority |
| --- | --- | --- |
| PROV-01 | The household has a province or territory, chosen when it is created (existing households are in Quebec) and changeable later; a person who lives or files elsewhere can have their own. | Must |
| PROV-02 | Bank holidays follow the province or territory for "last business day" bills: Family Day and its equivalents, the Civic Holiday, Fête nationale in Quebec, the territorial days. | Must |
| PROV-03 | Default categories follow the province: Quebec-only ones (school taxes, Quebec Family Allowance, solidarity credit, EI/QPIP) only in Quebec; Employment Insurance and provincial benefits elsewhere. Changing province adds the new province's categories and removes none. | Should |
| PROV-04 | Provincial RESP grants follow the beneficiary's province: the QESI in Quebec, the B.C. Training and Education Savings Grant in British Columbia; none where the province has none. | Should |
| PROV-05 | Each LIRA and LIF records the law it answers to (a province, or federal), by default the holder's province; the LIF maximum follows it (none in Saskatchewan and Prince Edward Island). | Must |
| PROV-06 | Pension and tax wording follow the province: the QPP offered first in Quebec and the CPP elsewhere; Quebec forms (TP-21.4.39, RL slips) named only for Quebec. | Should |
| PROV-07 | Later phases use the province too: provincial medical expense credit totals (MED-14) and provincial slips (RL-3, RL-16 in Quebec) in the tax and medical reports. | Should |

Not covered yet: unlocking rules for locked-in plans (age 55 or 65, small balances, financial
hardship), which differ by jurisdiction and are left to the institution; provincial grants that are
suspended (Saskatchewan's SAGES).

## Updates

Details of SEC-08 and DIST-05 as built in Phase 4d (2026-10-03), beyond what the SRS states
(ADR 0008).

| ID | Requirement | Priority |
| --- | --- | --- |
| UPD-01 | A copy that can check for updates asks once, on first start, whether to check once a day, saying what GitHub learns; nothing is requested before the answer. The choice can be changed later (About on the desktop, Settings on the phone). | Must |
| UPD-02 | Only copies no store updates check: Linux .deb, .rpm and AppImage, and the Android build from GitHub Releases. The Microsoft Store, Flathub and Google Play builds never check. | Must |
| UPD-03 | An update is offered only from a list signed with RANN's release key, and a download is kept only if it matches the signed size and SHA-256; anything else is refused and deleted. | Must |
| UPD-04 | Every release file has a minisign signature and is listed in a signed SHA256SUMS, so a download can also be checked by hand. | Should |

## Navigation

Owner's request (2026-10-04), to be built in Phase 5g with the other usability work.

| ID | Requirement | Priority |
| --- | --- | --- |
| NAV-01 | The desktop's sections are grouped under a few headings instead of one long list, with reference lists and setup (categories, payees, rules, institutions, phones, AI reading, users, backups, security, About) under Settings. | Should |
| NAV-02 | Each user chooses the menu's place: a list on the left with collapsible groups, or a menu bar at the top with drop-down menus. The choice is remembered on that computer. | Should |
| NAV-03 | Both menus show the same groups and the same counts (such as documents waiting for review), and work from the keyboard (NFR-08). | Should |

## Contacts

Owner's request (2026-10-05): one place for the people and organizations the household deals with, linked to the rest of the app, standing on its own in the menu (not under Settings), with several of a kind told apart.

| ID | Requirement | Priority |
| --- | --- | --- |
| CON-01 | A contact is an organization or a person (who can work at an organization), with one or more kinds (bank, investment firm, insurer, pharmacy, family doctor, specialist, lawyer, contractor, employer, school, utility, government...), a "What for" line written by the user, and the household members it serves. | Must |
| CON-02 | A contact keeps labelled phones and emails, account or client numbers (masked; showing one asks for the password and is logged), address, website, hours and notes. | Must |
| CON-03 | Contacts are kept in an account group like health records: the household's shared group by default, or a private group chosen when adding one. | Must |
| CON-04 | A contact is linked with a role to records of the app (accounts, loans, investments, policies, health providers, medications, appointments, bills, payees, pets, vehicles, assets, contractors or one of their jobs, estate papers); its page lists them by role, and each record shows and picks its contacts. | Must |
| CON-05 | Contacts are filtered by kind, by the person served and by what they are linked to, and found by the global search. | Should |
| CON-06 | Contacts can be gathered from the records that already hold contact details, and two contacts merged, likely duplicates proposed, nothing merged or moved without the user's confirmation. | Should |
| CON-07 | The phone shows the contacts its user can see (never account or client numbers), with tap to call, email or map, and can send new contacts to the desktop, where they are reviewed before being added. | Should |

## Calendar views, schedules and activities

Owner's request (2026-10-06), before 1.0.

| ID | Requirement | Priority |
| --- | --- | --- |
| CAL-07 | The calendar has year, month, week and day views beside the agenda, with today, previous and next, and a date picker. | Should |
| CAL-08 | Each user chooses what the calendar shows: each kind (appointments and events, bills, health, maintenance and seasonal tasks, renewals, schedules, imported calendars) and each person; the choice is remembered per user on that computer. | Should |
| CAL-09 | Work and school schedules per person: days of the week with start and end times, repeating weekly or on a rotation of several weeks (shift work), with exceptions (holidays, sick days, professional development days) and the province's bank holidays off when chosen. | Should |
| CAL-10 | Each day shows a thin bar per person with a schedule that day (name, work or school, start and end times); week and day views also shade the scheduled hours. | Should |
| CAL-11 | Children's activities (practices, games, lessons) are events for the child, with who drives each way (carpool) and the cost, which can become a transaction. | Could |

## Calendars from each person's own accounts

| ID | Requirement | Priority |
| --- | --- | --- |
| CSY-01 | On the phone, with the user's permission, the calendars Android already syncs (Google, Outlook or Exchange, Samsung and others) can be read; the user picks which calendars to bring in. The app never signs in to a calendar account itself. | Should |
| CSY-02 | Upcoming items of the chosen calendars (a set number of days ahead) are sent to the desktop with the phone's other transfers, encrypted end to end, and updated or removed when they change on the phone. | Should |
| CSY-03 | Each brought-in calendar, and any single item, is private (only its owner sees it), busy only (others see the person busy at those times, without details) or shared (details visible to those who can see the chosen account group). Private by default. | Should |
| CSY-04 | Brought-in items show in the calendar as read-only, marked with their source; reminders are left to the calendar they come from. | Should |
| CSY-05 | iCalendar (.ics) files can be imported on the desktop as a one-time copy. | Could |

## Seasonal checklists

| ID | Requirement | Priority |
| --- | --- | --- |
| SEA-01 | Pool and yard (and garden) are kinds of asset, with starter tasks (pool opening and closing, chemicals, pump and filter; lawn, irrigation shut-off and start-up, outside taps, snow blower, lawn mower), beside fuller seasonal starter tasks for homes, cottages and vehicles (outside taps, eavestroughs, air conditioner cover, wiper blades, block heater, emergency kit). | Should |
| SEA-02 | A seasonal checklist per change of season (spring, summer, fall, winter): every task of that season across vehicles, homes, cottages, pools, yards and other assets, with its due date, ticked off one by one; ticking records the service in the log. | Should |
| SEA-03 | Seasons start on dates the household can change (Rates and rules), with Quebec's winter tire dates kept. | Should |
| SEA-04 | The seasonal checklist is on the phone too; ticks made there reach the desktop. | Should |
| SEA-05 | Renovations and energy upgrades (insulation, heat pump, windows, solar) recorded as home projects with their receipts, the rebates and grants applied for and received, and their effect on the home's cost base. | Could |

## Trips and vehicles on the phone

| ID | Requirement | Priority |
| --- | --- | --- |
| TRP-01 | A trip is started and ended on the phone: vehicle, driver, date and time, odometer and place at each end; the phone takes one location fix at Start and at Arrive only (no background tracking), with the user's permission. | Should |
| TRP-02 | Saved places: name, category (home, work, client, store, fuel, garage, medical, other), address and a matching radius; a location fix is matched to the nearest saved place, or the user names a new one. Places are kept on the user's devices only and never sent to a map service. | Should |
| TRP-03 | Distance comes from the odometer readings (the CRA's basis); time travelled from the two times. The purpose (business, employment, medical, personal) is suggested from the place and confirmed. | Should |
| TRP-04 | A trip records whether the vehicle tows a trailer (which one) or carries a heavy load, and the passengers. | Should |
| TRP-05 | Fuel and charging are entered on the phone too (litres or kWh, cost, full or partial, station as a saved place); consumption (L/100 km or kWh/100 km) is shown separately for normal driving, towing and heavy loads. | Should |
| TRP-06 | Trips reach the desktop's trip log and odometer readings, so distance-based maintenance is forecast from real driving; the CRA business-use log and medical travel use them. | Should |
| TRP-07 | Vehicle details add engine, transmission, drive, fuel tank or battery capacity, tire sizes (summer and winter), oil type and capacity, towing capacity and gross vehicle weight, and personal or commercial use. | Should |
| TRP-08 | Vehicle budget forecast: fuel or energy and maintenance for the coming months from the recent driving, consumption by kind of trip and prices. | Should |
| TRP-09 | For commercial vehicles: a logbook export the CRA accepts, kilometres per province or state (for fuel tax reports such as IFTA) and inspection reminders (annual safety inspection, CVOR or NSC renewals). | Could |
| TRP-10 | EV charging: home or public, kWh and cost, cost per km compared with fuel. | Should |

## Other trackers

| ID | Requirement | Priority |
| --- | --- | --- |
| UTL-01 | Utility meters (electricity, natural gas, water) per property: readings entered on the phone or the desktop, use per month compared with the same month last year, unusual use flagged. | Should |
| UTL-02 | Fuel tanks (propane, heating oil): level readings and deliveries, use per month, and a reminder to order before the tank is low. | Should |
| HRS-01 | Hours worked for side income, per client and task, started and stopped on the phone or entered on the desktop; unbilled hours become invoice lines (SAL-04). | Should |
| CHO-01 | Chores per child, ticked on the phone, earning amounts that feed the child's allowance and money (HH-03). | Could |
| VOL-01 | Volunteer hours per person and organization (volunteer firefighter and search and rescue tax credits need 200 hours; students' community service hours), with a yearly total. | Could |

## From the owner's real-phone test (2026-10-07)

| ID | Requirement | Priority |
| --- | --- | --- |
| TRP-11 | A trip keeps the GPS position of its departure, each stop and its arrival, and an address for each: from a saved place, typed, or looked up from the position (opt-in, see TRP-17). | Should |
| TRP-12 | A trip can have several stops: each stop records its arrival time, odometer and place, then the trip continues to the next; a single-destination trip stays one tap. | Should |
| TRP-13 | An optional planned destination at the start (a saved place, a typed address, home or the previous stop), which can be changed or cancelled during the trip. | Should |
| TRP-14 | Arriving offers: back home, back to the previous stop, or a new place. | Should |
| TRP-15 | Rest breaks during a trip, started and ended with one tap (time and position), left out of driving time and shown in the trip log. | Should |
| TRP-16 | A note dictated or recorded (voice) and photos of the location can be added to a trip or a stop; they reach the computer with the trip. | Should |
| TRP-17 | Address lookup from a GPS position through Android's geocoder (Google's service), off by default and turned on in Settings; the privacy policy says what is sent. | Should |
| TRP-18 | Nearby fuel stations and EV chargers, closest first, from OpenStreetMap (no account), at any time (before or during a trip, from the fuel form); off by default, sends the rough position to OpenStreetMap only when asked; a station picked fills the form and can be saved as a place. | Should |
| TRP-19 | A station can be added by hand on the phone (name, address, kind), with the current position filled in. | Should |
| SEC-09 | The phone app's lock time is chosen in Settings: immediately, after 1, 5 or 15 minutes. | Should |
| CSY-06 | Calendar sync on the phone is chosen in Settings: off, bring in only (default), or both ways. Both ways also writes the household's appointments, schedules and bills the user may see into a calendar the user chooses: a RANN's Roost calendar kept only on the phone, or a calendar of one of the phone's accounts. Items written are removed when the option is turned off or the phone is unpaired. | Should |
| CAL-12 | The phone's agenda is reached from a calendar icon at the top of the Capture and Summary tabs. | Should |
| DOC-01 | The document viewer on the computer shows every page of a document (page by page) and can zoom in, out and back to fit. | Should |
| DOC-02 | A receipt or invoice can be itemized by hand in the review dialog (item, amount, category, sales tax), creating the transaction's split lines without AI reading. | Should |

## Bills: classification and statements (owner's request, 2026-10-07)

Source: `docs/Bill-Modifications.md` (the owner's list).

| ID | Requirement | Priority |
| --- | --- | --- |
| BILL-13 | Every bill has a type (Home or Business), a category and a subcategory from the owner's lists in `docs/Bill-Modifications.md`, shipped in English and French; each subcategory has a default spending category used for the bill's payments. | Should |
| BILL-14 | The bill classification lists can be edited in Settings (rename, add, hide subcategories), like spending categories; the built-in lists stay the default. | Should |
| BILL-15 | A bill keeps its account number with the company (masked like account numbers elsewhere). | Should |
| BILL-16 | Each statement received for a bill is kept: statement number, issued date, due date, amount, and the scanned document; the Bills screen lists a bill's statements, and a due date from a statement becomes that occurrence's due date. | Should |
| BILL-17 | A utility bill's statement also keeps the previous and current meter readings with their dates and the amount used; a utility bill can be linked to a meter on the Utilities screen (UTL-01), and each statement's readings are added to that meter. | Should |
| BILL-18 | A captured document of kind Bill that matches no bill offers to create one, prefilled from what was read (payee, account number, amount, due date, a classification guessed from the payee), or to attach it to an existing bill; the statement is recorded either way. | Should |
| BILL-19 | Reading a bill (on the device and with AI reading) fills the account number, statement number, issued date, due date, amount and, for utilities, the meter readings and their dates when the bill shows them. | Should |
| BILL-20 | A Business bill names the person it belongs to; its payments count as that person's business expenses, with the sales taxes paid on them, in the year-end package. | Should |

## Bills: amounts paid, instalments, column headings (owner's request, 2026-10-07)

| ID | Requirement | Priority |
| --- | --- | --- |
| BILL-21 | The To pay and All bills lists have a heading row: due date, bill, amount due, to pay, outstanding, actions. To pay starts at the amount still due and can be changed; outstanding is the amount due less what has been paid. | Should |
| BILL-22 | A bill can be paid in part: the rest stays due on the same occurrence, shown as outstanding, with reminders, until paid in full; each payment is recorded and linked. | Should |
| BILL-23 | A statement can list instalments on set dates (date and amount each), for a yearly tax bill or any bill paid in instalments; each instalment is a due date of the bill with reminders. Home and Business alike. | Should |
| BILL-24 | Instalments roll over: until next year's statement is entered, next year's instalments are proposed on the same dates (adjusted to business days) with this year's amounts, marked as estimated. | Should |
| BILL-25 | Reading a tax bill (on the device and with AI reading) finds its instalment dates and amounts when printed. | Should |
| NAV-04 | Every list or table with columns has a heading row naming each column; the column of buttons is headed Actions. | Should |

## Help menu, About and Walk-Me guides (owner's request, 2026-10-07)

| ID | Requirement | Priority |
| --- | --- | --- |
| HLP-01 | A Help group in the menu (the last group of the side list; a Help menu at the right of the top menu bar) with Manual, Help (the short help for the screen shown), Walk-Me and About. The top bar's Help and Manual buttons and F1 / Shift+F1 stay. | Should |
| HLP-02 | About shows the version and updates, what's new in this version (the release notes), the privacy summary and policy, the licence and brand notice, support contact, third-party notices, and a button that copies details for support (version, operating system, Java, database versions; no personal data). | Should |
| HLP-03 | Walk-Me: step-by-step guides for the common tasks, chosen from a Walk-Me menu. A guide opens as a small window docked at the side (not blocking the app); each step says where to go, what to select and what to enter, highlights the control on screen, has Show me (opens the right screen), Back and Next, moves on by itself when the step is done where that can be seen, and links to the manual section. | Should |
| HLP-04 | Walk-Me guides in English and French for: household creation; institutions and accounts; members and users; importing a statement or Quicken file; reconciling; backups; bills; municipal property taxes; paying a bill in part; budgets; savings goals; pairing a phone; capturing and processing a receipt; capturing and processing a bill; itemizing; AI reading; mortgages; loans and lines of credit; investment accounts and holdings; registered plans; vehicles and maintenance; trips on the phone; the seasonal checklist; utilities and meters; medical expenses and claims; insurance; contacts; family money and allowances; the year-end tax package; the income tax estimate; custom reports; the emergency and estate summary. Guides are content files that can be extended. | Should |

## Several computers per household (owner's request, 2026-10-25; release 2.0.0)

Decided in ADR 0011; built as described in `docs/plan-multi-device.md`.

| ID | Requirement | Priority |
| --- | --- | --- |
| MD-01 | A household can be used on several computers. Each computer keeps its own full copy on its own disk, works offline, and comes back in step with the others by itself. Sharing is opt-in; a household on one computer works as before. | Must |
| MD-02 | Computers exchange changes through a folder the family already has (OneDrive, Dropbox, Google Drive, Nextcloud, a network share, or a USB stick carried between computers). Each computer writes only its own files and never changes a file once written. | Must |
| MD-03 | Computers on the same home network also exchange changes directly over Wi-Fi: they find each other on the network, and the first contact between two computers is approved once on both screens with a code. The folder and Wi-Fi can be used together. | Must |
| MD-04 | A new computer joins a household by pointing to the shared folder or finding a computer on Wi-Fi, then signing in with the user's own password. It receives only the groups that user can open; a second user signing in later receives theirs. A child's or other restricted user's own computer therefore holds only the groups granted to that user. | Must |
| MD-05 | Everything exchanged, in the folder or over Wi-Fi, is encrypted with the group's key and signed by the user who made the change. Private groups stay unreadable to anyone without their key (HH-11). | Must |
| MD-06 | Changes made at the same time on different computers are merged field by field: edits to different fields are all kept. When the same money field (amount, date, account, splits, budget amount, loan terms) was changed on two computers before they saw each other's change, the latest is kept and the other is listed in Changes to review with Keep, Use the other and Open. | Must |
| MD-07 | A deletion is never undone by an older change from another computer. Anything added offline to a record deleted elsewhere is kept in Changes to review with Restore and Discard, never silently lost. | Must |
| MD-08 | The same statement or file imported on two computers is recognised: the second copy is listed in Changes to review as imported twice. The same tag or payee created on two computers is merged into one. | Should |
| MD-09 | Things the app does by itself (posting goal contributions and loan payments, bill and event occurrences, scheduled reports and e-mails, price and rate downloads) happen once, not once per computer. Scheduled reports and downloads run on the main computer for automatic jobs: the household's first computer, unless the role is transferred to another computer, or taken over by an administrator when the main computer has problems (with a prompt when it has not been seen for 3 days). A former main computer that comes back stops its jobs. | Must |
| MD-10 | A computer accepts a change only if its author had the right to make it (edit permission on that group; administrators only for users, groups, permissions and household settings). Refused changes are shown to administrators. | Must |
| MD-11 | A Computers screen lists each computer, its users, app version, when it was last seen and how it connects. An administrator can remove a computer, and rotate a group's key so a removed computer can no longer read new data. | Must |
| MD-12 | The top bar shows the sync state: up to date, sending, not synced for a number of days, or offline. The history shows who made each change and on which computer. | Should |
| MD-13 | A computer with an older version keeps working and holds changes made by a newer version, with a message asking to update it. | Must |
| MD-14 | Existing households upgrade in place: 2.0.0 opens a household folder wherever the user points (local disk, external drive, network share, cloud folder) or restores a 1.x backup, makes a full backup first, then converts it. A household found in a cloud or network folder is moved to the computer's own disk (the old files kept aside, never deleted) and that folder becomes the shared folder. A household copy is never opened by two computers at once. | Must |
| MD-15 | A phone stays paired with the computer it was paired with and syncs with that computer only. One computer can have several phones, each paired under a separate user account; a phone syncs while its user is signed in on that computer. What the phone captures reaches the other computers like any other change. | Must |
| MD-16 | Documents are copied to every computer by default. A computer can be set to keep documents only on the main computer, opening them from there when it is reachable. | Should |

## Real-property details (owner's request, 2026-10-25; release 2.0.0)

Fields, choice lists and tables are specified in `docs/spec-real-property.md`. Every field is optional.

| ID | Requirement | Priority |
| --- | --- | --- |
| AST-06 | New asset kinds Condo, Land and Building (multi-unit, commercial, farm and other buildings) beside Home and Cottage. These five kinds have a Property section, and any detail not listed can be added as a custom field (label and value). | Must |
| AST-07 | Identification and use: property type, building style, use (principal residence, seasonal, rental…), occupancy, how it was acquired; and the civic address in Canadian form (unit, number, street, municipality, county or region, province or territory, postal code checked as A1A 1A1, country), with the rural civic number, map coordinates entered by hand, and directions. | Must |
| AST-08 | Legal identifiers and title: assessment roll number (with the province's format as a hint), parcel identifier (PIN, PID, title or LINC, Quebec cadastre lot), lot, plan, concession, block, part, geographic township, survey description (section–township–range–meridian), full legal description, title or instrument number, registry office, tenure, lease end date, survey, easements and restrictions. | Must |
| AST-09 | Land and terrain: lot area, frontage and depth, shape, topography, soil, drainage, vegetation, waterfront and shoreline, water frontage and depth, view, access and road maintenance, fencing, zoning, land designations (agricultural reserve, greenbelt, flood plain, conservation, heritage…) and farm land use. | Should |
| AST-10 | Building: year built (or estimated), builder, renovations, storeys and floors, areas above and below grade, ceiling height, units, bedrooms, bathrooms and kitchens, construction type, foundation, exterior finish, roof shape, material and year, windows and doors, insulation, EnerGuide rating, interior walls and flooring, fireplaces, accessibility, known hazardous materials and radon; basement type, finish, height, entrance, suite, waterproofing, sump pump, backwater valve and past water damage; garage, driveway and parking. | Should |
| AST-11 | Rooms: a list of rooms with type, name, level, dimensions and area, flooring, ceiling height, features and condition, with photos; the home inventory can be grouped by room. | Should |
| AST-12 | Systems: heating, cooling, water heating, ventilation, electrical, plumbing, water supply and treatment, sewage, fireplaces and stoves, solar, generator, EV charger, security and fire protection, with type, fuel, make and model, capacity, installation year, last inspection (WETT, septic, well water), and whether owned or rented; each can link to its own asset for maintenance and warranty. | Should |
| AST-13 | Outbuildings and features: other buildings and fixtures on the land (garage, shed, barn, bunkie, boathouse, dock, sauna, greenhouse…), the pool, hot tub, decks and other outdoor and interior features; and for condos and co-ops, the corporation, legal unit, monthly fee and what it includes, parking stall, locker, exposure, amenities, property manager, reserve fund study, special assessments and bylaw notes. | Should |
| AST-14 | Value and tax: assessed value (land and building), assessment year and authority, tax account number, annual property tax linked to the property-tax bill, school support, insured replacement cost, market value estimates, and the linked mortgage or line of credit with the equity shown. | Must |
| AST-15 | Owners: title holders among the household members with their share and form of tenancy (joint tenancy, tenants in common…); shares must total 100 % when given; used by net worth per person and the estate summary. | Should |
| AST-16 | A printable property fact sheet (print and PDF) with every filled-in field, with the choice to leave out legal identifiers and owners. Rentals link to their property and show its address; the estate summary and home inventory show the address, roll number and parcel identifier. | Should |
| AST-17 | Years designated as principal residence per property and member, on every real-property asset (home, cottage, condo, land, building), as an aid for CRA form T2091, with a warning when two properties are designated for the same year; marked as an aid, not tax advice. | Should |
