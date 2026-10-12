<!--
File path and name: docs/session-handover-2026-10-11.md
Modified On Timestamp: 2026-10-11 @ 18:35 EDT
Created On Timestamp: 2026-10-11 @ 18:35 EDT
File Description: Handover for the session of 2026-10-11 (sample households in any province and language).
Uses: docs/NOW.md, docs/session-log.md, app/desktop Demo*.kt, SampleScreen.kt.
Used By: Claude Code and the owner when resuming.
Purpose: Keep the detail that does not belong in NOW.md or the session log.
-->

# Session handover — 2026-10-11

## 1. What shipped

- `2c3709b0` on `main` (PR #6, CI green on Windows, Ubuntu and performance; branch deleted).
- **Open a sample household** (welcome screen → `SampleScreen.kt`): choose Alex & Sam, the family of
  five (the Mitchells / la famille Tremblay) or the single person (Jordan / Camille), the language and
  any of the 13 provinces and territories. The copy lives in `%TEMP%\hfm-demo*`, shows a banner, is never
  backed up or added to recent households, and is deleted when locked or closed (`AppState.discardSample`).
- `DemoPlace.kt`: per-province names, prices and places (`Local` table for the 12 outside Quebec);
  `l(fr, en)` picks by language, `q(qc, other)` picks by place, so Quebec works in English too.
- `DemoKit.kt` (shared helpers; pay-stub income tax and the child benefit come from `IncomeTax`),
  `DemoFamily.kt`, `DemoSingle.kt`. Alex & Sam (`DemoHousehold`, `DemoTrips`, `DemoSeasonal`) made
  province-aware; a before/after fingerprint proved the default EN/ON and FR/QC households identical.
- `BillLists.kt`: SaskEnergy, Eastward Energy, Liberty Utilities recognised as gas.
- runDemo: `-Psample=COUPLE|FAMILY|SINGLE`, `-Pprovince=<code>`. Manual "Trying a sample household"
  (EN/FR) with new `sample-household.png` and retaken `welcome-screen.png`. NOW.md: repository rename to-do.

## 2. Tests

- Local `./gradlew build`: 985 tests, 0 failed, 1 skipped (HeifTest skips itself on this PC), no warnings.
- Every-province sweeps (tag `samples`, excluded from `build`): `./gradlew :app:desktop:sampleHouseholdsTest`
  green locally (DemoHouseholdTest 407 s, SampleHouseholdsTest 306 s). CI runs only the few-province tests.

## 3. Open items

- Nobody has looked at the family and single households in the running app yet (only the tests and the
  two manual pictures).
- The local details outside ON/QC (cities, utilities, daycare costs, fuel and kWh prices, property tax,
  rebates) are plausible sample figures, not checked against sources. Worth a glance per province.
- CPP/QPP and EI on the samples' pay stubs are round figures, not calculated (stated in `DemoKit.payStub`).
- Ships to users with release 1.1.

## 4. Decisions (all made with the owner)

- Keep Alex & Sam unchanged so the manual and Store pictures stay valid; add the two new households.
- One choice of household + language + province instead of fixed households per province.
- The full province sweep is a separate tagged task, not part of `./gradlew build` (about 12 minutes).
