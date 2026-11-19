# Handoff — dónde vamos y qué sigue

Actualizado: 19 nov 2026. Este archivo existe para retomar el trabajo en otra superficie (CLI) sin releer todo el historial. Si contradice a `FASE3.md`, a `FASE1.md` o a un logbook de `odd/tasks/`, **gana el logbook del bloque**.

## 1. Qué estamos haciendo

Cerrar el **flujo completo de la app** para la demo:

> La directora da de alta docentes y alumnos → el docente pasa lista, reporta incidentes y publica tareas/avisos → el padre se registra con el código de su hijo y entra a las pantallas del tutor, donde ve **todo lo de su propio hijo**.

Estamos en la **Fase 3 (tutor/familias)**, bloque por bloque, en este orden acordado:
`3.4 ✅ → 3.5 ✅ → 3.6 ✅ → 3.10 → 3.9 → 3.7 → 3.8`.

Regla de trabajo: cada bloque termina con `assembleDebug` + `testDebugUnitTest` en verde, su logbook en `odd/tasks/` y un commit. **El dueño prueba en el teléfono al cerrar cada bloque**; sin dispositivo conectado, el agente no puede validar UI.

## 2. Estado del repo

- Últimos commits, todos en `main` y **publicados en `origin/main`** el 19 nov 2026: `3b26f24` endurecimiento de reglas · `68c5e10` bug de la tarjeta de asistencia del panel · `cc76359` handoff · `3aecba6` bloque 3.6 · `d24798a` bloque 3.5 · `32c7d9f` bloque 3.4 · `8deed20` primer handoff.
- **Reglas publicadas y verificadas**: `firebase deploy --only firestore:rules --project aulasegu` el 19 nov 2026, compilación limpia, y el dueño probó los cinco flujos en el teléfono. El detalle de qué se endureció y qué límites quedan a propósito está en `odd/tasks/reglas-revision-19nov.md`.
- Nada sin commitear salvo `.atl/` (caché de una herramienta, no del proyecto; no se versiona).
- Fase 1 (docente) y Fase 2 (directivo) ya integradas en `main`.

Verificación obligatoria (Windows, Git Bash):

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew.bat :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:assembleDebug --console=plain --offline
```

Estado último (19 nov 2026, tras el endurecimiento de reglas): **BUILD SUCCESSFUL · 67 suites · 497 tests · 0 fallos**.

El `java` del PATH es JDK 20 y rompe Gradle: siempre fijar `JAVA_HOME` al JBR de Android Studio.

## 3. Dónde vamos (Fase 3)

| # | Bloque | Estado |
|---|---|---|
| **3.1** | Registro **solo del tutor** con código de 6 caracteres + vinculación atómica | ✅ hecho. Reglas publicadas en `aulasegu` |
| **3.2** | Inicio del tutor con resumen real de los hijos | ✅ hecho (`3362f3e`) |
| **3.3** | Asistencia: calendario desde `attendanceSessions` + `records` | ✅ hecho |
| **3.4** | Justificar falta con adjunto en Cloudinary | ✅ cerrado (`32c7d9f`) · **probado en teléfono** |
| **3.5** | Tareas y avisos reales, con marca de leído | ✅ **cerrado** (`d24798a`) · tareas, detalle y bandeja con `noticeReads` · **probado en teléfono** |
| **3.6** | Incidentes de sus hijos + marcar visto | ✅ **cerrado** (`3aecba6`) · lista por hijo, detalle y mapa reales · **probado en teléfono** |
| **3.10** | Perfil del tutor y foto | ⏭️ **siguiente** (copiar el patrón de `TeacherProfileViewModel`) |
| **3.9** | Notificaciones del tutor (bandeja real) | pendiente (`sampleNotifications()`) |
| **3.7** | Chat con el docente del grupo (RTDB) | pendiente (`sampleConversations()`) |
| **3.8** | Asistente IA (AI Logic, Gemini Developer API) | pendiente |

Importante: la lista de hijos, la asistencia, el justificante, las tareas, los avisos y los incidentes ya son reales (`ParentRepository`). Lo que sigue inventado en el tutor son **notificaciones** (`sampleNotifications()`) y **chat** (`sampleConversations()`), además del perfil (3.10) y el asistente (3.8). Ese es el hueco que queda para la demo.

## 4. Lo que ya está resuelto del lado directivo (no tocar)

- **Alta de docentes desde la app**: `AdminUserRepository.createAccount` crea el usuario en Auth con una **FirebaseApp secundaria** (no desloguea a la directora) y luego el documento `users/{uid}`; si falla, limpia el usuario de Auth y reporta `orphaned`.
- **Alta de alumnos**: CURP, número de lista automático, grupo y `linkingCodes/{code}` de 6 caracteres con vencimiento.
- **Vinculación**: el código lleva el `groupId` del alumno (`8eebd68`) y el alta del tutor crea la cuenta, actualiza `students.tutorIds` y quema el código en un `WriteBatch`.
- Reglas, índices y RTDB publicados con `firebase deploy`.

## 5. Decisiones del tutor que no se reabren sin motivo

- **Sin `studentIds` en `AppUser`**: los hijos se resuelven con `students.whereArrayContains("tutorIds", uid)`. No hubo migración de Room.
- **El tutor ve a sus hijos aunque estén `active: false`**: `active` significa "inscripto hoy" para el roster del docente, no "es mi hijo".
- **Asistencia**: `attendanceSessions` se lee por `groupId` y el mes se filtra en cliente (no hay índice nuevo). Porcentaje = `(presentes + retardos) / total`; una justificada sigue siendo inasistencia.
- **Datos que la escuela no produjo no se inventan**: día sin sesión = celda gris; mes sin registros = `—`; tareas vacías = "Sin tareas próximas"; sin falta que justificar = **Enviar** deshabilitado.
- **3.4 — qué falta se justifica**: la que el tutor toca en el detalle de Asistencia; desde Inicio, la más reciente del mes y, si no hay, la del mes anterior. La fecha del documento es la del día que marcó la escuela, nunca `now()`.
- **3.4 — adjunto**: se copia a `filesDir` (`justification-attachments/`), se comprime si es imagen y se encola en la outbox (`UPSERT_JUSTIFICATION`). `JustificationSyncHandler` **sube primero a Cloudinary**, guarda la URL en el `payloadJson` y solo entonces hace `set()` del documento. Si el archivo ya no está, el justificante se registra sin evidencia. No hay entidad Room nueva (base en `version = 4`).
- **3.4 — motivos**: el schema manda `enfermedad | cita | viaje | otro`; "Asunto familiar" se guarda como `otro`, se distingue por la descripción. Límite conocido.
- **Sin Cloud Functions (Spark)**: no hay push automático ni triggers; los push se mandan desde la consola o `scripts/notify`.
- **3.5 — tareas**: lista por `groupIds` y detalle por id; secciones por fecha de entrega. `active = false` se descarta en la lista. El chip **Entregadas queda vacío** porque `tasks` no tiene estado por alumno y el schema no registra entregas: no se inventa. Se quitó el botón “Marcar como vista” del detalle (no hay colección de leídos para tareas).
- **3.5 — avisos**: la bandeja lee por grupo y marca leído en `users/{uid}/noticeReads/{noticeId}` (`readAt`, idempotente). Si leer los leídos falla, los avisos se muestran como no leídos; si escribir la marca falla, se revierte y se avisa en pantalla.
- **3.5 — regla de avisos, cerrada el 19 nov**: Firestore permite **un solo `array-contains`** por consulta y prueba las listas sólo con sus filtros, así que la rama del tutor en `notices` no puede exigir `audience.roles` (sería `PERMISSION_DENIED` para toda la bandeja). El rol se garantiza **al escribir**: `create` exige `audience.roles.hasAny(['tutor'])`. Un aviso sólo para personal se rechaza; la comunicación interna va por `notifications`. El cliente (`parentNoticeFrom`) sigue descartando lo que no sea de tutor como segunda barrera.
- **3.6 — incidentes**: una consulta `whereEqualTo("studentId", id)` por hijo (hasta 100, `Source.SERVER`) y filtro por el hijo del pill en memoria; cambiar de hijo no vuelve a leer. Detalle y mapa comparten `ParentIncidentDetailViewModel` y leen `incidents/{id}`. Los contadores son del hijo en pantalla.
- **3.6 — “Enterado” no es optimista**: se escribe `seenByTutorIds` con `arrayUnion` y el estado sólo cambia cuando la nube acepta, porque las reglas permiten agregar pero **no quitar**. Si falla, se queda en pendiente con mensaje; si ya está visto, el botón queda inactivo.
- **3.6 — mapa**: `CampusMap` con `campus = null` y el pin de `incidents.location`; sin coordenada completa no se dibuja mapa. No se pinta geocerca ni se inventa un centro.
- **3.6 — la asimetría de reglas queda cerrada**: leer y marcar visto un incidente usa `tutorOfStudent()`, es decir `students/{id}.tutorIds`, que es el mismo vínculo con el que se listan los hijos. `users.studentIds` sigue siendo la copia desnormalizada que leen tareas, avisos, justificaciones y `records`. La función exige `exists()` y `studentId` no vacío para que un incidente de grupo (sin alumno) no convierta un error de reglas en un `allow` negado.
- **Idioma**: UI en español neutro mexicano (tú), código e identificadores en inglés.

## 6. Pendiente de humano

**Verificado el 19 nov 2026 por el dueño en el teléfono** (con las reglas nuevas ya publicadas): panel de la directora con el conteo correcto · el docente publica un aviso al grupo · el tutor edita nombre y foto · incidentes del hijo y **Enterado** · "ver código" de vinculación del alumno. Los puntos 1–4 de la lista anterior (publicar reglas y probar 3.4, 3.5 y 3.6) quedan cerrados por esa misma pasada.

Queda pendiente:

1. Comprobar la cuenta real de la directora (hay una discrepancia sin resolver entre `directora@escuela.mx` y `directora@gmail.com` en la documentación).
2. Probar el flujo completo de la demo con las tres cuentas.
3. Probar en dispositivo el 3.10/3.9/3.7/3.8 cuando se cierren.

## 7. Datos de prueba

- Docente: `docente@aulasegura.com` / `123456`. Proyecto Firestore: `aulasegu`.
- Grupos demo: `grp-3b`, `grp-3a`, `grp-2c`, `grp-1a`.
- Horario: `groups/{grp-3b}/schedule/slot-1` con `weekday` (1 = lunes … 7 = domingo), `startTime "08:00"`, `endTime "08:45"`, `subject`, `teacherId`.
- Geocerca: `schools/{id}.location` con coordenadas reales y `radiusMeters` 20..5000 (usar 500–1000 para probar). `evaluateGeofence` exige además `accuracy <= radius`.
- Mapa de incidentes: necesita `MAPS_API_KEY` en `local.properties` (la misma del pase de lista).

## 8. Trampas conocidas

- `java.time` **no** está disponible (`minSdk` 24, sin desugaring).
- `google-services.json` está versionado a propósito, aunque figure en `.gitignore`.
- `scripts/notify/service-account*.json`, la llave de la service account y las credenciales de Cloudinary **no** van al repo.
- `notifications` es de solo lectura desde el cliente salvo el campo `read`; el `create` es del Admin SDK. Un push de la consola no crea documento en la bandeja.
- Firestore **no filtra** con las reglas: toda consulta de lista lleva el filtro exacto que exige la regla (`schoolId`, `groupId` + `active`, `userId`), o devuelve `PERMISSION_DENIED`.
- El repo no tiene `AGENTS.md`; las convenciones viven en `CONTEXT.md`, `FASE1.md`, `FASE3.md` y los logbooks de `odd/tasks/`.

## 9. Cómo seguir en el CLI

```bash
cd C:/Users/diego/AndroidStudioProjects/AulaSegura
git pull
freebuff
```

Primer mensaje sugerido:

> Lee `odd/tasks/handoff.md` y `FASE3.md`. Continúa con el bloque **3.10 (perfil del tutor)** con datos reales y foto, copiando el patrón de `TeacherProfileViewModel`, sin datos de ejemplo. Al cerrarlo, `assembleDebug` + `testDebugUnitTest` en verde, logbook en `odd/tasks/` y commit; no hagas push sin pedirlo.
