<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-18 @ 13:39 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-18 13:39 EDT

**Branch:** `main` (pushed; PR #11 merged as `d5ec04c1`). Tag **`v1.1.1`** = `1215ea2b` (GitHub release
published, Latest). Phone repo Rann.Roost.Mobile: 1.0.0 (`main` = `3f5b90cf`, PR #5 merged), `roost`
submodule still pinned at `v1.0.0`.

**What happened:** Microsoft support only pointed back to the report. **1.1.1 MSIX submitted to Partner
Center on 2026-10-18** with a note asking testers for a screenshot of the full message and
`startup-error.log`. 1.1.1 tested here (developer-mode install): starts normally. Owner runs 1.1.1 on the
laptop from `C:\Users\PSchi\Apps\RANNs Roost 1.1.1` until the Store passes. GitHub repository **renamed to
`Rann.Roost`**: every address in both repositories and on rann.ca updated (old address still redirects).

**NEXT (in order):**
1. Wait for certification of 1.1.1. Fails -> the report should quote the real error; fix as 1.1.2.
2. Passes -> remove the developer copy (`Get-AppxPackage RANN.RannsRoost | Remove-AppxPackage`, delete the
   folder), install from the Store; rann.ca Store link; Store ID into the release checklist.
3. Then the Play closed test (12+ testers, 14 days): owner waits for the Store so testers get the complete
   application (desktop + phone). Bump the phone's `roost` submodule at its next release.

**Waiting on the developer:** the certification result.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-18.md`.

**Standing state:** no Android code in this repository. Never create a new repository named
`Schippers.Household.Finance.Manager` (it would break the redirect released apps rely on).
