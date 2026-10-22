<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-22 @ 16:05 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-22 16:05 EDT

**Branch:** `main` = `origin/main` = `febf86b1` (PR #13 merged). This session's notes are uncommitted on
`main`; they go on a topic branch once the owner approves. Tag **`v1.1.2`** = `bef9b95b` (GitHub release,
Latest). Phone repo Rann.Roost.Mobile: 1.0.0, `roost` submodule still pinned at `v1.0.0`.

**What happened:** RANN's Roost 1.1.2 **passed certification and is live on the Microsoft Store**, at
CAD $19.99. The owner put the Store link on the live rann.ca pages. The Linux link for the site is
https://github.com/ScouterPerry01/Rann.Roost/releases/latest (it always opens the newest release).

**NEXT (in order):**
1. Owner: send the Store link / Store ID (12 characters, starts with `9`). Then update `website/rann-roost-en.md`
   and `-fr.md` to match the live site (Windows: Store button; Linux: the `releases/latest` link), and add the
   Store ID to `docs/release-checklist.md`.
2. Live Store description says the Android companion is "sold separately on Google Play", but it is not on
   Play yet. Draft an EN/FR "coming soon to Google Play" wording for the next listing edit.
3. Website text vs Store listing: the site says phone captures go only over home Wi-Fi and "no cloud"; the
   listing also offers a cloud folder or email of your own. Make the site match.
4. Owner: install from the Store with a free promo code (Partner Center > Promotional codes), also as
   `RoostTest` (Store-only sign-in), then remove that account.
5. Then the Play closed test (12+ testers, 14 days). Bump the phone's `roost` submodule at its next release.

**Waiting on the developer:** the Store link / ID; the Store install test; approval to commit these notes.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-22.md`.

**Standing state:** no Android code in this repository. Never create a new repository named
`Schippers.Household.Finance.Manager` (it would break the redirect released apps rely on).
