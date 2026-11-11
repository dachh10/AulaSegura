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

- [ ] **C1** Crear las 3 cuentas demo · Authentication → Users → Add user
      `docente@aulasegura.com` / `123456`
      `directora@escuela.mx` / `123456`
      `carlos.gomez@familia.mx` / `AulaSegura#2024`
- [ ] **C2** Crear el documento `users/{uid}` de cada cuenta (rol, nombre, schoolId)
- [ ] **C3** `domain/repository/AuthRepository.kt` — interfaz: `signIn`, `signOut`, `currentUser`, `observeSession`
- [ ] **C4** `data/repository/AuthRepositoryImpl.kt` — `FirebaseAuth` + lectura de rol desde Firestore
- [ ] **C5** `ui/screens/access/LoginViewModel.kt` — `UiState` con loading / error / success
- [ ] **C6** Cablear `LoginScreen` al ViewModel (quitar la comparación de strings del NavHost)
- [ ] **C7** `SplashScreen`: sesión activa → `uid → role → grafo`; sin sesión → `LOGIN`
- [ ] **C8** Logout desde Perfil de los tres roles
- [ ] **C9** Recuperar contraseña con `sendPasswordResetEmail`
- [ ] **C10** Usuario `active = false` → cierre de sesión forzado

**Criterio:** login real con las tres cuentas, cada una aterriza en su grafo, y cerrar
la app y reabrirla mantiene la sesión.

---

## Bloque D — Firestore: reglas e índices

- [ ] **D1** `docs/firestore.rules` con el esquema de `CONTEXT.md` §6.4
- [ ] **D2** `docs/firestore.indexes.json` con los índices de §6.3
- [ ] **D3** `firebase.json` + `.firebaserc` para apuntar a `aulasegu`
- [ ] **D4** Publicar reglas: `firebase deploy --only firestore:rules`
- [ ] **D5** Publicar índices: `firebase deploy --only firestore:indexes`
- [ ] **D6** Probar en el emulador los casos permitidos y los denegados

**Criterio:** un docente no puede leer incidentes de otro plantel; un tutor solo ve a sus hijos.

---

## Bloque E — Room

- [ ] **E1** Agregar KSP al catálogo y al módulo
- [ ] **E2** `data/local/AulaSeguraDatabase.kt`
- [ ] **E3** Entidades: `GroupEntity`, `StudentEntity`, `AttendanceSessionEntity`, `AttendanceRecordEntity`, `IncidentDraftEntity`, `IncidentMediaEntity`, `NoticeEntity`, `TaskEntity`, `OutboxEntity`
- [ ] **E4** DAOs con `Flow` de lectura
- [ ] **E5** Converters de fechas (`Long` ↔ `Timestamp`)
- [ ] **E6** Test de instrumentación: insertar y leer una entidad

**Criterio:** la app arranca sin red y lee grupos desde Room.

---

## Bloque F — WorkManager y sincronización

- [ ] **F1** Dependencia `work-runtime-ktx`
- [ ] **F2** `sync/OutboxEntity` + DAO
- [ ] **F3** `sync/SyncWorker` con reintento exponencial
- [ ] **F4** `sync/SyncScheduler` — encola un worker único con `NetworkType.CONNECTED`
- [ ] **F5** Registrar fallos como non-fatal en Crashlytics

**Criterio:** con modo avión se encola; al reconectar, la outbox queda vacía sin duplicados.

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
