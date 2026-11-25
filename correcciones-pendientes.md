# Correcciones pendientes — presentación del proyecto

Fecha: 2026-11-24 (cierre). Estado: `main == origin/main`,Work unit todos commiteados.
Estado de los cinco ítems originales: **D1 ✅, D2 ✅, D3 ✅, D4 ✅, D5 ✅**.
**Todos los ítems del documento están cerrados y verificados en dispositivo.**
Además se cerraron los subdetalles derivados de grupos: grupos duplicados, materia por grupo,
crash de arranque por una query denegada y selector de materias en tareas.
Suite final: 129 suites / 793 tests / 0 fallos.

> **APK de entrega actual:** `C:\Users\diego\Documents\AulaSegura.apk` (SHA-256
> `30375938A5D8224C4508F3ECE6C6279C2EEEC95A4AC79094D529BD138F91FF8E`, 25.57 MB)
> es el **release firmado** construido desde el árbol de trabajo actual: incluye todos
> los cambios documentados abajo y el pase de lista único descrito más adelante,
> con la firma de release verificada (`minSdk 24`, `targetSdk 37`,
> sin flag `debuggable`). Quien tenga el build de debug debe desinstalarlo antes de
> instalar este, porque la firma es distinta.

## Un pase de lista por grupo, día y materia (2026-11-25)

> Un docente solo puede cerrar **una** asistencia por grupo, fecha y materia.
> `StartRollCallUseCase` consulta `existingSessionId` antes de abrir la pantalla y
> devuelve `AlreadyTaken` sin crear borrador ni registrar el evento de inicio;
> `SaveRollCallUseCase` repite la verificación antes de escribir en Room, para que
> dos pantallas abiertas no puedan guardar dos sesiones. La búsqueda incluye
> `teacherId` y acepta la etiqueta de materia de las sesiones heredadas, así que
> otro docente o otra materia del mismo grupo no quedan bloqueados. La pantalla
> muestra el aviso y deshabilita marcar/guardar. 6 tests JVM nuevos: 129 suites /
> 793 tests / 0 fallos. El APK de entrega ya incluye este cambio; el código queda
> pendiente de commit.

## Pulido visual final (2026-11-24) - 20 detalles reportados

Diego aprobó tres excepciones después de ver la evidencia: la campana de "Mis grupos"
**se conserva** (navega a Notificaciones), los filtros de Tareas del tutor **se conservan**
(filtran por fecha; "Entregadas" queda vacía porque no hay estado de entrega), y el saludo
del panel ahora es neutral (`adminGreeting`, sin asumir género). El monograma "RS" era un
artefacto del mockup de otro docente y se quitó. `testDebugUnitTest` en verde; **pendiente
la prueba en dispositivo**, que hace Diego porque no hay equipo conectado.

| # | Detalle | Resultado |
|---|---|---|
| 1 | Fondo de escudos duplicado en el chat | Quitado el `ShieldPattern` local en docente y tutor; queda solo el global de `MainActivity`. |
| 2 | Icono de ayuda en Publicar aviso | Quitado. |
| 3 | "Enviar notificación" en Publicar aviso | Quitada la tarjeta deshabilitada. |
| 4 | Tres puntos en Publicar tarea | Quitados. |
| 5 | "Notificar a las familias" | Quitada la tarjeta deshabilitada. |
| 6 | Icono de ayuda en Registrar incidente | Quitado. |
| 7 | "RS" y campana en Mis grupos | "RS" quitado (monograma fijo del mockup); campana conservada. |
| 8 | Cámara en el composer del chat | Quitada en docente y tutor. |
| 9 | Tres puntos y micrófono en el asistente IA | Quitados. |
| 10 | Filtros Pendientes/Entregadas/Vencidas/Todas | Conservados por decisión de Diego. |
| 11 | "Aceptar todas" partido en caracteres | Bajado a su propia fila, con forma de píldora y ancho propio. |
| 12 | Títulos tapados por la cámara | `statusBarsPadding()` en Solicitudes de docentes y Alumnos sin grupo. |
| 13 | "Hola, directora Diego…" | Saludo neutral: `adminGreeting(fullName)`. |
| 14 | Filtros de grupos hasta 3° | Agregados 4°, 5° y 6° (`AdminGroupGradeFilter`). |
| 15 | "Continuar con Google" en el login | Quitado junto con el divisor "o" (no era Google: hacía login con correo). |
| 16 | Materias desacomodadas en el registro | `FlowRow` en grados, grupos y materias; verificado para los 6 grados del catálogo. |
| 17 | "Omitir" en olvidé contraseña | Quitado (ya existe "Volver a iniciar sesión"). |
| 18 | "MIS MATERIAS" ocupaba demasiado | Resumen colapsable "Mis materias · N" que se despliega al tocar. |
| 19 | Línea de escuela en el perfil | Una sola línea con ellipsis y tipografía legible. |
| 20 | Correo apachurrado en el encabezado | El correo bajó a su propia línea con ellipsis. |

## Tres detalles finales (segunda ronda, 2026-11-24)

| # | Detalle | Resultado |
|---|---|---|
| 1 | Filtros de "Mis grupos" se sobreencimaban al elegir materia | Causa real: las dos filas eran hermanas dentro del `Box` de `Reveal`, que las apila en el mismo origen. Se envolvieron en un `Column`; además ambos grupos usan `FlowRow` y el texto del chip tiene `maxLines = 1`, ellipsis y ancho máximo. Corregido en `ce1de5b` y refinado después. |
| 2 | Filtros de grado en Grupos (directora) amontonados y sin 6° | Los siete chips (Todos + 1°…6°) van en `FlowRow`, con salto de línea y separación vertical. Los seis grados ya estaban en el enum; el problema era que el `Row` los sacaba de pantalla. |
| 3 | "Registrar incidente" tardaba en abrir | `rememberCurrentLocation()` pedía un fix GPS fresco con timeout de 8 s al entrar. Ahora el formulario usa la última posición conocida (inmediata) y el botón "Usar mi ubicación actual" sí pide el fix preciso. Las pantallas de geocerca conservan el fix fresco de entrada. 3 tests JVM (`LocationFixPolicyTest`). |

> **Nota para el APK final:** estos 20 cambios y los 3 detalles finales ya están en
> `Documents\AulaSegura.apk`, junto con todo lo anterior.

> **Resuelto 2026-11-24 — "No se pudo cargar" en Inicio/Asistencia de un tutor nuevo.**
> No era la base de datos. Lectura de solo lectura de Firestore: los 3 tutores tienen
> `studentIds`, `groupIds` y `tutorIds` coherentes; el tutor afectado tiene un alumno con
> 3 sesiones de asistencia y **0 registros** del alumno en esas sesiones. Los otros tutores
> sí tienen 1–3 registros. `readRecord()` tragaba todas las excepciones y devolvía `null`,
> y `loadMonth()` interpretaba "todos null" como fallo de carga. Ahora `Missing` es un mes
> vacío y `Failed` conserva el motivo real (denegado / sin conexión). Ver commit
> `e1b11c4` y `ParentRecordReadTest`.

## Reglas de trabajo

- No tocar `app/build.gradle.kts` (firma, lint, `testOptions`), `keystore*`, `google-services.json`
  ni `local.properties`. No generar un APK de release ni crear un keystore nuevo.
- No cambiar la configuración de App Check en Firebase Console.
- No hacer push a `main`: trabajar en una rama por arreglo y avisar a Diego para el merge.
- Texto visible en español; identificadores, nombres de archivo y mensajes de commit en inglés.
- Todo cambio de comportamiento lleva su test. Si no hay runner determinista, documentar la
  excepción y hacer una verificación estructural.

## Orden recomendado

| Orden | ID | Por qué |
| --- | --- | --- |
| 1 | D1 | ✅ **Completado 2026-11-24.** Preselección validada contra el roster; aviso a un grupo específico funciona. |
| 2 | D3 | ✅ **Completado.** Loop de navegación corregido por Monserrat; además "Volver a enviar solicitud" reabre la solicitud sin borrar la cuenta. |
| 3 | D5 | ✅ **Completado 2026-11-24.** Perfil muestra `SchoolSite.name`, con ID solo como fallback; 3 tests. |
| 4 | D2 | ✅ **Completado por Monserrat.** Botón async-aware con "Ubicando…", reintento y mensaje visible; 6 tests JVM. |
| 5 | D4 | ✅ **Completado 2026-11-24.** Bandeja en el docente dueño de la sesión; directora solo lectura; foto obligatoria salvo "Asunto familiar"; falta → `justified`. |

---

> **Grupos duplicados (2026-11-24):** `createGroup()` usaba `.document()` con ID random
> mientras el resto de la app usa `groupIdFor()` → `grp-5c`, así que "5° C" existía dos veces.
> Además la aprobación por QR escribía `subjectTeachers.g5-espanol-v` como clave literal
> dentro de un `set(merge)`, por lo que `subjectTeachers` parecía vacío y se podía asignar
> la misma materia a dos docentes. Corregido: ID canónico + merge idempotente y mapa anidado
> conservando las materias ya asignadas. Datos reparados: eliminado el grupo manual
> `4nFTff7…`, quitado de `users.groupIds` de Diego, y `grp-5c` con
> `subjectTeachers: { g5-espanol-v: <Dach> }`. 3 tests nuevos, suite 773/773.
> **Verificado en dispositivo por Diego el 2026-11-24:** crear manualmente un grupo y
> registrar un docente con el mismo grado/grupo no duplica el grupo; la materia ya asignada
> aparece bloqueada en el registro y otra materia sí se acepta.

> **Materia por grupo (2026-11-24):** `users.subjects` era una lista global que se
> sobrescribía al asignar un docente a otro grupo, así que su materia cambiaba en TODOS sus
> grupos. La fuente de verdad pasa a ser `groups/{id}.subjectTeachers`: el diálogo de
> "Asignar" pide la materia (solo las libres del grado), desasignar libera esa materia, y
> `users.subjects` queda como proyección de todos los grupos con etiqueta
> `"Materia (Grupo)"` en el perfil. Pase de lista, tareas y tarjetas leen la materia del
> grupo. Datos reproyectados en Firestore. 4 tests nuevos, suite 777/777.

> **Crash de arranque (2026-11-24):** la proyección de materias consultaba
> `groups where teacherIds array_contains uid`; las reglas no pueden probar esa query y el
> listener devolvía `PERMISSION_DENIED`, tumbando la app al iniciar sesión. Se cambió por
> lecturas puntuales de los grupos en `users.groupIds`, envueltas en `runCatching`. Verificado
> en dispositivo: app abierta sin `FATAL EXCEPTION`. Commit `959f88c`.

> **Selector de materias en tareas (2026-11-24):** el formulario calculaba las materias una
> vez desde la lista global, así que con dos grupos aparecían las dos. Ahora se derivan de
> `subjectAssignments` del grupo seleccionado y la materia se ajusta al cambiar de grupo.
> Commit `bff1499`, verificado en dispositivo por Diego.

## D1 — Aviso a un grupo específico falla; "Todos mis grupos" funciona — ✅ COMPLETADO (2026-11-24)

> **Resuelto:** al abrir "Publicar aviso" desde el detalle de un grupo, el ID de la ruta se
> preseleccionaba sin validarlo contra el roster vigente. Ese ID podía estar vencido; al
> publicar, `RoomNoticeRepository.publish()` no lo encontraba en `observeMyGroups()` y devolvía
> `GroupNotMine`, mientras "Todos mis grupos" funcionaba porque recalcula desde el roster en
> vivo. `preselectGroup()` ahora ignora IDs que no estén en los grupos del docente. Datos
> verificados en Firestore: `grp-1a` sí tiene al docente asignado, así que no era un problema
> de vinculación. Commit `a932dfc`, 2 tests nuevos, suite 768/768. Verificado en dispositivo
> por Diego.

**Síntoma (texto literal):** «Ese grupo ya no está en tu lista. Actualiza tus grupos.»
Aparece al publicar con "1° A" seleccionado, aunque el grupo esté en la lista. Con
"Todos mis grupos" el aviso llega bien al mismo grupo.

**Ruta confirmada:**
- `ui/screens/teacher/TeacherTaskNewUi.kt:89,97` produce el mensaje para tasks y avisos.
- `data/repository/RoomNoticeRepository.kt:47-54` (y `RoomTaskRepository.kt:50-56`) es el
  único origen del error: compara cada ID de `NewNotice.groupIds` contra
  `groups.observeMyGroups().first()` y devuelve `GroupNotMine(id)` si no lo encuentra.
- `ui/screens/teacher/TeacherNoticeNewViewModel.kt:35,96-119,138-162` guarda la selección en
  un `MutableStateFlow<Set<String>>`. `toggleAllGroups()` recalcula el conjunto desde
  `myGroups.value` **en el momento del clic**; `toggleGroup()` y `preselectGroup()` guardan un
  ID que puede quedar guardado mucho antes de `publish()`.
- `data/repository/FirestoreRosterRepository.kt:57-64`: `observeMyGroups()` filtra por
  `user.schoolId` y por `user.uid in teacherIds`. Si el `schoolId` de la sesión cambia
  (ver D5) o el roster se recarga entre la selección y el envío, el conjunto de IDs válidos
  cambia y el ID guardado deja de existir.
- Descartado: mezcla de etiqueta/ID en la navegación. `TeacherGroupsScreen.kt:198`,
  `Routes.kt:102`, `AulaSeguraNavHost.kt:827,833,846-847` pasan `group.id` de principio a fin.

**Causa:** confirmada la condición exacta que dispara el error; la razón por la que el ID
seleccionado deja de estar en la lista es una hipótesis (selección vencida tras recarga de
roster o de `schoolId`). **No arreglar a ciegas.**

**Paso 1 obligatorio — instrumentar y reproducir en dispositivo:**
en `publish()`, loguear `selectedGroupIds.value`, los IDs de `observeMyGroups().first()` y
`auth.currentUser.schoolId`; verificar con el caso del usuario. Adjuntar el log al informe.

**Dirección de arreglo (según lo que muestre el log):**
- Si el ID no está en la lista al publicar: recargar grupos antes de validar y podar la
  selección cuando el roster cambie; o hacer que publish valide contra el mismo snapshot que
  usó la UI. No debilitar la validación de propiedad del grupo (es una regla de seguridad).
- Si el `schoolId` cambió: correlacionar con D5 y resolver la identidad de la escuela desde
  la fuente canónica antes de publicar.

**Aceptación:** publicar a un grupo específico funciona sin pasar por "Todos"; publicar a un
grupo ajeno sigue devolviendo el error. Test de repositorio con recarga de roster entre
selección y publicación.

---

## D2 — "Usar mi ubicación actual" no hace nada — ✅ COMPLETADO (Monserrat, 2026-11-23)

**Ruta confirmada:**
- `ui/screens/teacher/TeacherIncidentNewScreen.kt:191-200`: si `location.fix == null`, el
  handler solo llama a `requestPermission()` o a `retry()` y termina. No hay estado visible
  de "ubicando" ni mensaje de error en ese botón.
- `location/LocationSupport.kt:42-90`: `retry()` incrementa un contador y dispara **una**
  lectura de `provider.currentFix()`; `locating` existe pero el botón no lo muestra.
- `location/LocationProvider.kt:56-76`: `currentFix()` usa `getCurrentLocation` con timeout y
  hace fallback a last-known; puede devolver `null` si todavía no hay fix.

**Causa:** con permiso concedido pero sin fix disponible, la pulsación hace una tentativa
silenciosa y no deja rastro en la UI: para el docente el botón está muerto.

**Corrección:** el botón debe ser async-aware. Si no hay permiso → pedirlo; si hay permiso
pero no hay fix → mostrar "Ubicando…" mientras se espera una lectura fresca con timeout
visible; si falla → mensaje concreto ("No pudimos obtener tu ubicación. Intenta de nuevo.").
No tocar la lógica de geocerca ni el guardado del incidente. Extraer la decisión a una
función pura y cubrirla con tests; verificar en dispositivo con GPS activado y desactivado.

> **Avance 2026-11-23 (rama `fix/d2-location-button`):** `LocationUiState` ahora expone
> `failed` cuando una lectura con permiso no devuelve nada. La decisión del botón vive en
> `locationButtonUi()` (pura): sin permiso pide permiso, con fix lo aplica, durante la lectura
> muestra «Ubicando…» con el botón deshabilitado, y si falla muestra «Reintentar ubicación»
> junto al mensaje «No pudimos obtener tu ubicación. Intenta de nuevo.». El handler del clic y
> la UI consumen la misma decisión, así que no puede haber etiqueta sin acción. La geocerca y
> el guardado del incidente quedan intactos. Cubierto por 6 tests JVM. **Excepción
> documentada:** no hay runner determinista para el estado Compose ni para el GPS real en la
> suite unitaria; queda pendiente la verificación en dispositivo con GPS activado y desactivado.

---

## D3 — Pantalla "Solicitud rechazada" se reinicia en bucle — ✅ COMPLETADO (Monserrat 2026-11-23 + reenvío 2026-11-24)

> **Extensión 2026-11-24:** un rechazo por error dejaba al docente atrapado: la cuenta de
> Auth y su `users/{uid}` seguían existiendo con `estado: rechazado`, así que no podía
> registrarse de nuevo (*"Ya existe una cuenta con ese correo"*) ni entrar. La pantalla
> rechazada ahora tiene **"Volver a enviar solicitud"**, que abre un formulario para elegir
> de nuevo grado, grupo y materia y vuelve el perfil a `pendiente` sin crear ni borrar
> ninguna cuenta de Auth. `firestore.rules` permite ese único write al propio documento
> (`estado`, `requestedGroupId`, `requestedSubject`, `subjects`, `updatedAt`), manteniendo
> intactos identidad, escuela y `active`. Reglas desplegadas; suite 770/770.

**Causa CONFIRMADA: ciclo infinito de navegación.** Cadena de evidencia:

1. `ui/screens/access/AccessStateScreens.kt:468` — `LaunchedEffect(Unit) { onRefresh() }` al entrar.
2. `AccessStateScreens.kt:255-281` — `refreshState()` setea `proceed = true` también cuando
   `estado == "rechazado"` (línea 274).
3. `navigation/AulaSeguraNavHost.kt:346-352` — `LaunchedEffect(ui.proceed)` navega con
   `popUpTo(Routes.ACCOUNT_REVIEW) { inclusive = true }`.
4. `AccessStateScreens.kt:686-690` — `nextRoute()` para un docente rechazado devuelve
   **`"account_review"`**: el composable se destruye y se recrea, vuelve el
   `LaunchedEffect(Unit)`, vuelve el refresh, vuelve `proceed` → bucle.

Eso explica exactamente el movimiento "de arriba hacia abajo" (se reinicia el scroll) y que
"Cerrar sesión" sea casi imposible de presionar.

**Corrección (chica):** en `refreshState()`, para `UserRole.TEACHER` setear
`proceed = user.estado == "aprobado"` — un rechazo nunca habilita navegación. Además, no
auto-refreshear al entrar cuando `rejected == true` (evita trabajo de red innecesario).
Opcional: guardia idempotente en `consumeProceed()`.

**Aceptación:** test de `AccessStateViewModel` donde un docente rechazado queda con
`proceed == false` después de `refreshState()`; verificación en dispositivo de que la
pantalla queda quieta y "Cerrar sesión" responde al primer toque.

---

## D4 — La justificación del tutor no le llega al docente — ✅ COMPLETADO (2026-11-24)

> **Estado final:** el docente **dueño de la sesión donde se marcó la falta** ve las
> justificaciones de sus grupos y es el único que aprueba o rechaza; la directora conserva
> solo lectura supervisora y no tiene acceso desde Inicio ni desde Más. Al aprobar, la falta
> pasa a `justified` (sin cambios en ese efecto). El tutor debe adjuntar foto salvo en
> "Asunto familiar". Verificado en dispositivo por Diego el 2026-11-24.
> Implementado en los commits `0fc25a9` (paso 1) y `cc30a9c` (paso 2); reglas desplegadas
> con `firebase deploy --only firestore:rules`. Suite final: 766 tests, 0 fallos.

> **Avance 2026-11-23 (rama `fix/d4-justification-inbox`):** Etapas 1, 2 y 3 implementadas
> y verificadas con tests unitarios. Etapa 1 = bandeja de lectura del docente; Etapa 2 =
> bandeja de la directora con aprobación/rechazo y nota; Etapa 3 = la aprobación pasa la
> falta a `justified` en el mismo `WriteBatch`. La tarjeta es compartida por ambos roles.
> Las decisiones de producto ya quedaban registradas en el código y en `CONTEXT.md`: aprueba
> la directora (las reglas no permiten otra cosa) y la falta pasa a `justified`, nunca a
> `present`. No se tocó `firestore.rules`: ya autorizan a la directiva a escribir el
> registro y los `counts` de la sesión.

### Qué pasa hoy, con evidencia

- `data/repository/QueuedJustificationRepository.kt:43-94`: `submit()` guarda el comprobante
  en disco y encola **una fila** en outbox. El comentario de las líneas 77-78 lo dice:
  «there is no cache entity to keep in step».
- `sync/JustificationSyncHandler.kt:64-69`: el worker sube el comprobante a Cloudinary y
  hace `set()` en `justifications/{uuid}`. No escribe nada más.
- `data/mapper/JustificationPayload.kt:13-16,101-122`: el documento lleva `studentId`,
  `tutorId`, `schoolId`, `groupId`, rango de fechas, motivo, descripción, `attachment` y
  `status: "pendiente"`. El comentario es explícito: **«a justification is write only — no
  tutor screen reads one back»**.
- `docs/firestore.rules:548-567`:
  - `read`: permitido a directivo de la escuela, al tutor autor **y al docente cuyo
    `groupId` esté en `myGroups()`**. O sea: el permiso de lectura del docente **ya existe**.
  - `create`: solo tutor, con `status == 'pendiente'`.
  - `update`: **solo directivo**, y solo sobre `status`, `reviewedBy`, `reviewedAt`, `reviewNote`.

> **Seguimiento 2026-11-23 (bandeja vacía, sin importar el estado):** el docente no ve
> ninguna justificación, pendiente o aprobada. Hipótesis con evidencia en código: conviven
> dos vocabularios de ID de grupo. `assignGroup()`/QR escriben `grp-{grado}{letra}`
> (`groupIdFor()`, `FirestoreAdminStudentRepository.kt:303`), pero "Crear grupo" usa
> `.document()` con ID random (`FirestoreAdminGroupRepository.kt:83`). La bandeja consulta
> por los IDs de `groups` (`observeMyGroups()`), mientras la justificación lleva el ID del
> alumno. Si no coinciden, la query da 0 y la directora —que lee por `schoolId`— igual ve
> y aprueba. **Confirmado con `node tools/seed/repair-group-ids.mjs`** (dry-run, solo lectura):
> con `--write` reescribe las referencias al ID canónico sin borrar documentos. La llave de
> servicio va en `~/.aulasegura/firebase-adminsdk.json` o en `GOOGLE_APPLICATION_CREDENTIALS`;
> si ninguna existe, el script usa el ADC de `gcloud auth application-default login` con el
> proyecto `aulasegu`. El dry-run de 2026-11-23 dio «No hay grupos con id no canónico»: los
> 2 grupos, 4 alumnos y 2 justificaciones actuales ya usan `grp-1a`/`grp-2a` y los
> docentes/tutores tienen esos IDs en `groupIds`. No hubo nada que reparar en los datos.
- Búsqueda exhaustiva en `app/src/main`: no hay `JustificationDao`, no hay
  `observeJustifications`, ninguna pantalla consulta la colección `justifications`, y las
  variables `reviewedBy` / `reviewNote` no aparecen en código de app.

**Conclusión:** no es un problema de notificación ni de sincronización. La justificación se
escribe en un documento que **nadie lee en la app**. No existe bandeja del docente ni
pantalla de aprobación de la directora. El dato está en Firestore; la funcionalidad no existe.

### Qué pasa con la asistencia

- `domain/model/Attendance.kt:15-21` ya define `JUSTIFIED("justified")` como uno de los cuatro
  estados válidos. No hay que inventar un quinto.
- La UI ya lo renderiza: `RollCallUi.kt:84`, `TeacherHistoryScreen.kt:1121,1300`,
  `ParentAttendanceUi.kt:171-179` ("Justificada por Dirección"),
  `AssistantPrompt.kt:113` ("falta justificada").
- La fuente de verdad es `attendanceSessions/{sessionId}/records/{studentId}.status`
  (`docs/firestore.rules:403-425`; update permitido a directivo y al docente dueño de la sesión).
- Hoy nadie cambia ese `status` al justificar: el tutor solo encola el documento y el worker
  solo escribe `justifications/{id}`.

### Decisión de producto que Diego debe tomar ANTES de programar

1. **¿Quién aprueba?** Recomendación: la **directora**. Las reglas ya lo permiten solo a ella;
   el docente sería lector. Si se quiere que apruebe el docente, hay que cambiar
   `docs/firestore.rules` y perder la propiedad de "la escuela decide".
2. **¿La falta pasa a "presente"?** Recomendación: **NO**. Pasa a **`justified`**.
   Razones: conserva el historial y la auditoría, los contadores ya tienen el bucket
   `justified`, el tutor ya ve "Justificada por Dirección", y marcar `present` borraría que
   hubo una inasistencia. "Justificada" no es "presente".
3. **¿Dónde la ve el docente?** Recomendación: (a) una tarjeta "Justificaciones pendientes"
   en el inicio del docente con el número de solicitudes de sus grupos; (b) una pantalla de
   lista con alumno, fecha, motivo, descripción, comprobante y estado; (c) el detalle
   accesible también desde el historial de asistencia del grupo/sección.

### Implementación por etapas

- **Etapa 1 — bandeja de lectura del docente (slice mínimo para la demo, sin tocar reglas).**
  Nuevo método en el repositorio de justificaciones: `observeForGroups(groupIds)` consultando
  `justifications` filtrando por `groupId` (ojo: `whereIn` de Firestore acepta máximo 10
  valores por consulta — particionar). Mapear a un modelo de dominio (incluido el attachment
  de Cloudinary). Tarjeta + lista en el rol docente. Sin escribir estados, sin tocar asistencia.
- **Etapa 2 — aprobación de la directora.** Pantalla que haga `update()` de
  `status` (`aprobada` / `rechazada`), `reviewedBy`, `reviewedAt` y `reviewNote`, respetando
  `changedKeys().hasOnly(...)` de las reglas actuales.
- **Etapa 3 — efecto en asistencia.** En un **único `WriteBatch`**: actualizar el
  `justifications/{id}` **y** el `status` del registro en
  `attendanceSessions/{sessionId}/records/{studentId}` a `"justified"`, recalculando los
  `counts` con la lógica existente (`AttendanceSyncMapper.withCountsFrom`). Para ubicar la
  sesión hace falta el `sessionId` en el documento de justificación: agregarlo al payload
  cuando se envía (campo nuevo, compatible con documentos viejos; para esos, resolver por
  `groupId` + fecha). Después de esto, los pantallazos de docente y tutor ya muestran
  "Justificado" sin UI nueva.

  **Implementado:** `JustificationPayload.sessionId` ya venía en el payload; el review lee
  el documento desde `Source.SERVER` (no se fiaba de la tarjeta), resuelve la sesión vieja
  por `groupId` + fecha ISO con un límite de 10 candidatos en orden de ID, y decide el
  efecto con la función pura `justifyAbsence()`. El rechazo no toca asistencia; aprobar un
  registro ya `justified` es idempotente; un mark posterior de `present`/`late` no se
  degrada. Si no aparece el registro de falta, **no se escribe nada** y se devuelve
  `ABSENCE_NOT_FOUND` con mensaje visible, para no aprobar una justificación que no cambia
  la asistencia del tutor. `AttendanceCounts.toFirestoreMap()` es ahora la única fuente del
  subdocumento `counts`, compartida con `withCountsFrom`.

**Tests requeridos:** mapeo del documento (incluido attachment ausente), consulta por grupos
con troceo de 10, transiciones de estado, write batch atómico, y recálculo de contadores.
Si se cambian reglas, agregar el test de reglas correspondiente.

**Verificación del 2026-11-23:** `testDebugUnitTest` en verde (741 tests, 0 fallos). La
decisión de asistencia (`justifyAbsence`), el recálculo/mapeado de contadores y el mensaje
de `ABSENCE_NOT_FOUND` están cubiertos por tests JVM. **Excepción documentada:** no hay
runner determinista de Firestore en la suite unitaria (sin emulador ni tests de
instrumentación), así que el `WriteBatch` se verificó estructuralmente — una sola
`batch.commit()` con las tres escrituras, `Source.SERVER` para las lecturas y ninguna
regla nueva — y queda pendiente la prueba en dispositivo aprobando como directora.

---

## D5 — El perfil del docente muestra "sch-benito-juarez" en vez del nombre del plantel — ✅ COMPLETADO (2026-11-24)

> **Resuelto:** `TeacherProfileViewModel` observa `SchoolRepository.observeSchool()` y el
> perfil renderiza `SchoolSite.name`; el ID queda solo como fallback de documentos viejos.
> Se agregó `refreshSchool()` al entrar al perfil para que un nombre recién guardado por la
> directora aparezca sin visitar otra pantalla. 3 tests JVM (`schoolLabel`) y verificado en
> dispositivo por Diego. Commit `291cd80`.
> **Nota de datos:** en la inspección de Firestore no había ningún documento en `schools`,
> así que el panel mostraba "Plantel sin nombre". Hasta que la directora guarde
> "Configuración del plantel", el perfil muestra correctamente "Sin asignar".

**Causa CONFIRMADA:** se estaba mostrando el **ID** de la escuela como si fuera el nombre.
- `ui/screens/teacher/TeacherProfileScreen.kt:429-432` → `"Escuela · ${ui.schoolId}"`.
- `TeacherProfileScreen.kt:502,529` → `SettingsValueRow(label = "Escuela", value = schoolId…)`.
- `ui/screens/teacher/TeacherProfileViewModel.kt:35-55` solo observa los grupos; no consulta
  la escuela.
- La fuente correcta ya existe: `domain/repository/SchoolRepository.kt:28` →
  `observeSchool(): Flow<SchoolSite?>`; implementado en
  `data/repository/FirestoreSchoolRepository.kt:36-39` y cacheado por
  `data/local/dao/SchoolDao.kt:14`. La directora escribe ese mismo documento desde
  `FirestoreAdminSchoolRepository`.

**Corrección:** inyectar `SchoolRepository` en `TeacherProfileViewModel`, exponer `schoolName`
en `TeacherProfileUi`, recolectar `observeSchool()` y mostrar `site.name` con fallback
("Sin asignar"). Mantener el ID solo como dato secundario si hace falta. Actualizar
`TeacherProfileUiTest` (hoy usa `schoolId = "sch-benito-juarez"`, líneas 34 y siguientes).

---

## Prompt para la colaboradora (copiar y pegar)

```text
Trabajás en C:\Users\diego\AndroidStudioProjects\AulaSegura. Antes de tocar nada, leé
correcciones-pendientes.md (en la raíz del repo, al lado de FASE0.md…) completo: ahí está el
estado final de cada detalle con su evidencia. D1, D2, D3, D4 y D5 están COMPLETADOS y
verificados en dispositivo; NO los reabras salvo petición explícita de Diego.

Reglas obligatorias:
- No modifiques app/build.gradle.kts, keystore*, google-services.json ni local.properties.
  No generes un APK de release, no crees un keystore nuevo y no cambies nada de App Check
  en Firebase Console. Diego arma y reparte el APK.
- No hagas push a main. Para cada arreglo creá una rama desde un main actualizado
  (fix/d1-group-notice, fix/d3-rejected-loop, fix/d5-school-name, fix/d2-location-button)
  y pusheá la rama. Avisá a Diego con el nombre de la rama y el resumen.
- Texto visible en español neutro; identificadores, nombres de archivo y commits en inglés.
- Todo cambio de comportamiento lleva su test. Corré
  `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest`
  antes de reportar, y pegá el resultado (número de tests y fallos).
- Si algo no se puede verificar, decilo explícitamente en vez de asumir que funciona.

Orden de trabajo:

1) D1 — YA NO HACER: quedó completado y verificado en dispositivo el 2026-11-24.
   No reabrir salvo petición explícita de Diego.

2) D3 — YA NO HACER: completado y verificado en dispositivo.

3) D5 — YA NO HACER: completado y verificado en dispositivo el 2026-11-24.

4) D2 — YA NO HACER: completado por Monserrat con 6 tests JVM.

5) D4 — YA NO HACER: quedó completado y verificado en dispositivo el 2026-11-24.
   No reabrir este ítem salvo petición explícita de Diego.

Entregable por cada ítem: rama, archivos tocados, comando de test con su resultado, y
cualquier decisión o duda abierta. No toques los otros ítems aunque veas algo cercano.
```
