<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-26 @ 05:15 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-26 05:15 EDT

**Branches:** `main` has PRs #18-#21 (phase R, Store ID, shared-folder docs, **2.0.0 phase 1**). Released:
**v1.1.2**, unchanged until 2.0.0. **PR #22** `feature/phase2-capture` (phase 2: clock, capture triggers,
sealing, signed change-set files; capture off until sharing is on) is open: the owner said merge once CI is green.
`feature/phase2-capture-perf` (from phase 2): performance test switch `HFM_PERF_CAPTURE=1` plus these notes.

**Open question:** with capture on, reports ran 1.3-3.4 times slower on this laptop than without, reads
included; likely the outbox doubling the file. Handover 2026-10-26 §9 has the numbers and the lead.

**NEXT:**
1. Merge PR #22 when green; then a PR for `feature/phase2-capture-perf` (owner approval to merge).
2. Capture performance: confirm the cause (outbox size) and empty sealed changes from the outbox.
3. Phase 3, apply engine (plan §6.1, §11): clocks per field, tombstones, conflicts, aggregates, fuzz test.
   Open items to settle there: handover 2026-10-26 §11.

**Waiting on the developer:** review the decisions taken while away (handover 2026-10-26 §10, decisions log);
upload `website/images/desktop-*.jpg` to Google Sites; Store pictures with the next listing update; the
two-computer folder probe (second laptop, another family member's account).

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-26.md` §7-§13.

**Standing state:** no Android code here. Never create a repository named
`Schippers.Household.Finance.Manager`. The phone repo needs no change for 2.0.0 (plan §7).
