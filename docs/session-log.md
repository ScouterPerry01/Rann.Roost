<!--
File path and name: docs/session-log.md
Modified On Timestamp: 2026-10-12 @ 23:59 EDT
Created On Timestamp: 2026-10-09 @ 08:14 EDT
File Description: Append-only session history, newest first.
Uses: N/A (historical record).
Used By: Claude Code on demand; the end-session skill adds a block at the top.
Purpose: Preserve what happened, session by session, without paying for it in context.
-->

# RANN's Roost — Session log

**Append-only. Add each new session as a block at the TOP (just below this header). Do not rewrite or condense
earlier blocks. Keep each block to ~5-10 lines - detail belongs in that day's handover file.**

When this file passes ~40 KB, move it to `docs/archive/session-log-through-<date>.md` and start a fresh one.
Earlier history (before 2026-10-09) is in git log and `docs/development-plan.md`.

---

## 2026-10-12 23:20 -> 23:59 EDT - Store resubmitted; PR #9 merged; v1.1.0 published

- Owner resubmitted 1.1.0 to the Microsoft Store; certification in progress.
- Shipped: session notes `ee2ccabe`; PR #9 merged (`287975fb`, Linux CI and release on ubuntu-26.04),
  branch deleted; GitHub release **v1.1.0 published** (Latest; 1.0.0 Linux copies are offered the update).
- Decisions (owner): merge PR #9 without re-running the tests on 26 (small accepted risk); publish 1.1.0
  before Store certification; session notes committed straight to `main`.
- Builds: no code changed and no local build run; PR #9 checks green on `ee2ccabe` (tests from Gradle's
  cache again). Detail: `docs/session-handover-2026-10-12.md` § 10.

## 2026-10-12 08:50 -> 23:08 EDT - Store rejection fixed, v1.1.0 tagged; workflow add-ons; Ubuntu 26 trial

- Microsoft Store rejected 1.0.0 ("Failed to launch JVM"): Java Access Bridge on and no `jdk.accessibility`
  in the runtime. Fixed with start-up error log/message, VC++ runtime in the MSIX, release window check.
- Shipped: PR #7 (`44eaa1e6`, Calendar at the top of the menu, all pictures retaken), tag **`v1.1.0`**
  (GitHub release drafted, not published); PR #8 (`d524cdb7`) current workflow add-ons. PR #9
  (`chore/ubuntu-26`) green, not merged.
- Decisions (owner): release as 1.1.0; Gradle add-on stays on open-source v5 (not v6's commercial terms).
- Builds: local `./gradlew build` 985 tests, 0 failed, 1 skipped (HeifTest by design); CI green on PRs
  #7-#9; release dry runs green (window check OK; Ubuntu 26 package self-checks OK). PRs #8-#9 reused
  cached test results (no code change). Detail: `docs/session-handover-2026-10-12.md`.

## 2026-10-11 08:15 -> 18:35 EDT - Sample households in any province and language

- Shipped: `2c3709b0` on `main` (PR #6): "Open a sample household" on the welcome screen; Alex & Sam,
  a family of five or a single person, in EN or FR and any province or territory (throw-away copy).
- Alex & Sam's default EN/ON and FR/QC households unchanged (proved by a before/after fingerprint).
- Decisions: one household + language + province choice; full province sweep is a separate tagged task.
- Builds: local `./gradlew build` 985 tests, 0 failed, 1 skipped (HeifTest by design); every-province
  sweeps green locally; CI green on the PR and on `main`. Detail: `docs/session-handover-2026-10-11.md`.

## 2026-10-09 08:20 -> 2026-10-10 21:15 EDT - Release 1.0.0, Store listing and assets, trailers

- Shipped: `v1.0.0` (`f20b66e2`, GitHub release still a draft); repository made public; `a7d294a6` Store
  captions and alt text, retaken screenshots, Store logos in `branding/store/` (`make_store_logos.py`).
- Outside the repository: 15 trailers (8 EN, 7 FR, Azure voices Liam and Thierry) in the owner's Downloads.
- Phone app 1.0.0 is in Google Play closed testing (Rann.Roost.Mobile).
- Decisions: 1.0.0 is the first Microsoft Store submission; trailer 08 (phone) stays off the Microsoft Store.
- Builds: CI green on `main` (Windows, Ubuntu, performance); this last commit is
  documentation only (no build run). Detail: `docs/session-handover-2026-10-10.md`.

## 2026-10-09 07:50 -> 08:20 EDT - Claude Code context set up

- Ran `organize-project-context` (repo had no CLAUDE.md): added CLAUDE.md, docs/NOW.md, this log,
  `.claude/settings.json` deny list, `end-session` skill, `test-runner` agent; `.gitignore` ignores
  `.claude/worktrees/` and `.claude/settings.local.json`.
- Branch `docs/context-slim` from `f18f6e12`; documentation only, no source/test/CI change; no build run.
