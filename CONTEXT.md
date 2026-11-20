# AulaSegura — Contexto técnico del proyecto

> Documento de onboarding del equipo. Última actualización: 2026-11-09.
> Estado: etapa de diseño cerrada · etapa de lógica de negocio por comenzar.
> Roadmap y reparto de tareas: `PLAN.md`.

---

## 1. Qué es AulaSegura

Aplicación Android para la gestión de asistencia, incidentes y comunicación de una escuela primaria. Tres roles:

| Rol | Uso principal |
|---|---|
| **Directivo** | Administra usuarios, grupos, alumnos, configuración del plantel y reportes. |
| **Docente** | Pasa lista con validación GPS, registra incidentes con evidencia, publica tareas y avisos. |
| **Tutor (padre/alumno)** | Consulta asistencia, tareas, avisos e incidentes de sus hijos; justifica faltas; chatea con el docente; usa un asistente con IA. |

El tutor representa a la familia. **No existe login propio de alumno** en esta versión: el alumno se consulta a través de la cuenta del tutor.

**Nombre del paquete:** `com.example.aulasegura`
**applicationId:** `com.example.aulasegura`
**minSdk:** 24 · **targetSdk / compileSdk:** 37
**Kotlin:** 2.2.10 · **AGP:** 9.5.1 · **Compose BOM:** 2026.02.01

---

## 2. Estado actual

### Etapa 1 — Diseño de pantallas ✅ cerrada

- **45 pantallas** implementadas en Compose, fieles al design system de Stitch (`projects/7677408035297541086`).
- Navegación completa en `navigation/Routes.kt` y `navigation/AulaSeguraNavHost.kt`.
- Tema M3 propio, tipografías Bricolage Grotesque / DM Sans / JetBrains Mono.
- Patrón de escudos global (`ShieldPattern`) y animación de entrada por bloques (`Reveal` / `Modifier.revealOnEnter`) en las pantallas de los tres roles.
- **Todos los datos están hardcodeados** en cada archivo de pantalla (`private val sample…`, `private data class …`).

### Etapa 2 — Lógica de negocio ✅ completada (registro histórico)

> Las líneas siguientes describen el punto de partida de la Fase 0 y **ya no son válidas**:
> Firebase, Room, Cloudinary y Coil están integrados (ver §6 y §8). Se conservan como contexto.
> El estado actual del proyecto es Fase 3 (tutor).

Estado original:

Nada de Firebase, Room ni Cloudinary está integrado todavía:

- No hay proyecto Firebase ni `google-services.json`.
- No hay dependencias de Firebase, Room, WorkManager, Hilt ni Coil en `app/build.gradle.kts`.
- El login es una comparación de strings dentro de `AulaSeguraNavHost`.
- Los modelos de datos viven como `private data class` dentro de cada pantalla y **no se comparten**.

Ese último punto es el trabajo más grande de la etapa 2: extraer esos modelos a `domain/model` y reemplazar los datos de ejemplo por repositorios reales.

---

## 3. Stack

### Actual

| Área | Librería |
|---|---|
| UI | Jetpack Compose + Material 3 + Material Icons Extended |
| Navegación | Navigation Compose 2.10.2 |
| Mapas | Google Maps Compose 6.1.2 + Play Services Maps/Location |
| Async | Coroutines (sin Flow estructurado todavía) |

### A agregar en la Fase 0

| Área | Librería | Para qué |
|---|---|---|
| Backend | Firebase BOM | Auth, Firestore, RTDB, FCM, Analytics, Crashlytics, Remote Config, App Check |
| Auth | `firebase-auth` | Login real y sesión persistente |
| Datos | `firebase-firestore-ktx` | Base de datos principal |
| Chat | `firebase-database-ktx` | Mensajes en tiempo real |
| Push | `firebase-messaging` | Notificaciones |
| Local | Room (KSP) | Caché y trabajo offline |
| Sync | WorkManager | Cola de sincronización |
| DI | Hilt (KSP) | Inyección de repositorios |
| Imágenes | Coil 3 | Cargar fotos desde Cloudinary |
| HTTP | OkHttp + `kotlinx-serialization` | Subida a Cloudinary |
| Coroutines | `kotlinx-coroutines-play-services` | `await()` sobre `Task` de Firebase |
| IA | `firebase-ai` (Firebase AI Logic) | Asistente del tutor con modelos Gemini |
| Testing | Firebase Test Lab, JUnit, Compose UI Test | Pruebas |

> **AI Logic usa el proveedor Gemini Developer API** (el "Gemini API" clásico), no Vertex AI ni el Agent Platform. Es el único que tiene free tier sin vincular facturación. El SDK on-device **no** se usa: exige API 26 y el proyecto tiene `minSdk 24`.

> **Verificar compatibilidad de KSP con Kotlin 2.2.10 / AGP 9.5.1 antes de agregar Room y Hilt.** Si KSP no está estable para esa combinación, la alternativa es DI manual con un `AppContainer` y Room sin procesador (no aplica: Room necesita KSP/KAPT).

---

## 4. Arquitectura objetivo

### Capas y regla de dependencia

```
ui/          Compose + ViewModel + UiState
   ↓ depende de
domain/      modelos, interfaces de repositorio, casos de uso
   ↑ implementado por
data/        repositorios, Room, Firebase, Cloudinary
   ↓
sync/        outbox + WorkManager
```

- La UI **nunca** importa `com.google.firebase.*` ni Room.
- Cada pantalla tiene su `ViewModel` con `StateFlow<UiState>`.
- `UiState` mínimo: `data`, `loading`, `error`, `isEmpty`.

### Estructura de paquetes propuesta

```
com.example.aulasegura/
  core/                 Result, Dispatchers, extensiones, formateo de fechas
  domain/
    model/              User, Student, Group, AttendanceSession, Incident, …
    repository/         interfaces (AuthRepository, GroupRepository, …)
    usecase/            casos de uso con reglas de negocio
  data/
    local/              Room: AulaSeguraDatabase, DAOs, entidades
    remote/
      firebase/         Auth, Firestore, RTDB, FCM
      cloudinary/       cliente de subida
    repository/         implementaciones
    mapper/             entity ↔ domain ↔ dto
  sync/                 OutboxEntity, SyncWorker, SyncScheduler
  di/                   módulos de Hilt
  ui/
    screens/{access,teacher,admin,parent}
    components/
    theme/
  navigation/
```

La migración es incremental: los archivos de pantalla actuales se quedan donde están; solo se les quita el `sample…` cuando su rol entra en implementación.

### Navegación por rol

`Routes` ya está separado por rol. En la Fase 0 se agrega:

1. `SplashScreen` consulta `FirebaseAuth.currentUser`.
2. Si hay sesión, lee `users/{uid}.role` y navega al grafo del rol con `popUpTo(SPLASH) { inclusive = true }`.
3. Si no hay sesión, va a `LOGIN`.
4. Un rol desconocido o un usuario `active = false` cierra sesión y vuelve a `LOGIN`.

**Regla de seguridad:** el rol de la UI es solo presentación. Las reglas de Firestore vuelven a validar el rol contra `users/{uid}` en cada operación.

---

## 5. Reglas no negociables

1. **Cloudinary, nunca Firebase Storage.** No agregar `firebase-storage`, no activar Storage en la consola. Toda imagen o video (perfil, incidentes, justificantes, adjuntos) va a Cloudinary —**plan free**— y en Firestore solo se guardan metadatos y URLs.
2. **Room es la fuente local de verdad** para pase de lista, incidentes y adjuntos. La caché persistente de Firestore se **desactiva** (`setPersistenceEnabled(false)`) para no tener dos fuentes de verdad compitiendo.
3. **Ningún secreto en el repo.** `google-services.json` y `local.properties` van en `.gitignore`. Las credenciales de Cloudinary no se embeben en el APK.
4. **IDs generados por el cliente (UUID) en documentos creados offline.** Así un reintento sobrescribe el mismo documento en vez de duplicarlo.
5. **Fechas:** `Timestamp` en Firestore, `Long` (epoch millis) en Room, formato `es-MX` solo en la capa de UI.
6. **Idioma:** identificadores y código en inglés; textos de UI en español; documentación en español neutro.
7. **Un evento de Analytics por acción de negocio relevante** (ver §9), no por cada tap.
8. **Todo fallo de sincronización se registra como non-fatal en Crashlytics** con el `opType` y el número de intento.
9. **Todo el proyecto corre en planes gratuitos.** Firebase se queda en **Spark** y Cloudinary en **free**. No se vincula facturación: hacerlo mueve el proveedor Gemini Developer API a tier pago y rompe el free tier de AI Logic. Consecuencia: **no hay Cloud Functions**. El alta de cuentas se hace con un script local que usa el Admin SDK y un service account, y los push se envían desde la consola de Firebase.

---

## 6. Modelo de datos — Firestore

### 6.1 Árbol de colecciones

```
users/{uid}
  fcmTokens/{tokenId}
  noticeReads/{noticeId}

students/{studentId}

groups/{groupId}
  schedule/{slotId}

schools/{schoolId}

attendanceSessions/{sessionId}
  records/{studentId}

incidents/{incidentId}

tasks/{taskId}

notices/{noticeId}

justifications/{justificationId}

conversations/{conversationId}      ← solo índice; los mensajes viven en RTDB

notifications/{notificationId}

linkingCodes/{code}
```

Criterio general: **colecciones raíz con `schoolId` y campos de consulta**, no anidar todo bajo `schools/{id}`. Motivos: los índices compuestos son más simples, un alumno puede cambiar de grupo sin mover documentos, y las consultas del tutor cruzan grupos.

### 6.2 Detalle por colección

#### `users/{uid}`
```
uid              string   (= id del documento)
email            string
role             string   "docente" | "directivo" | "tutor"
firstName        string
lastName         string
fullName         string   denormalizado, para listas y chat
phone            string?
photoUrl         string?
photoPublicId    string?
schoolId         string
active           bool
subjects         string[]          solo docente
groupIds         string[]          solo docente (grupos que imparte)
studentIds       string[]          solo tutor (hijos vinculados)
preferences      map { notificationsEnabled: bool }
createdAt        timestamp
updatedAt        timestamp
lastLoginAt      timestamp
```

`photoPublicId` es la ruta completa del asset en Cloudinary (`aulasegura/profiles/{uid}`), no un
`public_id` suelto: el preset de subida no prefija la carpeta. La Fase 1 (bloque 11) lo escribe
junto a `photoUrl` con un `update()` de dos campos. La caché Room del perfil guarda solo `photoUrl`,
porque mostrar la foto no necesita el id y una foto nueva genera uno nuevo: eso evita una migración
de esquema para un campo que solo se usa al reemplazar la imagen.

`preferences.notificationsEnabled` existe en el esquema pero **ningún consumidor lo lee**: en Spark
no hay Cloud Functions y el push se envía desde la consola o desde `scripts/notify`. La app no lo
escribe; la pantalla de notificaciones consulta el permiso del sistema operativo, que es lo que
realmente decide si el teléfono suena.

#### `students/{studentId}`
```
firstName, lastName, fullName   string
curp                            string?
listNumber                      int
groupId                         string
schoolId                        string
tutorIds                        string[]   vínculo tutor ↔ alumno
photoUrl, photoPublicId         string?
active                          bool
createdAt, updatedAt            timestamp
```

#### `groups/{groupId}`
```
name                string   "3° B"
grade               int      1..6
section             string   "A" | "B" | "C"
schoolId            string
titularTeacherId    string?
teacherIds          string[]
studentCount        int      denormalizado
cycle               string   "2026-2027"
createdAt, updatedAt timestamp
```
Subcolección `schedule/{slotId}`:
```
weekday    int     1 = lunes … 7 = domingo
startTime  string  "08:00"
endTime    string  "08:45"
subject    string
teacherId  string?
```

#### `schools/{schoolId}`
```
name            string
key             string   clave del plantel, "15EPR0123X"
address         string
cycle           string
location        map { lat: double, lng: double }
radiusMeters    int      geocerca del pase de lista
schedule        map { entryTime: "08:00", exitTime: "14:00" }
requireLocation bool
createdAt, updatedAt timestamp
```
Es un solo documento hoy, pero se modela como colección para soportar varias escuelas.

#### `attendanceSessions/{sessionId}`
```
sessionId (id)   string   UUID generado por el cliente
groupId          string
schoolId         string
teacherId        string
subject          string
date             string   "2026-11-03"  (para consultas por rango)
startedAt        timestamp
closedAt         timestamp?
status           string   "open" | "closed"
counts           map { present, absent, late, justified: int }
location         map { lat, lng, accuracy, distanceMeters, verified: bool }
createdAt, updatedAt timestamp
```
La sesión **nace cerrada**: el docente la cierra en el teléfono y se escribe una sola vez con
`set()` sobre el UUID del cliente, con `status = "closed"` y `closedAt`. `open` existe en el
esquema para un flujo futuro de sesión en vivo, no para el de hoy.
Subcolección `records/{studentId}` (el id del documento es el del alumno):
```
studentId    string
listNumber   int
status       string   "present" | "absent" | "late" | "justified"
time         string?  "08:12"
note         string?
updatedAt    timestamp
updatedBy    string
```

#### `incidents/{incidentId}`
```
incidentId (id)   string   UUID del cliente si se creó offline
schoolId, groupId string
studentId         string?
studentName       string   denormalizado
title             string
description       string
severity          string   "leve" | "moderado" | "grave"
category          string   "discusion" | "caida" | "danio" | "desacato" | "otro"
reportedBy        string
reportedByName    string
occurredAt        timestamp
location          map { lat, lng, accuracy, distanceMeters, verified }
media             array de map:
                    { id, type: "image"|"video", publicId, url, thumbnailUrl,
                      format, bytes, width, height, durationSec }
status            string   "nuevo" | "revisado" | "cerrado"
seenByTutorIds    string[]
createdAt, updatedAt timestamp
```
`media` va embebido porque el formulario limita a 3 evidencias y así el incidente se lee con un solo `get()`. Si en el futuro se permiten más, se migra a subcolección `media/{mediaId}`.

#### `tasks/{taskId}`
```
schoolId        string
groupIds        string[]
subject         string
title           string
description     string
dueDate         timestamp
attachments     array de map { name, url, publicId, type }
createdBy       string
createdByName   string
createdAt, updatedAt timestamp
active          bool
```

#### `notices/{noticeId}`
```
schoolId     string
audience     map {
               roles: string[]        "tutor" | "docente"
               groupIds: string[]
               studentIds: string[]
             }
title        string
body         string
category     string   "escuela" | "grupo" | "urgente"
urgent       bool
attachments  array de map { name, url, publicId, type }
createdBy    string
createdByName string
createdAt    timestamp
expiresAt    timestamp?
```

#### `justifications/{justificationId}`
```
studentId         string
tutorId           string
schoolId, groupId string
dateFrom, dateTo  timestamp
reason            string   "enfermedad" | "cita" | "viaje" | "otro"
description       string
attachment        map { publicId, url, type }
status            string   "pendiente" | "aceptada" | "rechazada"
reviewedBy        string?
reviewedAt        timestamp?
reviewNote        string?
createdAt         timestamp
```

#### `conversations/{conversationId}`
Solo metadatos para pintar la lista de chats. Los mensajes están en Realtime Database.
```
participants       string[]   [uid1, uid2]
participantNames   map { uid: fullName }
participantRoles   map { uid: role }
studentId          string?    contexto del chat (hijo)
lastMessage        string
lastMessageAt      timestamp
lastSenderId       string
unread             map { uid: int }
createdAt          timestamp
```

#### `notifications/{notificationId}`
Un documento **por destinatario**.
```
userId     string
title      string
body       string
type       string   "asistencia" | "incidente" | "tarea" | "aviso" | "mensaje" | "sistema"
deepLink   string?  ruta interna a abrir
data       map
read       bool
createdAt  timestamp
```

#### `linkingCodes/{code}`
```
code        string   (= id del documento, 6 caracteres)
studentId   string
schoolId    string
createdBy   string
createdAt   timestamp
expiresAt   timestamp
usedBy      string?
usedAt      timestamp?
```
Es el respaldo del "código de vinculación" que ya muestra `AdminNewStudentScreen`.

#### `users/{uid}/fcmTokens/{tokenId}`
```
token       string
platform    string   "android"
updatedAt   timestamp
```
Subcolección y no array para que dos dispositivos no se pisen al escribir.

#### `users/{uid}/noticeReads/{noticeId}`
```
readAt timestamp
```
El id del documento es el del aviso, así el "marcar leído" es idempotente.

### 6.3 Índices compuestos necesarios

| Colección | Campos |
|---|---|
| `students` | `groupId` + `active` |
| `students` | `tutorIds` (array-contains) + `active` |
| `students` | `schoolId` + `groupId` + `active` |
| `incidents` | `schoolId` + `createdAt` desc |
| `incidents` | `groupId` + `createdAt` desc |
| `incidents` | `studentId` + `createdAt` desc |
| `attendanceSessions` | `groupId` + `date` desc |
| `attendanceSessions` | `teacherId` + `date` desc |
| `attendanceSessions` | `schoolId` + `date` desc |
| `attendanceSessions` | `schoolId` + `date` asc — consultas por rango sin `orderBy` |
| `tasks` | `groupIds` (array-contains) + `dueDate` asc |
| `notices` | `audience.groupIds` (array-contains) + `createdAt` desc |
| `notifications` | `userId` + `createdAt` desc |
| `notifications` | `userId` + `read` + `createdAt` desc |
| `conversations` | `participants` (array-contains) + `lastMessageAt` desc |
| `justifications` | `groupId` + `status` + `createdAt` desc |

Los índices se versionan en `docs/firestore.indexes.json`.

### 6.4 Reglas de seguridad — esquema

```
users/{uid}            read: el propio usuario | directivo del mismo schoolId |
                             tutor de un grupo del docente (solo docentes del mismo plantel)
                       write: el propio usuario SOLO campos de perfil
                              (email, role, active y schoolId son inmutables)
students/{id}          read: directivo | docente de un grupo del alumno | tutor en tutorIds
                       write: directivo
groups/{id}            read: todo el schoolId
                       write: directivo
schools/{id}           read: todo el schoolId
                       write: solo directivo (o el script de administración)
attendanceSessions     read: directivo | docente dueño | tutor de un alumno del grupo
                       create: docente dueño, dentro de su groupIds, status open|closed
                       write: docente dueño y dentro de su groupIds
records/{studentId}    write: docente dueño de la sesión (getAfter del padre: se crea en el mismo batch)
incidents              create: docente del grupo
                       read: directivo | docente que lo creó | tutor del alumno
                       update: directivo (estado) | tutor (solo seenByTutorIds)
tasks / notices        read: audiencia | directivo
                       write: docente de los grupos | directivo
justifications         create: tutor del alumno
                       read: tutor dueño | directivo | docente del grupo
                       update: directivo
notifications          read: userId == auth.uid
                       create/update: solo Admin SDK (script local) o la consola
linkingCodes           create: directivo ; read: nadie por listado, solo get exacto por código
```

Toda regla que dependa del rol hace `get(/databases/$(database)/documents/users/$(request.auth.uid))`. Es una lectura extra por operación; se acepta por claridad y se optimiza con `custom claims` solo si aparece un problema de coste.

**En un batch se usa `getAfter()`, no `get()`, para leer el documento padre que el mismo batch está creando.** `get()` devuelve `null` en ese caso y la escritura se rechaza. El límite de 20 lecturas por batch se respeta porque Firestore cachea las lecturas al mismo documento dentro de un request: 28 registros que validan la misma sesión cuentan como una lectura.

---

## 7. Cloudinary

### 7.1 Carpetas

Prefijo raíz `aulasegura/`. **Una carpeta por tipo de recurso**, nunca todo mezclado:

| Carpeta | Contenido | Formato |
|---|---|---|
| `aulasegura/profiles/{uid}` | Foto de perfil de cualquier rol | imagen |
| `aulasegura/incidents/{incidentId}` | Fotos y videos de evidencia de un incidente | imagen + video |
| `aulasegura/justifications/{justificationId}` | Adjunto del justificante de falta | imagen/pdf |
| `aulasegura/tasks/{taskId}` | Adjuntos de tareas | imagen/pdf |
| `aulasegura/notices/{noticeId}` | Adjuntos de avisos | imagen/pdf |
| `aulasegura/schools/{schoolId}` | Logo del plantel | imagen |

Regla: **el `publicId` guardado en Firestore incluye la ruta completa** sin extensión (`aulasegura/incidents/abc123/img_01`). Con eso se puede borrar o transformar sin reconstruir rutas.

### 7.2 Presets y permisos

- **Un solo preset unsigned** (`docenteaula`) en la cuenta `dncnom7mu`, con
  *dynamic folders* activado y `use asset folder as public id prefix` apagado.
  Un preset es política de subida, no autorización: el nombre viaja en el APK, así
  que tener uno por rol no agregaría seguridad. La autorización real es Firestore
  Rules sobre el documento que referencia la imagen.
- **La carpeta viaja en `asset_folder`**, no en el preset: el cliente manda
  `aulasegura/<tipo>/<scope>` (ver §7.1) y `CloudinaryFolder` sanea el scope
  (un `/` o `..` no puede escapar del prefijo).
- **El cliente no manda `public_id`.** Cloudinary lo genera (preset con
  `use_filename: false`, `unique_filename: false`), así que dos archivos con el
  mismo nombre nunca colisionan y nadie enumera assets.
- **No hay límite de tamaño por preset**: Cloudinary no lo soporta. Se valida en
  el cliente (MIME, y compresión a ≤ 1080 px / JPEG q80 antes de subir).
- **Recomendado en la consola:** `allowed_formats = jpg,png,webp`. Es el único
  control que un cliente propio no puede saltear.
- **Nunca** embeber el `api_secret` en el APK. **Sin Cloud Functions** (ver §9.1)
  no hay firma en runtime: el proyecto usa preset unsigned restringido. Si la
  entrega exigiera subida firmada, habría que habilitar Blaze o firmar desde un
  script local fuera de la app.
- El preset unsigned es aceptable para el alcance del proyecto porque está
  restringido por formato, carpeta y tamaño en el cliente. Queda documentado como
  decisión.

**Límites del plan free de Cloudinary** (verificados en la consola, agosto 2026): **25 créditos por
mes**, sin tarjeta y sin límite de tiempo. **1 crédito = 1 GB almacenado** · **1 GB de ancho de
banda mensual** · **500 s de procesamiento de video SD** · **250 s en HD**. Además, **cada archivo
de video tiene un tope duro de 100 MB** en el plan free. Los videos consumen crédito mucho más
rápido que las imágenes: 100 MB de video ya son ~0.1 crédito solo por existir, y el bandwidth de un
aula mirándolo se come el resto. Mitigaciones obligatorias:

- Comprimir la imagen en el dispositivo antes de subir (`≤ 1080 px` de lado mayor, calidad ~80).
- Limitar el video a **15 segundos** y ≤ 20 MB antes de subir: muy por debajo del tope de 100 MB de
  Cloudinary, porque el tope de la plataforma no es el presupuesto del proyecto.
- Pintar listas con la transformación de miniatura, nunca con el original.
- Guardar `bytes` en Firestore y mostrar el consumo en el panel de administración (opcional).
- Borrar de Cloudinary la media de incidentes rechazados o de pruebas.
- No usar transformaciones `eager` masivas ni generación de videos.

### 7.3 Metadatos que se guardan en Firestore

Siempre los mismos campos: `publicId`, `url` (`secure_url`), `thumbnailUrl` (transformación derivada), `assetFolder`, `type`, `format`, `bytes`, `width`, `height` y `durationSec` en video. **Nunca** se guarda el archivo ni el base64.

`assetFolder` se guarda aparte porque el preset no prefija la carpeta en el `public_id`: sin ese campo, una limpieza futura tendría que adivinar dónde vive el archivo. El `thumbnailUrl` se arma insertando la transformación de §7.4 después de `/upload/`; la transformación depende de la carpeta (recorte de rostro para perfiles, límite de ancho para evidencia).

### 7.4 Transformaciones

| Uso | Transformación |
|---|---|
| Avatar en listas | `w_96,h_96,c_fill,g_face,q_auto,f_auto` |
| Perfil | `w_512,h_512,c_fill,g_face,q_auto,f_auto` |
| Miniatura de video | `so_0,w_400,h_300,c_fill,q_auto,f_auto` + `.jpg` |
| Evidencia en detalle | `w_1080,c_limit,q_auto,f_auto` |

Coil apunta a la URL transformada; no se descarga el original para pintar una lista.

---

## 8. Room y sincronización

### 8.1 Entidades

| Entidad | Rol |
|---|---|
| `GroupEntity` | Caché de grupos del docente y del directivo |
| `StudentEntity` | Caché de alumnos por grupo |
| `AttendanceSessionEntity` | Sesión de pase de lista, con `syncState` |
| `AttendanceRecordEntity` | Estado por alumno, con `syncState` |
| `IncidentDraftEntity` | Incidente creado sin red, con `syncState` |
| `IncidentMediaEntity` | Archivo local y estado de subida a Cloudinary |
| `NoticeEntity`, `TaskEntity` | Caché de lectura |
| `OutboxEntity` | Cola de operaciones pendientes |
| `ProfileEntity` | Último perfil válido de `users/{uid}`, para arrancar sin red (v2) |
| `SchoolEntity` | Plantel con su geocerca, para evaluar el radio sin red (v3; `requireLocation` desde v4) |

La base está en **`version = 4`** con `MIGRATION_1_2`, `MIGRATION_2_3` y `MIGRATION_3_4` (esta
última añade `requireLocation`, con default `true`). No hay `fallbackToDestructiveMigration`.
El perfil del docente no participa en el cambio: `photoPublicId` vive solo en Firestore.

`syncState`: `PENDING` → `SYNCING` → `SYNCED` | `FAILED`.
`uploadState` (media): `PENDING` → `UPLOADING` → `UPLOADED` | `FAILED`.

**`ProfileEntity` no es un lujo.** La sesión de Firebase Auth sobrevive sin red, pero el perfil vive en
Firestore y su caché persistente está apagada (§2, regla 2). Sin la copia local, `restoreSession()`
falla sin conexión y la app manda al login justo en el escenario para el que existe: el aula sin señal.
Se guarda **una sola fila**, que se reemplaza en cada resolución válida del perfil.

**`SchoolEntity` existe por el mismo motivo, un paso más adelante.** La geocerca se evalúa al abrir
el pase de lista, o sea en el peor momento para depender de la red. Se cachean solo los campos que
la regla necesita (`name`, `latitude`, `longitude`, `radiusMeters`); el resto del documento es
administrativo y no tiene consumidor en la ruta del docente. Si el radio falta o está fuera de
`20..5000` metros se usa `default_radius_meters` de Remote Config: un `radiusMeters = 0` guardado en
la consola dejaría a todo el plantel "fuera de la escuela".

**Offline gana la caché.** El refresh de `schools` es best effort: si falla, Room queda como está y
la geocerca se evalúa con el último centro y radio conocidos. Es lo que permite pasar lista en un
aula sin señal, con el compromiso de que un cambio de radio hecho en la consola no llega al
dispositivo hasta que vuelve la red.

### 8.2 Outbox

`OutboxEntity` guarda `id`, `opType`, `entityLocalId`, `payloadJson`, `attempts`, `lastError`, `createdAt`.
`opType`: `UPSERT_ATTENDANCE_SESSION`, `UPSERT_INCIDENT`, `UPLOAD_INCIDENT_MEDIA`, `UPSERT_JUSTIFICATION`.

### 8.3 Flujo offline

1. La UI escribe en Room de inmediato y marca `PENDING`; encola el `OutboxEntity`.
2. `SyncScheduler` encola un `SyncWorker` único con `NetworkType.CONNECTED` y backoff exponencial.
3. El worker, en orden:
   1. Sube los adjuntos pendientes a Cloudinary y actualiza `IncidentMediaEntity`.
   2. Escribe el documento en Firestore usando el **UUID local como id**.
   3. Marca `SYNCED` y elimina la entrada de la outbox.
4. Si falla, incrementa `attempts`, guarda `lastError`, registra non-fatal en Crashlytics y reintenta con backoff.

En el pase de lista la sesión y sus 28 registros salen en **un solo `WriteBatch`**, no en escrituras
sueltas: o queda la sesión completa con sus registros, o no queda nada. El `sessionId` se genera en
el cliente antes de la primera escritura, así que reintentar sobrescribe el mismo documento en vez de
duplicarlo.

El campo `location` de la sesión se calcula **antes** de escribir y se guarda tal cual
(`Geofence.kt`, `LocationFix.toSessionLocation`). No se recalcula al leer: si mañana cambia el radio
del plantel, la sesión de hoy tiene que seguir diciendo si se verificó o no con la regla que estaba
vigente.

### 8.4 Conflictos

- **Pase de lista:** gana la última escritura por `updatedAt`, pero los registros se mergean por alumno (nunca se pisa el grupo completo). Una sesión `closed` solo la puede reabrir el directivo.
- **Pase de lista — completitud:** los cuatro estados son `present | absent | late | justified`; no existe "sin marcar". Con alumnos sin marcar no se guarda: `SaveRollCallUseCase` devuelve `Incomplete(n)` y la pantalla avisa. Escribir `absent` por alguien que el docente no tocó sería mentirle al tutor.
- **Incidentes:** el docente es el único que edita el suyo mientras esté en `nuevo`; después, solo el directivo cambia estado.
- **Justificantes:** el tutor solo crea; el directivo solo cambia estado y nota de revisión.
- Nunca se borra un documento de forma destructiva: `active = false` para usuarios y alumnos.

---

## 9. Servicios y dónde se usan

| Servicio | Uso concreto | Pantallas / módulo | Fase |
|---|---|---|---|
| **Authentication** | Login email/contraseña, sesión persistente, logout, recuperación | Splash, Login, Recuperar contraseña, Perfil | 0 |
| **Firestore** | Toda la persistencia estructurada | Todas | 0 |
| **Cloudinary** (en lugar de Storage) | Fotos de perfil, evidencia de incidentes, adjuntos de justificantes, tareas y avisos | Perfiles, Registrar incidente, Justificar falta | 1 |
| **Realtime Database** | Mensajes de chat y presencia | Chat y Conversación (docente y tutor) | 1 |
| **Cloud Messaging** | Push de incidente, asistencia, tarea, aviso y mensaje | Notificaciones de los tres roles | 1 |
| **Vertex AI** | — | **No se usa** | — |
| **Firebase AI Logic** (Gemini Developer API) | Asistente de IA del tutor, directo desde el cliente con App Check | Asistente IA | 3 |
| **Room** | Caché + borradores + outbox offline | Pase de lista, Registrar incidente | 1 |
| **App Check** | Play Integrity en release, debug provider en debug; exigido en Firestore, RTDB y AI Logic | Global | 0 |
| **Test Lab** | Robo test de los flujos críticos + matriz de dispositivos | Flujos de la §13 | 4 |
| **Hosting** | Sitio complementario: landing, política de privacidad, soporte | `docs/hosting/` | 4 |
| **Analytics** | Eventos de negocio y embudos | Global | 0 |
| **Crashlytics** | Crashes + non-fatals de sincronización | Global | 0 |
| **Remote Config** | Radio por defecto, IA activada, mantenimiento, versión mínima | Configuración del plantel | 2 |

### 9.1 Restricciones de costo (todo gratuito)

**Regla:** el proyecto Firebase no vincula facturación. Se queda en **Spark** y Cloudinary en **free**.

Por qué es una regla y no una preferencia: el free tier de AI Logic existe **solo** con el proveedor *Gemini Developer API* y **solo** si el proyecto está en Spark. Si se activa Blaze, Gemini Developer API pasa a tier pago. No se puede tener Functions y AI gratis en el mismo proyecto.

Consecuencias que el equipo debe asumir:

| Necesidad | Sin Cloud Functions (Spark) | Cómo se resuelve |
|---|---|---|
| Crear cuentas de docente/directivo | El cliente no puede (`createUserWithEmailAndPassword` cierra la sesión del directivo) | Script local `tools/seed` con Admin SDK + service account, o alta manual en la consola |
| Enviar push cuando ocurre un evento | No hay trigger de Firestore | Enviar desde la consola de Firebase a un tema o a un token; la app solo recibe |
| Escribir `notifications` | Sin trigger automático | El cliente que origina el evento escribe el documento (permitido por reglas) |
| Firma de subida a Cloudinary | No hay endpoint | Preset unsigned restringido por carpeta, formato y tamaño |
| Vertex AI | Requiere Blaze | Se reemplaza por AI Logic con Gemini Developer API |

Cuotas gratuitas relevantes (verificar en la consola antes de la entrega):

- **Firestore:** 1 GiB almacenado, 50 000 lecturas y 20 000 escrituras por día. Suficiente para demo; cuidado con listeners en tiempo real durante pruebas largas.
- **Realtime Database:** 1 GB almacenado y 10 GB/mes de transferencia.
- **Cloud Messaging:** sin costo.
- **Hosting:** 10 GB almacenados y 360 MB/día de transferencia.
- **Remote Config, Analytics, Crashlytics, App Check:** sin costo en el alcance del proyecto.
- **Test Lab:** cuota diaria gratuita limitada; algunos dispositivos exigen facturación.
- **AI Logic:** free tier con límites de requests por minuto y por día; ciertos modelos (preview, generación de media) requieren pago y **no** se usan.
- **Cloudinary:** 25 créditos/mes (ver §7.2).

### 9.2 Asistente con Firebase AI Logic (free tier)

**Producto:** Firebase AI Logic (`com.google.firebase:firebase-ai`), proveedor **Gemini Developer API**. Nada de Vertex AI ni del *Agent Platform Gemini API*: esos exigen facturación.

**Por qué este proveedor:** es el único con free tier sin método de pago. Requiere que el proyecto esté en **Spark**. Si se activa Blaze, Gemini Developer API pasa a tier pago.

**Configuración:**

1. Firebase Console → **AI Services → AI Logic → Get started**.
2. Elegir **Gemini Developer API**. La consola habilita las APIs y crea una Gemini API key asociada al proyecto.
3. **No** copiar esa key al código ni a `local.properties`: el SDK de AI Logic la resuelve desde el proyecto. Si se filtra al repo, hay que rotarla.
4. App Check obligatorio (Play Integrity en release, debug provider en desarrollo).

**Modelo:** elegir un modelo **de texto estable** disponible en el free tier desde la consola. Quedan prohibidos por costo:

- Modelos *preview* o *experimental*.
- Modelos de generación de media.
- Grounding con Google Search o Google Maps.

**Modelo elegido: `gemini-3.5-flash-lite`.** La consola ofrece exactamente dos modelos sin
facturación: `gemini-3.8-flash` y `gemini-3.5-flash-lite`. Se eligió el *lite* porque el
asistente responde preguntas cortas sobre asistencia —no necesita calidad de frontera— y
porque trae cuota por minuto más alta y menor latencia.

> Comprobado el 2026-11-13 con un token de App Check válido: `gemini-3.5-flash-lite`
> respondió en 2,6 s, mientras `gemini-3.8-flash` devolvía `500 high demand` o dejaba la
> conexión abierta sin responder. Si se cambia de modelo, repetir esa prueba antes de tocar
> `MODEL_NAME`: un modelo saturado se ve igual que un bug del cliente.

**Caso de uso — Asistente del tutor:**

- Responde preguntas sobre asistencia, tareas, avisos e incidentes **de sus propios hijos**.
- El contexto se arma en el cliente antes del prompt: resumen de asistencia de los últimos 30 días, tareas pendientes y avisos vigentes de esos alumnos.
- **Nunca** se envían datos de otros alumnos, credenciales, CURP ni identificadores internos.
- Historial de conversación solo en memoria del `ViewModel`; no se persiste en Firestore por privacidad.
- Respuestas acotadas (`maxOutputTokens` bajo) y en español.

**Manejo de límites:** el free tier tiene cuotas por minuto y por día. La UI debe:

- Bloquear el botón mientras hay una consulta en curso.
- Traducir `429 RESOURCE_EXHAUSTED` a un mensaje claro, sin reintentos automáticos en loop.
- Respetar el flag `ai_assistant_enabled` de Remote Config.
- Caer a respuestas predefinidas (FAQ) si la IA no está disponible.

**Ubicación en el código:**

```
data/remote/ai/AiAssistantDataSource.kt   → instancia de GenerativeModel y envío del prompt
domain/usecase/AskAssistantUseCase.kt     → arma el contexto desde Firestore y valida el rol
ui/screens/parent/ParentAIScreen.kt       → estado de UI (idle, loading, respuesta, error)
```

### Eventos de Analytics

`login_success` (role), `roll_call_started`, `roll_call_saved` (synced: bool), `incident_created` (severity, media_count), `task_published`, `notice_published`, `chat_message_sent`, `justification_created`, `ai_query` (role), `sync_failed` (op_type, attempts).

### Remote Config — claves

`default_radius_meters` (150), `ai_assistant_enabled` (true), `maintenance_mode` (false), `min_app_version` (1), `incident_media_max` (3).

---

## 10. Reparto de trabajo

| Persona | Rol principal | Fases |
|---|---|---|
| **Diego** | Docente y núcleo offline | Fase 0 (compartida) → Fase 1 → apoyo en Fase 3 |
| **Evelyn** | Directivo y tutor | Fase 0 (compartida) → Fase 2 → Fase 3 |

Reglas de convivencia:

- `main` siempre compila. Es la rama publicada: la página del repo muestra el estado real del
  proyecto y el checklist de la fase vigente con lo que ya está entregado.
- **El trabajo se hace en una rama y llega a `main` por merge.** En la Fase 1 eso fue un
  *fast-forward* de `feat/docente-pase-lista` al cerrar cada bloque, con `assembleDebug` y
  `testDebugUnitTest` en verde **antes** de cada merge. Nadie pushea commits sueltos a `main`.
- Por qué la regla importa: `main` es lo único que se ve sin abrir una rama. Si el checklist vive
  solo en la rama, el repo aparenta estar dos fases atrás.
- **Historial:** la Fase 0 se hizo lineal sobre `main`, sin ramas ni pull requests (el repo nunca
  tuvo un PR). A partir de la Fase 1 el trabajo vive en una rama y llega por merge.
- Ramas: `feat/docente-pase-lista`, `feat/admin-usuarios`, `fix/sync-reintento`.
- PRs pequeños (una pantalla o un repositorio), con captura o GIF del flujo, cuando hay dos ramas
  en vuelo al mismo tiempo.
- Conventional Commits: `feat(teacher): …`, `fix(sync): …`, `docs(context): …`.
- **Si una tarea toca el esquema de Firestore, se avisa antes de mergear**: el esquema es contrato compartido.
- Los archivos de `domain/model` y `data/` los toca una sola persona a la vez para evitar conflictos.

---

## 11. Datos de prueba

### Credenciales demo (se migran a Firebase Auth en la Fase 0)

| Rol | Email | Contraseña |
|---|---|---|
| Docente | `docente@aulasegura.com` | `123456` |
| Directora | `directora@escuela.mx` | `123456` |
| Tutor | `carlos.gomez@familia.mx` | `AulaSegura#2024` |

### Semilla mínima

- **Plantel:** Escuela Primaria "Benito Juárez", clave `15EPR0123X`, ciclo 2026-2027, 19.2926 / -99.6557, radio 150 m.
- **Directora:** Patricia Rangel Vega.
- **Docente:** Ricardo Salinas Ortega, Matemáticas, grupos 3° B (28), 3° A (30), 2° C (27).
- **Tutor:** Carlos Gómez Pérez, hijos Mariana Gómez Ruiz (3° B) y Diego Gómez Ruiz (1° A).
- **Grupos:** 3° B, 3° A, 2° C, 1° A.
- **Alumnos de 3° B:** los 28 del mockup, empezando por Ana Sofía Aguilar (N° 01).

La semilla se carga con un script único (`tools/seed/`) usando el Admin SDK, **no** a mano desde la app.

---

## 12. Cómo compilar y verificar

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
Set-Location "C:\Users\diego\AndroidStudioProjects\AulaSegura"
.\gradlew.bat :app:assembleDebug --console=plain
```

- El `java` del PATH es JDK 20 y **rompe Gradle**. Siempre setear `JAVA_HOME` al JBR de Android Studio.
- Cambios solo en Kotlin/Compose → **Run** alcanza.
- Cambios en `build.gradle.kts`, `libs.versions.toml`, `google-services.json` o recursos → **Sync** obligatorio.
- `assembleDebug` en verde es requisito para dar una tarea por terminada.

---

## 13. Definition of Done — lógica de negocio

Una pantalla pasa de "diseño" a "funcional" cuando:

- [ ] No queda ningún `sample…` ni `data class` local en el archivo de la pantalla.
- [ ] Lee y escribe a través de un repositorio; la UI no conoce Firebase.
- [ ] Tiene estados de carga, vacío y error visibles.
- [ ] Funciona sin red cuando el flujo lo exige, y sincroniza al reconectar (probado activando el modo avión).
- [ ] Las reglas de Firestore cubren lectura y escritura del caso.
- [ ] Emite su evento de Analytics.
- [ ] Los fallos de sync quedan como non-fatal en Crashlytics.
- [ ] Probada en emulador y en teléfono físico.
- [ ] `assembleDebug` verde y PR revisado por la otra persona.

---

## 14. Decisiones abiertas

| # | Decisión | Recomendación |
|---|---|---|
| 1 | Plan de Firebase | **Spark, sin facturación.** Es condición para el free tier de AI Logic (Gemini Developer API). Blaze queda descartado porque pasa la IA a tier pago. |
| 2 | Dueño del proyecto Firebase | Una sola persona lo crea y agrega a la otra como Editor. `google-services.json` se comparte por canal privado, no por git. |
| 3 | Alta de cuentas de docente/directivo | Script local con Admin SDK + custom claims, o alta manual en la consola. En cliente, `createUserWithEmailAndPassword` cerraría la sesión del directivo. |
| 4 | Cloudinary firmado o unsigned | Unsigned con preset restringido por carpeta. No hay Cloud Functions para firmar en Spark. |
| 5 | Hilt o DI manual | Hilt si KSP es estable con Kotlin 2.2.10; si no, `AppContainer` manual. |
| 6 | Emulador de Firebase | Sí para desarrollo diario; datos reales solo para la demo final. |
| 7 | Alcance del sitio en Hosting | Landing + política de privacidad + soporte. No hace falta un panel web. |
