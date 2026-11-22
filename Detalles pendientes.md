# Detalles pendientes — revisión de los tres roles (Fase 4)

Equipo: Diego y Evelyn · base: `main` @ `897f62b` · fecha: 22 de noviembre de 2026.

Documento de seguimiento de ocho detalles encontrados en dispositivo en los módulos directivo, docente y tutor. **Ninguno está implementado**: este archivo solo registra el análisis, el plan propuesto y los riesgos. Se complementa con `FASE4.md` y `Prioridades de arreglo.md`, que ya están cerrados.

---

## 1. Estado general

| # | Detalle | Rol | Estado | Complejidad |
|---|---|---|---|---|
| 1 |Botón de foto inerte en el registro | Tutor y docente | 🟡 PENDIENTE | Pequeña |
| 2 |Foto de perfil de la directora y foto en su inicio | Directiva | 🟡 PENDIENTE | Pequeña |
| 3 |"Escuela" del perfil con ID hardcodeado | Directiva | 🟡 PENDIENTE | Pequeña |
| 4 |"Tomar foto" del tutor no funciona y su inicio no muestra la foto | Tutor | 🟡 PENDIENTE | Pequeña/media |
| 5 |Historial muestra incidentes de otros docentes y de versión anterior | Docente | 🟡 PENDIENTE | Media |
| 6 |"Actividad reciente" muestra incidentes ajenos | Docente | 🟡 PENDIENTE | Media |
| 7 |Incidentes de la directora muestran icono en vez de la foto | Directiva | 🟡 PENDIENTE | Pequeña |
| 8 |Pase de lista independiente por materia y filtro por materia en el tutor | Docente y tutor | 🟡 PENDIENTE | **Grande** |

Orden sugerido: 1–7 primero (dos o tres unidades), y el 8 al final con su propio feature doc, migración de Room y commits separados.

---

## 2. Detalle 1 — Foto en el formulario de registro

**Evidencia:** `ui/screens/access/RegisterScreen.kt:456` tiene `.clickable { }` vacío en el badge de cámara de `AvatarPicker` (`:431-477`). `RegisterUiState` no tiene campo de foto (`RegisterViewModel.kt:20-41`), el ViewModel no tiene setter y el NavHost no pasa callback (`AulaSeguraNavHost.kt:285-309`).

**Plan:** agregar un dialog con **Tomar foto** y **Elegir de la galería** reutilizando el patrón de los perfiles (`TakePicture` + `GetContent` + FileProvider, ya declarados en `AndroidManifest.xml:11,29-37` y `file_paths.xml:4`). Guardar el `Uri` en el estado y subirlo después de crear la cuenta, cuando ya existe el `uid`.

**Riesgos:** Cloudinary sin configurar deja la foto sin subir; limpiar el archivo temporal; no hace falta permiso CAMERA adicional para `TakePicture`.

---

## 3. Detalle 2 — Foto de la directora

**Evidencia:** "Cambiar foto" **sí está cableado**: `AdminProfileScreen.kt:128-162` usa `TakePicture`, `:722-726` conecta `onClick = onChangePhoto`, y `AdminProfileViewModel.changePhoto` (`:242-311`) comprime, sube a Cloudinary y actualiza `users.photoUrl`. Lo que falta:
- No hay opción de **galería** (solo cámara).
- El inicio muestra iniciales "PR" fijas (`AdminDashboardScreen.kt:304-318`) y no recibe `photoUrl`.

**Plan:** agregar un `GetContent` launcher y una segunda fila en el dialog; pasar `photoUrl`/`fullName` desde `auth.currentUser` al ViewModel del panel y dibujar `AsyncImage` con fallback a iniciales, igual que `TeacherHomeScreen.kt:185-203`.

**Riesgos:** snapshot de sesión desactualizado; configuración de Cloudinary; autoridad del FileProvider.

---

## 4. Detalle 3 — "Escuela" del perfil de la directora

**Evidencia:** `ui/screens/admin/AdminProfileUi.kt:177` hace `schoolName.ifBlank { schoolId }`. El nombre se guarda en Configuración (`AdminCampusConfig.toUpdateMap`), pero el perfil lo lee con `get(Source.SERVER)`; cualquier fallo, documento sin nombre o modo offline produce cadena vacía y cae al ID crudo (`sch-benito-juarez`).

**Plan:** quitar el fallback al ID; mostrar "Plantel sin nombre" o vacío hasta que exista un nombre real. Refrescar el perfil después de guardar en Configuración y, si es posible, resolver el nombre desde caché en vez de solo servidor.

---

## 5. Detalle 4 — Foto del tutor: cámara y cabecera del inicio

**Evidencia:** "Tomar foto" y "Elegir de la galería" **están cableados** en `ParentProfileScreen.kt:196-222` (cámara con `TakePicture` + FileProvider, galería con `GetContent`). La ruta tutor usa `file://` mientras la directora usa `content://`; el compresor lee ambos. Si la cámara no hace nada, las causas probables son `TakePicture` con `saved = false` en esa app de cámara o `CloudinaryResult.NotConfigured` sin mensaje visible — hace falta un logcat para confirmar antes de cambiar contratos.

El inicio no muestra la foto: `ParentHomeScreen.kt:83` no recibe `photoUrl`, `HomeTopBar` dibuja iniciales (`:222-272`) y el NavHost solo pasa `fullName` (`:468-485`). El docente sí lo hace bien.

**Plan:** agregar diagnóstico de `saved=false`; pasar `photoUrl` al estado del inicio y reemplazar iniciales por `AsyncImage`.

---

## 6. Detalles 5 y 6 — Incidentes ajenos en Historial y Actividad reciente

**Evidencia:** el refresco de Firestore sí filtra por `reportedBy == uid` (`FirestoreIncidentHistoryRepository.kt:48-54`), pero la lista pintada sale de Room: `IncidentDao.observeHistory()` (`IncidentDao.kt:36-44`) es `SELECT * FROM incident_drafts` sin filtro. La entidad ya tiene `reportedBy` (`IncidentDraftEntity.kt:30`), simplemente no se usa. `AuthRepositoryImpl.signOut()` (`:440-443`) no limpia Room, así que sobreviven borradores de otra cuenta o versión anterior. Listas (`TaskDao.observeByCreator`) y publicaciones (`NoticeDao.observeByCreator`) sí filtran bien.

**Plan:** agregar `WHERE reportedBy = :uid` al DAO; limpiar drafts y media al cerrar sesión o cambiar de cuenta; aplicar el mismo filtro al merge de "Actividad reciente" (`TeacherHomeUi.kt:166-214`). No requiere migración.

---

## 7. Detalle 7 — Foto del incidente en el panel de la directora

**Evidencia:** `AdminIncident` ya trae `thumbnailUrl`/`url` (`AdminIncident.kt:13-20`, mapeado en `FirestoreAdminIncidentRepository.kt:91-101,183-188`), pero `AdminIncidentsScreen.kt:723-797` dibuja solo iconos; el comentario interno dice "no hay Coil en el proyecto", lo cual ya no es cierto. La pantalla del tutor sí usa `AsyncImage` (`ParentIncidentsUi.kt:126,136` y `ParentIncidentsScreen.kt:511-527`).

**Plan:** cambiar solo la UI: `AsyncImage` con `thumbnailUrl ?: url`, manteniendo icono y contador para videos o filas sin media.

---

## 8. Detalle 8 — Pase de lista independiente por materia (análisis completo)

### Cómo funciona hoy

- La sesión se identifica por un UUID generado en el cliente; guarda `teacherId` y `subject`, pero el subject se **adivina** del perfil (`subjectForGroup` en `RoomAttendanceRepository.kt:63`), no de `groups.subjectTeachers`.
- Las rutas llevan solo `groupId` (`Routes.kt:102-106`, NavHost `:793-799,824,843,869`); `StartRollCallUseCase.invoke(groupId)` y `RollCallViewModel` no reciben materia.
- Los horarios del grupo sí tienen `subject` y `teacherId` (`ClassSlot.kt:19-20`), pero la acción "Pasar lista" descarta la materia del slot.
- El tutor consulta `attendanceSessions.whereEqualTo("groupId", g)` (`FirestoreParentRepository.kt:139-142`) y **colapsa todas las sesiones del día en una sola marca** (`ParentAttendanceUi.kt:129-132`). No existe concepto de materia en `ParentAttendanceViewModel`.
- Las reglas (`docs/firestore.rules:372-420`) ya permiten **varias sesiones por grupo por día** y no exigen subject: el límite es del cliente.

### Plan ordenado

1. **Modelo:** agregar `subjectId` canónico (slug de `SchoolCatalog`) a `RollCallDraft`, `AttendanceSessionEntity` y el mapa de Firestore, con valor por defecto `""` y migración de Room. Índice único `(groupId, fecha, teacherId, subjectId)` para impedir duplicados.
2. **Propiedad real:** resolver el `subjectId` desde el horario o desde `groups.subjectTeachers`, y validar `subjectTeachers[subjectId] == uid` en vez de inferirlo del perfil. Pasarlo por ruta → NavHost → ViewModel → `StartRollCallUseCase` → `startDraft`.
3. **Docente:** en el detalle de grupo mostrar un botón por materia que ese docente imparte; la acción rápida del inicio usar la materia de la próxima clase.
4. **Tutor:** `loadMonth`/`loadHome` reciben `subjectId` opcional; filtrar sesiones en memoria (la query de Firestore se mantiene por grupo por reglas/índice); selector de materia en `ParentAttendanceViewModel`; `marksByDay` indexado por `(fecha, subjectId)`. "Todas" conserva el comportamiento actual.
5. **Reglas:** sin cambios funcionales; opcionalmente exigir `subjectId is string` al crear.
6. **Compatibilidad:** las sesiones antiguas tienen etiqueta pero no `subjectId`; derivarlo con `subjectIdFromLabel(label, grade)` al leer y agrupar las desconocidas como "Sin materia" **solo dentro de "Todas"**. Nunca reescribir documentos antiguos desde el cliente; si hace falta, proveer una reparación del lado directivo.

### Riesgos

Duplicación de sesiones, materias mal etiquetadas en datos viejos, validación de propiedad, volumen de lecturas, nueva migración de Room y pruebas en los tres roles.

---

## 9. Registro

**L1** — Solicitud (verbatim): "espera antes de seguir guarda eso en engram, tengo otro inconveniente que solucionar ahora. Si puedes crea un nuevo .md en main en el repo para esos 8 puntos indicados, los haremos despues de solucionar lo que te dire."

**L2** — Este documento es solo de seguimiento: **no se modificó código de la app** para ninguno de los ocho puntos.
