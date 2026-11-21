# Bloque 3.10 — Perfil del tutor y foto

Rama: `main` · entrega: commit en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantalla: `ParentProfileScreen` (Stitch `7ad5b00011fe438390ac923e9e0d28e6`, bloque 2.13)

## Specs

**S1 — La maqueta se fue del perfil.** `ParentProfileScreen` ya no dibuja «Carlos Gómez Pérez», `carlos.gomez@familia.mx» ni la inicial «C» fijo. `ParentProfileViewModel` resuelve el perfil de la sesión con `AuthRepository.currentUser` (la misma fuente que la pantalla del docente) y la pantalla sólo pinta el estado: nombre, correo y avatar reales, con las iniciales del nombre cuando no hay foto.

**S2 — «MIS HIJOS» son los hijos del roster.** La tarjeta fija con «Mariana» y «Diego» se fue. Los hijos salen de `ParentRepository.loadChildren()`, la misma lectura que usa el inicio del tutor. Hay tres estados honestos: cargando, error de lectura con el mensaje del fallo (`homeFailureMessage`) y cuenta sin hijos vinculados («Esta cuenta todavía no tiene hijos vinculados.»). Nunca una tarjeta inventada.

**S3 — Editar perfil escribe sólo lo que el tutor posee.** El formulario editable es el nombre completo; el correo queda bloqueado con la nota de que lo cambia la administración. `AuthRepository.updateProfile` escribe `fullName` y `subjects`; un tutor no tiene materias, así que viajan vacías y no tocan nada de la escuela. Las reglas ya lo permiten: `users/{uid}` deja editar a cualquier usuario activo sobre sí mismo con `changedKeys().hasOnly(selfEditableUserKeys())` (`docs/firestore.rules:205-219`).

**S4 — Cambiar contraseña es el mismo formulario que el del docente.** La política de contraseña, el checklist y la pantalla viven ahora en `ui/profile` (`ProfileFormRules.kt`, `ChangePasswordScreen.kt`), y el docente los consume igual que el tutor. Un solo lugar decide qué contraseña acepta la app; dos copias se desincronizan sin que nadie lo note.

**S5 — La foto se sube, se guarda y se ve.** El avatar sigue la ruta del docente: `ImageCompressor` reduce la imagen, `CloudinaryUploader` la sube a `aulasegura/profiles/{uid}` y `AuthRepository.updateProfilePhoto` escribe `photoUrl` y `photoPublicId` en `users/{uid}`. La cámara usa el `FileProvider` y la caché propia (`perfil/`); la galería usa `GetContent`.

**S6 — Notificaciones es el permiso del sistema, no un interruptor en memoria.** La fila ya no conserva un `mutableStateOf(true)` que nadie puede cambiar desde el sistema operativo. Refleja `POST_NOTIFICATIONS` (Android 13+) o `areNotificationsEnabled()`; al tocarla pide el permiso o abre los ajustes de notificaciones de la app, y un `DisposableEffect` sobre `ON_RESUME` vuelve a leer el estado cuando el tutor regresa.

**S7 — Tests y documentación.** Tests JVM del mapeo de la UI, del ViewModel del tutor (hijos, nombre, contraseña) y del flujo de avatar compartido; `FASE3.md`, `CONTEXT.md` y este logbook al día en el mismo commit.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S1, S2 | Estado de la pantalla, mapeos de perfil e hijos | `ui/screens/parent/ParentProfileUi.kt` | ✅ |
| T2 | S1, S2, S3, S4, S5 | ViewModel del tutor: hijos, avatar, nombre, contraseña | `ui/screens/parent/ParentProfileViewModel.kt` | ✅ |
| T3 | S1, S2, S6 | Pantalla real sobre el diseño 2.13, sin maqueta | `ui/screens/parent/ParentProfileScreen.kt` | ✅ |
| T4 | S3 | Formulario de editar perfil del tutor | `ui/screens/parent/ParentProfileSections.kt` | ✅ |
| T5 | S4 | Reglas de cuenta y formulario de contraseña compartidos | `ui/profile/ProfileFormRules.kt`, `ui/profile/ProfileFormKit.kt`, `ui/profile/ChangePasswordScreen.kt` | ✅ |
| T6 | S5 | Flujo de avatar compartido y compresor como interfaz | `ui/profile/ProfileAvatarUpload.kt`, `data/remote/cloudinary/ImageCompressor.kt` | ✅ |
| T7 | S7 | Tests de UI, ViewModel y avatar | `ui/screens/parent/ParentProfileUiTest.kt`, `ParentProfileViewModelTest.kt`, `ui/profile/ProfileAvatarUploadTest.kt` | ✅ (3 suites, 20 tests) |
| T8 | S1–S7 | Ruta con ViewModel y factory | `navigation/AulaSeguraNavHost.kt`, `di/AppContainer.kt` | ✅ |
| T9 | S7 | Docs: fase, contexto y logbook | `FASE3.md`, `CONTEXT.md`, `odd/tasks/bloque-3.10-perfil.md` | ✅ |

## Log

**L1 — 20 nov 2026, pedido del dueño.** «Implementar el bloque 3.10 — Perfil del tutor y foto (con los ajustes reales que ya tiene el docente) de AulaSegura, tras cerrar 3.9.»

**L2 — 20 nov 2026, decisiones de ingeniería.**

- **El perfil del tutor es el del docente, sin copia.** La validación de nombre, la política de contraseña, los campos, el dock de guardado y la pantalla de contraseña se movieron a `ui/profile`, y las dos pantallas los consumen. Lo que el tutor **no** tiene son las materias: el formulario del tutor edita sólo el nombre y `updateProfile` viaja con `subjects` vacío.
- **Las reglas ya permiten la escritura; no hubo que tocarlas.** `users/{uid}` deja que el propio usuario active edite `fullName`, `subjects`, `photoUrl` y `photoPublicId` (`docs/firestore.rules:111-123, 205-219`). El tutor no necesita una excepción nueva.
- **La fila de notificaciones se ató al sistema operativo.** El interruptor en memoria era una mentira: el push se manda desde la consola, así que lo único que decide si el teléfono suena es el permiso de Android. La fila ahora lo lee, lo pide en Android 13+ y abre los ajustes de la app cuando hace falta.
- **«Estado de sincronización» dejó de ser una fila que no llevaba a ningún lado.** El chevron apuntaba a un `onOpenSyncStatus` sin ruta. Ahora es un dato, igual que en la pantalla del docente: la insignia «Sincronizado» sin flecha.
- **La foto se movió a un helper sin `Uri`.** `uploadProfileAvatar` recibe los bytes ya comprimidos y se ocupa de la subida y de la escritura del perfil; el `Uri` sólo aparece en el ViewModel, porque la compresión necesita un `Context` de Android. Así el flujo de avatar se puede testear en la JVM. `ImageCompressor` pasó a implementar `PhotoCompressor`, igual que `CloudinaryUploader` es una interfaz desde el principio.
- **El estado vacío es honesto.** Sin hijos vinculados la pantalla lo dice; con un fallo de red muestra el mensaje del fallo. Un hijo de mentira en la tarjeta es una mentira que la familia no puede corregir desde la app.
- **El diseño se respetó.** Header, «MIS HIJOS», tarjeta de ajustes, logout y versión siguen la maqueta 2.13 (crema, índigo, badge de sincronizado, tarjetas de hijos con dos tonos alternados). Lo que cambió fue de dónde salen los datos, no cómo se ven.
- **Apareció un bug del docente al compartir el kit.** El dock del diseño (`StitchDock`) no tenía `clickable`: en la pantalla del docente, «Guardar cambios» y «Actualizar contraseña» no guardaban nada. Se arregló en el kit compartido y el guardián de `saving` evita el doble envío. El botón del tutor nunca habría funcionado sin ese arreglo.
- **La tarjeta de un hijo dejó de fingir que es un botón.** Desde el perfil no hay un destino por hijo (la pantalla de asistencia elige el suyo), así que la tarjeta se ve igual pero no se hace pasar por control.

**L3 — 20 nov 2026, verificación.**

- **Tests nuevos (3 suites, 20 tests; total 78 suites / 568 tests / 0 fallos):** `ParentProfileUiTest` (5) — perfil de la sesión, foto ausente, nombre y grupo del hijo, nombre de reserva e inicial legible; `ParentProfileViewModelTest` (9) — hijos del roster, cuenta sin hijos, fallo de red, escritura del nombre sin tocar materias, nombre en blanco, escritura rechazada, contraseña débil, contraseña cambiada y contraseña incorrecta; `ProfileAvatarUploadTest` (6) — carpeta y archivo del avatar, escritura del `publicId`, foto ilegible, error de red, subida rechazada y escritura rechazada.
- **El docente no se rompió:** `TeacherProfileUiTest` (12) sigue verde después de mover las reglas y la pantalla de contraseña a `ui/profile`.
- **Build:** `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**, `app-debug.apk` generado.
- **Pendiente en dispositivo:** entrar con la cuenta real del tutor y comprobar que el nombre, el correo y los hijos son los de la cuenta; cambiar el nombre y ver que persiste al volver a abrir; subir una foto (cámara y galería) y ver que aparece en el header y sobrevive al reinicio; tocar «Notificaciones» y comprobar que pide el permiso o abre los ajustes; cambiar la contraseña y entrar de nuevo con la nueva.
