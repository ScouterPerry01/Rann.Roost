<!--
File path and name: docs/release-checklist.md
Modified On Timestamp: 2026-10-21 @ 12:08 EDT
Created On Timestamp: 2026-10-06 @ 17:12 EDT
File Description: The owner's release-day checklist for the desktop app (keys, tag, GitHub release, Microsoft Store).
Uses: .github/workflows/release.yml, docs/store/, branding/store/, docs/releases/.
Used By: The owner on release day.
Purpose: One ordered list so no release step is forgotten.
-->

# Release day checklist: RANN's Roost 1.0.0

For the owner. Everything that touches a secret (the release key, the Android upload key, their
passwords and the repository secrets) is done **in your own PowerShell window**, never in a Claude
session and never with `!` in one. Commands assume the repository at
`C:\Users\PSchi\source\repos\Rann.Roost` and Git, `gh` (signed in) and the
JDK at `C:\Program Files\Android\openjdk\jdk-21.0.8`.

Background: [ADR 0008](adr/0008-signed-releases-and-updates.md) (signed releases and update checks),
`.github/workflows/release.yml`, the store texts in [`docs/store/`](store), the rann.ca pages in
[`website/`](../website). Release notes: [`docs/releases/1.0.0.en.md`](releases/1.0.0.en.md) and
[`.fr.md`](releases/1.0.0.fr.md).

## 0. Before the day

- [ ] `main` builds and the last CI run is green: `gh run list --branch main -L 3`.
- [ ] A release dry run on the current `main` passes (it builds every package, installs each Linux
      package and runs the self-check inside it, with a throwaway key; it publishes nothing):

      gh workflow run release.yml --ref main
      gh run watch (gh run list --workflow release.yml -L 1 --json databaseId --jq '.[0].databaseId')

- [ ] The Windows package opens normally when installed (not only the self-check): see
      [section 6](#6-check-the-msix-on-this-computer), including **6a** (signed, as a new standard user)
      before every Store upload.
- [ ] The real-phone test of the companion is done (in Rann.Roost.Mobile: its release checklist, step 0).
- [ ] The release notes read well in both languages. The GitHub release body is the English file
      followed by the French one (assembled by the workflow); `update.json` carries both.

## 1. The release key (once, ever)

Losing this key orphans every installed copy's update check; leaking it lets someone sign updates.
Keep the secret file offline (a USB key or your password manager) with a second copy.

In your own PowerShell window:

    cd C:\Users\PSchi\source\repos\Rann.Roost
    $env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
    .\gradlew :tools:release:run --args="keygen E:\RANN-keys\rann-release.key"

(Use your own offline folder instead of `E:\RANN-keys`; the tool refuses to overwrite an existing key.)
It writes the secret to that file and the public key to
`core/update/src/main/resources/hfm/update/release-key.pub`, and prints the key id.

- [ ] Add the secret to the repository, without it showing on the screen:

      gh secret set HFM_RELEASE_KEY --body (Get-Content -Raw E:\RANN-keys\rann-release.key).Trim()

- [ ] Commit the **public** key (only that file):

      git add core/update/src/main/resources/hfm/update/release-key.pub
      git commit -m "RANN's release public key"
      git push

## 2. The Android upload key (once, ever)

Google Play App Signing keeps the app signing key; this upload key signs the bundles you upload. Its
secrets now go in the phone app's repository, Rann.Roost.Mobile (its release checklist, step 1).
If the key does not exist yet, make it in your own PowerShell window:

    $kt = "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\keytool.exe"
    & $kt -genkeypair -v -keystore E:\RANN-keys\upload.jks -keyalg RSA -keysize 4096 -validity 10000 -alias upload

keytool asks for the keystore password, your name and organization (RANN), and the key password.
Keep `upload.jks` and both passwords offline, with a second copy.

- [ ] Check this repository's secrets (names only, never values): `gh secret list`. Expect
      `HFM_RELEASE_KEY`; the four `ANDROID_*` belong to Rann.Roost.Mobile and can be removed here
      (`gh secret delete ANDROID_KEYSTORE_BASE64`, and the same for the other three).

## 3. Version 1.0.0 and the tag

- [ ] In `gradle.properties`, change `hfm.version=0.9.0` to `hfm.version=1.0.0`. Then:

      git pull
      .\gradlew build
      git commit -am "Version 1.0.0"
      git push

- [ ] Wait for CI on that commit to pass (`gh run watch`), then tag it:

      git tag -a v1.0.0 -m "RANN's Roost 1.0.0"
      git push origin v1.0.0

## 4. What the release workflow does

The tag starts **Release** (`gh run watch (gh run list --workflow release.yml -L 1 --json databaseId --jq '.[0].databaseId')`):

1. **Version:** stops unless the tag is `v` + `hfm.version` and the public key is in the repository.
2. **Linux packages** (Ubuntu): builds the .deb, .rpm and AppImage, installs the .deb (Ubuntu) and the
   .rpm (Fedora container) and runs the self-check inside each, and inside the AppImage.
3. **Microsoft Store package** (Windows): builds the unsigned MSIX and runs the self-check in its app
   image. Kept as the `msix` artifact (the Store signs it).
4. **Sign and draft the release:** signs every Linux package with the release key, writes
   `SHA256SUMS` and `update.json` (with both release notes), checks every signature with `minisign`,
   and creates a **draft** GitHub release `v1.0.0` with the English notes. Nothing is public yet.

If a job fails, nothing is published. Fix on `main`, delete the tag (`git push origin :v1.0.0`,
`git tag -d v1.0.0`) and the draft if one was made, and tag again.

## 5. Publish the GitHub release

- [ ] Look at the draft: `gh release view v1.0.0` (or on GitHub, Releases). Expect the .deb, .rpm
      and AppImage, a `.minisig` for each, `SHA256SUMS`, `update.json` and their `.minisig`.
- [ ] Check one download yourself:

      gh release download v1.0.0 --pattern "*.deb*" --pattern "SHA256SUMS*" -D $env:TEMP\rel100
      # with minisign installed (winget install jedisct1.minisign):
      minisign -Vm "$env:TEMP\rel100\ranns-roost_1.0.0_amd64.deb" -p core\update\src\main\resources\hfm\update\release-key.pub

- [ ] Read the release text: the workflow puts the English notes first, then a line and the French
      notes under a "Français" heading (from `docs/releases/1.0.0.en.md` and `.fr.md`).
- [ ] Publish: `gh release edit v1.0.0 --draft=false --latest`. From now on, Linux copies that agreed to
      check will see later releases through `releases/latest`.

## 6. Check the MSIX on this computer

Download the artifacts of the tag's run:

    $run = gh run list --workflow release.yml -L 1 --json databaseId --jq '.[0].databaseId'
    gh run download $run -n msix -D $env:TEMP\rel100\msix
    gh run download $run -n android -D $env:TEMP\rel100\android

The downloaded MSIX is unsigned, so Windows will not install it as a file; check it from a local build
of the same commit instead (developer mode is on):

    .\gradlew :app:desktop:packageMsix
    Add-AppxPackage -Register app\desktop\build\msix\layout\AppxManifest.xml
    $fam = (Get-AppxPackage RANN.RannsRoost).PackageFamilyName
    Start-Process "shell:AppsFolder\$fam!RANNsRoost"

- [ ] The welcome or unlock window opens (a "Failed to launch JVM" box means a JDK module is missing
      from the packaged runtime; see `suggestRuntimeModules` in the README).
- [ ] Again with Java Access Bridge on, as the Microsoft Store's testers run it (1.0.0 was rejected for
      this): close the app, create `%USERPROFILE%\.accessibility.properties` containing
      `assistive_technologies=com.sun.java.accessibility.AccessBridge`, start it again, then delete the
      file. A start-up error is also written to `%LOCALAPPDATA%\RANN's Roost\startup-error.log`.
- [ ] Close it, then `Get-AppxPackage RANN.RannsRoost | Remove-AppxPackage`.

The packaged app reads your real settings (recent households, language), but its writes are kept
apart; your real household is not touched unless you open it.

### 6a. As the Store installs it: signed, under a new standard user

The developer-mode check above misses what the Store's testers do: install the package into the
read-only `WindowsApps` folder and start it as a **fresh standard user**. 1.1.0 and 1.1.1 passed every
check above and still failed this way (Java's socket-file pipe under MSIX file redirection, fixed in
1.1.2). So test **the exact `msix` artifact you will upload**, signed with a throw-away test certificate.
In your own **administrator** PowerShell window, with Developer Mode off:

    $publisher = 'CN=A48DF0F5-6188-4675-BF5D-92E006CC2E83'   # the manifest's Publisher
    $signtool  = 'C:\Program Files (x86)\Windows Kits\10\bin\10.0.26100.0\x64\signtool.exe'
    $msix = (Get-ChildItem $env:TEMP\rel100\msix\*.msix).FullName
    $cert = New-SelfSignedCertificate -Type Custom -Subject $publisher -KeyUsage DigitalSignature `
        -FriendlyName 'RANNs Roost local Store test (delete after)' -CertStoreLocation 'Cert:\CurrentUser\My' `
        -TextExtension @('2.5.29.37={text}1.3.6.1.5.5.7.3.3', '2.5.29.19={text}') -NotAfter (Get-Date).AddDays(7)
    Copy-Item $msix C:\Users\Public\test.msix -Force
    & $signtool sign /fd SHA256 /sha1 $cert.Thumbprint C:\Users\Public\test.msix
    Export-Certificate -Cert $cert -FilePath C:\Users\Public\test.cer | Out-Null
    Import-Certificate -FilePath C:\Users\Public\test.cer -CertStoreLocation Cert:\LocalMachine\TrustedPeople | Out-Null
    Get-AppxPackage -AllUsers RANN.RannsRoost | Remove-AppxPackage -AllUsers   # same version, other contents: blocked otherwise
    $pw = Read-Host -AsSecureString "Password for the test user"
    New-LocalUser -Name RoostTest -Password $pw -Description "Store test - delete after"
    Add-LocalGroupMember -Group Users -Member RoostTest

Switch user to **RoostTest**, and in a normal (not administrator) PowerShell window:

    Add-AppxPackage -Path C:\Users\Public\test.msix
    Start-Process "shell:AppsFolder\RANN.RannsRoost_9cg7jhsmyye38!RANNsRoost"

- [ ] The welcome window opens (no "could not start" box). If it fails, the log is in
      `C:\Users\RoostTest\AppData\Local\Packages\RANN.RannsRoost_9cg7jhsmyye38\LocalCache\Local\RANN's Roost\`
      (MSIX redirects it from the path the message shows).
- [ ] Open a sample household and update the rates (proves network connections work).
- [ ] Clean up as yourself, administrator window (the certificate can sign packages this computer trusts):

      Get-ChildItem Cert:\LocalMachine\TrustedPeople, Cert:\CurrentUser\My | Where-Object Subject -eq $publisher | Remove-Item
      Get-AppxPackage -AllUsers RANN.RannsRoost | Remove-AppxPackage -AllUsers
      Remove-Item C:\Users\Public\test.msix, C:\Users\Public\test.cer
      Remove-LocalUser RoostTest    # then delete C:\Users\RoostTest

Upload the **unsigned** artifact (`$msix`), never `test.msix`.

## 7. Microsoft Store (Partner Center)

Partner Center > Apps and games > **RANN's Roost** > Start submission (before the name reservation lapses).

- [ ] **Packages:** upload `RANNsRoost-1.0.0-x64.msix` from the `msix` artifact. Partner Center checks
      the identity (`RANN.RannsRoost`, publisher `CN=A48DF0F5-…`) against the
      reservation; a mismatch means `app/desktop/packaging/msix/store-identity.properties` is out of date.
- [ ] **Restricted capability `runFullTrust`:** explain that this is a desktop (Win32) app packaged as
      MSIX, which needs full trust to run.
- [ ] **Pricing and availability:** CAD $19.99 (SRS 16.2), set before the first submission so nobody
      installs it free; markets your choice, Canada at least. A launch discount is a sale price.
- [ ] **Properties:** category Personal finance; privacy policy
      https://www.rann.ca/rann-apps/rann-roost/privacy-policy-en; website https://www.rann.ca/rann-apps/rann-roost;
      support info-rann-apps@NorthMail.ca.
      **Check the "Generative AI" box** in the product declarations: the app's optional AI reading
      sends documents to Claude (Anthropic). Leaving it unchecked fails certification under policy 11.16
      (1.1.2, 2026-10-21).
- [ ] **Age ratings:** the IARC questionnaire (no violence, no user interaction with strangers, no purchases in the app).
- [ ] **Store listings:** English (Canada) and French (Canada) from [`docs/store/microsoft-store.md`](store/microsoft-store.md),
      with the eight screenshots per language in `docs/store/screenshots/desktop-en` and `desktop-fr`
      and the tiles from `branding/`. Check the texts still fit: `python tools/dev/check_store_texts.py`.
      Each screenshot's caption and alt text are in that file's "Screenshots: captions and alt text".
- [ ] **Store logos:** from `branding/store/`: 2:3 poster `poster-1440x2160.png`, 1:1 box art
      `box-2160x2160.png`, tiles `tile-300x300.png`, `tile-150x150.png`, `tile-71x71.png`, and the 16:9
      super hero art `hero-3840x2160.png` / `hero-1920x1080.png` (required once a trailer is added).
      Rebuild them with `python tools/dev/make_store_logos.py`.
- [ ] **Trailers (optional):** the 1080p MP4s are not kept in this repository (they are in the owner's
      `Downloads\rann-trailers\en` and `fr`, each with a thumbnail PNG, a `.vtt` caption file and a
      title). Upload 01 to 07; keep 08 (the phone companion) for rann.ca, since a Store trailer about
      a separately sold Android app may not pass certification.
- [ ] Submit. Certification usually takes one to three business days.

## 8. Google Play (Play Console)

The phone app is released from its own repository, Rann.Roost.Mobile: follow its
`docs/release-checklist.md` (secrets, version and tag, then Play Console).

## 9. rann.ca (Google Sites)

- [ ] **Privacy policy pages** (`/privacy-policy-en` and `/privacy-policy-fr`): paste the changed
      sections from `website/privacy-policy-en.md` and `-fr.md`. Since the version on the site, check at
      least "Calendars on your phone" and "Location on the phone" (added with calendars and trips),
      "Optional features that use your own accounts" (now in the present tense, and AI reading of
      statements with hidden parts sent as plain grey), and the **Dictation** paragraph under "The phone
      app and Google". If the page changes, set its "Effective" date to the day you publish it, in both
      languages and in the files.
- [ ] **Home pages** (`/rann-roost` and `/rann-roost-fr`): under "Get RANN's Roost", replace each
      "Coming soon" with a button to the Microsoft Store listing, the GitHub release
      (https://github.com/ScouterPerry01/Rann.Roost/releases/latest) and,
      once in production, the Play listing.
- [ ] **Images:** upload `website/images/` to Google Drive and replace the pictures on both home pages
      (they were retaken on 2026-10-06), with the alt text written beside each in the page files.

## 10. After publishing

- [ ] Each store listing, the GitHub release and rann.ca link to one another and to the privacy policy.
- [ ] Install from the Microsoft Store on this computer and open your household once.
- [ ] Note the release in the development plan's decisions log (date, version, what went where).
- [ ] Keep `gradle.properties` at 1.0.0 until the next release; bump it (1.0.1 or 1.1.0) in the first
      commit of that release.
