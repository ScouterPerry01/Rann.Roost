<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-21 @ 12:10 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-21 12:10 EDT

**Branch:** `docs/store-genai-declaration` (local only, not pushed; docs-only commits on top of `main`
`8fef7872`). `main` is pushed. Tag **`v1.1.2`** = `bef9b95b` (GitHub release published, Latest). Phone repo
Rann.Roost.Mobile: 1.0.0, `roost` submodule still pinned at `v1.0.0`.

**What happened:** the Store's first certification of 1.1.2 failed **only on policy 11.16**: the
"Generative AI" box under Properties was not checked. The report's screenshot shows the tester inside the
app (Settings > AI reading), so the start-up crash is fixed. The owner checked the box and resubmitted the
same MSIX on 2026-10-21. The release checklist (§ 7 Properties) now has that step.

**NEXT (in order):**
1. Owner: approve pushing `docs/store-genai-declaration` and merging it into `main` (docs only, so a PR
   with green CI or a fast-forward).
2. Wait for certification. Fails -> read the new report in `docs/MS.Cert.Issue/` (git-ignored).
3. Store passes -> install from the Store (also as `RoostTest`, then remove that account); rann.ca Store
   link; Store ID into the release checklist.
4. Then the Play closed test (12+ testers, 14 days). Bump the phone's `roost` submodule at its next
   release (1.1.2 changed only `app/desktop`, so no core change to pick up).

**Waiting on the developer:** the certification result; approval to push/merge the docs branch.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-21.md`.

**Standing state:** no Android code in this repository. Never create a new repository named
`Schippers.Household.Finance.Manager` (it would break the redirect released apps rely on).
