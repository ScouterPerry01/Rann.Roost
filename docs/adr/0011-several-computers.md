<!--
File path and name: docs/adr/0011-several-computers.md
Modified On Timestamp: 2026-10-25 @ 15:20 EDT
Created On Timestamp: 2026-10-25 @ 12:15 EDT
File Description: ADR 0011: one household used on several computers (release 2.0.0).
Uses: ADR 0002 (storage and keys), ADR 0005 (backups), ADR 0006 (phone pairing), requirements MD-01 to MD-16.
Used By: docs/plan-multi-device.md; the 2.0.0 work; later ADR addenda.
Purpose: Record why 2.0.0 keeps a full copy on each computer and exchanges signed, encrypted changes.
-->

# ADR 0011: One household on several computers

Status: Accepted (2026-10-25), subject to the phase 0 spike's go/no-go (`docs/plan-multi-device.md` §11).
Not built.

## Context

- In a family, most members have their own laptop. Using one person's computer for the household is often not
  possible (owner, 2026-10-25).
- The owner found that a household folder can be placed anywhere the user can reach, including a network share
  or a cloud folder. The code does not make that safe:
  - WAL mode and `busy_timeout` (`SqlCipherJdbcDriverFactory.kt`) protect threads in one process only.
  - Nothing stops a second computer from opening the same folder.
  - SQLite's WAL does not work over network file systems, and cloud folders sync each file on its own with no
    locking. Two computers writing at once can corrupt a database or lose changes. Even taking turns can open
    a stale copy.
- The fixed constraints are:
  - no RANN server or account (ARC-01);
  - per-user encryption of private groups (HH-11, ADR 0002);
  - offline use;
  - one developer;
  - a paid desktop app with a free update promise.

## Decision

**Each computer keeps its own full copy of the household on its own disk, and computers exchange changes, not
database files.** Released as 2.0.0, a free update. The details are in `docs/plan-multi-device.md`.

- **Change capture:** generated SQLite triggers record every insert, update and delete into an outbox. No
  service code has to remember to log. Each committed transaction becomes one change set, stamped with a
  hybrid logical clock.
- **Merging:** every table has a declared policy:
  - field-level latest-wins;
  - parent with children replaced together;
  - append-only;
  - counter;
  - deterministic-id idempotent rows;
  - local-only.

  Deletes leave tombstones. Unique clashes have resolvers. Money fields changed on two computers, and
  additions to deleted records, go to a Changes to review list. Tests fail when a table has no policy or a
  unique constraint has no resolver.
- **Transport:**
  - **A shared folder** the family already has (cloud, network share, USB). Each computer writes only its own
    files, write-once, so the folder never holds conflicting edits.
  - **Home Wi-Fi** between computers: mDNS discovery and a one-time code on first contact, carrying the same
    files.
- **Security:**
  - Change files are encrypted under the group key and signed by the user (new Ed25519 signing key per user).
  - Users, groups and grants become signed key-ring entries that every computer merges; `household.json` is
    rebuilt from them.
  - A received change is applied only if its author had the permission.
  - Group key rotation provides real revocation of a computer.
- **Identity:** each copy has a local `device.json` and local-only settings. Restoring a backup creates a new
  device.
- **Phones:** unchanged. A phone stays paired with the computer it was paired with, several phones per
  computer, each under its own user (HH-12). The phone repository needs no change; new code lives in
  `core/replication`.
- **Upgrade:** the first open by 2.0.0 backs up and converts an existing household wherever it is. A household
  found in a cloud or network folder is moved to the local disk, and that folder becomes the shared folder.

## Rejected options

- **Shared database on a network drive or cloud folder** (what happens today by accident): unsafe for SQLite
  across computers, and useless away from home.
- **A household lock file only:** prevents damage on a network share, but it allows one computer at a time,
  fails on cloud folders (the lock itself is delayed by syncing), and gives no offline use. The owner chose not
  to ship it in 1.x.
- **RANN-hosted server:** breaks the no-server promise. It also means 24/7 operation and privacy-law
  obligations for a solo developer, and a recurring cost against a one-time price.
- **A server the family runs (NAS, home server):** setup and support most families cannot manage.
- **One main computer, other laptops as remote screens:** simpler (one database, no merging), but the main
  computer must be on and everyone at home. It gives no offline use on laptops.

## Consequences

- The data layer grows, and every future table needs a sync policy. Migrations must be deterministic, and
  seed and automatic rows must use deterministic ids. These become `CLAUDE.md` rules when phase 1 lands.
- 1.x cannot open a household converted by 2.0.0. The pre-upgrade backup is the way back.
- Metadata in the shared folder (group and device ids, sizes, times) is visible to whoever can see the folder.
  No names, amounts or text are visible.
- A computer that already held a group key can read what it had until the key is rotated, as with any grant
  (ADR 0002).
- Addenda follow during the work: ADR 0002 (key ring, signing keys, rotation), ADR 0005 (device file not
  backed up; restore creates a new device), ADR 0006 (phones per computer, computer-to-computer Wi-Fi). The
  SRS's ARC-01 gets a note that the household is replicated, not hosted.
- Rough size: about 8–11 months for one developer working with Claude, most of the risk in capture, merge,
  key ring and Wi-Fi.

## Phase 0 spike (2026-10-25)

**Proven** by `ReplicationSpikeTest` in `core/data-jdbc` (10 tests, run on Windows with the project's
SQLite3 Multiple Ciphers build 3.53.4.0, SQLCipher v4 mode, real ledger and core schemas):

- **Triggers install on every table.** The generator reads `PRAGMA table_info` and creates 381 triggers on
  the 127 ledger tables and 57 on 19 of the 21 core tables. The spike skipped every name starting with
  `sync_`, and the core database already has `sync_device` and `sync_item` (phone pairing). Those two stay
  local-only anyway, but **the new bookkeeping tables need their own prefix (`repl_`) and the generator an
  explicit list**, not a name filter. Every table has a primary key. The widest table is `vehicle` (41 columns). There is one BLOB column, `app_user.public_key`; it is
  carried as hex and restored with `unhex`.
- **The JSON functions work on encrypted files.** An update records only the columns that changed, and an
  update that changes nothing records nothing.
- **Cascaded deletes fire the children's triggers** (`txn` → `txn_split`).
- **Upserts are captured correctly.** An `ON CONFLICT DO UPDATE` that hits an existing row is captured as
  an update only.
- **Replay works.** Replaying the outbox into a second, empty copy, with SQL generated from the schema only,
  gives identical tables. The test covered inserts, updates to NULL, deletes with cascades, an upsert,
  composite keys, accents and quotes.
- **The "applying remote changes" switch must live in the file, not in a temp table.** A trigger stored in
  the file cannot read a temp table ("no such table: main.ctx"). The switch is therefore a row in a table of
  the file, inserted at the start of the apply transaction and deleted before its commit. SQLite allows one
  writer at a time, so no other connection can write while the row exists. The test with two threads
  confirms it: the screen's write waited for the apply, and it was captured, while the applied row was not.
- **The clock works.** The hybrid logical clock stays ordered when the computer's clock goes back, and
  moves past a clock received from another computer.

**Cost of capture:** 250,000 transactions with their splits in one transaction took 6.7 s without capture
and 24.3 s with capture (about 3.6 times). That is 500,001 changes, about 300 bytes each before compression.

- Ordinary saves are a few rows, so the extra time does not show.
- A 10,000-line statement import costs about 1 s extra.
- Converting an existing household must not capture its existing rows. It stamps a base clock and starts
  from a snapshot instead (plan §8 step 5), which this result confirms is necessary.
- Phase 2 should try one outbox row per statement, rather than per row, for bulk imports, if needed.

**Not yet proven (they need two computers or two Windows accounts, and are owner-run):**

- The shared-folder round trip through OneDrive, Google Drive, Dropbox and a network share. Use
  `tools/dev/folder_sync_probe.py` (procedure in `tools/dev/README.md`), with both computers writing and
  reading at the same time.
- Network discovery (mDNS) on Windows and Linux. No library has been chosen yet. JmDNS (Apache 2.0, works
  with GPL-3.0) is the first candidate. The check moves to the start of phase 6, and typing the address by
  hand stays the fallback.

**Go/no-go:** **go** for change capture with generated triggers (phases 1–3). The folder transport (phase 4)
waits on the probe results, and Wi-Fi discovery (phase 6) is checked when that phase starts.
