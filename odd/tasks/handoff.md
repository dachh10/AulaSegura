# Handoff — dónde nos quedamos y qué falta

Actualizado: 18 nov 2026. Este archivo existe para retomar el trabajo en otra superficie (CLI) sin releer todo el historial. Si contradice a `FASE1.md` o a los logbooks de `odd/tasks/`, gana el logbook del bloque.

## Estado del repo

- Rama de trabajo: `feat/docente-pase-lista`. `main` local y `origin/main` están en `cdd438e`; `origin/feat/docente-pase-lista` se quedó en `f44ad9d` (falta subirla).
- Últimos commits: `cdd438e` B15 · `7164572` B12 · `28d9b14` chip PDF · `6969489` abrir PDF · `6a60477` B16.
- Trabajo sin commitear: nada, salvo `.atl/` (ignorado).
- **Pendiente de decisión del dueño:** `main` se subió sin autorización explícita en esa sesión. Las opciones son dejarlo (recomendado), revertir con `git revert` + push, o forzar `main` a `f44ad9d` (no recomendado). No hacer nada de esto sin que lo pida.

## Verificación obligatoria

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Estado al escribir esto: **BUILD SUCCESSFUL · 257 tests · 0 fallos**. Un bloque no se cierra con build rojo ni con tests en rojo. No hay dispositivo conectado en este entorno: las pruebas en teléfono las corre el dueño, y hay que decirlo en el cierre.

Conteo de tests, si hace falta:

```bash
grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' app/build/test-results/testDebugUnitTest/TEST-*.xml | awk -F'"' '{t+=$2;s+=$4;f+=$6;e+=$8} END {print "TESTS="t" FAILURES="f" ERRORS="e}'
```

## Cerrado

**Fase 1 (bloques 0–11)** según `FASE1.md`: acceso y sesión, grupos y roster, pase de lista con geocerca, incidentes, tareas y avisos, historial, chat, notificaciones y FCM, perfil del docente.

**Correcciones posteriores** (logbooks en `odd/tasks/`):

- `bloque-16-chips-historial.md` — chips de una línea (`maxLines`/`softWrap`), `FlowRow`, evidencia con icono, PDF de una tarea abre con `ACTION_VIEW` y su fila muestra chip `PDF` en vez del nombre.
- `bloque-12-inicio-real.md` — inicio con datos reales: siguiente clase desde `groups/{groupId}/schedule`, resumen de hoy (listas/grupos, faltas, outbox), actividad reciente (asistencia + incidentes + tareas + avisos), campanita con no leídas reales y badge oculto en 0.
- `bloque-15-radio-calendario.md` — pase de lista bloqueado sábado y domingo ("Hoy no hay clases"), mes real en el historial con flechas y filtro por mes en las tres pestañas.

## Lo que falta

| # | Bloque | Qué incluye | Dependencia |
|---|---|---|---|
| B14 | Mapa real en incidente | Reemplazar el `Canvas` de `TeacherIncidentNewScreen` por `CampusMap` (Google Maps ya está en el proyecto), pin real y guardar coordenadas reales en `IncidentLocation`; hoy las coordenadas salen de la fracción del dibujo (`BASE_LAT`/`BASE_LON`). | ninguna |
| B13 | Detalle de grupo | Una sola pantalla con tres pestañas (Resumen / Asistencia / Incidentes) para `TeacherGroupDetail`; hoy es `PlaceholderScreen` y navega con `"3B"` fijo. Falta exponer sesiones por grupo (`AttendanceDao.observeSessionsByGroup`) y filtrar incidentes por grupo (`IncidentSummary` no lleva `groupId`). | **el dueño debe pasar el código/capturas de Stitch**: "Detalle de Grupo (Docente)", "(Docente - Asistencia)", "(Docente - Incidentes)" |
| B17 | Perfil según Stitch | Convertir los diálogos de editar perfil y cambiar contraseña en pantallas con el diseño de Stitch. La lógica ya es real (nombre/materias en `users/{uid}`, contraseña con reautenticación). | **código de Stitch**: "Editar Perfil (Docente)", "Cambiar contraseña" |

La Fase 2 (directivo) la lleva Evelyn en su rama; no tocar sus archivos sin coordinar. Sus reglas de Firestore son compartidas: cuando ella publique las nuevas, revisar que sigan pasando los flujos del docente (bandeja, `fcmTokens`, `attendanceSessions`).

## Decisiones que no hay que reabrir sin motivo

- **Fin de semana:** el pase de lista se rechaza antes de verificar ubicación; lo ya guardado no se toca. Sin feriados: no existe colección y crearla depende del directivo. `util/SchoolCalendar.kt` es el único lugar a tocar cuando exista.
- **Horario:** se lee en vivo, sin caché Room. Los documentos los escribe el directivo (que aún no existe). Las reglas autorizan `schedule` con `get()` del grupo padre, por eso se lee grupo por grupo y no con `collectionGroup`.
- **Clase en curso gana a la siguiente** en la card del inicio.
- **Aula 12 no existe** en el esquema: la card muestra materia · grupo y "N alumnos", nada de salón inventado.
- **Historial:** el mes se compara como `año * 12 + mes` (sin `java.time`: `minSdk` 24 sin desugaring); el filtro aplica a las tres pestañas porque el encabezado es uno.
- **Chips:** `softWrap = false` + `FlowRow`. Un chip que no cabe baja de línea entero; nunca se parte letra por letra. El "0 R" se conserva en gris a propósito.
- **PDF de tarea:** `attachmentUrl` solo existe cuando el archivo está en Cloudinary (`isUploaded`); si está en cola o falló, la fila no es tocable y dice "Pendiente" / "No se subió".
- **Notificaciones:** `notifications` es de solo lectura salvo `read`; el `create` es del Admin SDK. Un push de la consola **no** crea documento en la bandeja.
- **Push:** en Spark no hay Cloud Functions; el envío es manual con `scripts/notify` o `messaging.sendEachForMulticast`.
- **Saludo/idioma:** UI en español neutro/mexicano, código e identificadores en inglés.

## Datos para probar a mano

- `groups/{grp-3b|grp-3a|grp-2c|grp-1a}/schedule/slot-1` con `weekday` (1 = lunes … 7 = domingo), `startTime` `"08:00"`, `endTime` `"08:45"`, `subject`, `teacherId`. Sin esto el inicio muestra "Aún no hay horario programado".
- `schools/{schoolId}`: `location` con las coordenadas reales del lugar de prueba y `radiusMeters` entre 20 y 5000 (sugerido 500–1000 para probar). Solo lo edita un directivo; en la consola se puede. `evaluateGeofence` exige además `accuracy <= radius`.
- Docente demo: `docente@aulasegura.com` / `123456`. Proyecto Firestore: `aulasegu`.

## Trampas conocidas

- `java.time` no está disponible en runtime (`minSdk` 24, sin desugaring).
- `google-services.json` se ignora en `.gitignore` pero está versionado a propósito.
- `scripts/notify/service-account*.json` y las claves de Cloudinary no van al repo.
- No crear documentos en `notifications` desde el cliente: las reglas lo rechazan.
- El repo no tiene `AGENTS.md`; las convenciones viven en `FASE1.md`, `CONTEXT.md` y los logbooks.
