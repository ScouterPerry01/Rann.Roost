// File path and name: core/books/src/main/kotlin/ca/schippers/hfm/books/RealPropertyCatalogue.kt
// Modified On Timestamp: 2026-10-25 @ 16:20 EDT
// Created On Timestamp: 2026-10-25 @ 16:20 EDT
// File Description: The catalogue of real-property fields (AST-07 to AST-14): each field's key, section,
//   type and choice list, and every choice list of rooms, systems, outbuildings and owners.
// Uses: docs/spec-real-property.md (the field list); core/i18n texts property.* (labels and choices).
// Used By: RealPropertyService (validation and storage), the desktop Property tabs and fact sheet.
// Purpose: One place that defines the fields, so adding a field needs no database migration.
package ca.schippers.hfm.books

/** AST-06: the asset kinds that have real-property details. */
val REAL_PROPERTY_KINDS: Set<AssetKind> = setOf(AssetKind.HOME, AssetKind.COTTAGE, AssetKind.CONDO, AssetKind.LAND, AssetKind.BUILDING)

/** The tabs of the Property section, in order (docs/spec-real-property.md §3). */
enum class PropertySection { IDENTIFICATION, ADDRESS, LEGAL, LAND, BUILDING, BASEMENT, PARKING, FEATURES, CONDO, VALUE }

/**
 * How a field's value is entered and stored (always as text in `real_property_field`):
 * - TEXT up to 200 characters, LONG_TEXT up to 4,000;
 * - INTEGER and YEAR as whole numbers, DECIMAL as a plain decimal;
 * - AREA in square metres and LENGTH in metres, as plain decimals (shown in the user's unit);
 * - MONEY in minor units of the asset's currency; DATE as ISO yyyy-mm-dd; YES_NO as 1 or 0;
 * - CHOICE as one key of its list, MULTI_CHOICE as keys sorted and joined by commas; OTHER on either
 *   keeps its words in the field "<key>.other";
 * - PROVINCE as its postal abbreviation, COUNTRY as an ISO 3166 two-letter code, POSTAL_CODE as "A1A 1A1"
 *   when the country is Canada; LATITUDE and LONGITUDE as decimal degrees;
 * - BILL, ACCOUNT and CONTACT as the id of a bill, an account or a contact.
 */
enum class PropertyFieldType {
    TEXT, LONG_TEXT, INTEGER, YEAR, DECIMAL, AREA, LENGTH, MONEY, DATE, YES_NO, CHOICE, MULTI_CHOICE,
    PROVINCE, COUNTRY, POSTAL_CODE, LATITUDE, LONGITUDE, BILL, ACCOUNT, CONTACT,
}

/**
 * One field of the Property section. Its label is the text `property.field.<key>`; a choice is
 * `property.choice.<choices>.<KEY>`. [hint] names a `property.hint.<key>` text shown under the field.
 */
data class PropertyField(
    val key: String,
    val section: PropertySection,
    val type: PropertyFieldType,
    val choices: String? = null,
    val hint: Boolean = false,
)

/** AST-07 to AST-14: every single-value field of the Property section and every choice list. */
object RealPropertyCatalogue {

    /** The word kept for "Other" in every choice list; its own words go in "<field>.other". */
    const val OTHER = "OTHER"

    /** The suffix of the field holding the words of an Other choice. */
    const val OTHER_SUFFIX = ".other"

    val choices: Map<String, List<String>> = linkedMapOf(
        // 2.1 Identification and use
        "propertyType" to listOf(
            "DETACHED", "SEMI_DETACHED", "TOWNHOUSE", "DUPLEX", "TRIPLEX", "FOURPLEX", "CONDO_APARTMENT", "CONDO_TOWNHOUSE", "MOBILE_HOME",
            "COTTAGE_CABIN", "FARM", "VACANT_LAND", "RECREATIONAL_LAND", "COMMERCIAL", "INDUSTRIAL", "MIXED_USE", "MULTI_UNIT", OTHER,
        ),
        "buildingStyle" to listOf(
            "BUNGALOW", "RAISED_BUNGALOW", "STOREY_1_5", "STOREY_2", "STOREY_2_5", "STOREY_3", "SPLIT_LEVEL", "BACK_SPLIT", "SIDE_SPLIT", "BI_LEVEL",
            "A_FRAME", "CHALET", "LOG_HOME", "TIMBER_FRAME", "VICTORIAN", "EDWARDIAN", "CAPE_COD", "COLONIAL", "CRAFTSMAN", "TUDOR", "RANCH",
            "CONTEMPORARY", "QUEBEC_CANADIENNE", "MONTREAL_PLEX", OTHER,
        ),
        "use" to listOf("PRINCIPAL_RESIDENCE", "SECONDARY_RESIDENCE", "SEASONAL", "RENTAL", "PARTLY_RENTED", "FARM", "BUSINESS", "VACANT", OTHER),
        "occupancy" to listOf("OWNER", "TENANT", "MIXED", "VACANT"),
        "acquiredHow" to listOf("PURCHASE", "NEW_BUILD", "INHERITANCE", "GIFT", "SPOUSAL_TRANSFER", OTHER),
        // 2.3 Legal
        "tenure" to listOf("FREEHOLD", "CONDOMINIUM", "COOPERATIVE", "LEASEHOLD", "LIFE_LEASE", "CROWN_LEASE", OTHER),
        // 2.4 Land and terrain
        "lotShape" to listOf("RECTANGULAR", "IRREGULAR", "PIE", "CORNER", "FLAG", OTHER),
        "topography" to listOf("FLAT", "GENTLE_SLOPE", "SLOPED", "HILLY", "RAVINE", "TERRACED", OTHER),
        "soil" to listOf("CLAY", "LOAM", "SAND", "GRAVEL", "ROCK", "ORGANIC", OTHER),
        "drainage" to listOf("GOOD", "FAIR", "POOR", "TILE_DRAINED", OTHER),
        "vegetation" to listOf("LAWN", "LANDSCAPED", "WOODED", "PARTLY_WOODED", "CLEARED", "FARMLAND", "NATURAL", OTHER),
        "waterfront" to listOf("NONE", "LAKE", "RIVER", "STREAM", "OCEAN", "POND", "CANAL", OTHER),
        "shoreline" to listOf("SAND", "ROCK", "MIXED", "WEEDY", "RETAINING_WALL", OTHER),
        "access" to listOf("MUNICIPAL_ROAD", "PRIVATE_ROAD", "SEASONAL_ROAD", "WATER_ONLY", "FLY_IN", OTHER),
        "fencing" to listOf("NONE", "PARTIAL", "FULL", OTHER),
        "landDesignation" to listOf(
            "BC_ALR", "ON_GREENBELT", "OAK_RIDGES_MORAINE", "NIAGARA_ESCARPMENT", "QC_AGRICULTURAL_ZONE", "FLOOD_PLAIN", "CONSERVATION_REGULATED",
            "HERITAGE", "ENVIRONMENTAL_PROTECTION", OTHER,
        ),
        // 2.5 Building
        "construction" to listOf(
            "WOOD_FRAME", "MASONRY", "DOUBLE_BRICK", "CONCRETE_BLOCK", "POURED_CONCRETE", "ICF", "SIP", "STEEL_FRAME", "LOG", "TIMBER_FRAME",
            "MODULAR", "STRAW_BALE", OTHER,
        ),
        "foundation" to listOf("POURED_CONCRETE", "CONCRETE_BLOCK", "STONE", "PRESERVED_WOOD", "SLAB", "PIERS", "SCREW_PILES", "CRAWL_SPACE", OTHER),
        "exteriorFinish" to listOf("BRICK", "STONE", "VINYL", "ALUMINUM", "WOOD", "CEDAR_SHAKE", "FIBRE_CEMENT", "STUCCO", "EIFS", "LOG", "METAL", OTHER),
        "roofShape" to listOf("GABLE", "HIP", "FLAT", "MANSARD", "GAMBREL", "SHED", "A_FRAME", "COMBINATION", OTHER),
        "roofMaterial" to listOf("ASPHALT_SHINGLE", "METAL", "CEDAR_SHAKE", "SLATE", "TILE", "EPDM", "TAR_GRAVEL", "TPO", "GREEN_ROOF", OTHER),
        "glazing" to listOf("SINGLE", "DOUBLE", "TRIPLE", "MIXED"),
        "windowFrame" to listOf("VINYL", "WOOD", "ALUMINUM", "FIBREGLASS", "CLAD_WOOD", OTHER),
        "energuideScale" to listOf("GJ_PER_YEAR", "SCORE_0_100"),
        "interiorWalls" to listOf("DRYWALL", "PLASTER", "PANELLING", OTHER),
        "flooring" to listOf("HARDWOOD", "ENGINEERED_WOOD", "LAMINATE", "VINYL", "TILE", "CARPET", "CONCRETE", "CORK", OTHER),
        "accessibility" to listOf(
            "STEP_FREE_ENTRANCE", "RAMP", "MAIN_FLOOR_BEDROOM", "MAIN_FLOOR_BATHROOM", "STAIR_LIFT", "ELEVATOR", "WIDE_DOORS", "ROLL_IN_SHOWER", OTHER,
        ),
        "hazards" to listOf(
            "ASBESTOS", "VERMICULITE", "UFFI", "LEAD_PAINT", "KNOB_AND_TUBE", "ALUMINUM_WIRING", "POLYBUTYLENE", "OIL_TANK_IN_USE", "OIL_TANK_REMOVED",
            "RADON_TESTED", OTHER,
        ),
        // 2.8 Basement
        "basementType" to listOf("FULL", "PARTIAL", "CRAWL_SPACE", "SLAB", "WALK_OUT", "WALK_UP", "LOOK_OUT", "CELLAR", OTHER),
        "basementFinish" to listOf("FINISHED", "PARTLY_FINISHED", "UNFINISHED"),
        "waterproofing" to listOf("INTERIOR", "EXTERIOR", "MEMBRANE", "NONE_KNOWN"),
        "sumpPump" to listOf("NONE", "SINGLE", "BATTERY_BACKUP", "WATER_BACKUP"),
        // 2.9 Garage and parking
        "garageType" to listOf("NONE", "ATTACHED", "BUILT_IN", "DETACHED", "CARPORT", "UNDERGROUND", OTHER),
        "driveway" to listOf("ASPHALT", "CONCRETE", "PAVERS", "GRAVEL", "SHARED", "NONE", OTHER),
        // 2.10 Pool and features
        "poolType" to listOf("NONE", "IN_GROUND", "ABOVE_GROUND", "SEMI_IN_GROUND", "INDOOR"),
        "poolSurface" to listOf("VINYL", "CONCRETE", "FIBREGLASS", OTHER),
        "poolSanitation" to listOf("CHLORINE", "SALT", "MINERAL", OTHER),
        "poolHeating" to listOf("NONE", "GAS", "HEAT_PUMP", "SOLAR", OTHER),
        "deckMaterial" to listOf("NONE", "PRESSURE_TREATED", "CEDAR", "COMPOSITE", "PVC", "CONCRETE", "STONE", OTHER),
        // 2.11 Condo
        "condoFeeIncludes" to listOf("HEAT", "HYDRO", "WATER", "BUILDING_INSURANCE", "PARKING", "LOCKER", "CABLE_INTERNET", "COMMON_ELEMENTS", OTHER),
        "exposure" to listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        "amenities" to listOf("CONCIERGE", "GYM", "POOL", "PARTY_ROOM", "GUEST_SUITE", "ROOFTOP", "VISITOR_PARKING", "CAR_WASH", "EV_CHARGERS", OTHER),
        // 2.12 Value and tax
        "schoolSupport" to listOf("ENGLISH_PUBLIC", "ENGLISH_SEPARATE", "FRENCH_PUBLIC", "FRENCH_SEPARATE"),
        // 2.6 Rooms
        "roomType" to listOf(
            "BEDROOM", "PRIMARY_BEDROOM", "BATHROOM", "ENSUITE", "POWDER_ROOM", "KITCHEN", "KITCHENETTE", "DINING", "LIVING", "FAMILY", "GREAT_ROOM",
            "DEN_OFFICE", "LIBRARY", "LAUNDRY", "MUDROOM", "FOYER", "HALLWAY", "RECREATION", "MEDIA", "GAMES", "GYM", "WORKSHOP", "UTILITY", "STORAGE",
            "COLD_CELLAR", "WINE_CELLAR", "PANTRY", "WALK_IN_CLOSET", "SUNROOM", "FOUR_SEASON", "SCREENED_PORCH", "LOFT", "ATTIC", "NURSERY",
            "GUEST_ROOM", "IN_LAW_SUITE", "BONUS_ROOM", OTHER,
        ),
        "roomLevel" to listOf("SUB_BASEMENT", "BASEMENT", "MAIN", "SECOND", "THIRD", "FOURTH", "LOFT", OTHER),
        "roomCondition" to listOf("EXCELLENT", "GOOD", "FAIR", "NEEDS_WORK"),
        // 2.7 Systems: the kind, then a list of types per kind
        "systemKind" to listOf(
            "HEATING", "COOLING", "WATER_HEATING", "VENTILATION", "ELECTRICAL", "PLUMBING", "WATER_SUPPLY", "WATER_TREATMENT", "SEWAGE", "FIREPLACE",
            "SOLAR", "GENERATOR", "EV_CHARGER", "SECURITY", "FIRE_PROTECTION", "TELECOM", "CENTRAL_VACUUM", "SUMP", OTHER,
        ),
        "heatingType" to listOf(
            "FORCED_AIR", "BOILER", "RADIANT_FLOOR", "ELECTRIC_BASEBOARD", "HEAT_PUMP_AIR", "HEAT_PUMP_GROUND", "MINI_SPLIT", "WOOD_STOVE",
            "PELLET_STOVE", "WOOD_FURNACE", "OUTDOOR_WOOD_BOILER", "SPACE_HEATERS", OTHER,
        ),
        "coolingType" to listOf("CENTRAL_AIR", "HEAT_PUMP", "MINI_SPLIT", "WINDOW_UNITS", "NONE", OTHER),
        "waterHeatingType" to listOf("TANK", "TANKLESS", "HEAT_PUMP", "COMBINATION_BOILER", "SOLAR", OTHER),
        "ventilationType" to listOf("HRV", "ERV", "EXHAUST_FANS", OTHER),
        "electricalType" to listOf("BREAKERS", "FUSES", "COPPER_WIRING", "ALUMINUM_WIRING", "KNOB_AND_TUBE", "MIXED_WIRING", OTHER),
        "plumbingType" to listOf("COPPER", "PEX", "GALVANIZED", "POLYBUTYLENE", "CPVC", "LEAD", "ABS_PVC_DRAINS", "CAST_IRON_DRAINS", "MIXED", OTHER),
        "waterSupplyType" to listOf("MUNICIPAL", "DRILLED_WELL", "DUG_WELL", "SAND_POINT", "LAKE_INTAKE", "CISTERN", "SHARED_WELL", OTHER),
        "waterTreatmentType" to listOf("SOFTENER", "UV", "REVERSE_OSMOSIS", "FILTER", "CHLORINATOR", OTHER),
        "sewageType" to listOf("MUNICIPAL", "SEPTIC", "HOLDING_TANK", "TERTIARY", "OUTHOUSE_COMPOSTING", OTHER),
        "fireplaceType" to listOf("WOOD", "GAS", "PROPANE", "ELECTRIC", "PELLET", OTHER),
        "securityType" to listOf("MONITORED_ALARM", "UNMONITORED_ALARM", "CAMERAS", "SMART_LOCKS", OTHER),
        "fireProtectionType" to listOf("SMOKE_ALARMS", "CO_ALARMS", "SPRINKLERS", "MONITORED", OTHER),
        "fuel" to listOf("NATURAL_GAS", "PROPANE", "HEATING_OIL", "ELECTRICITY", "WOOD", "PELLETS", "SOLAR", "GEOTHERMAL", "NONE", OTHER),
        "systemOwnership" to listOf("OWNED", "RENTED", "LEASED", "FINANCED"),
        // 2.9 Outbuildings and fixtures
        "structureType" to listOf(
            "DETACHED_GARAGE", "SHED", "BARN", "WORKSHOP", "BUNKIE", "BOATHOUSE", "DOCK_FIXED", "DOCK_FLOATING", "DOCK_CRIB", "BOAT_LIFT", "SAUNA",
            "GAZEBO", "GREENHOUSE", "COACH_HOUSE", "SILO", "MACHINE_SHED", "KENNEL_STABLE", OTHER,
        ),
        // 2.13 Owners
        "tenancy" to listOf("SOLE", "JOINT_TENANCY", "TENANTS_IN_COMMON", "TRUST", "CORPORATION", OTHER),
    )

    /** The list of types for each kind of system (AST-12); kinds without one take free words. */
    val systemTypes: Map<String, String> = mapOf(
        "HEATING" to "heatingType", "COOLING" to "coolingType", "WATER_HEATING" to "waterHeatingType", "VENTILATION" to "ventilationType",
        "ELECTRICAL" to "electricalType", "PLUMBING" to "plumbingType", "WATER_SUPPLY" to "waterSupplyType", "WATER_TREATMENT" to "waterTreatmentType",
        "SEWAGE" to "sewageType", "FIREPLACE" to "fireplaceType", "SECURITY" to "securityType", "FIRE_PROTECTION" to "fireProtectionType",
    )

    private fun section(section: PropertySection, vararg fields: Triple<String, PropertyFieldType, String?>, hints: Set<String> = emptySet()) =
        fields.map { (key, type, list) -> PropertyField(key, section, type, list, key in hints) }

    private fun f(key: String, type: PropertyFieldType, choices: String? = null) = Triple(key, type, choices)

    private val T = PropertyFieldType.TEXT
    private val LT = PropertyFieldType.LONG_TEXT
    private val INT = PropertyFieldType.INTEGER
    private val YEAR = PropertyFieldType.YEAR
    private val DEC = PropertyFieldType.DECIMAL
    private val AREA = PropertyFieldType.AREA
    private val LEN = PropertyFieldType.LENGTH
    private val MONEY = PropertyFieldType.MONEY
    private val DATE = PropertyFieldType.DATE
    private val YN = PropertyFieldType.YES_NO
    private val ONE = PropertyFieldType.CHOICE
    private val MANY = PropertyFieldType.MULTI_CHOICE

    val fields: List<PropertyField> = listOf(
        section(
            PropertySection.IDENTIFICATION,
            f("property_type", ONE, "propertyType"), f("building_style", ONE, "buildingStyle"), f("use", ONE, "use"),
            f("occupancy", ONE, "occupancy"), f("acquired_how", ONE, "acquiredHow"),
        ),
        section(
            PropertySection.ADDRESS,
            f("unit", T), f("street_number", T), f("street_name", T), f("address_line_2", T), f("municipality", T), f("county_region", T),
            f("province", PropertyFieldType.PROVINCE), f("postal_code", PropertyFieldType.POSTAL_CODE), f("country", PropertyFieldType.COUNTRY),
            f("emergency_civic_number", T), f("latitude", PropertyFieldType.LATITUDE), f("longitude", PropertyFieldType.LONGITUDE), f("directions", LT),
            hints = setOf("emergency_civic_number", "latitude"),
        ),
        section(
            PropertySection.LEGAL,
            f("roll_number", T), f("parcel_id", T), f("lot", T), f("plan", T), f("concession", T), f("block", T), f("part", T),
            f("geographic_township", T), f("survey_description", T), f("legal_description", LT), f("title_number", T), f("registry_office", T),
            f("tenure", ONE, "tenure"), f("lease_end_date", DATE), f("survey_date", DATE), f("surveyor", T), f("easements", LT), f("restrictions", LT),
            hints = setOf("roll_number", "parcel_id", "survey_description"),
        ),
        section(
            PropertySection.LAND,
            f("lot_area", AREA), f("lot_frontage", LEN), f("lot_depth", LEN), f("lot_shape", ONE, "lotShape"), f("topography", ONE, "topography"),
            f("soil_type", ONE, "soil"), f("drainage", ONE, "drainage"), f("vegetation", ONE, "vegetation"), f("waterfront", ONE, "waterfront"),
            f("water_frontage", LEN), f("shoreline", ONE, "shoreline"), f("water_depth_at_dock", LEN), f("view", T), f("access", ONE, "access"),
            f("road_maintained_by", T), f("fencing", ONE, "fencing"), f("zoning", T), f("land_designation", MANY, "landDesignation"),
            f("farm_area_tillable", AREA), f("farm_area_pasture", AREA), f("farm_area_woodland", AREA),
        ),
        section(
            PropertySection.BUILDING,
            f("year_built", YEAR), f("year_built_estimated", YN), f("builder", T), f("major_renovations", LT), f("storeys", DEC), f("floors_total", INT),
            f("finished_area_above_grade", AREA), f("finished_area_below_grade", AREA), f("total_floor_area", AREA), f("footprint_area", AREA),
            f("ceiling_height_main", LEN), f("units", INT), f("bedrooms", INT), f("bathrooms_full", INT), f("bathrooms_half", INT), f("kitchens", INT),
            f("construction_type", ONE, "construction"), f("foundation", ONE, "foundation"), f("exterior_finish", MANY, "exteriorFinish"),
            f("roof_shape", ONE, "roofShape"), f("roof_material", ONE, "roofMaterial"), f("roof_year", YEAR), f("window_glazing", ONE, "glazing"),
            f("window_frames", ONE, "windowFrame"), f("windows_year", YEAR), f("doors_year", YEAR), f("insulation_attic_r", DEC),
            f("insulation_walls_r", DEC), f("insulation_basement_r", DEC), f("energuide_rating", DEC), f("energuide_scale", ONE, "energuideScale"),
            f("energy_audit_date", DATE), f("interior_walls", ONE, "interiorWalls"), f("flooring_main", MANY, "flooring"), f("fireplaces", INT),
            f("accessibility", MANY, "accessibility"), f("hazardous_materials", MANY, "hazards"), f("hazardous_notes", LT), f("radon_test_date", DATE),
            f("radon_level_bq", INT),
            hints = setOf("storeys", "energuide_rating", "radon_level_bq"),
        ),
        section(
            PropertySection.BASEMENT,
            f("basement_type", ONE, "basementType"), f("basement_finish", ONE, "basementFinish"), f("basement_height", LEN), f("separate_entrance", YN),
            f("basement_suite", YN), f("basement_suite_notes", T), f("waterproofing", ONE, "waterproofing"), f("sump_pump", ONE, "sumpPump"),
            f("backwater_valve", YN), f("past_water_damage", LT),
            hints = setOf("backwater_valve"),
        ),
        section(
            PropertySection.PARKING,
            f("garage_type", ONE, "garageType"), f("garage_spaces", INT), f("garage_heated", YN), f("garage_insulated", YN), f("garage_door_opener", YN),
            f("driveway", ONE, "driveway"), f("parking_spaces", INT),
        ),
        section(
            PropertySection.FEATURES,
            f("pool_type", ONE, "poolType"), f("pool_shape", T), f("pool_size", T), f("pool_surface", ONE, "poolSurface"),
            f("pool_sanitation", ONE, "poolSanitation"), f("pool_heating", ONE, "poolHeating"), f("pool_cover", YN), f("pool_fenced", YN), f("hot_tub", YN),
            f("deck_material", ONE, "deckMaterial"), f("deck_area", AREA), f("patio", YN), f("porch", YN), f("balcony", YN), f("irrigation", YN),
            f("outdoor_kitchen", YN), f("play_structure", YN), f("fire_pit", YN), f("landscape_lighting", YN), f("central_vacuum", YN), f("elevator", YN),
            f("wet_bar", YN), f("built_in_sound", YN), f("skylights", YN),
        ),
        section(
            PropertySection.CONDO,
            f("condo_corporation", T), f("condo_unit_level", T), f("condo_fee_monthly", MONEY), f("condo_fee_includes", MANY, "condoFeeIncludes"),
            f("parking_stall", T), f("locker_number", T), f("exposure", ONE, "exposure"), f("amenities", MANY, "amenities"),
            f("property_manager", PropertyFieldType.CONTACT), f("reserve_fund_study_date", DATE), f("special_assessment", MONEY),
            f("special_assessment_notes", T), f("pets_allowed", YN), f("rentals_allowed", YN), f("bylaw_notes", LT),
            hints = setOf("condo_corporation"),
        ),
        section(
            PropertySection.VALUE,
            f("assessed_value", MONEY), f("assessment_year", YEAR), f("assessed_land_value", MONEY), f("assessed_building_value", MONEY),
            f("assessing_authority", T), f("tax_account_number", T), f("annual_property_tax", MONEY), f("property_tax_bill", PropertyFieldType.BILL),
            f("school_support", ONE, "schoolSupport"), f("insured_replacement_cost", MONEY), f("market_value_estimate", MONEY), f("estimate_date", DATE),
            f("estimate_source", T), f("mortgage_account", PropertyFieldType.ACCOUNT),
            hints = setOf("assessing_authority", "school_support", "insured_replacement_cost"),
        ),
    ).flatten()

    private val byKey: Map<String, PropertyField> = fields.associateBy { it.key }

    fun field(key: String): PropertyField? = byKey[key]

    fun inSection(section: PropertySection): List<PropertyField> = fields.filter { it.section == section }

    /** The keys of choice list [name]; fails for a list that does not exist (a mistake in the code). */
    fun choiceList(name: String): List<String> = choices[name] ?: error("No choice list $name")

    /** Every text key the catalogue needs, for the test that checks EN and FR have them all. */
    fun textKeys(): Set<String> = buildSet {
        PropertySection.entries.forEach { add("property.section.$it") }
        fields.forEach { pf ->
            add("property.field.${pf.key}")
            if (pf.hint) add("property.hint.${pf.key}")
        }
        choices.forEach { (list, keys) -> keys.forEach { add("property.choice.$list.$it") } }
    }
}
