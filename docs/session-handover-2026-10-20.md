<!--
File path and name: docs/session-handover-2026-10-20.md
Modified On Timestamp: 2026-10-20 @ 12:20 EDT
Created On Timestamp: 2026-10-20 @ 12:20 EDT
File Description: Handover for the session of 2026-10-20: the Store start-up crash reproduced and fixed, 1.1.2 released.
Uses: docs/NOW.md, app/desktop/.../LoopbackPipe.kt, app/desktop/.../Updater.kt, docs/release-checklist.md.
Used By: Claude Code and the owner when resuming.
Purpose: Keep the detail that does not belong in NOW.md or the session log.
-->

# Session handover — 2026-10-20

## 1. The third rejection (1.1.1)

- Report 2026-10-19: same as 1.1.0 (10.1.2.10, crash at launch, OS build 22621.1265, "RANN's Roost could
  not start"); again only the title quoted, no attachments.

## 2. Reproduced: a Store-style install under a new standard user

- The exact uploaded 1.1.1 MSIX (release run 38528167425) was test-signed with a throw-away certificate
  (subject = the Store publisher `CN=A48DF0F5-...`, made by the owner in their own admin window), its public
  part trusted in `LocalMachine\TrustedPeople`, and installed with `Add-AppxPackage` (into `WindowsApps`,
  Developer Mode off).
- On the owner's own account: starts normally. **Under a new local standard user (`RoostTest`): fails**
  exactly like the Store report.
- Log (found in `C:\Users\RoostTest\AppData\Local\Packages\RANN.RannsRoost_9cg7jhsmyye38\LocalCache\Local\RANN's Roost\`,
  not where the message said: MSIX redirected it): `ExceptionInInitializerError` at `Updater.create` ->
  `GitHubSource.<clinit>` -> `HttpClient` -> `Selector.open` -> `PipeImpl` (AF_UNIX) ->
  `SocketException: Invalid argument: connect`.
- Cause: Java 21's Windows selector wake-up pipe uses a Unix-domain socket file in `%TEMP%`; under MSIX
  AppData redirection the file is bound in one place and connected to in another. Every `HttpClient`
  (updates, rates, AI reading) is affected. Why the owner's own account is not affected is not known.

## 3. The fix (PR #12, merged as `bef9b95b`; tag `v1.1.2`)

- `LoopbackPipe.useTcp()`, first line of `main()`: on Windows sets `jdk.net.unixdomain.tmpdir` to a
  folder that cannot exist inside the app's runtime, so binding fails and Java falls back to its TCP
  loopback pipe (checked: pipe kind AF_UNIX -> TCP; `HttpClient` works). A value given on the command line wins.
- `GitHubSource`'s `HttpClient` made lazily at the first check (Windows never checks).
- `LoopbackPipeTest` (3 tests, one in a separate Java process). Version 1.1.2; notes `docs/releases/1.1.2.*.md`.

## 4. Tests

- Local `./gradlew build` (JDK 21.0.8): BUILD SUCCESSFUL, 994 tests, 0 failed, 1 skipped (HeifTest by
  design), no warnings; only `:app:desktop` re-ran (core results up to date).
- PR #12 CI green (Ubuntu 26.04, Windows 15m, Performance). Release dry run 39439191550 green.
- Tag release run 39441668916 green; GitHub release 1.1.2 **published, Latest**.
- As `RoostTest` (test-signed, standard user): locally built 1.1.2 starts and updates rates; the **tag
  run's MSIX** (SHA-256 `086f8b0d...`, the file to upload) starts and updates rates.
- Pitfall: two different packages with the same version cannot both be staged; remove for all users
  first (`Get-AppxPackage -AllUsers RANN.RannsRoost | Remove-AppxPackage -AllUsers`, admin).

## 5. Left on the machine

- Test certificate (CurrentUser\My + LocalMachine\TrustedPeople) and `C:\Users\Public\*-testsigned.msix`:
  owner to remove after the upload (commands given in the session). Account `RoostTest` kept until the
  Store version passes, then `Remove-LocalUser RoostTest` and delete `C:\Users\RoostTest`.
- No RANN's Roost installed on the owner's account now (developer copy removed for the test).

## 6. Decisions (owner)

- Reproduce locally (Store-style install, then VirtualBox if needed) instead of more diagnostics; the
  VirtualBox step was not needed.
- PR, merge, tag v1.1.2 and publish the GitHub release without asking at each step; notes straight to `main`.

## 7. Suggestion (not done)

- Add "test-signed install under a new standard user" to `docs/release-checklist.md` before each Store upload.
