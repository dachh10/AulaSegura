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
- [~] **F3** `sync/SyncWorker` con reintento exponencial · implementado, pendiente de verificar en device
- [~] **F4** `sync/SyncScheduler` — encola un worker único con `NetworkType.CONNECTED` · implementado, pendiente de verificar en device
- [~] **F5** Registrar fallos como non-fatal en Crashlytics · implementado (`CrashlyticsSyncReporter`), pendiente de verificar en device

**Criterio:** con modo avión se encola; al reconectar, la outbox queda vacía sin duplicados.

**Cómo probarlo**

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd C:\Users\diego\AndroidStudioProjects\AulaSegura
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Después, con el teléfono desconectado de red: cerrar un pase de lista (Fase 1) y
verificar que la sesión queda `PENDING`; reconectar y comprobar que la outbox se vacía.

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

- [ ] **G1** Decidir Hilt vs `AppContainer` manual (validar KSP con Kotlin 2.4.21)
- [ ] **G2** Módulos de Firebase, Room, repositorios
- [ ] **G3** `AulaSeguraApp` inicializa el contenedor
- [ ] **G4** Primer ViewModel inyectado y funcionando

---

## Bloque H — Cloudinary (Evelyn)

- [ ] **H1** Cuenta free + credenciales fuera del repo
- [ ] **H2** Presets unsigned por carpeta: `profiles`, `incidents`, `justifications`, `tasks`, `notices`, `schools`
- [ ] **H3** Klient de subida con OkHttp (`data/remote/cloudinary/`)
- [ ] **H4** Compresión previa en el dispositivo (≤ 1080 px, calidad ~80)
- [ ] **H5** Subida de prueba a `aulasegura/profiles/test` y metadatos en Firestore

**Criterio:** una imagen sube, devuelve `publicId` + `secure_url`, y Cloudinary descuenta crédito.

---

## Bloque I — Observabilidad (Evelyn)

- [ ] **I1** Analytics: eventos de `CONTEXT.md` §9
- [ ] **I2** Crashlytics: crash de prueba + non-fatal de sincronización
- [ ] **I3** Remote Config: `default_radius_meters`, `ai_assistant_enabled`, `maintenance_mode`, `min_app_version`, `incident_media_max`

---

## Bloque J — AI Logic

- [ ] **J1** Firebase Console → AI Logic → **Get started** → Gemini Developer API
- [ ] **J2** Confirmar que el proyecto sigue en **Spark** (sin facturación)
- [ ] **J3** Consulta de prueba desde el cliente con App Check
- [ ] **J4** Manejo de `429 RESOURCE_EXHAUSTED` con mensaje al usuario

---

## Bloque K — Seed y cierre

- [ ] **K1** Service account descargada, fuera del repo
- [ ] **K2** `tools/seed/` con Admin SDK: plantel, grupos, alumnos, docentes, tutores
- [ ] **K3** Poblar Firestore y verificar en la consola
- [ ] **K4** `assembleDebug` verde
- [ ] **K5** Cierre de Fase 0 según `PLAN.md` §Fase 0

---

## Bloque L — Pendiente para la Fase 4

- [ ] **L1** Encender enforcement de App Check (Firestore, RTDB, AI Logic)
- [ ] **L2** Registrar SHA-256 del keystore de **release**
- [ ] **L3** Regenerar el debug token en cada instalación nueva si hace falta

---

## Orden recomendado

```
A (hecho) → B (hecho) → C → D → E → F → G → H → I → J → K
                                  ↑
                    H e I puede hacerlas Evelyn en paralelo
```

**No saltear D antes de E.** Las reglas definen qué puede leer Room; si construís la
capa local primero, vas a cachear datos que después las reglas te van a negar.
