<!-- File path and name: docs/outstanding-and-deferred-items.md
     Modified On Timestamp: 2026-10-27 @ 11:25 EDT
     Created On Timestamp: 2026-10-27 @ 11:25 EDT
     File Description: The authoritative register of every OUTSTANDING and DEFERRED item still to be addressed in
                       RANN's Roost (desktop and core). Each item is defined, given a status, a time estimate,
                       and flagged as a release gate or not.
     Uses: the code and git history (the authority), CLAUDE.md, docs/NOW.md, docs/plan-multi-device.md,
           docs/session-handover-2026-10-26.md, docs/adr/0011.
     Used By: session planning, the release checklist, and the App.Tracker sync (each row is one App.Tracker item).
     Purpose: One place where nothing outstanding gets lost between sessions.
-->

# RANN's Roost — Outstanding & Deferred Items (master register)

**When an item changes, update its row here in the same commit.** Each row is mirrored as one App.Tracker item
(project `ROOST`): the row's **#** is the key that starts the App.Tracker title.

## How to read this

Each item carries a **gate** flag:

| Flag | Meaning |
|------|---------|
| 🔴 **RELEASE GATE** | Must be resolved before any real / production use. Non-negotiable. |
| 🟠 **SECURITY** | A real weakness, mitigated for now (e.g. dev-only, no real data). Becomes a release gate once reachable. |
| 🟡 **FUNCTIONAL** | A feature is stubbed, partial or not yet built. Affects completeness, not safety. |
| ⚪ **HOUSEKEEPING** | Clean-up, tech debt, tests or cosmetic. Low risk. |

and a **status**: **DONE** (kept for one revision so the change is visible, then removable), **PARTLY DONE**,
**OPEN** (not started), **DEFERRED** (decided to do later - say why), **CLOSED** (no longer applies - the premise
was wrong or it was superseded). "UNVERIFIED" marks a status read from docs that a code check could not confirm.

Status → App.Tracker: OPEN = Accepted (or Proposed if not yet agreed), PARTLY DONE = In Progress, DONE = Done,
DEFERRED = Deferred, CLOSED = Rejected.

**Estimates** are working days (5 per week). **Plan** = the rough size in `docs/plan-multi-device.md` §11,
recorded as the middle of its range; **guess** = Claude's estimate on 2026-10-27, to be corrected by the owner.
The plan puts 2.0.0 at about 8-11 months for one developer working with Claude.

---

## 1. Release 2.0.0 — several computers per household (plan §11, ADR 0011) 🔴

| # | Item | Gate | Status | Estimate | Evidence / what remains |
|---|------|------|--------|----------|-------------------------|
| 1.0 | Phase 0 spike: triggers in SQLCipher, HLC, change file through OneDrive, mDNS | 🟡 | **DONE** | plan 1-2 wk | ADR 0011 "Phase 0 spike (2026-10-25)": **go** for capture; folder transport waits on the probe (3.1) |
| 1.R | Phase R: real-property details | 🟡 | **DONE** | plan 4-6 wk | PR #18; `RealPropertyService`, `RealProperty.sq` on `main` |
| 1.1 | Phase 1 foundations: `device.json`, local-only settings, `Ids.nameBased`, policy manifest, P14 | 🟡 | **DONE** | plan 2-3 wk | PR #21; `DeviceIdentity.kt`, `PolicyManifest.kt` on `main` |
| 1.2 | Phase 2 capture: sync tables, trigger generator, outbox, HLC, change sets | 🟡 | **DONE** | plan 3-4 wk | PR #22 (`08c3dd05`); `CaptureCompletenessTest`, `HybridClock`, `ChangeSetFile` on `main` |
| 1.3 | Phase 3 merge: apply engine, policies, tombstones, resolvers, conflicts, fuzz test | 🟡 | **OPEN** | plan 25 d (4-6 wk) | No `repl_clock` / `repl_tombstone` in code. Exit: fuzz tests green. Includes 2.3, 2.4 |
| 1.4 | Phase 4 exchange folder: writer, watcher, snapshots, join, pruning | 🟡 | **OPEN** | plan 17.5 d (3-4 wk) | Waits on the folder probe (3.1). Exit: two replicas in step through a folder. Includes 2.2 |
| 1.5 | Phase 5 key ring: signing keys, header rebuild, permission checks, rotation | 🟠 | **OPEN** | plan 17.5 d (3-4 wk) | Exit: security tests green. Includes 4.1 |
| 1.6 | Phase 6 Wi-Fi: `/hfm/v2` endpoints, mDNS, first-contact code, pinning | 🟡 | **OPEN** | plan 17.5 d (3-4 wk) | Can overlap phase 4. Exit: two computers in step over Wi-Fi. Includes 4.2 |
| 1.7 | Phase 7 desktop UI: wizard, Join, status, Computers, Changes to review, EN/FR, manual, Walk-Me | 🟡 | **OPEN** | plan 17.5 d (3-4 wk) | Exit: owner's walk-through |
| 1.8 | Phase 8 upgrade path: conversion, move off the cloud folder, backup format 2 | 🔴 | **OPEN** | plan 10 d (2 wk) | Exit: upgrade from every released version |
| 1.9 | Phase 9 beta: family test, GitHub pre-release, Partner Center flight | 🔴 | **OPEN** | plan 20+ d (4+ wk) | Exit: no open data-loss bug. Calendar time, not only effort |
| 1.10 | Phase 10 release 2.0.0: ADR/SRS final, release notes, Store and rann.ca texts | 🔴 | **OPEN** | plan 5 d (1 wk) | `docs/release-checklist.md`; includes 4.3 |

## 2. Change capture: performance and open findings (handover 2026-10-26 §9, §11)

| # | Item | Gate | Status | Estimate | Evidence / what remains |
|---|------|------|--------|----------|-------------------------|
| 2.1 | Merge the `feature/phase2-capture-perf` PR (performance switch `HFM_PERF_CAPTURE=1`, notes) | ⚪ | **OPEN** | guess 0.25 d | Branch pushed; waits on the owner's yes |
| 2.2 | Capture slows reports 1.3-3.4x: confirm the cause (outbox size) and empty sealed changes from the outbox | 🔴 | **OPEN** | guess 3 d | NFR-02. One comparison on one laptop so far; handover §9 has the numbers |
| 2.3 | A delete from another computer cascades and can remove a child added offline here, with no "Changes to review" entry | 🟡 | **OPEN** | guess 2 d | Plan 4.4 wants review; settle in phase 3 (1.3) |
| 2.4 | `category_rule` edited as delete + insert under the same id; `medication.refills_remaining` lowered in Kotlin (needs a counter change) | 🟡 | **OPEN** | guess 1.5 d | Both noted in `PolicyManifest`; phase 3 (1.3) |
| 2.5 | A snapshot (`VACUUM INTO`) carries core.db's local-only tables, including the phone-pairing private key | 🟠 | **OPEN** | guess 1 d | Must be stripped before a joining computer gets it (ADR 0005 addendum); phase 4 (1.4) |
| 2.6 | Run `:app:desktop:sampleHouseholdsTest` (family capture check), not run on phase 2 | ⚪ | **OPEN** | guess 0.25 d | Handover §13 |
| 2.7 | Baseline performance test fails "calendars, trips and trackers" on this laptop (3519 ms) | ⚪ | **OPEN** | guess 0.5 d | CI passes on PRs #21, #22: likely the laptop, not a regression. Confirm |

## 3. Waiting on the owner

| # | Item | Gate | Status | Estimate | Evidence / what remains |
|---|------|------|--------|----------|-------------------------|
| 3.1 | Two-computer folder probe (second laptop, another family member's account) | 🔴 | **OPEN** | guess 0.5 d | ADR 0011: blocks phase 4 (1.4) |
| 3.2 | Review the decisions taken while away | ⚪ | **OPEN** | guess 0.25 d | Handover 2026-10-26 §10 (ten decisions) |
| 3.3 | Upload `website/images/desktop-*.jpg` to Google Sites | ⚪ | **OPEN** | guess 0.25 d | NOW.md |
| 3.4 | Store pictures with the next listing update | ⚪ | **DEFERRED** | guess 0.5 d | Until the next Store listing update (2.0.0 at the latest) |

## 4. Documents to update during 2.0.0 (plan §12)

| # | Item | Gate | Status | Estimate | Evidence / what remains |
|---|------|------|--------|----------|-------------------------|
| 4.1 | ADR 0002 addendum: key ring, signing keys, rotation | ⚪ | **OPEN** | guess 0.25 d | No addendum in `docs/adr/0002*`; with phase 5 |
| 4.2 | ADR 0006 addendum: phones stay per computer; computer-to-computer Wi-Fi | ⚪ | **OPEN** | guess 0.25 d | No addendum in `docs/adr/0006*`; with phase 6 |
| 4.3 | SRS ARC-01 note: the household is replicated, not hosted | ⚪ | **OPEN** | guess 0.25 d | Not in the SRS yet; by phase 10 |

## N. Findings (new items found while working)

New findings get the next **N-number** (N1, N2, ...) and go here AND into App.Tracker in the same session.

| # | Item | Gate | Status | Estimate | Evidence / what remains |
|---|------|------|--------|----------|-------------------------|
| — | (none yet) | | | | |

---

## Release gate — the short list 🔴

1. 1.8 - Upgrade path from every released version
2. 1.9 - Beta with no open data-loss bug
3. 1.10 - Release 2.0.0 checklist
4. 2.2 - Capture must not break NFR-02 report times
5. 3.1 - Folder probe (blocks the folder transport)
