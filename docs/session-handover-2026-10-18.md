<!--
File path and name: docs/session-handover-2026-10-18.md
Modified On Timestamp: 2026-10-18 @ 11:05 EDT
Created On Timestamp: 2026-10-18 @ 11:05 EDT
File Description: Handover for the session of 2026-10-18: 1.1.1 submitted to the Store, local install on the laptop, repository renamed.
Uses: docs/NOW.md, docs/release-checklist.md, app/desktop/.../StartupFailure.kt, core/update/.../Updates.kt.
Used By: Claude Code and the owner when resuming.
Purpose: Keep the detail that does not belong in NOW.md or the session log.
-->

# Session handover — 2026-10-18

## 1. Microsoft Store: 1.1.1 submitted

- Microsoft support's reply only pointed back to the certification report already provided.
- Support files: the certification report in Partner Center shows a download link only when the tester
  attached something. The 1.0.0 ZIP (`docs/MS.Cert.Issue/`, ignored by git) held one screenshot ("Failed
  to launch JVM"), no logs. The owner found no ZIP for the 1.1.0 report.
- Decision (owner, on the recommendation): submit 1.1.1 without waiting, because its start-up box shows the
  root cause. Uploaded `RANNsRoost-1.1.1-x64.msix` (run 38528167425, `msix` artifact) with a note for the
  testers: screenshot of the full message, and `startup-error.log` from `%LOCALAPPDATA%\RANN's Roost\` or
  the package's `LocalCache\Local\RANN's Roost\` (MSIX may redirect it).

## 2. Testing the MSIX locally (lesson)

- The artifact is unsigned, so `Add-AppxPackage` will not install the file; it was unpacked and registered in
  developer mode (`Add-AppxPackage -Register <dir>\AppxManifest.xml`).
- **Pitfall:** inside an MSIX, names are percent-encoded (`RANN%27s%20Roost.exe`, `app\RANN%27s%20Roost.cfg`).
  A plain ZIP extract keeps the encoded names; Windows then "activates" the app successfully but nothing runs
  and no log is written. Decode the names (`[Uri]::UnescapeDataString`) before registering, or use the local
  build layout as `docs/release-checklist.md` § 6 says.
- Result on this laptop (HP OmniBook 7, build 26200): 1.1.1 starts normally, no `startup-error.log`.
  Not proof for the testers' build 22621.1265.

## 3. 1.1.1 on the owner's laptop until the Store passes

- Registered from `C:\Users\PSchi\Apps\RANNs Roost 1.1.1` (developer mode must stay on).
- The app's settings (Java preferences) live in the package's private registry (`Helium\User.dat`) and are
  lost when the package is removed. To move it they were exported and re-imported inside the package with
  `Invoke-CommandInDesktopPackage ... reg.exe export|import`; the restored copy matched. Owner has since
  deleted the backup `.reg`.
- When the Store version arrives: close the app, `Get-AppxPackage RANN.RannsRoost | Remove-AppxPackage`,
  delete the folder, install from the Store; expect to reopen the household once (File > Open).

## 4. Phone app

- Rann.Roost.Mobile is 1.0.0 (versionCode 10000); `roost` submodule pinned at `v1.0.0` (`f20b66e2`).
- Core changes since then that reach the phone: three gas companies in `BillLists.kt` (SaskEnergy, Eastward
  Energy, Liberty Utilities), 26 EN/FR texts, help/manual text. No phone fix needed; bump at the next release.
- Owner decision: hold the Play closed test until the Store passes, so testers try the complete application.

## 5. Repository renamed to `Rann.Roost`

- Owner renamed the GitHub repository. Checked: `.../Schippers.Household.Finance.Manager/releases/latest/
  download/update.json` and the new address both return 200 (GitHub redirect). Released 1.0.0-1.1.1 have the
  old address built in, so the redirect must stay: never create a repository with the old name.
- Still on the old name (9 tracked files, follow-up PR): `core/update/.../Updates.kt` (RELEASES),
  `app/desktop/.../AboutScreen.kt`, `third-party-notices.txt`, `README.md`, `docs/release-checklist.md`,
  `docs/store/microsoft-store.md`, `website/rann-roost-en|fr.md` (paste the website change into Google Sites).
  Local remotes: `origin` here and `.gitmodules` in Rann.Roost.Mobile still use the old URL (work via redirect).

## 6. Builds

- No code changed this session; no build or tests run. Only these notes.
