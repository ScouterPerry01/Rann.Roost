<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-25 @ 19:45 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-25 19:45 EDT

**Branches:** `main` = `605f1244` (PR #17 merged: phase 0 spike, go verdict in ADR 0011). Work branch
**`feature/real-property`**: batch 1 pushed as `e903cb10` (ledger 35→36, catalogue, service, texts, tests);
batch 2 (Property dialog) is **uncommitted** on it, with these notes. No PR open for it yet.
Released: **v1.1.2**, unchanged until 2.0.0.

**Just finished:** phase R batch 2: `PropertyDialog.kt` (tabs per section + rooms, systems, outbuildings,
owners, residence years, custom), "Property details…" button in `AssetDialog`, ~70 EN/FR texts,
`ResidenceYearsTest`. Desktop compiles; `:app:desktop:test :core:i18n:test` = 81 tests, 0 failures.
**Not yet:** a full `./gradlew build`, and nobody has seen the dialog on screen.

**NEXT:**
1. Full `./gradlew build`, then (owner approval) commit + push batch 2 on `feature/real-property`.
2. Demo: full property details for the demo house and cottage (`DemoFamily.kt` ~193, ~211); screenshot the
   dialog in EN, then FR, and check the layout.
3. Rest of phase R: fact sheet (AST-16), rental `asset_id` picker (`ExtrasScreens.kt` ~961-994), address /
   roll / parcel on estate summary and home inventory, manual chapter + help topics.
4. Small docs branch from `main` (owner approval): probe instructions and plan §5.3 say the exchange folder
   is **shared between different accounts** (handover §8).
5. Then phases 1–10 (plan §11). At phase 1, add the sync rules to `CLAUDE.md` (plan §12).

**Waiting on the developer:** approvals above; the two-computer folder probe (needs a second laptop signed
in to *another* family member's account, folder shared to them); carried over from 2026-10-22: Store link /
ID and the website/listing items.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-25.md` §7-§10; `docs/spec-real-property.md`
for the phase R item being started.

**Standing state:** no Android code here. Never create a repository named
`Schippers.Household.Finance.Manager`. The phone repo needs no change for 2.0.0 (plan §7).
