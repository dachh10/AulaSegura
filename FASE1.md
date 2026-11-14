# Fase 1 — Docente (Checklist de ejecución)

> Abrí este archivo en el editor dividido a la derecha (`clic derecho en la pestaña → Split Right`)
> y dejalo visible mientras trabajás. Tildá cada ítem al terminarlo, no antes.
>
> Referencia técnica: `CONTEXT.md`. Roadmap general: `PLAN.md` §Fase 1. Fase anterior: `FASE0.md`.

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso · `[!]` bloqueado

**Criterio de cierre de la fase** (`PLAN.md`): con **modo avión** activo, el docente pasa lista y
registra un incidente con foto; al reconectar, ambos aparecen en Firestore **sin duplicados** y
con la media subida.

---

## Punto de partida

La Fase 0 dejó la fundación. Esto ya existe y **no se vuelve a construir**:

| Ya está | Dónde |
|---|---|
| Room con 9 entidades, DAOs con `Flow`, `syncState` | `data/local/` |
| Outbox transaccional + worker único con backoff | `sync/` |
| Handler de asistencia (batch + contadores recalculados) | `sync/AttendanceSyncHandler.kt` |
| Subida a Cloudinary + compresión de imagen | `data/remote/cloudinary/` |
| Contenedor manual de dependencias | `di/AppContainer.kt` |
| Analytics tipado, Crashlytics, Remote Config validado | `domain/telemetry/`, `data/remote/` |
| Asistente con AI Logic (sin cablear a pantalla) | `data/remote/ai/` |
| 45 pantallas de la Etapa 1, **con datos de ejemplo** | `ui/screens/` |

Lo que **falta** y es el trabajo real de esta fase:

- **No hay ni un repositorio de Firestore.** `domain/repository/` solo tiene `AuthRepository` y
  `AiAssistant`. Todo lo demás es UI sobre listas hardcodeadas.
- `firebase-database` y `play-services-location` están en el catálogo pero **no** en
  `app/build.gradle.kts`.
- **No hay reglas de RTDB** ni sección `database` en `firebase.json`. Hoy el chat no tiene
  contrato de seguridad.

### Antes del bloque 1

- [ ] **P0.1** Revocar la key de la service account del seed (o dejar anotado por qué sigue viva)
- [ ] **P0.2** Confirmar en consola `allowed_formats = jpg,png,webp` del preset `docenteaula`
- [x] **P0.3** Rama de trabajo: `feat/docente-pase-lista` (nadie pushea a `main`, ver `CONTEXT.md` §10)

> **Regla de convivencia:** si una tarea toca el **esquema de Firestore**, se avisa antes de
> mergear. `domain/model` y `data/` los toca una sola persona a la vez (`CONTEXT.md` §10).

---

## Bloque 1 — Datos del docente (PLAN 1.1)

- [x] **1.1** Modelos de dominio: `Group`, `Student` (no reusar las entidades de Room en la UI)
- [x] **1.2** `GroupRepository` / `StudentRepository` en `domain/repository/`
- [x] **1.3** Implementación Firestore en `data/repository/` con las consultas que las reglas exigen
- [x] **1.4** Estrategia **cache-first**: leer de Room con `Flow`, refrescar de Firestore a Room en segundo plano
- [~] **1.5** `TeacherHomeScreen` y `TeacherGroupsScreen` cableadas a datos reales
- [x] **1.6** Mappers Firestore ↔ Room ↔ dominio en un solo lugar (`data/mapper/`)

> **Estado de 1.5:** *Mis grupos* ya lee de Room y refresca de Firestore (`TeacherGroupsViewModel`).
> El **roster** ya existe en `StudentRepository.observeRoster()`, pero todavía no hay pantalla que lo
> pinte: `onVerAlumnos` sigue cayendo en `PlaceholderScreen` y la lista real llega con **2.8**
> (`RollCallScreen`). Los contadores del inicio son de asistencia y también son del bloque 2.
> No se tildó para no declarar media entrega como entrega.
>
> **Hallazgo del bloque 1 (corregido acá):** con modo avión y arranque en frío la app volvía al login.
> `restoreSession()` resolvía el perfil leyendo `users/{uid}` en Firestore, y con la caché persistente
> de Firestore desactivada esa lectura no existe sin red. Se agregó `profile` a Room (versión 2,
> `MIGRATION_1_2`) como caché del perfil: si Firestore no responde, se usa el último perfil válido.
> La regla de fondo: **el arranque offline no puede depender de la red**, o el criterio de la fase
> ("modo avión, cerrar y abrir la app") es imposible de cumplir.
>
> **Segundo hallazgo, más peligroso:** sin red, un `get()` de Firestore puede resolverse como
> **snapshot vacío** en lugar de fallar. `refreshRoster` habría concluido que todos los alumnos se
> fueron del grupo y **borrado el roster cacheado** — justo en el escenario para el que existe la caché.
> Las dos consultas piden `Source.SERVER`: o el servidor contesta de verdad, o falla y la caché no se
> toca. Con eso además aparece el aviso de "sin conexión" que antes nunca llegaba, porque el refresco
> quedaba esperando sin límite (`withTimeout` de 12 s).

**Criterio:** el docente abre la app y ve sus 3 grupos con los conteos reales (28 / 30 / 27); en
modo avión, después de una carga online, sigue viendo lo mismo.

**Cómo probarlo**

1. Login con `docente@aulasegura.com` → *Mis grupos* muestra 3° B, 3° A, 2° C con los conteos de la semilla.
2. Abrir 3° B → 28 alumnos, en orden de número de lista, con los nombres sembrados.
3. Activar modo avión, cerrar y abrir la app → los grupos y el roster siguen ahí.

> **Trampa que ya nos costó una tarde en la Fase 0:** Firestore **no filtra por reglas**. Toda
> consulta de lista tiene que llevar exactamente el filtro que la regla exige. Para alumnos:
> `whereEqualTo("groupId", …)` **y** `whereEqualTo("active", true)` (el índice existe). Sin eso,
> `PERMISSION_DENIED` aunque el documento sea legible.
>
> **Y ojo con el `groupIds` denormalizado:** el docente tiene `groupIds = [grp-3b, grp-3a, grp-2c]`.
> `grp-1a` existe en la semilla justamente para probar que **no** puede leerlo.

**Evidencia (CPH2669, Android 14, `assembleDebug` + `testDebugUnitTest` verdes)**

| Check | Resultado observado |
|---|---|
| *Mis grupos* online | 3° B **28** · 3° A **30** · 2° C **27**, materia "Matemáticas" leída del perfil real |
| Orden | por grado y sección: 2° C → 3° A → 3° B |
| Nada inventado | sin anillo de asistencia, sin píldora de "lista hecha", sin horario: esos datos no existen todavía y la tarjeta los oculta |
| Modo avión + arranque en frío | abre en **Inicio** con el perfil cacheado (antes volvía al login) |
| *Mis grupos* en modo avión | los mismos 3 grupos y conteos, más el aviso "Sin conexión. Estás viendo los grupos guardados." con botón **Reintentar** |
| Roster cacheado | intacto después de refrescos sin red |
| Tests | 78 unitarios, 0 fallos (Fase 0 cerró con 61) |

---

## Bloque 2 - Pase de lista online (PLAN 1.2)

- [ ] **2.1** `AttendanceRepository` + `StartRollCallUseCase` / `SaveRollCallUseCase`
- [ ] **2.2** `sessionId` = **UUID generado en el cliente** (nunca `add()`, nunca un id del servidor)
- [ ] **2.3** Escritura de la sesión: `groupId`, `schoolId`, `teacherId`, `subject`, `date`, `status`, `counts`, `location`
- [ ] **2.4** Registros en `attendanceSessions/{id}/records/{studentId}` — **el id del documento es el del alumno**
- [ ] **2.5** Un solo `WriteBatch` para sesión + registros
- [ ] **2.6** `counts` se **recalcula desde los registros**, nunca desde los contadores de la UI
- [ ] **2.7** Cerrar sesión: `closedAt` + `status = "closed"`
- [ ] **2.8** `RollCallScreen`: borrar `sampleStudents()`, cablear al ViewModel
- [ ] **2.9** Eventos `roll_call_started` y `roll_call_saved` (`synced = 1`)

**Criterio:** el docente marca los 28, cierra, y en la consola hay **una** sesión con 28 registros
cuyos contadores coinciden con lo marcado.

**Cómo probarlo**

1. *Pase de lista* → marcar presente / falta / retardo / justificado en varios alumnos.
2. Cerrar la sesión.
3. En la consola: `attendanceSessions/{id}.counts` suma exactamente lo marcado y
   `records` tiene un documento por alumno.
4. Repetir el cierre (doble tap) → **no** aparece una segunda sesión.

> **Por qué el id lo genera el cliente:** es lo que hace idempotente al reintento. Un `add()` en
> Firestore crea un documento nuevo cada vez, y el primer reintento de la outbox duplicaría el
> pase de lista entero.
>
> **Las reglas exigen `teacherId == uid` y que el grupo esté en su `groupIds`.** Si la escritura
> falla, el error tiene que llegar a la UI con el `groupId` en el mensaje; tragárselo convierte un
> bug de permisos en "no guardó, no sé por qué".

---

## Bloque 3 — GPS y geocerca (PLAN 1.5)

- [ ] **3.1** Agregar `play-services-location` al módulo (ya está en `libs.versions.toml`)
- [ ] **3.2** Permiso `ACCESS_FINE_LOCATION` con racional y caso "permiso denegado" (no bloquea el pase de lista, lo deja **no verificado**)
- [ ] **3.3** `LocationProvider` con `FusedLocationProviderClient`: última ubicación conocida y después fix nuevo con timeout
- [ ] **3.4** Distancia con **haversine** contra `schools.location`, radio desde `schools.radiusMeters` (fallback validado: `default_radius_meters`)
- [ ] **3.5** Umbral de precisión: un fix con `accuracy` mayor que el radio **no** puede declarar `verified = true`
- [ ] **3.6** `TeacherLocationVerifyingScreen`, `TeacherOutsideRadiusScreen` y `TeacherRollCallMapScreen` con coordenadas reales
- [ ] **3.7** Persistir `location { lat, lng, accuracy, distanceMeters, verified }` en la sesión

**Criterio:** dentro del radio la sesión se guarda con `verified = true`; fuera del radio la app
muestra la pantalla de "fuera de la escuela" y **no** deja cerrar una sesión verificada.

**Cómo probarlo**

1. En el plantel (o con una ubicación simulada dentro de 150 m) → *Verificando ubicación* → pasa al pase de lista.
2. Simular ubicación a ~2 km → pantalla de fuera de radio, sin opción de guardar como verificada.
3. Revisar en Firestore `location.distanceMeters` y `location.verified` de la sesión guardada.

> **Precisión no es distancia.** Un fix a 2 km con 500 m de error no está "casi adentro": la
> distancia se calcula al centro del plantel y la precisión se guarda aparte. El campo `verified`
> tiene que ser honesto, porque de él va a depender la asistencia del tutor en la Fase 3.
>
> **El GPS funciona sin datos.** No lo metas en la outbox: la ubicación se toma al abrir la
> sesión, no cuando hay red.

---

## Bloque 4 — Pase de lista offline (PLAN 1.4)

- [ ] **4.1** Reusar `OutboxRepository` (ya encola sesión + registros + outbox en una transacción)
- [ ] **4.2** Con red caída: escribir en Room con `syncState = PENDING` y programar `SyncScheduler`
- [ ] **4.3** `TeacherRollCallOfflineScreen` refleja el estado real (`PENDING` / `SYNCING` / `SYNCED` / `FAILED`), no un texto fijo
- [ ] **4.4** Verificar que el handler sube la **misma** sesión y no crea otra
- [ ] **4.5** Una fila envenenada no bloquea la cola (ya cubierto en Fase 0: `MAX_ATTEMPTS_BEFORE_DROP`)
- [ ] **4.6** Prueba de ida y vuelta completa: avión → guardar → reconectar → outbox vacía

**Criterio:** con modo avión el pase de lista se guarda igual; al reconectar hay **exactamente una**
sesión en Firestore, con 28 registros, y la outbox queda vacía.

**Cómo probarlo**

1. Modo avión → hacer el pase de lista completo → cerrar la sesión → la UI dice "pendiente de sincronizar".
2. Revisar en Room que la sesión está `PENDING` y hay una fila en `outbox`.
3. Quitar modo avión → esperar el worker (o forzarlo) → `SYNCED` y outbox vacía.
4. En la consola: contar sesiones de ese día → **una**.
5. Repetir el paso 3 dos veces (forzar el worker de nuevo) → sigue habiendo una.

> **La unidad de idempotencia es el UUID.** Si alguna vez aparece un `add()` en el camino de
> asistencia, el modo avión empieza a duplicar sesiones y el tutor va a ver dos asistencias del
> mismo día.

---

## Bloque 5 — Historial (PLAN 1.9)

- [ ] **5.1** Consulta `attendanceSessions` con `teacherId == uid` + `date` desc (índice ya publicado)
- [ ] **5.2** Lectura offline desde Room (las sesiones guardadas se leen sin red)
- [ ] **5.3** `TeacherHistoryScreen` con sesiones reales agrupadas por fecha
- [ ] **5.4** Detalle: abrir una sesión y ver sus registros por alumno

**Criterio:** después de N pases de lista, el historial los lista en orden y abre el detalle de
cualquiera, incluso sin red.

---

## Bloque 6 — Incidentes (PLAN 1.6)

- [ ] **6.1** `IncidentRepository` + `CreateIncidentUseCase`
- [ ] **6.2** Borrador en Room (`IncidentDraftEntity`) con `syncState` y `opType = UPSERT_INCIDENT`
- [ ] **6.3** Handler `IncidentSyncHandler` y registrarlo en `syncHandlers` del contenedor
- [ ] **6.4** Severidad (`leve` / `moderado` / `grave`) y categoría según `CONTEXT.md` §6.2
- [ ] **6.5** Denormalizar `studentName` y `reportedByName`: el tutor lee el incidente sin poder leer el padrón
- [ ] **6.6** `TeacherIncidentNewScreen` cableada; evento `incident_created` (`severity`, `media_count`)

**Criterio:** un incidente creado en modo avión aparece **una vez** en Firestore al reconectar, con
su severidad, su nombre de alumno y su ubicación.

> **`studentName` no es redundancia, es contrato.** Las reglas dejan al tutor leer sus incidentes
> pero no el padrón completo; si el nombre no viaja denormalizado, la pantalla del tutor tiene que
> hacer una lectura que le van a denegar.

---

## Bloque 7 — Media del incidente (PLAN 1.7)

- [ ] **7.1** Adjuntar hasta `incident_media_max` archivos (Remote Config ya validado, default 3)
- [ ] **7.2** Imagen: `ImageCompressor` existente antes de subir
- [ ] **7.3** Subida con el uploader de Cloudinary existente, carpeta `aulasegura/incidents/{schoolId}`
- [ ] **7.4** Progreso **por archivo** (`uploadState`: `PENDING` → `UPLOADING` → `UPLOADED` / `FAILED`)
- [ ] **7.5** Reintento individual: un archivo que falla no pierde el incidente ni los otros adjuntos
- [ ] **7.6** El documento del incidente se escribe **después** de subir, con el array `media` completo

**Criterio:** un incidente con foto hecho en modo avión termina en Firestore con la URL de
Cloudinary, sin perder la foto ni duplicar el incidente.

> **Decisión tomada: video → Fase 4.** Fase 1 entrega **solo imágenes**. `ImageCompressor` procesa
> imágenes y no hay transcodificación de video en el dispositivo; subir video sin re-encode obliga a
> un tope de peso y a un costo que no corresponde pagar en esta fase.
>
> Límites del plan Free de Cloudinary (verificados, para cuando llegue la Fase 4):
> - **25 créditos/mes**, sin tarjeta y sin límite de tiempo.
> - **100 MB máximo por archivo de video.**
> - 1 crédito = 1 GB almacenado · 1 GB de ancho de banda · 500 s de procesamiento SD · 250 s en HD.
> - Incluye conversión automática de formato, ajuste de calidad y entrega por CDN.
>
> **Consecuencia práctica:** un video de 100 MB consume ~1 crédito de los 25 solo por existir, y el
> bandwidth de un aula mirándolo se come el resto. En Fase 4 el video va con compresión previa y
> límite de duración, no "subir lo que el teléfono grabe".
>
> **Orden que no se puede invertir:** subir primero y escribir después. Al revés, el incidente queda
> en Firestore con `media` vacío y el tutor ve un incidente sin evidencia.

---

## Bloque 8 — Tareas y avisos (PLAN 1.8)

- [ ] **8.1** `TaskRepository.publish`: `groupIds` (array), `dueDate`, `createdBy`, `active`
- [ ] **8.2** `NoticeRepository.publish`: `audience.roles`, `audience.groupIds`, `urgent`
- [ ] **8.3** Caché de lectura en Room (`TaskEntity`, `NoticeEntity`) desde el primer momento
- [ ] **8.4** `TeacherTaskNewScreen` y `TeacherNoticeNewScreen` cableadas a grupos reales
- [ ] **8.5** Eventos `task_published` y `notice_published`

**Criterio:** una tarea publicada para 3° B y un aviso urgente para los tutores de 3° B aparecen en
Firestore con la audiencia correcta y se leen sin red después de la primera carga.

---

## Bloque 9 — Chat (PLAN 1.10)

- [ ] **9.1** Agregar `firebase-database` al módulo (ya está en el catálogo)
- [ ] **9.2** `docs/rtdb.rules` con el contrato de conversaciones + `database` en `firebase.json` + deploy
- [ ] **9.3** Esquema RTDB: `messages/{conversationId}/{messageId}` (`senderId`, `text`, `sentAt`) y presencia
- [ ] **9.4** `conversations/{id}` en Firestore como **metadatos**: `participants`, `lastMessage`, `lastMessageAt`, `unread`
- [ ] **9.5** `ChatRepository` con `Flow` de mensajes + envío optimista
- [ ] **9.6** `TeacherChatScreen` y `TeacherConversationScreen` reales; evento `chat_message_sent`

**Criterio:** dos cuentas intercambian mensajes en tiempo real y la lista de chats muestra el último
mensaje y la hora, sin recargar la pantalla.

> **Hoy no hay reglas de RTDB.** Sin `rtdb.rules`, la base está en el modo por defecto del proyecto y
> cualquiera con la config puede leer todo. Este bloque no está terminado hasta que las reglas estén
> publicadas, no solo escritas en `docs/`.
>
> **Dos almacenes, un solo mensaje:** el mensaje vive en RTDB; Firestore solo guarda el resumen para
> pintar la lista. Si se escriben los dos y uno falla, la lista miente. El resumen se actualiza
> **después** de que RTDB acepta el mensaje.

---

## Bloque 10 — Notificaciones y FCM (PLAN 1.11)

- [ ] **10.1** `FirebaseMessagingService`: token en `users/{uid}/fcmTokens/{tokenId}` (`token`, `platform`, `updatedAt`)
- [ ] **10.2** Rotación de token (`onNewToken`) y borrado del token al cerrar sesión
- [ ] **10.3** Canal de notificación + permiso `POST_NOTIFICATIONS` en Android 13+
- [ ] **10.4** Apertura por `deepLink` (el campo `notifications.deepLink` ya está en el esquema)
- [ ] **10.5** `TeacherNotificationsScreen` con la colección `notifications` real (`userId`, `read`)
- [ ] **10.6** Escritura de `fcmTokens` validada contra las reglas

**Criterio:** desde la consola de Firebase se envía una notificación al token del dispositivo, llega,
y al tocarla abre la pantalla correcta. Marcar como leída la quita del contador.

**Cómo probarlo**

1. Copiar el token con `adb logcat` (o desde la pantalla de perfil en debug).
2. Firebase Console → Messaging → *Send test message* → pegar el token → enviar.
3. Tocar la notificación → abre la pantalla del `deepLink`.
4. Login/Logout → el token desaparece de `users/{uid}/fcmTokens` al salir.

> **Esto es Spark: no hay envío automático.** En plan gratuito no hay Cloud Functions, así que un
> push "cuando pasa X" **no existe** todavía. En esta fase el envío es manual desde la consola y el
> documento en `notifications` se escribe por script o consola (las reglas lo reservan al Admin SDK).
> No diseñes un flujo que dependa de push automático: no va a sonar.

---

## Bloque 11 — Perfil (PLAN 1.12)

- [ ] **11.1** `TeacherProfileScreen` con datos reales (`fullName`, `email`, `schoolId`, `subjects`)
- [ ] **11.2** Saludo del `TeacherHomeScreen` desde `fullName` (cierra el pendiente de la Fase 0)
- [ ] **11.3** Foto: comprimir + subir a `aulasegura/profiles/{uid}` + escribir `photoUrl` y `photoPublicId`
- [ ] **11.4** Foto visible en la UI y persistida entre sesiones (Coil)
- [ ] **11.5** Logout y cierre forzado por `active = false` siguen funcionando

**Criterio:** el docente cambia su foto, cierra la app, vuelve a entrar y la foto sigue ahí; el
saludo muestra su nombre real.

---

## Cierre de la Fase 1

- [ ] **C1** Modo avión: pase de lista completo **y** incidente con foto
- [ ] **C2** Reconectar: ambos en Firestore, **sin duplicados**, media subida
- [ ] **C3** `assembleDebug` verde
- [ ] **C4** `testDebugUnitTest` verde (sin bajar el número de la Fase 0: eran 61)
- [ ] **C5** `connectedDebugAndroidTest` verde en device físico
- [ ] **C6** Reglas de Firestore y de RTDB publicadas
- [ ] **C7** `FASE1.md` completo y `CONTEXT.md` actualizado con lo que cambió de esquema

### Evidencia mínima esperada

| Check | Cómo se demuestra |
|---|---|
| Offline real | Logcat del `SyncWorker` + outbox vacía + consola con **una** sesión y **un** incidente |
| Sin duplicados | Repetir el drenaje dos veces y volver a contar documentos |
| Media | `incident.media[0].url` abre en el navegador |
| Geocerca | `location.verified` / `distanceMeters` de una sesión adentro y otra afuera |
| Chat | Dos cuentas, mensaje en ambas pantallas sin recargar |
| Push | Notificación desde consola + apertura por `deepLink` |

---

## Riesgos anotados

1. **El esquema es contrato compartido.** Cada colección nueva o campo nuevo se avisa antes de
   mergear, porque Evelyn escribe las reglas y la Fase 2/3 leen los mismos documentos.
2. **`data/` lo toca una sola persona a la vez.** El pase de lista y los incidentes comparten
   `OutboxRepository` y `SyncRunner`: dos ramas tocándolos en paralelo es un conflicto garantizado.
3. **La outbox no puede crecer sin control.** Todo lo que se guarda offline tiene que tener handler
   registrado; una fila sin handler se reporta y espera, no se finge que sincronizó.
4. **`main` siempre compila.** Un rojo en `main` bloquea a la otra persona, y este proyecto ya tiene
   dos personas.
