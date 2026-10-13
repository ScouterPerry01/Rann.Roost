<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-12 @ 23:32 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-12 23:32 EDT

**Branch:** `chore/ubuntu-26` (PR #9 open, all checks green, **not merged**). `main` = `d524cdb7`
(pushed). Tag **`v1.1.0`** = `44eaa1e6`.

**What happened:** 1.1.0 fixes the Microsoft Store rejection of 1.0.0 (Java Access Bridge crash) and moves
Calendar to the top of the menu. Tagged; the release workflow drafted the GitHub release (**draft, not
published**); the MSIX is in the owner's `Downloads\RANNsRoost-1.1.0\`. Workflow add-ons updated (PR #8,
merged); Gradle add-on kept on open-source v5 (owner). Ubuntu 26.04 tried on PR #9: packages build and
self-check OK; unit tests came from Gradle's build cache (not re-run on 26). Detail: handover 2026-10-12.

**NEXT (in order):**
1. 1.1.0 resubmitted to the Microsoft Store (owner, 2026-10-12): wait for certification.
2. PR #9: owner chose **merge now** (unit tests not re-run on 26: accepted small risk). Must be merged
   before GitHub moves `ubuntu-latest` to 26 on **2026-10-19**.
3. Publish the v1.1.0 draft GitHub release: owner said **not yet** (2026-10-12); 1.0.0 Linux copies see
   the update once it is published.
4. After the Store passes: rann.ca Linux link = `.../releases/latest`; Store link from RANN's Roost >
   Product identity (record its Store ID in the release checklist).
5. Owner: Play closed test (12+ testers, 14 days), then production.
6. Optional: rename the GitHub repository to `Rann.Roost` (earlier handovers).

**Waiting on the developer:** Store certification result; when to publish the draft release.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-12.md` § 6-9.

**Standing state:** no Android code in this repository. Public 1.0 = end of Phase 5.
