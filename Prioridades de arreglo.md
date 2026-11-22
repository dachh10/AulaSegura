# Prioridades de arreglo — auditoría de módulos (Fase 4)

Equipo: Diego y Evelyn · base: `main` @ `8962f6e` · fecha: 22 de noviembre de 2026.

Este documento complementa `FASE4.md`. No redefine ningún bloque de la fase: ordena los detalles que quedaron después de verificar el flujo completo en dispositivo. Se armó con una auditoría de solo lectura de los tres módulos (directivo, docente y tutor) y los hallazgos más visibles se contrastaron con `file:line` antes de clasificarlos.

**Convención de estado:** ✅ **COMPLETA** = corregida, verificada y pusheada a `main`. 🟡 **PENDIENTE** = detectada y documentada, sin implementar. El orden de Ejecución es 1 → 2 → 3.

---

## 1. Cómo se originó

Después de cerrar la asignación de grupo y materia por grado (`4f1a47b`) y dejar las reglas desplegadas en `aulasegu`, se revisaron los tres módulos buscando **detalles pequeños**, no cambios de arquitectura:

- **Directivo:** panel, alta de alumno, revisión/escaneo de QR, aprobación online y offline, alumnos sin grupo, solicitudes de docentes.
- **Docente:** perfil, grupos, detalle de grupo, tareas, avisos, asistencia y navegación.
- **Tutor:** inicio, asistencia, tareas, avisos, incidentes, selección compartida de hijo y flujo "Agregar otro hijo".

Criterio usado: solo se cuenta como problema algo que el usuario **ve o pierde** — un número falso, un texto fijo, una acción que no hace nada, un dato que se descarta o una escritura incoherente. Lo que es solo deuda técnica se dejó fuera.

---

## 2. Resumen de estado

| Prioridad | Alcance | Estado | Evidencia |
|---|---|---|---|
| **1** | Datos falsos visibles en los tres módulos | ✅ **COMPLETA** | commit `170cddd`, merge `8962f6e` en `main`; 104 suites / 665 tests / 0 fallos + `assembleDebug` |
| **2** | Consistencia de datos y flujos que pierden información | 🟡 **PENDIENTE** | 9 hallazgos documentados en la sección 4 |
| **3** | Detalles de texto, estados vacíos y controles inertes | 🟡 **PENDIENTE** | 11 hallazgos documentados en la sección 5 |

---

## 3. Prioridad 1 — Datos falsos visibles (✅ COMPLETA)

### 3.1 Directivo: contador fijo de "Alumnos sin grupo"

- **Síntoma:** el panel mostraba siempre `0` y el subtítulo "Sin asignar", aunque la pantalla de asignación tuviera alumnos.
- **Evidencia:** `ui/screens/admin/AdminDashboardScreen.kt:175-177` (`count = 0`, `subtitle = "Sin asignar"`).
- **Corrección aplicada:** `AdminDashboardUiState.ungroupedStudents`, helper puro `ungroupedStudentCount()` en `AdminDashboardUi.kt`, `AdminDashboardViewModel.refreshUngroupedStudents()` leyendo `AdminStudentRepository.getStudents()`, y la card usa el número real.
- **Verificación:** `DashboardCountTest` (2 tests) + build verde.

### 3.2 Directivo: secciones E y F fuera del catálogo

- **Síntoma:** la revisión del QR ofrecía A–F; elegir E/F guardaba `grp-{grado}e/f`, un grupo que no existe y que el alumno nunca ve como aula real.
- **Evidencia:** `ui/screens/admin/ReviewStudentScreen.kt:47` (`listOf("A",...,"F")`); catálogo real en `domain/model/SchoolCatalog.kt:10`.
- **Corrección aplicada:** la lista de chips ahora sale de `AvailableSections` (A–D), igual que en `UngroupedStudentsUi`.

### 3.3 Tutor: número fijo en las cards del inicio

- **Síntoma:** "Avisos" decía siempre `2 nuevos` e "Incidentes" decía `Sin pendientes`, con el inbox vacío o con reportes nuevos.
- **Evidencia:** `ui/screens/parent/ParentHomeScreen.kt:565,573`.
- **Corrección aplicada:** Avisos usa `unreadNotifications` real (`$n nuevos` / "Sin nuevos") e Incidentes usa texto neutro "Ver reportes", sin afirmar un dato que hoy no se mide.

### 3.4 Tutor: el chip del hijo mostraba el ID del grupo

- **Síntoma:** si fallaba la lectura de `groups/{id}.name`, el chip mostraba `grp-3b` en vez de "3° B".
- **Evidencia:** `data/repository/FirestoreParentRepository.kt:116-120,479-488`.
- **Corrección aplicada:** helper puro `displayGroupLabel()` en `SchoolCatalog.kt`: nombre guardado → nombre canónico derivado del ID → ID como último recurso. No se oculta al alumno por un nombre faltante.

### 3.5 Docente: vista previa de aviso con persona y grupo falsos

- **Síntoma:** la vista previa mostraba "Prof. Salinas · 3° B" y "Primaria Matutina" aunque el ViewModel ya recibía el nombre real y los grupos elegidos.
- **Evidencia:** `ui/screens/teacher/TeacherNoticeNewScreen.kt:679,687`; datos disponibles en `:265-266` y `:620-621`.
- **Corrección aplicada:** se renderizan `teacherName` y `audienceLabel`, con fallback "Docente" / "Sin grupos seleccionados".

### 3.6 Cierre de la prioridad

- Commit: `170cddd` — `fix: remove fake counts and hardcoded labels in all three modules`.
- Merge en `main`: `8962f6e` — `merge: remove fake counts and hardcoded labels`.
- Verificación: **104 suites / 665 tests / 0 fallos** + `assembleDebug` verde.
- `FASE4.md` actualizado con el registro.

---

## 4. Prioridad 2 — Consistencia de datos y flujos que pierden información (🟡 PENDIENTE)

Son problemas que no rompen la pantalla pero sí pueden dejar un dato mal guardado o una vista incoherente. Conviene hacerlos antes que los de texto.

| # | Módulo | Hallazgo | Evidencia | Corrección mínima propuesta |
|---|---|---|---|---|
| 2.1 | Directivo | La aprobación **offline** no crea el grupo ni asigna número de lista; el alumno queda con `listNumber = 0` y `studentCount` sin incrementar | `sync/StudentApprovalSyncHandler.kt:33-64` vs `data/repository/FirestoreAdminStudentRepository.kt:174-208`; `domain/repository/AdminStudentRepository.kt:127` | Extraer la canonicalización de grado/sección y el `nextListNumber` a un helper compartido y usarlo en el replay |
| 2.2 | Directivo | Buscar en "Alumnos sin grupo" no limpia la selección; el botón asigna también los IDs que quedaron ocultos por el filtro | `ui/screens/admin/UngroupedStudentsViewModel.kt:63`, `UngroupedStudentsScreen.kt:145-174`, `assign()` en `:100-105` | Limpiar `selected` al cambiar el query, o restringir `assign()` a los IDs visibles |
| 2.3 | Docente | El perfil permite editar como texto libre la materia aprobada por el director; ese valor luego gobierna cards, tareas y asistencia | `ui/screens/teacher/TeacherProfileViewModel.kt:101-119`, `data/mapper/ProfileUpdateMapper.kt:16`, `docs/firestore.rules:141-145` | Hacer `subjects` de solo lectura en el perfil; mantener nombre/avatar/contraseña editables |
| 2.4 | Docente | `refresh()` del perfil reemplaza el estado y borra los chips de grupos asignados, que solo vienen del collector | `TeacherProfileViewModel.kt:203-205`, `TeacherProfileUi.kt:44-50`, `AulaSeguraNavHost.kt:1118`, `TeacherProfileSections.kt:251-257` | Conservar los grupos: `it.toProfileUi().copy(groups = _ui.value.groups)` |
| 2.5 | Docente | Desde el detalle de un grupo, "Nueva tarea"/"Nuevo aviso" no reciben el `groupId`; hay que volver a elegirlo y se puede elegir el equivocado | `AulaSeguraNavHost.kt:842-843`, `Routes.kt:59-60` (rutas sin argumentos), `TeacherTaskNewViewModel.kt:48`, `TeacherNoticeNewViewModel.kt:35` | Argumento opcional `groupId` en ambas rutas y preselección en los ViewModels |
| 2.6 | Docente | El subject se trata como un único valor global (`subjects.firstOrNull()`); con dos grupos de grados distintos se muestra la materia del primero en todas partes | `TeacherGroupsViewModel.kt:37,48`, `TeacherGroupDetailViewModel.kt:63,133`, `RoomAttendanceRepository.kt:62` | Resolver la materia del grupo concreto desde `groups.subjectTeachers` / el grado del grupo |
| 2.7 | Directivo | La asignación manual de un docente a un grupo escribe `teacherIds` y `users.groupIds`, pero no `subjectTeachers` ni `users.subjects`; el docente ve el grupo con la materia de otro grado | `data/repository/FirestoreAdminGroupRepository.kt:237-249` | Escribir el subject id y su etiqueta en el mismo batch, validando que corresponda al grado del grupo |
| 2.8 | Tutor | Solo Inicio y Avisos llaman a `ensureValid()`; Asistencia, Tareas e Incidentes usan el ID persistido sin reconciliarlo con el roster actual | `ParentAttendanceViewModel.kt:82`, `ParentTasksViewModel.kt:71`, `ParentIncidentsViewModel.kt:63` vs `ParentHomeViewModel.kt:101`, `ParentNoticesViewModel.kt:88` | Llamar a `selection.ensureValid(currentUid(), ids)` en el `loadChildren()` de esos tres ViewModels |
| 2.9 | Directivo | La revisión del QR afirma "Correo verificado" para todo tutor, pero el payload solo trae `v`/`uid`/`alumno` | `ui/screens/admin/ReviewStudentScreen.kt:99-104`, `domain/repository/ParentLinkQr.kt:14-17` | Quitar la afirmación o derivarla del perfil real del tutor |

**Criterio de cierre de la prioridad 2:** cada ítem queda con su test correspondiente, el build en verde y una comprobación en dispositivo si el cambio afecta una escritura (2.1, 2.2, 2.3, 2.7).

---

## 5. Prioridad 3 — Detalles de texto, estados vacíos y controles inertes (🟡 PENDIENTE)

| # | Módulo | Hallazgo | Evidencia | Corrección mínima propuesta |
|---|---|---|---|---|
| 3.1 | Directivo | "Subir foto del alumno" y los botones "Copiar"/"Compartir" del código de vinculación son `clickable { }` sin acción | `ui/screens/admin/AdminNewStudentScreen.kt:329,511-512,548` | Conectar portapapeles y `ACTION_SEND`, o deshabilitarlos hasta que existan |
| 3.2 | Directivo | Estado vacío de usuarios siempre dice `Sin resultados para ""` cuando el buscador está vacío | `ui/screens/admin/AdminUsersScreen.kt:319-325` | Ramificar el mensaje para consulta vacía / sin usuarios |
| 3.3 | Directivo | "Aceptar todas" no da feedback si no hay solicitudes pendientes | `ui/screens/admin/TeacherRequestsViewModel.kt:80-82` | Deshabilitar la acción o mostrar "No hay solicitudes pendientes" |
| 3.4 | Docente | Los chips de filtro de "Materia" toman nombre y predicado solo del primer grupo, ocultando los demás | `ui/screens/teacher/TeacherGroupsScreen.kt:112-119` | Un chip por materia distinta y filtrar por el valor elegido |
| 3.5 | Docente | Quedan etiquetas viejas "General" y defaults de roll call `"3B"` que no coinciden con `grp-{grado}{letra}` | `TeacherTaskNewViewModel.kt:49,60,311`, `TeacherProfileScreen.kt:593`, `AulaSeguraNavHost.kt:867,892,931,944,961`, `TeacherLocationVerifyingScreen.kt:87` | "Sin materia asignada" y quitar/reemplazar los defaults `"3B"` |
| 3.6 | Tutor | El inicio sin hijos dice "regístrate de nuevo", pero el flujo actual es escanear el QR de la directora | `ui/screens/parent/ParentHomeScreen.kt:177-180` | Apuntar al QR / "+ Agregar hijo" |
| 3.7 | Tutor | El mismo estado sin porcentaje usa dos textos: "Sin datos aún" en el inicio y "—" en asistencia | `ParentHomeLabels.kt:12`, `ParentAttendanceScreen.kt:336` | Unificar en "Sin datos aún" |
| 3.8 | Tutor | Avisos filtra por el hijo seleccionado pero no dice de qué hijo es, a diferencia del resto de pestañas | `ParentNoticesViewModel.kt:88-94`, `ParentNoticesScreen.kt:74-79,107-140` | Mostrar el nombre/grupo del hijo seleccionado en el encabezado |
| 3.9 | Tutor | En modo "agregar hijo" conviven dos listeners de `observeMyChildren()`: el del `init` y el del watcher del QR | `ui/screens/access/AccessStateScreens.kt:114-118,176` | Capturar los IDs conocidos una sola vez al generar el QR y no mantener el listener global en ese modo |
| 3.10 | Tutor | Avisos carga dos veces el roster: `loadNotices()` ya lo hace internamente y el ViewModel lo vuelve a pedir para obtener el grupo | `ParentNoticesViewModel.kt:81-89`, `data/repository/FirestoreParentRepository.kt:307` | Incluir el grupo resuelto en `ParentNoticesResult.Loaded` |
| 3.11 | Documentación | `FASE4.md` conserva secciones que dicen "reglas no desplegadas" y "queda 4.7/4.8", ya cerradas | secciones 7.2 y 8 de `FASE4.md` | Actualizar el texto a estado real; no borrar el registro histórico |

**Criterio de cierre de la prioridad 3:** ningún control visible sin acción, ningún texto que afirme un dato no medido y tracker sin estados obsoletos. No requiere cambios de reglas.

---

## 6. Orden sugerido

1. **Prioridad 2** en este orden: 2.1 → 2.7 → 2.3 → 2.4 → 2.5 → 2.6 → 2.8 → 2.2 → 2.9. Primero lo que escribe datos mal; después lo que muestra datos desactualizados.
2. **Prioridad 3** agrupada por módulo, en una sola rama por módulo para no mezclar diffs.
3. Al cerrar ambas: actualizar `FASE4.md`, correr `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` y hacer una pasada final en dispositivo con el checklist de la sección 7.2 de `FASE4.md`.

No se decides en este documento qué se implementa primero: eso se acuerda antes de abrir cada rama.

---

## 7. Registro

**L1** — Auditoría solicitada (verbatim): "Biennn, si funciono correctamente todo, ahora necesito que por favor subas todo esto a main, para que todo este oparejo." y luego "Aun faltan pequeñas cosas a corregir pero creo que ya son minimas, gustarias que le demos una checada a cada modulo, directivo, docente y tutor para ver que pequeñas cosas faltan?"

**L2** — Prioridad 1 autorizada (verbatim): "si arranca corrigiendo primero la prioridad 1."

**L3** — Cierre de la prioridad 1: commit `170cddd`, merge y push a `main` en `8962f6e`. Verificación 104 suites / 665 tests / 0 fallos + `assembleDebug` verde.

**L4** — Estado actual: prioridades 2 y 3 pendientes, sin fecha de ejecución. Las reglas de Firestore ya están desplegadas en `aulasegu` y no hay docentes previos que backfillear.
