# Fase 0 — Checklist de ejecución

> Abrí este archivo en el editor dividido a la derecha (`clic derecho en la pestaña → Split Right`)
> y dejalo visible mientras trabajás. Tildá cada ítem al terminarlo, no antes.
>
> Referencia técnica: `CONTEXT.md`. Roadmap general: `PLAN.md`.

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso

---

## Bloque A — Firebase Console

- [x] **A1** Proyecto `aulasegu` en plan **Spark** (sin facturación) · núm. 63329976744
- [x] **A2** App Android registrada · `com.example.aulasegura`
- [x] **A3** `google-services.json` en `app/` · ignorado por git
- [x] **A4** SHA-1 debug registrado · `0A58D36D…`
- [x] **A5** SHA-256 debug registrado · `9B67810F…`
- [x] **A6** Firestore en modo **Native** · base `(default)`
- [x] **A7** Authentication → Correo/Contraseña habilitado
- [x] **A8** Authentication → Google habilitado
- [x] **A9** Realtime Database creada · `aulasegu-default-rtdb.firebaseio.com`
- [x] **A10** App Check registrado con Play Integrity
- [ ] **A11** Enforcement de App Check **apagado** hasta Fase 4 (verificar antes de cada demo)

> **A11 importa:** si encendés enforcement antes de que la app entregue tokens, todo
> devuelve `PERMISSION_DENIED`. Hoy los tres servicios están en `UNENFORCED`.

---

## Bloque B — Build y arranque

- [x] **B1** Firebase BOM `35.1.0` + Auth, Firestore, RTDB, FCM, Analytics, Crashlytics, Remote Config, App Check, AI
- [x] **B2** Plugin `com.google.gms.google-services` `4.5.0`
- [x] **B3** Kotlin `2.4.21` (el BOM 35.1.0 exige ≥ 2.4.0)
- [x] **B4** `AulaSeguraApp`: init de Firebase + App Check + caché de Firestore apagada
- [x] **B5** `assembleDebug` verde

### Verificar localmente

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd C:\Users\diego\AndroidStudioProjects\AulaSegura
.\gradlew.bat :app:assembleDebug --console=plain
```

---

## Bloque C — Authentication real

- [x] **C1** Crear las 3 cuentas demo · Authentication → Users → Add user
      `docente@aulasegura.com` / `123456`
      `directora@escuela.mx` / `123456`
      `carlos.gomez@familia.mx` / `AulaSegura#2024`
- [x] **C2** Crear el documento `users/{uid}` de cada cuenta (rol, nombre, schoolId)
- [x] **C3** `domain/repository/AuthRepository.kt` — interfaz: `signIn`, `signOut`, `currentUser`, `observeSession`
- [x] **C4** `data/repository/AuthRepositoryImpl.kt` — `FirebaseAuth` + lectura de rol desde Firestore
- [x] **C5** `ui/screens/access/LoginViewModel.kt` — `UiState` con loading / error / success
- [x] **C6** Cablear `LoginScreen` al ViewModel (quitar la comparación de strings del NavHost)
- [x] **C7** `SplashScreen`: sesión activa → `uid → role → grafo`; sin sesión → `LOGIN`
- [x] **C8** Logout desde Perfil de los tres roles
- [x] **C9** Recuperar contraseña con `sendPasswordResetEmail`
- [x] **C10** Usuario `active = false` → cierre de sesión forzado

**Criterio:** login real con las tres cuentas, cada una aterriza en su grafo, y cerrar
la app y reabrirla mantiene la sesión.

**Cómo probarlo**

| Cuenta | Contraseña | Debe aterrizar en |
|---|---|---|
| `docente@aulasegura.com` | `123456` | Home docente |
| `directora@escuela.mx` | `123456` | Dashboard directivo |
| `carlos.gomez@familia.mx` | `AulaSegura#2024` | Home tutor |

1. Login con cada cuenta → cada una cae en su grafo.
2. Cerrar la app por completo y abrirla → el splash te deja adentro, sin pedir login.
3. Perfil → *Cerrar sesión* → vuelve al login.
4. *¿Olvidaste tu contraseña?* con un correo real → llega el mail de Firebase.
5. En la consola, poner `active = false` a un usuario ya logueado → la app lo expulsa al login sola.

Tests automatizados del bloque: `.\gradlew.bat :app:testDebugUnitTest` (10 tests).

---

## Bloque D — Firestore: reglas e índices

- [x] **D1** `docs/firestore.rules` con el esquema de `CONTEXT.md` §6.4
- [x] **D2** `docs/firestore.indexes.json` con los índices de §6.3
- [x] **D3** `firebase.json` + `.firebaserc` para apuntar a `aulasegu`
- [x] **D4** Publicar reglas: `firebase deploy --only firestore:rules`
- [x] **D5** Publicar índices: `firebase deploy --only firestore:indexes`
- [x] **D6** Probar casos permitidos y denegados (hecho por API con tokens reales)

**Criterio:** un docente no puede leer incidentes de otro plantel; un tutor solo ve a sus hijos.

**Verificado contra el servicio real** (tokens de las tres cuentas, no emulador):

| Prueba | Resultado |
|---|---|
| Docente lee su propio perfil | ✅ permitido |
| Docente lee perfil de otro usuario | ❌ denegado (403) |
| Docente lee alumnos de un grupo suyo | ✅ permitido |
| Docente lista todo el plantel de alumnos | ❌ denegado |
| Tutor lee alumnos donde está en `tutorIds` | ✅ permitido (solo los suyos) |
| Tutor lee alumnos por `groupId` | ❌ denegado |
| Directivo lee perfiles y alumnos de su plantel | ✅ permitido |
| Directivo consulta otro `schoolId` | ❌ denegado |
| Sin sesión (`users`) | ❌ denegado |

> **Regla de oro de las consultas:** Firestore **no filtra** con las reglas. Toda
> consulta de lista tiene que llevar el mismo filtro que la regla exige
> (`whereEqualTo("groupId", …)`, `whereEqualTo("schoolId", …)`, `whereArrayContains("tutorIds", uid)`).
> Una consulta sin ese filtro devuelve `PERMISSION_DENIED` aunque el documento
> exista y el usuario tenga permiso sobre él.

> **Decisión de esquema que tomé (y hay que respetar):** `users/{uid}.groupIds`
> guarda los grupos que dicta un **docente** y los grupos de sus hijos en el caso
> de un **tutor**. Sin esa denormalización la pregunta "¿puede este tutor leer la
> tarea de 3° B?" no se puede expresar: las reglas no pueden recorrer
> `studentIds → groupId`. El flujo de vinculación tiene que mantenerla al día.

---

## Bloque E — Room

- [x] **E1** Agregar KSP al catálogo y al módulo · KSP `2.3.13`, Room `2.8.5`, `ksp` plugin + `room.schemaLocation`
- [x] **E2** `data/local/AulaSeguraDatabase.kt` · 9 entidades, `exportSchema = true`, sin destructive migration
- [x] **E3** Entidades: `GroupEntity`, `StudentEntity`, `AttendanceSessionEntity`, `AttendanceRecordEntity`, `IncidentDraftEntity`, `IncidentMediaEntity`, `NoticeEntity`, `TaskEntity`, `OutboxEntity`
- [x] **E4** DAOs con `Flow` de lectura · `GroupDao`, `StudentDao`, `AttendanceDao`, `IncidentDao`, `NoticeDao`, `TaskDao`, `OutboxDao`
- [x] **E5** Converters de fechas (`Long` ↔ `Timestamp`) · `Converters` + `mapper/TimestampMappers.kt`
- [x] **E6** Test de instrumentación: insertar y leer una entidad · 4 tests verdes en device `65d5dd78`

**Criterio:** la app arranca sin red y lee grupos desde Room.

**Cómo probarlo**

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd C:\Users\diego\AndroidStudioProjects\AulaSegura
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

**Resultado real:** `AulaSeguraDatabaseTest` → 4 tests, 0 fallos (insertar/leer grupo,
converters de `List<String>` + `SyncState`, merge por alumno, orden y reintentos de la outbox).
`testDebugUnitTest` sigue verde con los 10 tests del bloque C.

> **Decisiones que quedan anotadas:**
> - Las entidades guardan timestamps como `Long` (epoch ms). El tipo
>   `com.google.firebase.Timestamp` solo aparece en `mapper/TimestampMappers.kt`, así
>   la base se puede testear sin sesión de Firebase.
> - Los `List<String>` (`teacherIds`, `tutorIds`, `groupIds`, `audienceRoles`) se
>   guardan con un separador `U+001F` y **no se pueden filtrar en SQL**. Todo filtro
>   por profesor/tutor se hace en memoria después de una lectura indexada. Una tabla
>   de unión queda fuera de la Fase 0.
> - `attachmentsJson` guarda el array de mapas de tareas y avisos como JSON, porque
>   SQLite no tiene listas de mapas.
> - Room 2.8.5 con KSP `2.3.13` obliga a compilar con **Java 17**; se subió
>   `compileOptions` de 11 a 17.
> - No se puso `fallbackToDestructiveMigration`: borrar la outbox en silencio
>   perdería un pase de lista hecho sin red.

---

## Bloque F — WorkManager y sincronización

- [x] **F1** Dependencia `work-runtime-ktx` · `2.12.0` (+ `work-testing` para los tests)
- [x] **F2** `sync/OutboxEntity` + DAO · la entidad y el DAO viven en `data/local` desde E3; F2 agrega `OutboxRepository`, que encola sesión + registros + outbox en una sola transacción
- [x] **F3** `sync/SyncWorker` con reintento exponencial · worker corrió en device y devolvió `SUCCESS`
- [x] **F4** `sync/SyncScheduler` — encola un worker único con `NetworkType.CONNECTED` · job `dd7a3075…` programado con tag `aulasegura-sync`
- [x] **F5** Registrar fallos como non-fatal en Crashlytics · `CrashlyticsSyncReporter` conectado al worker

**Criterio:** con modo avión se encola; al reconectar, la outbox queda vacía sin duplicados.

**Verificado en device `65d5dd78`**

| Check | Resultado |
|---|---|
| `:app:assembleDebug` | ✅ |
| `:app:testDebugUnitTest` | ✅ 14 tests (4 del mapper) |
| `:app:connectedDebugAndroidTest` | ✅ 12 tests, 0 fallos |
| Arranque real | ✅ app en foco, sin FATAL |
| Worker | ✅ `Starting work for …SyncWorker` → `Worker result SUCCESS`, tag `aulasegura-sync` |
| Room en runtime | ✅ `aulasegura.db` creado por el worker |

Los 12 tests de instrumentación: 4 de Room, **5 del runner de sincronización** (orden,
borrado al éxito, retry que conserva filas, excepción que se vuelve retry, operación
desconocida que no bloquea la cola, fila envenenada que sale tras el umbral) y **2 del
scheduler** (un solo worker vivo + requiere `CONNECTED` + `KEEP` no apila).

**Cómo probarlo**

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd C:\Users\diego\AndroidStudioProjects\AulaSegura
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

El criterio completo (modo avión → encola → reconectar → outbox vacía) necesita el pase
de lista de Fase 1: hoy la cola se puede llenar por código (`OutboxRepository`), pero
**ninguna pantalla la usa todavía**.

> **Cómo está armado (y por qué):**
> - `SyncRunner` tiene toda la política y no depende de WorkManager, así se testea
>   con una base en memoria. El `SyncWorker` solo arma las dependencias y traduce
>   el resultado a `success()` / `retry()`.
> - `enqueueUniqueWork(KEEP)` es lo que evita duplicados: diez taps sin red dejan
>   diez filas en la outbox, pero **un solo** worker pendiente.
> - Un fallo transitorio corta la pasada y devuelve `retry()`, así WorkManager
>   aplica el backoff exponencial (30 s inicial) sobre toda la cola.
> - Un fallo permanente marca `attempts`, reporta non-fatal y **deja pasar** la
>   siguiente fila. Después de `MAX_ATTEMPTS_BEFORE_DROP` (10) sale de la outbox:
>   una fila envenenada no puede bloquear para siempre el pase de lista de una
>   docente. La entidad sigue en Room marcada `FAILED`.
> - El id del documento en Firestore es el UUID local, y se escribe con `set()`,
>   así reintentar es idempotente: no crea un segundo documento.
> - Sesión y registros van en un mismo `WriteBatch`, y los contadores
>   (`counts.present/absent/…`) se recalculan desde los registros en vez de
>   confiar en la entidad: dos equipos marcando el mismo grupo no pueden dejar
>   contadores que contradigan la lista.
> - Fase 0 registra **solo** el handler de asistencia. Incidentes, justificantes
>   y subida de media quedan sin handler hasta que existan sus repositorios
>   (Cloudinary es Bloque H): esas filas se reportan y esperan, no se finge que
>   sincronizaron.

---

## Bloque G — Inyección de dependencias

- [x] **G1** Decidir Hilt vs `AppContainer` manual (validar KSP con Kotlin 2.4.21) · **contenedor manual**, acordado con el equipo; KSP ya validado en el bloque E
- [x] **G2** Módulos de Firebase, Room, repositorios · `di/AppContainer.kt` expone `authRepository`, `outboxRepository`, `syncScheduler`, `syncFailureReporter`, `syncHandlers` y `database`
- [x] **G3** `AulaSeguraApp` inicializa el contenedor · `container = AppContainer(this)` en `onCreate`, antes de cualquier activity o worker
- [x] **G4** Primer ViewModel inyectado y funcionando · `AppViewModelFactory` construye login, sesión y recuperación; `SyncWorker` toma sus dependencias del contenedor

**Criterio:** el contenedor compila, arranca la app y un ViewModel se construye por el factory sin que la UI importe Firebase.

**Verificado en device `65d5dd78`**

| Check | Resultado |
|---|---|
| `:app:assembleDebug` | ✅ |
| `:app:testDebugUnitTest` | ✅ 14 tests |
| `:app:connectedDebugAndroidTest` | ✅ 16 tests, 0 fallos (4 nuevos del contenedor) |
| Arranque real | ✅ `pid 26927`, `topResumedActivity=.MainActivity`, sin FATAL |
| Worker vía contenedor | ✅ `Starting work for …SyncWorker` → `Worker result SUCCESS` |

Los 4 tests nuevos de `AppContainerTest` cubren: el contenedor existe tras
`onCreate`, cada dependencia es una sola instancia, el factory construye los tres
ViewModels del acceso, y un ViewModel sin registrar falla ruidosamente en vez de
devolver `null`.

**Decisión:** Hilt se descartó. El grafo es chico y un archivo legible de arriba
a abajo se depura mejor que un procesador de anotaciones más. Cuando aparezca un
segundo módulo, `AppContainer` es el único lugar a dividir.

**De paso:** `ForgotPasswordViewModel` dejó de depender de `FirebaseAuth` y ahora
usa `AuthRepository.sendPasswordReset()`, así que la UI ya no conoce el SDK.

---

## Bloque H — Cloudinary (Evelyn)

- [x] **H1** Cuenta free + credenciales fuera del repo · `CLOUDINARY_CLOUD_NAME` y `CLOUDINARY_UPLOAD_PRESET` en `local.properties` → `BuildConfig`; ningún `api_secret` en el APK
- [x] **H2** Presets unsigned por carpeta · **decisión: un solo preset** (`docenteaula`); la carpeta viaja en `asset_folder` porque la cuenta usa dynamic folders. Un preset por rol no aportaba seguridad: el nombre es visible en el APK y la autorización real está en las Rules
- [x] **H3** Cliente de subida con OkHttp (`data/remote/cloudinary/`) · `OkHttpCloudinaryUploader` con multipart de 3 partes (`file`, `upload_preset`, `asset_folder`), sin SDK de Cloudinary
- [x] **H4** Compresión previa en el dispositivo (≤ 1080 px, calidad ~80) · `ImageCompressor` con `inSampleSize`, rotación EXIF y re-encode JPEG
- [x] **H5** Subida de prueba a `aulasegura/profiles/test` y metadatos en Firestore · verificado en device con cuenta real

**Criterio:** una imagen sube, devuelve `publicId` + `secure_url`, y Cloudinary descuenta crédito.

**Verificado en device `65d5dd78`**

| Check | Resultado |
|---|---|
| `:app:testDebugUnitTest` | ✅ 33 tests (19 nuevos) |
| `:app:connectedDebugAndroidTest` | ✅ 25 tests, 0 fallos |
| Subida real (test) | ✅ `aulasegura/profiles/test`, metadata escrita y releída de `users/{uid}.photo` |
| Subida real (curl) | ✅ `public_id=vapivoxv59ktlzjufvgd`, `asset_folder` respetado |

Los 19 unit tests nuevos: **9** del uploader contra un servidor HTTP real (campos del multipart,
que no viaje `api_key`/`api_secret`, mapeo de la respuesta, 400 con mensaje, 5xx, red caída,
build sin credenciales, archivo vacío, 200 ilegible), **8** de rutas y transformaciones
(inserción antes del segmento de versión, no apilar transformaciones, carpeta que no escapa del
prefijo) y **2** del mapa a Firestore. Los 7 de device son del compresor: 3000×2000 → 1080×720,
aspecto, sin upscale, JPEG real, no recicla el bitmap del llamador, ruta `ByteArray` y bytes que
no son imagen.

**Nota de la cuenta:** con `use asset folder as public id prefix: false`, la URL devuelta **no**
incluye la carpeta. Por eso `MediaMetadata` guarda `assetFolder` además de `publicId`: sin ese
campo, borrar o transformar un asset obliga a adivinar la ruta. Documentado en `CONTEXT.md` §7.3.

**Pendiente de la consola:** `allowed_formats = jpg,png,webp` en el preset. Es el único control
que un cliente propio no puede saltear; la app ya valida MIME y tamaño, pero esa validación vive
en nuestro código.

---

## Bloque I — Observabilidad (Evelyn)

- [x] **I1** Analytics: eventos de `CONTEXT.md` §9 · `AnalyticsEvent` declara los 10 nombres y **3 emiten hoy** (`login_success`, `roll_call_saved` con `synced` 0/1, `sync_failed`). Los otros 7 no tienen emisor porque su pantalla todavía no existe: declararlos sin dispararlos sería cobertura de papel
- [x] **I2** Crashlytics: crash de prueba + non-fatal de sincronización · el crash se dispara con un extra de Intent (`crashlytics_test_crash`) que solo se atiende con `BuildConfig.DEBUG`; el non-fatal ya venía del bloque F con `sync_operation` y `sync_entity` como custom keys
- [x] **I3** Remote Config: 5 claves publicadas en la consola (plantilla **versión 1**) y leídas con defaults y rangos validados

**Verificado en device `65d5dd78`**

| Check | Resultado |
|---|---|
| `:app:testDebugUnitTest` | ✅ 48 tests, 0 fallos |
| Device, bloque I | ✅ 20 tests: Analytics (7), Remote Config (2), contenedor (5), sync (6) |
| Crash de prueba | ✅ `FATAL EXCEPTION` en `MainActivity.onCreate` + Crashlytics envió el batch a `crashlyticsreports-pa.googleapis.com` |
| Remote Config | ✅ los 5 valores llegaron desde la consola (`isDeliveredFromConsole` = `true` en el device) |

**Plantilla publicada (versión 1)**

| Clave | Valor | Tipo |
|---|---|---|
| `default_radius_meters` | 150 | number |
| `ai_assistant_enabled` | true | boolean |
| `maintenance_mode` | false | boolean |
| `min_app_version` | 1 | number |
| `incident_media_max` | 3 | number |

**Decisiones que importan**

- **Los valores se validan, no solo se leen.** Remote Config lo edita una persona desde una consola, así que el fallo realista no es "falta la clave" sino "la clave está en otra unidad". Un `incident_media_max = 0` desactivaría todas las subidas sin un solo error; por eso cada lectura tiene rango y vuelve al default documentado si se sale.
- **La app refresca al arrancar, sin bloquear.** Los defaults viajan en el APK, así que el primer frame nunca espera a la red; si el fetch falla, queda lo último activado.
- **En debug el intervalo de fetch es 0.** Con el default de 12 h, un cambio en la consola tardaría medio día en verse y cada verificación parecería rota. Release conserva el default.
- **Los booleanos viajan como `1L`/`0L`.** Firebase no tiene tipo boolean para parámetros y lo descarta en silencio; hay un test que fija esa conversión.
- **`roll_call_saved` se emite dos veces con distinto `synced`:** al guardar en Room (`false`) y cuando Firestore acepta el batch (`true`). Registrar solo el éxito escondería justo las sesiones trabadas en la outbox, que es lo que el contador tiene que detectar.

**Pendiente:** el non-fatal de sincronización y el crash de prueba se confirman en el panel de Crashlytics (tarda unos minutos en procesar). El código y el envío están verificados en el dispositivo.

---

## Bloque J — AI Logic

- [x] **J1** Firebase Console → AI Logic → **Get started** → Gemini Developer API
- [x] **J2** Confirmar que el proyecto sigue en **Spark** (sin facturación)
- [x] **J3** Consulta de prueba desde el cliente con App Check
- [x] **J4** Manejo de `429 RESOURCE_EXHAUSTED` con mensaje al usuario

**Modelo:** `gemini-3.5-flash-lite`. La consola ofrece solo dos modelos sin facturación
(este y `gemini-3.8-flash`); el *lite* trae cuota por minuto más alta y menor latencia, que es
lo que necesita un asistente de preguntas cortas.

| Ítem | Evidencia |
| --- | --- |
| J1 | Service Usage API: `generativelanguage` y `firebasevertexai` en `ENABLED` |
| J2 | Cloud Billing API: `billingEnabled=False` para `aulasegu` |
| J3 | Device: `session: Success(uid=3lXeJEvp…)`, `ask: starting, assistant available=true`, `ask: finished with Answer` — 3.5 s, `OK (1 test)` |
| J4 | `AiFailureMappingTest`: 8 casos con las excepciones reales del SDK (construidas por reflexión, porque los constructores son `internal`) + un `429` real observado al insistir |

> **Si el asistente "no contesta", mirá el modelo antes que el código.** El 2026-11-13
> `gemini-3.8-flash` devolvía `500 high demand` o dejaba la conexión abierta sin responder
> (el cliente se quedaba esperando), mientras `gemini-3.5-flash-lite` contestaba en 2,6 s.
> Un modelo saturado se ve idéntico a un bug propio.

---

## Bloque K — Seed y cierre

- [x] **K1** Service account descargada, fuera del repo
- [x] **K2** `tools/seed/` con Admin SDK: plantel, grupos, alumnos, docentes, tutores
- [x] **K3** Poblar Firestore y verificar en la consola
- [x] **K4** `assembleDebug` verde
- [x] **K5** Cierre de Fase 0 según `PLAN.md` §Fase 0

**Llave:** cuenta de servicio `firebase-adminsdk-fbsvc@aulasegu.iam.gserviceaccount.com`,
key id `d1fb395a…` guardada en `%USERPROFILE%\.aulasegura\firebase-adminsdk.json` (ACL solo
para el usuario). **Revocarla cuando la Fase 0 esté cerrada** — no hace falta después del seed.

**Qué siembra `tools/seed/seed.mjs`:**

| Colección | Documentos |
| --- | --- |
| `schools` | `sch-benito-juarez` |
| `groups` | `grp-3b` (28), `grp-3a` (30), `grp-2c` (27), `grp-1a` (26) |
| `students` | 111, con id `stu-<grupo>-<nn>` |
| `users` | solo vínculos: `schoolId`; docente `subjects`/`groupIds`; tutor `groupIds`/`studentIds` |

El script resuelve las tres cuentas por email (`auth.getUserByEmail`) y **nunca toca** los
campos de identidad de `users` (`role`, `fullName`, `email`): esas cuentas ya las creó el
flujo de auth. Los vínculos del tutor se escriben con `merge`, y `tutorIds` nace vacío salvo
para sus dos hijos, para no borrar un vínculo que agregue el flujo real más adelante. El
`groupIds` del tutor está denormalizado a propósito: las reglas no pueden caminar
`studentIds → groupId`.

| Ítem | Evidencia |
| --- | --- |
| K1 | Key creada por API (`iam.googleapis.com/.../serviceAccounts/.../keys`), archivo fuera del repo, 2.364 bytes, ACL restringida |
| K2 | `tools/seed/{package.json,seed.mjs,README.md}`; `node_modules/` en `.gitignore` |
| K3 | Corrida real: `schools 1`, `groups 4`, `students 111`, `users 3`; segunda corrida `0 nuevos, 111 existentes` (idempotente); read-back con `studentCount` igual al conteo real en los 4 grupos, `Mariana Gómez Ruiz` y `Diego Gómez Ruiz` con `tutorIds=OK` |

> La verificación se lee a sí misma: en la primera corrida encontró que `stu-1a-07` era
> "César Galván" en vez de Diego Gómez Ruiz, porque el id del hijo se había escrito pero
> nunca se había puesto en el roster de 1° A. Un seed que sólo imprime "listo" no lo habría
> visto.

### Cierre de la Fase 0

`PLAN.md` pide: un docente **creado por el seed** inicia sesión real, aterriza en
`TeacherHome`, la app lee su perfil desde Firestore, App Check activo, AI Logic contesta y
`assembleDebug` compila. Punto por punto:

| Criterio | Estado | Evidencia |
| --- | --- | --- |
| Docente inicia sesión real | ✅ | Device: `session: Success(user=AppUser(uid=3lXeJEvp…, role=TEACHER, fullName=Ricardo Salinas Ortega, schoolId=sch-benito-juarez, active=true))` |
| Perfil leído desde Firestore | ✅ | Mismo log: `signIn`/`restoreSession` devuelven `AppUser` armado desde `users/{uid}` |
| Aterriza en `TeacherHome` | ✅ (código) | `SessionState.SignedIn → role.homeRoute()` y `UserRole.TEACHER → Routes.TEACHER_HOME` |
| App Check activo | ✅ | Device: `appCheckHandsOutATokenForTheRequest` en verde |
| AI Logic responde | ✅ | Device: `ask: finished with Answer` en 3,5 s con `gemini-3.5-flash-lite` |
| `assembleDebug` verde | ✅ | `BUILD SUCCESSFUL in 7s`, APK de 31,47 MB |
| Spark, sin Cloud Functions | ✅ | Cloud Billing API: `billingEnabled=False` |

> **Único matiz:** el saludo del `TeacherHome` todavía dice "Hola, profe Ricardo" escrito a
> mano. El perfil real ya se lee y se usa para elegir el grafo de navegación; reemplazar el
> texto por `fullName` es trabajo de la Fase 1, no de la fundación técnica.
>
> **Antes de pasar a Fase 1:** revocar la key de la service account. Ya cumplió su función y
> una llave de Admin SDK viva no aporta nada al repo.

---

## Bloque L — Pendiente para la Fase 4

- [ ] **L1** Encender enforcement de App Check (Firestore, RTDB, AI Logic)
- [ ] **L2** Registrar SHA-256 del keystore de **release**
- [ ] **L3** Regenerar el debug token en cada instalación nueva si hace falta

---

## Orden recomendado

```
A (hecho) → B (hecho) → C (hecho) → D (hecho) → E (hecho) → F (hecho) → G (hecho)
→ H (hecho) → I (hecho) → J (hecho) → K (hecho) ✅  Fase 0 cerrada
```

**No saltear D antes de E.** Las reglas definen qué puede leer Room; si construís la
capa local primero, vas a cachear datos que después las reglas te van a negar.
