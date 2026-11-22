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
| **4.3 (T3)** | Escanear alumno: cámara, permiso, validación del QR | `ScanStudentScreen` | CameraX + ZXing | ✅ **lista, mergeada y verificada en dispositivo** - `c24fefa`, `a26fa07`, fixes `15828eb`, `d43354b` (merge `74df0ca`) |
| **4.4 (T4)** | Revisar alumno: grado/grupo, duplicados, segundo tutor, alta offline | `ReviewStudentScreen` | `students`, `users`, reglas | ⚠️ **mergeado, desplegado y verificado en dispositivo** - `bc20574`, `a7de890`, `4a74526` (merge `8fc1fcf`), fixes `1bba1de`, `a842819`, `232937a`, `1883c2a`; T4b (offline) completado y verificado en `1f44c10`/`c333734` |
| **4.5 (T5)** | Solicitudes de docentes: listar verificados, aceptar/rechazar, lote | `TeacherRequestsScreen` | `users/{uid}.estado` | ✅ **mergeada y verificada en dispositivo** - `e9bccc3`, `3ddd414` (merge `cc3bfa9`); fixes de verificaci-on `f102d3e`, `734f826`, `000d7ef` |
| **4.6 (T6)** | Panel del director: tarjetas, FAB y navegación con badges | `AdminDashboardScreen` | - | ✅ **hecho según pedido del dueño** - dos cards de acción (Solicitudes con contador, Alumnos sin grupo) y hero con círculo de asistencia + leyenda - `c7bc139`; tabs se mantienen (decisión del dueño) (merge `91eb762`; cards parejas `c299cd3`/merge `714b422`) |
| **4.7 (T7)** | Alumnos sin grupo: busqueda, seleccion y asignacion en lote | `UngroupedStudentsScreen` | `students` | OK mergeado en `3f8ff74` y verificado en dispositivo (asignacion, contador y numeros de lista) |
| **4.8 (T8)** | Inicio del tutor: + Agregar hijo, grupo por asignar, sin datos | `ParentHomeScreen` | `students` | OK rama `3b457a5`; merge pendiente |
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

**Registro cronologico de entrega (todo cerrado salvo 4.7-4.10):**

- **T3 - escaner QR:** `c24fefa`/`a26fa07` (merge `74df0ca`). Correcciones por prueba en dispositivo: permiso CAMERA declarado y boton flotante (`15828eb`), decoder sin rotacion que crasheaba (`d43354b`), regla `tutorIds` accesible desde la query (`232937a`) y `unbindAll` al salir (`a842819`). Verificado en dispositivo.
- **T4 - revision y aprobacion online:** `bc20574`/`a7de890`/`4a74526` (merge `8fc1fcf`). Fix de reglas para la directora creada a mano (`1bba1de`) y de `studentIds`/`groupIds` en el perfil del tutor (`1883c2a`). Verificado en dispositivo: escanear, revisar, aceptar y el tutor ve sus datos (asistencia, tareas, avisos).
- **T4b - aprobacion offline:** `1f44c10` (merge `c333734`). `NetworkStatus` elige online/offline; sin senal se guarda el alumno en Room con `syncStatus=PENDING` y se encola `APPROVE_STUDENT`; el handler reproduce el batch de dos documentos al volver la senal. **Verificado en dispositivo con modo avion**: mensaje "Se vinculara al volver la senal", al restaurar red el SyncWorker subio el alumno y los enlaces del tutor, y el tutor vio al hijo. CERRADO.
- **T5 - solicitudes de docentes:** `e9bccc3`/`3ddd414` (merge `cc3bfa9`) mas los fixes de verificacion automatica `f102d3e`, `734f826`, `000d7ef` y `000d7ef` (auto-persistir en login y al entrar a revision). `emailVerified` se escribe solo cuando Auth lo confirma (token fresco), sin depender de que el usuario descubra un boton. Verificado en dispositivo: registro, verificacion, solicitud en la cola, aprobacion y acceso del docente a su menu. CERRADO.
- **4.6 - panel del director:** `c7bc139` (merge `91eb762`) y cards parejas `c299cd3` (merge `714b422`). Dos cards de accion y hero con circulo de asistencia alimentado por datos reales. Tabs se mantienen por decision del dueno. CERRADO.
- **Docs de orden:** `86df584` deja checklist y tabla sincronizados; `FASE4.md` al dia en `main`.

**Pendiente real para cerrar la fase:** 4.7 (Alumnos sin grupo), 4.8 (inicio del tutor con Agregar hijo), 4.9 (reglas completas) y 4.10 (ciclo completo en dispositivo). No hay ningun bloque 4.1-4.6 pendiente.

**T3 (commits `c24fefa`, `a26fa07`, `15828eb`, `d43354b`, merge `74df0ca`):** `ScanStudentUi.from` valida el payload y rechaza códigos ajenos con «QR no válido» sin escribir nada (2 tests JVM); `ScanStudentScreen` usa CameraX (vista, marco y línea del diseño, linterna y explicación si se niega el permiso) y entrega el payload a una ruta de revisión; botón «Escanear alumno» en el panel del director. CameraX 1.4.0 agregado desde Google Maven (Risk: dependencias nuevas). Verificación: 82 suites / 582 tests / 0 fallos + `assembleDebug` verde. Ya probado en dispositivo: permiso, escaneo real del QR y navegación a revisión.

**Evidencia en dispositivo (T2, 21 nov 2026):** el dueño registró un tutor con correo real, verificó el correo y la app generó el QR del hijo correctamente. El lado directora (T3/T4) se completo y cerro el ciclo de escaneo.

**T2 (commits `8cd167a`, `75e564f`, merge `43f46f0`):** payload versionado `{"v":1,"uid":"...","alumno":"..."}` validado por 3 tests JVM; ZXing `core:3.5.3` agregado al catálogo y resuelto desde Maven Central (Risk: dependencia nueva); `QrCodeImage` dibuja el código sin red; el nombre rechazado se corrige y guarda en `users/{uid}.hijoSolicitado`; `observeLinkingState()` escucha el documento en tiempo real y el ViewModel pone `proceed` cuando llega `vinculado` (2 tests JVM nuevos). Verificación: 81 suites / 580 tests / 0 fallos + `assembleDebug` verde. Verificado en dispositivo: QR legible y salto automatico al vincular.

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
- [x] 4.2 - QR del tutor generado offline con corrección de nombre y escucha en tiempo real (verificado en dispositivo: registro con correo real, verificación y QR generado; recuperación al reabrir también verificada).
- [x] 4.3 - escáner con permiso y validación de QR (código y tests; prueba en dispositivo pendiente).
- [x] 4.4 (online) - alta del alumno y vinculación, con duplicados: verificado en dispositivo.
- [x] 4.4b (T4b) - aprobación sin conexión (verificada en dispositivo: modo avión → mensaje «Se vinculará al volver la señal» → al volver la red el alumno y los enlaces del tutor aparecen en Firestore y el tutor ve al hijo).
- [x] 4.5 - aprobación de docentes con correo verificado y lote (verificado en dispositivo: registro, verificación automática del flag, solicitud en la cola y aprobación; el docente entra a su menú).
- [x] 4.6 - panel del director con contadores (dos cards de acción y hero con círculo de asistencia, según pedido del dueño; tabs se mantienen).
- [x] 4.7 - asignacion de grupo en lote (verificado en dispositivo: asignacion, studentCount y numeros de lista).
- [ ] 4.8 - inicio del tutor con «Agregar hijo» y estado sin grupo.
- [ ] 4.9 - reglas completas desplegadas.
- [ ] 4.10 - ciclo completo probado en dispositivo.

## Riesgos anotados

1. **Persistencia:** migración de esquema y formatos de documento; mitigada con defaults y doble escritura, pendiente de prueba en dispositivo.
2. **Seguridad:** las reglas base ya están desplegadas, pero la aprobación/vinculación completas (4.9) y la prueba con cuentas reales siguen pendientes antes de cerrar la fase.
3. **Permisos:** cámara y notificaciones; la negación del permiso debe explicarse, no bloquear la app.
4. **Sin conexión:** la aprobación offline depende del outbox/WorkManager existente y de que la regla de correo verificado se compruebe al sincronizar.

**Orden al 21 nov 2026 (antes de T7):** 4.1-4.6 completos y en main. 4.2, 4.4, 4.4b y 4.5 verificados en dispositivo; 4.3 verificado en dispositivo (escaneo real probado por el dueno); 4.6 con las dos cards y el círculo de asistencia segun pedido. Todo pusheado a origin/main. Siguiente: 4.7 (Alumnos sin grupo), luego 4.8, 4.9 y 4.10 para cerrar la fase.

**T7 - Alumnos sin grupo (rama `feat/fase4-t7-alumnos-sin-grupo`, commits `e52ba67`, `8239c0f`, merge `3f8ff74`):** pantalla del director con lista de alumnos sin grupo (sólo `groupId` vacío), búsqueda por alumno/tutor, "Seleccionar todos", selectores de grado/grupo derivados de los grupos reales de la escuela y "Asignar a mismo salón". `assignGroup` escribe `GROUP_ID`/`GRADO`/`GRUPO` e incrementa `studentCount` del grupo destino en un batch. La card del panel "Alumnos sin grupo" ahora abre esta ruta. Verificación: 92 suites / 617 tests / 0 fallos + `assembleDebug` verde. Falta prueba en dispositivo: aprobar un alumno "sin grupo" desde el QR, verlo en la lista y asignarle grupo.

**Modelo de grados y grupos (decision del dueno, 21 nov 2026):** grados fijos 1-6, secciones fijas A-D. El id del grupo es DETERMINISTA `grp-{grado}{letra}` (ej. `grp-3b`) y el nombre canonico `"{grado}° {letra}"` (ej. "3° B"). No se elige de una lista de grupos existentes (con base limpia no habia ninguno y no se podia asignar); se decide grado+seccion y el grupo se CREA BAJO DEMANDA con `set(merge)` en el mismo batch de la asignacion, por lo que siempre aparece en GRUPOS y no hay duplicados. Aplica tanto a T7 (Alumnos sin grupo) como a T4 (aprobar QR con grado+grupo crea el grupo y el tutor recibe el `groupId` canonico). Implementado en rama `fix/grupos-id-determinista`: `SchoolCatalog.kt` (4 tests), `assignGroup` con id determinista y get-or-create, `approveStudent` crea el grupo canonico. Verificacion: 93 suites / 622 tests / 0 fallos + assembleDebug verde. Falta merge y prueba en dispositivo.

**Numeros de lista y contador del grupo (rama `fix/student-count-y-lista`):** corregido el `studentCount` que se quedaba en 1: el `set(merge)` del grupo escribia `studentCount: 0` y un segundo `update(increment)` al mismo documento en el mismo batch se ignoraba, asi que cada aprobacion reiniciaba a 0+1=1. Ahora el increment va DENTRO del `set(merge)` (una sola escritura por documento) y se acumula. Ademas: al asignar alumno a un grupo se le da el siguiente numero de lista libre (1..30, tope 30) y el boton "Agregar alumno" del detalle de grupo se reemplazo por "Asignar N° de lista" que reordena el roster por apellido paterno (lastName, luego firstName) con `renumberRoster`. Verificacion: 94 suites / 627 tests / 0 fallos + assembleDebug verde. Falta prueba en dispositivo.

**Fix depermisos en lectura de alumnos y numero al aprobar (rama `fix/student-count-y-lista`):** el boton "Asignar N° de lista" daba `PERMISSION_DENIED` en `students where groupId==grp-1a`: la regla de lectura del director exige `schoolId == mySchool()` y Firestore solo lo puede probar si la query filtra por schoolId. `renumberRoster` y `assignGroup` ahora reutilizan `getStudents()` (la query que si funciona) y filtran por grupo en memoria. Ademas `approveStudent` asigna el siguiente numero de lista libre al aprobar con grado+grupo, para que no se quede en N° 0. Nota: los crashes del escaner en el log eran viejos (timestamps 00:02-01:27), de antes de los fixes d43354b/a842819. Verificacion: 94 suites / 627 tests / 0 fallos + assembleDebug verde. Falta prueba en dispositivo.

**4.8 - Inicio del tutor (rama `feat/fase4-t8-inicio-tutor`):** chip "+ Agregar hijo" al final del selector, abre `LINK_CHILD` para generar un QR nuevo (se reutiliza la pantalla de T2 y su opcion de corregir nombre). Un alumno con `groupId` vacio muestra "Grupo por asignar" en el chip (antes quedaba el texto vacio), y la asistencia sin datos dice "Sin datos aún" en vez de "-". Logica pura en `ParentHomeLabels.kt` con 4 tests. Verificacion: 95 suites / 631 tests / 0 fallos + assembleDebug verde. Falta merge y prueba en dispositivo.

**Flujo de Agregar otro hijo redisenado (rama `fix/qr-add-child-mode`):** `LINK_CHILD` ahora lleva un modo explicito `pending|add` (`Routes.linkChildRoute`). En modo `pending` (primera vinculacion) todo sigue igual: nombre del perfil, QR inmediato, listener que lleva al inicio al aprobar. En modo `add` (desde "+ Agregar hijo"): campo de nombre VACIO, el QR no aparece hasta escribir nombre y tocar "Generar QR", sin escribir en Firestore (el nombre viaja dentro del payload QR), sin listener de vinculacion y con titulo "Agregar otro hijo". Esto corrige ademas el PERMISSION_DENIED al guardar nombre: `hijoSolicitado` no es self-editable segun las reglas, asi que en modo add NO se escribe. Logica pura en LinkChildMode.kt con 4 tests. Verificacion: 97 suites / 638 tests / 0 fallos + assembleDebug verde. Falta prueba en dispositivo.

**Hijo seleccionado compartido (rama `feat/fase4-t8-shared-child`):** nuevo `SelectedChildStore` con DataStore que guarda `uid + studentId` (un tutor distinto en el mismo dispositivo no hereda la seleccion). Los 5 ViewModels del tutor (Home, Asistencia, Tareas, Avisos, Incidentes) consumen la interfaz `ChildSelection`: cambiar de hijo en el inicio actualiza todas las pestañas. Notices ahora filtra los avisos por el grupo del hijo seleccionado (`ParentNoticeItem.groupId`). Ademas: listener post-QR en modo "agregar" (`observeMyChildren`) que vuelve al inicio cuando la directora crea el nuevo alumno, y refresco del home en ON_RESUME para que el 2° hijo aparezca al volver del QR. Verificacion: 99 suites / 645 tests / 0 fallos + assembleDebug verde. Falta prueba en dispositivo.

**Fix: vinculación del tutor al aprobar un 2° hijo (rama `fix/fase4-tutor-links-additive`):** `tutorProfileOnApproval` construye `studentIds`/`groupIds` con `listOf(...)`, y `batch.update(users/{uid}, ...)` los REEMPLAZA. Al aprobar el segundo hijo, `users.groupIds` quedó solo con `grp-2a`; como las reglas scopedean TODA lectura del tutor con `myGroups() = users.groupIds`, `grp-1a` dejó de estar autorizado y `attendanceSessions/tasks/notices where groupId==grp-1a` devolvió PERMISSION_DENIED. `loadHome()` falla si cualquier hijo falla, por eso el inicio no mostraba ninguno aunque `loadChildren()` sí veía ambos. Fix: nuevo `tutorApprovalFirestoreUpdate` convierte ambos arrays a `FieldValue.arrayUnion` en los DOS caminos (online `FirestoreAdminStudentRepository` y replay `StudentApprovalSyncHandler`); el helper puro sigue igual para el payload offline y sus tests. Test RED->GREEN: 2 tests nuevos (`TutorApprovalFirestoreUpdateTest`). Verificación: 100 suites / 647 tests / 0 fallos + assembleDebug verde. Pendiente: reparación puntual del documento `users/{uid}` ya dañado (dejar `groupIds = [grp-1a, grp-2a]` y `studentIds` con ambos alumnos) y prueba en dispositivo.

**Fix: pantalla de "Agregar otro hijo" se abría y cerraba sola en la primera instalación:** `watchForNewChild` comparaba la primera emisión de `observeMyChildren()` contra `ui.knownChildIds`, que se llena asíncronamente en el `init` del ViewModel. En un arranque en frío ese estado todavía estaba vacío, así que el hijo ya existente se contaba como "nuevo", se ponía `proceed = true` y el NavHost volvía al inicio al instante. Al reabrir la app la caché de Firestore ya estaba caliente y el listener emitía antes del tap, por eso funcionaba. Fix: nuevo `NewChildWatcher` usa la primera emisión como baseline cuando no hay baseline provisto, ignora esa primera emisión, y cancela el collector anterior al generar otro QR. Test RED->GREEN: 3 tests nuevos (`NewChildWatcherTest`). Verificación: 101 suites / 650 tests / 0 fallos + assembleDebug verde. Falta probar en dispositivo con instalación limpia.

**Fix definitivo: pantalla de "Agregar otro hijo" se cerraba sola al entrar:** la causa era `AccessStateViewModel.wasLinkedOnEntry`, calculado desde `auth.currentUser?.vinculacion`. Tras aprobar el primer hijo, `observeLinkingState()` actualizaba el `uiState` pero NO el perfil cacheado, así que al entrar en modo `add` el snapshot inicial (`vinculado`) activaba `shouldAutoProceed` y el NavHost volvía al inicio antes de poder escribir el nombre. Se reiniciaba la app → perfil restaurado → funcionaba. Fix: (1) `addMode` se pasa al ViewModel desde el destino `LINK_CHILD` y en ese modo el listener de vinculación NUNCA auto-navega ni limpia un `proceed` ya concedido por el watcher del QR; (2) el baseline de "ya venía vinculado" sale del primer snapshot de Firestore, no del perfil en memoria. Tests RED->GREEN: 2 tests nuevos en `RegisterViewModelTest`. Verificación: 101 suites / 652 tests / 0 fallos + assembleDebug verde. Falta prueba en dispositivo con instalación limpia.

---

### Cierre de las correcciones del tutor — verificado en dispositivo (2026-11-21)

El usuario verificó en dispositivo, con instalación limpia y cuenta recreada desde cero, que:
- se puede agregar un segundo hijo desde "+ Agregar hijo" (la pantalla ya no se abre y se cierra sola);
- el hijo se selecciona desde el inicio y esa elección gobierna la información de todas las pestañas;
- los grupos de ambos hijos quedan en `users/{uid}.groupIds` y no se produce `PERMISSION_DENIED`.

Aclaración histórica: la nota anterior sobre `NewChildWatcher`/`watchForNewChild` fue un diagnóstico erróneo y NO fue la causa del cierre de la pantalla; ese rebote ocurre al entrar, antes de generar el QR. La causa real y su fix son los de la nota "`addMode` + baseline del primer snapshot". El `NewChildWatcher` se conserva porque evita un falso positivo cuando el listener tarda en emitir, pero no resolvía el síntoma reportado.

Estado final: `main` en `cf72d43` (merge `fix/fase4-add-mode-auto-proceed`), sin commits pendientes de push. Verificación automatizada final: 101 suites / 652 tests / 0 fallos + `assembleDebug` verde. Quedan para cerrar la Fase 4: 4.9 (reglas completas) y 4.10 (ciclo completo en dispositivo).

**Asignación de grupo y materia al docente (rama `feat/fase4-docente-grupo-materia`):** se reemplaza el campo libre "Materia o grado que impartes" por dos selectores (grado 1..6 y sección A..D → `grp-{grado}{letra}`) y un selector de las seis materias fijas (español, matemáticas, lectura, historia, geografía, ciencias) en `SchoolCatalog`. El registro guarda solo la INTENCIÓN (`requestedGroupId` + `requestedSubject`) y avisa si esa materia ya está ocupada en el grupo (`groups.subjectTeachers`). La aprobación del director sigue decidiendo solo si la persona entra, pero en la MISMA transacción sobre `groups/{id}` aplica la elección: `subjectTeachers[materia] = uid`, `teacherIds` con `arrayUnion`, y en `users/{uid}` escribe `groupIds`, `subjects` y `estado = aprobado`. Si la materia fue tomada mientras la solicitud esperaba, la aprobación falla con `SUBJECT_TAKEN` y la solicitud queda pendiente: no hay decisión humana sobre grupo/materia. `acceptAllPendingTeachers` usa el mismo camino. Las cuentas de docente creadas por el director también escriben la intención en vez de `groupIds` directo. Reglas: nueva rama estrecha que permite al docente pendiente corregir solo `requestedGroupId`/`requestedSubject`/`subjects`/`updatedAt`, sin `groupIds`. Verificación: 103 suites / 661 tests / 0 fallos + assembleDebug verde. Pendiente: desplegar `docs/firestore.rules`, backfill manual de docentes existentes y prueba en dispositivo.

**Ajuste antes del merge: catálogo de materias por grado:** las materias NO son las mismas en los seis grados. `SchoolCatalog.SUBJECTS_BY_GRADE` define las seis materias de cada grado (36 en total, IDs canónicos `g{grado}-{slug}`), y `subjectsForGrade(grado)` alimenta el selector del registro. Cambiar el grado limpia la materia elegida y vuelve a consultar las materias ocupadas del nuevo grupo. `teacherAssignmentError` y `teacherApprovalPlan` rechazan una materia de otro grado, así que no se puede aprobar una asignación incoherente. Verificación: 103 suites / 662 tests / 0 fallos + assembleDebug verde.

---

### Verificación en dispositivo — asignación de docente (2026-11-22)

El usuario verificó en dispositivo que el flujo completo funciona: registro con grado + sección + una de las seis materias **de ese grado**, aviso de materia ocupada, aprobación de la directora sin elegir grupo/materia, y panel del docente mostrando el grupo y la materia asignados.

Estado operativo real (aclaración de las notas anteriores, que quedaron desactualizadas):
- Las materias **no** son seis fijas para todos los grados: son 6 por grado, 36 en total (nota "Ajuste antes del merge").
- `docs/firestore.rules` fue desplegado y compilado correctamente en el proyecto `aulasegu` (`firebase deploy --only firestore:rules --project aulasegu`).
- No existían docentes en Firestore, así que no hubo backfill.
- Verificación automatizada final: 103 suites / 662 tests / 0 fallos + `assembleDebug` verde.
- Código en `main` desde el merge `4f1a47b`.
