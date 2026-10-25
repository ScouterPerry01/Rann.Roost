<!--
File path and name: docs/adr/0011-several-computers.md
Modified On Timestamp: 2026-10-25 @ 12:15 EDT
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
