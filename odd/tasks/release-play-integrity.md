# Feature: release build with Play Integrity so every service works on any device

Branch: `release/play-integrity` · Delivery: single-pr · Runner: `gradlew testDebugUnitTest`
Risk: item 5 (release signing and App Check attestation are delivery/environment surfaces).

## Specs

S1 — The APK handed to classmates runs the app as a **release** build (Play Integrity
App Check), so no device needs its debug token registered.
Verbatim request: «quiero que todo quede chido, ya que esta app debe de funcionar cada uno
de los servicios que ofrecemos en los diferentes dispositivos de mis compañeros, arranca
con el camino 2».

S2 — Every service the app offers keeps working on those devices: Auth, Firestore, RTDB,
Cloud Messaging, Remote Config, Cloudinary and the AI assistant.

S3 — No signing secret enters the repository: keystore and passwords stay in files git
already ignores (`*.jks`, `keystore.properties`).

S4 — A clone without the keystore still compiles: the release build falls back to unsigned
instead of failing the build.

S5 — The owner gets the exact console steps and the certificate SHA-256 needed for
Play Integrity, plus how to verify the signed APK before distributing it.

## Tasks

T1 — Release keystore `keystore/aulasegura-release.jks` (RSA 4096, PKCS12, alias
`aulasegura`, 10 000 days) and `keystore.properties`, both untracked. Route: inline.
T2 — `signingConfigs.release` in `app/build.gradle.kts`, applied to the release build type,
with an unsigned fallback when the properties file is missing. Route: inline.
T3 — Build and verify: `assembleRelease`, `apksigner verify --print-certs`, `aapt dump
badging`, `FLAG_DEBUGGABLE` absent, certificate equals the release keystore. Route: inline.
T4 — Register the release SHA-256 on the Firebase Android app (`firebase apps:android:sha:create`).
T5 — Firebase console: Play Integrity provider advanced settings for distribution outside
Google Play, and confirm App Check enforcement per API (owner-only step). Done: the release
SHA-256 is registered, `PLAY_RECOGNIZED` is no longer required, `LICENSED` stays off, and the
minimum device integrity level is "device integrity".
T6 — Document the flow (CONTEXT.md §9.3, FASE0 bloque L) and deliver the APK. Done: the
signed APK was copied to `C:\Users\diego\Documents\AulaSegura.apk`.

## Log

L1 — (2026-11-22) «SI arma el camino dos, quiero que todo quede chido, ya que esta app debe
de funcionar cada uno de los servicios que ofrecemos en los diferentes dispositivos de mis
compañeros, arranca con el camino 2».

L2 — Why a release build is the answer: the delivered APK was debug-signed, so
`installAppCheckProvider()` chose `DebugAppCheckProviderFactory`, which mints a different
debug token per install. Firebase has enforced App Check for AI Logic since 2026-11-02, so
unregistered devices get `403 PERMISSION_DENIED`, mapped to `AiFailure.NOT_CONFIGURED`.
A release build selects `PlayIntegrityAppCheckProviderFactory` and the app's signing
certificate becomes the identity, with no per-device token.

L3 — The debug certificate was already registered in Firebase (SHA-1 `0a58d36d…`, SHA-256
`9b67810f…`), which is why the owner's device worked. Play Integrity needs the **release**
certificate registered as well.

L4 — First real release build had never run. It failed twice before reaching the APK:
`produceReleaseComposeMapping` needs `org.jetbrains.kotlin:compose-group-mapping:2.4.21`,
which `--offline` could not resolve, and then `lintVitalRelease` reported
`InvalidFragmentVersionForActivityResult` as fatal on `MainActivity.kt:34`.

L5 — Root cause of the lint failure, verified from the detector bytecode in
`androidx.activity:activity:1.13.0` (`lint.jar`, class
`androidx.activity.lint.ActivityResultFragmentVersionDetector`): it slices the resolved
coordinate after `androidx.fragment:fragment:` and compares the remainder lexicographically
against `"1.3.0"`. `androidx.fragment:fragment` resolves to **1.5.4** through appcompat
1.7.1 (see `dependencyInsight`), and the app has no Fragment at all — `MainActivity` is a
plain `ComponentActivity` with Compose. The check is therefore a false positive, and only
that check is disabled in `lint { }`; every other vital issue still fails the build.

L6 — `optimization.enable` was turned off for this release on purpose: the minified binary
has no device-level smoke test yet, and the deliverable must behave like the build that was
already validated. Re-enable it once the tester devices exist.

L7 — T3 done: `assembleRelease` green in 5m 54s. `app-release.apk` is 26 748 349 bytes,
signed by `CN=AulaSegura, OU=Mobile, O=AulaSegura, C=MX`
(SHA-256 `4c17ebc39016d9fb976086a0885de2fc29b9f933924f1c67868687bd246d08a2`), and
`aapt dump xmltree` confirms there is no `android:debuggable`, so the app takes the
Play Integrity provider at startup. T4 done: certificate registered through
`firebase apps:android:sha:create` (SHA id `a8a1615792d6f93a`). Independent verifier
unavailable (`permission.rejected: subagent`), recorded as such; Risk stays item 5.

L8 — Suite after the Gradle and lint change: `gradlew testDebugUnitTest` green in 48s,
111 suites / 701 tests / 0 failures / 0 skipped. The delivered artifact is
`C:\Users\diego\Documents\AulaSegura.apk`, same 26 748 349 bytes and SHA-256
`6E46B1CD690A5CFCE7F63029E1FD92C305FE238F8D8BB70A012E29CDB5EA02F7` as the build output.
No device was attached, so runtime behavior of this binary on a phone is still unverified.

L9 — Device verification (OPPO CPH2669, Android 14, `verifiedbootstate=green`,
`flash.locked=1`, `release-keys`, no root). "Run" in Android Studio installs the `debug`
variant (`flags=[DEBUGGABLE ...]`, `run-as` worked), so the first on-device test was still
the debug provider. After uninstalling and installing the release APK (`run-as` then said
`package not debuggable`), logcat proved the release path was active but rejected:
`PlayIntegrityAppCheckProvider.getToken` → `403 App attestation failed`.
Root cause was console configuration, two independent conditions: the App Check Play
Integrity app only had the **debug** fingerprint `9b67…`, and `PLAY_RECOGNIZED` was still
required — a sideloaded APK only ever gets `UNRECOGNIZED_VERSION`. After registering
`4c17ebc3…` and clearing `PLAY_RECOGNIZED` (keeping "device integrity" as the minimum level),
the change took roughly 30 seconds to propagate: a 19:10:20 attempt still returned 403, a
19:10:55 attempt succeeded. Verified end to end at 19:13: the model answered
"Sobre Enrique Peña Nieto: tareas pendientes, sin registro. Sobre Juan Pablo Rios Trujillo:
tareas pendientes, sin registro." with no App Check error in logcat.

L10 — Error mapping follow-up: an App Check attestation rejection reached the tutor as
`AiFailure.UNKNOWN` ("intenta de nuevo en un rato"), which is wrong copy for a setup
problem. `aiFailureFor` now maps a `FirebaseException` whose message contains
`App attestation failed` to `NOT_CONFIGURED`, and leaves every other `FirebaseException`
in `UNKNOWN`. Two JVM tests pin both branches. Constructing `FirebaseException` in a unit
test needs `testOptions.unitTests.isReturnDefaultValues = true`, because its constructor
calls `Preconditions.checkNotEmpty` → `android.text.TextUtils`; the flag is scoped to unit
tests and documented in `app/build.gradle.kts`. Suite: 111 suites / 705 tests / 0 failures.
Rebuilt artifact after the change: `C:\Users\diego\Documents\AulaSegura.apk`, 26 731 965
bytes, SHA-256 `794F979EF44F3F6AFF2DE87D63227DBF04713F972622F913533B55A03F420A0F`, same
release certificate (`4c17ebc3…`) so it installs over the previous build with `-r`.
Smoke test on the device at 19:30: the assistant answered and logcat had no App Check or
quota error.
