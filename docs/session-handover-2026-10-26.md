<!--
File path and name: docs/session-handover-2026-10-26.md
Modified On Timestamp: 2026-10-26 @ 02:30 EDT
Created On Timestamp: 2026-10-26 @ 02:30 EDT
File Description: Handover for the session of 2026-10-25 evening to 2026-10-26: phase R finished, PR #18.
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
