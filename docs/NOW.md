<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-20 @ 12:25 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-20 12:25 EDT

**Branch:** `main` (pushed; PR #12 merged as `bef9b95b`). Tag **`v1.1.2`** = `bef9b95b` (GitHub release
published, Latest). Phone repo Rann.Roost.Mobile: 1.0.0, `roost` submodule still pinned at `v1.0.0`.

**What happened:** the Store rejected 1.1.1 like 1.1.0. **Cause found** by a test-signed Store-style install
under a new standard Windows user: under MSIX AppData redirection Java's AF_UNIX wake-up pipe fails, so the
first `HttpClient` (update checker) ended start-up. Fixed in **1.1.2** (`LoopbackPipe`: TCP pipe on Windows).
The tag run's MSIX starts and updates rates as that user.

**NEXT (in order):**
1. Wait for certification: the tag run's MSIX (run 39441668916, SHA-256 `086f8b0d...`) was uploaded to
   Partner Center on 2026-10-20; test certificate and test files removed. Fails -> read the new report.
2. Store passes -> install from the Store (also as `RoostTest`, then remove that account); rann.ca Store
   link; Store ID into the release checklist. Release checklist § 6a now has the new-user test.
3. Then the Play closed test (12+ testers, 14 days). Bump the phone's `roost` submodule at its next
   release (1.1.2 changed only `app/desktop`, so no core change to pick up).

**Waiting on the developer:** the Store upload and its certification result.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-20.md`.

**Standing state:** no Android code in this repository. Never create a new repository named
`Schippers.Household.Finance.Manager` (it would break the redirect released apps rely on).
