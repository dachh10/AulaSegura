# Fase 4 — Pase de lista por materia y filtro del tutor

Branch: `fix/fase4-pase-lista-materia`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `testDebugUnitTest assembleDebug`.

## Specs

**S1** — User request, verbatim: "Pues sigue sin funcionar pero la verdad ahorita me importa que quede ya funcionando el pase de lista por profesor, así que ya pasemos al punto 8." Continuation, verbatim: "Listo continua con el detalle 8".

**S2** — Punto 8, alcance (de `Detalles pendientes.md:82-103`): "Pase de lista independiente por materia y filtro por materia en la tutor". Máximo seis docentes por grupo, uno por materia; cada docente toma su lista de forma independiente; el tutor filtra la asistencia por materia.

**S3** — Modelo: agregar `subjectId` canónico (slug de `SchoolCatalog`) a `RollCallDraft`, `AttendanceSessionEntity` y al mapa de Firestore, con `""` por defecto; migración de Room 7→8 con `ALTER TABLE ... ADD COLUMN subjectId TEXT NOT NULL DEFAULT ''` e índice único `(groupId, fecha, teacherId, subjectId)`. No reescribir documentos antiguos de Firestore desde el cliente.

**S4** — Propiedad: resolver el `subjectId` desde el horario (`ClassSlot.subject` + `teacherId`) o desde `groups.subjectTeachers`, validando `subjectTeachers[subjectId] == uid` cuando esté disponible y respetando el máximo de seis docentes por grupo, uno por materia. Pasarlo por ruta → NavHost → `RollCallViewModel` → `StartRollCallUseCase` → `startDraft`. La inferencia del perfil (`subjectForGroup`) queda solo como compatibilidad.

**S5** — Docente: en el detalle de grupo mostrar un botón por materia que ese docente imparte; la acción rápida del inicio usa la materia de la próxima clase. Dos docentes del mismo grupo pueden abrir y cerrar su propia sesión el mismo día sin sobrescribirse.

**S6** — Tutor: `loadMonth`/`loadHome` filtran por `subjectId` opcional en memoria; selector de materia con **"Todas"** por defecto; `marksByDay` indexado por `(fecha, subjectId)`. Las sesiones antiguas sin `subjectId` se derivan con `subjectIdFromLabel(label, grade)` al leer; las materias desconocidas se agrupan como "Sin materia" **solo dentro de "Todas"**.

**S7** — Reglas de Firestore: sin cambios funcionales. Verificación con tests de mapper, caso de uso, UI de asistencia y build verde; `FASE4.md` actualizado. Merge y push solo con autorización explícita del usuario.

## Tasks

- **T1** — S3,S4: `subjectId` en modelo, entidad, DAO, migración 7→8, mappers de ida/vuelta, repositorio y caso de uso; validación de propiedad. Route: inline. Evidence: RED observado (tests no compilaban sin `subjectId`); `./gradlew testDebugUnitTest` GREEN, suite completa. Commit: pendiente.
- **T2** — S4,S5: propagación por rutas y GPS gate, botones por materia en el detalle de grupo, materia de la próxima clase en el inicio. Route: inline. Evidence: pendiente.
- **T3** — S6: filtro y selector del tutor, `marksByDay` por `(fecha, subjectId)` y compatibilidad con sesiones legacy. Route: inline. Evidence: pendiente.
- **T4** — S7: verificación completa, `FASE4.md`, merge/push solo con autorización. Route: inline. Evidence: pendiente.

## Log

**L1** — User request (verbatim): "Pues sigue sin funcionar pero la verdad ahorita me importa que quede ya funcionando el pase de lista por profesor, así que ya pasemos al punto 8."

**L2** — User request (verbatim): "Listo continua con el detalle 8".

**L3** — Análisis de partida: `Detalles pendientes.md:82-103`. Hoy la sesión se identifica solo por `groupId`, el `subject` se adivina del perfil (`subjectForGroup`), las rutas llevan solo `groupId`, el tutor colapsa todas las sesiones del día en una marca y las reglas ya permiten varias sesiones por grupo por día. El límite es del cliente.

**L4** — Mapa de integración (explorador de solo lectura): `SchoolCatalog.subjectIdFromLabel` y `subjectAvailability` ya existen; `ClassSlot` trae `subject` y `teacherId`; `Group` del docente no trae `subjectTeachers`, así que la validación de propiedad se apoya en el horario y, cuando haya dato disponible, en `subjectTeachers`. Seis rutas de pase de lista + GPS gate + factory propagan solo `groupId`. `findSession(groupId, date)` y `marksByDay` asumen una sesión por día. Room está en v7; `DatabaseProvider` registra las migraciones.

**L5** — Decisiones de diseño: (1) `subjectId`viaja en la ruta para que ninguna sesión se cree sin materia; (2) unicidad por `(groupId, fecha, teacherId, subjectId)` en Room, no en reglas; (3) los documentos antiguos se leen con derivación de etiqueta, nunca se reescriben; (4) "Todas" conserva el comportamiento actual del tutor; (5) sin writer delegado: las cuatro unidades son secuenciales y dependientes, el padre escribe inline siguiendo este logbook.

**L6** — T1 verificado: RED observado antes de la implementación (compilación de los nuevos tests sin `subjectId`); GREEN con `./gradlew testDebugUnitTest` sobre la suite completa. La migración 7→8 copia la etiqueta legacy a `subjectId` y desambigua duplicados con el `sessionId`, sin borrar filas ni tocar Firestore. `startDraft` valida que el id canónico exista y pertenezca al grado del grupo; la titularidad desde el horario se comprueba en T2, donde están los slots.
