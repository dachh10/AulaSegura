# Fase 4 — Prioridad 2 del backlog de arreglos

Branch: `fix/fase4-prioridad2`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`.

## Specs

**S1** — User request, verbatim: "vamos con la prioridad dos, te parece?" (after "ya habiamos terminado la prioridad 1, nos faltan la 2 y 3"). Scope is exactly section 4 of `Prioridades de arreglo.md`, items 2.1 to 2.9. No new features.

**S2** — 2.1: the offline QR approval replay must create the deterministic group on demand, increment `studentCount` and assign the next free roster number exactly like the online path.

**S3** — 2.2: changing the ungrouped-student search must clear the selection or restrict assignment to visible rows; hidden IDs must never be assigned.

**S4** — 2.3/2.4: the teacher's approved subject is read-only in the profile; `refresh()` must not erase the assigned groups.

**S5** — 2.5: task and notice forms opened from a group must receive that `groupId` and preselect it.

**S6** — 2.6: a teacher with groups from different grades must see the subject for each group, not `subjects.firstOrNull()`.

**S7** — 2.7: director's manual teacher assignment must write `subjectTeachers` and `users.subjects` consistently with the assigned group's grade.

**S8** — 2.8: Attendance, Tasks and Incidents must reconcile the persisted child selection via `ensureValid`.

**S9** — 2.9: the QR review screen must not claim "Correo verificado" when the payload does not carry that fact.

## Tasks

- **T1** — S2: shared group/roster canonicalisation for both approval paths. Route: inline. Evidence: pending.
- **T2** — S3: ungrouped search/selection. Route: inline. Evidence: `onQueryChange` clears selection; `UngroupedStudentsViewModelTest` GREEN.
- **T3** — S4: teacher profile subject and groups. Route: inline. Evidence: pending.
- **T4** — S5: groupId through task/notice navigation. Route: inline. Evidence: pending.
- **T5** — S6: per-group subject resolution. Route: inline. Evidence: pending.
- **T6** — S7: director manual assignment. Route: inline. Evidence: pending.
- **T7** — S8: parent tab selection reconciliation. Route: inline. Evidence: Attendance/Tasks/Incidents call `ensureValid` before mapping the id; existing VM tests GREEN.
- **T8** — S9: remove unverifiable QR claim. Route: inline. Evidence: `ReviewStudentScreen` says "Tutor del alumno" instead of "Correo verificado".
- **T9** — all: full verification, FASE4.md update, merge/push on approval. Route: inline. Evidence: pending.

## Log

**L1** — User request (verbatim): "Okey, entonces te parece si seguimos con lo plantenado en Prioridades de arreglo.md ya habiamos terminado la prioridad 1, nos faltan la 2 y 3, vamos con la prioridad dos, te parece?"

**L2** — Order chosen: 2.1 → 2.7 → 2.3 → 2.4 → 2.5 → 2.6 → 2.8 → 2.2 → 2.9, as suggested in `Prioridades de arreglo.md` section 6. Writes that can persist wrong data go first.
