<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-12 @ 23:59 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-12 23:59 EDT

**Branch:** `main` (pushed; session notes on top of `287975fb`, PR #9 merged: Linux CI and release on ubuntu-26.04; branch deleted).
Tag **`v1.1.0`** = `44eaa1e6`; its GitHub release is **published** (Latest).

**What happened:** 1.1.0 fixes the Microsoft Store rejection of 1.0.0 (Java Access Bridge crash) and moves
Calendar to the top of the menu. Resubmitted to the Store; GitHub release published, so 1.0.0 Linux copies
are offered the update. Gradle add-on kept on open-source v5 (owner). Ubuntu 26.04 merged; its unit tests
came from Gradle's build cache (not re-run on 26; owner accepted). Detail: handover 2026-10-12.

**NEXT (in order):**
1. Microsoft Store certification of 1.1.0 is in progress (resubmitted 2026-10-12): wait for it.
2. If any Linux test trouble shows up on ubuntu-26.04, re-run the tests without the build cache.
3. Check that a 1.0.0 Linux copy is offered 1.1.0 (optional).
4. After the Store passes: rann.ca Linux link = `.../releases/latest`; Store link from RANN's Roost >
   Product identity (record its Store ID in the release checklist).
5. Owner: Play closed test (12+ testers, 14 days), then production.
6. Optional: rename the GitHub repository to `Rann.Roost` (earlier handovers).

**Waiting on the developer:** Store certification result.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-12.md` § 9-10.

**Standing state:** no Android code in this repository. Public 1.0 = end of Phase 5.
