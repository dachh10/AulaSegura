# Bloque 17 — Pantallas de perfil según Stitch

Branch `main` · entrega `single-pr` · tests `./gradlew.bat testDebugUnitTest` · build `./gradlew.bat assembleDebug` · `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

Registro oficial del bloque: `FASE1.md` §Bloque 11 (extensión B17). Este archivo es el logbook de ejecución.

## Specs

**S1** — Usuario: `has las pantallas de perfil segun stitch, de esas pantallas solamente usa lo necesario no traigas todo de ellas, solo lo que de verdad este en el proyecto y podamos usar.`

**S2** — "Editar perfil" deja de ser un diálogo y pasa a ser una pantalla completa inspirada en el diseño de Stitch: encabezado con volver, nombre completo, materias, datos asignados por la escuela en solo lectura y guardar/cancelar.

**S3** — "Cambiar contraseña" deja de ser un diálogo y pasa a ser una pantalla completa: contraseña actual, nueva y confirmación, con un checklist que refleja **las mismas reglas que se validan** antes de llamar a Firebase.

**S4** — No se inventan campos ni promesas: cada dato mostrado existe en `AppUser`, en Firestore o en Room.

## Tasks

**T1** — S2,S4 — inline — **hecho** — `ProfileSection` en vez de `ProfileDialog`; `passwordRules` y `passwordStrengthLabel` comparten predicados con `passwordChangeError`.
**T2** — S2 — inline — **hecho** — `TeacherEditProfileScreen`: nombre y materias reales, correo y escuela en solo lectura.
**T3** — S3 — inline — **hecho** — `TeacherChangePasswordScreen`: tres campos reales y checklist con cada regla validada.
**T4** — S2,S3 — inline — **hecho** — Los diálogos se eliminaron; las pantallas se muestran a pantalla completa dentro del `Box` del perfil y comparten su ViewModel.
**T5** — S1..S4 — inline — **hecho** — 3 tests nuevos de reglas/checklist, `FASE1.md` §11.10-11.11, verificación y commit.

## Log

**L1** — 18 nov 2026, pedido original: `pushea el commit de docs, y una vez hecho eso, has las pantallas de perfil segun stitch, de esas pantallas solamente usa lo necesario no traigas todo de ellas, solo lo que de verdad este en el proyecto y podamos usar.` El push de docs (`c9d849a`) se hizo antes de arrancar el bloque.

**L2** — Diseños consultados en Stitch (proyecto AulaSegura): "Editar Perfil (Docente)" `2e72df45b9cb43dab7d35168f6509694` y "Cambiar contraseña" `a4421d1b26514531a35705e8a723e21f`.

**L3** — Decisión: se toma del diseño la estructura (encabezado con volver, tarjetas de datos, botones, checklist de contraseña) y se descarta lo que el proyecto no puede sostener:
- `Nombre(s)` y `Apellidos` separados: `AppUser` solo modela `fullName`. Se usa un único campo "Nombre completo".
- `Teléfono`: está en el esquema de `users/{uid}` pero no en `AppUser` ni en la caché Room; agregarlo sería otro cambio de contrato.
- La nota "Si no hay internet, tus cambios se guardarán y se enviarán después": el perfil escribe directo en Firestore y **no** pasa por la outbox, así que sería falsa.
- "Cambiar foto" dentro de la pantalla de edición: ya existe en el perfil; duplicar el selector sería trabajo muerto.
- "Grupos" como chips: los grupos reales viven en "Mis grupos" y los asigna la directora; no se editan desde el perfil. La sección de datos asignados muestra correo y escuela, que sí son reales y son inmutables por las reglas.

**L4** — Decisión: el checklist de la contraseña no es decorativo. Se adoptan las reglas que el propio diseño anuncia (mínimo 8 caracteres, mayúscula, número; símbolo solo recomendado) y `passwordChangeError` pasa a exigirlas, para que cada marca verde signifique algo. "Coinciden" se calcula de nueva + confirmación.

**L5** — Decisión: las pantallas se renderizan dentro de `TeacherProfileScreen` como contenido a pantalla completa, no como rutas nuevas. Así se comparte el mismo `TeacherProfileViewModel` (estado del formulario, `saving` y `message`), no se duplica el factory y no se necesita recargar el perfil al volver.

**L6** — Decisión: se conserva el selector de foto y la fila de notificaciones tal como estaban; este bloque solo cambia las dos "ventanas chafas" que señaló el dueño.

**L7** — 18 nov 2026: verificación verde con `assembleDebug` y **52 suites / 381 tests / 0 fallos / 0 errores** (eran 379). Dos errores de compilación corregidos durante la implementación: `KeyboardOptions` se importa desde `androidx.compose.foundation.text` en este BOM, no desde `ui.text.input`. Hubo que quitar el bloque de diálogos completo del archivo del perfil (180 líneas) para no dejar código muerto. Sin dispositivo operado por el agente: la prueba visual la hace el dueño.

**L8** — 19 nov 2026, revisión pedida por el dueño: traer el diseño de Stitch **exactamente**, con animaciones y el efecto de carga de las otras pantallas del docente. Se bajó el HTML real de los dos screens y se portó la estructura: barra superior con botón circular y badge, avatar de 104 dp con badge coral de cámara, campos de 52/56 dp con icono y foco primario, tarjeta institucional `PrimaryFixed/40` con marca de agua de escudos, fila "Grupos" con chips reales del roster, dock inferior fijo con botón coral e icono, toast superior, patrón de escudos al 4 % en contraseña, ícono de candado de 112 dp, barra de fuerza de 4 segmentos, píldora "Coinciden" y checklist con radio button. Las entradas escalonadas usan el mismo `Reveal`/`revealOnEnter` que `OnboardingScreen.kt`, y los botones usan `rememberPressScale`.

**L9** — Revisiones de exactitud dentro del port: se descartó la barra de estado simulada del mockup (el teléfono ya tiene la real) y se cambió el aviso "se guardará sin internet" por "Los cambios se guardan en tu cuenta", porque el perfil escribe directo en Firestore y no usa la outbox. Los grupos sí se trajeron: `TeacherProfileViewModel` observa `GroupRepository.observeMyGroups()` y la tarjeta pinta los reales (o "Sin asignar"). Los datos de ejemplo del HTML (`Ricardo`, teléfono, correo) se reemplazan por `ui.fullName`, `ui.subjects`, `ui.email`, `ui.groups` y `ui.schoolId`. `visible` por campo permitió traer los botones de mostrar/ocultar contraseña del diseño.

**L10** — 19 nov 2026, corrección visual tras la captura del dueño: la tarjeta "Datos asignados por la escuela" mostraba un recuadro raro porque el escudo de marca de agua de 96 dp se salía de la esquina y las filas eran semitransparentes (`Color.White.copy(alpha = 0.8f)`), dejando ver el patrón del fondo. Se quitó el escudo de esa tarjeta y las filas pasan a blanco opaco; el patrón de escudos sigue solo en la pantalla de contraseña, que es donde lo pone Stitch. `assembleDebug` y 381 tests siguen verdes.

**L11** — 19 nov 2026, segunda corrección: el dueño reportó que el recuadro gris seguía igual. La captura mostraba además el patrón de escudos en "Editar perfil", que en el código actual solo se dibuja en la pantalla de contraseña: el dispositivo estaba corriendo una build anterior. Igual se eliminó el fondo `PrimaryFixed/40` de la tarjeta: ahora es `SurfaceContainerLowest` con borde suave y las filas son `SurfaceContainerLow`, para que no quede ningún bloque gris. Hay que hacer *Clean Project* y volver a correr, o desinstalar la app, para que el teléfono tome esta versión. `assembleDebug` y 381 tests verdes; APK nuevo a las 19:10.
