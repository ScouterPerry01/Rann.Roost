// File path and name: core/replication/src/main/kotlin/ca/schippers/hfm/replication/PolicyManifest.kt
// Modified On Timestamp: 2026-10-26 @ 03:45 EDT
// Created On Timestamp: 2026-10-26 @ 03:45 EDT
// File Description: The merge policy of every table of the core and ledger databases (plan-multi-device 4.3).
// Uses: Nothing: plain data, checked against the real schemas by PolicyManifestTest.
// Used By: PolicyManifestTest now; the change capture and apply engine (phases 2 and 3).
// Purpose: No table can be added without saying how two computers' changes to it merge.
package ca.schippers.hfm.replication

/** Which database of a household a table lives in. */
enum class Database { CORE, LEDGER }

/** How changes to a table made on two computers come together (plan-multi-device 4.3). */
enum class Policy {
    /** Each column keeps the value with the latest clock; edits to different fields both survive. */
    FIELD_LWW,

    /** Rows belong to a parent row and are saved as a set with it; the latest change set replaces the set. */
    AGGREGATE,

    /** Rows are only added (a delete still travels as a tombstone); merging is the union. */
    APPEND_ONLY,

    /** Each computer keeps its own count of the [TablePolicy.counterColumns]; the value shown is the sum. */
    COUNTER,

    /**
     * Rows made by automatic jobs from natural keys or name-based ids, so the same row from two
     * computers is one row (4.6). Their other columns merge as in [FIELD_LWW].
     */
    IDEMPOTENT,

    /** Never sent to another computer. */
    LOCAL_ONLY,
}

data class TablePolicy(
    val database: Database,
    val table: String,
    val policy: Policy,
    /** For [Policy.AGGREGATE]: the table whose row owns the set. */
    val parent: String? = null,
    /** Columns that merge by adding each computer's changes, as in [Policy.COUNTER]. */
    val counterColumns: Set<String> = emptySet(),
    /** Cursor columns moved forward by jobs: they merge by keeping the largest value (4.6). */
    val maxColumns: Set<String> = emptySet(),
)

/**
 * Every table of both schemas with its policy. PolicyManifestTest fails when a table has none, when
 * a listed table no longer exists, or when a parent or column named here is missing.
 *
 * Known work before change capture (phase 2), from the 2026-10-26 survey:
 * - Tables still saved with INSERT OR REPLACE rewrite every column, so a capture cannot tell which
 *   field changed; they move to INSERT ... ON CONFLICT DO UPDATE first.
 * - category_rule is edited by a delete and an insert under the same id; capture must treat that
 *   pair in one transaction as an update.
 * - medication.refills_remaining is lowered in Kotlin (read, subtract, write); it must be captured as
 *   a change of -1, or worked out from medication_fill.
 */
object PolicyManifest {

    val tables: List<TablePolicy> = buildList {
        fun core(table: String, policy: Policy = Policy.FIELD_LWW, parent: String? = null, counters: Set<String> = emptySet(), max: Set<String> = emptySet()) =
            add(TablePolicy(Database.CORE, table, policy, parent, counters, max))
        fun ledger(table: String, policy: Policy = Policy.FIELD_LWW, parent: String? = null, counters: Set<String> = emptySet(), max: Set<String> = emptySet()) =
            add(TablePolicy(Database.LEDGER, table, policy, parent, counters, max))

        // --- Core database ------------------------------------------------------------------------
        core("household")
        core("member")
        core("pet")
        core("app_user")
        core("account_group")
        core("group_permission")
        core("institution")
        core("category")
        core("payee")
        core("payee_alias", Policy.APPEND_ONLY)
        core("tag")
        core("category_rule")
        core("fx_rate", Policy.IDEMPOTENT)
        core("spot_price", Policy.IDEMPOTENT)
        core("budget")
        core("setting")
        core("audit_log", Policy.APPEND_ONLY)
        core("rule_value", Policy.APPEND_ONLY)
        core("saved_report", max = setOf("last_period"))
        // Phones are paired with one computer (plan section 7); this computer's own settings (5.1).
        core("sync_device", Policy.LOCAL_ONLY)
        core("sync_item", Policy.LOCAL_ONLY)
        core("repl_local_setting", Policy.LOCAL_ONLY)

        // --- Ledger databases: money --------------------------------------------------------------
        ledger("account")
        ledger("account_owner", Policy.AGGREGATE, parent = "account")
        ledger("account_alert")
        ledger("txn_alert_dismissed", Policy.APPEND_ONLY)
        ledger("txn")
        ledger("txn_split", Policy.AGGREGATE, parent = "txn")
        ledger("txn_tag", Policy.AGGREGATE, parent = "txn")
        ledger("txn_sales_tax", Policy.AGGREGATE, parent = "txn")
        ledger("txn_suggested_category")
        ledger("txn_template")
        ledger("txn_template_line", Policy.AGGREGATE, parent = "txn_template")
        ledger("change_log", Policy.APPEND_ONLY)
        ledger("statement")
        ledger("statement_line")
        ledger("statement_match", Policy.AGGREGATE, parent = "statement")
        ledger("merchant_memory", Policy.COUNTER, counters = setOf("uses"))
        ledger("bill")
        ledger("bill_occurrence", Policy.IDEMPOTENT)
        ledger("bill_statement")
        ledger("bill_payment")
        ledger("credit_card")
        ledger("card_statement")
        ledger("card_holder")
        ledger("card_benefit")
        ledger("card_reward")
        ledger("reward_entry", Policy.APPEND_ONLY)
        ledger("loan", max = setOf("last_paid_date"))
        ledger("loan_change", Policy.APPEND_ONLY)
        ledger("savings_goal", max = setOf("last_posted"))
        ledger("goal_entry", Policy.APPEND_ONLY)
        ledger("donation")
        ledger("donation_receipt")
        ledger("wallet", max = setOf("last_sync"))

        // --- Investments, plans and taxes ---------------------------------------------------------
        ledger("security")
        ledger("security_price", Policy.IDEMPOTENT)
        ledger("inv_txn")
        ledger("inv_statement")
        ledger("metal_item")
        ledger("registered_plan")
        ledger("plan_beneficiary")
        ledger("plan_value")
        ledger("room_entry")
        ledger("room_adjustment", Policy.APPEND_ONLY)
        ledger("resp_grant", Policy.APPEND_ONLY)
        ledger("pension")
        ledger("pension_statement")
        ledger("tax_slip")
        ledger("tax_slip_check")
        // Saved as a set per account, member, year and authority.
        ledger("tax_instalment", Policy.AGGREGATE, parent = "account")
        ledger("tax_estimate_figure")

        // --- Documents and AI -----------------------------------------------------------------------
        ledger("document")
        ledger("document_link", Policy.APPEND_ONLY)
        ledger("document_ai")
        ledger("ai_usage", Policy.APPEND_ONLY)

        // --- Assets, insurance, real property --------------------------------------------------------
        ledger("asset")
        ledger("asset_task")
        ledger("asset_reading", Policy.APPEND_ONLY)
        ledger("asset_service")
        ledger("asset_service_task", Policy.AGGREGATE, parent = "asset_service")
        ledger("warranty")
        ledger("warranty_claim")
        ledger("insurance_policy")
        ledger("policy_asset", Policy.AGGREGATE, parent = "insurance_policy")
        ledger("policy_premium")
        ledger("policy_beneficiary")
        ledger("insurance_claim")
        // A property's details are saved as one record with the asset (RealPropertyService.save).
        ledger("real_property_field", Policy.AGGREGATE, parent = "asset")
        ledger("real_property_room", Policy.AGGREGATE, parent = "asset")
        ledger("real_property_system", Policy.AGGREGATE, parent = "asset")
        ledger("real_property_structure", Policy.AGGREGATE, parent = "asset")
        ledger("real_property_owner", Policy.AGGREGATE, parent = "asset")
        ledger("real_property_residence_year", Policy.AGGREGATE, parent = "asset")
        ledger("real_property_custom", Policy.AGGREGATE, parent = "asset")
        ledger("rental_property")
        ledger("home_project")
        ledger("home_project_rebate")
        ledger("project_cost", Policy.APPEND_ONLY)
        ledger("contractor")
        ledger("contractor_job")
        ledger("invoice")

        // --- Vehicles ---------------------------------------------------------------------------------
        ledger("vehicle")
        ledger("vehicle_reading", Policy.APPEND_ONLY)
        ledger("maintenance_task")
        ledger("service_record")
        ledger("service_task", Policy.AGGREGATE, parent = "service_record")
        ledger("fuel_entry")
        ledger("vehicle_warranty")
        ledger("vehicle_warranty_claim")

        // --- Health and medical -----------------------------------------------------------------------
        ledger("med_plan")
        ledger("med_plan_person", Policy.AGGREGATE, parent = "med_plan")
        ledger("med_coverage")
        ledger("med_expense")
        ledger("med_claim")
        ledger("health_provider")
        ledger("medication", max = setOf("last_fill_date"))
        ledger("medication_fill", Policy.APPEND_ONLY)
        ledger("health_condition")
        ledger("allergy")
        ledger("health_test")
        ledger("immunization")

        // --- Calendar, contacts, trips, schedules -----------------------------------------------------
        ledger("event")
        ledger("event_occurrence", Policy.IDEMPOTENT)
        ledger("event_activity_day")
        ledger("brought_in_calendar", max = setOf("snapshot_at"))
        ledger("brought_in_item", Policy.AGGREGATE, parent = "brought_in_calendar")
        ledger("contact")
        ledger("contact_detail", Policy.AGGREGATE, parent = "contact")
        ledger("contact_link")
        ledger("phone_contact", Policy.APPEND_ONLY)
        ledger("trip")
        ledger("trip_stop", Policy.AGGREGATE, parent = "trip")
        ledger("trip_place")
        ledger("person_schedule")
        ledger("person_schedule_shift", Policy.AGGREGATE, parent = "person_schedule")
        ledger("person_schedule_exception", Policy.AGGREGATE, parent = "person_schedule")
        ledger("estate_record")

        // --- Family money and trackers ----------------------------------------------------------------
        ledger("share_group")
        ledger("share_person")
        ledger("share_entry")
        ledger("family_loan")
        ledger("family_loan_payment", Policy.APPEND_ONLY)
        ledger("allowance")
        ledger("allowance_entry", Policy.APPEND_ONLY)
        ledger("utility_meter")
        ledger("utility_reading", Policy.APPEND_ONLY)
        ledger("fuel_tank")
        ledger("fuel_tank_reading", Policy.APPEND_ONLY)
        ledger("fuel_tank_delivery", Policy.APPEND_ONLY)
        ledger("work_client")
        ledger("work_task")
        ledger("work_hours")
        ledger("chore")
        ledger("chore_tick")
        ledger("volunteer_hours")
    }

    fun of(database: Database, table: String): TablePolicy? = tables.firstOrNull { it.database == database && it.table == table }
}
