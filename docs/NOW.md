<!--
File path and name: docs/NOW.md
Modified On Timestamp: 2026-10-12 @ 10:18 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: The ONE short "where are we right now" file. Read this first when resuming.
Uses: docs/session-log.md, the current handover file.
Used By: Claude Code at the start of every session; the end-session skill rewrites it.
Purpose: Keep current state out of the always-loaded CLAUDE.md.
-->

# NOW — RANN's Roost

> **This file is OVERWRITTEN at the end of every session, never appended to. Keep it under ~40 lines.**
> History goes in `docs/session-log.md`; detail in the day's handover file.

**As of:** 2026-10-12 10:18 EDT

**Branch:** `fix/store-first-launch` (from `main` `2c3709b0`), version **1.1.0**. Released: `v1.0.0`.

**What happened:** the Microsoft Store **rejected 1.0.0** ("Failed to launch JVM"; report kept in the
git-ignored `docs/MS.Cert.Issue/`). Cause found and reproduced: the testers run with Java Access Bridge on,
and the packaged runtime lacked `jdk.accessibility`. Fixed on the branch, with start-up errors now logged
and shown, the Visual C++ runtime declared in the MSIX, and a release-workflow step that opens the real
window with Access Bridge on. The owner approved the sample households and asked for **Calendar at the
top of the menu** (done; all manual and Store pictures retaken). Detail: `docs/session-handover-2026-10-12.md`.

**NEXT (in order):**
1. Commit the branch (owner's yes), pull request, CI green, merge to `main`.
2. Release workflow **dry run** (`gh workflow run release.yml --ref main`): the new window step must pass.
3. Tag `v1.1.0` (owner's yes), publish the GitHub release, upload `RANNsRoost-1.1.0-x64.msix` to the
   Store with a note to the testers (handover § 4); update the Store screenshots from `docs/store/screenshots`.
4. Owner: keep the 12+ Play closed testers opted in for 14 days, then apply for production.
5. Optional, after the Store passes: rename the GitHub repository to `Rann.Roost` (handover 2026-10-11 and
   earlier NOW notes: 8 files, the submodule URL, test that 1.0.0 still finds updates).

**Waiting on the developer:** approval to commit, tag and resubmit; the closed test.

**To resume, read ONLY:** this file; `docs/session-handover-2026-10-12.md` § 3-4 if needed.

**Standing state:** no Android code in this repository. Public 1.0 = end of Phase 5.
