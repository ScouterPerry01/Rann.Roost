<!--
File path and name: docs/spec-real-property.md
Modified On Timestamp: 2026-10-25 @ 12:30 EDT
Created On Timestamp: 2026-10-25 @ 11:40 EDT
File Description: Field-by-field specification of the real-property details for houses, cottages, condos, land
  and buildings (requirements AST-06 to AST-17), delivered in RANN's Roost 2.0.0.
Uses: docs/requirements-additions.md (AST-06 to AST-17); the asset model (Assets.sq, AssetService.kt,
  AssetsScreen.kt); docs/plan-multi-device.md section 9.
Used By: The developer and Claude Code when building the real-property phase of 2.0.0.
Purpose: Fix the full list of fields, choice lists and tables before the schema is written.
-->

# Real-property details: specification

**Status:** draft, approved in principle by the owner on 2026-10-25 ("collect as much information about the
property as possible"). Not built.

## 1. Scope and principles

- Applies to the asset kinds **HOME, COTTAGE** (today) and the new **CONDO, LAND, BUILDING** (multi-unit,
  commercial, farm and other buildings). The asset dialog shows a **Property** section with tabs for these
  kinds only.
- **Every field is optional.** A user can record a cottage's address only, or every detail of a house.
- **Choice lists end with Other**, which opens a free-text box. Each choice needs EN and FR texts.
- Areas and lengths are stored in **metric** (m², m) with the unit the user typed. They are shown in the
  user's preferred unit (m², ft², acres, hectares; m or ft); 1 ft² = 0.09290304 m² exactly.
- Money fields use `Money` (minor units), never `Double`.
- Things that need **maintenance** (furnace, pool, roof, septic) can link to a **child asset** (AST-02), so
  the existing maintenance, warranty and service log features work on them unchanged.
- Everything is stored in the asset's group ledger, encrypted like all other data (HH-11).
- Repeating things (rooms, systems, outbuildings, owners) are their own tables. The rest is one row per
  property.
- **Any other detail** the lists do not cover goes into custom fields (section 2.12), so nothing is ever
  "missing a field".

## 2. Fields

### 2.1 Identification and use (table `real_property`)

| Field | Type | Notes |
| --- | --- | --- |
| property_type | choice | Detached, semi-detached, townhouse / row, duplex, triplex, fourplex, apartment / condo unit, condo townhouse, mobile / manufactured home, cottage / cabin / chalet, farm, vacant land, recreational land, commercial, industrial, mixed use, multi-unit residential, Other |
| building_style | choice | Bungalow, raised bungalow, 1½-storey, 2-storey, 2½-storey, 3-storey, split-level, back-split, side-split, bi-level, A-frame, chalet, log home, timber frame, Victorian, Edwardian, Cape Cod, Colonial, Craftsman, Tudor, ranch, contemporary / modern, Quebec-style (canadienne), plex (Montréal), Other |
| use | choice | Principal residence, secondary residence, seasonal / recreational, rental, partly rented, farm, business, vacant, Other |
| occupancy | choice | Owner-occupied, tenant-occupied, mixed, vacant |
| acquired_how | choice | Purchase, new build, inheritance, gift, transfer between spouses, Other (the date and price stay on the asset) |
| nickname shown on lists | text | (the asset name already covers this; no new column) |

### 2.2 Civic address

| Field | Type | Notes |
| --- | --- | --- |
| unit | text | Apartment / suite / unit |
| street_number | text | Allows "123A", "123-125" |
| street_name | text | Including type and direction ("Main St. W") |
| address_line_2 | text | Rural route, site / box, c/o |
| municipality | text | City, town, township or village |
| county_region | text | County, regional municipality, MRC (Quebec), district |
| province | choice | The existing province and territory list |
| postal_code | text | Checked as `A1A 1A1` when the country is CA |
| country | choice | Default Canada |
| emergency_civic_number | text | 911 / rural civic number when it differs |
| latitude, longitude | decimal | Optional; entered by hand (no map service is called) |
| directions | text | Directions for visitors and contractors (cottages) |

### 2.3 Legal identifiers and title

| Field | Type | Notes |
| --- | --- | --- |
| roll_number | text | Assessment roll number. Hint per province: Ontario 19-digit ARN, Quebec matricule, B.C. roll / folio, Alberta roll, Manitoba roll, N.S. AAN, N.B. PAN, etc. |
| parcel_id | text | Ontario PIN, B.C. PID, Alberta title / LINC, Saskatchewan parcel / title number, Manitoba title, N.S. PID, N.B. PID, P.E.I. parcel, Quebec cadastre lot number |
| lot | text | |
| plan | text | Registered plan / reference plan |
| concession | text | |
| block | text | |
| part | text | "Part 2, Reference Plan 42R-…" |
| geographic_township | text | |
| survey_description | text | Section–township–range–meridian (Prairies), district lot (B.C.) |
| legal_description | long text | The full legal description as on the deed |
| title_number | text | Title or instrument number |
| registry_office | text | Land registry / land titles office |
| tenure | choice | Freehold, condominium (strata), co-operative, leasehold, life lease, crown land lease, Other |
| lease_end_date | date | For leasehold and crown leases |
| survey_date, surveyor | date, text | Last survey / surveyor's real property report |
| easements | long text | Easements, rights-of-way, encroachments |
| restrictions | long text | Covenants, heritage designation, conservation or flood-plain rules |

### 2.4 Land and terrain

| Field | Type | Notes |
| --- | --- | --- |
| lot_area | area | |
| lot_frontage, lot_depth | length | |
| lot_shape | choice | Rectangular, irregular, pie, corner, flag, Other |
| topography | choice | Flat, gently sloping, sloped, hilly, ravine, terraced, Other |
| soil_type | choice | Clay, loam, sand, gravel, rock / Canadian Shield, organic / peat, Other |
| drainage | choice | Good, fair, poor, tile drained, Other |
| vegetation | choice | Lawn, landscaped, wooded, partly wooded, cleared, farmland, natural, Other |
| waterfront | choice | None, lake, river, stream, ocean / sea, pond, canal, Other |
| water_frontage | length | |
| shoreline | choice | Sand, rock, mixed, weedy, retaining wall, Other |
| water_depth_at_dock | length | |
| view | text | |
| access | choice | Year-round municipal road, year-round private road, seasonal road, water access only, fly-in, Other |
| road_maintained_by | text | Municipality, road association (with yearly fee in costs) |
| fencing | choice | None, partial, full, Other |
| zoning | text | Zoning code and description |
| land_designation | multi-choice | Agricultural Land Reserve (B.C.), Greenbelt (Ont.), Oak Ridges Moraine, Niagara Escarpment, agricultural zone (CPTAQ, Que.), flood plain, conservation authority regulated, heritage, environmental protection, Other |
| farm_area_tillable, farm_area_pasture, farm_area_woodland | area | Farm land use |

### 2.5 Building (main building)

| Field | Type | Notes |
| --- | --- | --- |
| year_built | year | |
| year_built_estimated | yes/no | |
| builder | text | |
| major_renovation_years | text | e.g. "2009 kitchen, 2018 addition" (projects keep the detail) |
| storeys | decimal | 1, 1.5, 2… above grade |
| floors_total | number | Including basement levels |
| finished_area_above_grade | area | |
| finished_area_below_grade | area | |
| total_floor_area | area | |
| footprint_area | area | |
| ceiling_height_main | length | |
| units | number | Dwelling units (plexes, multi-unit) |
| bedrooms | number | Summary; the room list (2.6) gives the detail and can fill these counts |
| bathrooms_full, bathrooms_half | number | |
| kitchens | number | |
| construction_type | choice | Wood frame, masonry / brick, double brick, concrete block, poured concrete, insulated concrete forms (ICF), structural insulated panels (SIP), steel frame, log, timber frame, modular / prefab, straw bale, Other |
| foundation | choice | Poured concrete, concrete block, stone, preserved wood, slab-on-grade, piers / posts, screw piles, crawl space, Other |
| exterior_finish | multi-choice | Brick, stone, vinyl siding, aluminum siding, wood siding, cedar shake, fibre cement, stucco, EIFS, log, metal, Other |
| roof_shape | choice | Gable, hip, flat, mansard, gambrel, shed, A-frame, combination, Other |
| roof_material | choice | Asphalt shingle, metal, cedar shake, slate, clay / concrete tile, rubber / EPDM membrane, tar and gravel, TPO, green roof, Other |
| roof_year | year | Last replaced (insurers ask) |
| windows | choice | Single, double or triple glazing; vinyl, wood, aluminum, fibreglass frames (two choices) |
| windows_year | year | |
| doors_year | year | |
| insulation_attic_r, insulation_walls_r, insulation_basement_r | number | R-values |
| energuide_rating | number | EnerGuide (GJ/year or the older 0–100 score; store both the number and the scale) |
| energy_audit_date | date | |
| interior_walls | choice | Drywall, plaster, panelling, Other |
| flooring_main | multi-choice | Hardwood, engineered wood, laminate, vinyl / LVP, tile, carpet, concrete, cork, Other |
| fireplaces | number | Details per fireplace in systems (2.7) |
| accessibility | multi-choice | Step-free entrance, ramp, main-floor bedroom, main-floor bathroom, stair lift, elevator, wide doors, roll-in shower, Other |
| hazardous_materials | multi-choice | Asbestos, vermiculite insulation, urea-formaldehyde foam (UFFI), lead paint, knob-and-tube wiring, aluminum wiring, polybutylene pipes, oil tank (in use / removed), radon tested, Other; with a note |
| radon_test_date, radon_level_bq | date, number | Bq/m³ |

### 2.6 Rooms (table `real_property_room`, many per property)

| Field | Type | Notes |
| --- | --- | --- |
| room_type | choice | Bedroom, primary bedroom, bathroom, ensuite, powder room, kitchen, kitchenette, dining room, living room, family room, great room, den / office, library, laundry, mudroom, foyer / entry, hallway, recreation room, media room, games room, gym, workshop, utility / mechanical, storage, cold cellar, wine cellar, pantry, walk-in closet, sunroom, four-season room, screened porch, loft, attic, nursery, guest room, in-law suite, bonus room, Other |
| name | text | "Emma's room" |
| level | choice | Sub-basement, basement, main, second, third, fourth, loft / attic, other (with text) |
| length, width | length | |
| area | area | Calculated from length × width when both are given; can be typed instead |
| flooring | choice | As flooring_main |
| ceiling_height | length | |
| features | text | Fireplace, built-ins, walk-in closet, vaulted ceiling… |
| condition | choice | Excellent, good, fair, needs work |
| position | number | Order in the list |

Room photos use the existing document links (`document_link`, entity `real_property_room`). The home
inventory report (AST-04) can group contents by room when an asset's `location` names a room from this list.

### 2.7 Systems (table `real_property_system`, many per property)

| Field | Type | Notes |
| --- | --- | --- |
| system_kind | choice | Heating, cooling, water heating, ventilation, electrical, plumbing, water supply, water treatment, sewage, fireplace / stove, solar, generator, EV charger, security, fire protection, internet / telecom, central vacuum, sump / drainage, Other |
| system_type | choice per kind | **Heating:** forced air furnace, boiler / hot water radiators, in-floor radiant, electric baseboard, air-source heat pump, ground-source (geothermal) heat pump, ductless mini-split, wood stove, pellet stove, wood furnace, outdoor wood boiler, space heaters, Other. **Cooling:** central air, heat pump, ductless mini-split, window / portable units, none. **Water heating:** tank, tankless, heat pump, combination with boiler, solar, Other (owned or rented in `ownership`). **Ventilation:** HRV, ERV, exhaust fans only. **Electrical:** service size in amps (60/100/125/200/400), panel type (breakers, fuses), wiring (copper, aluminum, knob-and-tube, mixed). **Plumbing:** supply pipes (copper, PEX, galvanized steel, polybutylene, CPVC, lead), drains (ABS, PVC, cast iron, clay). **Water supply:** municipal, drilled well, dug well, sand point, lake / river intake, cistern, shared well. **Water treatment:** softener, UV, reverse osmosis, filter, chlorinator. **Sewage:** municipal sewer, septic tank and field, holding tank, tertiary system, outhouse / composting. **Fireplace / stove:** wood, gas, propane, electric, pellet; WETT inspected (yes / date). **Fire protection:** smoke alarms, CO alarms, sprinklers, monitored. **Security:** alarm (monitored or not), cameras. |
| fuel | choice | Natural gas, propane, heating oil, electricity, wood, pellets, solar, geothermal, none, Other |
| make_model | text | |
| capacity | text | BTU, tons, litres, kW, amps (free text with unit) |
| install_year | year | |
| last_inspection_date | date | WETT, septic pump-out, well water test, electrical inspection |
| ownership | choice | Owned, rented, leased, financed (rental water heaters and furnaces are common in Ontario) |
| rental_provider, rental_monthly | text, Money | Links to a bill when rented |
| child_asset_id | asset | The furnace, heat pump, pool heater… as its own asset for maintenance and warranty (SET NULL) |
| notes | text | |

### 2.8 Basement

| Field | Type | Notes |
| --- | --- | --- |
| basement_type | choice | Full, partial, crawl space, slab (none), walk-out, walk-up, look-out, cellar, Other |
| basement_finish | choice | Finished, partly finished, unfinished |
| basement_height | length | |
| separate_entrance | yes/no | |
| basement_suite | yes/no | Legal secondary suite (with note) |
| waterproofing | choice | Interior, exterior, membrane, none known |
| sump_pump | choice | None, single, with battery backup, with water-powered backup |
| backwater_valve | yes/no | Insurers often ask |
| past_water_damage | long text | Date and description |

### 2.9 Garage, parking and outbuildings

On `real_property`:

| Field | Type | Notes |
| --- | --- | --- |
| garage_type | choice | None, attached, built-in, detached, carport, underground (condo), Other |
| garage_spaces | number | |
| garage_heated, garage_insulated | yes/no | |
| garage_door_opener | yes/no | |
| driveway | choice | Asphalt, concrete, interlock / pavers, gravel, shared, none, Other |
| parking_spaces | number | Outdoor spaces |

Table `real_property_structure` (many per property) for the other buildings and fixtures on the land:

| Field | Type | Notes |
| --- | --- | --- |
| structure_type | choice | Detached garage, shed, barn, workshop, bunkie / guest cabin, boathouse, dock (fixed, floating, crib, lift), boat lift, sauna, gazebo, greenhouse, coach house / laneway house, silo, machine shed, kennel / stable, Other |
| name | text | |
| area | area | |
| year_built | year | |
| construction | text | |
| heated, electricity, water | yes/no | |
| child_asset_id | asset | When it is maintained as its own asset (SET NULL) |
| notes | text | |

### 2.10 Pool and outdoor features

- **Pool:** type (none, in-ground, above-ground, semi in-ground, indoor), shape, size or volume, liner or
  surface (vinyl, concrete, fibreglass), sanitation (chlorine, salt, mineral), heated (gas, heat pump, solar,
  none), cover, fenced. The pool itself can link to its existing POOL child asset.
- **Hot tub**, deck (material, area), patio, porch / veranda, balcony, irrigation system, outdoor kitchen,
  play structure, fire pit, landscape lighting.
- Stored as rows of table `real_property_feature (asset_id, feature_key, value_text, value_number)`, from a
  fixed list of feature keys. Each key has EN/FR texts and, where it applies, a sub-choice; the table also
  holds interior features such as central vacuum, elevator, wet bar, built-in sound and skylights.

### 2.11 Condominium and co-op (shown for CONDO and for tenure condominium / co-operative)

| Field | Type | Notes |
| --- | --- | --- |
| condo_corporation | text | Name and number (e.g. "TSCC 1234", strata plan "VR 567") |
| condo_unit_level | text | Legal unit and level, when they differ from the address |
| condo_fee_monthly | Money | Links to the condo-fee bill when one exists |
| condo_fee_includes | multi-choice | Heat, hydro, water, building insurance, parking, locker, cable / internet, common elements, Other |
| parking_stall, locker_number | text | |
| exposure | choice | N, NE, E, SE, S, SW, W, NW |
| amenities | multi-choice | Concierge, gym, pool, party room, guest suite, rooftop, visitor parking, car wash, EV chargers, Other |
| property_manager | contact | Link to a contact (CON-04) |
| reserve_fund_study_date | date | |
| special_assessment | Money + text | |
| pets_allowed, rentals_allowed | yes/no + text | Bylaw notes |

### 2.12 Valuation, assessment, tax and insurance figures

| Field | Type | Notes |
| --- | --- | --- |
| assessed_value | Money | |
| assessment_year | year | Valuation date / base year |
| assessed_land_value, assessed_building_value | Money | Where the notice splits them |
| assessing_authority | text | MPAC, BC Assessment, municipality… |
| tax_account_number | text | When it differs from the roll number |
| annual_property_tax | Money | |
| property_tax_bill_id | bill | Link to the existing tax bill and its instalments (BILL-23); SET NULL |
| school_support | choice | Ontario school support (English public, English separate, French public, French separate) |
| insured_replacement_cost | Money | From the insurer, for the policy linked by `policy_asset` |
| market_value_estimate, estimate_date, estimate_source | Money, date, text | Realtor, appraisal, own estimate; the asset's value (AST-03) can use it |
| mortgage_account_id | account | The mortgage or HELOC (SET NULL); shows equity on the property page |

### 2.13 Owners and residence (separate tables)

- `real_property_owner (asset_id, member_id, share_bp, tenancy)`. Tenancy: joint tenancy, tenants in common,
  sole, trust, corporation, Other. Shares in basis points must total 10,000 when given. Used by the estate
  summary and net worth per person.
- `real_property_residence_year (asset_id, member_id, year)`: years designated as principal residence, an
  aid for CRA form T2091. It is available on every real-property asset: home, cottage, condo, land and
  building (owner, 2026-10-25). Land and buildings are included because the land under a home, and a
  dwelling unit in a building the family lives in, can qualify. A warning appears when two properties of the same family unit are designated for
  the same year after 1981. This is an aid only, not tax advice; the text says so.

### 2.14 Custom fields

`real_property_custom (id, asset_id, label, value, position)`: any detail the lists do not cover ("Well
depth: 45 m", "Hydro pole number"), in the user's own words.

## 3. Screens and reports

- **Asset dialog:** for property kinds, a **Property** section with tabs: Address and legal · Land ·
  Building · Rooms · Systems · Basement and parking · Outbuildings and features · Condo (when it applies) ·
  Value and tax · Owners · Other details. The tabs follow the existing pattern (`AssetsScreen.kt:147-272`).
  Long choice lists use the existing searchable picker.
- **Rental properties:** `RentalDialog` gets the property picker (`rental_property.asset_id` exists but is not
  shown), and a rental shows the property's address.
- **Property fact sheet** (new report, AST-16): one or two printable pages per property (PDF and print) with
  every filled-in field. It is useful for insurers, a sale, an executor or a contractor. The user can choose
  to leave out the legal identifiers and owners.
- **Estate summary and home inventory** show the address, roll number and parcel id.
- **Utilities, projects, contractors, maintenance:** their home pickers (`UtilitiesScreen.kt:52`
  `homes()`, `ExtrasScreens.kt:590`) include the new kinds CONDO and BUILDING (and LAND for projects).
- **Demo family** (`DemoFamily.kt`): the house and the cottage get full sample details, rooms and systems,
  for the manual's screenshots.

## 4. Data and sync notes

- **As built (2026-10-25), ledger version 36 (migration `35.sqm`):**
  - The single-value fields of §2.1–2.5 and §2.8–2.12, including the pool and feature list of §2.10, are
    rows of `real_property_field (asset_id, field, value)`. They are defined in code by
    `RealPropertyCatalogue` (key, section, type, choice list), not as one column per field, so a new field
    needs no migration.
  - Rooms, systems, outbuildings, owners, residence years and custom fields have their own tables:
    `real_property_room`, `real_property_system`, `real_property_structure`, `real_property_owner`,
    `real_property_residence_year`, `real_property_custom`.
  - Every table references `asset(id)` with ON DELETE CASCADE. Child assets use ON DELETE SET NULL. Saves use
    `INSERT … ON CONFLICT … DO UPDATE` (never `INSERT OR REPLACE`).
  - Links to a bill, an account or a contact (`property_tax_bill`, `mortgage_account`, `property_manager`)
    are stored as ids without a database link. A deleted bill simply shows as not linked.
  - Areas and lengths are stored in m² and m. The unit the user typed is not kept; the screen shows the
    user's preferred unit.
- Choice values are stored as stable keys (`DETACHED`, `HEAT_PUMP_AIR`), never as display text. Multi-choice
  values are stored as keys in the list's order, joined by commas. The words of an Other choice go in the
  field `<key>.other` and are dropped when Other is no longer chosen.
- Sync policies (plan section 4.3): `real_property_field` FIELD_LWW per row (each field merges on its own,
  so two people can fill in different fields offline); the room, system, structure, custom and owner tables
  FIELD_LWW per row; `real_property_residence_year` APPEND_ONLY with tombstones.
- Moving an asset to another group moves its property rows with it, as for its other child rows.
- The phone gets nothing new (asset details are not sent to the phone today).

## 5. Tests

- `RealPropertyServiceTest`: save and load every field, owner shares that do not total 100 % are refused,
  postal code check, area units and conversions, the residence-year warning.
- `SaveKeepsChildrenTest`: saving the property again keeps rooms, systems, owners and documents.
- `MigrationTest`: upgrade from ledger version 35 keeps every existing asset, and HOME / COTTAGE assets show
  empty property details.
- `MessagesTest`: every new choice key has EN and FR texts.
- The fact sheet report is checked against a fixed demo property (golden text).
