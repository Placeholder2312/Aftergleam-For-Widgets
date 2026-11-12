# Implementation plan and verified status

## Work sequence

### Phase 0 — repository and contract audit (complete)
- Keep the original upstream read-only.
- Work only on `feature/research-widgets-integration` in the Aftergleam fork and `feature/aftergleam-integration` in this repository.
- Freeze provider API v1 fields and permission name before building the companion against it.

### Phase 1 — Aftergleam provider and handoff (implemented; automated CI green)
- Read today's persisted digest in its stored rank order; do not rank in the companion.
- Read currently saved records ordered by the last SAVED signal timestamp.
- Expose only the seven documented metadata columns through a signature-protected, read-only ContentProvider.
- Open a paper through the signature-protected PaperOpenActivity trampoline, not by exporting MainActivity as a new API.
- Invalidate the two feed URIs and send a package-targeted, signature-protected update broadcast after digest, save/unsave, import, restore, and notification-save writes.
- Current automated coverage: feed order, saved order, empty feeds, unsupported path/selection/projection, write rejection, ID validation, and permission declaration metadata.

### Phase 2 — companion widgets and settings (implemented scaffold; automated CI green)
- Native AppWidget/RemoteViews app with package `com.placeholder2312.researchwidgets`.
- Paper Strip uses StackView, per-instance Recommended/Saved preferences, empty/disconnected states, and previous/next controls.
- Decorative Aftergleam tile uses the supplied brand mark and opens the customized fork.
- Companion receiver handles protected data-change notifications and refreshes widgets; boot/update lifecycle handling restores widget state.

### Phase 3 — shared signing and cross-app integration (blocked on owner-provided secret)
- Use one locally controlled release keystore for both applications.
- Configure identical `RESEARCH_WIDGETS_KEYSTORE_BASE64`, `RESEARCH_WIDGETS_KEYSTORE_PASSWORD`, `RESEARCH_WIDGETS_KEY_ALIAS`, and `RESEARCH_WIDGETS_KEY_PASSWORD` Actions secrets in both repositories.
- Re-run both CI workflows and compare APK signer certificate SHA-256 fingerprints.
- Install both APKs signed by that certificate and verify provider query succeeds; verify an APK signed with a different certificate is denied.
- Verify the explicit paper-open trampoline opens the requested cached paper.

### Phase 4 — robustness and device verification (pending)
- Add instrumentation coverage for actual cross-package permission grant/denial, lifecycle, and open-paper routing.
- Exercise StackView vertical gestures, button fallback, resizing, multiple instances, empty feed, source app missing, process restart, and app update on an emulator.
- Test the same interactions on Samsung One UI; launchers can differ in StackView gesture handling and grid sizing.
- Confirm database migrations and JSON backup/restore regression tests remain green.

### Phase 5 — migration and release (not started; user-controlled)
- Do not uninstall or modify the official installation automatically.
- User exports and verifies JSON backup, then chooses when to replace the official package.
- Restore backup into the custom fork; cached paper metadata is refetched and is not included in JSON.
- Install companion, validate both feeds and widgets, then consider migration complete.
- Do not merge to either main branch or publish a release without explicit user approval.

## Latest verified CI
- Aftergleam fork branch `feature/research-widgets-integration`, commit `72563b15f46d76e2555e57797c546a077ff126fb`: workflow run [43199854746](https://github.com/Placeholder2312/Aftergleam-For-Widgets-/actions/runs/43199854746) succeeded; `testDebugUnitTest` and `assembleDebug` completed and artifact `aftergleam-widget-fork-debug-apk` was uploaded.
- Companion branch `feature/aftergleam-integration`, commit `92338ddba6905481bb85b77baaa242f2be88f0be`: workflow run [43201882824](https://github.com/Placeholder2312/Research-Widgets/actions/runs/43201882824) succeeded; `testDebugUnitTest` and `assembleDebug` completed and artifact `research-widgets-debug-apk` was uploaded.
- Both logs explicitly report that no shared IPC keystore was configured. These are build-validation APKs only; they are not mutually authorized for signature-level IPC.

## Current risks / release blockers
- The current Robolectric permission test checks the manifest declaration, not an actual cross-package denial/allowance. Add instrumentation tests for this.
- CI has not yet built with the same signing identity in both repositories.
- No emulator or physical Samsung One UI verification is recorded.
- The widget's visual size and StackView swipe behavior must be verified on the target launcher.
- Both main branches remain unchanged; no release or migration has occurred.
