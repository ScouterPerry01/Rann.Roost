<!--
File path and name: docs/session-handover-2026-10-25.md
Modified On Timestamp: 2026-10-25 @ 14:05 EDT
Created On Timestamp: 2026-10-25 @ 14:05 EDT
File Description: Handover for 2026-10-25: the decision and plan for RANN's Roost 2.0.0.
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
