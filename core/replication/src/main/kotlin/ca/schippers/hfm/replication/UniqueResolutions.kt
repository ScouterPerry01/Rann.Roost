// File path and name: core/replication/src/main/kotlin/ca/schippers/hfm/replication/UniqueResolutions.kt
// Modified On Timestamp: 2026-10-26 @ 03:48 EDT
// Created On Timestamp: 2026-10-26 @ 03:48 EDT
// File Description: What happens when two computers make rows with the same unique value (plan-multi-device 4.5).
// Uses: Nothing: plain data, checked against the real schemas by UniqueResolutionsTest.
// Used By: UniqueResolutionsTest now; the apply engine's resolvers (phase 3).
// Purpose: No UNIQUE constraint can be added without saying how a clash between computers is settled.
package ca.schippers.hfm.replication

/** How a clash on a UNIQUE constraint is settled when another computer's change is applied. */
enum class Resolution {
    /** The unique value is how the app finds the row (a natural key or a name-based id): both are the same row, merged field by field. */
    SAME_ROW,

    /** Duplicates (a tag or payee named twice): keep the earlier row, point references to it, and audit the merge. */
    MERGE_INTO_EARLIER,

    /** The same file or line imported on two computers: keep the earlier rows; the later go to Changes to review as "imported twice". */
    IMPORTED_TWICE,

    /** A real clash a person must look at: the later row goes to Changes to review. */
    REVIEW,

    /** A login name created offline on two computers: the later user is renamed with a suffix and the admins are told. */
    RENAME_LATER,

    /** The table is never synced. */
    LOCAL,
}

data class UniqueRule(val database: Database, val table: String, val columns: List<String>, val resolution: Resolution) {
    val key: String get() = "$database.$table(${columns.joinToString(",")})"
}

/** Every UNIQUE constraint and unique index of both schemas (not primary keys), with its resolution. */
object UniqueResolutions {

    val rules: List<UniqueRule> = buildList {
        fun core(table: String, vararg columns: String, r: Resolution) = add(UniqueRule(Database.CORE, table, columns.toList(), r))
        fun ledger(table: String, vararg columns: String, r: Resolution) = add(UniqueRule(Database.LEDGER, table, columns.toList(), r))

        // --- Core database ------------------------------------------------------------------------
        core("app_user", "login_name", r = Resolution.RENAME_LATER)
        core("tag", "name", r = Resolution.MERGE_INTO_EARLIER)
        // Built-in categories have name-based ids from their system key (Ids.nameBased).
        core("category", "system_key", r = Resolution.SAME_ROW)
        // One budget per category, saved with ON CONFLICT(category_id).
        core("budget", "category_id", r = Resolution.SAME_ROW)
        // A group's partition is made with the group; two groups never share one.
        core("account_group", "partition_id", r = Resolution.SAME_ROW)

        // --- Ledger databases ---------------------------------------------------------------------
        ledger("bill_occurrence", "bill_id", "due_date", r = Resolution.SAME_ROW)
        ledger("contact_link", "contact_id", "role", "target_type", "target_id", r = Resolution.SAME_ROW)
        ledger("txn", "account_id", "external_id", r = Resolution.IMPORTED_TWICE)
        ledger("inv_txn", "account_id", "external_id", r = Resolution.IMPORTED_TWICE)
        ledger("statement", "account_id", "file_sha256", r = Resolution.IMPORTED_TWICE)
        ledger("invoice", "number", r = Resolution.REVIEW)
        ledger("card_statement", "account_id", "statement_date", r = Resolution.REVIEW)
        ledger("tax_slip", "account_id", "year", "kind", "security_id", r = Resolution.REVIEW)
        ledger("room_entry", "member_id", "plan", "year", r = Resolution.REVIEW)
        ledger("estate_record", "member_id", r = Resolution.REVIEW)
    }

    fun of(database: Database, table: String, columns: List<String>): UniqueRule? =
        rules.firstOrNull { it.database == database && it.table == table && it.columns == columns }
}
