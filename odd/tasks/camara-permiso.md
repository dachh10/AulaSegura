# Feature: cámara que no abre desde ninguna pantalla

Rama: `fix/camara-permiso` · Estrategia: un solo PR (`single-pr`) · Runner: `gradlew testDebugUnitTest`

## Specs

**S1 — "Tomar foto" abre la cámara en los tres roles.** El pedido textual del usuario:
> «cuando quieres poner una fotografia en perfil y seleccionar "Tomar foto" no hace nada,
> en ninguno de los tres roles»

Perfil de tutor, docente y directora (y el registro) usan el mismo picker compartido; tocar
"Tomar foto" debe abrir la cámara del sistema y devolver la foto al formulario.

**S2 — La evidencia de un incidente se puede fotografiar.** El pedido textual:
> «en incidencias ... que es tomar una foto tampoco deja»

El botón de evidencia de `TeacherIncidentNewScreen` debe abrir la cámara y guardar la foto real.

**S3 — El comprobante de una justificación se puede fotografiar.** El pedido textual:
> «cuando justificas una falta que es tomar una foto tampoco deja»

El botón "Tomar foto" de `ParentJustifyScreen` debe adjuntar la foto del comprobante.

**S4 — Ningún fallo de cámara vuelve a ser silencioso.** El pedido textual:
> «lo que sea de tomar foto no lo hace»

Si la cámara no puede abrirse, no devuelve foto o el permiso fue negado, la pantalla lo dice
con un mensaje visible en lugar de quedar como un botón muerto.

**S5 — La tarjeta de Cámara de la pantalla de permisos dice la verdad.** La app declara
`android.permission.CAMERA` para los escáneres QR, así que la tarjeta debe pedir el permiso y
reflejar su estado real, no mostrarlo concedido de antemano.

## Tasks

- **T1** — Gate compartido de permiso CAMERA (S1, S4): nuevo `CameraAccess.kt` con
  `rememberCameraPermissionGate`, usado por `rememberProfilePhotoPicker`; los errores del
  picker se muestran con Toast cuando la pantalla no pasa uno. Commit `T1`.
- **T2** — Incidentes y justificaciones (S2, S3, S4): ambas pantallas pasan por el gate y
  avisan cuando la cámara no devuelve la foto. Commit `T2`.
- **T3** — Pantalla de permisos (S5): tarjeta de Cámara con estado real y solicitud efectiva. Commit `T3`.
- **T4** — Verificación: `testDebugUnitTest` + `assembleDebug`, APK firmado para probadores. Commit `T4`.

## Log

**L1** — (2026-11-22) Pedido del usuario, textual:

> «Mira ya probe y sigue sin funcionar cuando quieres poner una fotografia en perfil y seleccionar "Tomar foto" no hace nada, en ninguno de los tres roles, y en incidencias y cuando justificas una falta que es tomar una foto tampoco deja, lo que sea de tomar foto no lo hace, necesito que corrijas eso.»

**L2** — Causa raíz encontrada leyendo el repo: el manifiesto declara
`android.permission.CAMERA` desde `15828eb` (escáner QR con CameraX), pero nada la pide en
tiempo de ejecución. Android rechaza `ACTION_IMAGE_CAPTURE` cuando la app declara CAMERA y no
la tiene: el lanzador falla en el acto. En el perfil el fallo lo tragaba
`runCatching { ... }` con `onError = {}` por defecto, de ahí el botón muerto; en incidencias se
borraba el archivo y no se avisaba; en justificaciones no había manejo alguno. La pantalla de
permisos, además, mostraba la tarjeta de Cámara como concedida (`onGrant = {}`).

**L3** — `CameraAccess.kt` añadido; picker compartido, incidentes y justificaciones usan el
gate y muestran un Toast cuando algo falla (mensajes en español, sin cambiar identificadores).

**L4** — `PermissionsScreen`: `cameraLauncher` real, `refreshStates` incluye CAMERA, la tarjeta
pasa a `onGrant = ::onCameraClick` y la nota deja de afirmar que no hace falta permiso.

**L5** — T4: `clean testDebugUnitTest assembleDebug` GREEN (3m 6s). El APK ensamblado contiene
las dos cadenas nuevas (`Permite el acceso a la c…`) y `rememberCameraPermissionGate` en sus
dex, así que el binario entregado incluye el arreglo. Verificación en dispositivo pendiente: la
corrección solo se puede confirmar en un teléfono real, porque el fallo dependía del estado del
permiso en el dispositivo. No hay test unitario posible: el comportamiento vive en el diálogo
del sistema y en el registro de resultados de actividad, que no corre en JVM.

**L6** — El verificador independiente no pudo lanzarse: el runtime respondió
`permission.rejected: subagent`. Se preserva ese resultado como no disponible; la revisión
inline quedó en la lectura del diff y en el build. Risk: item 2 (permisos en tiempo de
ejecución, tocados por el gate de cámara y la pantalla de permisos).
