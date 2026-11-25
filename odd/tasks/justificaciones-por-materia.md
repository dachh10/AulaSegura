# Justificaciones dirigidas por materia
Branch: main · Delivery: direct main (user-authorized commits) · Test runner: `./gradlew testDebugUnitTest --no-daemon --no-configuration-cache` · Risk: item 2 (Firestore read rules narrow teacher access).

## Specs

S1. "ese justificante de falta le tendria que aparecer unicamente al profesor el cual marco en su materia como falta al alumno, no a todos los profesores de ese grupo les tiene que llegar la justificacion"

S2. "en caso de que dos profesores le marcaran falta en sus respectivas materias de su grupo, ahi podria ser que si son dos o mas materias donde se le marcado falta tuviera la opcion de "Usar este justificante para todas las materias" y ahora si se envia a los profesores que se le marcado como falta"

S3. "Un documento por materia (recomendado): cada profesor recibe el suyo y aprueba solo su falta; el tutor ve una entrada por materia. Encaja con la aprobación actual y con reglas simples."

S4. "Dale implemetalo"

S5. Non-destructive constraint from the standing project requirements: preserve existing data and audit history; prefer repairs over deleting accounts or documents. Legacy justifications without `teacherId` keep their current read path instead of being hidden.

## Tasks

- T1 (S1,S3,S5) — domain + payload: add a justification target (session, subject, teacher), carry `teacherId` on `ParentDayAttendance`, write `teacherId`/`subjectId`/`subject` to each `justifications/{id}` document — inline — done
- T2 (S2,S3) — parent form: list every absence of the selected date with its subject, select one or all, submit one draft per selected absence — inline — done
- T3 (S1,S3,S5) — teacher inbox: query by `teacherId`, show the subject on the card, tighten the read rule with legacy compatibility — inline — done
- T4 (S1-S5) — JVM tests, full suite, signed release rebuild, delivery guide — inline — done (129 suites / 798 tests / 0 failures; APK SHA-256 `BB9ED6C9DFE25D7AF0AE2720EA07E867048246501C3F49ED98CA06CDED77B57F`)

Verification: 5 JVM tests added; full suite green. Independent read-only verifier found three blockers (legacy query provability, create-rule `get()` on an unsynced session, denied query masked as empty), all corrected before closing. Native review assess escalated to high on item 2; the user chose "Omitir esta vez" and the exact decline invocation was recorded. Firestore rules and the new composite index were deployed to `aulasegu` on 2026-11-25 and committed in `4865cd0`.

## Log

L1 (2026-11-25): "Ahora tengo un tema con Justificaciones, no em habia dado cuenta, que por ejemplo un alumno esta en el grupo 5°C ese alumno puestiene asignados a distintos profesores para cada una de sus seis materias, cuando el docente pasa lista, alguno de los seis le puede poner falta a ese alumno, entonces aqui el tutor tiene la opcion de enviar justificante, y si lo envia de manera correcta, pero aqui viene el detalle, ese justificante de falta le tendria que aparecer unicamente al profesor el cual marco en su materia como falta al alumno, no a todos los profesores de ese grupo les tiene que llegar la justificacion, y eso es lo que hace la app actualmente, si envias una justificacion le aparece a todos los profesores de ese grupo, no solo al que le puso falta y en caso de que dos profesores le marcaran falta en sus respectivas materias de su grupo, ahi podria ser que si son dos o mas materias donde se le marcado falta tuviera la opcion de "Usar este justificante para todas las materias" y ahora si se envia a los profesores que se le marcado como falta, primero realiza el analisis de esto."

L2 (2026-11-25): User selected "Un documento por materia (recomendado)" and authorized implementation: "Dale implemetalo". Analysis established that the read rule and the group-scoped query are the cause, that approval already flips exactly one session, and that the one-roll-call-per-slot guard makes the recipient teacher unambiguous.

L3 (2026-11-25): Decision taken without a data migration — legacy documents without `teacherId` keep the existing group-scoped read fallback so no stored justification is hidden or rewritten. New documents are addressed to one teacher and read only by that teacher.

L4 (2026-11-25): Implementation finished. The independent verifier found that the legacy group query was not provable against the new read rule, that the create rule's `get()` could drop a report whose session was still queued, and that a denied targeted query was reported as an empty inbox; all three were corrected with a `teacherId == ""` filter plus a composite index, an `exists()` guard before `get()`, and an early failure on targeted-query denial. The tutor's tapped session now travels in the route so the right subject is preselected, and unchecking one subject under "all subjects" keeps the others selected. The user declined the candidate-scoped native review. Rules and indexes in `docs/` were deployed to `aulasegu` with Firebase CLI (compile and deploy confirmed) and committed in `4865cd0`.
