# Bloque 3.1 — Registro del tutor con código de alumno

Rama `main` · entrega `single-pr` · tests `./gradlew.bat testDebugUnitTest` · build `./gradlew.bat assembleDebug` · `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

Registro oficial: `FASE3.md` §3 y §4. Este archivo es el logbook de ejecución.

## Specs

**S1** — Usuario: `el tutor se registra en la app y pide como campo obligatorio el codigo del alumno, solo poniendo el codigo del alumno se puede registrar de manera correcta e inicia sesion en las pantallas de tutor`.

**S2** — Usuario: `solo el padre o tutor se puede registrar, no se puede registrar ni directivo ,i docente`.

**S3** — `RegisterScreen` (Stitch `4ed8ecd6ef044004bce46d44e15fc520`) pasa de pantalla visual a registro real: todos sus campos se validan y el código del alumno es obligatorio.

**S4** — El registro crea la cuenta Auth, el documento `users/{uid}` con `role: "tutor"`, el vínculo en `students.tutorIds` y el canjeo de `linkingCodes/{code}` en un solo lote atómico. Al terminar, la app entra en `Routes.PARENT_HOME` con la sesión ya resuelta.

**S5** — Las reglas de Firestore se amplían de forma acotada: nadie puede autoasignarse `role: "directivo"` ni `"docente"`, y un código solo se puede canjear una vez.

## Tasks

**T1** — S4,S5 — inline — **hecho** (sin publicar) — Reglas: `users.create`, `students.update` y `linkingCodes.update` con `getAfter()` sobre un lote atómico. Dry-run del CLI: `rules file compiled successfully`.
**T2** — S1,S3 — inline — **hecho** — Tipos y validación pura del registro (incluido el formato real de 6 caracteres).
**T3** — S4 — inline — **hecho** — `registerTutor` en `AuthRepository`/`AuthRepositoryImpl`: Auth + lote de 3 escrituras + perfil resuelto y cacheado.
**T4** — S3 — inline — **hecho** — `RegisterViewModel` y `RegisterScreen` conectados; mensajes de error y estado de carga.
**T5** — S2 — inline — **hecho** — Login con acceso a "Crea tu cuenta" y ruta `Routes.REGISTER` cableada; la sesión entra como tutor.
**T6** — S1..S5 — inline — **hecho** — 8 tests nuevos (389 totales), `FASE3.md`, verificación y commit.

## Log

**L1** — 18 nov 2026: aprobación del usuario para arrancar 3.1 y corrección del flujo de registro: solo el tutor se registra desde el Login con código obligatorio; el directivo crea docente y alumnos, nunca tutor.

**L2** — Hallazgo: `RegisterScreen.kt` existe pero **no está cableado** en `AulaSeguraNavHost.kt` (no hay `composable(Routes.REGISTER)`) y `LoginScreen` no tiene acceso a él. El bloque incluye crear la ruta y el enlace.

**L3** — La pantalla de Stitch de registro ya trae el campo "Código del alumno · Requerido por plantel" con ejemplo `ALU-2024-9821`. El código **real** que genera la Fase 2 tiene 6 caracteres del alfabeto `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (`AdminStudentRepository.LINKING_CODE_LENGTH`), así que se ajustan placeholder y ayuda para no inducir a error.

**L4** — Decisión: el registro se apoya en `getAfter()` porque las reglas actuales no dejan crear `users/{uid}` al propio usuario ni escribir `students.tutorIds`. El lote atómico incluye un campo `registrationCode` en el documento del tutor para que la regla pueda comprobar, sin listar códigos, que el usuario se creó a partir de ese código exacto. Ese campo queda como registro de auditoría y no es editable desde la app.

**L5** — Decisión: `registerTutor` vive en `AuthRepository` porque necesita el mismo `FirebaseAuth` y el mismo `currentUser` que Login y Session; así, después del lote se puede resolver el perfil y dejar la sesión lista sin una segunda implementación del caché Room.

**L6** — 19 nov 2026: verificación verde con `assembleDebug` y **54 suites / 389 tests / 0 fallos / 0 errores** (eran 52/381). Un error de compilación en el test corregido: `registerResult` es una propiedad del fake, no un parámetro del constructor. Las reglas nuevas compilan con `firebase deploy --only firestore:rules --dry-run --project aulasegu` → `rules file compiled successfully`. **No se publicaron**: publicar reglas de seguridad que permiten autocrear cuentas es un cambio de producción y requiere autorización explícita. Sin eso, el registro en dispositivo fallará con `PERMISSION_DENIED`.

**L7** - 19 nov 2026, correccion tras probar en dispositivo (`65d5dd78`): el primer intento devolvio `PERMISSION_DENIED`. El logcat mostro `Listen ... linkingCodes/35Q3BC failed`: la lectura del codigo se hacia antes de crear la cuenta Auth, y `linkingCodes.get` exige `isSignedIn()`. Se invirtio el orden: primero `createUserWithEmailAndPassword`, despues leer codigo y alumno; cualquier fallo posterior cierra la sesion para no dejar cuentas a medias. No quedo usuario creado. APK recompilado e instalado con la correccion.

**L8** - Reglas publicadas con autorizacion explicita del dueno: `firebase deploy --only firestore:rules --project aulasegu` -> `released rules to cloud.firestore`. Hay 7 codigos sin usar; `35Q3BC` (vence 26 nov) es uno de ellos. El dueno hara la prueba funcional del registro.

**L9** — 19 nov 2026, causa raiz del `PERMISSION_DENIED` (auditoria del intento anterior): el lote no fallaba por el orden Auth/codigo, sino por el orden de las ramas dentro de `students.update` y `linkingCodes.update`. La rama del directivo/tutor iba primero y llama a `isDirectivo()`/`isActive()`, que leen `users/{uid}` con `get()`. Durante el registro ese documento **solo existe como escritura pendiente del lote**, así que `get()` consulta el estado previo, devuelve `null`, y acceder a `.data` produce un error de evaluación: el `allow` completo queda denegado y la segunda rama nunca se evalúa. `getAfter()` no ayuda en esas ramas porque el error ocurre antes. Correccion: la rama del registro va **primera** en las tres reglas (`users.create`, `students.update`, `linkingCodes.update`) y no usa ninguna función que lea el perfil propio. El reordenamiento anterior solo tocaba `users.create` y nunca se publicó, por eso no cambió nada.

**L10** — 19 nov 2026, defecto de diseño detectado: `signOut()` no liberaba el correo. Un lote rechazado dejaba la cuenta de Auth creada, con lo que reintentar respondia `EmailInUse` y entrar respondia "sin perfil en el plantel": el email quedaba secuestrado sin salida. Ahora `discardHalfRegisteredAccount()` borra la cuenta con `FirebaseUser.delete()` y despues cierra la sesion; todos los caminos de fallo posteriores al `createUserWithEmailAndPassword` pasan por ahi.

**L11** — 19 nov 2026: cuenta huerfana `prueba.familia@correo.com` (uid `OmAbTht9i6elYaovkodiM3M35822`) borrada con Admin SDK, verificada antes como "sin perfil". Reglas republicadas tras dry-run. Verificacion: 54 suites / 389 tests / 0 fallos, `assembleDebug` verde, APK instalado en `65d5dd78`. Estado de prueba confirmado en Firestore: email libre, 7 codigos sin usar; `35Q3BC` es del alumno "juan loe", grupo 4° D, vence 26 nov.

**L12** — 19 nov 2026, causa real del segundo `PERMISSION_DENIED` (evidencia de logcat, no inferencia): `Listen for QueryWrapper(target=Query(students/stu-...-01-...)) failed: PERMISSION_DENIED`. No era el lote: `registerTutor` leia `students/{id}` **antes** del batch para obtener el `groupId`, y `students.read` exige `isActive()`, que lee `users/{uid}`, que en ese momento no existe. La cuenta se creaba y el `delete()` nuevo la borraba correctamente (log de sign-out + email verificado libre). Mi arreglo de L9 era necesario pero insuficiente: tapaba las tres reglas de escritura, no esta lectura.

**L13** — 19 nov 2026, decision de diseno: el `linkingCodes` es un ticket y debe llevar todo lo necesario para el registro. `createLinkingCode` ahora lee el alumno (el directivo si puede) y escribe `groupId` en el codigo; `registerTutor` deja de leer `students` y toma `studentIds`, `schoolId` y `groupIds` del codigo. Ademas se cerro un agujero: `users.groupIds` es lo que despues usan las reglas de tareas/incidentes via `myGroups()`, y no estaba validado; la rama del tutor ahora exige `groupIds == [getAfter(code).data.groupId]`, y `linkingCodes.create` exige `groupId is string`. Los 8 codigos existentes se migraron con Admin SDK (`migrated=8 broken=0`).

**L14** — 19 nov 2026, estado final del bloque: 54 suites / 389 tests / 0 fallos, `assembleDebug` verde, reglas publicadas con validacion de `groupId`, codigos migrados, email de prueba libre. Sin dispositivo conectado no se ejecuto la prueba funcional del registro; queda para el dueno. Si el registro falla ahora, la cuenta Auth se borra sola y el correo queda libre para reintentar.

