# Fase 4 — Prioridad 3 del backlog de arreglos

Branch: `fix/fase4-prioridad3`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`.

## Specs

**S1** — User request, verbatim: "sigue con la prioridad tres". Scope is exactly section 5 of `Prioridades de arreglo.md`, items 3.1 to 3.11. No new features.

**S2** — 3.1/3.2/3.3: no inert controls in the director module; honest empty-state copy; feedback when "Aceptar todas" has nothing to do.

**S3** — 3.4/3.5: the teacher's subject filter must reflect all distinct subjects, and no legacy `"General"` / `"3B"` defaults may leak into canonical groups.

**S4** — 3.6/3.7/3.8: the tutor module must not tell a user to re-register, must use one string for the "no data" state, and must name the selected child on the notices screen.

**S5** — 3.9/3.10: the add-child flow must not keep two live roster listeners, and notices must not read the roster twice per refresh.

**S6** — 3.11: `FASE4.md` must mark stale sections as historical without erasing the record.

## Tasks

- **T1** — S2: director controls and empty states. Route: inline. Evidence: copy/share wired, photo badge disabled, honest empty copy with `adminUsersEmptyMessage` test, accept-all feedback.
- **T2** — S3: teacher filter and legacy defaults. Route: inline. Evidence: distinctGroupSubjects test, per-subject chips, "Sin materia asignada" default, no "3B" fallbacks.
- **T3** — S4: tutor copy and notices child label. Route: inline. Evidence: pending.
- **T4** — S5: duplicate listeners/reads. Route: inline. Evidence: pending.
- **T5** — S6: tracker cleanup. Route: inline. Evidence: pending.
- **T6** — all: full verification, FASE4.md update, merge/push on approval. Route: inline. Evidence: pending.

## Log

**L1** — User request (verbatim): "sigue con la prioridad tres".

**L2** — Order: director → teacher → tutor → listeners/reads → tracker, so each module can be verified on device independently.
