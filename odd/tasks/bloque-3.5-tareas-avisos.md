# Bloque 3.5 — Tareas y avisos reales, con marca de leído

Rama: `main` · entrega: commit en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantallas: `ParentTasksScreen` (Stitch `58f3dbb8b1f2486abcfd8efe5dde189b`), `ParentTaskDetailScreen` (`29ac02e4480a4828a0fe2ba1351e3014`), `ParentNoticesScreen` (`a60924077dd1436e895010e33504e0c1`)

## Specs

**S1 — La lista de tareas es la que publicó el docente.** `ParentTasksScreen` deja de usar `sampleTasks()`: lee `tasks.whereArrayContains("groupIds", groupId)` del hijo en pantalla, descarta `active == false` y agrupa por fecha de entrega (`VENCIDAS`, `HOY`, `MAÑANA`, `ESTA SEMANA`, `MÁS ADELANTE`). El pill del top bar cicla entre hijos como en Asistencia (3.3).

**S2 — El filtro no inventa estados.** `Pendientes` = lo que vence hoy o después; `Vencidas` = lo que ya pasó; `Todas` = todo. `Entregadas` queda **vacío a propósito**: `tasks/{id}` no tiene estado por alumno y `students` no registra entregas, así que la pantalla explica que la escuela todavía no las registra en vez de pintar una tarea "entregada" que nadie marcó.

**S3 — El detalle lee el documento real.** `ParentTaskDetailScreen` lee `tasks/{taskId}` por id (lectura de un documento: las reglas la evalúan contra el documento real, así que también funciona desde un deep link). Muestra materia, grupo, título, autor, fecha de publicación, fecha de entrega, instrucciones y adjuntos; cada adjunto abre su URL de Cloudinary con `ACTION_VIEW`. Se fueron el botón "Marcar como vista" y el marcador del top bar: no existe colección de leídos para tareas y un botón que sólo cambia de color sería lo único falso que quedaba en la pantalla.

**S4 — Los avisos son los del grupo del hijo, con leído real.** `ParentNoticesScreen` lee `notices.whereArrayContains("audience.groupIds", groupId)` por cada grupo distinto de los hijos, filtra en cliente los dirigidos a `tutor`, los vencidos (`expiresAt`) y los sin título, y une el estado leído de `users/{uid}/noticeReads`. Al abrir un aviso se despliega el cuerpo y se escribe `users/{uid}/noticeReads/{noticeId} = { readAt }` (idempotente por id). El marcado es optimista y **se revierte con mensaje** si la escritura falla. El aviso nunca se escribe: lo posee la escuela.

**S5 — Nada inventado en pantalla.** Sin `dueDate` → "Sin fecha registrada". Sin adjuntos → la sección no existe. El adjunto imprime el tipo MIME, no un tamaño que el schema no guarda. Sin `createdByName` → "Docente del grupo"; `category: "escuela"` → "Dirección". El resumen de no leídos dice "Estás al día" cuando no hay ninguno, en vez de repetir la hora del último aviso.

**S6 — Sin colecciones, índices ni migraciones nuevas.** Ni Room (`version = 4` sigue igual) ni `firestore.indexes.json`. La única escritura del tutor es `noticeReads`, que ya estaba en `CONTEXT.md` y en las reglas.

## Cambio de reglas necesario (publicar antes de probar)

La regla del tutor para `notices` pedía **dos** condiciones de arreglo (`audience.roles` y `audience.groupIds`). Firestore demuestra una consulta de lista sólo con sus propios filtros y permite **un solo `array-contains` por consulta**, así que ninguna consulta puede probar ambas: la bandeja del tutor habría devuelto `PERMISSION_DENIED` completa.

`docs/firestore.rules` cambia **sólo la rama del tutor**:

- Antes: `isTutor() && audience.roles.hasAny(['tutor']) && (groupIds.hasAny(myGroups()) || studentIds.hasAny(myStudents()))`.
- Ahora: `isTutor() && (groupIds.hasAny(myGroups()) || studentIds.hasAny(myStudents()))`.

Qué se conserva y qué se relaja:

- **Se conserva el alcance por grupo**, que es la frontera real: un tutor no puede leer avisos de otro grupo ni de otra escuela.
- **Se relaja el filtro de rol**: el cliente (`parentNoticeFrom`) sigue exigiendo que el aviso incluya `tutor` en `audience.roles`, y el único publicador actual (`NoticeSyncHandler` del docente) escribe `["tutor"]`. Riesgo residual: un aviso dirigido **sólo** a docentes del mismo grupo sería legible llamando la API a mano; hoy no existe ese publicador ni una bandeja docente.
- `studentIds` sigue en la regla; como los publicadores dejan ese arreglo vacío, un aviso dirigido sólo por alumno no aparece en la bandeja (limitación ya documentada, no se resuelve acá).

**Pendiente de humano: `firebase deploy --only firestore:rules --project aulasegu`.** Sin publicar, la bandeja de avisos del tutor falla en el teléfono; la lista y el detalle de tareas no dependen de este cambio.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S1, S3, S4 | Modelos `ParentTaskItem`, `ParentNoticeItem`, `ParentAttachment` | `domain/model/ParentPublications.kt` | ✅ |
| T2 | S1, S3, S4 | Métodos y resultados en `ParentRepository` (`loadTasks`, `loadTask`, `loadNotices`, `markNoticeRead`) | `domain/repository/ParentRepository.kt` | ✅ |
| T3 | S1-S5 | Lecturas en `FirestoreParentRepository` + mapeos puros `parentTaskFrom` / `parentNoticeFrom` / `parentAttachmentsFrom` | `data/repository/` | ✅ |
| T4 | Cambio de reglas | Rama del tutor en `notices` con el porqué escrito al lado | `docs/firestore.rules` | ✅ (código; deploy pendiente) |
| T5 | S1, S2 | Estados, secciones y etiquetas puras + `ParentTasksViewModel` | `ui/screens/parent/ParentTasksUi.kt`, `ParentTasksViewModel.kt` | ✅ |
| T6 | S3 | Estado del detalle + `ParentTaskDetailViewModel` | `ParentTaskDetailUi.kt`, `ParentTaskDetailViewModel.kt` | ✅ |
| T7 | S4, S5 | Estado de la bandeja + `ParentNoticesViewModel` (marca de leído) | `ParentNoticesUi.kt`, `ParentNoticesViewModel.kt` | ✅ |
| T8 | S1-S5 | Las tres pantallas con estado real, sin `sample*` | `ParentTasksScreen.kt`, `ParentTaskDetailScreen.kt`, `ParentNoticesScreen.kt` | ✅ |
| T9 | S1-S5 | Factory + wiring de navegación (detalle por `taskId`) | `di/AppContainer.kt`, `navigation/AulaSeguraNavHost.kt` | ✅ |

## Log

**L1 — 18 nov 2026, pedido del dueño (verbatim).** "Lee odd/tasks/handoff.md y FASE3.md. Continúa con el bloque 3.5 tareas y avisos reales), sin datos de ejemplo. Al cerrarlo, assembleDebug + testDebugUnitTest en verde, logbook en odd/tasks/ y commit; no hagas push sin pedirlo."

**L2 — 18 nov 2026, decisiones al leer el contrato:**

- **`noticeReads` y no un campo del aviso.** El aviso es de la escuela y el tutor no tiene `update` sobre `notices`; el "leído" es del tutor. `users/{uid}/noticeReads/{noticeId}` con `readAt` ya está en `CONTEXT.md` §6.2 y en las reglas, y el id del documento hace el marcado idempotente.
- **"Entregadas" vacío, no oculto.** El chip del diseño se queda y explica su vacío; esconderlo sugeriría que la entrega no aplica, y pintar entregas sería inventar un dato que ni `tasks` ni `students` tienen.
- **El detalle se lee por id.** Las reglas de lista son "todo o nada", pero una lectura de un documento se evalúa contra sus datos reales: por eso `parent/tasks/{taskId}` funciona aunque la tarea haya vencido o el tutor entrara desde un enlace.
- **La marca de leído no bloquea la lectura.** Si `noticeReads` no se puede leer, la bandeja muestra los avisos como no leídos en vez de fallar la pantalla: el estado del tutor no puede ocultar lo que publicó la escuela.
- **Sin adjuntos de aviso en la UI.** El docente hoy publica `attachments: []`; no se agrega una fila que nunca tendría datos. Cuando Fase 1 publique avisos con archivo, se reutiliza `ParentAttachmentUi` (ya existe en el detalle de tarea).

**L3 — 18 nov 2026, cierre del bloque.**

- **Sin tocar nada de Fase 1/2.** No cambian publicadores, `NoticeSyncHandler`, `TaskSyncHandler` ni Room; solo se leen las colecciones que esas fases ya escriben.
- **Tests (33 nuevos; 63 suites / 469 tests / 0 fallos):**
  - `ParentPublicationMappingTest` (8): campos del detalle, tarea sin título descartada, adjunto sin URL descartado, aviso leído, aviso de staff oculto, vencido por `expiresAt`, sin `audience` ilegible, `category: escuela` marcado como Dirección.
  - `ParentPublicationsUiTest` (12): hoy no es "vencida", ayer sí, mañana vs. esta semana vs. más adelante, `Pendientes`/`Vencidas`/`Todas`, `Entregadas` vacío, orden de secciones, PDF gana al nombre del docente, secciones de avisos por antigüedad (más nuevo primero), filtros Escuela/Mi grupo/Urgentes, "Último: …" vs. "Estás al día", fuente de respaldo, etiqueta de adjunto sin tamaño inventado.
  - `ParentTasksViewModelTest` (7): split pendiente/vencida, `Entregadas` vacío, filtrar no vuelve a leer, el pill cambia de hijo y recarga su grupo, un solo hijo no cicla, cuenta sin hijo vinculado, fallo con reintento que luego funciona.
  - `ParentNoticesViewModelTest` (6): badge según `read`, abrir marca una sola vez y baja el badge, marca rechazada se revierte y se explica, filtros, bandeja vacía, fallo de lectura es error (no bandeja vacía).
- **Verificación:** `:app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` → **BUILD SUCCESSFUL**, 63 suites / 469 tests / 0 fallos.
- **Dato de la línea base anterior:** 59 suites / 436 tests (cierre 3.4).

**Límites conocidos:**

- **Deploy de reglas pendiente** (arriba). Hasta publicarlo, sólo la bandeja de avisos del tutor falla.
- **Avisos sólo por `studentIds`** (sin `groupIds`) no se listan: la consulta va por grupo y ningún publicador escribe hoy esa forma.
- **"Entregadas" no existe como dato.** Si el equipo quiere ese chip con contenido, primero hay que definir en Fase 1/2 quién registra la entrega (contrato nuevo, no UI).
- **Sin paginación:** `tasks` se lee hasta 100 documentos y `notices` hasta 50 por grupo, igual que el resto del tutor (un ciclo escolar cabe).

**Pendiente de humano:**

1. `firebase deploy --only firestore:rules --project aulasegu`.
2. En el teléfono con las cuentas reales: el docente publica una tarea con PDF y un aviso urgente al grupo → el tutor abre Tareas, ve la tarea y el PDF abre en el navegador; abre Avisos, ve el aviso con badge, lo toca (se despliega), revisa en la consola `users/{tutorUid}/noticeReads/{noticeId}` con `readAt`, cierra y vuelve a entrar: el badge ya no lo cuenta.
3. Repetir la bandeja en modo avión: debe mostrar el error de lectura con "Reintentar", no una lista vacía.
