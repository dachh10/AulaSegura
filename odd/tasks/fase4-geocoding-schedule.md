# Fase 4 — Nombre automático del plantel y bloqueo por horario

Branch: `fix/fase4-geocoding-schedule`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`.

## Specs

**S1** — User request, verbatim: "solo me gustaria que en configuracion del plantel al ingresar la latitud y longitud del pnatel que qureamos automaticamnte se llene el campo de nombre tambien".

**S2** — User request, verbatim: "en horario de pase de lista confirmame que si de verdad funcione por ejemplo si pongo un horario de 7:00 am a 1:pm que solo en ese horario deje pasar lista y agrega justo eso que selecciones la hora con am y pm."

**S3** — Confirmed fact: `entryTime`/`exitTime` are only validated, persisted and displayed; no teacher-side code reads them. The only roll-call gates are Monday–Friday and GPS. The campus form uses a plain 24-hour text field.

**S4** — Auto-fill the school name from coordinates using Android `Geocoder`, only while the user has not typed a name manually. A failed/unavailable lookup must never clear or overwrite a name.

**S5** — The campus form uses a Material time picker with AM/PM; the stored value stays canonical `HH:mm` so the rules/data contract does not change.

**S6** — The teacher's school data carries the configured window and the roll-call entry point refuses to start outside it. A missing/blank window means "no time restriction" (backward compatible). Weekday and GPS gates stay as they are.

## Tasks

- **T1** — S1,S4: reverse geocoding on coordinate change with a user-edited guard. Route: inline. Evidence: SchoolNameResolver port + Android Geocoder impl, VM auto-fill with `nameEditedByUser`, AdminCampusViewModelTest GREEN.
- **T2** — S2,S5: AM/PM time picker in the campus form. Route: inline. Evidence: pending.
- **T3** — S3,S6: carry the schedule into the teacher school model and gate roll call. Route: inline. Evidence: pending.
- **T4** — all: full verification, FASE4.md update, merge/push on approval. Route: inline. Evidence: pending.

## Log

**L1** — User request (verbatim): "okey, ya quedo bien, solo me gustaria que en configuracion del plantel al ingresar la latitud y longitud del pnatel que qureamos automaticamnte se llene el campo de nombre tambien, y en horario de pase de lista confirmame que si de verdad funcione por ejemplo si pongo un horario de 7:00 am a 1:pm que solo en ese horario deje pasar lista y agrega justo eso que selecciones la hora con am y pm."

**L2** — Decision: `Geocoder` (platform, no API key) with graceful failure; manual edits win over auto-fill. Time values remain `HH:mm` in Firestore; AM/PM is presentation only.
