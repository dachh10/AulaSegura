# Fase 2 — Directivo (Checklist de ejecución)

> Abrí este archivo en el editor dividido a la derecha (`clic derecho en la pestaña → Split Right`)
> y dejalo visible mientras trabajás. Tildá cada ítem al terminarlo, no antes.
>
> Referencia técnica: `CONTEXT.md`. Roadmap general: `PLAN.md` §Fase 2. Fase anterior: `FASE1.md`.
> Responsable: **Evelyn**. Rama de trabajo sugerida: `feat/admin-usuarios` (`CONTEXT.md` §10).

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso · `[!]` bloqueado

**Tablero de bloques** — se actualiza **en el mismo commit** que entrega el bloque, no después:

| Bloque | Qué entrega | Estado |
|---|---|---|
| **1** — Alta de cuentas | script con Admin SDK + usuario real y edición de perfiles en la app | ✅ **listo** · build verde · regla desplegada |
| **2** — Usuarios | edición, baja lógica y filtros reales sobre `users` | ✅ **listo** · build verde · 224 tests |
| **3** — Grupos | CRUD, horario y asignación atómica de docentes | ✅ **listo** · build verde · 230 tests |
| **4** — Alumnos | alta con CURP, número de lista automático, grupo y código de vinculación | ✅ **listo** · build verde · 306 tests |
| **5** — Panel | métricas reales del plantel + asistencia del día | ✅ **listo** · build verde · 246 tests |
| **6** — Reportes | consultas por rango y agregados de asistencia | ✅ **listo** · build verde · 246 tests |
| **7** — Bandeja de incidentes | lista real y cambio de estado | ✅ **listo** · build verde · 253 tests |
| **8** — Configuración del plantel | radio, horario y switch escritos en `schools` | ✅ **listo** · build verde · `requireLocation` con consumidor en el gate (`6e83f0b`) |
| **9** — Notificaciones | bandeja real + push manual (Spark, sin Functions) | ✅ **listo** · build verde · 271 tests · verificado |
| **10** — Perfil de la directora | datos reales y foto en Cloudinary con Coil | ✅ **listo** · build verde · 275 tests |

**Criterio de cierre de la fase** (`PLAN.md`): la directora crea **un grupo y dos alumnos desde la
app**, y el docente los ve en su pase de lista **sin tocar código**.

> Ese criterio es el hilo conductor de toda la fase. El pase de lista del docente lee
> `students where groupId == X AND active == true`, y sus grupos salen de `users/{uid}.groupIds`.
> Si el alta de un alumno no incrementa `studentCount`, o si asignar un grupo a un docente no
> actualiza su `groupIds`, el criterio **no se cumple aunque la app no dé errores**: el docente
> simplemente no ve lo que se creó.

---

## Punto de partida

La Fase 1 dejó esto construido y **no se vuelve a hacer**:

| Ya está | Dónde |
|---|---|
| Auth real, 3 cuentas demo, sesión persistente, logout, `active=false` expulsa | Fase 0 · bloques C/G |
| Reglas de Firestore y RTDB publicadas (`aulasegu`) | `docs/firestore.rules`, `docs/rtdb.rules` |
| 13 índices compuestos desplegados | `docs/firestore.indexes.json` |
| Outbox + `SyncWorker` + handlers de asistencia, incidentes, tareas y avisos | `sync/` |
| Uploader de Cloudinary + compresor de imagen + store de archivos | `data/remote/cloudinary/`, `data/local/media/` |
| Bandeja de notificaciones real + script que escribe documento y manda push | `push/`, `scripts/notify/` |
| Contenedor manual de dependencias | `di/AppContainer.kt` |
| Seed local con Admin SDK (resuelve cuentas por email, no crea) | `tools/seed/` |
| 45 pantallas del directivo/docente/tutor con datos de ejemplo | `ui/screens/admin/` |

Lo que **falta** y es el trabajo de esta fase: no hay **ni un repositorio** de usuarios, grupos,
alumnos, incidentes (bandeja), reportes ni configuración del plantel. Las 11 pantallas de
administración siguen leyendo listas hardcodeadas.

> La Fase 1 cerró con **219 tests unitarios**. Ese es el piso: ningún bloque baja ese número.

---

## Antes del bloque 1 — decisiones que hay que tomar

Estas cuatro decisiones **bloquean código**. Están aquí porque el `PLAN.md` pide cosas que las
reglas publicadas hoy no permiten; conviene resolverlas antes de escribir el primer ViewModel.
Las reglas son contrato compartido: cualquier cambio se avisa antes de mergear.

| # | Conflicto | Opciones | Recomendación |
|---|---|---|---|
| **D1** ✅ **resuelta** | `PLAN` 2.1/2.2 piden que la directora **edite perfiles e inactive usuarios desde la app**. Hoy `users/{uid}` prohíbe `update` de `active` **y** exige `request.auth.uid == uid` (`firestore.rules:95`), así que tampoco puede editar el perfil de otro usuario. `create`/`delete` están prohibidos. | (a) ampliar la regla para que el directivo edite campos no sensibles y `active` de usuarios de su `schoolId`; (b) mantener altas, bajas y ediciones desde la consola/script y dejar la pantalla de Usuarios en solo lectura. | **Decidido: (a)**. Lista blanca (`firstName`, `lastName`, `fullName`, `phone`, `subjects`, `groupIds`, `studentIds`, `preferences`, `photoUrl`, `photoPublicId`, `active`, `updatedAt`) + comparación de `schoolId`. Nunca `uid`, `email`, `role`, `schoolId` ni `createdAt`. La rama de "uno mismo" mantiene su lista y no puede tocar `active`. |
| **D2** | `PLAN` 2.9 pide "notificaciones reales + push". Hoy `notifications` tiene `create: false` para clientes: solo Admin SDK/consola puede escribir. | (a) permitir que el directivo cree notificaciones para usuarios de su plantel con `type`/`deepLink` validados; (b) la app solo lee la bandeja y el envío sigue por `scripts/notify` / consola. | **(b) para el push**: aunque se cambie la regla, en Spark **no hay Cloud Functions**, así que el push automático sigue sin existir. La regla (a) es útil para la bandeja, pero no reemplaza al script. No diseñes un flujo que dependa de un trigger. |
| **D3** | `PLAN` 2.1 pide "custom claim de rol". Las reglas leen el rol de `users/{uid}.role`, no del claim. | (a) escribir el claim además del documento; (b) solo el documento. | **(b)** hoy; el claim es una optimización de coste, no la fuente de verdad. Si se escribe, que sea copia de `users.{role}`, nunca al revés. |
| **D4** | La llave de la service account del seed se iba a revocar al cerrar la Fase 0 (`FASE0.md` bloque K). El alta de cuentas vuelve a necesitar Admin SDK. | (a) generar una llave nueva para las altas, fuera del repo, y revocarla al terminar la tanda; (b) crear las cuentas a mano en la consola. | **(a)** para altas multiples; **(b)** para una o dos. Nunca commitear el JSON, nunca dejar la llave viva "por si acaso". |

**Prerrequisitos operativos**

- [x] **P0.1** Rama integrada en `main` con el merge `06e606a`; `assembleDebug` y 379 tests en verde
- [ ] **P0.2** Decidir D1, D2, D3 y D4 y anotar la decisión en `CONTEXT.md` si cambia el contrato
- [x] **P0.3** Regla de D1 **desplegada el 17 nov 2026**: `firebase deploy --only firestore:rules
      --project aulasegu` → `rules file compiled successfully` + `released rules to
      cloud.firestore` + `Deploy complete!`. El primer intento había dado 401; se reautenticó y
      volvió a correr. (El CLI informó `already up to date, skipping upload`: el ruleset ya
      coincidía con el archivo local.)
- [x] **P0.4** `requireLocation` tiene consumidor real desde `6e83f0b`: `SchoolSite` y
      `SchoolEntity` lo llevan y el gate de GPS del docente respeta el flag (ver «Novena continuación»)
- [ ] **P0.5** Discrepancia de correo sin resolver: `FASE0.md` y `CONTEXT.md` documentan
      `directora@escuela.mx`, pero el dueño del proyecto indicó el 17 nov que la cuenta es
      `directora@gmail.com`. No se pudo confirmar contra Firebase: el endpoint de
      `signInWithPassword` devolvió **403** con la API key del proyecto (restringida). Confirmar
      cuál es la real antes de corregir los documentos base. **No se guarda la contraseña.**

> **Regla de oro de las consultas** (ya nos costó una tarde en la Fase 0): Firestore **no filtra**
> con las reglas. Toda consulta de lista lleva exactamente el filtro que la regla exige:
> `whereEqualTo("schoolId", …)` para lo del plantel, `whereEqualTo("groupId", …)` +
> `whereEqualTo("active", true)` para alumnos, `whereEqualTo("userId", uid)` para notificaciones.
> Sin ese filtro, `PERMISSION_DENIED` aunque el documento sea legible.

---

## Bloque 1 — Alta de cuentas sin Cloud Functions (PLAN 2.1)

- [x] **1.1** Script local (`tools/seed/create-user.mjs`) que crea el usuario en Firebase Auth
      con email/contraseña y escribe `users/{uid}` completo
- [x] **1.2** Campos obligatorios del documento: `uid`, `email`, `role`, `firstName`, `lastName`,
      `fullName`, `schoolId`, `active: true`, `subjects`/`groupIds` (docente) o `studentIds`
      (tutor), `preferences`, `createdAt`, `updatedAt`
- [x] **1.3** El script es **idempotente**: si el email ya existe, resuelve el `uid`, **no cambia
      la contraseña** y no sobrescribe una identidad distinta
- [~] **1.4** Credencial de service account fuera del repo (✅). **Pendiente de humano:** generar
      una llave para correr el alta y revocarla al terminar la tanda — hoy no hay llave en
      `%USERPROFILE%\.aulasegura\`
- [x] **1.5** En la app: formulario de edición de perfil de un usuario (sin crear cuentas) —
      `AdminUsersScreen` lee `users` reales y abre un diálogo con nombre, teléfono, materias,
      grupos, alumnos y el switch **Cuenta activa**
- [x] **1.6** Alta desde la app **nunca** llama a `createUserWithEmailAndPassword`: cerraría la
      sesión de la directora (`CONTEXT.md` §9.1)

**Cómo correr el alta** (ver `tools/seed/README.md`):

```powershell
cd tools/seed
npm install
node create-user.mjs --email=nuevo@aulasegura.com --password=secreto123 `
  --role=docente --first-name=Ana --last-name=López `
  --subjects=Matemáticas --group-ids=grp-3b --dry-run
```

El `--dry-run` muestra el plan y resuelve si el email ya existe sin escribir. Para escribir,
sacar `--dry-run` con `GOOGLE_APPLICATION_CREDENTIALS` apuntando a la llave. El script vuelve a
leer Auth y Firestore al terminar y falla si email, rol, `schoolId`, `fullName` o `active` no
coinciden con lo pedido.

> **Verificado hasta acá:** `node --check` en verde para los dos scripts; `--help`, validación
> de argumentos y mensaje de llave faltante comprobados en ejecución real. **No verificado:**
> el alta contra Firebase, porque no hay service account en este equipo.

**Parte de la app (17 nov 2026)**

| Archivo | Cambio |
|---|---|
| `domain/model/AdminUser.kt` | **nuevo**: modelo de dominio; `roleFromFirestore` reusa `UserRole` (ya tiene `UNKNOWN`) |
| `domain/repository/AdminUserRepository.kt` | **nuevo**: `getUsers()` + `updateProfile(uid, changes)` con resultado tipado |
| `data/repository/FirestoreAdminUserRepository.kt` | **nuevo**: `whereEqualTo("schoolId", …)` con `Source.SERVER`; `update(map)` con lista blanca y `updatedAt` de servidor |
| `ui/screens/admin/AdminUsersUi.kt` | **nuevo**: `UiState`, `formFrom`, `validationError` y `buildUpdatePayload` puros |
| `ui/screens/admin/AdminUsersViewModel.kt` | **nuevo**: `StateFlow`, carga y refresco tras guardar |
| `ui/screens/admin/AdminUsersScreen.kt` | docentes y padres reales; diálogo de edición; estados de carga/vacío/error+reintento/éxito |
| `di/AppContainer.kt` | `adminUserRepository` y `AdminUsersViewModel` registrados |
| `test/.../AdminUsersUiTest.kt` | **nuevo**: 5 tests de filtros, validación, payload y fallback de rol |

**Decisiones que importan**

- **Una sola lectura para las dos pestañas.** Se consulta por `schoolId` y se separa docentes
  de tutores en memoria: cambiar de pestaña no gasta lecturas (presupuesto Spark).
- **El payload se filtra dos veces.** `buildUpdatePayload` solo produce claves permitidas y el
  repositorio vuelve a filtrar con `WRITABLE_KEYS` antes de `update(map)`. Ni un campo de
  identidad puede llegar al cable, aunque un llamador futuro lo pase por error.
- **`fullName` se deriva, no se edita.** Nombre y apellido se concatenan en el builder, así que
  la lista no puede quedar con un nombre compuesto viejo.
- **Pestaña Alumnos sigue siendo de ejemplo** y lo dice en su pista: los alumnos reales llegan
  con el Bloque 4.
- **Sin `createUserWithEmailAndPassword` en la app.** El alta es siempre por script.

**Verificación (ejecutada y releída)**

| Check | Resultado |
|---|---|
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` | ✅ `BUILD SUCCESSFUL` |
| Re corrida del mismo comando en el spot check del orchestrador | ✅ `BUILD SUCCESSFUL` |
| XML de `testDebugUnitTest` | **224 tests, 0 fallos, 0 errores** (37 archivos) — piso era 219 |
| `gentle-ai review assess` | `risk: medium` (sin perfil small-model, no requiere verifier aparte) |
| Los 8 archivos nuevos, declarados en el inventario del assess | ✅ aceptados |

> **Bloqueo para probar en el teléfono:** la regla de D1 está escrita pero **no desplegada**
> (401 de OAuth). Editar otro usuario va a dar `PERMISSION_DENIED` hasta que se corrija el login
> del CLI y se despliegue. El build y los tests no cubren permisos en vivo: eso se prueba con la
> cuenta real de la directora.

**Decisiones que importan**

- **El alta no puede vivir en el cliente.** `createUserWithEmailAndPassword` autentica al nuevo
  usuario y deslogea a la directora. Es por eso que el proyecto está en Spark y no hay Functions:
  el script es la única vía, igual que para el seed.
- **La identidad la escribe el Admin SDK, no la app.** Las reglas dejan `create: false` en `users`
  a propósito. Si la app pudiera crear usuarios, podría autoregistrarse como `directivo`.
- **`fullName` se denormaliza desde el alta.** Lo leen las listas, el chat y los encabezados;
  armarlo en la UI desde `firstName + lastName` en cada pantalla es cómo se generan nombres
  distintos para la misma persona.

**Cómo probarlo**

1. Correr el script con un email nuevo → existe en Authentication **y** hay un `users/{uid}` con
   `role` correcto y `active: true`.
2. Repetir la corrida → mismo `uid`, sin segundo documento.
3. Hacer login con esa cuenta en un teléfono → aterriza en el grafo de su rol.
4. Mientras la directora está logueada, crear otra cuenta desde el script → **su sesión no se corta**.

**Criterio:** una cuenta nueva creada por el script entra a la app con su rol, y la directora no
sale de la suya.

---

## Bloque 2 — Usuarios (PLAN 2.2)

- [x] **2.1** `AdminUserRepository` en `domain/repository/` + implementación Firestore
      (entregado en el Bloque 1)
- [x] **2.2** Lista real de `users` filtrada por `schoolId`, 3 pestañas y búsqueda local sobre
      `fullName`/`email`. La pestaña Alumnos sigue siendo ejemplo (Bloque 4) y lo dice
- [x] **2.3** Edición de campos de perfil permitidos: `firstName`, `lastName`, `fullName`
      (derivado), `phone` y `subjects`. **Excluidos a propósito** `groupIds` y `studentIds`:
      son enlaces denormalizados y escribir solo ese lado rompe las reglas; se gestionan en el
      Bloque 3 con escritura en dos documentos. `photoUrl`/`photoPublicId` se miden en el
      Bloque 10 (foto)
- [x] **2.4** Baja lógica: switch **Cuenta activa** escribe `active = false` (decisión D1).
      **No** se borra el documento
- [~] **2.5** Cambios atómicos: asignar un grupo a un docente ⇒ actualizar también
      `users/{uid}.groupIds` en la misma operación. **Trasladado al Bloque 3**, donde se
      escriben `groups.teacherIds` y `users.groupIds` juntos
- [x] **2.6** **No se agrega evento de Analytics.** La lista de `CONTEXT.md` §9 es el contrato
      y no incluye eventos de administración; agregar uno es una decisión de telemetría
      separada. Registrar la edición como "sin evento" es más honesto que emitir un nombre
      inventado que nadie consume

**Decisiones que importan**

- **Los campos de identidad son inmutables por diseño.** `uid`, `email`, `role`, `active` y
  `schoolId` no se tocan desde el cliente salvo el caso explícito de D1. Un `update` que mande
  cualquiera de esos campos es `PERMISSION_DENIED`.
- **`groupIds` es denormalizado y es la fuente de la regla.** El docente ve sus grupos y el tutor
  ve los de sus hijos por ese array (`usarGroups()` en `firestore.rules`). Escribir el grupo en
  `groups/{id}.teacherIds` pero no en `users/{uid}.groupIds` deja un docente que "debería" ver el
  grupo y no lo ve.
- **Baja lógica, no `delete`.** La regla hoy sí permite `delete` en `students` y `groups`, pero
  `CONTEXT.md` §8.4 manda `active = false`. Un borrado rompe el historial de asistencia que ya
  apunta a ese id.

**Cómo probarlo**

1. La lista trae las 3 cuentas demo con nombre, rol y correo reales; buscar "Ricardo" filtra.
2. Editar el teléfono de la directora → se refleja al recargar y al entrar con esa cuenta.
3. Dar de baja a un docente → en la consola `active: false`; esa cuenta, al abrir la app, vuelve
   al login sola (Fase 0 · C10).
4. Intentar cambiar `role` desde la app → la operación falla, no "parece" que funcionó.

**Criterio:** la directora ve, edita y da de baja usuarios reales de su plantel, sin poder
cambiar identidad ni borrar documentos.

---

## Bloque 3 — Grupos (PLAN 2.3)

- [x] **3.1** `AdminGroupRepository` con listado por `schoolId` y `getGroup(groupId)` (necesario
      para el detalle; no estaba en el plan original)
- [x] **3.2** Alta de grupo con `name`, `grade`, `section`, `schoolId`, `cycle`,
      `studentCount: 0`, `teacherIds`, `titularTeacherId` y timestamps. **No se inventó un campo
      `active`**: `CONTEXT.md` §6.2 no lo define para `groups`
- [x] **3.3** Asignación/desasignación de docentes: `groups.teacherIds` y
      `users/{uid}.groupIds` en **un solo `WriteBatch`** con `arrayUnion`/`arrayRemove`;
      `titularTeacherId` se setea en ese mismo batch solo si está vacío
- [x] **3.4** Subcolección `groups/{id}/schedule/{slotId}` con `slotId` determinista
      (`weekday-startTime`), upsert y borrado de slots viejos en un batch. Se carga solo en el
      detalle para no hacer N+1 lecturas en la lista
- [x] **3.5** `studentCount` arranca en 0 y nunca se escribe a mano; lo actualiza el alta de
      alumnos del Bloque 4
- [x] **3.6** `AdminGroupsScreen` y `AdminGroupDetailScreen` cableadas. La pestaña Alumnos queda
      como pista honesta hasta el Bloque 4
- [x] **3.7** Asignación exclusiva desde Grupos (Usuarios dejó de escribir `groupIds`).

**Verificación (17 nov 2026):** `assembleDebug` verde · **230 tests, 0 fallos** (eran 224) ·
`gentle-ai review assess` = `medium` · test de regresión verifica que la descripción de la
asignación nombre **ambas** rutas (`groups/g-3b.teacherIds` y `users/u-1.groupIds`).

**Decisiones que importan**

- **`studentCount` es un derivado.** La semilla lo verificó leyendo la cuenta real de los 4
  grupos. Si el alta de alumnos (Bloque 4) no lo incrementa, el panel y la tarjeta del grupo
  mienten aunque la lista de alumnos esté bien.
- **Asignar es una transacción de dos documentos.** No hay regla que mantenga `groupIds` solo:
  el código tiene que hacerlo. Conviene un único caso de uso `AssignTeacherUseCase` para que
  no se haga en dos pantallas distintas con dos implementaciones.
- **El horario es una subcolección, no un array.** La regla de `schedule` ya está publicada y
  valida `schoolId` leyendo el padre; no hace falta tocar `firestore.rules`.

**Cómo probarlo**

1. Crear un grupo "4° D" → existe en `groups` con `schoolId` del plantel y `studentCount: 0`.
2. Asignar a Ricardo Salinas → en `groups/{id}.teacherIds` y en `users/{uidDelDocente}.groupIds`
   aparece el grupo nuevo. Entrar como docente → **Mis grupos** lo muestra sin recargar nada.
3. Desasignar → desaparece de los dos lados.
4. Agregar un slot de horario → `groups/{id}/schedule/{slotId}` con `weekday`, `startTime`,
   `endTime`, `subject`.

**Criterio:** un grupo creado y asignado desde la app aparece en el pase de lista del docente.

---

## Bloque 4 — Alumnos (PLAN 2.4)

- [x] **4.1** `AdminStudentRepository` + alta con `firstName`, `lastName`, `fullName`, `curp`,
      `listNumber`, `groupId`, `schoolId`, `active: true`, `tutorIds: []`
- [x] **4.2** Validación de CURP (18 caracteres alfanuméricos en mayúsculas; el campo sigue siendo
      opcional porque así lo etiqueta el mockup). El número de lista **ya no se escribe**: se asigna
      automáticamente por orden alfabético de apellidos y se renumeran todos los alumnos activos
      del grupo con `Collator` en `es-ES` (fuerza `PRIMARY`, desempate por nombre y por id)
- [x] **4.3** Al crear: una sola transacción de Firestore que renumera los alumnos activos,
      escribe el alumno nuevo con su número calculado y hace `FieldValue.increment(1)` sobre
      `groups/{groupId}.studentCount`
- [x] **4.4** Código de vinculación en `linkingCodes/{code}`: 6 caracteres, `studentId`,
      `schoolId`, `createdBy`, `createdAt`, `expiresAt` (7 días), `usedBy: null`
- [x] **4.5** El código se muestra al guardar y se explica que se entrega a la familia
- [x] **4.6** Sin cambios de reglas: las ya publicadas cubren creación y canjeo del código
- [x] **4.7** Pestaña Alumnos real + entrada al formulario desde el botón `+` de esa pestaña.
      Docentes/Padres conservan el diálogo del alta por script. El id sigue el formato de la
      semilla (`grp-3b` → `stu-3b-07`) con fallback a UUID si el id ya existe
- [x] **4.8** Baja lógica (`active = false`) con `FieldValue.increment(-1)` en el mismo batch;
      **sin borrado del documento**

**Verificación (17 nov 2026):** `assembleDebug` verde · **235 tests, 0 fallos** (eran 230) ·
`gentle-ai review assess` = `medium`. El alumno nace con `active: true` y `groupId` reales, que es
exactamente lo que filtra el pase de lista del docente.

**Decisiones que importan**

- **El código no se lista desde el tutor.** `allow list: if isDirectivo()`; el tutor hace un
  `get` por el código exacto. Un listado abierto permitiría vincular alumnos ajenos.
- **`tutorIds` nace vacío.** Lo completa la vinculación de la Fase 3. Escribir un tutor
  inventado acá dejaría al alumno "vinculado" con nadie.
- **Alta con `active = true` y `groupId` correcto es literalmente el criterio de cierre.** La
  consulta del pase de lista exige ambos campos; un alumno created con otro estado no aparece.

**Cómo probarlo**

1. Crear dos alumnos en 3° B con CURP, sin escribir el número de lista → `students` tiene dos
   documentos con `schoolId`, `groupId: grp-3b`, `active: true` y números 1 y 2 según apellidos.
2. En la consola, `groups/grp-3b.studentCount` sube de 28 a 30.
3. Entrar como docente → **Pase de lista** de 3° B muestra los dos alumnos nuevos en su posición
   alfabética (no al final).
4. Crear un alumno con apellido que va antes → todos los números del grupo se reasignan 1..N en la
   misma transacción; el formulario ya no ofrece el campo "Número de lista".
5. El código generado existe en `linkingCodes` con `usedBy: null` y expira en la fecha prevista.

**Criterio:** la directora crea dos alumnos y el docente los ve en su pase de lista sin tocar código.

---

## Bloque 5 — Panel (PLAN 2.5)

- [x] **5.1** `AdminDashboardRepository`: conteos reales de alumnos, grupos e incidentes con
      `count()` por `schoolId`; docentes se cuentan de un snapshot filtrado para no pedir un
      índice `schoolId + role`
- [x] **5.2** Cartel de asistencia del día desde `attendanceSessions` del plantel. Resuelto en el
      Bloque 6 con el índice `schoolId + date` ya desplegado: sesiones de hoy y porcentaje reales.
      Sin sesiones hoy lo dice explícitamente; la tarjeta de atención sigue marcada como pendiente
      porque no tiene fuente de datos real
- [x] **5.3** `AdminDashboardScreen` sin constantes: reemplazar los `sample…` por el `UiState`
- [x] **5.4** Estados de carga, vacío y error visibles (Definition of Done, `CONTEXT.md` §13).
      Los `…` de carga y la tarjeta de reintento nunca muestran un cero inventado

**Verificación (17 nov 2026):** `assembleDebug` verde · **241 tests, 0 fallos** (eran 235) ·
`gentle-ai review assess` = `medium`. Nota técnica: `AggregateQuery.get()` exige un
`AggregateSource` explícito en el SDK resuelto; se usa `AggregateSource.SERVER` para no leer
conteos de caché.

**Decisiones que importan**

- **Los números del mockup no coinciden con la semilla.** `PLAN.md` dice 85 alumnos / 3 grupos;
  el seed cargó **111 alumnos en 4 grupos** (`grp-3b` 28, `grp-3a` 30, `grp-2c` 27, `grp-1a` 26).
  El panel muestra lo que hay en Firestore, nunca los del diseño.
- **Conteos vs. listeners.** Un `snapshotListener` abierto en el panel consume lecturas mientras
  la pantalla esté abierta; en Spark conviene `get()` + refresco al volver a entrar.

**Cómo probarlo**

1. Entrar como directora → el panel muestra los conteos reales de la consola, no 85/3.
2. Crear un alumno (Bloque 4) → al volver al panel el conteo sube.
3. Forzar un error de red → mensaje de error, no ceros que parezcan datos reales.

**Criterio:** cada cifra del panel se puede corroborar con una consulta a Firestore.

---

## Bloque 6 — Reportes (PLAN 2.6)

- [x] **6.1** Consulta de `attendanceSessions` por rango (`schoolId` + `date` desde/hasta),
      `Source.SERVER` y timeout de 12 s
- [x] **6.2** Agregados: porcentaje de asistencia, totales por estado, desglose por grupo y
      días con más faltas. El porcentaje es `present / todas las marcas`; sin marcas da 0% con
      estado vacío explícito, nunca barras inventadas
- [x] **6.3** `AdminReportsScreen` con anillo, barras y ranking reales; los ids de grupo se
      resuelven a nombres con una lectura filtrada por `schoolId` (sin índice nuevo)
- [x] **6.4** Índice compuesto agregado y desplegado (ver decisión abajo)

**Verificación (17 nov 2026):** `assembleDebug` verde · **246 tests, 0 fallos** (eran 241) ·
`gentle-ai review assess` = `medium`. Falta la prueba en dispositivo con datos reales de la
directora para confirmar que el índice sirve el rango.

**Decisión que importa — el índice que falta**

Los índices desplegados tienen `attendanceSessions: groupId + date desc` y
`teacherId + date desc`, pero **no** `schoolId + date desc`. Las opciones son:

| Opción | Tradeoff |
|---|---|
| Agregar el índice compuesto `schoolId + createdAt/date` y desplegarlo | Consulta directa y rápida; toca `docs/firestore.indexes.json` → avisar antes de mergear |
| Consultar por grupo (índice existente) y sumar en el cliente | Sin cambio de esquema; más lecturas y más código |
| Traer por `schoolId` y ordenar en memoria | Funciona con el índice de un campo; se vuelve costoso al crecer el historial |

Recomendación: agregar el índice. Es una línea en un archivo versionado y evita agregados
cliente que se vuelven lentos justo cuando haya más datos.

**Decisión tomada y ejecutada (17 nov 2026): agregar el índice.** Se agregó
`attendanceSessions: schoolId ASC + date DESC` a `docs/firestore.indexes.json` y se desplegó con
`firebase deploy --only firestore:indexes --project aulasegu` → `Deploy complete!`. Con eso se
cierra también el pendiente **5.2** (tarjeta de asistencia del día en el panel).

> Ojo además con la regla de oro: la consulta lleva `whereEqualTo("schoolId", …)` aunque el
> directivo "ya pueda leer todo". Sin ese filtro, `PERMISSION_DENIED`.

**Cómo probarlo**

1. Elegir un rango de dos semanas → los números coinciden con las sesiones reales de ese período.
2. Cambiar el rango → los agregados cambian; no son los del mockup.
3. Sin sesiones en el rango → pantalla de vacío, no un 0% que parezca "todos faltaron".

**Criterio:** los reportes salen de las sesiones reales del plantel y respetan el rango elegido.

---

## Bloque 7 — Bandeja de incidentes (PLAN 2.7)

- [x] **7.1** `AdminIncidentRepository`: `whereEqualTo(schoolId)` + `orderBy(createdAt DESC)`,
      `Source.SERVER`, timeout de 12 s y `limit(300)` para acotar lecturas
- [x] **7.2** Filtro por severidad (`leve`/`moderado`/`grave`) y por estado
      (`nuevo`/`revisado`/`cerrado`), ambos como chips en memoria
- [x] **7.3** Cambio de estado: `update` de `status` + `updatedAt` de servidor. Los chips de
      severidad son **solo filtros**, no edición: el contrato del repositorio no expone
      `setSeverity` y las reglas no piden cambiarla
- [x] **7.4** `AdminIncidentsScreen` cableada; detalle expandible con `studentName`,
      `reportedByName`, descripción, categoría, `location` y **Abrir** por cada `media[].url`
- [x] **7.5** Sin borrado: los incidentes son evidencia y la regla lo prohíbe

**Verificación (17 nov 2026):** `assembleDebug` verde · **253 tests, 0 fallos** (eran 246) ·
`gentle-ai review assess` = `medium`. Pendiente la prueba en teléfono: aceptación del `update` de
estado con las reglas desplegadas y apertura de las URLs de Cloudinary.

**Decisiones que importan**

- **La regla ya permite el update del directivo** (`resource.data.schoolId == mySchool()`). No
  hace falta tocar `firestore.rules` para este bloque.
- **Los nombres vienen denormalizados.** `studentName` y `reportedByName` viajan en el documento
  justamente para que la bandeja no tenga que leer el padrón.
- **Va a haber incidentes de prueba.** La Fase 1 dejó uno en Firestore y las reglas no permiten
  borrarlo desde el cliente (`FASE1.md` bloque 6). No es un bug de la bandeja; es evidencia.
- **`media[].url` abre en el navegador** si no se agrega Coil (ver Bloque 10).

**Cómo probarlo**

1. La bandeja lista los incidentes reales con severidad, nombre y fecha; los chips filtran.
2. Cambiar uno a `revisado` → se ve en Firestore y al recargar la app.
3. Abrir el detalle → la foto se abre por la URL de Cloudinary.
4. Intentar borrar → no se puede (ni desde la app ni desde el cliente).

**Criterio:** la directora ve los incidentes que cargó el docente y puede cambiarles el estado.

---

## Bloque 8 — Configuración del plantel (PLAN 2.8)

- [x] **8.1** `AdminSchoolRepository` propio para el directivo (online-only, sin caché): no se
      extendió el `FirestoreSchoolRepository` del docente, que está atado a Room y al fallback de
      Remote Config
- [x] **8.2** Edición real de `location`, `radiusMeters`, `schedule` y `requireLocation`. El mapa
      muestra las coordenadas guardadas; no es arrastrable (no hay dependencia de mapas nueva)
- [x] **8.3** Radio **20..5000 m** con paso de 5 m; se rechaza 0. El update map se construye en
      `domain` con un centinela que la capa de datos cambia por `serverTimestamp()`: solo
      `location`, `radiusMeters`, `schedule`, `requireLocation` y `updatedAt`
- [x] **8.4** `AdminCampusScreen` cableada con estados de carga, error+reintento, éxito y el aviso
      de que el docente recibe el cambio en su próximo refresco con red
- [x] **8.5** Sin solapamiento con Remote Config: `default_radius_meters` sigue siendo solo
      fallback del docente

> **P0.4 original:** `requireLocation` no tenía consumidor: se escribía en Firestore pero no
> cambiaba el comportamiento del docente. **Resuelto en la novena continuación:** el flag viaja
> en `SchoolSite` desde Firestore y desde la caché Room v4, y el gate lo respeta. Ver el final
> del documento.

**Verificación (17 nov 2026):** `assembleDebug` verde · **260 tests, 0 fallos** (eran 253) ·
`gentle-ai review assess` = `medium`. Test de regresión: el update map no contiene `key`,
`createdAt`, `name`, `cycle`, `address` ni `schoolId`.

**Decisiones que importan**

- **El cambio no llega al docente al instante.** El gate de GPS lee `schools` desde la caché de
  Room (`SchoolEntity`, v3) y refresca en segundo plano. Con el docente sin red, sigue usando el
  último centro/radio conocidos (`FASE1.md` bloque 3). Es lo correcto para un aula sin señal, pero
  hay que saberlo al probar.
- **La regla ya permite el update** excepto `key` y `createdAt`. No se crea ni se borra el
  documento del plantel.

**Cómo probarlo**

1. Cambiar el radio a 200 m → en Firestore `schools/{schoolId}.radiusMeters = 200`.
2. Con el docente online, abrir el pase de lista → el banner muestra el radio nuevo.
3. Con el docente en modo avión → sigue mostrando el radio cacheado (comportamiento esperado).
4. Mover el pin → `location` cambia y la geocerca se mide al centro nuevo.

**Criterio:** la configuración se persiste en `schools` y el docente la recibe en su próximo
refresco con red.

---

## Bloque 9 — Notificaciones (PLAN 2.9)

- [x] **9.1** `AdminNotificationsScreen` cableada a `notifications where userId == uid` (índice
      ya desplegado) con agrupación HOY/ANTERIORES y chips por tipo. Se **reutilizó**
      `FirestoreNotificationRepository`, que ya es genérico; no hizo falta un repositorio nuevo
- [x] **9.2** D2 **decidida: solo consola/script.** Las reglas mantienen `create: false` en
      `notifications`; la app únicamente lee y cambia `read`. No se amplió la regla: en Spark
      tampoco habría trigger automático, así que dar permiso de escritura al cliente solo
      agregaría una forma de falsificar comunicados
- [x] **9.3** `scripts/notify/index.js` acepta `--role=...` y `--school-id=...`; resuelve uids,
      escribe **un** documento y manda **un** push por destinatario, limpia tokens muertos y
      falla con exit 1 si algo falla. `--uid`, `--dry-run` y `--no-push` siguen iguales
- [x] **9.4** Lista blanca extendida con rutas `ADMIN_*` y `PARENT_*`, con segmentos dinámicos
      estrictos. Se reforzó además para rechazar `.` y `..` como id (verificación independiente)
- [x] **9.5** Documentado en pantalla y en el script: el envío es manual, no hay trigger por evento

**Verificación (17 nov 2026):** `assembleDebug` verde · **271 tests, 0 fallos** (eran 260) ·
`node --check scripts/notify/index.js` limpio · dry-runs de modo masivo y `--uid` ejecutados ·
`gentle-ai review assess` = `high` por el proceso del script → **verificador independiente:
`verified`** (sin secretos, un documento por destinatario, sin creación desde Kotlin, `deepLink`
solo tras resolución no nula).

**Decisiones que importan**

- **Spark = envío manual.** El push lo dispara una persona desde la consola o el script; la app
  escribe `fcmTokens`, recibe y abre el `deepLink`. No existe "notificar cuando pase X".
- **La bandeja es de solo lectura salvo `read`.** Las reglas ya lo limitan. Un cliente que pudiera
  crear notificaciones podría falsificar un comunicado de la dirección.
- **El `deepLink` es lista blanca.** Viene de un documento escrito fuera de la app; pasar directo
  a `navigate()` permitiría abrir cualquier ruta. Valor desconocido → no abre nada.

**Cómo probarlo**

1. Crear un documento en `notifications` con `userId` = uid de la directora → aparece en la bandeja.
2. Tocarlo → marca leído y navega al `deepLink` permitido; uno inventado no abre nada.
3. Correr `scripts/notify` con `--uid` de un docente → documento en su bandeja **y** push en su
   teléfono (`--dry-run` para ensayar).
4. Verificar que el push con app cerrada abre la pantalla correcta.

**Criterio:** la directora ve notificaciones reales y puede disparar un aviso a una cuenta real
del plantel con documento + push en una sola corrida.

---

## Bloque 10 — Perfil de la directora (PLAN 2.10)

- [x] **10.1** `AdminProfileScreen` con `fullName`, `email`, `schoolId`, `phone` reales
- [x] **10.2** Foto: comprimir con `ImageCompressor`, subir a `aulasegura/profiles/{uid}` con el
      uploader existente y escribir `photoUrl` + `photoPublicId` **solo si la subida terminó en
      `Success`**. El update map no contiene campos de identidad
- [x] **10.3** Avatar con Coil `AsyncImage` (`coil3.compose` en la 3.6.3) y transformación
      `w_512,h_512,c_fill,g_face,q_auto,f_auto`; iniciales si no hay foto. Sync obligatorio al
      abrir el proyecto en Android Studio
- [x] **10.4** Logout y cierre forzado por `active = false` siguen funcionando
- [x] **10.5** Bloque 11 de la Fase 1 (perfil del docente) entregado por Diego en `8728cbf`,
      `c3eb819` y `14b508b`; integrado aquí y verificado con el merge `06e606a`

**Verificación (17 nov 2026):** `assembleDebug` verde · **275 tests, 0 fallos** (eran 271) ·
`gentle-ai review assess` = `high` por `scripts/notify/index.js` → verificador independiente
final en curso. Nota: `coil 3.6.3` usa el paquete `coil3.compose`, no `coil.compose`.

**Decisión tomada: agregar Coil**

El dueño del proyecto eligió agregar la dependencia pese al peso del APK. Quedó resuelta la
discrepancia histórica (la Fase 1 dejaba las fotos abriéndose en el navegador). Versión confirmada
en la documentación oficial: **3.6.3**, con `coil-compose` y `coil-network-okhttp`.

> Coil habilitó el pendiente **11.4** del docente y el bloque 11 quedó cerrado por Diego con
> `8728cbf`, `c3eb819` y `14b508b`: datos reales, saludo, foto persistida y ajustes de cuenta.
> En esta fase no se tocó código del docente; la integración de ambas ramas ocurrió después en `main`.

**Cómo probarlo**

1. Entrar como directora → el perfil muestra su nombre y correo reales.
2. Cambiar la foto → sube a Cloudinary y `users/{uid}` queda con `photoUrl`/`photoPublicId`.
3. Cerrar y reabrir la app → la foto sigue ahí.
4. Cerrar sesión → vuelve al login; poner `active = false` en la consola expulsa a la directora.

**Criterio:** el perfil de la directora muestra datos reales y su foto sobrevive al reinicio.

---

## Cierre de la Fase 2

- [ ] **C1** La directora crea un grupo y dos alumnos desde la app — **código listo**, falta
      ejecutar la prueba en el teléfono
- [ ] **C2** El docente los ve en su pase de lista **sin tocar código** — **código listo**, misma
- [ ] **C2** El docente los ve en su pase de lista **sin tocar código** — **código listo**, misma
      prueba manual
- [x] **C3** `assembleDebug` verde (verificado en varias corridas)
- [x] **C4** `testDebugUnitTest` verde: **275 tests, 0 fallos** (piso 219 de la Fase 1)
- [ ] **C5** `connectedDebugAndroidTest` verde en dispositivo físico — `adb` no está en el PATH
      de este equipo; lo corre el dueño del proyecto
- [x] **C6** Reglas e índices publicados: regla de `users` (D1) e índice
      `attendanceSessions: schoolId + date`, ambos con `Deploy complete!`
- [x] **C7** `FASE2 Directores.md` actualizado bloque por bloque y `CONTEXT.md` §6.3 actualizado
      con el índice `attendanceSessions: schoolId + date desc`. El resto de campos usados ya
      estaban documentados. Dos verificadores independientes: notificaciones `verified` y
      Coil/perfil `verified`

### Evidencia mínima esperada

| Check | Cómo se demuestra |
|---|---|
| Alta de cuentas | `Authentication` + `users/{uid}` con el rol, y la sesión de la directora intacta |
| Grupo nuevo | `groups/{id}` con `schoolId`, `teacherIds` y el `groupIds` del docente actualizado |
| Alumnos nuevos | Dos documentos con `active: true` + `studentCount` incrementado en la consola |
| Criterio de cierre | Pase de lista del docente con los dos alumnos nuevos, sin recompilar |
| Baja lógica | `active: false` y expulsión automática al reabrir |
| Configuración | `schools/{id}.radiusMeters` cambiado y reflejado en el docente tras refresco |
| Notificaciones | Documento en `notifications` + push recibido en el teléfono |

---

## Continuación 18 nov — tres pedidos del dueño del proyecto

Decisión del usuario: **dar de alta al docente desde la app**, no por script. Se amplió la regla de
`users` (desplegada con `Deploy complete!`) para que el directivo cree cuentas de **docente o
tutor** de su plantel; nunca otro directivo, y sin borrado.

| Pedido | Entrega |
|---|---|
| Alta de docente | Formulario real en la pestaña Docentes (y tutor en Padres), con contraseña temporal. Usa una instancia **secundaria** de Firebase Auth para no desloguear a la directora; el documento se escribe con la app primaria para que `isDirectivo()` siga evaluando contra la sesión real |
| Alta de alumno | Ya existía en la pestaña Alumnos; ahora también desde el detalle de grupo, con el grupo preseleccionado |
| Tres puntitos de Grupos | El `MoreVert` de cada tarjeta (antes `.clickable { }`) abre un menú con **Ver detalle** y **Agregar alumno** |

**Detalles que importan**

- `AccountRole` es un enum cerrado (`DOCENTE`, `TUTOR`): no se puede representar un directivo en
  dominio, UI ni datos. El rol se infiere de la pestaña, sin desplegable que permita escalar.
- El documento nuevo cumple exactamente la regla: `uid` = id del documento, `schoolId` del
  directivo, `active: true`, `createdAt` con timestamp de servidor y campos de identidad como
  strings. `signOut()` de la instancia secundaria va en `finally`.
- Si falla el escrito en Firestore después de crear la cuenta Auth, se intenta borrar esa cuenta
  huérfana y se avisa con el flag `orphaned`; nunca se reporta éxito falso.
- Ruta nueva `admin/users/new/{groupId}` convive con la ruta sin argumento: la pestaña Alumnos
  sigue eligiendo el primer grupo; el detalle y el menú de tarjeta preseleccionan el suyo.

**Verificación:** `assembleDebug` verde · **279 tests, 0 fallos** (eran 275) · assess `high` por
`scripts/notify/index.js` → verificador independiente: **1 issue encontrado y corregido**.

> **Bug corregido tras la verificación independiente:** `orphanedAfterCleanup` usaba el valor de
> `delete().await()`, que es `null` en éxito, así que marcaba `orphaned = true` incluso cuando la
> limpieza del usuario huérfano funcionaba. Ahora es una función pura en `domain`
> (`orphanedAfterCleanup(hadUser, cleanupSucceeded)`) con 3 tests que fijan el caso éxito, fallo y
> "no había usuario que borrar". El mensaje de error de la UI ya no miente.

---

## Segunda continuación — cuatro fallos reportados en ejecución

El dueño del proyecto probó la app y reportó cuatro problemas. Diagnóstico y corrección:

| Síntoma | Causa real | Corrección |
|---|---|---|
| Alta de alumno: "sin permiso" | La consulta previa del roster filtraba por `groupId` + `active` pero **no** por `schoolId`; las reglas no filtran, así que un documento ajeno hacía fallar toda la consulta | Agregado `whereEqualTo("schoolId", …)` + índice `students: schoolId + groupId + active`, **desplegado** |
| No se ven reportes | La consulta por rango no tenía `orderBy` → Firestore ordena `date` **ASC**, pero el índice desplegado era `date DESC` | Índice `attendanceSessions: schoolId ASC + date ASC` **desplegado** + `orderBy` explícito en el repositorio |
| No se puede editar el perfil de la directora | El callback `onEditProfile` nunca se pasaba desde la navegación | Diálogo interno de nombre/apellido/teléfono → `updateProfile` con solo campos editables |
| Tres puntitos superiores del detalle de grupo no hacen nada | `onMore = {}` desde la navegación | Menú real: **Editar grupo**, **Agregar alumno**, **Eliminar grupo** con confirmación y bloqueo si `studentCount > 0` |

> **Nota técnica sobre el borrado de grupo:** se sospechó que la regla de `schedule` usaba `get()`
> sobre un grupo borrado en el mismo batch. No es un problema: `get()` lee el estado **anterior**
> al batch, donde el grupo todavía existe. `getAfter()` sí daría `null` después del borrado, así
> que la regla se dejó como está.

`CONTEXT.md` §6.3 actualizado con los dos índices nuevos. Verificación: `assembleDebug` verde ·
**283 tests, 0 fallos** (eran 279) · `gentle-ai review assess` = `high` por `scripts/notify/index.js`
(verificado en rondas anteriores) → **verificador independiente: `verified`, sin defectos nuevos**.

**Para probar en el teléfono (reinstalar el APK primero)**

1. Usuarios → Alumnos → `+`: el alta ya no debe dar `PERMISSION_DENIED`.
2. Reportes: elegir un rango → deben aparecer las sesiones reales; una tarjeta de "Asistencia de hoy".
3. Perfil → **Editar perfil**: cambiar nombre/apellido/teléfono y guardar; recargar y verificar.
4. Grupos → entrar a un grupo → tres puntitos arriba a la derecha → **Editar grupo**, **Agregar alumno** o **Eliminar grupo** (este último pide confirmación y se niega si el grupo tiene alumnos).

---

## Tercera continuación — alta de alumno, borrado con alumnos y ajustes del perfil

- **Alta de alumno (causa real encontrada)**: el chequeo de colisión hacía `get()` sobre un
  documento de `students` que **no existe todavía**. La regla de lectura referencia
  `resource.data`, que es `null` en un documento inexistente → `PERMISSION_DENIED`. Se reemplaza
  por una consulta con `schoolId` + `FieldPath.documentId()` (devuelve 0 filas en vez de fallar).
- **Borrar grupo con alumnos (decisión del dueño)**: **mover los alumnos a otro grupo**. El
  diálogo pedirá un grupo destino distinto; en un solo `WriteBatch` se reasignan los alumnos, se
  ajustan ambos `studentCount`, se borran los horarios y se borra el grupo. Ningún alumno se da de
  baja ni se elimina.
- **Perfil**: persistir el interruptor de notificaciones en `users.preferences`, diálogo real para
  cambiar contraseña (reautenticación + `updatePassword`) y acción de permisos cableada.

> **Configuración pendiente en este equipo:** `local.properties` no tiene
> `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_UPLOAD_PRESET` ni `MAPS_API_KEY`. Sin las dos primeras la
> subida de foto es imposible (la app lo reporta como "no configurada"); sin la tercera no carga
> el mapa. Son valores públicos según `CONTEXT.md` §7.2 (`dncnom7mu` / `docenteaula`) y la clave de
> Maps es privada: el dueño debe agregarlas el mismo. **No se escriben en el repo.**

**Verificación:** `assembleDebug` verde · **291 tests, 0 fallos** (eran 283) ·
`gentle-ai review assess` = `high` por `scripts/notify/index.js` (verificado en rondas anteriores)
→ **verificador independiente: `verified`**. Observaciones no bloqueantes: `onOpenSyncStatus`
sigue sin destino real (no hay pantalla de estado de sincronización), y un grupo destino de otro
plantel se rechaza por la regla de lectura antes que por el error tipado — el efecto es el mismo.

---

## Cuarta continuación — diagnóstico con emulador de Firestore

Se reproducieron las reglas desplegadas en el emulador con un directivo simulado
(`schoolId = sch-benito-juarez`):

| Operación | Resultado |
|---|---|
| Lectura de `users/{self}` y consulta de `groups` por `schoolId` | ✅ |
| `get()` sobre `students/{id}` inexistente | ❌ `permission-denied` — `Null value error` en la regla de lectura (L159) |
| Consulta `students` con `schoolId` + `documentId()` sobre id inexistente | ❌ `permission-denied` — error de variable `resource` en `list` |
| Consulta `groups` con `schoolId` + `documentId()` | ✅ |
| Batch alta de alumno (`set` + `increment` + `serverTimestamp`) | ✅ |
| `get`/`set` de `linkingCodes` | ✅ |
| Batch borrar grupo con alumnos (mover + contadores + horarios + grupo) | ✅ |

**Conclusión:** el alta de alumno tenía **dos** lecturas prohibidas seguidas (la primera arreglada
en la ronda anterior, la segunda recién descubierta). Se eliminó la segunda: el id del alumno ahora
lleva UUID y no requiere ninguna consulta previa. Los batches pasan con las reglas reales.

Para el borrado de grupo, el dueño confirmó que `users`, `groups` y `students` tienen
`schoolId = sch-benito-juarez`, así que los datos están consistentes. Como el batch pasa en el
emulador pero falla en el dispositivo, se agrega el **código crudo de Firestore** al mensaje de
error para identificar la escritura exacta en la próxima corrida.

> El emulador se corrió con `@firebase/rules-unit-testing` en
> `%TEMP%\opencode\aulasegura-rules-clean`, no dentro del repo. Nada de eso se versiona.

Diagnóstico agregado y verificado: `assembleDebug` verde · **293 tests, 0 fallos** · verificador
independiente `verified`. El detalle sanitizado de Firestore (≤160 chars, sin secretos) se muestra
en los mensajes de error de alta de alumno, borrado de grupo y generación del código de
vinculación. APK para probar: `app\build\outputs\apk\debug\app-debug.apk`.

---

## Quinta continuación — alumnos y horarios en el detalle de grupo

Pedido del dueño del proyecto:

1. La pestaña **Alumnos** del detalle de grupo debe mostrar la lista real del grupo (antes era una
   tarjeta que decía "vista de ejemplo"). Se inyecta `AdminStudentRepository` en
   `AdminGroupDetailViewModel`, se filtran los alumnos del plantel por `groupId` y se pintan con
   nombre, número de lista y estado Activo/Inactivo. El botón "Agregar alumno" se mantiene.
2. Los bloques de horario deben permitir elegir **docente → materia que imparte → horario**. El
   selector de docente se restringe a `UserRole.TEACHER`; las materias salen de
   `users.subjects` del docente elegido (un docente puede impartir varias); si no tiene materias
   cargadas se permite escribirla manualmente con una pista clara. Se valida `HH:mm` y que el fin
   sea posterior al inicio, sin cambiar la forma del documento `schedule`.

Verificación: `assembleDebug` verde · **299 tests, 0 fallos** (eran 293) ·
`gentle-ai review assess` = `high` por `scripts/notify/index.js` (verificado en rondas previas) →
**verificador independiente: `verified`, sin hallazgos**. APK:
`app\build\outputs\apk\debug\app-debug.apk`.

**Ajuste del dueño (quinta continuación, día posterior):** en Horario el selector de docente debe
mostrar **solo los docentes asignados al grupo** (`groups.teacherIds`), no todos los del plantel.
Cada docente asignado conserva sus materias desde `users.subjects`, y un grupo puede tener varios.
Se restringe `teacherPickerOptions` con los ids asignados y se agrega una pista cuando el grupo todavía
no tiene docentes. Un `teacherId` viejo que ya no esté asignado sigue visible en ese bloque para
poder corregirlo sin perder el dato. Verificación: `assembleDebug` verde · **302 tests, 0 fallos** ·
`gentle-ai review assess` = `high` por `scripts/notify/index.js` (verificado en rondas previas) →
**verificador independiente: `verified`, sin hallazgos**. APK:
`app\build\outputs\apk\debug\app-debug.apk`.

---

## Sexta continuación — número de lista automático

Pedido del dueño: la directora **deja de escribir el número de lista** al dar de alta un alumno.
El número se calcula como en una lista de clase: orden alfabético por apellidos, con la misma
regla que usa el docente en el pase de lista.

**Decisión del dueño (opción A):** insertar un alumno renumera **todos** los alumnos activos del
grupo en la **misma operación atómica**. No es una lista append-only: los números siempre reflejan
el orden alfabético vigente. No se agregan campos nuevos al documento `students`.

- Orden centralizado en `domain/model/StudentListOrdering.kt`: `Collator.getInstance(Locale("es",
  "ES"))` con `strength = Collator.PRIMARY` (ignora acentos y mayúsculas, `ñ` va después de `n`),
  desempate por `firstName` con el mismo `Collator` y, para nombres idénticos, por id del
  documento. Así "Pérez" precede a "Pineda" por la segunda letra del primer apellido.
- `AdminStudentInput` ya no recibe `listNumber`; `newStudentDocument` lo recibe como parámetro
  explícito. La función pura `orderStudentList(existing, newStudent)` devuelve la lista ordenada
  con `listNumber = índice + 1` y se prueba sin Firestore.
- `FirestoreAdminStudentRepository.createStudent` ejecuta una sola transacción: lee el roster
  activo del grupo (con el filtro `schoolId`, exigido por las reglas), calcula los números y
  escribe en la misma transacción los `listNumber` cambiados de los alumnos existentes (solo ese
  campo y `updatedAt`; `schoolId` nunca va en un `update`), el alumno nuevo con su número y el
  `studentCount` del grupo con `increment(1)`. El id conserva el formato de la semilla con sufijo
  UUID para no sondear la existencia del documento. Cero alumnos previos ⇒ el nuevo recibe 1.
- La UI quita el campo "Número de lista", su validación 1..99 y su mensaje de error. La fila del
  alumno y el filtro por texto siguen usando el `Int` almacenado.
- **Nota de plataforma:** el SDK de Firestore para Android expone `Transaction.get` solo con
  `DocumentReference`, no con `Query`. Los ids del roster se enumeran con la consulta
  `schoolId + groupId + active` antes de la transacción y cada documento se relee con `tx.get(ref)`
  dentro de ella; todas las lecturas siguen siendo anteriores a las escrituras.
  **Limitación residual:** un alumno insertado por otro cliente entre la enumeración y el `commit`
  queda fuera del conjunto leído, así que dos altas verdaderamente concurrentes podrían repetir
  número. Con una sola directora operando es poco probable; eliminarlo requiere un documento de
  roster por grupo que `setActive` también mantenga.

Verificación: `assembleDebug` verde · **306 tests, 0 fallos** · sin cambios en
`docs/firestore.rules` ni en índices. APK: `app\build\outputs\apk\debug\app-debug.apk`.

---

## Séptima continuación — detalle de alumno, nombres de grupo, titular, PDF y permisos

Pedido del dueño, seis puntos (el sexto —llaves de Maps y Cloudinary— ya fue resuelto
por el orchestrator en `local.properties` y no se tocó en este cambio):

1. **Detalle y baja del alumno.** La fila de la pestaña Alumnos ya no ignora el toque:
   abre un diálogo con nombre, CURP (o «sin CURP»), número de lista, grupo, estado,
   cantidad de tutores y el código de vinculación vigente. El código se obtiene con
   `AdminStudentRepository.getLinkingCode()`, una consulta de **un solo campo**
   (`linkingCodes whereEqualTo("studentId", …)` con `Source.SERVER`) que usa el índice
   automático de Firestore: no se agregó `schoolId` porque eso exigiría un índice
   compuesto nuevo. Se devuelve el primer código con `usedBy == null` y `expiresAt`
   no vencido. La baja usa el camino existente `setActive(id, false)`, que ya hace
   `active` + `groups.studentCount increment(-1)` en un lote, y luego refresca la lista;
   un fallo se muestra en el propio diálogo.
2. **Nombre real del grupo.** Los grupos creados en la app tienen id aleatorio de
   Firestore, así que «4° D» se veía como ese id. El ViewModel carga los grupos con
   `AdminGroupRepository.getGroups()` y arma `Map<groupId, name>`; `groupLabel`, la
   fila del alumno y el desplegable de filtro usan ese nombre. Los ids de semilla
   (`grp-4d`) siguen con el formato «4° D» como fallback.
3. **Sin «titular».** Se quitó el concepto de la interfaz del detalle de grupo y de
   las filas de docentes; un grupo puede tener varios docentes y ninguno se destaca.
   Los documentos nuevos ya no escriben `titularTeacherId`, y `assignTeacher` dejó de
   hacer la lectura extra y de setear el campo en el lote. `AdminGroup.titularTeacherId`,
   `Roster`, `GroupEntity` y las columnas de Room quedan intactos por compatibilidad:
   no hay migración ni cambio de esquema.
4. **Exportación de reportes real.** El botón de descarga genera un PDF con
   `android.graphics.pdf.PdfDocument` + `Canvas` (API 19+, sin dependencias nuevas).
   `reportPdfLines(state)` es una función pura con el periodo, el resumen (porcentaje,
   sesiones, presentes/faltas/retardos/justificadas y total de marcas), el desglose por
   grupo y los días con más faltas; el render paginado y el `Intent` quedan separados.
   El archivo se escribe en `cacheDir/reportes/reporte-<periodo>.pdf` y se abre con
   `ACTION_VIEW`, `FileProvider` y `grantUriPermission`. El `UiState` sale del
   ViewModel que ya crea la pantalla, sin instanciar uno segundo en la navegación.
5. **Perfil y permisos.** Causa raíz de la foto: `AdminProfileScreen` guardaba la
   captura en `cacheDir/perfil`, pero `file_paths.xml` solo declaraba
   `cacheDir/incidentes/`; `FileProvider.getUriForFile` lanzaba y el `runCatching`
   borraba el archivo silenciosamente. Se agregó la entrada `perfil` (y `reportes`).
   El interruptor de notificaciones sigue persistiendo en
   `users.preferences.notificationsEnabled`. La pantalla de permisos reemplazó los
   estados ficticios por `ContextCompat.checkSelfPermission` y
   `rememberLauncherForActivityResult` para los permisos **declarados**
   (`POST_NOTIFICATIONS` desde API 33 y `ACCESS_FINE_LOCATION`); los rechazos
   permanentes ofrecen «Abrir ajustes» con `ACTION_APPLICATION_DETAILS_SETTINGS`.
   No se solicita `CAMERA`: el manifiesto no lo declara y la app usa la cámara del
   sistema vía `TakePicture` + `FileProvider`, que no requiere permiso de runtime.

> No se tocó `docs/firestore.rules`, índices, `local.properties`, esquemas ni
> migraciones de Room. Se quitó el botón circular «Filtrar» (`Icons.Filled.Tune`) al lado
> del título «Incidentes» en `AdminIncidentsScreen.kt`, junto con su parámetro `onFiltrar`
> y su cableado en el NavHost; el callback ya era un `{}` vacío, así que no se perdió
> funcionalidad. Los ítems C4/C2/P0.4/2.5 siguen como estaban.

**Verificación:** `assembleDebug` y `testDebugUnitTest` en verde — conteo final:
`308`.

---

## Riesgos anotados

1. **Las reglas son contrato compartido.** D1 y D2 las cambian: avisar antes de mergear y
   desplegar. La Fase 3 lee los mismos documentos.
2. **La denormalización es la parte frágil.** `users.groupIds`, `groups.studentCount` y
   `students.tutorIds` no se mantienen solos. Cada alta o asignación tiene que escribir todos
   los lados en una sola operación.
3. **`data/` y `domain/model` los toca una sola persona a la vez** (`CONTEXT.md` §10). La Fase 1
   terminó, su bloque 11 quedó entregado y ambas ramas ya conviven en `main`: aun así, coordinar
   antes de tocar esas carpetas.
4. **`main` siempre compila.** Un rojo bloquea al otro. `assembleDebug` y `testDebugUnitTest` en
   verde antes de cada merge, como en la Fase 1.
5. **Spark sin Functions.** Ningún flujo de esta fase puede depender de un disparador automático:
   ni alta de cuentas, ni push. Las dos cosas son manuales por diseño.
6. **Los datos del mockup no son los datos reales.** 85/3 del diseño contra 111/4 de la semilla.
   Si una pantalla muestra una cifra redonda "bonita", sospechar que sigue hardcodeada.

---

## Octava continuación — ubicación editable del plantel

Pedido del dueño: la directora debe poder cambiar la ubicación del plantel escribiendo
latitud/longitud **o** moviendo el pin directamente en el mapa.

- `AdminCampusUi.kt`: `CoordinateValidation` y `validateCoordinates` (rango -90..90 y
  -180..180, vacío, no numérico, NaN/Inf y rechazo de `0,0`, porque `AdminSchool.hasLocation`
  lo trata como "sin ubicación"). El decimal con coma se normaliza y todo se formatea con
  `Locale.US` para que un dispositivo en español no rompa el round-trip. `canSave` incluye la
  validez de las coordenadas y `buildCampusConfig` es puro, para probarlo sin Firestore.
- `AdminCampusViewModel.kt`: `setLatitude`, `setLongitude` y `setLocation` alimentan el
  borrador; `save()` construye el `AdminCampusConfig` con las coordenadas parseadas, nunca con
  las del documento leído. Tras guardar se conserva el texto exacto tecleado.
- `CampusMap.kt`: nuevo `EditableCampusMap` aditivo con `GoogleMap`, el `Circle` de la geocerca
  y un marcador arrastrable. En maps-compose 6.1.2 `Marker` no expone callbacks de arrastre:
  se observa `MarkerState.position` con `snapshotFlow` mientras `isDragging`, y `onMapClick`
  también relocaliza el pin. El `CampusMap` del docente no se modificó.
- `AdminCampusScreen.kt`: `LocationCard` muestra los dos campos con teclado decimal, el error
  en línea y el mapa editable; se eliminaron el mapa dibujado a mano y `CoordinateBox`, que ya
  no tenían uso.
- `AdminCampusUiTest.kt`: 10 tests nuevos (rangos, vacío, NaN/Inf, `0,0`, coma decimal,
  round-trip con locale español, proveniencia del config y compuerta de `canSave`).

**Integración con `origin/main`:** la rama `feat/admin-fase2` partió de `main` y se mergeó
`origin/main` (9 commits de la Fase 1 del docente). Hubo 8 conflictos de unión —
`build.gradle.kts`, `libs.versions.toml`, `AuthRepository`, `AuthRepositoryImpl`, `AppContainer`,
`PermissionsScreen`, `file_paths.xml` y `AuthViewModelsTest` — resueltos conservando **ambos**
lados. En particular Coil 2 (`coil.compose`, docente) y Coil 3 (`coil3.compose`, directora)
coexisten como dependencias separadas, y `PasswordChangeResult` conserva las siete variantes
del docente más la rama `Failure` que usa la directora.

**Verificación:** `assembleDebug` y `testDebugUnitTest` en verde · **356 tests, 0 fallos**
(318 antes del merge). Sin cambios en `docs/firestore.rules` ni en índices en este cambio.
Pendiente la prueba en dispositivo del arrastre dentro del scroll vertical y de la foto del
perfil del docente con Coil 2.

---

## Novena continuación — consumidor de `requireLocation`

Decisión del dueño: **agregar el consumidor**, no quitar el switch. Con el interruptor en
`false`, el docente pasa lista sin esperar el GPS; con `true` (o campo ausente) el gate queda
igual que hoy.

- `SchoolSite` y `SchoolEntity` incorporan `requireLocation: Boolean = true`. El flag llega al
  gate por **las dos rutas**: la lectura de Firestore (`schoolEntityFrom`, con `as? Boolean ?: true`
  para campo faltante o malformado) y la caché Room, así que sobrevive al modo avión.
- Room sube de **3 a 4** con `MIGRATION_3_4`:
  `ALTER TABLE schools ADD COLUMN requireLocation INTEGER NOT NULL DEFAULT 1`. Es aditiva y no
  destructiva; las filas existentes siguen exigiendo GPS. Se exportó `4.json`.
- `Geofence.shouldRequireGps(site)` falla cerrado (`null` ⇒ exige GPS). `GeofenceResult.honouring(site)`
  fija `verifiedOverride = false` cuando el flag está apagado: se conservan la distancia,
  precisión y radio reales, pero se anula la afirmación de "verificado".
- `TeacherLocationVerifyingScreen` short-circuitea a `onInsideRadius()` antes de mirar el fix,
  así que nunca espera GPS ni puede caer en la pantalla de bloqueo. El NavHost calcula **un solo**
  `GeofenceResult` y lo usa tanto para `setLocation(...)` como para el banner: un docente dentro
  del radio no puede producir una sesión `verified = true` mientras el plantel está optado out.
  El mapa deja de decir "Estás dentro de la escuela" y muestra "Ubicación no requerida para pasar lista".
- Tests nuevos en `GeofenceTest` y `SchoolMappersTest`: fail-closed con site nulo, `verified=false`
  con medición real, no-op cuando el flag está encendido y round-trip del campo con default `true`.

**Verificación:** `assembleDebug` y `testDebugUnitTest` en verde · **362 tests, 0 fallos**
(eran 356). Verificador independiente: sin blockers. Sin cambios en reglas ni índices.

> Fuera de alcance: `TeacherIncidentNewScreen` calcula el `verified` del **incidente** solo por
> distancia, sin consultar el flag. Si el dueño quiere que `requireLocation` aplique también a
> incidentes, es otro cambio.
