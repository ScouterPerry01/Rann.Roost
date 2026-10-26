<!--
File path and name: docs/session-handover-2026-10-26.md
Modified On Timestamp: 2026-10-26 @ 05:15 EDT
Created On Timestamp: 2026-10-26 @ 02:30 EDT
File Description: Handover for 2026-10-26: phase R finished (first session); PRs #18-#21 merged and 2.0.0 phases 1-2 (second session).
Uses: docs/spec-real-property.md, docs/manual-format.md, tools/dev/README.md.
Used By: Claude Code when resuming; the owner.
Purpose: What was done and decided this session, and where to start next.
-->

# Session handover — 2026-10-26

## 1. Phase R delivered on `feature/real-property` (all pushed)

- `3b17be8a`: the Property details dialog (AST-06 to AST-17), EN/FR texts, `ResidenceYearsTest`.
- `75352304`: full property details for the family sample's house and cottage; exact room areas in feet;
  `manualScreenshots -Pset=property` review pictures (build/property-images).
- `1011b26a`: fact sheet (AST-16); rental property picker; "Homes and land" in the estate summary; address,
  roll and parcel numbers in the home inventory.
- `9be750d1`: manual chapter `property.md` (EN/FR) with pictures from the family sample
  (`-Pset=property-manual`, light and dark); help topics; related chapters (assets, estate, side, reports,
  utilities) updated.
- `6b4c0b55`: the owner's review of the dialog (§2).
- **PR #18** (`feature/real-property` → `main`) opened. Merge only after green CI and the owner's approval.

## 2. Owner's decisions (also in the plan's decisions log)

- Property details sections listed down the left side (16 of them) instead of a tab row.
- Uniform box width (270 dp), and choices wrap to two lines (`Picker(wrap = true)`), in this dialog only.
- Principal residence tab: the owners on the Owners tab, or the adults when it is empty, plus anyone who
  already has years.
- "Count in net worth": ticked for the demo homes and cottages, and ticked by default for new
  Home/Cottage/Condo/Land/Building assets (until the user changes the box).
- Demo TFSA's last purchase reduced from 120 to 110 units so its cash stays positive.
- Registered plan cash below zero: **a warning, never a block** (§3).

## 3. Uncommitted at the end of the session (awaiting approval)

- `InvestmentsScreen.kt`: a red line under an account's figures when a registered plan's cash is below zero;
  in the transaction dialog, a red line when the transaction would leave it below zero (on the later of today
  and the transaction's date). Texts `investments.negativeCash` / `negativeCashAfter` (EN/FR); manual
  `investments.md` (EN/FR) explains both.
- `SampleHouseholdsTest`: no sample household has a registered plan below zero. It caught the single sample's
  TFSA at −$56 (6 units a month outgrew the $200 contribution); `DemoSingle.kt` now buys 5.
- Store pictures retaken (`docs/store/screenshots/desktop-en|fr`, 16) and the website's desktop pictures
  (`website/images/desktop-*.jpg`, 6, scaled to 1080 × 675) now show net worth with the homes ($896,773.84 /
  897 664,74 $). The website pictures must be uploaded to Google Sites by the owner; the Store pictures go
  up with the next listing update.
- These wrap-up files.

## 4. Other branches

- `docs/shared-exchange-folder` (`81356a23`, pushed, no PR yet): the exchange folder is shared between
  family members' own accounts (plan §5.3, probe instructions).

## 5. Next

1. Owner approval: commit §3 and push (updates PR #18); CI on PR #18; merge with approval.
2. PR for `docs/shared-exchange-folder` (with approval).
3. Phases 1–10 of 2.0.0 (plan §11). At phase 1, add the sync rules to `CLAUDE.md` (plan §12).

## 6. Builds and tests

- Before `6b4c0b55`: `./gradlew build` 1010 tests, 0 failures, 1 skipped (HeifTest).
- With §3: `./gradlew :app:desktop:test --tests SampleHouseholdsTest :core:i18n:test` first failed as above
  (single TFSA −56.00), then after the fix `./gradlew build` 1011 tests, 0 failures, 1 skipped (HeifTest);
  no compiler warnings. The province-by-province `sampleHouseholdsTest` was not run.

---

# Second session — 2026-10-26 02:36 -> 05:15 EDT (owner away 02:50 -> 05:05)

The owner authorised merges, commits and pushes while away, and asked for decisions on Claude's
recommendation (memory "decide when away"). Sections 7-13 cover it.

## 7. Merged to `main` (each after green CI: Windows, Ubuntu, performance)

- **PR #18** phase R (property details). **PR #19** `docs/store-identity`: Microsoft Store ID `9NZC41PQ1BWG`,
  listing link and package identity in `docs/release-checklist.md`; Windows and Linux buttons in
  `website/rann-roost-en|fr.md`. **PR #20** `docs/shared-exchange-folder`.
- **PR #21** `feature/phase1-foundations`: 2.0.0 phase 1, no behaviour change:
  - `Ids.nameBased` (UUIDv5): built-in categories, scheduled set-asides, stored bill occurrences.
  - P14: `insertDevice` updates in place; re-pairing keeps last seen and items received, lifts a removal.
  - `device.json` (core/data `DeviceIdentity`): this copy's computer id; not backed up, so a restore is a new
    computer; a damaged file is set aside as `device.json.bad`.
  - Core migration 8 (version 9): `repl_local_setting`; `Books.setting`/`putSetting` route `LocalSettings.KEYS`.
  - New module `core/replication`: `PolicyManifest` (156 tables), `UniqueResolutions` (15 constraints),
    `NoReplaceTest`; 32 queries moved from INSERT OR REPLACE to ON CONFLICT DO UPDATE.
  - `CLAUDE.md` sync rules (plan §12); decisions-log rows; ADR 0005 addendum.
- Merged branches were kept on GitHub (not deleted). No tags (nothing to release before 2.0.0).

## 8. Phase 2 on `feature/phase2-capture`, PR #22

- `HybridClock` / `HlcStamp` (plan 4.2). Migrations core 9 (→10) and ledger 36 (→37): `repl_outbox`,
  `repl_applying`. `CaptureTriggers` from the manifest; `SchemaManager.dropCaptureTriggers` before migrations;
  `Outbox.seal`; `ChangeSetFile` (plan 5.2) with Ed25519 `Signing` in core/security; change-set numbers per
  partition in `device.json`; `HouseholdStore.onDatabaseOpened` hook (how sharing will turn capture on).
- Exit check: `CaptureCompletenessTest` (app/desktop) builds the sample households with capture on and replays
  the outbox into empty databases: identical tables. Checked to fail when the `txn` update trigger is missing.
- Capture is off in every normal household until sharing is turned on (phase 7).

## 9. Branch `feature/phase2-capture-perf` (from phase 2)

- `PerformanceTest` gains `HFM_PERF_CAPTURE=1` (capture on); core/books tests depend on core/replication.
- First run with capture on (this laptop): 2 of 4 failed. Calendar year 1586 ms and month 1535 ms (limit
  1000), income-tax estimate 7818 ms (limit 3000); investments generation 8 min 8 s.
- Baseline without capture, same laptop, right after: calendar year 741 ms, tax estimate 1814 ms, tax package
  1034 ms (2586 with capture), net worth 30 years 733 ms (2507), transactions generated in 14 s (35 s).
- So reports that only read were also 1.3-3.4 times slower with capture on. The triggers do not run on reads;
  the likely cause is the database file roughly doubling, since every sealed change stays in `repl_outbox`.
  Lead for phase 3/4: remove sealed changes from the outbox once written to a change-set file, or keep the
  outbox in a separate file. One comparison on one laptop: treat as a lead, not a measurement.
- The baseline itself fails one test on this laptop ("calendars, trips and trackers"; "what the phone
  receives" 3519 ms). CI's performance job passed on PRs #21 and #22, so this laptop is slower, not a regression.

## 10. Decisions taken while the owner was away (owner may overturn any)

1. PRs merged with merge commits; merged branches kept.
2. `Ids.nameBased` lives in `core/domain`; namespaces: household (categories), goal, bill.
3. Re-pairing keeps a phone's history and lifts a removal.
4. Local-only settings: phone-pairing identity, transfer and watch folders, all `backup.*`.
5. Merge policies for all 156 tables from a survey of how each is written (decisions log row).
6. Four INSERT OR REPLACE left (`inv_txn`, `card_statement`, `room_entry`, `tax_slip`: second unique key).
7. Clock text uses 16 digits of milliseconds.
8. Capture only when sharing is on, through the `onDatabaseOpened` hook.
9. Migrations drop the capture triggers rather than relying on the applying switch.
10. Capture completeness: single person in every build (~45 s), family in `sampleHouseholdsTest` (~80 s).

## 11. Open items found tonight

- Phase 4: a snapshot (`VACUUM INTO`) holds core.db's local-only tables, including the phone-pairing private key;
  it must be stripped before a joining computer gets it (ADR 0005 addendum).
- Phase 3: a delete arriving from another computer cascades locally and could remove a child added offline on
  this computer without a "Changes to review" entry (plan 4.4 wants review).
- Phase 2/3: `category_rule` is edited as delete + insert under the same id; `medication.refills_remaining` is
  lowered in Kotlin (needs a counter change). Both noted in `PolicyManifest`.
- Capture cost on writing screens (section 9).

## 12. Next

1. PR #22 merged at about 06:00 after green CI (owner said yes). Merge the `feature/phase2-capture-perf` PR on the owner's yes.
2. Capture performance: compare with the baseline; profile the tax estimate and calendar with capture on.
3. Phase 3 (apply engine, plan 6.1): `repl_clock`, `repl_tombstone`, `repl_conflict`, field-by-field merge,
   aggregates, convergence fuzz test.
4. Owner: the two-computer folder probe; upload `website/images/desktop-*.jpg`; Store pictures at the next
   listing update.

## 13. Builds and tests

- Every batch: `./gradlew build` (test-runner agent or directly), totals from the test XML. Last on the phase 2
  branch: 1067 tests, 0 failures, 1 skipped (HeifTest), no warnings. One failed run on the way: six
  MigrationTest checks hard-code the ledger version; raised to 37.
- `:core:books:performanceTest` with `HFM_PERF_CAPTURE=1` (2 of 4 failed) and without (1 of 4 failed, as on
  this laptop before): see section 9.
- Not run: `:app:desktop:sampleHouseholdsTest` (includes the family capture check).
