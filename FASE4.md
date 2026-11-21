# Fase 4 - Registro, aprobación y vinculación (Checklist de ejecución)

Equipo: Diego y Evelyn · base: `main` @ `d503046` · dispositivo de prueba usado en Fase 3: `65d5dd78` (`adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`).

Esta fase se apoya en `PLAN.md`, `FASE1.md`, `FASE2 Directores.md`, `FASE3.md` y `CONTEXT.md`, y en los documentos de contexto `AulaSegura-registro-vinculacion.md` y `AulaSegura-pantallas-actualizadas-y-nuevas.md`. **No redefine nada de lo anterior**: cambia quién captura los datos (el director deja de ser capturista) y lo convierte en revisor y aprobador.

---

## 1. Cómo se engancha con las fases anteriores

| Dato | Lo produce | Lo consume en esta fase |
|---|---|---|
| `users/{uid}` con `role`, `schoolId`, `studentIds` | Fase 0/2 - alta con Admin SDK y app del directivo | 4.2-4.5 estado de cuenta, verificación, solicitudes |
| `users/{uid}.estado` (`pendiente`/`aprobado`/`rechazado`) | Fase 4 - registro del docente y aprobación del director | acceso al menú docente |
| `users/{uid}.vinculacion` (`pendiente`/`vinculado`/`rechazada`) | Fase 4 - QR y revisión de la directora | 4.2 salto al inicio del tutor |
| `users/{uid}.fechaSolicitud` | Fase 4 - registro | ordenar la cola de solicitudes del director |
| `students/{id}` con `groupId`, `listNumber`, `tutorIds` | Fase 2 - alta de alumnos | 4.4 lectura y escritura con el nuevo vocabulario, 4.7 asignación en lote |
| `students/{id}` con `tutores`, `grado`, `grupo`, `creadoPor`, `syncStatus` | Fase 4 - aprobación del QR | Fase 3 - inicio, asistencia y tareas del tutor |
| `linkingCodes/{code}` de 6 caracteres | Fase 2 - alta de alumnos | se conserva como alta individual secundaria; ya no es obligatoria |
| `groups/{id}` y horario | Fase 2 - grupos | 4.4 asignación de grado/grupo al aprobar |
| Outbox + WorkManager | Fase 1/2 - sincronización | 4.4 aprobación sin conexión |
| `docs/firestore.rules` (`firebase.json` apunta acá) | Fase 2 | 4.1 reglas base, 4.9 reglas completas |

**Regla de oro:** *«Validar solo en la interfaz no basta: las reglas de Firestore deben impedir que alguien se apruebe a sí mismo o se vincule a un alumno ajeno.»* Ninguna pantalla nueva se da por terminada si su escritura no está permitida -y restringida- en las reglas.

---

## 2. Escenario de aceptación (el flujo que pidió la escuela)

Prueba guía; cada bloque debe dejarla más cerca de funcionar:

1. **Docente:** elige la pestaña Docente, escribe correo real, datos y materia, envía la solicitud. Se crea la cuenta, se envía el correo de verificación y queda `estado = pendiente`.
2. **Docente:** verifica el correo y ve *«Tu cuenta está en revisión»*. No puede entrar a ninguna otra pantalla.
3. **Director:** en Solicitudes ve sólo docentes con correo verificado, con nombre, correo y antigüedad. Acepta, rechaza o acepta todas en lote. El docente aprobado entra a su menú.
4. **Tutor:** elige la pestaña Tutor, escribe su nombre, teléfono, correo real, contraseña y **el nombre completo de su hijo**. Ya no existe el campo del código del alumno.
5. **Tutor:** verifica el correo y ve un QR grande con `{"v":1,"uid":"...","alumno":"..."}` generado en el dispositivo. No pide internet. Si la directora rechaza el nombre, puede corregirlo y generar otro QR.
6. **Directora:** abre Escanear alumno, da permiso de cámara, escanea el QR y ve al alumno, al tutor con su correo verificado, y selectores de grado y grupo (A-F) con opción «Sin grupo». Si ya existe un alumno con ese nombre, ofrece «Vincular como segundo tutor».
7. **Directora:** acepta. Se crea `students/{id}` con `tutores = [uid]`, `grado`, `grupo` (vacíos si queda sin grupo) y `users/{uid}.vinculacion = vinculado`. Con sin conexión, guarda en Room con `syncStatus = PENDIENTE` y WorkManager sincroniza al volver la señal.
8. **Tutor:** su app escucha su documento y pasa sola al inicio con el hijo ya vinculado: asistencia, tareas, avisos e incidentes reales. Si la directora rechaza, ve el mensaje y corrige el nombre.
9. **Director:** los alumnos sin grupo aparecen en su propio filtro y se asignan en lote; mientras tanto el tutor ve «Grupo por asignar».

**Criterio de cierre:** el docente accede sólo cuando el director lo aprueba, y el tutor ve los datos de su hijo sólo cuando la directora escanea y aprueba el QR -sin código de alumno y sin que el director capture los datos-.

---

## 3. Bloques de trabajo

Los IDs siguen la lista acordada en el logbook. El orden de ejecución se define en §6.

| # | Tarea | Pantalla(s) | Servicios | Estado |
|---|---|---|---|---|
| **4.1 (T1)** | Migrar `students`/`users` y Room; reglas base; quitar el código obligatorio | sin pantalla nueva | Firestore, Room, reglas | ✅ **lista y mergeada en `main`** - `1ff8eea`, `d98e2ba`, `5b7da3f` (merge `8b6b982`) |
| **4.2 (T2)** | QR del tutor: generación offline, corregir nombre, escucha en tiempo real | `LinkChildScreen` | `users/{uid}` | ✅ **lista, mergeada y verificada en dispositivo** - `8cd167a`, `75e564f` (merge `43f46f0`), fixes `c39dfa3`, `33e6ebb` (merge `fc3da5f`) |
| **4.3 (T3)** | Escanear alumno: cámara, permiso, validación del QR | `ScanStudentScreen` | CameraX + ZXing | ✅ **lista, mergeada y en verificación en dispositivo** - `c24fefa`, `a26fa07`, fixes `15828eb`, `d43354b` (merge `74df0ca`) |
| **4.4 (T4)** | Revisar alumno: grado/grupo, duplicados, segundo tutor, alta offline | `ReviewStudentScreen` | `students`, `users`, reglas | ⚠️ **online mergeado, desplegado y verificado en dispositivo** - `bc20574`, `a7de890`, `4a74526` (merge `8fc1fcf`), fixes `1bba1de`, `a842819`, `232937a`, `1883c2a`; falta T4b (offline) |
| **4.5 (T5)** | Solicitudes de docentes: listar verificados, aceptar/rechazar, lote | nueva, directora | `users/{uid}.estado` | ⬜ pendiente |
| **4.6 (T6)** | Panel del director: tarjetas, FAB y navegación con badges | `AdminDashboardScreen` | - | ⬜ pendiente |
| **4.7 (T7)** | Alumnos sin grupo: búsqueda, selección y asignación en lote | nueva, directora | `students` | ⬜ pendiente |
| **4.8 (T8)** | Inicio del tutor: «+ Agregar hijo», grupo por asignar, sin datos | `ParentHomeScreen` | `students` | ⬜ pendiente |
| **4.9 (T9)** | Reglas completas: `email_verified`, director-only, lectura por tutor/grupo | sin pantalla | `docs/firestore.rules` | ⚠️ parcial |
| **4.10 (T10)** | Tests JVM por regla y flujo, build verde, prueba en dispositivo | todas | Gradle + device | ⬜ pendiente |

Ya entregado antes de esta fase: login sin selección de rol y redirección por estado, Registro Tutor/Docente, Verificar correo, Cuenta en revisión/rechazada, pantalla de vinculación como placeholder, chips de hijos y reglas de autocreación.

---

## 4. Cambio de contrato necesario (antes de seguir)

El bloque 4.1 ya dejó la base; lo que sigue la consume:

| Campo | Antes | Ahora | Dónde quedó |
|---|---|---|---|
| `students.tutores` | `tutorIds` | lista de uno o dos uid | doble escritura + fallback en ambos sentidos |
| `students.grupo` / `grado` | `groupId` / `listNumber` | texto, vacío si es «Sin grupo» | `AdminStudent`, `StudentEntity`, reglas |
| `students.creadoPor` | no existía en alumnos | uid de la directora que aprobó | `newStudentDocument(createdBy = ...)` |
| `students.syncStatus` | `syncState` de la app | `PENDIENTE` = `SyncState.PENDING` | Room migración 4→5, columna con default `SYNCED` |
| `users.fechaSolicitud` | se escribía, no se leía | `Long` en `AppUser` para ordenar la cola | `resolveProfile` |
| `users.registrationCode` | obligatorio en el registro tutor | ya no lo pide el registro nuevo | se conserva sólo para el alta individual secundaria |

No se agregó ninguna colección nueva y no se borró ningún dato almacenado.

---

## 5. Decisiones y restricciones (no reabrir sin motivo)

- **Doble vocabulario, no ruptura.** Los nombres nuevos se escriben junto a los viejos y se leen con fallback simétrico, igual que el par `role`/`rol`. El roster docente sigue viendo a los alumnos creados por QR.
- **Migración Room 4→5 no destructiva.** Cuatro columnas con default; nada se droppea. El outbox y la asistencia offline sobreviven.
- **El QR se genera en el dispositivo** (ZXing), sin red y sin inventar datos del alumno.
- **No hay registro público de director**: su cuenta se crea a mano en Firebase.
- **Permiso de cámara**: si se niega, la app explica para qué lo necesita.
- **`minSdk 24`, sin desugaring**: nunca `java.time`; usar `Calendar`/`SimpleDateFormat`.
- **Idioma**: documentos y UI en español; código, comentarios y commits en inglés; sin atribución de IA en commits.
- **Entrega**: work units en rama de trabajo; merge y push a `main` sólo con OK explícito del dueño.

---

## 6. Orden de ejecución y bloques de entrega

1. **4.1 ✅** - base de datos y reglas (3 work units, rama `feat/registro-vinculacion-t1`).
2. **4.2** - QR del tutor. Es el punto de partida de la mitad visual: sin él no hay 4.3 ni 4.4.
3. **4.3 → 4.4** - escáner y revisión/aprobación (incluye offline).
4. **4.5** - solicitudes de docentes; luego **4.6** panel con contadores.
5. **4.7** - alumnos sin grupo; **4.8** - inicio del tutor con «Agregar hijo».
6. **4.9** - reglas completas, después de que cada escritura tenga su pantalla.
7. **4.10** - tests y prueba en dispositivo del ciclo completo.

Cada work unit lleva sus tests en el mismo commit y se verifica con `testDebugUnitTest` + `assembleDebug` antes de marcarlo.

---

## 7. Verificación

### 7.1 Ya ejecutado (4.1)

- `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` → **80 suites / 575 tests / 0 fallos**, `assembleDebug` verde.
- Esquema Room 5 exportado en `app/schemas/.../5.json`.
- Reglas: chequeo estructural (73/73 llaves, 382/382 paréntesis) y **despliegue realizado** el 21 nov 2026 con `firebase deploy --only firestore:rules --project aulasegu`: compilación sin errores, `uploading rules` y `Deploy complete!`. Segundo deploy para T4 (whitelist del director con `estado`/`vinculacion`). Aún no se ejecutaron con cuentas reales en el dispositivo.

### 7.2 Pasos en el dispositivo (a cargo del equipo)

1. **Instalar la build:** `adb devices` y luego
   `adb -s <device> install -r app\build\outputs\apk\debug\app-debug.apk`.
2. **Base limpia:** la base se borró y quedó sólo la cuenta de la directora en `users`, así que ya no hay alumnos/asistencia con los que probar la migración 4→5 con datos viejos. Instalar desde cero, iniciar con la directora y confirmar que la app abre sin `IllegalStateException` (`adb logcat | findstr /i "room migration"`). La migración con datos reales queda cubierta por el esquema exportado y los tests JVM; cuando vuelvan a existir alumnos, repetir la comprobación de roster/outbox.
3. **Reglas:** ya están desplegadas en `aulasegu` (§7.1). Confirmá en Firebase Console → Firestore → Rules que la regla de `students` incluye `tutores` y que el alta individual de la directora sigue funcionando. Las reglas completas (4.9, `email_verified`) todavía no están.
4. **Flujo existente (regresión):** con la cuenta de directora, crear un alumno por el alta individual de siempre; verificar que el docente lo ve en su grupo y que el tutor sigue viendo a su hijo en el inicio. Esto prueba que el doble vocabulario no rompió Fase 2/3.
5. **Perfil y sesión:** iniciar y cerrar sesión con tutor, docente y directora; verificar que cada uno cae en su pantalla y que el docente pendiente sigue en «Cuenta en revisión».
6. **Aún no se puede probar:** el QR, el escáner, la aprobación por QR, el segundo tutor y la asignación en lote llegan con 4.2-4.7.

**Segundo fallo reportado en dispositivo y corregido (T3):** al escanear, la app moría con `FATAL EXCEPTION: pool-19-thread-1` → `UnsupportedOperationException: This luminance source does not support rotation by 90 degrees` en `decodeQr`. `PlanarYUVLuminanceSource` no soporta rotación y la llamada estaba fuera del `try`, así que la excepción escapaba del hilo del analyzer. Fix `d43354b`: `ImageProxy.toBitmap()` (CameraX ya aplica la rotación) + plano de luminancia puro (`QrLuminance`, 2 tests JVM) + `runCatching` en todo el analyzer. Verificación: 83 suites / 584 tests / 0 fallos + `assembleDebug` verde. Falta que el dueño repita la prueba.

**Fallo reportado en dispositivo y corregido (T3):** el botón «Permitir cámara» parecía no hacer nada: el permiso `android.permission.CAMERA` no estaba declarado en `AndroidManifest.xml`, así que el launcher lo denegaba al instante (el log mostraba foreground/background del diálogo sin que se abriera la vista). Fix `15828eb`: declarar el permiso y sacar «Escanear alumno» del `Column` con scroll para dejarlo como botón flotante sobre la barra inferior. Nota: el HTML `docs/stitch/07-panel-directivo.html` ubica la isla centrada (`fixed bottom-[88px] ... justify-center`), no en la esquina; se sigue la ubicación que pidió el dueño (inferior derecha). Verificación: 82 suites / 582 tests / 0 fallos + `assembleDebug` verde. Falta que el dueño repita la prueba en dispositivo.

**Evidencia en dispositivo (21 nov 2026, T4b):** el dueño probó el ciclo con modo avión: la aprobación mostró «Se vinculará al volver la señal», y al restaurar la red el `SyncWorker` subió el alumno y los enlaces del tutor a Firestore; el tutor vio al hijo vinculado sin errores de permisos. T4b queda cerrado.

**T4b (commits `1f44c10`, merge `c333734`):** la aprobación decide online/offline con `NetworkStatus`. Sin señal, `ReviewStudentViewModel` guarda el alumno en Room con `syncStatus=PENDING` y encola `APPROVE_STUDENT` con un payload versionado; el mensaje dice explícitamente «Se vinculará al volver la señal». `StudentApprovalSyncHandler` reproduce al volver la conexión el mismo batch de dos documentos (alumno + `studentIds`/`groupIds`/`vinculacion` del tutor), con `PERMISSION_DENIED` idempotente y bookkeeping PENDING→SYNCING→SYNCED/FAILED. El repositorio online y el handler comparten `approvedStudentDocument`, así que escriben campos idénticos. Verificación: 86 suites / 597 tests / 0 fallos + `assembleDebug` verde. Instrumentado `StudentApprovalOfflineSyncTest` escrito (airplane mode → PENDING → drenar → SYNCED en Firestore) pero **no ejecutado**: no hay dispositivo, y el módulo `androidTest` ya tenía un error preexistente en `AiAssistantInstrumentedTest` (firma cambiada de `AskAssistantUseCase`) que impide compilarlo.

**Evidencia en dispositivo (21 nov 2026):** ciclo completo verificado: escanear el QR del tutor → revisar → asignar grado/grupo → Aceptar → el alumno y sus datos aparecen correctamente en las pantallas del tutor (asistencia, tareas, avisos). Durante la prueba aparecieron y se corrigieron tres fallos: `PERMISSION_DENIED` en el write (alias `rol` del director, `1bba1de`), doble bind de cámara al volver (`a842819`) y `PERMISSION_DENIED` en las lecturas del tutor porque la aprobación no llenaba `studentIds`/`groupIds` en `users/{tutorUid}` (`1883c2a`; el `232937a` de `tutorIds` también quedó desplegado). Reglas desplegadas en cada paso.

**T4 online desplegado (merge `8fc1fcf`):** reglas con el whitelist del director desplegadas en `aulasegu` (`rules file compiled successfully`, `uploading rules`, `Deploy complete!`). Verificación tras el merge: 84 suites / 590 tests / 0 fallos + `assembleDebug` verde. Falta probar en dispositivo el ciclo completo: escanear el QR del tutor → revisar → aceptar → el tutor pasa a su inicio; y rechazar → el tutor ve el mensaje. Sigue pendiente la aprobación offline (T4b).

**T4 parcial (rama `feat/fase4-t4-revisar-alumno`, sin mergear):** `bc20574` decide el plan de aprobación sin I/O (4 tests: sin grupo deja grado y grupo vacíos, duplicado ofrece segundo tutor, nombre vacío se rechaza); `a7de890` agrega `approveStudent` (crea alumno + vincula tutor en un batch, o agrega segundo tutor a un alumno existente), `setTutorLinking` para rechazo y `estado`/`vinculacion` al whitelist del director en reglas; `4a74526` reemplaza el stub con el formulario real (chips de grado/grupo, sin grupo, aviso de duplicado, aceptar/rechazar) y su ViewModel. Verificación: 84 suites / 590 tests / 0 fallos + `assembleDebug` verde. **Falta:** aprobación offline (outbox/WorkManager, no existe operación de alumno) y desplegar las reglas. Sin el deploy, el batch de aprobación será rechazado por las reglas viejas.

**Evidencia en dispositivo (21 nov 2026):** el dueño cerró la app con el tutor pendiente y al reabrir volvió a ver el QR; «Escanear alumno» abrió directo el diálogo real del permiso y la cámara funcionó. Ciclo de recuperación verificado.

**Recuperación del QR y permiso directo (commits `c39dfa3`, `b968325`, `33e6ebb`, merge `fc3da5f`).** El dueño reportó que, tras registrarse y verificar, cerrar la app dejaba al tutor sin forma de volver al QR: el Splash navegaba con `role.homeRoute()` (siempre `PARENT_HOME`) y `SessionState.SignedIn` sólo guardaba el rol. Tres work units: `c39dfa3` (el estado lleva el `AppUser` completo y el Splash usa `destinationRoute()`), `b968325` (el botón flotante pide el permiso real de cámara al tocarlo; se eliminó la pantalla intermedia; si se niega hay mensaje corto y enlace a Ajustes cuando es permanente) y `33e6ebb` (caché offline de `estado`/`vinculacion`/`hijoSolicitado`/`fechaSolicitud` + migración Room 5→6, esquema 6 exportado). Verificación: 83 suites / 586 tests / 0 fallos + `assembleDebug` verde. Falta probar en dispositivo: cerrar y reabrir con el tutor pendiente debe mostrar el QR otra vez; tocar «Escanear alumno» debe abrir directo el diálogo del sistema.

**T3 (commits `c24fefa`, `a26fa07`, `15828eb`, `d43354b`, merge `74df0ca`):** `ScanStudentUi.from` valida el payload y rechaza códigos ajenos con «QR no válido» sin escribir nada (2 tests JVM); `ScanStudentScreen` usa CameraX (vista, marco y línea del diseño, linterna y explicación si se niega el permiso) y entrega el payload a una ruta de revisión; botón «Escanear alumno» en el panel del director. CameraX 1.4.0 agregado desde Google Maven (Risk: dependencias nuevas). Verificación: 82 suites / 582 tests / 0 fallos + `assembleDebug` verde. Falta probar en dispositivo el permiso, el escaneo real del QR del tutor y la navegación a revisión.

**Evidencia en dispositivo (T2, 21 nov 2026):** el dueño registró un tutor con correo real, verificó el correo y la app generó el QR del hijo correctamente. Falta el lado directora (T3/T4) para cerrar el ciclo de escaneo.

**T2 (commits `8cd167a`, `75e564f`, merge `43f46f0`):** payload versionado `{"v":1,"uid":"...","alumno":"..."}` validado por 3 tests JVM; ZXing `core:3.5.3` agregado al catálogo y resuelto desde Maven Central (Risk: dependencia nueva); `QrCodeImage` dibuja el código sin red; el nombre rechazado se corrige y guarda en `users/{uid}.hijoSolicitado`; `observeLinkingState()` escucha el documento en tiempo real y el ViewModel pone `proceed` cuando llega `vinculado` (2 tests JVM nuevos). Verificación: 81 suites / 580 tests / 0 fallos + `assembleDebug` verde. Falta probar en dispositivo que el QR es legible y que el salto ocurre solo.

**Evidencia observada (21 nov 2026, dispositivo):** Run desde `main` con T1 mergeada. Cold start sin `FATAL EXCEPTION` ni `IllegalStateException` de Room; `FirebaseApp initialization successful`; login de la directora exitoso (notificación de token de FirebaseAuth y FCM token recibido); `WM-WorkerWrapper: Worker result SUCCESS` para `SyncWorker`. No aparece `PERMISSION_DENIED` de Firestore. Los mensajes `E` del log corresponden al sistema OEM (Oplus/Play Services), no a la app. Queda pendiente confirmar en pantalla que el panel carga datos bajo las reglas nuevas -el log no prueba lecturas de Firestore- y ejecutar la migración 4→5 con una base v4 real cuando vuelva a haber datos.

---

## 8. Riesgos y cosas que quedan fuera

- Las reglas modificadas no se ejecutaron en emulador ni se desplegaron: sólo pasaron chequeo estructural.
- La migración Room se validó por esquema exportado y tests JVM, pero no se ejecutó en un dispositivo con datos reales.
- `fechaSolicitud` se lee en memoria pero no se cachea en `ProfileEntity`: la cola del director se sirve desde Firestore.
- Quedan fuera de 4.1: generación/escaneo de QR, aprobación en lote, segundo tutor, asignación grupal, `email_verified` en reglas y las pantallas 4.2-4.8.

---

## Cierre de la Fase 4

### Evidencia mínima esperada

- [x] 4.1 - modelos y Room migrados sin borrar datos; reglas base adaptadas; tests y build en verde.
- [x] 4.2 - QR del tutor generado offline con corrección de nombre y escucha en tiempo real (código y tests; prueba en dispositivo pendiente; recuperación al reabrir en rama, pendiente de merge).
- [x] 4.3 - escáner con permiso y validación de QR (código y tests; prueba en dispositivo pendiente).
- [x] 4.4 (online) - alta del alumno y vinculación, con duplicados: verificado en dispositivo.
- [x] 4.4b (T4b) - aprobación sin conexión (verificada en dispositivo: modo avión → mensaje «Se vinculará al volver la señal» → al volver la red el alumno y los enlaces del tutor aparecen en Firestore y el tutor ve al hijo).
- [ ] 4.5 - aprobación de docentes con correo verificado y lote.
- [ ] 4.6 - panel del director con contadores.
- [ ] 4.7 - asignación de grupo en lote.
- [ ] 4.8 - inicio del tutor con «Agregar hijo» y estado sin grupo.
- [ ] 4.9 - reglas completas desplegadas.
- [ ] 4.10 - ciclo completo probado en dispositivo.

## Riesgos anotados

1. **Persistencia:** migración de esquema y formatos de documento; mitigada con defaults y doble escritura, pendiente de prueba en dispositivo.
2. **Seguridad:** las reglas base ya están desplegadas, pero la aprobación/vinculación completas (4.9) y la prueba con cuentas reales siguen pendientes antes de cerrar la fase.
3. **Permisos:** cámara y notificaciones; la negación del permiso debe explicarse, no bloquear la app.
4. **Sin conexión:** la aprobación offline depende del outbox/WorkManager existente y de que la regla de correo verificado se compruebe al sincronizar.
