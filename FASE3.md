# Fase 3 — Tutor / Familias

Responsable: **Evelyn** · apoyo: **Diego** · base: `main` @ `2ea023e` · dispositivo de prueba: `65d5dd78` (USB debugging, `adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`).

Este documento se apoya en `PLAN.md` §Fase 3, `FASE1.md` (docente), `FASE2 Directores.md` (directivo) y `CONTEXT.md` (contrato de datos). **No redefine nada de lo anterior**: solo consume lo que esas fases producen y cierra el vínculo que faltaba.

---

## 1. Cómo se engancha con las fases anteriores

| Dato | Lo produce | Lo consume en esta fase |
|---|---|---|
| `attendanceSessions/{id}` + `records/{studentId}` | Fase 1 — pase de lista del docente | 3.2 resumen e **3.3 asistencia del hijo** |
| `incidents/{id}` + media en Cloudinary | Fase 1 — registrar incidente | 3.6 incidentes del hijo, detalle y mapa |
| `tasks/{id}` y `notices/{id}` con `audience.groupIds` | Fase 1 — publicar tarea/aviso | 3.5 tareas y avisos con marca de leído |
| `conversations/{id}/messages` en RTDB | Fase 1 — chat del docente | 3.7 chat con el docente |
| `notifications/{id}` + FCM | Fase 1 — bandeja y token | 3.9 notificaciones del tutor |
| `users/{uid}` con `role`, `schoolId`, `studentIds` | Fase 0/2 — alta con Admin SDK y app del directivo | 3.1 vinculación y 3.10 perfil |
| `groups/{id}` + `schedule` y asignación de docente | Fase 2 — grupos y detalle | define qué ve el tutor y con quién chatea |
| `students/{id}` con `groupId`, `listNumber`, `tutorIds: []` | Fase 2 — alta de alumnos | 3.1 completa `tutorIds`; 3.2-3.6 filtran por hijo |
| `linkingCodes/{code}` de 6 caracteres, `usedBy: null` | Fase 2 — alta de alumnos | 3.1 canjeo exacto, una sola vez |
| `schools/{id}`, reglas e índices publicados | Fase 2 | sin cambios de esquema salvo el punctual de §4 |

**Regla de oro:** si algo que el tutor necesita no existe en las fases 1 o 2, se agrega ahí o se documenta, pero **no se inventa un dato nuevo en la UI del tutor**.

---

## 2. Escenario de aceptación (el flujo que pidió el equipo)

Esta es la prueba guía; todo bloque debe dejarla más cerca de funcionar:

1. **Directiva (Evelyn/Diego):** desde la app crea un **docente nuevo** en Usuarios, con contraseña temporal (Auth secundaria, para no desloguear a la directora). Queda `users/{uid}` con `role: "docente"`, `schoolId` y `groupIds: []`.
2. **Directiva:** crea un **grupo nuevo** y su horario; le asigna ese docente. Escritura atómica: `groups.teacherIds` y `users.groupIds` se actualizan juntos (`AssignTeacherUseCase`, previsto en Fase 2).
3. **Directiva:** crea **dos alumnos** en ese grupo con CURP y número de lista. Cada alta genera un `linkingCodes/{code}` de 6 caracteres, `usedBy: null`, con vencimiento.
4. **Directiva:** crea el docente, el grupo y los alumnos. **No crea tutores**: esa cuenta la crea el padre en el registro (ver abajo). Le entrega al padre el código de vinculación de su hijo.
5. **Docente nuevo:** entra con su cuenta, ve el grupo asignado, pasa lista, reporta un incidente con foto y publica una tarea/aviso. Todo eso ya existe (Fase 1) y **no se toca**.
6. **Padre/tutor:** desde el Login toca **Crea tu cuenta** y completa la pantalla de Stitch `4ed8ecd6ef044004bce46d44e15fc520`: nombre, correo, teléfono, contraseña (mínimo 8), confirmación y el campo obligatorio **Código del alumno**. Con ese código se crea su cuenta `role: "tutor"`, se vincula al hijo y entra directo a las pantallas del tutor. No hay paso extra: **registro = vinculación = inicio de sesión**. El código es el único campo que decide el rol; nadie puede registrarse como docente ni directivo desde aquí.
7. **Tutor:** ve el resumen real de sus hijos, la asistencia que cargó el docente, las tareas y avisos, los incidentes con mapa, justifica una falta con adjunto, conversa con el docente y recibe notificaciones.
8. **Directiva/docente:** verifican que no duplicaron datos ni roles y que la asignación del paso 2 sigue intacta.

**Criterio de cierre del plan** (`PLAN.md:276`): *"el tutor ve la asistencia que cargó el docente, recibe el push del incidente y conversa con el docente en tiempo real"*. Se extiende con el flujo 1-8 de arriba.

---

## 3. Bloques de trabajo

Los IDs siguen la numeración de `PLAN.md` (3.1-3.10). El orden de ejecución se define en §6.

| # | Tarea | Pantalla(s) | Servicios | Estado |
|---|---|---|---|---|
| **3.1** | Registro **solo del tutor** con código obligatorio y vinculación atómica al alumno | `RegisterScreen` (Stitch `4ed8ecd6...`) | Auth + Firestore `linkingCodes`, `users`, `students` | ✅ **hecho** (código y tests; reglas pendientes de publicar) |
| **3.2** | Inicio: resumen real de los hijos (asistencia, tareas, incidentes) | `ParentHomeScreen` | Firestore | ✅ **hecho** (código y tests) |
| **3.3** | Asistencia: calendario desde `attendanceSessions` y `records` | `ParentAttendanceScreen` | Firestore | ✅ **hecho** (código y tests) |
| **3.4** | Justificar falta con adjunto en Cloudinary | `ParentJustifyScreen` | Firestore + Cloudinary | ✅ **hecho** (código y tests; falta probar en dispositivo) |
| **3.5** | Tareas y avisos reales, con marca de leído | `ParentTasksScreen`, `ParentTaskDetailScreen`, `ParentNoticesScreen` | Firestore `noticeReads` | ✅ **hecho** (código y tests; regla de avisos pendiente de publicar) |
| **3.6** | Incidentes de sus hijos + marcar visto | `ParentIncidentsScreen`, `ParentIncidentDetailScreen`, `ParentIncidentMapScreen` | Firestore `seenByTutorIds` | ✅ **hecho** (código y tests; sin incidentes en los datos demo al validar la lectura) |
| **3.7** | Chat con el docente asignado al grupo | `ParentChatScreen`, `ParentConversationScreen` | RTDB | ✅ **hecho** (código y tests; falta probar en dispositivo) |
| **3.8** | Asistente con Firebase AI Logic (Gemini Developer API, free tier, App Check) | `ParentAIScreen` | AI Logic | pendiente |
| **3.9** | Notificaciones + deep links + push desde consola | `ParentNotificationsScreen` | Firestore + FCM | pendiente |
| **3.10** | Perfil del tutor y foto, con los ajustes reales que ya tiene el docente | `ParentProfileScreen` | Auth + Cloudinary | pendiente |

Las 14 pantallas del Bloque 2 de `PLAN.md` están implementadas visualmente. La Fase 3 las conecta a datos reales una por una; no se rediseñan. Cada bloque cerrado tiene ViewModel, repositorio y tests propios (`ui/screens/parent/*Ui.kt`, `*ViewModel.kt`); la nota de entrega correspondiente indica el detalle.

**Notas de entrega 3.2 + 3.3:**

- `students.whereArrayContains("tutorIds", uid)` resuelve los hijos: `AppUser` no lleva `studentIds`/`groupIds`, así que no hubo migración de Room.
- `attendanceSessions` se lee por `groupId` y el mes se filtra en cliente, para no declarar un índice compuesto que hoy solo existe en la consola.
- El porcentaje es `(presentes + retardos) / total`; una falta justificada sigue siendo inasistencia y se muestra aparte.
- Los datos que la escuela no produjo no se inventan: día sin sesión = celda gris, mes sin registros = porcentaje `—`, tareas vacías = "Sin tareas próximas", hijo sin vínculo = tarjeta explicativa.
- El tutor ve a sus hijos por el vínculo `tutorIds`, **aunque el alumno esté `active: false`**: `active` significa "inscripto hoy" para el roster del docente, no "es mi hijo". Un alumno dado de baja sigue apareciendo en la pantalla de la familia; el roster del docente sigue filtrándolo. Para pasar lista de un alumno dado de baja, el directivo lo reactiva desde el detalle del alumno (pestaña Alumnos → «Reactivar»).
- `ParentAttendanceScreen` cambió de hijo con el pill del top bar (cicla entre hijos); no se agregó un selector nuevo que el diseño no tiene.

**Notas de entrega 3.5** (detalle en `odd/tasks/bloque-3.5-tareas-avisos.md`):

- **Tareas:** `tasks.whereArrayContains("groupIds", g)` para la lista y lectura por id para el detalle. Las secciones salen de la fecha de entrega (`VENCIDAS`, `HOY`, `MAÑANA`, `ESTA SEMANA`, `MÁS ADELANTE`).
- **“Entregadas” queda vacío a propósito:** `tasks` no tiene estado por alumno. Se explica en pantalla en vez de inventar una entrega.
- **Detalle sin botón falso:** se quitaron “Marcar como vista” y el marcador; no hay colección de leídos para tareas.
- **Avisos:** `notices.whereArrayContains("audience.groupIds", g)` por grupo + `users/{uid}/noticeReads/{noticeId}` para el leído (idempotente, optimista y con reversión si falla). El aviso no se escribe nunca desde el tutor.
- **Regla de avisos ajustada:** Firestore sólo permite un `array-contains` por consulta, así que la rama del tutor ya no exige `audience.roles` en la regla (lo exige el cliente, `parentNoticeFrom`); el alcance por grupo se conserva. **Requiere `firebase deploy --only firestore:rules`.**
- **Sin Room, colecciones ni índices nuevos.** La base sigue en `version = 4`.

**Notas de entrega 3.6** (detalle en `odd/tasks/bloque-3.6-incidentes.md`):

- **Lista:** `incidents.whereEqualTo("studentId", id)` por hijo (hasta 100, `Source.SERVER`), orden por `occurredAt` desc; los contadores (`Registrados`, `Nuevo`, `Graves`) son del hijo que muestra el pill, no de la familia. Cambiar de hijo filtra en memoria, sin volver a leer.
- **Detalle y mapa** comparten ViewModel y leen `incidents/{incidentId}` por id. El mapa dibuja el pin de `location`; sin coordenada completa no hay mapa (nada de centro inventado).
- **“Enterado” no es optimista:** escribe `seenByTutorIds` con `arrayUnion` y el estado sólo cambia cuando la nube acepta, porque las reglas no permiten quitarse de la lista. Se fue el botón muerto “Más opciones”.
- **Sin cambios de reglas, colecciones, índices ni Room** (`version = 4`). La rama del tutor en `incidents` ya soportaba la lectura por `studentId` y el update de sólo `seenByTutorIds`.
- **Verificado por REST con la sesión real:** consulta por cada hijo → 200 (hoy 0 incidentes en los datos demo), alumno ajeno y consulta sin filtro → 403.
- **Límite conocido:** la regla de lectura valida contra `users.studentIds` (no contra `students.tutorIds`); con el registro por código coinciden. Un vínculo hecho sólo en `students.tutorIds` daría `PERMISSION_DENIED` al leer incidentes.

**Notas de entrega 3.7** (detalle en `odd/tasks/bloque-3.7-chat-docente.md`):

- **Lista real:** `ParentChatViewModel` observa `conversations` donde participa el tutor y pinta la hora relativa con el mismo formateador que la lista del docente (`ui/chat/ChatFormatting.kt`). La búsqueda filtra en el ViewModel para poder fijarla con tests.
- **Nuevo mensaje:** el lápiz del encabezado abre una hoja con los docentes de los grupos de sus hijos (`students.whereArrayContains("tutorIds", uid)` → `groups/{id}.teacherIds`), una fila por docente distinto. Sin docentes asignados, la hoja lo explica; no se inventa un destinatario.
- **El nombre del docente sale del grupo:** las reglas sólo permiten leer `users/{uid}` a su dueño y al directivo, así que el tutor ve "Docente de 3° B". No se gasta una lectura que la nube negaría; el nombre real aparece sólo si el docente inicia el chat.
- **Conversación real:** mensajes de RTDB (hasta 200), encabezado desde el resumen de Firestore, presencia del otro participante, recibo de lectura al abrir y aviso de "se enviará al reconectar". El chip de día se dibuja sólo al cambiar de día.
- **Sin estados inventados:** se fue "Pendiente de enviar" (RTDB no trae ack por mensaje) y la lista no dibuja presencia por fila. El ícono de cámara queda del diseño, pero el chat con media está fuera de este bloque.
- **"Hablar con el docente"** del detalle y el mapa de incidentes abre la pestaña Mensajes; ya no navega a un id de ejemplo.
- **Sin cambios de reglas, colecciones, índices ni Room** (`version = 4`). Lo compartido se extrajo a `ChatFormatting` y a `ChatContact.otherUid/otherRole`, con `chatConversationSubtitle` derivando "Tutor de…" o "Docente · 3° B" según quién mira.
- **Límite conocido:** el nombre real del docente requiere abrir `users/{uid}` a los tutores de sus grupos (decisión del dueño, no se agregó acá).

---

## 4. Cambio de contrato necesario (antes de tocar código)

El registro libre del tutor con código exige tres ajustes en las reglas publicadas, porque hoy:

- `users.create` solo lo permite `isDirectivo()` (`docs/firestore.rules:112`): el padre no puede crear su propio documento.
- `students.update` solo lo permite `isDirectivo()` (`docs/firestore.rules:164`): el tutor no puede agregarse a `students.tutorIds`.
- `linkingCodes.update` exige `isTutor()` (`docs/firestore.rules:423`), que es falso durante el registro porque `users/{uid}` todavía no existe.

Con Spark y sin Cloud Functions, la forma de hacerlo atómica es un **`WriteBatch` validado con `getAfter()`**:

1. **Secuencia (un solo flujo, sin pasos visibles para el usuario):**
   a. `createUserWithEmailAndPassword` → el usuario queda autenticado (necesario para el `get` del código: `linkingCodes.get` exige `isSignedIn()`).
   b. `get` exacto de `linkingCodes/{code}` — nunca `list` — validar `usedBy == null` y `expiresAt` vigente, y leer `studentId` y `schoolId`.
   c. `WriteBatch` atómico con tres escrituras:
      - `users/{uid}` create → `role: "tutor"`, `studentIds: [studentId]`, `schoolId` del código, `active: true`, `email` del token.
      - `students/{studentId}` update → `tutorIds: arrayUnion(uid)` y `updatedAt`.
      - `linkingCodes/{code}` update → `usedBy: uid`, `usedAt: now`.
   d. Navegar a `Routes.PARENT_HOME`.

2. **Cláusulas nuevas en `docs/firestore.rules`:**
   - `users.create`: además de la de directivo, permitir el autocreate cuando `request.auth.uid == uid`, `role == 'tutor'`, `active == true`, `email == request.auth.token.email`, `schoolId == getAfter(código).schoolId`, `studentIds == [getAfter(código).studentId]`, `get(código).usedBy == null` y `getAfter(código).usedBy == uid`.
   - `students.update`: además de la de directivo, permitir que el tutor se agregue **solo a sí mismo**, con `changedKeys().hasOnly(['tutorIds','updatedAt'])`, `uid` presente en `request.resource.data.tutorIds` pero no en `resource.data.tutorIds`, y el código canjeado apuntando a ese alumno.
   - `linkingCodes.update`: conservar la cláusula actual para el canjeo posterior y agregar una que acepte `getAfter(users/{uid}).data.role == 'tutor'` con el `studentIds` coincidente, porque durante el registro el documento de usuario no existe en el estado previo.
   - `getAfter()` garantiza que el lote sea atómico: si una escritura falla, no se aplica ninguna.

3. **Publicar** con `firebase deploy --only firestore:rules --project aulasegu` antes de probar en dispositivo. No se agrega ninguna colección ni índice nuevo.

**Consecuencia de rolls:** el directivo **no crea tutores**. La pestaña "Padres" del alta de usuarios de la Fase 2 se desactiva o se deja fuera de uso en este bloque, para que haya una sola fuente de verdad: el registro libre con código obligatorio.

**Formato del código:** la pantalla de Stitch muestra `ALU-2024-9821` como ejemplo, pero el valor real que valida la app es el **código de 6 caracteres** que ya genera `AdminNewStudentScreen` (Fase 2, `linkingCodes/{code}`). Se ajusta el placeholder y el texto de ayuda para que no induzca a error.

## 5. Decisiones y restricciones (no reabrir sin motivo)

1. **El rol sale de `users/{uid}.role`**, no de un custom claim. El script `tools/seed/create-user.mjs` puede escribir el claim como optimización, pero las reglas leen el documento (`docs/firestore.rules:47-61`).
2. **Solo el tutor se registra desde el Login**, con el código del alumno como campo obligatorio. La pantalla de Stitch `4ed8ecd6...` ya lo trae ("Código del alumno · Requerido por plantel"); se implementa con el `WriteBatch` de §4, que crea la cuenta, vincula al hijo y deja al usuario dentro de las pantallas del tutor en un solo paso. **Docente y directivo nunca se registran**: el directivo crea docentes y alumnos desde su app; el tutor se crea solo. Esto reemplaza la decisión anterior de este documento y corrige el wording del `PLAN.md:265`.
3. **Un código, un uso.** `usedBy` inmutable después del canjeo. Si el padre pierde el código, la directora genera otro desde el detalle del alumno.
4. **Sin Cloud Functions:** no hay vinculación ni push automáticos. Los push se envían desde la consola o `scripts/notify`; la app solo registra tokens y lee la bandeja.
5. **Chat:** el id de conversación sigue el patrón `{uidA}__{uidB}` de `docs/rtdb.rules`. El tutor chatea con el `teacherId` del grupo de su hijo, resuelto desde `groups/{id}.teacherIds`.
6. **Asistente IA:** Firebase AI Logic con Gemini Developer API, directo desde el cliente y con App Check. Nunca Vertex AI (movería el proyecto a Blaze).
7. **Justificantes:** el tutor **crea**, el directivo **revisa**; el docente solo lee. Adjunto en `aulasegura/justifications/{id}`, nunca en Firestore como base64.
8. **El horario lo escribe el directivo** (Fase 2). El tutor no edita nada del plantel.

---

## 6. Orden de ejecución y bloques de entrega

Cada bloque termina con `assembleDebug` + `testDebugUnitTest` en verde y su registro en este documento. Se entregan de a uno y se reporta antes de seguir.

| Orden | Bloque | Por qué va ahí |
|---|---|---|
| 1 | **3.1** | Registro del tutor con código + reglas. Sin vínculo no hay ningún dato visible; desbloquea todo lo demás. |
| 2 | **3.2 + 3.3** | Resumen y asistencia son la prueba de que el vínculo funciona con datos reales del docente. |
| 3 | **3.5 + 3.6** | Tareas/avisos e incidentes: el resto de lo que el docente ya produce. |
| 4 | **3.4** | Justificar falta es el único flujo de escritura del tutor además del chat. |
| 5 | **3.7 + 3.9** | Chat y notificaciones; dependen de que el grupo y el docente ya estén bien vinculados. |
| 6 | **3.10** | Perfil del tutor: espejo de lo que ya se resolvió en Fase 1 y 2. |
| 7 | **3.8** | Asistente IA: se alimenta de los datos consolidados en los bloques anteriores. |
| 8 | **Cierre** | Verificación en dispositivo, reglas publicadas y Fase 4. |

---

## 7. Verificación

**Build y tests (por bloque):**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest assembleDebug --offline --no-daemon --no-configuration-cache
```

Conteo de suites/tests desde `app/build/test-results/testDebugUnitTest/*.xml`. La línea base al iniciar la fase es **52 suites / 381 tests / 0 fallos**; al cerrar 3.2 + 3.3 es **56 suites / 410 tests / 0 fallos**; al cerrar 3.4 es **59 suites / 436 tests / 0 fallos**; al cerrar 3.5 es **63 suites / 469 tests / 0 fallos**; al cerrar 3.6 es **67 suites / 496 tests / 0 fallos**; al cerrar 3.7 es **68 suites / 504 tests / 0 fallos**.

**Dispositivo:** no hay teléfono conectado. Las pruebas funcionales con las cuentas reales (directora, docente, tutor) las ejecuta el equipo; los pasos se entregan en el reporte de cada bloque.

**Checklist de cierre de la fase:**

- [x] Reglas publicadas en `aulasegu` con las tres cláusulas de §4 (`users.create`, `students.update`, `linkingCodes.update`), con la rama del registro **primero** para que `isDirectivo()`/`isActive()` no evalúen un `users/{uid}` que solo existe como escritura pendiente.
- [ ] El registro solo crea tutores: sin código no se crea la cuenta y no existe opción de registrarse como docente ni directivo.
- [ ] Directora crea docente, grupo y dos alumnos desde la app.
- [ ] Docente queda vinculado al grupo y lo ve sin tocar código.
- [ ] Tutor entra, canjea el código y ve **sus** hijos (no los de otro grupo).
- [ ] Asistencia, tareas, avisos e incidentes del tutor son los que cargó el docente.
- [ ] Tutor justifica una falta con adjunto y el directivo la ve en su bandeja.
- [ ] Chat tutor ↔ docente en tiempo real.
- [ ] Push de incidente llega al teléfono y abre el detalle correcto.
- [ ] `connectedDebugAndroidTest` verde en el dispositivo `65d5dd78`.

---

## 8. Riesgos y cosas que quedan fuera

- **Cambio de reglas compartido:** §4 afecta `students`, que también usan Fase 1 y 2. Hay que correr los tests de esas fases después de publicar.
- **Cuentas temporales:** si la contraseña temporal se usa una vez y Firebase obliga a cambiarla, hay que documentar el paso en el high-five al "padre" antes de la prueba en vivo.
- **Sin feriados ni sustituciones:** ya quedó decidido en Fase 1/2; no se reabre acá.
- **Nombre del docente en el chat del tutor:** la regla de `users` sólo abre el perfil a su dueño y al directivo, así que el tutor ve "Docente de 3° B" hasta que ese docente inicie el chat. Mostrar el nombre real exige abrir `users/{uid}` a los tutores de sus grupos; es una decisión de reglas que queda pendiente.
- **Fase 4** (`PLAN.md:280`) queda para después: Hosting y Test Lab. Test Lab tiene cuota gratuita limitada; verificar dispositivos antes de usarlo.
