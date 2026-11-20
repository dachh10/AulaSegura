# Bloque 3.7 — Chat del tutor con el docente del grupo

Rama: `main` · entrega: commit en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantallas: `ParentChatScreen` (Stitch `c52d05bdfa5b42f494d9cc0d7062ff90`), `ParentConversationScreen` (`889c39e23e6142c09cd11698a057830e`)

## Specs

**S1 — La lista es la del tutor, no un ejemplo.** `ParentChatScreen` deja de usar `sampleConversations()`: `ParentChatViewModel` observa las conversaciones de Firestore (`participants` contiene el uid) y las pinta con `parentChatRowOf`, que resuelve la hora relativa (`10:42`, `Ayer`, `Lun`) con el mismo formateador que la lista del docente. La búsqueda vive en el ViewModel (`filterParentChatRows`) para que el mismo criterio se pueda fijar con un test: nombre, quién es y último mensaje, sin distinguir mayúsculas.

**S2 — Nuevo mensaje: el docente del grupo de sus hijos.** El lápiz del encabezado abre una hoja con `tutorContacts()`, que camina el vínculo al revés: `students.whereArrayContains("tutorIds", uid)` → `groups/{id}.teacherIds`. Una fila por docente distinto: dos hermanos en el mismo grupo dan un chat, no dos. Sin hijos o sin docentes asignados la hoja lo dice con una frase; no se inventa un destinatario.

**S3 — El nombre del docente sale de su perfil, y si no se puede leer, el titular se nombra.** La regla de `users/{uid}` se abrió en modo `get` para un tutor sobre un docente del mismo plantel que comparte grupo (`users.groupIds` del tutor contra `groupIds` del docente); `list` sigue cerrado. La hoja y la lista muestran el nombre real, que es lo único que distingue a dos docentes del mismo grupo. Si esa lectura falla (sin red, grupo legado), el fallback no repite la misma fila: el titular se llama "Docente titular de 3° B" y el resto "Docente de 3° B".

**S4 — La conversación son los mensajes reales.** `ParentConversationScreen` recibe `ParentConversationUi`: encabezado desde el resumen de Firestore, mensajes desde RTDB (hasta 200, ordenados por `sentAt`), presencia del otro participante y recibo de lectura al abrir. El chip de día se dibuja **sólo en la primera burbuja de cada día** (`chatDayLabel`: Hoy, Ayer, día de la semana o fecha), no una vez al principio como el mockup de cuatro mensajes. El composer recorta a 2000 caracteres; sin conexión avisa que el mensaje saldrá al reconectar.

**S5 — Nada inventado en pantalla.** Se fue el estado "Pendiente de enviar": RTDB pinta la burbuja apenas la escritura llega a la caché local y el snapshot no trae confirmación por mensaje, así que marcarla pendiente mostraría un estado que la base no puede confirmar. La lista no dibuja el punto verde por fila (la presencia se observa en la conversación abierta, igual que en la lista del docente). El ícono de cámara del composer queda como parte del diseño, pero no adjunta: el chat con media está fuera de este bloque.

**S6 — "Hablar con el docente" abre Mensajes, no una ruta falsa.** El detalle y el mapa de incidentes navegaban a `Routes.parentConversation("1")`, un id de ejemplo. Ahora abren la pestaña Mensajes, donde el tutor elige al docente real de la lista; no se resuelve un docente adivinado desde el incidente.

**S7 — Sin cambios de reglas, colecciones, índices ni Room.** `docs/firestore.rules` ya permitía al tutor leer `conversations` donde participa, crear la suya con dos participantes y leer `students`/`groups` de su escuela; RTDB ya tenía el contrato `{uidA}__{uidB}` de `docs/rtdb.rules`. La base sigue en `version = 4`. Lo compartido se extrajo a `ui/chat/ChatFormatting.kt` y a `ChatContact.otherUid/otherRole`, y el subtítulo se deriva con `chatConversationSubtitle` (el mismo documento dice "Tutor de…" para el docente y "Docente · 3° B" para la familia).

**S8 — Cierre medible.** `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` verdes; tests nuevos para el chip de día, la búsqueda, la fila sin mensajes, el subtítulo por rol, las iniciales y la etiqueta del docente (nombre real / titular / grupo sin nombre). La prueba funcional con la cuenta real la hace el equipo: no hay dispositivo conectado.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S1, S4, S5 | `ParentChatUi`, `ParentChatRowUi`, `ParentTeacherUi`, `ParentConversationUi`, `ParentMessageUi`, `parentChatRowOf`, `filterParentChatRows`, `parentTeacherRowOf`, `parentMessagesOf` | `ui/screens/parent/ParentChatUi.kt` | ✅ |
| T2 | S1, S2, S3 | Estado de la lista, búsqueda, hoja de docentes y apertura de conversación | `ui/screens/parent/ParentChatViewModel.kt` | ✅ |
| T3 | S4, S5 | Mensajes, presencia, borrador, envío y `markRead`/`goOnline` | `ui/screens/parent/ParentConversationViewModel.kt` | ✅ |
| T4 | S1, S2, S5 | Lista real con lápiz, búsqueda y hoja de docentes sobre el diseño aprobado | `ui/screens/parent/ParentChatScreen.kt` | ✅ |
| T5 | S4, S5 | Conversación real con chips por día, burbujas y composer | `ui/screens/parent/ParentConversationScreen.kt` | ✅ |
| T6 | S7 | `chatDayLabel` reutilizable y `chatTimeLabel` apoyado en él | `ui/chat/ChatFormatting.kt` | ✅ |
| T7 | S3, S7 | `otherUid`/`otherRole`, `chatConversationSubtitle` y `tutorContacts()` | `domain/model/Chat.kt`, `domain/repository/ChatRepository.kt`, `data/repository/FirestoreChatRepository.kt` | ✅ |
| T8 | S1-S6 | Factory de los dos ViewModels y rutas con `conversationId`; `onTalkToTeacher` a Mensajes | `di/AppContainer.kt`, `navigation/AulaSeguraNavHost.kt` | ✅ |
| T9 | S1, S4, S8 | Tests del chip de día, búsqueda, fila vacía, subtítulo por rol e iniciales | `ui/screens/parent/ParentChatUiTest.kt`, `domain/model/ChatTest.kt` | ✅ |
| T10 | S3 | Regla `get` de `users` para el tutor, nombre real en `tutorContacts()`, `chatTeacherLabel` y refresco de `participantNames` al abrir | `docs/firestore.rules`, `data/repository/FirestoreChatRepository.kt`, `domain/model/Chat.kt` | ✅ |

## Log

**L1 — 19 nov 2026, pedido del dueño.** Entrar al repo de GitHub, ver si su colaboradora subió cambios, traerlos al proyecto local, dejar todo parejo y sin errores, y terminar el bloque 3.7.

**L2 — 19 nov 2026, sincronización con GitHub.**

- `fetch` a `origin` sin novedades: `main` local y `origin/main` en `d8ee2f0`; no hay commits nuevos de la colaboradora ni forks. Las dos ramas remotas (`feat/admin-fase2`, `feat/docente-pase-lista`) son anteriores a `main`.
- El trabajo previo del árbol (chat del docente, `ChatFormatting`, `otherUid`/`otherRole`, `tutorContacts`) quedó como estaba y se completó en este bloque.

**L3 — 19 nov 2026, decisiones al leer las reglas y el diseño.**

- **No se toca `users` para el nombre del docente.** La regla de lectura es `request.auth.uid == uid || (isDirectivo() && …)`: el tutor no puede leer el perfil del docente. La primera versión de `tutorContacts()` intentaba esa lectura y caía siempre al `catch`; se quitó para no gastar una petición que la nube niega y se documentó el fallback.
- **Una fila por docente, no por hijo.** Dos hermanos en el mismo grupo comparten docente y chat; la fila conserva el primer hijo para el encabezado. Si los hermanos están en grupos distintos, el chat con cada docente es independiente, que es lo que la familia espera.
- **El lápiz del encabezado.** El diseño 2.9 es la misma pantalla que la del docente con roles invertidos, y esa implementación ya tenía el botón "Nuevo mensaje" en el encabezado; el port visual del tutor lo había omitido. Se agregó para que el tutor pueda iniciar un chat sin depender de que el docente escriba primero.
- **Chips de día reales.** El mockup muestra un único "Hoy" porque tiene cuatro mensajes del mismo día. Con datos reales un chat puede cruzar semanas, así que el chip se calcula por grupo de días (`chatDayLabel`) y se dibuja sólo cuando cambia el día.
- **Sin "Pendiente de enviar".** El estado del mockup no tiene respaldo en RTDB: no hay ack por mensaje. Se prefiere no mostrarlo antes que mostrar algo que nunca se resuelve.
- **"Hablar con el docente" no adivina.** El botón del incidente apunta a la pestaña Mensajes; resolver el docente desde el incidente exigiría leer el grupo del alumno y elegir entre varios docentes sin que el tutor lo vea.

**L4 — 19 nov 2026, cierre del bloque.**

- **Tests (8 nuevos; 68 suites / 504 tests / 0 fallos):**
  - `ParentChatUiTest` (6): fila sin mensajes no inventa texto ni hora, contador no leído sólo si es mayor a cero, búsqueda por nombre/subtitle/último mensaje sin distinguir mayúsculas, chip de día una vez por día con horas reales, burbuja propia a la derecha, iniciales y grupo de la fila del docente.
  - `ChatTest` (2 nuevos): el subtítulo por rol ("Tutor de…" / "Docente · 3° B" / "Docente" / "Tutor").
- **Verificación:** `:app:testDebugUnitTest :app:assembleDebug` → **BUILD SUCCESSFUL**, 68 suites / 504 tests / 0 fallos, `app-debug.apk` generado.
- **Pendiente funcional (lo hace el equipo, no hay dispositivo conectado):** abrir Mensajes con el tutor, ver la lista real, tocar el lápiz, abrir el chat con el docente del grupo, enviar un mensaje y verlo llegar al docente; con modo avión, el mensaje debe quedar en la burbuja y salir al reconectar.
- **Límite conocido:** el tutor lee "Docente de 3° B" hasta que ese docente inicie el chat; el nombre real requiere abrir `users/{uid}` del docente a los tutores de sus grupos, decisión que queda para el dueño.

**L5 — 19 nov 2026, corrección del dueño: "el tutor puede enviar pero el docente no lo ve".**

Pedido textual: «no pues entonces esta mal, necesito que tu mismo tengas ese pensamiento de ingeniero de software que debes de tener ya que como tu dices el limite es que el tutor como tal si puede enviarle msj al docente pero desde lapantalla de docente no le aparece ese mensaje, tiene que ser el docente el primero en enviar msj para que aparezca en las dos partes, asi que necesito que corrijas eso, si cambia las reglas para que todo cuadre, necesito mas apoyo de ti en esa parte de logica.»

**Diagnóstico con datos reales** (Admin SDK sobre el proyecto `aulasegu`, no suposiciones):

- El envío nunca estuvo roto: los tres documentos de `conversations` y sus mensajes en RTDB existen y son legibles por ambos participantes.
- `groups/4° D` tiene **dos** docentes: `3lXe…` (`docente@aulasegura.com`) y `OepF…` (`dach@gmail.com`), con `titularTeacherId = OepF…`. `tutorContacts()` etiquetaba a los dos igual ("Docente de 4° D"), así que la hoja mostraba **dos filas idénticas** y el tutor elegía a ciegas.
- Las dos conversaciones del tutor existen: `3lXe…__lxSG…` tiene "que onda" del docente; `OepF…__lxSG…` tiene "que onda pa" del tutor con `unread[OepF…] = 1`. El mensaje estaba entregado a `OepF…`; la sesión de prueba era `3lXe…`. De ahí el "no aparece hasta que el docente escribe primero".
- Segundo defecto, encontrado al leer `participantRoles`: `initialDocument` escribía `user.role.name.lowercase()`, y `UserRole.TEACHER.name` es **"teacher"**, no el valor de Firestore `"docente"`. Las conversaciones creadas por el docente quedaban con un rol que la app no reconoce (rompe título y subtítulo). `UserRole.kt` ya lo advertía: nunca comparar contra el nombre del enum.

**Arreglo.**

- `docs/firestore.rules`: `users/{uid}` permite `get` a un tutor sobre un docente del mismo plantel que comparte grupo (`myGroups().hasAny(resource.data.get('groupIds', []))`); `list` sigue cerrado. Publicado con `firebase deploy --only firestore:rules` (compiló y se liberó).
- `tutorContacts()` lee `users/{teacherUid}.fullName` una vez por docente y arma la etiqueta con `chatTeacherLabel(fullName, groupLabel, titular)`, con el titular tomado de `groups/{id}.titularTeacherId`: dos docentes del mismo grupo nunca comparten etiqueta, ni siquiera sin red ("Docente titular de 4° D" vs "Docente de 4° D").
- `ensureConversation()` refresca `participantNames.{other}` al abrir, así una conversación vieja deja de mostrar "Docente de 4° D".
- `initialDocument` escribe `user.role.firestoreValue`.
- Migración de datos, una pasada idempotente: `participantRoles.teacher → docente` en `3lXe…__61JJ…` y el nombre real en `participantNames` de las dos conversaciones del tutor; verificado leyendo los tres documentos después.
- Costo aceptado y anotado en `FASE3.md` §5: la rama nueva abre el documento completo del docente (incluye `email` y `phone`), porque Firestore no filtra por campo. La alternativa, si se quiere cerrar, es denormalizar `teacherNames` en `groups/{id}`.

**Verificación:** `:app:testDebugUnitTest :app:assembleDebug` → **BUILD SUCCESSFUL**, 68 suites / 505 tests / 0 fallos. Reglas compiladas y publicadas. Prueba funcional sigue pendiente del equipo (no hay dispositivo).
