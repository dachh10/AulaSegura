# Bloque 3.4 — Justificar falta con adjunto en Cloudinary

Rama: `main` · entrega: commits en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantalla: `ParentJustifyScreen` (Stitch `9a38ede8a0894eb9886bd7197031d841`)

## Specs

**S1 — La pantalla justifica una falta real.** `ParentJustifyScreen` deja de mostrar "Viernes 31 de octubre / Mariana Gómez Ruiz · 3° B" escrito a mano y muestra el hijo vinculado, su grupo y la fecha de una falta real leída de `attendanceSessions` + `records/{studentId}`. Desde Asistencia se justifica la falta tocada; desde el acceso rápido de Inicio, la falta más reciente del mes. Si no hay falta que justificar o no hay hijo vinculado, la pantalla lo dice y **Enviar** queda deshabilitado: nunca se inventa una fecha.

**S2 — El adjunto va a Cloudinary.** Carpeta `aulasegura/justifications/{justificationId}` (ya existe `CloudinaryFolder.JUSTIFICATIONS`). En Firestore solo el mapa `attachment { publicId, url, type }`; nunca base64 ni Firebase Storage.

**S3 — El documento respeta el contrato.** `justifications/{justificationId}` con `studentId`, `tutorId`, `schoolId`, `groupId`, `dateFrom`, `dateTo`, `reason`, `description`, `attachment`, `status: "pendiente"`, `createdAt` (`CONTEXT.md` §6). Las reglas publicadas ya permiten el `create` del tutor (`firestore.rules:413-417`): **no se tocan reglas**.

**S4 — Offline sin Room nuevo.** El formulario promete "se guardará en tu celular y se enviará después". Se cumple con la outbox existente: el archivo queda en `filesDir` y se encola `UPSERT_JUSTIFICATION` (ya está en `SyncOperation` y en `CONTEXT.md` §8.2); el worker sube a Cloudinary y recién entonces crea el documento. El estado del adjunto viaja en el `payloadJson` de la fila, así que no hace falta entidad Room ni migración (la base sigue en `version = 4`).

**S5 — Motivos del schema, no del diseño.** Los chips se mapean a `enfermedad | cita | viaje | otro` (`CONTEXT.md` §6); la etiqueta visible sigue siendo la del diseño ("Enfermedad", "Cita médica", "Asunto familiar", "Otro").

**S6 — Reutilizar lo existente.** `CloudinaryUploader` (+ `uploadImage`/`uploadDocument`), `CloudinaryFolder.JUSTIFICATIONS`, `OutboxDao`/`SyncRunner`, el patrón de `TaskAttachmentStore` para guardar el archivo, `AttendanceTimes`/`SchoolCalendar` para fechas y el patrón `ViewModel + Ui + AppViewModelFactory` del tutor. No se duplica lógica de sync ni de fechas.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S3, S4 | `JustificationPayload` + codec JSON y `OutboxDao.updatePayload` (sin migración) | inline | ✅ |
| T2 | S2, S4 | `JustificationAttachmentStore` (filesDir) y `JustificationSyncHandler` (sube y crea) | inline | ✅ |
| T3 | S3, S4 | `JustificationRepository` + `QueuedJustificationRepository` (archivo → outbox → `SyncScheduler`) | inline | ✅ |
| T4 | S1, S5 | `ParentJustifyUi` (estado, motivos, resolución de la falta) y `ParentJustifyViewModel` | inline | ✅ |
| T5 | S1 | `ParentJustifyScreen` con estado real + wiring de navegación (Inicio y Asistencia) | inline | ✅ |
| T6 | S6 | Wiring en `AppContainer` (store, repositorio, handler) y rama del factory | inline | ✅ |

## Log

**L1 — 19 nov 2026, pedido del dueño (verbatim).** "ya la probe sigue con el paso 3.4 justificar la falta con adjunto en cloudinary de aqui te puedes giiar pátra hacerlo con docente tarea ya que ahi tambien usamos para adjuntar un pdf y subirlo a cloudinary."

**L2 — 19 nov 2026, decisiones de diseño tomadas al leer el contrato:**

- **Qué falta se justifica.** Desde Asistencia, la falta tocada en el detalle reciente (el botón ya existe y apunta a una ausencia concreta). Desde Inicio, la falta más reciente del mes del hijo seleccionado. Sin fecha en la ruta se resuelve con `ParentRepository.loadMonth`, no con `now()`: el formulario nunca inventa una fecha. Si no hay ninguna falta, `Enviar` se deshabilita y la tarjeta lo explica.
- **Sin estado local nuevo.** La tabla `justifications` no existe en `CONTEXT.md` §8.1 y no se agrega: el justificante es sólo escritura, no se relee en ninguna pantalla del tutor. El estado del adjunto vive en el `payloadJson` de la fila del outbox y el `id` del documento es el UUID generado al encolar, así que reintentar reescribe el mismo documento en vez de duplicarlo. Room sigue en `version = 4`.
- **Upload dentro de `UPSERT_JUSTIFICATION`.** La regla del tutor sólo permite `create`, no `update`, así que el documento no puede crearse sin la URL. El handler sube primero y guarda `publicId`/`url` en la fila antes de crear; si el `set` falla después de subir, el reintento no vuelve a subir (`url` ya está en el payload). Es la misma garantía de orden que `UPLOAD_TASK_ATTACHMENT` → `UPSERT_TASK`, con una sola fila.
- **Sin cambios de reglas.** `justifications` ya permite `create` del tutor con `tutorId == uid`, `schoolId == mySchool()`, `studentId in myStudents()` y `status == 'pendiente'`. El bloque sólo escribe lo que la regla acepta.
- **Motivos.** El schema manda `enfermedad | cita | viaje | otro`; el diseño muestra cuatro etiquetas. "Asunto familiar" se guarda como `otro` con la descripción del tutor, porque el schema no tiene esa categoría y agregarla sería cambiar el contrato.

**L3 — 19 nov 2026, cierre del bloque (el trabajo inicial venía de opencode, sin commit y sin compilar).**

Se completó lo que faltaba para que el bloque esté entero:

- **`JustificationSyncHandler`:** el `when` sobre `UploadStep.Done, is UploadStep.Dropped` no compilaba (el `sealed interface` no declara `attachment`). Ahora se resuelve el adjunto con tres ramas explícitas y `Later` corta con `Retry` sin tocar la fila, así el archivo sigue en el teléfono para el siguiente intento.
- **`AppContainer`:** `justificationAttachmentStore` (junto al store de tareas, mismo `filesDir`), `justificationRepository` con `syncScheduler` y analytics, `UPSERT_JUSTIFICATION` registrado en `syncHandlers`, y `parentJustifyViewModel(studentId, date)` en la factory (mismo patrón que `adminNewStudentViewModel`).
- **`AulaSeguraNavHost`:** `composable(Routes.PARENT_JUSTIFY_ARGS)` con `studentId` y `date` como argumentos de query **opcionales** (con `defaultValue = null`), así el deep link `parent/justify` y el acceso rápido de Inicio siguen entrando a la misma pantalla. Se conectan los siete callbacks de la pantalla al ViewModel y `onSent` vuelve atrás.
- **Tests (19 nuevos, 59 suites / 436 tests / 0 fallos):**
  - `ParentJustifyViewModelTest` (9): sin faltas no se puede enviar, la falta más reciente del mes, la fecha tocada en Asistencia gana, un día presente no se convierte en justificante, se busca el mes anterior, el envío encola la fecha real con el motivo del schema, un fallo de almacenamiento deja el formulario abierto, un fallo de lectura ofrece reintento y el mapeo `Asunto familiar → otro`.
  - `JustificationPayloadTest` (10): round-trip del JSON, payload corrupto, claves del documento, `dateFrom`/`dateTo` como `Timestamp`, adjunto solo si está en Cloudinary, adjunto fallido fuera del documento, tipo `pdf`, y `isUploaded`.

**Límite conocido (no se resuelve aquí):** "Asunto familiar" y "Otro" comparten `reason = "otro"`, así que el directivo los distingue solo por la descripción. Si molesta en la revisión, se arregla en la bandeja del directivo (mostrar la descripción), no cambiando el contrato.

**Pendiente de humano:** probar en el teléfono el flujo Asistencia → tocar una falta → justificar con foto/galería → ver el documento en la consola, y el mismo flujo con el teléfono en modo avión y luego con red.
