<!--
File path and name: docs/session-handover-2026-10-25.md
Modified On Timestamp: 2026-10-25 @ 19:45 EDT
Created On Timestamp: 2026-10-25 @ 14:05 EDT
File Description: Handover for 2026-10-25: the plan for RANN's Roost 2.0.0, the phase 0 spike and phase R.
Uses: docs/plan-multi-device.md, docs/spec-real-property.md, docs/adr/0011-several-computers.md,
  docs/requirements-additions.md.
Used By: Claude Code when resuming; the owner.
Purpose: What was decided and found this session, and where to start next.
-->

# Session handover — 2026-10-25

## 1. Why

The owner found that family members each have their own laptop, and that a household folder can be placed
anywhere (network share, cloud folder). They asked how to handle several computers using it at once.

## 2. Findings about today's code (1.1.2)

- **Not safe for two computers at once.** WAL mode and `busy_timeout` (`SqlCipherJdbcDriverFactory.kt:27-34`)
  protect threads in one process only. There is no lock and no "in use" check.
- **SQLite and shared folders:** SQLite's WAL does not work over network file systems.
- **Cloud folders are unsafe even taking turns:** they sync each file of the household on its own.
- Details are in plan §3, from three code surveys. The main ones:
  - every id is a UUIDv7;
  - no stored balances, no triggers;
  - about 50 service files write directly, with no central write path;
  - `change_log` covers transactions only;
  - 86 ON DELETE CASCADE;
  - `ensureDefaults()` creates categories with random ids;
  - `sync.desktopId` is stored in household settings;
  - no per-computer identity;
  - phones already belong to a user (`sync_device.user_id`), several per computer.
- **Existing defect** (not fixed, plan P14): `insertDevice` uses `INSERT OR REPLACE INTO sync_device`
  (`Core.sq:464`), against the project's rule. Fix in phase 1.
- **Real property today:** only HOME and COTTAGE kinds. The only address is `rental_property.address`,
  and the rental's `asset_id` is not shown in the UI. `loan` has no asset link.

## 3. Owner's decisions (all recorded in plan §1 and the decisions log)

1. A single release, 2.0.0, as a free update. No 1.2.0 and no 1.x household lock.
2. Local-first design: each computer keeps its own copy and exchanges signed, encrypted changes (ADR 0011).
   The owner first considered a separate "Rann.Roost.Pro" product and repository, then dropped it. Existing
   households upgrade in place, from wherever the user points.
3. Home Wi-Fi between computers is part of 2.0.0, alongside the shared folder.
4. Phones stay paired with their computer; several per computer, each under its own user.
5. Documents are copied to every computer by default, with an option to keep them on the main computer
   only.
6. The first computer is the main computer for automatic jobs; the role can be transferred or taken over.
7. A child's own computer holds only that child's granted groups.
8. Real property collects as much as possible (about 200 optional fields, `docs/spec-real-property.md`),
   including principal-residence years for every property kind.

## 4. What was delivered

- `docs/plan-multi-device.md`, `docs/spec-real-property.md` and `docs/adr/0011-several-computers.md`.
- `docs/requirements-additions.md`: MD-01 to MD-16 and AST-06 to AST-17.
- `CLAUDE.md`: the ADR list line only.
- Commit `740dc734`, tag `plan-2.0.0-2026-10-25`, PR #15 merged as `45ab897d`. PR CI passed (Ubuntu,
  Windows, performance).
- The decisions log row was added during this end-session.

## 5. Next

Plan §11: **phase 0 spike** first; **phase R** (real property) may run alongside. Work on topic branches.
The estimate is about 8–11 months to 2.0.0. The spike may change it.

## 6. Builds and tests

None run locally; the changes are documentation only. GitHub CI ran on PR #15 and all three checks passed.

## 7. Afternoon: phase 0 spike (merged)

- `spike/multi-device` → PR #17 (CI green on Ubuntu, Windows, performance) → merged as `605f1244`.
- `ReplicationSpikeTest` (10 tests, `core/data-jdbc`): triggers capture changes on 127 ledger and 19 core
  tables; the in-file `repl_apply` row switch (temp tables are not visible to stored triggers); BLOB
  `app_user.public_key` carried as hex. Cost on a bulk load: ~3.6× (250k transactions 6.7 s → 24.3 s),
  ~300 bytes per change. Verdict **go**, written into ADR 0011 § Phase 0 spike.
- New sync tables use the `repl_` prefix (plan §4.1 corrected); the trigger generator takes an explicit list.
- `tools/dev/folder_sync_probe.py` (write/read modes, prints RESULT PASS/FAIL) is on `main`; it has **not**
  been run on two computers yet. It gates phase 4 only.

## 8. Shared folder between different accounts (owner's point, 19:20)

Family members do not sign in to one OneDrive account. The design never required it, but the docs must say
how it works: (a) one person shares a folder from their OneDrive (or Google Drive / Dropbox) with each
member's own account, who adds it with **Add shortcut to My files**; (b) a NAS or Windows network share,
each with their own login; (c) no folder at all: home Wi-Fi sync (in 2.0.0). The probe must be run that way
(second laptop on another member's account), since shortcut folders and files-on-demand are where OneDrive
is most likely to misbehave. **To do:** update `tools/dev/README.md` (probe entry) and plan §5.3 on a small
docs branch from `main` (they are not on `feature/real-property`); owner approval needed for the branch.

## 9. Phase R on `feature/real-property`

- **Batch 1, `e903cb10`, pushed.** Snapshot `schemas/35.db`; migration `35.sqm` (ledger 35→36) with
  `real_property_field` (key/value per catalogue field) and child tables room, system, structure, owner,
  residence_year, custom; queries in `RealProperty.sq` (ON CONFLICT DO UPDATE only).
  `RealPropertyCatalogue.kt` (sections, ~150 fields, ~60 choice lists, system types per kind),
  `RealPropertyService.kt` (get/save with validation, residence conflicts: per family unit from 1982).
  AssetKind gained CONDO, LAND, BUILDING; maintenance starters and the home pickers follow.
  716 EN/FR texts. Tests: `RealPropertyServiceTest` (9), `SaveKeepsChildrenTest`, `MigrationTest`.
- **Batch 2, uncommitted.** `PropertyDialog.kt`: unit pickers (area/length, kept in household settings
  `property.areaUnit` / `property.lengthUnit`; values stored metric, typed values convert on change); one tab
  per section (CONDO only for condos; LAND hides building, basement, rooms); each field type has its own
  input, "Other" adds a describe box; editors for rooms (area from length × width, total), systems (type list
  per kind, rental fields when rented/leased, linked child asset), outbuildings, owners (share %), residence
  years ("2015-2019, 2023", conflicts listed), custom details. `AssetsScreen.kt`: "Property details…" button
  for saved real-property assets; file header added. ~70 EN/FR texts. `ResidenceYearsTest` (2).
- Python here-documents were used twice to edit files with quotes, against the project rule; results were
  checked by the compiler and tests. Use the Edit tool from now on.

## 10. Builds and tests (afternoon)

- Batch 1: full `./gradlew build`: 1006 tests, 0 failures, 1 skipped (HeifTest); then
  `:core:i18n:test --rerun` passed.
- Batch 2: `./gradlew :app:desktop:compileKotlin` clean (no warnings); `./gradlew :app:desktop:test
  :core:i18n:test`: 81 tests, 0 failures, 0 skipped (totals from the test XML). **A full build is still due
  before committing batch 2**, and the dialog has not been looked at on screen.
