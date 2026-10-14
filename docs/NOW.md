<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-14 @ 03:45 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-14 03:45 EDT

**Branch:** `main` (pushed; session notes on top of `1215ea2b`, PR #10 merged; branch deleted).
Tag **`v1.1.1`** = `1215ea2b`; its GitHub release is **published** (Latest).

**What happened:** the Store rejected **1.1.0** too (10.1.2.10, crash at launch, OS build 22621.1265,
Dell Latitude 5520 / Surface Laptop). The box was our own "RANN's Roost could not start", so the 1.0.0
Access Bridge crash is fixed, but the report quoted only the title: **the cause is unknown**. 1.1.1 makes
the box show the root cause and where it happened. Owner emailed Microsoft support for the full error text.

**NEXT (in order):**
1. Wait for Microsoft's answer (full error text, `startup-error.log`, Event Viewer, device details); also
   check Partner Center for the report's support-files ZIP (screenshot) -> `docs/MS.Cert.Issue/`.
2. With the error text: fix the real cause (suspects: Skia/DirectX graphics, tray, settings/store,
   locale; handover 2026-10-13 § 3), as 1.1.2 if needed.
3. Upload the 1.1.1 MSIX to Partner Center (`gh run download 38528167425 -n msix -D $env:TEMP\rel111\msix`)
   — owner decides when; safer after step 2.
4. After the Store passes: rann.ca Store link; record the Store ID in the release checklist.
5. Owner: Play closed test (12+ testers, 14 days), then production.
6. Optional: rename the GitHub repository to `Rann.Roost`.

**Waiting on the developer:** Microsoft support's reply; the Store resubmission decision.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-13.md`.

**Standing state:** no Android code in this repository. Public 1.0 = end of Phase 5.
