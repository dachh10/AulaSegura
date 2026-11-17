# Bloque 16 — Chips del historial

Branch `feat/docente-pase-lista` · tests `./gradlew.bat testDebugUnitTest` · build `./gradlew.bat assembleDebug`.

Registro oficial: pendiente de mover a `FASE1.md` cuando se cierren los B12–B17.

## Specs

**S1** — Usuario: `en historial [...] por ejemplo en incidentes si adjunto una foto sale como 1 foto pero raro y no buen diseño [...] lo mismo pasa en listas en presente falta o retardo, algunos de esos numeritos hacen que se vea raro ya que no tienen buen espacio.`

**S2** — Un chip nunca se parte: la etiqueta se dibuja en una sola línea y, si no cabe, el chip completo baja a la línea siguiente.

**S3** — La evidencia se cuenta con icono de cámara + número, no con la palabra "foto"/"fotos".

## Tasks

- T1 — S1,S2 — **hecho** — `StatChip` con `maxLines = 1`, `softWrap = false`, padding 7×3, 10.5 sp y sin `letterSpacing` negativo.
- T2 — S3 — **hecho** — `MediaChip(count)` con `Icons.Outlined.PhotoCamera` + número y `contentDescription` ("1 archivo adjunto" / "N archivos adjuntos").
- T3 — S2 — **hecho** — La fila de chips de `SessionCard` e `IncidentCard` pasa de `Row` a `FlowRow` (5 dp horizontal, 4 dp vertical): los chips fluyen como unidades.
- T4 — S1..S3 — **hecho** — Build + tests, este logbook y commit.

## Log

**L1** — Causa: `StatChip` era un `Box` sin ancho mínimo cuyo `Text` no tenía `maxLines` ni `softWrap`, así que "1 foto" se imprimía carácter por carácter cuando la fila (fecha + título + "Sincronizado" + chevron) se quedaba sin espacio. `letterSpacing = (-0.22).sp` con JetBrainsMono empeoraba la lectura de "28 P / 2 F / 0 R".

**L2** — Decisión: `FlowRow` en vez de encoger los chips o acortar `SyncPill`. Un chip que no cabe en 320 dp es un problema de distribución, no de tipografía; con `FlowRow` el peor caso es una segunda línea, nunca un pill ilegible.

**L3** — Decisión: el "0 R" se conserva en gris. Quitarlo haría que el número de chips cambiara entre sesiones y costaría comparar dos listas de un vistazo.

**L5** — 18 nov 2026, extensión pedida por el dueño: igual que la evidencia de un incidente se abre tocándola, el PDF de una tarea publicada debe abrirse desde el historial. `PublishedTask.attachmentUrl` y `HistoryPublicationUi.attachmentUrl` se llenan solo cuando el PDF ya está en Cloudinary (`isUploaded`), porque mientras está en la cola la copia vive en el teléfono y tras un fallo no queda nada que abrir. La fila es tocable exactamente en ese caso y muestra "Abrir" con `ACTION_VIEW` sobre la URL, el mismo patrón que usa `EvidenceRow`. Dos tests nuevos: el mapper expone la URL solo si subió, y `publicationRows` la propaga a la fila.

**L4** — Verificación: `assembleDebug` verde y `testDebugUnitTest` con 234 tests / 0 fallos / 0 errores (sin cambios respecto del bloque 11: este bloque es de layout y no agrega lógica testeable). No se probó en dispositivo; la comprobación visual queda para el dueño.
