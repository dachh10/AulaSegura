# Handoff — dónde nos quedamos y qué falta

Actualizado: 18 nov 2026. Este archivo existe para retomar el trabajo en otra superficie (CLI) sin releer todo el historial. Si contradice a `FASE1.md` o a los logbooks de `odd/tasks/`, gana el logbook del bloque.

## Estado del repo

- Rama de trabajo: `main` en `06e606a`, **alineada con `origin/main`** (divergencia `0 0`). La Fase 2 de Evelyn se integró con el merge `06e606a` y se subió con autorización explícita del dueño.
- Últimos commits: `06e606a` merge Fase 2 · `6e83f0b` `requireLocation` en el gate · `5db16ce` docs Fase 2 · `1a12dba` merge de la Fase 1 · `5abbb22` Fase 2 directiva.
- Trabajo sin commitear: nada, salvo `.atl/` (sin trackear, preexistente).
- Backup del `main` anterior al merge: `backup/main-pre-admin-merge` (`8322862`).

## Verificación obligatoria

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Estado tras integrar la Fase 2: **BUILD SUCCESSFUL · 379 tests · 0 fallos** (52 suites). Un bloque no se cierra con build rojo ni con tests en rojo. El dueño conectó su teléfono por USB con depuración, pero las pruebas en dispositivo siguen siendo suyas: el agente no opera ese dispositivo y hay que decirlo en el cierre.

Conteo de tests, si hace falta:

```bash
grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' app/build/test-results/testDebugUnitTest/TEST-*.xml | awk -F'"' '{t+=$2;s+=$4;f+=$6;e+=$8} END {print "TESTS="t" FAILURES="f" ERRORS="e}'
```

## Cerrado

**Fase 1 (bloques 0–11)** según `FASE1.md`: acceso y sesión, grupos y roster, pase de lista con geocerca, incidentes, tareas y avisos, historial, chat, notificaciones y FCM, perfil del docente.

**Correcciones posteriores** (logbooks en `odd/tasks/`):

- `bloque-13-detalle-grupo.md` — `teacher/groups/{groupId}` es una pantalla real con tres pestañas: Alumnos (roster con búsqueda), Asistencia (calendario del mes con el porcentaje real de cada día + últimas listas con chips P/F/R/J y sincronización) e Incidentes (tarjetas expandibles del grupo). El hero dice si la lista de hoy está pendiente, hecha o si hoy no hay clases. `IncidentSummary` ganó `groupId`.
- `bloque-14-mapa-real-incidente.md` — el registro de incidente usa `CampusMap` (Google Maps) con pin real: se siembra con el GPS y, si no hay, con el centro del plantel; tocar el mapa lo mueve y "Usar mi ubicación actual" usa el fix. Se guardan coordenadas reales en `IncidentLocation` (`accuracy` solo si el pin vino del GPS) y el chip dice el veredicto real: dentro, fuera con distancia o sin geocerca.
- `bloque-16-chips-historial.md` — chips de una línea (`maxLines`/`softWrap`), `FlowRow`, evidencia con icono, PDF de una tarea abre con `ACTION_VIEW` y su fila muestra chip `PDF` en vez del nombre.
- `bloque-12-inicio-real.md` — inicio con datos reales: siguiente clase desde `groups/{groupId}/schedule`, resumen de hoy (listas/grupos, faltas, outbox), actividad reciente (asistencia + incidentes + tareas + avisos), campanita con no leídas reales y badge oculto en 0.
- `bloque-15-radio-calendario.md` — pase de lista bloqueado sábado y domingo ("Hoy no hay clases"), mes real en el historial con flechas y filtro por mes en las tres pestañas.

## Lo que falta

| # | Bloque | Qué incluye | Dependencia |
|---|---|---|---|
| B17 | Perfil según Stitch | Convertir los diálogos de editar perfil y cambiar contraseña en pantallas con el diseño de Stitch. La lógica ya es real (nombre/materias en `users/{uid}`, contraseña con reautenticación). | **código de Stitch**: "Editar Perfil (Docente)", "Cambiar contraseña" |

La Fase 2 (directivo) de Evelyn ya está integrada en `main` (`06e606a`). Reglas de Firestore, índices y reglas de RTDB publicados con `firebase deploy`. Al tocar esos archivos, revisar que sigan pasando los flujos del docente (bandeja, `fcmTokens`, `attendanceSessions`).

## Decisiones que no hay que reabrir sin motivo

- **Fin de semana:** el pase de lista se rechaza antes de verificar ubicación; lo ya guardado no se toca. Sin feriados: no existe colección y crearla depende del directivo. `util/SchoolCalendar.kt` es el único lugar a tocar cuando exista.
- **Horario:** se lee en vivo, sin caché Room. Los documentos los escribe el directivo (que aún no existe). Las reglas autorizan `schedule` con `get()` del grupo padre, por eso se lee grupo por grupo y no con `collectionGroup`.
- **Clase en curso gana a la siguiente** en la card del inicio.
- **Aula 12 no existe** en el esquema: la card muestra materia · grupo y "N alumnos", nada de salón inventado.
- **Historial:** el mes se compara como `año * 12 + mes` (sin `java.time`: `minSdk` 24 sin desugaring); el filtro aplica a las tres pestañas porque el encabezado es uno.
- **Chips:** `softWrap = false` + `FlowRow`. Un chip que no cabe baja de línea entero; nunca se parte letra por letra. El "0 R" se conserva en gris a propósito.
- **PDF de tarea:** `attachmentUrl` solo existe cuando el archivo está en Cloudinary (`isUploaded`); si está en cola o falló, la fila no es tocable y dice "Pendiente" / "No se subió".
- **Mapa del incidente:** el pin es la coordenada que se guarda, no una decoración. Se siembra con el GPS, luego con `schools.location`, y si no hay ninguna de las dos **no se inventa**: la tarjeta explica y deja el botón de ubicación. Se mueve tocando el mapa (no arrastrando: el formulario tiene scroll y `scrollGesturesEnabled = false` dentro de la tarjeta). `accuracyMeters` solo viaja con lecturas del GPS.
- **Detalle de grupo:** una sola pantalla con tres pestañas, no tres rutas. Las métricas muestran `—` cuando no hay dato (sin lista hoy no hay faltas; sin listas no hay porcentaje) y el calendario solo pinta lunes a viernes. No se porta lo que el proyecto no tiene: salón ("Aula 12"), chat por alumno, porcentaje por alumno ni el resumen de AulaBot.
- **Notificaciones:** `notifications` es de solo lectura salvo `read`; el `create` es del Admin SDK. Un push de la consola **no** crea documento en la bandeja.
- **Push:** en Spark no hay Cloud Functions; el envío es manual con `scripts/notify` o `messaging.sendEachForMulticast`.
- **Saludo/idioma:** UI en español neutro/mexicano, código e identificadores en inglés.

## Datos para probar a mano

- `groups/{grp-3b|grp-3a|grp-2c|grp-1a}/schedule/slot-1` con `weekday` (1 = lunes … 7 = domingo), `startTime` `"08:00"`, `endTime` `"08:45"`, `subject`, `teacherId`. Sin esto el inicio muestra "Aún no hay horario programado".
- `schools/{schoolId}`: `location` con las coordenadas reales del lugar de prueba y `radiusMeters` entre 20 y 5000 (sugerido 500–1000 para probar). Solo lo edita un directivo; en la consola se puede. `evaluateGeofence` exige además `accuracy <= radius`.
- **Mapa del incidente:** necesita permiso de ubicación y la `MAPS_API_KEY` de `local.properties` (la misma del pase de lista). Sin GPS ni plantel cacheado no hay pin y el incidente se guarda con `location = null`, que el esquema acepta; con la ubicación apagada la tarjeta lo avisa y ofrece activarla.
- Docente demo: `docente@aulasegura.com` / `123456`. Proyecto Firestore: `aulasegu`.

## Trampas conocidas

- `java.time` no está disponible en runtime (`minSdk` 24, sin desugaring).
- `google-services.json` se ignora en `.gitignore` pero está versionado a propósito.
- `scripts/notify/service-account*.json` y las claves de Cloudinary no van al repo.
- No crear documentos en `notifications` desde el cliente: las reglas lo rechazan.
- El repo no tiene `AGENTS.md`; las convenciones viven en `FASE1.md`, `CONTEXT.md` y los logbooks.
