# Aftergleam × Research Widgets integration

## Verified baseline
- Fork: `Placeholder2312/Aftergleam-For-Widgets-`; upstream `jakobkreft/Aftergleam` is read-only.
- Base: `5975eb8695cf23fe7e76313eb4ddaa5a26b68268`, release 1.8.1 / code 14.
- Development branch: `feature/research-widgets-integration`.
- Existing app ID retained: `si.jakobkreft.aftergleam`; SQLite schema remains version 9.
- minSdk 26; targetSdk 37.

## Provider API v1
Authority: `si.jakobkreft.aftergleam.researchwidgets`.
- `content://si.jakobkreft.aftergleam.researchwidgets/v1/recommended`
- `content://si.jakobkreft.aftergleam.researchwidgets/v1/saved`

Columns: `paper_id`, `title`, `authors`, `source`, `published`, `feed_position`, `feed_type`. No abstract, reaction ledger, reading history, preference, file path or write operation is exposed. The provider rejects unknown paths, SQL filters/sort clauses, and unknown projection columns.

Recommended reads today's `Db.digestFor(day)` and resolves those IDs. It does not implement ranking. Saved selects current true flags from `Db.allReactions()` and sorts by `Db.signalTimes(Signal.SAVED)` descending. Stale IDs without locally cached paper metadata are skipped; the JSON backup does not include cached paper metadata.

## Security and updates
The provider and explicit-only `PaperOpenActivity` require `si.jakobkreft.aftergleam.permission.READ_RESEARCH_WIDGETS`, Android signature protection. The trampoline accepts a bounded paper ID, confirms the paper exists locally, then forwards it through the existing notification paper-opening route. It does not add a new public MainActivity API.

After successful writes, the app notifies both provider URIs and sends an explicit package-targeted broadcast requiring the same signature permission. Hooks cover daily digest generation, foreground digest rebuild, save/unsave, import completion, backup restore, and the keyword-notification Save action. No polling or persistent service is added.

## Signing and compatibility
Both APKs must be signed with the same locally controlled certificate to pass signature permission checks. Their default per-project debug keys will not match. Keep keystores/passwords outside Git. An official or F-Droid APK has a different signing identity and cannot be updated in place by this fork. This project does not claim F-Droid update compatibility.

## Companion project
Independent repository: `Placeholder2312/Research-Widgets`; app ID `com.placeholder2312.researchwidgets`; development branch `feature/aftergleam-integration`. The companion must declare provider visibility in `<queries>`, request the read permission, and protect its exported change receiver with that permission.

## Tests still required
Provider Robolectric tests cover digest order, saved-state filtering/order, empty feeds, selection/projection validation, write rejection, ID validation and manifest permission metadata. Actual cross-package permission denial/allowance with a shared certificate, database migration, backup/restore regression, widget host gestures, restart, emulator and physical Samsung One UI tests remain release gates.

## Upstream synchronization
Keep upstream read-only. Fetch upstream changes into this feature branch after reviewing changes to the manifest, `Db.kt`, `FeedViewModel.kt`, `MainActivity.kt`, and this provider contract. Do not merge into `main` or publish a release without owner approval.
