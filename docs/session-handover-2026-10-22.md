<!--
File path and name: docs/session-handover-2026-10-22.md
Modified On Timestamp: 2026-10-22 @ 16:05 EDT
Created On Timestamp: 2026-10-22 @ 16:05 EDT
File Description: Handover for the 2026-10-22 session: RANN's Roost 1.1.2 live on the Microsoft Store.
Uses: docs/NOW.md, docs/store/microsoft-store.md, website/rann-roost-en.md, docs/release-checklist.md.
Used By: Claude Code and the owner when resuming.
Purpose: Record the detail behind the session-log block and NOW.md.
-->

# Session handover — 2026-10-22

## 1. Microsoft Store: live

- RANN's Roost 1.1.2 passed certification after the 2026-10-21 resubmission (Generative AI box checked)
  and is live on the Microsoft Store at CAD $19.99, one-time.
- The owner put the Windows Store link on the live rann.ca pages.
- Not yet recorded in the repository: the Store link and Store ID (Partner Center > Product identity,
  12 characters starting with `9`). They belong in `docs/release-checklist.md` and `website/rann-roost-*.md`.

## 2. Linux link for rann.ca

- Use https://github.com/ScouterPerry01/Rann.Roost/releases/latest. Checked on 2026-10-22: it redirects to
  `releases/tag/v1.1.2`, and it follows each new release, so the site never needs changing.
- The v1.1.2 release has `ranns-roost_1.1.2_amd64.deb`, `ranns-roost-1.1.2-1.x86_64.rpm` and
  `ranns-roost-1.1.2-x86_64.AppImage`, each with a `.minisig`, plus `SHA256SUMS` and `update.json`.
- Do not link the tag page or a single file: those stay pinned to 1.1.2.

## 3. Wording to fix (found while summarizing the app)

- **Store listing** (`docs/store/microsoft-store.md`, "THE WHOLE HOUSEHOLD", EN and FR): says RANN's Roost
  Mobile is "sold separately on Google Play". The phone app is not on Play yet; reword as "coming soon to
  Google Play" at the next listing edit, then back once it is published.
- **Website** (`website/rann-roost-en.md` / `-fr.md`): "no cloud" and "Nothing passes through RANN or anyone
  else" vs the listing's "or through a cloud folder or email of your own, encrypted end to end". Mention the
  owner's-own-cloud/email option on the site so the two agree.
- **Website repo copy**: Windows and Linux boxes still say "Coming soon"; update to match the live site.
- Android box stays "Coming soon" (not on Play yet).

## 4. Installing your own paid app

- No purchase needed: Partner Center's **Promotional codes** page (under the app's Engage/Grow menu; exact
  name not checked) makes free codes, up to 1,600 per app every 6 months. Redeem at microsoft.com/redeem or
  in the Store app.
- The licence follows the Microsoft account. For the `RoostTest` check, sign in to the Store app only with
  the same account, install, sign out, then remove the Windows account.

## 5. Decisions

- None taken while the owner was away. Recommended (owner to confirm by using it): link Linux via
  `releases/latest`.

## 6. Builds and tests

- None run: no code changed this session.
