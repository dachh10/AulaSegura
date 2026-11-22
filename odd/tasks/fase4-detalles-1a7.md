# Fase 4 — Detalles pendientes 1 a 7

Branch: `fix/fase4-detalles-1a7`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`.

## Specs

**S1** — User request, verbatim: "listo ahora si ya arranca con los puntos 1 y 7" (alcance confirmado: puntos 1 a 7 de `Detalles pendientes.md`).

**S2** — Punto 1: el badge de foto del registro (tutor y docente) debe abrir Tomar foto / Elegir de la galería y conservar la imagen elegida hasta crear la cuenta.

**S3** — Punto 2: el perfil de la directora debe ofrecer cámara **y** galería, y su foto debe aparecer en el inicio junto a su nombre.

**S4** — Punto 3: la fila "Escuela" del perfil directivo no debe mostrar el ID crudo cuando no haya nombre configurado.

**S5** — Punto 4: revisar el flujo de cámara del tutor y hacer que funcione; su foto debe aparecer en "Hola, Nombre" del inicio.

**S6** — Puntos 5 y 6: Historial y Actividad reciente del docente solo deben mostrar lo que él creó en sus grupos (incidentes, listas y publicaciones).

**S7** — Punto 7: el listado de incidentes de la directora debe mostrar la primera imagen del incidente, no solo el icono de cámara.

## Tasks

- **T1** — S2,S3,S5: pickers de registro, directora y tutor; fotos en los tres inicios. Route: inline. Evidence: registration photo uploaded after account creation, director gallery + home avatar, tutor camera diagnostic + home avatar; build GREEN.
- **T2** — S4,S7: fallback de escuela y miniatura del incidente. Route: inline. Evidence: school row no longer prints the raw id; director incident tile uses incidentThumbnailUrl with AsyncImage; tests GREEN.
- **T3** — S6: filtro por `reportedBy` en Room y limpieza al cerrar sesión. Route: inline. Evidence: `IncidentDao.observeHistory(reporterUid)` con WHERE y repositorio que usa el uid de sesión; la limpieza de caché se descartó para no perder borradores pendientes de sincronizar. Build GREEN.
- **T4** — all: full verification, FASE4.md update, merge/push on approval. Route: inline. Evidence: pending.

## Log

**L1** — User request (verbatim): "listo ahora si ya arranca con los puntos 1 y 7".

**L2** — Hallazgos previos (ver `Detalles pendientes.md`): el badge del registro es un `clickable { }` vacío; la directora ya tiene cámara pero no galería y su inicio muestra "PR" fijo; el tutor tiene ambos launchers cableados y su inicio no recibe `photoUrl`; `IncidentDao.observeHistory()` no filtra por `reportedBy`; el tile de incidentes de la directora ignora `thumbnailUrl`.
