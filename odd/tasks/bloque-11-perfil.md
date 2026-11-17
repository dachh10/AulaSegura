# Bloque 11 — Perfil del docente

Branch `feat/docente-pase-lista` · entrega `single-pr` · tests `./gradlew.bat testDebugUnitTest` · build `./gradlew.bat assembleDebug` · `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

Registro oficial del bloque: `FASE1.md` §Bloque 11. Este archivo es el logbook de ejecución.

## Specs

**S1** — FASE1.md:1012: `- [ ] **11.1** `TeacherProfileScreen` con datos reales (`fullName`, `email`, `schoolId`, `subjects`)`

**S2** — FASE1.md:1013: `- [ ] **11.2** Saludo del `TeacherHomeScreen` desde `fullName` (cierra el pendiente de la Fase 0)`

**S3** — FASE1.md:1014: `- [ ] **11.3** Foto: comprimir + subir a `aulasegura/profiles/{uid}` + escribir `photoUrl` y `photoPublicId``

**S4** — FASE1.md:1015: `- [ ] **11.4** Foto visible en la UI y persistida entre sesiones (Coil)`

**S5** — FASE1.md:1016: `- [ ] **11.5** Logout y cierre forzado por `active = false` siguen funcionando`

**S6** — FASE1.md:1018-1019: `**Criterio:** el docente cambia su foto, cierra la app, vuelve a entrar y la foto sigue ahí; el saludo muestra su nombre real.`

**Restricción del dueño** — `empieza con el bloque 11, solo una aclaracion si tienes conectado el dispositivo pero es uno diferente al que esta usando , por si vas a hacer purbas con el para que sepas.` Se respeta sin usar dispositivos: verificación con `assembleDebug` + tests unitarios y checklist para el dueño.

## Tasks

- T1 — S3 — inline — **hecho** — `AppUser.photoPublicId`, `AuthRepository.updateProfilePhoto`, `ProfilePhotoMapper` y fakes de test. Sin migración Room: `photoPublicId` no se cachea.
- T2 — S4 — inline — **hecho** — Coil 2.7.0 (`io.coil-kt:coil-compose`) en `libs.versions.toml` + `app/build.gradle.kts`; resuelto offline.
- T3 — S1,S3 — inline — **hecho** — `TeacherProfileViewModel` (cámara/galería, compresión, subida Cloudinary, escritura del perfil) + factory en `AppContainer`.
- T4 — S1,S4 — inline — **hecho** — `TeacherProfileScreen` real: datos del perfil, avatar con `AsyncImage`/fallback a iniciales, selector de foto, `subjects`; cableado en `AulaSeguraNavHost`.
- T5 — S2 — inline — **hecho** — Saludo y avatar de `TeacherHomeScreen` desde el perfil de la sesión, propagados desde el NavHost.
- T6 — S5,S6 — inline — **hecho** — Tests de mapper/UI, `FASE1.md` con los 5 ítems y el dashboard, `CONTEXT.md`, verificación (227 tests) y commit del bloque.

Route: inline en las 6 tareas. Ningún trigger de delegación se cumple: una sola unidad de comportamiento (perfil del docente) con superficies acotadas y el mismo escritor; no hay dos unidades independientes paralelas ni backstop de contexto.

## Log

**L1** — 17 nov 2026: `empieza con el bloque 11, solo una aclaracion si tienes conectado el dispositivo pero es uno diferente al que esta usando , por si vas a hacer purbas con el para que sepas.`

**L2** — Decisión de diseño: `photoPublicId` se escribe en Firestore y se expone en `AppUser`, pero **no** se guarda en `ProfileEntity`. Evita la migración v3→4; el criterio de cierre solo necesita `photoUrl`, que ya tiene columna. Pendiente documentado: al restaurar desde caché sin red, `photoPublicId` es `null`; no afecta mostrar la foto ni generar uno nuevo.

**L3** — Decisión de diseño: la cámara y la galería alimentan el mismo flujo (`ImageCompressor.compress(Uri)` → `CloudinaryUploader.uploadImage(..., CloudinaryFolder.PROFILES, uid)` → `AuthRepository.updateProfilePhoto`). La URL del avatar no pasa por la outbox: es una escritura pequeña y el perfil se recompone al reabrir la app.

**L4** — `adb` no está en PATH en este entorno; no se ejecuta ninguna prueba en dispositivo. La verificación en teléfono queda para el dueño, con checklist al cierre del bloque.

**L5** — 17 nov 2026: `assembleDebug` verde y `testDebugUnitTest` con 38 suites / 227 tests / 0 fallos / 0 errores (eran 219 antes del bloque). Dos correcciones durante la implementación: `AlertDialog` no acepta `neutralButton` en este BOM de Material3 (se usa el overload con `content` y `@OptIn(ExperimentalMaterial3Api::class)`), y `teacherGreeting("")` dejaba un espacio final (se filtran los segmentos en blanco).

**L6** — 11.5 verificado por readback: `SessionViewModel.signOut()` y `observeAccountActive().firstOrNull { !it }` siguen intactos; el logout del perfil llama al mismo `sessionViewModel.signOut()` de siempre. La desactivación remota no se probó en dispositivo.

**L7** — 17 nov 2026, corrección visual tras la prueba del dueño: el `AlertDialog` con overload de `content` no dibujaba su contenedor y las opciones quedaban superpuestas sobre la pantalla. Se reemplaza por `Dialog` + `Surface` opaca con dos botones de ancho completo y cancelar; el resto del bloque funcionó en el teléfono. `assembleDebug` y 227 tests siguen verdes.

---

## Extensión — las cuatro filas del perfil

**S7** — Usuario: `ya solo en la parte de perfil necesito que le des funcionalidad a todo lo de "editar perfil", "cambiar contraseña", "permisos de la pp", "notificaciones".`

**S8** — Editar perfil: cambiar `fullName` y `subjects` en `users/{uid}`, con validación y reflejo inmediato en la caché Room y en el saludo.

**S9** — Cambiar contraseña: contraseña actual + nueva + confirmación, con reautenticación de Firebase antes de `updatePassword`.

**S10** — Permisos de la app: la fila abre una pantalla real que consulta el estado de los permisos del sistema y permite concederlos o abrir Ajustes.

**S11** — Notificaciones: la fila muestra el estado real del permiso `POST_NOTIFICATIONS` (API 33+) y permite pedirlo o abrir los ajustes de notificaciones de la app.

**T7** — S8,S9 — inline — **hecho** — Contrato y mapper: `updateProfile`, `changePassword`, resultados tipados, implementación con reautenticación y fake de tests.
**T8** — S8 — inline — **hecho** — Diálogo de edición de nombre y materias cableado al ViewModel y a la fila.
**T9** — S9 — inline — **hecho** — Diálogo de cambio de contraseña cableado al ViewModel y a la fila.
**T10** — S10 — inline — **hecho** — `PermissionsScreen` real (estados consultados + Intents) y ruta conectada desde el perfil docente.
**T11** — S11 — inline — **hecho** — Estado real de notificaciones en la fila, request de permiso y apertura de Ajustes.
**T12** — S7..S11 — inline — **hecho** — Tests, documentación, verificación y commit.

**L8** — Decisión: `preferences.notificationsEnabled` no se escribe. No hay ningún lector en `data/`, `domain/` ni en el script de envío, y sin Cloud Functions el push es manual desde la consola: escribir el flag sería cosmético. La fila de notificaciones controla el permiso del sistema, que es lo que sí determina si el teléfono suena.

**L9** — Decisión: "Permisos de la app" reutiliza `Routes.PERMISSIONS` en vez de crear otro diálogo, pero esa pantalla era decorativa (`PermissionsScreen.kt:105` decía "no real runtime permission calls"); se hace real con `ContextCompat.checkSelfPermission` e Intents de Ajustes. No se muestra cámara porque el manifest no declara `CAMERA`: el avatar usa `TakePicture`/`GetContent`.

**L10** — 17 nov 2026: verificación verde con `assembleDebug` y 39 suites / 234 tests / 0 fallos / 0 errores (eran 227). Un error de compilación durante la implementación: `Modifier.align` solo resuelve dentro de `BoxScope`; el botón de cancelar pasó a ancho completo. Quedan dos warnings de deprecación (`LocalLifecycleOwner` de `ui` y `Icons.Outlined.ArrowBack`), ambos preexistentes en patrón y sin impacto funcional.
