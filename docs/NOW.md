<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-25 @ 14:05 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-25 14:05 EDT

**Branch:** `main` = `origin/main` = `45ab897d` (PR #15 merged: the 2.0.0 plan). Tag `plan-2.0.0-2026-10-25`
marks the approved plan (not a release; `v*` tags start `release.yml`). Released: **v1.1.2** (Store, Linux),
which stays as it is until 2.0.0. This session's notes are uncommitted on `main` and go on a topic branch.

**What happened:** the owner decided to build **RANN's Roost 2.0.0**: one household on several computers
(own copy each; changes through a shared folder and home Wi-Fi) plus full real-property details. The plan,
spec, ADR and requirements are written and merged. **No code has been written yet.**

**NEXT (2.0.0, in order; see `docs/plan-multi-device.md` §11):**
1. **Phase 0 spike** (topic branch): triggers + JSON functions in the SQLCipher build, triggers on cascaded
   deletes, cost on NFR-02 data, HLC, one change file through OneDrive between two Windows accounts, mDNS on
   Windows and Linux. Write the go/no-go into ADR 0011.
2. **Phase R real property** (may run alongside): `docs/spec-real-property.md`. Snapshot the ledger schema
   first; the next ledger migration file is `35.sqm` (verify).
3. Then phases 1–10 as in the plan. At phase 1, add the sync rules to `CLAUDE.md` (plan §12).

**Carried over from 2026-10-22 (Store and website, still open):** owner sends the Store link / ID →
update `website/rann-roost-en|fr.md` and `docs/release-checklist.md`; "coming soon to Google Play" wording;
site vs listing on phone sync through the cloud; Store install test with a promo code; Play closed test.

**Waiting on the developer:** approval to commit these session notes; the Store link / ID.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-25.md`; then the plan section for the
phase being started.

**Standing state:** no Android code in this repository. Never create a repository named
`Schippers.Household.Finance.Manager`. The phone repo needs no change for 2.0.0 (plan §7).
