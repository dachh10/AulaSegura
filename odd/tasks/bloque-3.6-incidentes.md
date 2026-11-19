# Bloque 3.6 — Incidentes del hijo: lista, detalle y mapa reales

Rama: `main` · entrega: commit en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantallas: `ParentIncidentsScreen` (Stitch `2f4c…`), `ParentIncidentDetailScreen` (`2.7`), `ParentIncidentMapScreen` (`65e5f2a843054caa8acc844ef4d74681`)

## Specs

**S1 — La lista es la de la escuela, por hijo.** `ParentIncidentsScreen` deja de usar `sampleIncidents()`: `ParentRepository.loadIncidents()` lee `incidents.whereEqualTo("studentId", id)` por cada hijo vinculado (una consulta por hijo, hasta 100 documentos, `Source.SERVER`), descarta lo que no se puede mapear y ordena por `occurredAt` descendente. El pill del top bar cicla entre hijos como en Asistencia (3.3) y Tareas (3.5); la lectura se hace **una sola vez** y el cambio de hijo sólo filtra en memoria, así que no cuesta lecturas y un hermano no puede aparecer bajo el nombre del otro.

**S2 — Los contadores son del hijo en pantalla, no de la familia.** `Registrados` = los del hijo; `Nuevo` = los que no están en `seenByTutorIds` para el tutor que mira; `Graves` = `severity == "grave"`. Sin registros dice `Sin registros`, no `0`.

**S3 — El detalle lee el documento real por id.** `ParentIncidentDetailScreen` y `ParentIncidentMapScreen` comparten `ParentIncidentDetailViewModel`, que lee `incidents/{incidentId}` (lectura de un documento: las reglas la evalúan contra el documento real). Muestra severidad, folio derivado del id (`#INC-XXXXXXXX`), fecha y hora del reporte, coordenada con precisión y distancia cuando el docente las midió, quién lo registró, la descripción y la evidencia. Un incidente borrado o inaccesible da `NotFound`, no un error de red. No hay botón falso: se fue el `Más opciones` del detalle y del mapa.

**S4 — “Enterado” escribe `seenByTutorIds` y no es optimista.** El único campo que el tutor puede escribir es esa lista (`FieldValue.arrayUnion(uid)`), y las reglas sólo permiten **agregar**, nunca quitarse. Por eso el estado no cambia al tocar: cambia cuando la nube acepta. Mientras vuela, el botón dice `Guardando…` y está deshabilitado; si falla, se queda en `Enterado` con el botón activo y un mensaje de reintento; si ya está visto, el botón queda deshabilitado. La marca sobrevive reinstalaciones y es la misma para todos los dispositivos de la familia.

**S5 — El mapa dibuja la coordenada del reporte.** `CampusMap(teacherFix = null, campus = null, pin = LatLng(lat, lng))` con el pin de `incidents.location`; sin `campus`, el mapa centra el pin, y sin coordenada completa **no se dibuja mapa**: se muestra “Sin ubicación” en vez de inventar un centro. No hay geocerca ni radio pintado: la escuela ya validó la posición al reportar y el tutor no necesita recalcularla.

**S6 — Nada inventado en pantalla.** Sin `location` → “Sin ubicación”; sin `media` → “Sin evidencia adjunta” (la sección existe, no se esconde, porque el docente puede no haber adjuntado nada); sin `accuracy`/`distanceMeters` → esa línea no aparece; `severity` desconocida → chip neutro “Incidente”; `occurredAt` inválido → “Sin fecha”. La evidencia abre su URL con `ACTION_VIEW` (foto o video), sin reproductor propio ni miniaturas inventadas.

**S7 — Sin colecciones, índices, migraciones ni cambios de reglas.** Ni Room (`version = 4`) ni `docs/firestore.rules` ni `firestore.indexes.json`. `incidents` ya tenía la rama del tutor (lectura por `studentId` propio, update sólo de `seenByTutorIds`) y este bloque la usa tal cual.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S1-S6 | Modelos `ParentIncident`, `ParentIncidentLocation`, `ParentIncidentMedia` | `domain/model/ParentIncidents.kt` | ✅ |
| T2 | S1, S3, S4 | `loadIncidents`, `loadIncident`, `markIncidentSeen` + resultados | `domain/repository/ParentRepository.kt` | ✅ |
| T3 | S1-S6 | Lecturas por hijo, por id y `arrayUnion` + mapeos `parentIncidentFrom` / `parentIncidentLocationFrom` / `parentIncidentMediaFrom` | `data/repository/FirestoreParentRepository.kt` | ✅ |
| T4 | S1, S2, S6 | Estado de la lista, fila, severidad, fechas, ubicación, `incidentsOf`/`newIncidents`/`graveIncidents` | `ui/screens/parent/ParentIncidentsUi.kt` | ✅ |
| T5 | S3-S6 | Estado del detalle/mapa, `toDetailUi`, folio y etiquetas de media | `ui/screens/parent/ParentIncidentDetailUi.kt` | ✅ |
| T6 | S1, S2 | `ParentIncidentsViewModel` (hijos, filtro por hijo, ciclo del pill, reintento) | `ui/screens/parent/ParentIncidentsViewModel.kt` | ✅ |
| T7 | S3, S4 | `ParentIncidentDetailViewModel` (`load`, `refresh`, `acknowledge`) | `ui/screens/parent/ParentIncidentDetailViewModel.kt` | ✅ |
| T8 | S1-S6 | Las tres pantallas con estado real, sin `sample*` ni botones muertos | `ParentIncidentsScreen.kt`, `ParentIncidentDetailScreen.kt`, `ParentIncidentMapScreen.kt` | ✅ |
| T9 | S1, S3 | Factory de los dos ViewModels + rutas con `incidentId` | `di/AppContainer.kt`, `navigation/AulaSeguraNavHost.kt` | ✅ |

## Log

**L1 — 18 nov 2026, pedido del dueño (verbatim).** “Cierra el bloque 3.6: incidentes de los hijos con lista, detalle y mapa reales.”

**L2 — 18 nov 2026, decisiones al leer el contrato y las reglas:**

- **No se toca ninguna regla.** La rama del tutor en `incidents` ya permitía leer por `studentId` propio y actualizar sólo `seenByTutorIds` con `changedKeys().hasOnly([...])` y `hasAll` previo. El bloque se escribió contra ese contrato, no al revés. La verificación por REST (L3) confirma que la consulta pasa y que el alcance por alumno se respeta.
- **“Enterado” no es optimista.** En avisos la marca se puede revertir porque el tutor puede borrar su `noticeReads`; acá no: las reglas no permiten quitarse de `seenByTutorIds`. Pintar el visto antes de que la nube conteste dejaría en pantalla una marca que nunca podría deshacerse, así que el estado sólo cambia cuando la escritura vuelve bien.
- **Una lectura por hijo, filtro en cliente.** Se evaluó una sola consulta con `in`, pero las reglas demuestran la condición por igualdad sobre `studentId`; una consulta por hijo mantiene la misma forma que Asistencia y Tareas y evita el caso raro de `in` con lista vacía. Cambiar de hijo no vuelve a leer.
- **El mapa es el mismo incidente, no otro documento.** Detalle y mapa comparten ViewModel y estado: el mapa es el detalle con la coordenada dibujada. Sin coordenada, no hay mapa (nada de centro por defecto).
- **Ubicación como coordenada, no como lugar.** El schema guarda `location { lat, lng, accuracy, distanceMeters, verified }` y no un nombre de lugar, así que la UI no imprime “Patio central”: imprime la coordenada, la precisión y la distancia cuando existen.
- **La lista se repinta al volver.** No hay `lifecycle-runtime-compose` en el proyecto, así que el refresco al regresar del detalle es `LocalLifecycleOwner` + `LifecycleEventObserver` en `ON_START`, saltando el primer evento (que dispara la primera composición). Sin esto, “Visto por ti” quedaría viejo hasta reabrir la pantalla.
- **Se quitaron los botones muertos** `Más opciones` del detalle y del mapa: no hay acciones detrás y el dueño pidió no dejar nada falso.

**L3 — 18 nov 2026, cierre del bloque.**

- **Tests (27 nuevos; 67 suites / 496 tests / 0 fallos):**
  - `ParentIncidentMappingTest` (6): campos del documento, ubicación completa, ubicación sin mediciones, media de imagen/video, media sin URL descartada, `seenByMe` según el uid en `seenByTutorIds`.
  - `ParentIncidentsUiTest` (10): severidad desconocida como chip neutro, fechas corta/larga, sin fecha, ubicación y coordenada, detalle de precisión/distancia, contadores del hijo sin fuga del hermano, folio desde el id, miniatura de imagen que cae en su propia URL, duración de video.
  - `ParentIncidentsViewModelTest` (5): contadores y filas del hijo en pantalla, el pill cambia de hijo sin segunda lectura, lista vacía no es error, cuenta sin hijo vinculado, fallo con reintento que luego funciona.
  - `ParentIncidentDetailViewModelTest` (6): la coordenada real llega al mapa, “Enterado” escribe una vez y no reintenta cuando ya está visto, marca rechazada deja `seen = false` + error, incidente inexistente es `NotFound`, fallo de lectura con reintento, documento sin coordenada no tiene pin.
- **Dos correcciones durante la verificación, sólo en tests:** `ParentIncidentMappingTest` necesitaba el no-null assert antes de leer `location`, y una aserción de `ParentIncidentsUiTest` esperaba 2 incidentes del hermano cuando el fixture tiene 1. No cambió comportamiento de producción.
- **Verificación:** `:app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` → **BUILD SUCCESSFUL**, 67 suites / 496 tests / 0 fallos, `app-debug.apk` generado.
- **Verificación de reglas por REST con la sesión real del tutor** (sólo lecturas; sin escrituras de prueba en producción):
  - `users/{uid}` → `role: tutor`, `active: true`, `groupIds: [grp-3b, grp-1a]`, `studentIds: [stu-3b-06, stu-1a-07]`.
  - `incidents` con `where studentId == stu-3b-06` → **200**, 0 documentos; `stu-1a-07` → **200**, 0 documentos. Hoy no hay incidentes en los datos demo.
  - `incidents` con `studentId == stu-3b-01` (alumno ajeno) → **403**; consulta sin filtro → **403**. El alcance por alumno se respeta.
- **Dato de la línea base anterior:** 63 suites / 469 tests (cierre 3.5).

**Límites conocidos:**

- **La regla de lectura usa `users.studentIds`, no `students.tutorIds`.** Para este tutor coinciden porque el registro con código escribe `studentIds: [studentId]`. Si alguien vinculara a mano un alumno sólo en `students.tutorIds`, la lista de hijos lo mostraría y la lectura de incidentes daría `PERMISSION_DENIED`. Es una asimetría preexistente de las reglas; no se cambia en este bloque.
- **Sin paginación:** hasta 100 incidentes por hijo, igual que el resto del tutor (un ciclo escolar cabe).
- **Sin push en este bloque:** el push del incidente es 3.9; acá el tutor lo ve al entrar o al volver a la lista.
- **La evidencia se abre con `ACTION_VIEW`**, no hay reproductor ni galería propia: es lo mismo que ya hacía 3.5 con los adjuntos de tarea.
- **Hoy no hay incidentes en los datos demo**, así que la prueba en teléfono necesita que el docente reporte uno primero.

**Pendiente de humano:**

1. En el teléfono con las cuentas reales: el docente reporta un incidente con foto (idealmente con ubicación dentro de la geocerca) → el tutor abre Incidentes, ve el registro con `Nuevo` y `Graves` en 1 → entra al detalle, revisa fecha, coordenada y evidencia → toca **Enterado** → el botón queda inactivo y en la consola `incidents/{id}.seenByTutorIds` aparece su uid → vuelve a la lista: el incidente ya no cuenta como nuevo.
2. Repetir con un hijo distinto usando el pill: los contadores y la lista deben ser los de ese hijo, no los del anterior.
3. Probar el mapa con un incidente cuya `location` esté completa y otro sin `location`: el primero dibuja el pin, el segundo no dibuja mapa.
4. Repetir la lista y el detalle en modo avión: deben ofrecer “Reintentar”, no una lista o un detalle vacío.
