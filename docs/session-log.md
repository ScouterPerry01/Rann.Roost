<!--
File path and name: docs/session-log.md
Modified On Timestamp: 2026-10-25 @ 19:45 EDT
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

## 2026-10-25 14:10 -> 19:45 EDT - Phase 0 spike merged; phase R batches 1 and 2

- Spike (PR #17, CI green) merged into `main` as `605f1244`: capture by triggers works; go verdict in ADR 0011.
- Phase R batch 1 on `feature/real-property` (`e903cb10`, pushed): ledger migration 35→36 (7 tables),
  `RealPropertyCatalogue` (~150 fields), `RealPropertyService`, 716 EN/FR texts, tests.
- Batch 2 (uncommitted): `PropertyDialog.kt`, the asset dialog button, ~70 texts, `ResidenceYearsTest`.
- Owner pointed out: family members do not share a cloud login → the exchange folder is a folder shared
  between accounts (or a NAS / Wi-Fi). Docs to update on a docs branch.
- Builds: batch 1 full `./gradlew build` 1006 tests, 0 failures, 1 skipped (HeifTest); batch 2 only
  `:app:desktop:test :core:i18n:test` 81 tests, 0 failures. Detail: handover 2026-10-25 §7-§10.

## 2026-10-25 10:45 -> 14:05 EDT - Decision and plan for 2.0.0 (several computers, real property)

- Found: 1.1.2 is not safe for two computers using one household folder (no lock; WAL over network or cloud).
- Owner decided: 2.0.0 only (free update). Each computer keeps its own copy; changes go through a shared
  folder and home Wi-Fi. Phones stay with their computer. Full real-property details. No 1.2.0.
- Delivered: `docs/plan-multi-device.md`, `docs/spec-real-property.md`, ADR 0011, MD-01..16 and AST-06..17;
  commit `740dc734`, tag `plan-2.0.0-2026-10-25`, PR #15 merged (`45ab897d`).
- Builds: none locally (docs only); PR #15 CI green (Ubuntu, Windows, performance). Detail:
  `docs/session-handover-2026-10-25.md`.

---

## 2026-10-22 15:30 -> 16:05 EDT - RANN's Roost 1.1.2 live on the Microsoft Store

- 1.1.2 passed certification and is live on the Microsoft Store (CAD $19.99). Owner added the Store link to
  the live rann.ca pages; the repo copy in `website/` still says "Coming soon".
- Linux link for the site: `https://github.com/ScouterPerry01/Rann.Roost/releases/latest` (checked: opens v1.1.2).
- Found: the live Store description says the Android companion is on Google Play (it is not yet); the website
  and the Store listing disagree about phone sync through the cloud.
- Builds: none run (docs only). Detail: `docs/session-handover-2026-10-22.md`.

---

## 2026-10-21 11:50 -> 12:10 EDT - Store 1.1.2 failed on policy 11.16 (Generative AI not declared)

- First certification of 1.1.2 failed only because the "Generative AI" box under Properties was unchecked.
  The report screenshot shows the app running (Settings > AI reading): the start-up crash is fixed.
- Owner checked the box and resubmitted the same MSIX. Release checklist § 7 now includes that step.
- Branch `docs/store-genai-declaration` (`00303178` + end-session commit), not pushed.
- Builds: none run (docs only). Detail: `docs/session-handover-2026-10-21.md`.

---

## 2026-10-20 07:20 -> 12:20 EDT - Store crash reproduced and fixed; v1.1.2 published

- Store rejected 1.1.1 (same report as 1.1.0). Reproduced with the uploaded MSIX test-signed and installed
  under a new standard user: Java's AF_UNIX wake-up pipe fails under MSIX AppData redirection, so the
  update checker's `HttpClient` ended start-up.
- Shipped: PR #12 (`bef9b95b`) `LoopbackPipe` (TCP pipe on Windows) + lazy update client; tag
  **`v1.1.2`**, release run green, GitHub release **published** (Latest). MSIX not yet uploaded.
- Decisions (owner): reproduce locally rather than more diagnostics; merge/tag/publish without step approvals.
- Builds: local `./gradlew build` 994 tests, 0 failed, 1 skipped (HeifTest by design), no warnings; PR #12
  CI and release dry run green; tag run's MSIX verified as the new user. Detail: `docs/session-handover-2026-10-20.md`.

## 2026-10-18 06:15 -> 11:05 EDT - 1.1.1 submitted to the Store; repository renamed to Rann.Roost

- Microsoft support only pointed back to the report; no support ZIP for the 1.1.0 report.
- Owner submitted the 1.1.1 MSIX to Partner Center with a note asking testers for the full message and
  `startup-error.log`. 1.1.1 tested locally (developer-mode install): starts normally.
- Owner runs 1.1.1 from `C:\Users\PSchi\Apps\RANNs Roost 1.1.1` until the Store passes (settings moved).
- Decisions (owner): submit 1.1.1 now; Play closed test waits for the Store; GitHub repo renamed to
  `Rann.Roost` (old address redirects; follow-up PR for the old name in 9 files).
- Builds: none (notes only). Detail: `docs/session-handover-2026-10-18.md`.

## 2026-10-13 15:05 -> 2026-10-14 03:45 EDT - Store rejected 1.1.0 (cause unknown); v1.1.1 published

- Store rejected 1.1.0: crash at launch on OS build 22621.1265; our own "could not start" box, report
  quoted only its title. Not reproducible on build 26200. Owner emailed Microsoft support for the error text.
- Shipped: PR #10 (`1215ea2b`) start-up box shows root cause + 8 stack lines; version 1.1.1; tag
  **`v1.1.1`**, release run green, GitHub release **published** (Latest). MSIX not yet uploaded to the Store.
- Decisions (owner): release 1.1.1 now; Store upload timing left to the owner.
- Builds: local `./gradlew build` 991 tests, 0 failed, 1 skipped (HeifTest by design), no warnings;
  PR #10 CI green. Detail: `docs/session-handover-2026-10-13.md`.

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
