<!--
File path and name: docs/plan-multi-device.md
Modified On Timestamp: 2026-10-25 @ 20:55 EDT
Created On Timestamp: 2026-10-25 @ 11:10 EDT
File Description: Detailed plan for RANN's Roost 2.0.0: one household shared by several computers, upgrade of
  existing households, and the real-property details.
Uses: docs/adr/0011-several-computers.md; docs/spec-real-property.md; docs/adr/0002, 0005, 0006, 0008; the SRS;
  docs/requirements-additions.md (MD-01 to MD-16, AST-06 to AST-17); facts gathered from the code.
Used By: The owner and Claude Code when building 2.0.0.
Purpose: The single build plan for 2.0.0: what is built, in which order, how it is tested and released.
-->

# Plan: RANN's Roost 2.0.0

**Status:** approved in principle by the owner on 2026-10-25. Nothing in it is built or tested yet. Facts
about today's code were gathered on 2026-10-25 and are cited by file.

The decision itself is **ADR 0011** (`docs/adr/0011-several-computers.md`). The requirements are
**MD-01 to MD-16** and **AST-06 to AST-17** in `docs/requirements-additions.md`. The real-property fields are
specified in `docs/spec-real-property.md`.

## 1. Owner's decisions (2026-10-25)

1. **One release: 2.0.0.** No 1.2.0 or 1.3.0. Version 1.1.2 stays as it is until 2.0.0 replaces it.
2. **2.0.0 is a free update** for existing buyers, on the same Store listing.
3. **Phones stay paired with the computer they were paired with.** One computer can have several phones, each
   paired under a separate user account.
4. **Documents are copied to every computer by default.** A computer can be set to keep documents only on the
   main computer (small laptops).
5. **Home Wi-Fi sync between computers is part of 2.0.0**, alongside the exchange folder.
6. **Real property collects as much information as possible:** address, legal identifiers, land, building,
   rooms, systems, basement, garage, pool, outbuildings, condo, value and tax, owners, custom fields
   (`docs/spec-real-property.md`).
7. No household lock in 1.x (there is no 1.x release). 2.0.0 includes its own guard (section 5.7).
8. **The first computer is the main computer for automatic jobs.** The role can be transferred, or taken over
   from another computer when the main computer has problems (section 4.6).
9. **Children's and other restricted users' own computers** receive only the groups granted to that user, as
   on a shared computer today.
10. **Principal-residence years are kept for every real-property asset** (all five property kinds, AST-17,
    raised to Should).

## 2. Goals and what does not change

**Goals**

- G1. Each family member uses RANN's Roost on their own computer. Everyone sees the same household, works
  offline, and the copies come back in step on their own: through a shared folder or over home Wi-Fi.
- G2. Nothing is lost when two people change things at the same time. Real conflicts are shown to a person.
- G3. Existing users upgrade in place: 2.0.0 opens their existing household folder, wherever it is, backs it
  up and converts it. No re-entry, no export or import.
- G4. Real property records everything about a property (decision 6).

**Kept exactly as today**

- No RANN server, no RANN account, no tokens held by RANN (ARC-01, ADR 0006).
- Per-user privacy: a private group stays unreadable without its key, on every computer, in every shared
  folder and over Wi-Fi (HH-11, ADR 0002).
- The phone app (Rann.Roost.Mobile) needs no change and no new release for 2.0.0 (section 7).
- Single-computer households keep working as now. Sharing is opt-in.

## 3. Facts from today's code that shape the plan

Good news (makes this feasible):

- **Every row id is a TEXT UUIDv7** (`core/domain/.../Ids.kt:16`). There is no INTEGER or AUTOINCREMENT key
  anywhere.
- **Balances and totals are calculated**, never stored. There are no triggers and no views.
- **Schema changes are additions only**, so older changes always fit a newer schema.
- **Document vault files are written once.** They are sealed with a group-derived key and bound to
  household, group and document (`DocumentVault.kt`), so they can be copied between computers as they are.
- **Upgrades already back up first** and refuse newer formats (`Schemas.kt:27-64`, `HouseholdHeader.kt`).
- **Phones already belong to a user.** `sync_device.user_id` (`Core.sq:53-65`) holds the user the phone
  captures for (HH-12). Several phones per computer already work. A phone syncs only while its user is
  signed in on that computer (`OwnerAwayException`, `SyncService.kt:203`).

Problems the plan must solve:

| # | Fact | Why it matters | Solved in |
| --- | --- | --- | --- |
| P1 | No central write path: about 50 of 73 `core/books` files call SQLDelight writes directly | Changes cannot be captured in one place in Kotlin | 4.1 |
| P2 | `change_log` covers transactions only; `audit_log` is free text | Neither can replay changes | 4.1 |
| P3 | 127 ledger + 21 core tables, 21 composite keys, natural keys (`setting.key`, account-keyed tables) | Every table needs a merge rule | 4.3 |
| P4 | About 251 hard deletes, 86 ON DELETE CASCADE | A delete must not be undone by an older change | 4.4 |
| P5 | UNIQUE constraints two computers can both hit (`tag.name`, `category.system_key`, `txn_external`, `invoice.number`, `bill_occurrence (bill_id, due_date)`…) | Applying the second copy fails | 4.5 |
| P6 | `categories.ensureDefaults()` runs at every open (`Books.kt:159`) with random ids | Duplicate default categories | 4.5 |
| P7 | Automatic writes: auto-posted goal entries, loan postings, bill and event occurrences, scheduled reports, price downloads | Each computer would post them: duplicates | 4.6 |
| P8 | Counters (`merchant_memory.uses = uses + 1`) | "Latest wins" loses increments | 4.3 |
| P9 | `sync.desktopId`, its private key and the transfer folder live in household settings (`SyncService.kt:109-114`) | Every copy would claim to be the paired desktop | 5.1, 7 |
| P10 | `household.json` is rewritten whole; user, group and grant changes touch it and `core.db`, mostly not in one transaction | Key-ring changes must merge, not overwrite | 4.7 |
| P11 | Services check permissions; applying another computer's changes bypasses them | A member could forge changes beyond their rights | 6.3 |
| P12 | No per-computer identity exists | Change files need an author computer | 5.1 |
| P13 | MSIX, deb, rpm and AppImage cannot run code at install time | Conversion happens on first open | 8 |
| P14 | `insertDevice` uses `INSERT OR REPLACE INTO sync_device` (`Core.sq:464`) while `sync_item` refers to devices | Breaks the project's own rule; re-pairing could drop received-item records | 7 |
| P15 | The Wi-Fi listener has no discovery: the address goes into the QR code (`SyncServer.kt:147-157`) | Computers must find each other | 5.6 |

## 4. Change capture and merging

### 4.1 Capturing every change (P1, P2)

- Changes are captured **inside SQLite with generated triggers**, not in the 50 Kotlin services.
- **Generator:** at open time it reads `PRAGMA table_info` and `PRAGMA index_list` and creates three triggers
  per synced table: AFTER INSERT, AFTER UPDATE (only the columns whose value changed) and AFTER DELETE.
- **Outbox:** the triggers append to a local `repl_outbox` table: table, key (JSON of the key columns),
  kind, changed columns as JSON (`json_object`), local sequence.
- **Skipping capture:** while remote changes are applied or migrations run, a row sits in the file's
  `repl_apply` table. It is inserted at the start of that write transaction and deleted before the commit.
  The triggers skip capture while the row exists, and SQLite's single writer keeps other connections from
  writing meanwhile. A temp table cannot be used: triggers stored in the file cannot read temp tables (spike).
- **Who and when:** Kotlin adds the signed-in user, the computer and the clock when it seals the outbox rows,
  so the triggers do not need them.
- **Change sets:** after each committed transaction, Kotlin stamps the outbox rows with the clock (4.2) and
  seals them into one change set. One transaction stays one unit on every computer.
- **History:** `change_log` and `audit_log` stay as the user-facing history, synced as append-only tables.
  Applied changes are audited under their author, with the author's computer.
- **The generator works from an explicit list** of the tables in the policy manifest (4.3), never from a
  name filter. The core database already has `sync_device` and `sync_item`, so new bookkeeping tables are
  named `repl_*`.
- **Spike results (2026-10-25):** see ADR 0011 § Phase 0 spike. Capture works on every table. Its cost is
  about 3.6 times on a bulk load of 250,000 transactions, and no visible difference on ordinary saves.

### 4.2 Clock

- A **hybrid logical clock (HLC)**: wall time in milliseconds + counter + device id. It orders correctly with
  a wrong laptop clock and is unique across computers.
- Stored as fixed-width TEXT so it sorts as a string. A change dated more than 24 hours ahead is held, with a
  warning.

### 4.3 Merge policy per table (P3, P8)

`core/replication` holds a **policy manifest** listing every table of both schemas with one policy. A test
fails when a table has none, as `MessagesTest` does for texts.

| Policy | Meaning | Examples |
| --- | --- | --- |
| `FIELD_LWW` (default) | Each column keeps the value with the latest clock; edits to different fields both survive | account, payee, category, asset, real_property, contact, pet |
| `AGGREGATE(parent)` | Children belong to a parent; parent and children are replaced together by the latest change set | txn_split, txn_tag, txn_sales_tax under txn; txn_template_line; statement_line |
| `APPEND_ONLY` | Rows are only added; merge is the union | audit_log, change_log, ai_usage, goal_entry, reward_entry, readings, logs |
| `COUNTER` | Each computer keeps its own count; the value shown is the sum | merchant_memory.uses |
| `IDEMPOTENT` | Deterministic ids, so the same row from two computers is one row (4.6) | bill_occurrence, event_occurrence, auto-posted entries, fx_rate, spot_price, security_price |
| `LOCAL_ONLY` | Never synced | sync_device, sync_item, device settings (5.1), repl_* tables |

**Flagged fields:** for money-critical fields (transaction amount, date, account and splits; budget amounts;
loan terms) the latest change wins. When the losing change was made before the two computers had seen each
other's change, the losing value is also added to **Changes to review** (6.2).

### 4.4 Deletes (P4)

- A delete writes a **tombstone** (table, key, clock) in `repl_tombstone`. An older insert or update of that
  row is then ignored.
- Cascades still run locally on each computer, and the child deletes are idempotent.
- **A delete against an offline addition** (Anne deletes an account while Perry adds a transaction to it):
  the delete wins. Perry's transaction goes to **Changes to review** with "Restore the account" and
  "Discard", and is never silently lost.
- Tombstones are pruned once every active computer has seen them (5.4).

### 4.5 Unique values and built-in data (P5, P6)

- **Deterministic ids for built-in rows:** default categories and every seed row get
  `id = UUIDv5(householdId, system_key)`, so `ensureDefaults()` makes the same row everywhere. Existing rows
  keep their ids. A joining computer loads the snapshot before `ensureDefaults()` runs.
- **Resolvers for a unique clash while applying:**
  - `tag.name` and payee duplicates: merge into the earlier row, re-point references, audit the merge.
  - `txn_external`, `inv_txn_external`, `statement_file` (the same file imported on two computers): keep the
    earlier rows; the later ones go to **Changes to review** as "imported twice".
  - `invoice.number`, `card_statement (account_id, statement_date)`, `tax_slip_key`, `room_entry_key`,
    `estate_record.member_id`: go to **Changes to review**.
  - `app_user.login_name`: reserved through the key ring (4.7). A clash after offline creation renames the
    later user with a suffix, and the admins are told.
- A test forces a clash on every UNIQUE constraint of both schemas and fails if one has no resolver.

### 4.6 Automatic writes (P7)

- Every row the app creates **by itself** gets a deterministic id from what it represents, e.g.
  `UUIDv5(goalId, date)`. This covers goal contributions, loan postings, bill and event occurrences,
  scheduled-report runs, and downloaded prices and rates.
- Cursor columns (`savings_goal.last_posted`, `loan.last_paid_date`) merge with **max**.
- Scheduled reports, e-mails and downloads run only on the **main computer for automatic jobs**.
- **Who holds the role:**
  - The first computer of the household holds it (owner, 2026-10-25).
  - The role is one signed key-ring entry ("main computer = device X", with its clock). The latest entry wins
    on every computer.
- **Transfer** (Computers › Make this the main computer, on the current main computer or by an
  administrator anywhere): the new entry is written, and the old main computer stops its jobs as soon as it
  sees it.
- **Take over** when the main computer has trouble:
  - An administrator on another computer can take over the role at any time. The screen warns when the main
    computer was seen recently.
  - After 3 days without the main computer being seen, every computer shows "The main computer has not been
    seen since … Take over?".
  - If the old main computer comes back, it reads the newer entry and stops its jobs; it never takes the role
    back by itself.
- **Overlap is harmless:**
  - Postings and downloads use deterministic ids (above), so a job run on two computers during a hand-over
    still creates each row once.
  - Each scheduled e-mail run is recorded with a deterministic id before sending, and a computer skips a run
    already recorded. A run that both computers start before either sees the other's record could still be
    sent twice. That is accepted, and shown in the history.
- A test lists every write made without a user action and checks it uses a deterministic id.

### 4.7 Key ring: users, groups, grants (P10)

- `household.json` becomes a **view rebuilt from key-ring entries**: user added, public keys, group created,
  grant added or removed, role changed, login name reserved.
- Each entry is immutable and **signed** by the admin who made it, with a new per-user **Ed25519 signing key**
  wrapped like the private key today (password, recovery key, Windows Hello).
- Entries travel under `keyring/<deviceId>/` (folder) or the same records over Wi-Fi. They merge by union;
  a removal is a tombstone entry.
- A computer accepts an entry only if its signer was an admin at that clock in its own merged key ring. ADR
  0002's rule holds: grants are sealed to public keys from authenticated data, never from an editable file.
- `household.json` stays on disk (atomic write, `.bak`) so unlock works before `core.db` opens.
- **Key rotation** ("Rotate this group's key", admins only) is built in 2.0.0: it re-encrypts a group under a
  new key and re-grants it. This is true revocation for a lost or departing laptop.

## 5. Computers, transports, joining

### 5.1 Device identity (P9, P12)

- Each replica has a local `device.json` next to `household.json`. It is never synced and never backed up.
  It holds `deviceId` (UUIDv7), the computer name and the next sequence number per group.
- Device-specific settings (`sync.desktopId`, `sync.privateKey`, `sync.transferFolder`, backup folder,
  window state, "documents only on the main computer") move to a `LOCAL_ONLY` table. `SyncService.identity()`
  reads the local table.
- **Restoring a backup creates a new deviceId**, so sequence numbers are never reused.

### 5.2 Change-set format (shared by all transports)

- A magic line `ROOSTCHG1`, then a plain JSON header (format, householdId, partitionId, deviceId, userId,
  seq, first and last HLC, schema versions), then the body.
- The body is gzip JSON, sealed with AES-256-GCM under `HKDF(partitionKey, "hfm-changes-v1")`. The AAD binds
  household, group, device and seq.
- An Ed25519 signature by the user covers the header plus the ciphertext.
- This reuses `SyncCrypto` / `Aead` and the `BundleFile` container pattern.

### 5.3 Exchange folder

```
<chosen folder>/RANN Roost/<householdId>/
  README.txt                                          what this is; "do not edit or move files"
  keyring/<deviceId>/<seq>.roostkey                   signed key-ring entries
  changes/<partitionId>/<deviceId>/<seq>.roostchg     change sets
  snapshots/<partitionId>/<deviceId>-<hlc>.roostsnap  database copies for joining
  vault/<partitionId>/<documentId>.hfmdoc             documents, copied as they are
  devices/<deviceId>.json                             name, versions, how far it has read
```

- **Single-writer rule:** a computer writes only its own `<deviceId>` paths, through `.part` then a rename,
  and never changes a finished file.
- **Works with** OneDrive, Dropbox, Google Drive, Nextcloud, iCloud Drive, a NAS or Windows share, and a USB
  stick carried between computers.
- **Shared between accounts, not one shared sign-in:** family members keep their own cloud accounts. One
  person shares a folder from their OneDrive, Google Drive or Dropbox with each member's own account, and each
  member adds it to their storage (OneDrive: **Add shortcut to My files**; Google Drive: add a shortcut to My
  Drive; Dropbox: accept the shared folder). A NAS or Windows network share works the same way, each person
  with their own login. A household that wants no folder at all uses home Wi-Fi instead (5.6). Shortcut folders
  and files on demand are where OneDrive is most likely to misbehave, so the phase 0 folder probe is run this
  way: the second computer signed in to **another** member's account, with the folder shared to it.
- **Files on demand** (OneDrive placeholders, Google Drive streaming): the reader opens the file to force a
  download and retries later. A file that is not available never fails the whole sync.
- **Visible metadata:** group ids, device ids, sizes and times (no names, amounts or text). The manual says so.
- The watcher reuses `TransferFolder.kt` patterns: a 20-second poll plus a file-system watch, atomic
  writes, and `Not imported/` for unreadable files.

### 5.4 Snapshots, pruning, inactive computers

- A snapshot is the group's database copied with `VACUUM INTO`. It is still encrypted under the group key
  and includes the sync tables and the HLC it covers.
- A computer writes a new snapshot of a group every 5,000 changes or monthly.
- A computer deletes **its own** change files once every active computer has read past them and a newer
  snapshot exists. It keeps its last two snapshots.
- A computer not seen for 90 days becomes inactive and must rejoin from a snapshot. Its unsent changes are
  offered for review when it returns.

### 5.5 Joining a computer

- Install 2.0.0, choose **Join a household**, then either pick the exchange folder or choose **Find on home
  Wi-Fi** (5.6). Sign in with your **own** password.
- The app reads the key ring, unwraps your keys, loads the newest snapshot of each group you hold a key for,
  applies newer changes, then copies the documents (unless set to keep documents on the main computer).
- No admin step is needed: knowing a user's password already gives that user's keys (as today).
- A second user who later signs in on the same computer gets their extra groups the same way.

### 5.6 Home Wi-Fi between computers (P15; in 2.0.0)

- The existing `SyncServer` (JDK `HttpServer`, ports 47311–47320) gains computer-to-computer endpoints under
  `/hfm/v2/`:
  - `hello`: household id, device id, app and schema versions; nothing secret.
  - `pull`: change sets after a given position, per group.
  - `push`: offer change sets.
  - `snapshot`, `doc`, `keyring`.

  Each endpoint carries the same sealed and signed files as the folder (5.2), so Wi-Fi and the folder are
  interchangeable and can be used together.
- **Discovery:** mDNS / DNS-SD service `_rannroost._tcp` on the local network, announcing only the household
  id and device id. Typing an address by hand is the fallback.
- **Trust between computers:** the first contact is approved once on both screens with a 6-digit code (the
  same pattern as the phone's one-time code, ADR 0006). After that, each computer pins the other's device key
  and every request is authenticated with it. The content needs no extra protection from the connection:
  every file sent is already sealed under its group key and signed (5.2). Phase 0 confirms how this meets
  SEC-06 ("all network traffic uses TLS") in the same way the phone listener does today, and adds TLS with the
  pinned keys if it does not.
- **What is sent:** a computer serves a group's changes only to a device whose signed-in user holds that
  group's key. It sends ciphertext either way, but this also limits metadata.
- **Firewall:** Windows asks once to allow private-network connections, as it does for the phone today.
- **Linux, Wi-Fi with client isolation, or a guest network:** the folder still works; the screen explains why
  Wi-Fi could not connect.
- **Phones keep their own endpoints** (`/hfm/v1/pair`, `/hfm/v1/sync`) unchanged.

### 5.7 Guard against opening one replica from two computers

A replica belongs on its own computer's disk. If 2.0.0 opens a household whose folder is on a network or cloud
path, or finds a `replica.lock` with a live heartbeat from another computer, it refuses to write. It offers
"Make a copy for this computer" (join), so a replica is never opened by two computers at once.

### 5.8 Versions on different computers

- Each change set carries its schema versions. A computer applies changes up to its own version and holds
  newer ones, with a banner: "Anne's laptop uses a newer RANN's Roost. Update this computer to see her latest
  changes."
- **Code rule:** migrations are deterministic and never create rows with random ids. Each computer runs them
  itself with capture off.

## 6. Applying changes, conflicts, permissions

### 6.1 Apply engine

For each group, in clock order:

1. Check the signature, the format and the permissions (6.3).
2. Open a database transaction and set the "applying" flag.
3. Merge each row by its policy, against `repl_clock (table, pk, column, hlc)` and `repl_tombstone`.
4. Park failures (missing parent, unique clash) in `repl_conflict`.
5. Commit and record the read position.

The engine is deterministic: the same files give the same database in any order and with duplicates.

### 6.2 What the user sees

- **Status in the top bar:** Up to date · Sending 3 changes · Not synced for 4 days · Offline.
- **Computers screen:** each computer, its users, app version, last seen and how (folder or Wi-Fi). Actions:
  Remove, Rotate keys, Make this the main computer (transfer or take over, 4.6), keep documents only on the
  main computer.
- **Changes to review:** a plain sentence per item, e.g. "Perry changed the amount to $42.10 at 19:02; Anne
  changed it to $24.10 at 19:05 on her laptop. Kept: $24.10." Actions: Keep, Use the other, Open.
- **History:** the audit log shows who changed what, and on which computer.
- **Sharing wizard** (Settings › Share with other computers): choose a folder, Wi-Fi or both; write the
  first snapshots; show how to join from another computer.
- EN and FR texts, a manual chapter, and Walk-Me guides for "Share with another computer", "Join a household"
  and "Review changes".

### 6.3 Permissions and safety (P11)

- For each row, the engine checks that the signing user had **edit** permission on that group at that
  clock, from the merged key ring and `group_permission`.
- Changes to `app_user`, `account_group`, `group_permission` and household settings are accepted from admins
  only. Refused rows are logged and shown to admins.
- Constraints and foreign keys stay on while applying.
- A security review (as in `docs/security-review-*.md`) is part of the exit check.

## 7. Phones (decision 3; Rann.Roost.Mobile unchanged)

- A phone stays paired with **the computer it was paired with**. Its `sync_device` row becomes `LOCAL_ONLY`
  on that computer, and so do the computer's phone identity and transfer folder (5.1). Other computers do
  not answer that phone.
- **Several phones per computer**, each paired under its own user account (HH-12, as today). A phone syncs
  when its user is signed in on that computer. When another user is signed in, the phone gets "owner away"
  and retries, as today.
- The phone's captures become normal changes on that computer, so they reach the other computers.
  Reference data sent to the phone (`ReferenceData` FORMAT 7) is built from the computer's merged replica, so
  it includes what other computers entered.
- `SyncContracts.kt` and `SyncClient.kt` do not change, so the phone's `roost` submodule pin does not have to
  move. New code goes in `core/replication`, which the phone does not build.
- **Fix P14:** `insertDevice` becomes `INSERT … ON CONFLICT(id) DO UPDATE`, covered by `SaveKeepsChildrenTest`.
- **Pairing screen:** shows which user each phone belongs to and states that the phone syncs with this
  computer only.
- If that computer is retired, the phone is paired again with another computer (Phones › Pair). Captures
  already imported are safe in the household.

## 8. Upgrading existing households (G3, P13)

The installers cannot run code, so **the conversion runs the first time 2.0.0 opens a household**, as schema
upgrades do today:

1. **Open as now, or point to the folder.** The user opens the household from the recent list or with
   **Open**, pointing to wherever the folder is: local disk, external drive, network share or cloud folder.
   **Restore** of a 1.x `.hfmbak` also works and converts after restoring.
2. **Full backup first.** A complete `.hfmbak` goes next to the existing pre-upgrade copies. The screen says
   where it is and that 1.x cannot open the converted household.
3. **Schema.** The migrations of the 2.0 batch run: sync tables (`repl_outbox`, `repl_clock`,
   `repl_tombstone`, `repl_conflict`, `repl_local_setting`, `repl_peer`), the real-property tables, and the new
   asset kinds. They run on `core.db` and on every ledger the user can open. Ledgers the user cannot open are
   converted when their owner next signs in, and the household stays single-computer until all are converted.
4. **Identity and key ring.** Device settings move to the local table, `device.json` is created, and the key
   ring is built from `household.json`, signed by the first admin to sign in.
5. **Base clock.** Existing rows receive a base clock so the first snapshot is consistent.
6. **Household in a cloud or network folder** (detected by path and provider markers, such as the folder
   used in the owner's experiments): the app explains the risk and offers **Move the household to this
   computer and use that folder to share it**. It then:
   - copies the household to the local Documents folder and verifies it (backup `verify`);
   - converts it;
   - makes the old location the exchange folder;
   - moves the old files into a `Replaced by RANN Roost 2.0` subfolder (never deletes them).
7. **Single-computer users see nothing more.** Sharing starts from Settings whenever they want.

**Upgrade tests** run from households made by every released version: 1.0.0, 1.1.0, 1.1.1 and 1.1.2.

## 9. Real-property details (G4)

Specified field by field in `docs/spec-real-property.md` (AST-06 to AST-17):

- **Kinds:** HOME and COTTAGE, plus new CONDO, LAND and BUILDING.
- **Tables:** `real_property` and its tables for rooms, systems, outbuildings, features, owners, residence
  years and custom fields.
- **Screen:** a Property section with tabs in the asset dialog.
- **Links:** property-tax bill, mortgage, insurance replacement cost, rentals, child assets for
  maintenance.
- **Report:** a printable **property fact sheet**.

These tables are part of the same 2.0 migration batch and get sync policies from day one. Build them early
(phase R): they do not depend on sync, and they give the owner something to review while the sync engine is
built.

## 10. Testing

- **Capture completeness:** run the `core/books` test suites with capture on, replay the outbox into an empty
  replica, and compare every table. They must be identical.
- **Convergence fuzzing:** three simulated computers, random service calls, random delivery order, duplicates
  and delays. Every replica must end identical, and nothing may be lost without a `repl_conflict` row.
- **Rule tests:** every table has a policy; every UNIQUE constraint has a resolver; every automatic write
  uses a deterministic id.
- **Security:** each of these is refused: forged signature, wrong group key, key-ring entry by a non-admin,
  edit without permission, replayed or renamed file, future clock, unknown device over Wi-Fi, wrong pairing
  code.
- **Wi-Fi:** two instances on one machine (two ports) in CI; discovery and pairing by hand on two real
  computers.
- **Upgrade:** from every released version, including private groups the opener cannot read, and the
  "move off the cloud folder" path.
- **Performance:** `:core:books:performanceTest` with capture on; a 10,000-row statement import; applying
  50,000 changes on join; NFR-02 targets unchanged.
- **Real property:** see the spec's section 5.
- **Phones:** two phones under two users on one computer; a phone keeps working after its computer joins a
  shared household; its captures reach a second computer.
- **Family test before release:** two Windows laptops and one Linux computer, different users. Sync through
  OneDrive, Google Drive, Dropbox, a Windows share, and Wi-Fi. Cover a laptop offline for two weeks, a wrong
  clock, killing the app mid-write, mixed app versions (beta and older beta), removing a computer, and
  rotating a key.

## 11. Phases (one release, 2.0.0)

All work is on topic branches merged to `main` after green CI (branch policy). `main` is not released until
phase 10, because `release.yml` builds from a tag only and the Store only gets what is submitted.

| Phase | Content | Exit check | Rough size |
| --- | --- | --- | --- |
| 0 Spike | Triggers in SQLCipher, JSON functions, cascades, cost on NFR-02 data; HLC; one change file through OneDrive between two Windows accounts; mDNS on Windows and Linux | written go/no-go added to ADR 0011 | 1–2 weeks |
| R Real property | Spec `docs/spec-real-property.md`: schema, service, dialog tabs, fact sheet, EN/FR, demo data, manual | build green; owner's review in the demo | 4–6 weeks |
| 1 Foundations | `device.json`, LOCAL_ONLY settings, deterministic ids for seeds and automatic rows, policy manifest and rule tests, P14 fix | no behaviour change; all tests green | 2–3 weeks |
| 2 Capture | Sync tables, trigger generator, outbox, HLC, change sets | capture-completeness test green | 3–4 weeks |
| 3 Merge | Apply engine, policies, tombstones, resolvers, conflicts | fuzz tests green | 4–6 weeks |
| 4 Exchange folder | Formats, signing, writer and watcher, snapshots, join, pruning | two replicas in step through a folder | 3–4 weeks |
| 5 Key ring | Signing keys, entries, header rebuild, permission checks, key rotation | security tests green | 3–4 weeks |
| 6 Wi-Fi | `/hfm/v2` endpoints, mDNS, first-contact code, pinning | two computers in step over Wi-Fi, folder off | 3–4 weeks |
| 7 Desktop UI | Wizard, Join, status, Computers, Changes to review, phone screen notes, EN/FR, manual, Walk-Me | owner's walk-through | 3–4 weeks |
| 8 Upgrade path | Conversion, move off the cloud folder, backup format 2, upgrade tests | upgrade from every version | 2 weeks |
| 9 Beta | Family test (section 10); GitHub pre-release (Linux) and a Partner Center package flight (Windows) | no open data-loss bug | 4+ weeks |
| 10 Release 2.0.0 | ADR and SRS final, release notes, Store and rann.ca texts, release checklist | release checklist | 1 week |

Phase R can run alongside phases 0–1, and phases 4 and 6 can overlap once the change-set format (5.2) is
fixed. These are honest ranges for one developer working with Claude: **about 8–11 months to 2.0.0.** Phases
2–6 carry most of the risk, and the spike may change the estimate.

## 12. Documents and rules to update

- **Done with this plan:** ADR 0011; requirements MD-01 to MD-16 and AST-06 to AST-17 in
  `docs/requirements-additions.md`; `docs/spec-real-property.md`.
- **When phase 1 lands (rules become real):** add to `CLAUDE.md` that every new table needs a sync policy,
  migrations are deterministic, seed and automatic rows use deterministic ids, and the phone must not need
  `core/replication`.
- **During the phases:**
  - ADR 0002 addendum: key ring, signing keys, rotation.
  - ADR 0005 addendum: `device.json` is not backed up; a restore creates a new device.
  - ADR 0006 addendum: phones stay per computer; computer-to-computer Wi-Fi.
  - ARC-01 note in the SRS: the household is replicated, not hosted.
- **Phase 10:**
  - The manual, Walk-Me guides and Store listing (EN and FR).
  - The rann.ca texts, ready for Google Sites, with the four page addresses kept.
  - The release checklist: a multi-computer test.
  - Release notes for 2.0.0.

## 13. Open questions

None at present. The three questions of the first draft were answered on 2026-10-25 (section 1, decisions
8–10).
