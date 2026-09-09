# Play first, Local second

User decision: maintain only Play (Internet, no notification capture) and Local
(offline, notification capture). Shared UI and financial data architecture stay intact.

- [x] Baseline `:app:assemblePlayDebug`: passed before source changes (2026-09-06).
- [x] Move Full-only code/resources/tests to Local and remove Full flavor/source aliases.
- [x] Remove backdrop rendering outside navigation, preserving control dimensions/actions.
- [x] Gate navigation glass by Android API, hardware acceleration and saved preference; use a solid navigation fallback.
- [x] Prefer VietQR network images in Play; retain offline fallback and Local offline rendering.
- [x] Run Play APK build/unit tests; inspect merged manifest for Internet and absence of capture service.
- [x] Then build/test Local and verify offline manifest and retained notification capture.

AI API integration awaits provider/credential architecture choice. Do not embed a service secret in the APK.
No Room schema change, new dependency, or unrelated staged-file cleanup.

Motion: preserve navigation selection/drag on supported devices; standard clickable tabs
otherwise. Other controls retain size, labels and gestures but use solid theme surfaces.
Runtime device/visual acceptance is separate from Gradle build and unit tests.

## Navigation compatibility correction (2026-09-08)

- Removed the unsubstantiated Xiaomi/Redmi/POCO block. Manufacturer and brand are display metadata only.
- Android 13/API 33 and hardware acceleration remain required; explicit saved user preferences are preserved.
- Settings retain Android release, API level, manufacturer/model and the actual blocking reason.
- Static backdrop audit: content capture excludes navigation; the active-label source samples content,
  and its sibling indicator samples content plus labels. No circular capture was found in this graph.
- These eligibility checks do not establish driver stability; device rendering still requires runtime verification.
- Verification: Play/Local Debug builds passed; 195 Play and 223 Local unit tests passed.
  ADB reports no connected device, so Android 16 rendering acceptance remains pending.

## Verified results (2026-09-06)

- Play Debug + Release/R8: BUILD SUCCESSFUL.
- Local Debug: BUILD SUCCESSFUL.
- Play: {'tests': 192, 'failures': 0, 'errors': 0, 'skipped': 0}
- Local: {'tests': 220, 'failures': 0, 'errors': 0, 'skipped': 0}
- Play Debug/Release merged manifests: Internet present, notification capture absent. Play Debug DEX also checked.
- Local Debug merged manifest: Internet absent, notification capture present.
- 11 relocated source/resource/test files match their original content.
- No connected ADB device: rendering, QR network loading/scanning and device crash acceptance remain unverified.
- AI API remains deferred pending provider/credential choice; existing AI implementation unchanged.
- Build logs: `app/build/reports/stability/`. Existing deprecation/experimental Gradle warnings remain non-fatal.
