# Bloque 3.2 + 3.3 — Resumen del tutor y asistencia real

Rama: `main` · estrategia de entrega: commit único local, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`

## Specs

**S1 — 3.2 Inicio con datos reales.** `ParentHomeScreen` deja de mostrar hijos y tareas inventados y muestra los hijos vinculados al tutor en `students.tutorIds`, la asistencia de hoy del hijo seleccionado, el porcentaje de asistencia del mes y las próximas tareas de su grupo, todo leído de Firestore.

**S2 — 3.3 Asistencia con calendario real.** `ParentAttendanceScreen` arma el calendario del mes desde `attendanceSessions` del grupo del hijo y `records/{studentId}`, con navegación de mes, conteos del mes, porcentaje y detalle de las últimas sesiones.

**S3 — Reutilizar lo existente.** Se reusan `AttendanceMark`, `AttendanceTimes`, `SchoolCalendar`, el patrón `TeacherHomeViewModel` + `TeacherHomeUi` + `AppViewModelFactory`, y `ProgressRing`/`StudentPill`/`Reveal` del bloque visual. No se duplica lógica de fechas ni de mapeo de estado.

**S4 — No inventar datos.** Si el docente no pasó lista, la pantalla dice "Sin registro" / deja el día sin marca; nunca pinta un número que no existe. Si el mes no tiene sesiones, el calendario queda vacío y el porcentaje es nulo.

**S5 — Sólo lecturas permitidas por las reglas publicadas.** `students.whereArrayContains("tutorIds", uid)`, `attendanceSessions.whereEqualTo("groupId", g)`, `records/{studentId}` por documento, `tasks.whereArrayContains("groupIds", g)`. Sin reglas nuevas y sin índice compuesto nuevo.

**S6 — Idioma.** UI en español; código, comentarios y commits en inglés.

**S7 — Verificación.** `assembleDebug` + `testDebugUnitTest` verdes antes de reportar; sin pruebas en dispositivo (no hay equipo conectado, las hace el dueño).

## Tasks

| ID | Tarea | Specs | Ruta | Estado |
|---|---|---|---|---|
| T1 | Modelos de dominio del tutor (`ParentChild`, `ParentMonthAttendance`, resultados) | S1,S2,S4 | inline | hecho |
| T2 | `ParentRepository` + `FirestoreParentRepository` con las cuatro lecturas | S1,S2,S5 | inline | hecho |
| T3 | `ParentHomeUi` + `ParentAttendanceUi` (mappers puros y calendario) | S1,S2,S3,S4 | inline | hecho |
| T4 | `ParentHomeViewModel` y `ParentAttendanceViewModel` | S1,S2,S4 | inline | hecho |
| T5 | Pantallas conectadas al estado real | S1,S2 | inline | hecho |
| T6 | Wiring en `AppContainer` y `AulaSeguraNavHost` | S3 | inline | hecho |
| T7 | Tests de mappers y calendario | S3,S4 | inline | hecho |
| T8 | `assembleDebug` + `testDebugUnitTest`, registro y commit | S7 | inline | hecho |

## Log

**L1** — 19 nov 2026, pedido del dueño (verbatim): "Listo ahora si ya funciono de manera correcta ya me inicio sesion con las pantallas de padre, ahora si sigamos con el punto 3.2, lo que podamos reutilizar de los codigos ya hechos que tengan similutud con estos 3.N que vayamos usando hazlo para ahorrar el vovler a elaborar todo, ahora si continua con 3.2". Además, confirmado antes: "Ya no hay device conectado tu ya no realizaras las pruebas para ahorrar tokens, ahora si ya esta todo bien dime si yo lo pruebo y ya funcionara, ya las pruebas las hare yo, solo dejame los pasos."

**L2** — 19 nov 2026, decisión de alcance: el bloque de `FASE3.md` §6 es "3.2 + 3.3" y comparten el mismo repositorio y los mismos modelos de asistencia, así que se entregan juntos. Separarlos habría duplicado las lecturas de `attendanceSessions`.

**L3** — 19 nov 2026, descubrimiento: `AppUser` (perfil cacheado) no lleva `studentIds` ni `groupIds`, y agregarlos obligaría a migrar Room (v4 → v5). Se evita: el repositorio del tutor resuelve los hijos con `students.whereArrayContains("tutorIds", uid)`, que además es exactamente lo que la regla de lectura puede probar. Cero cambios de esquema.

**L4** — 19 nov 2026, decisión de consulta: `attendanceSessions` se lee con `whereEqualTo("groupId", g)` y se filtra el mes en cliente. Un rango por `date` junto al filtro de grupo exigiría un índice compuesto nuevo, y no existe `firestore.indexes.json` en el repo para declararlo sin arriesgar los índices que ya viven sólo en la consola. Con un grupo y un ciclo escolar el costo es acotado y queda documentado.

**L5** — 19 nov 2026, porcentaje: `presentes + retardos` sobre `presentes + retardos + faltas + justificadas`. Una falta justificada sigue siendo inasistencia; se muestra aparte en el calendario y en el detalle.

**L6** — 19 nov 2026, resultado: 4 lecturas del tutor (hijos, grupos, sesiones del mes, registro del hijo por sesión) y 1 de tareas. `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`: **56 suites / 410 tests / 0 fallos**, build verde. +2 suites y +21 tests sobre la línea base de 3.1 (54/389).

**L7** — 19 nov 2026, decisiones de UI que se apartan del mock de Stitch a propósito, porque el mock muestra datos que la escuela no produce:
- `ParentAttendanceScreen` pierde `Objetivo mensual: 95%` (no existe esa política en el dominio) y en su lugar muestra `${counts.total} registros`, que sí es un dato real.
- El calendario ya no pinta días fijos: `monthWeeks` arma las filas lunes-viernes desde el mes real, con celdas nulas para alinear el primer día hábil. Sábado y domingo no son celdas.
- El día de hoy lleva el anillo; su relleno es la marca real. Si no hubo pase de lista, se ve gris ("sin registro"), nunca verde.
- El botón "Justificar" aparece solo en la ausencia más reciente del detalle: un botón sin decir a qué clase apunta sería peor que ninguno. La pantalla de justificación real llega en 3.4.
- Los mappers de `RecentRecord` quedaron puros (`ParentAttendanceRecordUi`) y testeados, sin imports de Firebase, para poder probarlos en JVM.

**L9** — 19 nov 2026, bug encontrado por el dueño al probar con `diego.prueba@gmail.com` (código `Y8S9EX`): el Inicio y la Asistencia decían "Aún no hay hijos vinculados". Verificado contra Firestore con el Admin SDK: la vinculación estaba perfecta (`students/{id}.tutorIds` tenía el uid, `users/{uid}.studentIds` y `groupIds` correctos, el código quedó `usedBy`), pero el alumno tenía **`active: false`**. `FirestoreParentRepository.loadChildren` descartaba `active == false`, así que el tutor recibía una lista vacía y la UI concluía "sin vínculo". Arreglo: el mapeo se extrajo a `parentChildFrom(id, data)` y ya **no lee `active`** — el vínculo en `tutorIds` es lo que hace que sea su hijo; `active` sigue significando "inscripto hoy" para la lista del docente. Regresión cubierta por `ParentChildMappingTest` (5 tests).

**L10** — 19 nov 2026, estado de los datos de prueba al cerrar: el grupo 4° D (`IqywT5pkCuXHIYz5vRRv`) tiene **0 `attendanceSessions` y 0 `tasks`**, y de sus 7 alumnos solo Diego Corral Hernández está vinculado a un tutor. Aun con el arreglo, el tutor va a ver la tarjeta del hijo con "Sin pase de lista hoy", porcentaje `—` y "Sin tareas próximas". Para ver marcas reales hace falta que el docente pase lista (y el alumno esté activo, porque el roster del docente filtra `active == true`) y publique una tarea.

**L11** — 19 nov 2026, verificación tras el arreglo: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` → **57 suites / 415 tests / 0 fallos**, build verde.
