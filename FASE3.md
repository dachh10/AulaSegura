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
4. **Directiva:** crea la **cuenta del tutor** en la pestaña Padres. Queda `users/{uid}` con `role: "tutor"`, `studentIds: []`. Le entrega al "padre" su correo, contraseña temporal y el código del hijo.
5. **Docente nuevo:** entra con su cuenta, ve el grupo asignado, pasa lista, reporta un incidente con foto y publica una tarea/aviso. Todo eso ya existe (Fase 1) y **no se toca**.
6. **Padre/tutor:** inicia sesión con su cuenta y, en el primer ingreso, **canjea el código del hijo**. El canjeo marca `linkingCodes.usedBy` y completa en los dos sentidos `users.studentIds` y `students.tutorIds` (ver §4).
7. **Tutor:** ve el resumen real de sus hijos, la asistencia que cargó el docente, las tareas y avisos, los incidentes con mapa, justifica una falta con adjunto, conversa con el docente y recibe notificaciones.
8. **Directiva/docente:** verifican que no duplicaron datos ni roles y que la asignación del paso 2 sigue intacta.

**Criterio de cierre del plan** (`PLAN.md:276`): *"el tutor ve la asistencia que cargó el docente, recibe el push del incidente y conversa con el docente en tiempo real"*. Se extiende con el flujo 1-8 de arriba.

---

## 3. Bloques de trabajo

Los IDs siguen la numeración de `PLAN.md` (3.1-3.10). El orden de ejecución se define en §6.

| # | Tarea | Pantalla(s) | Servicios | Estado |
|---|---|---|---|---|
| **3.1** | Vinculación tutor ↔ alumnos por código, en el primer ingreso | Registro/Perfil del tutor | Firestore `linkingCodes`, `users`, `students` | pendiente |
| **3.2** | Inicio: resumen real de los hijos (asistencia, tareas, incidentes) | `ParentHomeScreen` | Firestore | pendiente |
| **3.3** | Asistencia: calendario desde `attendanceSessions` y `records` | `ParentAttendanceScreen` | Firestore | pendiente |
| **3.4** | Justificar falta con adjunto en Cloudinary | `ParentJustifyScreen` | Firestore + Cloudinary | pendiente |
| **3.5** | Tareas y avisos reales, con marca de leído | `ParentTasksScreen`, `ParentTaskDetailScreen`, `ParentNoticesScreen` | Firestore `noticeReads` | pendiente |
| **3.6** | Incidentes de sus hijos + marcar visto | `ParentIncidentsScreen`, `ParentIncidentDetailScreen`, `ParentIncidentMapScreen` | Firestore | pendiente |
| **3.7** | Chat con el docente asignado al grupo | `ParentChatScreen`, `ParentConversationScreen` | RTDB | pendiente |
| **3.8** | Asistente con Firebase AI Logic (Gemini Developer API, free tier, App Check) | `ParentAIScreen` | AI Logic | pendiente |
| **3.9** | Notificaciones + deep links + push desde consola | `ParentNotificationsScreen` | Firestore + FCM | pendiente |
| **3.10** | Perfil del tutor y foto, con los ajustes reales que ya tiene el docente | `ParentProfileScreen` | Auth + Cloudinary | pendiente |

Las 14 pantallas del Bloque 2 de `PLAN.md` están implementadas visualmente, pero **no tienen ViewModel ni repositorio propio** (no hay `ui/screens/parent/*ViewModel.kt`). La Fase 3 las conecta a datos reales una por una; no se rediseñan.

---

## 4. Cambio de contrato necesario (antes de tocar código)

Hay un bloqueo concreto en las reglas publicadas hoy:

- `linkingCodes/{code}` **sí** permite que el tutor canjee el código: `allow update: if isActive() && isTutor() && changedKeys().hasOnly(['usedBy','usedAt']) && usedBy == request.auth.uid && resource.data.usedBy == null` (`docs/firestore.rules:422-426`).
- Pero `students/{studentId}` solo permite `create, update` a `isDirectivo()` (`docs/firestore.rules:164-165`). El tutor **no puede** agregarse a `students.tutorIds`.
- Las reglas de lectura de `students`, `attendanceSessions`, `incidents` y `tasks/incidents` se apoyan en `request.auth.uid in resource.data.tutorIds`. Sin el vínculo inverso, el tutor **no podría leer nada de su hijo** aunque canjee el código.

**Solución (bloque 3.1):** ampliar `students` con una cláusula acotada que permita al tutor agregarse **solo a sí mismo** cuando el código que canjea apunta a ese alumno:

```
allow update: if isDirectivo() && request.resource.data.schoolId == mySchool()
  || (isTutor()
      && isActive()
      && request.auth.uid in request.resource.data.tutorIds
      && !(request.auth.uid in resource.data.tutorIds)
      && changedKeys().hasOnly(['tutorIds', 'updatedAt'])
      /* + comprobación del linkingCode canjeado por este uid */
     );
```

El canjeo se hace en **una transacción/`update` por documento**, en este orden y con rollback lógico si falla alguno:

1. `get` exacto de `linkingCodes/{code}` (sin `list`), validar `usedBy == null`, `expiresAt` vigente y `schoolId` del tutor.
2. `update` del código → `usedBy`, `usedAt`.
3. `update` de `students/{studentId}` → `arrayUnion(uid)` en `tutorIds`.
4. `update` de `users/{uid}` → `arrayUnion(studentId)` en `studentIds` (el propio usuario sí puede escribir `studentIds`).

El cambio de reglas se publica con `firebase deploy --only firestore:rules --project aulasegu` **antes** de probar el flujo en dispositivo. No se agrega ninguna colección ni índice nuevo.

---

## 5. Decisiones y restricciones (no reabrir sin motivo)

1. **El rol sale de `users/{uid}.role`**, no de un custom claim. El script `tools/seed/create-user.mjs` puede escribir el claim como optimización, pero las reglas leen el documento (`docs/firestore.rules:47-61`).
2. **El tutor no se crea a sí mismo.** `users.create` exige `isDirectivo()` (`docs/firestore.rules:112-121`). Con Spark y sin Cloud Functions, no hay forma segura de que un registro público cree un `users/{uid}` con `role: "tutor"`. El alta la hace la directora desde la app (Fase 2, Auth secundaria) y el "registro con código" es el **canjeo en el primer ingreso**, no una autocreación de cuenta. Esta es la diferencia clave con el wording del `PLAN.md:265`.
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
| 1 | **3.1** | Sin vínculo no hay ningún dato visible. Desbloquea todo lo demás; incluye el cambio de reglas. |
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

Conteo de suites/tests desde `app/build/test-results/testDebugUnitTest/*.xml`. La línea base al iniciar la fase es **52 suites / 381 tests / 0 fallos**.

**Dispositivo:** el teléfono `65d5dd78` está conectado por USB. Se puede usar `adb` completo desde `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` para instalar el APK y correr `connectedDebugAndroidTest` cuando el bloque lo amerite. Las pruebas que dependen de la cuenta real (directora, docente, tutor) las ejecuta el equipo con los tres roles.

**Checklist de cierre de la fase:**

- [ ] Reglas de `students` con la cláusula de canjeo **publicadas** en `aulasegu`.
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
- **Fase 4** (`PLAN.md:280`) queda para después: Hosting y Test Lab. Test Lab tiene cuota gratuita limitada; verificar dispositivos antes de usarlo.
