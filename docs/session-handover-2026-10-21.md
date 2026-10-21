<!--
File path and name: docs/session-handover-2026-10-21.md
Modified On Timestamp: 2026-10-21 @ 12:10 EDT
Created On Timestamp: 2026-10-21 @ 12:10 EDT
File Description: Handover for the 2026-10-21 session (Store certification of 1.1.2, policy 11.16).
Uses: docs/MS.Cert.Issue/ (git-ignored certification reports), docs/release-checklist.md.
Used By: The next session (via docs/NOW.md).
Purpose: Record why the first 1.1.2 certification failed and what was changed.
-->

# Session handover — 2026-10-21

## 1. Certification result for 1.1.2 (first try)

- Partner Center failed the submission. The support zip
  (`docs/MS.Cert.Issue/cert-report-Rann's Roost-support-files.zip`; the older 1.1.0/1.1.1 zip moved to
  `docs/MS.Cert.Issue/old/`) holds one screenshot, `11.16 Rann_s Roost.png`.
- **11.16** is the policy for apps with generative AI. The cause: the "Generative AI" box under the
  submission's Properties was not checked, while the app has optional AI reading (Claude, ADR 0009).
- The screenshot shows the app open on Settings > AI reading. The tester got past start-up and unlock, so
  the 1.1.2 fix (`LoopbackPipe`, see the 2026-10-20 handover) works under the Store's test setup.

## 2. What the owner did

- Checked the Generative AI box and resubmitted the same 1.1.2 MSIX on 2026-10-21. No code change.

## 3. What changed in the repository

- `docs/release-checklist.md` § 7 Properties: new step to check the Generative AI box (explains why).
- `docs/NOW.md`: updated for the resubmission.
- Committed on the new topic branch `docs/store-genai-declaration` (`00303178`), with the owner's approval.
  These end-of-session files are a second commit on that branch once approved. Not pushed.

## 4. Builds and tests

- None run: only Markdown files changed.

## 5. Next

- Push and merge the docs branch (needs approval), then wait for certification. If the AI declaration
  brings more questions, check the rann.ca privacy policy covers what AI reading sends to Anthropic.
