# Aftergleam × Research Widgets integration

## Current baseline

- Fork: `Placeholder2312/Aftergleam-For-Widgets-`
- Upstream (read-only): `jakobkreft/Aftergleam`
- Baseline commit: `5975eb8695cf23fe7e76313eb4ddaa5a26b68268`
- Baseline release: 1.8.1 (versionCode 14)
- Development branch: `feature/research-widgets-integration`
- Application ID: `si.jakobkreft.aftergleam` (retain unchanged)
- Database: SQLiteOpenHelper, `aftergleam.db`, schema version 9
- Minimum SDK 26; target SDK 37

This document records the source audit and implementation contract. It does not claim that the provider, companion app, APKs, or device integration are complete.

## Verified existing data paths

- `DigestBuilder.build(..., store = true)` persists the ranked sequence through `Db.markShown(...)`.
- `Db.digestFor(day)` reads that sequence with `ORDER BY rank`; this is the canonical Recommended feed. Do not rank again in the companion.
- `Db.papersById(ids)` resolves paper IDs to cached `Paper` records.
- `Db.allReactions()` reports saved state from the existing reaction table.
- `Db.signalTimes(Signal.SAVED)` supplies the latest save timestamp; `Db.toggleSave()` removes the SAVED signal on unsave, so the retained timestamp represents the last active save.
- `FeedViewModel.openPaperById(id)` resolves an ID locally and opens the normal detail flow.
- The existing notification handoff passes `Notifications.EXTRA_PAPER` to the non-exported `MainActivity`. Keep MainActivity non-exported.

## Provider contract (v1)

Authority: `si.jakobkreft.aftergleam.researchwidgets`

Paths:
- `content://si.jakobkreft.aftergleam.researchwidgets/v1/recommended`
- `content://si.jakobkreft.aftergleam.researchwidgets/v1/saved`

Columns:
- `paper_id` (stable ID)
- `title` (display-ready title)
- `authors` (display-ready author string)
- `source` (publication/archive label)
- `published` (publication date)
- `feed_position` (zero-based order)
- `feed_type` (`recommended` or `saved`)

Do not expose abstracts, signals, reading history, private preferences, file paths, or mutation operations. The provider should accept only the two documented collection URIs, fixed projections, and no caller-supplied SQL selection, sort order, or limit. Unknown paths and unsupported arguments return a clear argument error. Resolve records from a consistent database snapshot and omit stale digest IDs that no longer resolve to a paper.

Recommended: read today's `digestFor(LocalDate.now().toString())` in returned order, then resolve exactly those IDs. An empty or not-yet-built digest is empty; do not invent recommendations.

Saved: select IDs whose current `Reaction.saved` is true, order by `signalTimes(Signal.SAVED)` descending, then resolve those IDs. A save with no timestamp must not be promoted above a timestamped save; use a deterministic fallback (e.g. timestamp 0, then stable ID).

## Security boundary

Define a stable custom permission in the Aftergleam manifest:

`si.jakobkreft.aftergleam.permission.READ_RESEARCH_WIDGETS`

Use `android:protectionLevel="signature"`. Require it as the provider's `readPermission` and `writePermission`, and keep provider write methods unsupported. The provider must be exported only because the companion is a separate package. Framework permission enforcement is mandatory; do not rely solely on caller package-name checks.

Keep `MainActivity` non-exported. Add a narrowly scoped exported trampoline activity protected by the same signature permission. It accepts one bounded paper ID, validates it, and forwards it internally to MainActivity using the existing paper extra. It must not accept arbitrary URLs, SQL, component names, or commands.

For update notifications, use an explicit package-targeted broadcast with a stable action and the signature permission as the receiver requirement. The companion receiver should be exported only as required for cross-package delivery and protected by the Aftergleam signature permission. Include a monotonically increasing data revision if useful for de-duplicating refreshes. Send updates after the successful database write/transaction for digest rebuilds, saves, unsaves, and completed library import/restore. Do not poll or start a persistent service.

## Signing

Both release APKs must be signed using the same locally controlled release certificate. Keep keystore files and passwords outside Git, supplied through environment variables or local Gradle properties. The same signing certificate is also required for debug builds to exercise the signature permission across packages; using each project's unrelated default debug key will fail IPC permission checks. Never publish or commit secrets.

The custom fork cannot update an official/F-Droid build signed by a different certificate. This fork is not F-Droid update-compatible.

## Companion app

Keep Research Widgets in a separate repository with application ID `com.placeholder2312.researchwidgets`. It reads only the v1 provider contract, keeps widget-instance configuration independently, and performs no feed ranking. Prefer AppWidget/RemoteViews and a StackView/RemoteViewsService collection for the vertical paper strip. Verify gesture behavior on the target launcher; retain explicit previous/next controls as an accessible fallback. The decorative Aftergleam tile must reuse the actual Aftergleam brand artwork from the fork rather than inventing an unrelated mark.

## Test gates

Before shipping, run existing tests first, then add provider ordering/empty/security/invalid-argument tests; saved state and ordering tests; paper-open trampoline validation tests; import/restore and DB migration regression tests; and companion tests for connection, feed parsing, per-instance mode, stack navigation, clicks, empty/disconnected state, refresh, restart, and multiple instances. Build both debug APKs, verify package IDs and signer certificate equality, then test on an emulator and physically on Samsung One UI. No uninstall or migration should be performed on the user's phone by an agent.

## Upstream synchronization

Keep `jakobkreft/Aftergleam` read-only. Fetch upstream changes and merge/rebase them into this feature branch after reviewing conflicts, especially around `AndroidManifest.xml`, `Db.kt`, `FeedViewModel.kt`, and `MainActivity.kt`. Keep the provider API versioned and avoid editing upstream-owned ranking or backup semantics. Do not merge this branch to `main` or publish a release without user approval.
