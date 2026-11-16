# Fase 1 — Docente (Checklist de ejecución)

> Abrí este archivo en el editor dividido a la derecha (`clic derecho en la pestaña → Split Right`)
> y dejalo visible mientras trabajás. Tildá cada ítem al terminarlo, no antes.
>
> Referencia técnica: `CONTEXT.md`. Roadmap general: `PLAN.md` §Fase 1. Fase anterior: `FASE0.md`.

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso · `[!]` bloqueado

**Tablero de bloques** — se actualiza **en el mismo commit** que entrega el bloque, no después:

| Bloque | Qué entrega | Estado |
|---|---|---|
| **1** — Datos del docente | grupos y roster reales, cache-first | ✅ **hecho** · `f9fe627` |
| **2** — Pase de lista online | sesión + 28 registros en un solo batch | ✅ **hecho** · `49b2d76` |
| **3** — GPS y geocerca | `verified` contra `schools.location` | ✅ **hecho** · `43fcf21` |
| **4** — Pase de lista offline | el criterio de la fase, en modo avión | ✅ **hecho** · `118238b` |
| **5** — Historial | sesiones pasadas del docente | ✅ **hecho** · `cb69681` |
| **6** — Incidentes | reporte con severidad | ✅ **hecho** · `d08f75f` |
| **7** — Media del incidente | foto comprimida a Cloudinary | pendiente |
| **8** — Tareas y avisos | publicar con audiencia | pendiente |
| **9** — Chat | RTDB y presencia | pendiente |
| **10** — Notificaciones y FCM | push enviado desde la consola | pendiente |
| **11** — Perfil | datos reales del docente | pendiente |

**Criterio de cierre de la fase** (`PLAN.md`): con **modo avión** activo, el docente pasa lista y
registra un incidente con foto; al reconectar, ambos aparecen en Firestore **sin duplicados** y
con la media subida.

---

## Punto de partida

La Fase 0 dejó la fundación. Esto ya existe y **no se vuelve a construir**:

| Ya está | Dónde |
|---|---|
| Room con 9 entidades, DAOs con `Flow`, `syncState` | `data/local/` |
| Outbox transaccional + worker único con backoff | `sync/` |
| Handler de asistencia (batch + contadores recalculados) | `sync/AttendanceSyncHandler.kt` |
| Subida a Cloudinary + compresión de imagen | `data/remote/cloudinary/` |
| Contenedor manual de dependencias | `di/AppContainer.kt` |
| Analytics tipado, Crashlytics, Remote Config validado | `domain/telemetry/`, `data/remote/` |
| Asistente con AI Logic (sin cablear a pantalla) | `data/remote/ai/` |
| 45 pantallas de la Etapa 1, **con datos de ejemplo** | `ui/screens/` |

Lo que **falta** y es el trabajo real de esta fase:

- **No hay ni un repositorio de Firestore.** `domain/repository/` solo tiene `AuthRepository` y
  `AiAssistant`. Todo lo demás es UI sobre listas hardcodeadas.
- `firebase-database` y `play-services-location` están en el catálogo pero **no** en
  `app/build.gradle.kts`.
- **No hay reglas de RTDB** ni sección `database` en `firebase.json`. Hoy el chat no tiene
  contrato de seguridad.

### Antes del bloque 1

- [ ] **P0.1** Revocar la key de la service account del seed (o dejar anotado por qué sigue viva)
- [ ] **P0.2** Confirmar en consola `allowed_formats = jpg,png,webp` del preset `docenteaula`
- [x] **P0.3** Rama de trabajo: `feat/docente-pase-lista` (nadie pushea a `main`, ver `CONTEXT.md` §10)

> **Regla de convivencia:** si una tarea toca el **esquema de Firestore**, se avisa antes de
> mergear. `domain/model` y `data/` los toca una sola persona a la vez (`CONTEXT.md` §10).

---

## Bloque 1 — Datos del docente (PLAN 1.1)

> **Estado: ✅ HECHO** — 13 nov 2026 · commit `f9fe627` · 78 tests unitarios.
> Salvedad honesta: **1.5** sigue `[~]` porque `TeacherHomeScreen` todavía muestra datos mock;
> cablearlo es **11.2**. *Mis grupos* y el roster sí leen de Firestore y de la caché.

- [x] **1.1** Modelos de dominio: `Group`, `Student` (no reusar las entidades de Room en la UI)
- [x] **1.2** `GroupRepository` / `StudentRepository` en `domain/repository/`
- [x] **1.3** Implementación Firestore en `data/repository/` con las consultas que las reglas exigen
- [x] **1.4** Estrategia **cache-first**: leer de Room con `Flow`, refrescar de Firestore a Room en segundo plano
- [~] **1.5** `TeacherHomeScreen` y `TeacherGroupsScreen` cableadas a datos reales
- [x] **1.6** Mappers Firestore ↔ Room ↔ dominio en un solo lugar (`data/mapper/`)

> **Estado de 1.5:** *Mis grupos* ya lee de Room y refresca de Firestore (`TeacherGroupsViewModel`).
> El **roster** ya existe en `StudentRepository.observeRoster()`, pero todavía no hay pantalla que lo
> pinte: `onVerAlumnos` sigue cayendo en `PlaceholderScreen` y la lista real llega con **2.8**
> (`RollCallScreen`). Los contadores del inicio son de asistencia y también son del bloque 2.
> No se tildó para no declarar media entrega como entrega.
>
> **Hallazgo del bloque 1 (corregido acá):** con modo avión y arranque en frío la app volvía al login.
> `restoreSession()` resolvía el perfil leyendo `users/{uid}` en Firestore, y con la caché persistente
> de Firestore desactivada esa lectura no existe sin red. Se agregó `profile` a Room (versión 2,
> `MIGRATION_1_2`) como caché del perfil: si Firestore no responde, se usa el último perfil válido.
> La regla de fondo: **el arranque offline no puede depender de la red**, o el criterio de la fase
> ("modo avión, cerrar y abrir la app") es imposible de cumplir.
>
> **Segundo hallazgo, más peligroso:** sin red, un `get()` de Firestore puede resolverse como
> **snapshot vacío** en lugar de fallar. `refreshRoster` habría concluido que todos los alumnos se
> fueron del grupo y **borrado el roster cacheado** — justo en el escenario para el que existe la caché.
> Las dos consultas piden `Source.SERVER`: o el servidor contesta de verdad, o falla y la caché no se
> toca. Con eso además aparece el aviso de "sin conexión" que antes nunca llegaba, porque el refresco
> quedaba esperando sin límite (`withTimeout` de 12 s).

**Criterio:** el docente abre la app y ve sus 3 grupos con los conteos reales (28 / 30 / 27); en
modo avión, después de una carga online, sigue viendo lo mismo.

**Cómo probarlo**

1. Login con `docente@aulasegura.com` → *Mis grupos* muestra 3° B, 3° A, 2° C con los conteos de la semilla.
2. Abrir 3° B → 28 alumnos, en orden de número de lista, con los nombres sembrados.
3. Activar modo avión, cerrar y abrir la app → los grupos y el roster siguen ahí.

> **Trampa que ya nos costó una tarde en la Fase 0:** Firestore **no filtra por reglas**. Toda
> consulta de lista tiene que llevar exactamente el filtro que la regla exige. Para alumnos:
> `whereEqualTo("groupId", …)` **y** `whereEqualTo("active", true)` (el índice existe). Sin eso,
> `PERMISSION_DENIED` aunque el documento sea legible.
>
> **Y ojo con el `groupIds` denormalizado:** el docente tiene `groupIds = [grp-3b, grp-3a, grp-2c]`.
> `grp-1a` existe en la semilla justamente para probar que **no** puede leerlo.

**Evidencia (CPH2669, Android 14, `assembleDebug` + `testDebugUnitTest` verdes)**

| Check | Resultado observado |
|---|---|
| *Mis grupos* online | 3° B **28** · 3° A **30** · 2° C **27**, materia "Matemáticas" leída del perfil real |
| Orden | por grado y sección: 2° C → 3° A → 3° B |
| Nada inventado | sin anillo de asistencia, sin píldora de "lista hecha", sin horario: esos datos no existen todavía y la tarjeta los oculta |
| Modo avión + arranque en frío | abre en **Inicio** con el perfil cacheado (antes volvía al login) |
| *Mis grupos* en modo avión | los mismos 3 grupos y conteos, más el aviso "Sin conexión. Estás viendo los grupos guardados." con botón **Reintentar** |
| Roster cacheado | intacto después de refrescos sin red |
| Tests | 78 unitarios, 0 fallos (Fase 0 cerró con 61) |

---

## Bloque 2 - Pase de lista online (PLAN 1.2)

> **Estado: ✅ HECHO** — 14 nov 2026 · commit `49b2d76` · 93 tests unitarios · reglas desplegadas
> en `aulasegu` (`firebase deploy --only firestore:rules`).

- [x] **2.1** `AttendanceRepository` + `StartRollCallUseCase` / `SaveRollCallUseCase`
- [x] **2.2** `sessionId` = **UUID generado en el cliente** (nunca `add()`, nunca un id del servidor)
- [x] **2.3** Escritura de la sesión: `groupId`, `schoolId`, `teacherId`, `subject`, `date`, `status`, `counts`, `location`
- [x] **2.4** Registros en `attendanceSessions/{id}/records/{studentId}` — **el id del documento es el del alumno**
- [x] **2.5** Un solo `WriteBatch` para sesión + registros
- [x] **2.6** `counts` se **recalcula desde los registros**, nunca desde los contadores de la UI
- [x] **2.7** Cerrar sesión: `closedAt` + `status = "closed"`
- [x] **2.8** `RollCallScreen`: borrar `sampleStudents()`, cablear al ViewModel
- [x] **2.9** Eventos `roll_call_started` y `roll_call_saved` (`synced = 1`)

**Criterio:** el docente marca los 28, cierra, y en la consola hay **una** sesión con 28 registros
cuyos contadores coinciden con lo marcado.

**Evidencia (CPH2669 `65d5dd78`, Android 14, proyecto `aulasegu`)**

| Check | Resultado observado |
|---|---|
| Sesión creada | `5e09a542-13f1-45ef-b324-08c51e55062a` · `date 2026-11-14` · `status closed` · `subject "Matemáticas"` · `location null` (Bloque 3) |
| Registros | **28**, uno por alumno, con `studentId` como id del documento |
| `counts` vs registros | `{present:24, absent:1, late:2, justified:1}` — idéntico al desglose real de los 28 registros |
| `time` | `"19:27"` solo en presente/retardo; `null` en falta y justificado |
| Doble tap en *Guardar* | **una** sola sesión, no dos |
| Sync | `SyncWorker` → `Worker result SUCCESS`, sin `PERMISSION_DENIED` (29 escrituras en un batch con 28 `getAfter`) |
| Sesión fantasma | salir de la pantalla sin guardar deja la colección igual: **no** se crea nada |
| Contadores en pantalla | 5 chips (Pres./Falt./Ret./Just./Sin m.) que suman 28; antes sumaban 27 con un justificado |
| Tests | **93 unitarios, 0 fallos** (Bloque 1 cerró con 78) |

**Cómo probarlo**

1. *Pase de lista* → marcar presente / falta / retardo / justificado en varios alumnos.
2. Cerrar la sesión.
3. En la consola: `attendanceSessions/{id}.counts` suma exactamente lo marcado y
   `records` tiene un documento por alumno.
4. Repetir el cierre (doble tap) → **no** aparece una segunda sesión.

> **Por qué el id lo genera el cliente:** es lo que hace idempotente al reintento. Un `add()` en
> Firestore crea un documento nuevo cada vez, y el primer reintento de la outbox duplicaría el
> pase de lista entero.
>
> **Las reglas exigen `teacherId == uid` y que el grupo esté en su `groupIds`.** Si la escritura
> falla, el error tiene que llegar a la UI con el `groupId` en el mensaje; tragárselo convierte un
> bug de permisos en "no guardó, no sé por qué".
>
> **Las reglas cambiaron y esto es parte del bloque.** La regla `create` exigía `status == 'open'`,
> pero la sesión se crea ya cerrada (un solo `set` idempotente): **toda** escritura real habría sido
> rechazada. Ahora acepta `status in ['open', 'closed']`. Además la regla de `records` usaba
> `get(parent)`, que devuelve `null` cuando el padre se crea en el **mismo** batch; se cambió a
> `getAfter(parent)`. El límite de 20 lecturas por batch se sostiene porque Firestore cachea las
> lecturas al mismo documento: los 28 `getAfter` a la misma sesión cuentan una vez.
>
> **La completitud es un bloqueo de guardado, no una marca inventada.** El esquema solo admite
> `present | absent | late | justified`; no hay "sin marcar". `SaveRollCallUseCase` devuelve
> `Incomplete(n)` y la pantalla dice "Faltan N alumnos por marcar." en vez de escribir faltas que
> nadie eligió.
>
> **La fila de contadores tiene cinco chips, no cuatro.** Con cuatro, un grupo de 28 con un
> justificado sumaba 27 en pantalla. Los chips ahora son Pres. / Falt. / Ret. / Just. / Sin m.
>
> **Bypass temporal del GPS:** *Pasar lista* va directo a la pantalla de lista, sin pasar por
> `TeacherLocationVerifyingScreen`. El Bloque 3 es quien la vuelve a insertar con la geocerca real
> (resuelto: hoy la ruta pasa por el gate).

---

## Bloque 3 — GPS y geocerca (PLAN 1.5)

> **Estado: ✅ HECHO** — 14 nov 2026 · commit `43fcf21` · 111 tests unitarios · verificado en dispositivo (CPH2669, Android 14).

- [x] **3.1** Agregar `play-services-location` al módulo (ya está en `libs.versions.toml`)
- [x] **3.2** Permiso `ACCESS_FINE_LOCATION` con racional y caso "permiso denegado" (no bloquea el pase de lista, lo deja **no verificado**)
- [x] **3.3** `LocationProvider` con `FusedLocationProviderClient`: última ubicación conocida y después fix nuevo con timeout
- [x] **3.4** Distancia con **haversine** contra `schools.location`, radio desde `schools.radiusMeters` (fallback validado: `default_radius_meters`)
- [x] **3.5** Umbral de precisión: un fix con `accuracy` mayor que el radio **no** puede declarar `verified = true`
- [x] **3.6** `TeacherLocationVerifyingScreen`, `TeacherOutsideRadiusScreen` y `TeacherRollCallMapScreen` con coordenadas reales
- [x] **3.7** Persistir `location { lat, lng, accuracy, distanceMeters, verified }` en la sesión

**Dónde quedó cada cosa**

| Pieza | Archivo |
|---|---|
| Regla pura (`verified`, `withinRadius`, haversine) | `location/Geofence.kt` |
| Lectura de GPS (fix nuevo 8 s → última conocida GPS → red) | `location/LocationProvider.kt` |
| Plantel cacheado, con tope de espera para no bloquear sin red | `location/SchoolSiteState.kt` |
| Caché de `schools` (Room v3) y su refresh | `data/local/entity/SchoolEntity.kt`, `data/repository/FirestoreSchoolRepository.kt` |
| Congelado del veredicto en la sesión | `LocationFix.toSessionLocation`, `RollCallViewModel.setLocation` |

**Criterio:** dentro del radio la sesión se guarda con `verified = true`; fuera del radio la app
muestra la pantalla de "fuera de la escuela" y **no** deja cerrar una sesión verificada.

**Cómo probarlo**

1. En el plantel (o con una ubicación simulada dentro de 150 m) → *Verificando ubicación* → pasa al pase de lista.
2. Simular ubicación a ~2 km → pantalla de fuera de radio, sin opción de guardar como verificada.
3. Revisar en Firestore `location.distanceMeters` y `location.verified` de la sesión guardada.

> **Precisión no es distancia.** Un fix a 2 km con 500 m de error no está "casi adentro": la
> distancia se calcula al centro del plantel y la precisión se guarda aparte. El campo `verified`
> tiene que ser honesto, porque de él va a depender la asistencia del tutor en la Fase 3.
>
> **El GPS funciona sin datos.** No lo metas en la outbox: la ubicación se toma al abrir la
> sesión, no cuando hay red.

### Evidencia observada (Bloque 3)

| Caso | Resultado en el dispositivo |
|---|---|
| Fuera del radio (posición real, ~6.3 km del plantel) | Pantalla "Fuera del radio permitido · Estás a **6311 m** de la escuela … dentro de **150 m**", sin CTA de guardar |
| Dentro del radio | Banner "Estás dentro de la escuela · **431 m** del centro · radio **3000 m** · ±13 m"; sesión `status: closed`, `counts.present: 28`, `location { verified: true, distanceMeters: 431.16, accuracy: 13.47 }` |
| Permiso denegado | El gate muestra "Permiso de ubicación pendiente" y el CTA **Continuar sin verificar**; la lista se abre con "Ubicación sin verificar · Sin lectura de GPS" y la sesión se guarda **sin** el campo `location` |
| Migración Room 2 → 3 | La app arranca sobre la base existente sin crash: `schools` se crea vacía y el primer refresh la llena |
| Sin red (modo avión) | La app avisa "Sin conexión. Estás viendo los grupos guardados." y el gate sigue evaluando la geocerca **contra la caché de Room**: muestra el centro y el radio del último valor cacheado —en esa corrida, los temporales del caso "dentro", radio 3000 m—, no "Escuela sin ubicación guardada" |
| Tests unitarios | 111 tests, 0 fallos (+11 `GeofenceTest`, +7 `SchoolMappersTest`) |

> **Offline manda la caché, y la caché manda lo último que se supo.** Con el avión activado el radio
> y el centro son los del último refresh, no los del documento: un cambio de radio en la consola no
> llega al dispositivo hasta que vuelve la red. Para la escuela eso es lo correcto — el aula sin
> señal tiene que poder pasar lista — pero implica que el mensaje del banner puede ir un refresh
> atrás respecto de la consola.

> **Cómo se probó "dentro" en un teléfono real.** Este dispositivo no permite
> `cmd location providers add-test-provider` (`SecurityException: android from uid 2000 not allowed
> to perform MOCK_LOCATION`) y `pm grant` / `pm revoke` también están bloqueados por el OEM. El caso
> "dentro" se ejerció moviendo **temporalmente** el documento del plantel a 431 m del teléfono con
> radio 3000 m, y después se restauró a `19.2926, -99.6557 / 150 m`. El caso "permiso denegado" se
> revocó y se restauró desde Ajustes → Permisos de la app, no por `adb`.

---

## Bloque 4 — Pase de lista offline (PLAN 1.4)

> **Estado: ✅ HECHO en código** — 15 nov 2026 · `assembleDebug` verde · **116 tests unitarios, 0 fallos**
> (el bloque 3 cerró con 111) · **24 tests de instrumentación, 0 fallos** en CPH2669 (Android 14).
> **4.6 verificado en el teléfono** (modo avión → guardar → reconectar): la sesión queda
> `PENDING` con su fila en `outbox`, al reconectar pasa a `SYNCED`, la outbox queda vacía y en
> Firestore hay **una** sola sesión con sus registros. El estado de sync además quedó cubierto en
> hardware por `AttendanceSyncStateTest`. **Bloque cerrado.**

- [x] **4.1** Reusar `OutboxRepository` (ya encola sesión + registros + outbox en una transacción)
- [x] **4.2** Con red caída: escribir en Room con `syncState = PENDING` y programar `SyncScheduler`
- [x] **4.3** La UI refleja el estado real (`PENDING` / `SYNCING` / `SYNCED` / `FAILED`), no un texto fijo
- [x] **4.4** El handler sube la **misma** sesión y no crea otra
- [x] **4.5** Una fila envenenada no bloquea la cola (ya cubierto en Fase 0: `MAX_ATTEMPTS_BEFORE_DROP`)
- [x] **4.6** Prueba de ida y vuelta completa: avión → guardar → reconectar → outbox vacía

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/RollCallSync.kt` | nuevo enum de dominio (`PENDING`, `SYNCING`, `SYNCED`, `FAILED`) con `delivered`. La UI no importa `SyncState` de Room |
| `data/local/dao/AttendanceDao.kt` | `observeSession(sessionId): Flow<AttendanceSessionEntity?>` — la sesión que la pantalla vigila |
| `domain/repository/AttendanceRepository.kt` | `observeSync(sessionId): Flow<RollCallSync>` |
| `data/repository/RoomAttendanceRepository.kt` | mapea `SyncState → RollCallSync` en un solo lugar |
| `sync/AttendanceSyncHandler.kt` | escribe `SYNCING` **antes** del batch. Antes nadie escribía ese estado: existía en el enum pero jamás llegaba a la base |
| `ui/screens/teacher/RollCallUi.kt` | `sync` en el `UiState` y `rollCallSyncMessage()` |
| `ui/screens/teacher/RollCallViewModel.kt` | al guardar arranca `watchSync(sessionId)`; se fue el `"Se sincroniza en segundo plano"` fijo |
| `ui/screens/teacher/RollCallScreen.kt` | el strip queda verde solo con el guardado real, y ámbar si `FAILED` (sesión a salvo en el teléfono, no en la nube) |
| `androidTest/.../AttendanceSyncStateTest.kt` | 2 tests de device: las cuatro fases se leen de la fila real, y una sesión inexistente no emite nada |

**Por qué el estado vive en Room y no en el ViewModel:** el worker es otro proceso lógico que puede
correr con la pantalla cerrada. Un `MutableStateFlow` en memoria diría `PENDING` para siempre; leyendo
la fila, el maestro que vuelve a abrir la app ve `SYNCED` aunque no haya visto pasar la sincronización.

**Nota sobre 4.3 y `TeacherRollCallOfflineScreen`:** el estado real se implementó en
`RollCallScreen`, que es la pantalla que **escribe** (Room + outbox a través de `RollCallViewModel`).
`TeacherRollCallOfflineScreen` es el mock esquemático del bloque 3.6 (radar offline): tiene su propio
`demoRoster()` y su CTA solo vuelve atrás, no guarda nada. No se le cableó un guardado porque marcaría
faltas que nadie eligió. Si se quiere, el paso siguiente es retirarlo del nav o convertirlo en una
variante de solo lectura.

**Criterio:** con modo avión el pase de lista se guarda igual; al reconectar hay **exactamente una**
sesión en Firestore, con 28 registros, y la outbox queda vacía.

**Cómo probarlo**

1. Modo avión → hacer el pase de lista completo → cerrar la sesión → la UI dice "pendiente de sincronizar".
2. Revisar en Room que la sesión está `PENDING` y hay una fila en `outbox`.
3. Quitar modo avión → esperar el worker (o forzarlo) → `SYNCED` y outbox vacía.
4. En la consola: contar sesiones de ese día → **una**.
5. Repetir el paso 3 dos veces (forzar el worker de nuevo) → sigue habiendo una.

> **Evidencia de la corrida del 16 nov** (CPH2669, Android 14): sesión `885916bf-…` guardada con el
> modo avión activo — `syncState = PENDING`, 28 registros y una fila `UPSERT_ATTENDANCE_SESSION` en
> `outbox`; al reconectar quedó `SYNCED`, la outbox se drenó y Firestore tiene **una** sesión con
> `counts.present = 28` y sus 28 registros. En la misma corrida, una fila `UPSERT_INCIDENT`
> envenenada delante en la cola **no** bloqueó la asistencia: 4.5 quedó probado en el aparato, no
> solo en los tests.

> **La unidad de idempotencia es el UUID.** Si alguna vez aparece un `add()` en el camino de
> asistencia, el modo avión empieza a duplicar sesiones y el tutor va a ver dos asistencias del
> mismo día.

---

## Bloque 5 — Historial (PLAN 1.9)

> **Estado: ✅ HECHO** — 15 nov 2026 · `assembleDebug` verde · **130 tests unitarios, 0 fallos**
> (el bloque 4 cerró con 116; +7 de agrupación y mapeo, +7 del documento de Firestore).
> Las dos cosas que faltaban para la corrida en teléfono **se ejecutaron el 16 nov** en el CPH2669:
> el historial abrió con datos reales (HOY / ESTA SEMANA y el detalle por alumno al expandir una
> fila) y `AttendanceHistoryDaoTest` pasó (`OK (4 tests)`, en la misma corrida que
> `AttendanceSyncStateTest`). Con el modo avión activo la lista sobrevive desde Room y la pantalla
> avisa "Sin conexión. Estás viendo el historial guardado".

- [x] **5.1** Consulta `attendanceSessions` con `teacherId == uid` + `date` desc (índice ya publicado)
- [x] **5.2** Lectura offline desde Room (las sesiones guardadas se leen sin red)
- [x] **5.3** `TeacherHistoryScreen` con sesiones reales agrupadas por fecha
- [x] **5.4** Detalle: abrir una sesión y ver sus registros por alumno

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/AttendanceHistory.kt` | `AttendanceSessionSummary`, `AttendanceSessionRecord`, `AttendanceSessionDetail` |
| `domain/repository/AttendanceHistoryRepository.kt` | contrato de solo lectura: historial cache-first + detalle + refresco de registros |
| `data/local/dao/AttendanceDao.kt` | `observeSessionsByTeacher` (date desc, startedAt como desempate) y `upsertSessions` |
| `data/mapper/AttendanceDocumentMapper.kt` | documento Firestore → entidad de Room, y `SyncState → RollCallSync` compartido con el bloque 4 |
| `data/repository/FirestoreAttendanceHistoryRepository.kt` | refresh con `Source.SERVER` (12 s) + detalle combinando sesión, registros, grupos y alumnos de la caché |
| `ui/screens/teacher/TeacherHistoryUi.kt` | modelos de UI + `historySections()` puro (HOY / ESTA SEMANA / SEMANA ANTERIOR / ANTERIORES) |
| `ui/screens/teacher/TeacherHistoryViewModel.kt` | lista cache-first, refresco, y expansión de una fila a la vez |
| `ui/screens/teacher/TeacherHistoryScreen.kt` | filas reales: fecha, conteos, píldora de sync, verificado GPS y registros al expandir |

**Decisiones que importan**

- **El detalle se expande en la misma pantalla.** No hay mockup de "detalle de sesión" y abrir otra pantalla habría inventado un diseño; además el maestro está leyendo una lista y un push/pop le perdería el scroll.
- **Los registros se piden solo al expandir.** Traer 28 registros por cada fila del historial convertiría abrir la pantalla en cientos de lecturas. Si la sesión se tomó en este teléfono, la caché ya los tiene y aparecen al instante; el refresh va detrás y no bloquea.
- **`Source.SERVER` otra vez.** Un `get()` sin red puede volver como snapshot vacío; un historial que se resuelve a "no hay sesiones" haría creer al maestro que perdió la asistencia. Si falla, la caché queda intacta y la pantalla avisa con **Reintentar**.
- **Solo la fila abierta recibe los registros.** Lo detectó el primer test que escribí: `historySections` se los pasaba a todas y cada tarjeta pintaba el mismo roster.
- **Nada se inventa.** Una fecha ilegible cae en `ANTERIORES` (mejor un bucket equivocado que una asistencia escondida) y un `status` ilegible queda vacío, que la UI muestra como "Sin registrar" en vez de suponer que el alumno vino.
- **Incidentes y Publicaciones** de la barra segmentada dicen que su historial llega con su bloque; no muestran una lista vacía que parezca un bug.

**Criterio:** después de N pases de lista, el historial los lista en orden y abre el detalle de
cualquiera, incluso sin red.

---

## Bloque 6 — Incidentes (PLAN 1.6)

> **Estado: ✅ HECHO** — 15 nov 2026 · `assembleDebug` verde · **144 tests unitarios, 0 fallos**
> (el bloque 5 cerró con 130; +14 de este bloque: caso de uso, documento de Firestore, UI y
> el selector de categoría).
> **Verificado en teléfono** (CPH2669): `IncidentOfflineSyncTest` crea el incidente con el modo avión
> activo, lo apaga, deja que el runner vacíe el outbox y lee el documento con `Source.SERVER`: existe
> **una sola vez** con severidad, categoría, nombre de alumno y `location`, y un segundo replay no lo
> duplica. Ojo: ese incidente de prueba queda en Firestore, porque las reglas no permiten borrar
> incidentes desde el cliente (`allow delete: if false`).
> **Verificado además desde la pantalla** (16 nov): el reporte se llenó a mano con el modo avión
> activo — el borrador quedó `PENDING` en Room con su fila `UPSERT_INCIDENT`; al reconectar pasó a
> `SYNCED`, la outbox se drenó y en Firestore quedó un documento con `studentName`, `severity`,
> `category` y `location`. Los chips se probaron tocándolos: un segundo incidente llegó con
> `severity = grave` y `category = caida`, no con los valores por defecto.

- [x] **6.1** `IncidentRepository` + `CreateIncidentUseCase`
- [x] **6.2** Borrador en Room (`IncidentDraftEntity`) con `syncState` y `opType = UPSERT_INCIDENT`
- [x] **6.3** Handler `IncidentSyncHandler` y registrarlo en `syncHandlers` del contenedor
- [x] **6.4** Severidad (`leve` / `moderado` / `grave`) y categoría según `CONTEXT.md` §6.2
- [x] **6.5** Denormalizar `studentName` y `reportedByName`: el tutor lee el incidente sin poder leer el padrón
- [x] **6.6** `TeacherIncidentNewScreen` cableada; evento `incident_created` (`severity`, `media_count`)

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/SyncPhase.kt` | enum genérico de fase (`PENDING`/`SYNCING`/`SYNCED`/`FAILED` + `delivered`); `RollCallSync` pasó a ser `typealias` |
| `domain/model/Incident.kt` | `IncidentSeverity` e `IncidentCategory` (con `wire` para Firestore y `label` para la UI), `IncidentLocation`, `NewIncident`, `IncidentSaveOutcome` |
| `domain/repository/IncidentRepository.kt` | contrato de escritura: `create` + `observeSync` |
| `domain/usecase/CreateIncidentUseCase.kt` | valida alumno y descripción, encola, y emite `incident_created` solo si quedó en la cola |
| `data/mapper/IncidentSyncMapper.kt` | entidad → documento de `incidents/{incidentId}` con nombres denormalizados y `location` anidada |
| `data/repository/RoomIncidentRepository.kt` | draft + fila de outbox transaccional, `schoolId` resuelto del grupo cacheado, UUID de cliente |
| `sync/IncidentSyncHandler.kt` | `SYNCING` → `set()` idempotente por UUID → `SYNCED`/`FAILED` |
| `sync/FirestoreRetry.kt` | `FirebaseFirestoreException.isRetryable`, compartido con el handler de asistencia |
| `sync/OutboxRepository.kt` | `enqueueIncident`: draft `PENDING` + `UPSERT_INCIDENT` en una transacción, y `schedule()` |
| `data/local/dao/IncidentDao.kt` | `observeDraft(incidentId)` para que la pantalla siga la fase real y `updateSyncState` para el handler |
| `ui/screens/teacher/TeacherIncidentUi.kt` | modelos de UI, `canSave` y `incidentSyncMessage()` |
| `ui/screens/teacher/TeacherIncidentViewModel.kt` | roster real de sus grupos, severidad, descripción, guardado y seguimiento del sync |
| `ui/screens/teacher/TeacherIncidentNewScreen.kt` | selector de alumno real, severidad y categoría del dominio, aviso de guardado y CTA con estado |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | repositorio, caso de uso, handler registrado y ruta `TEACHER_INCIDENT_NEW` cableada |
| `androidTest/.../sync/IncidentOfflineSyncTest.kt` | prueba de device del criterio: modo avión real, outbox, documento leído con `Source.SERVER` y replay idempotente |

**Decisiones que importan**

- **El id lo genera el teléfono.** Es el `entityLocalId` del outbox *y* el id del documento, así que reintentar sobrescribe el mismo incidente en vez de crear un segundo. Es el criterio del bloque, escrito una sola vez.
- **Un `set()`, no un batch.** Un incidente es un documento; su evidencia (bloque 7) se sube antes de esta escritura, así que aquí no hay dos colecciones que puedan quedar a medias.
- **`schoolId` no lo manda la pantalla.** Se resuelve del grupo ya cacheado, que es lo que las reglas comparan contra `mySchool()` y `myGroups()`.
- **Nada se auto-selecciona.** Un incidente con el alumno equivocado es una llamada a la familia: el botón queda deshabilitado hasta que el docente nombra al alumno, y la descripción es obligatoria por la misma razón.
- **La ubicación guardada es el pin, no el GPS.** El pin es donde el docente dice que pasó; `accuracy` queda `null` a propósito porque un pin no es una lectura del sensor. `verified` responde "¿el pin cae dentro del radio de la escuela?", no "¿el GPS fue confiable?".
- **`media` no se escribe todavía.** Un array vacío diría "este incidente no tiene evidencia" justo cuando el docente sí adjuntó una foto; el array completo llega con el bloque 7, después de subir.
- **La categoría se elige con chips y `Otro` es el default.** "No sé en qué cajón entra" es una respuesta real y el incidente se tiene que poder reportar igual; las otras cuatro son las que filtran el admin.
- **`media_count` va en 0** por lo mismo: el evento cuenta lo que quedó en el documento, no lo que se ve en pantalla.

**Pendientes conocidos de este bloque**

1. **El título se deriva de la descripción** (primera línea, 80 caracteres): la pantalla no tiene campo de título y el documento lo necesita para la lista del admin.
2. **La evidencia de la pantalla es solo visual.** Se pueden tomar foto y video, pero no se persisten ni se cuentan hasta el bloque 7.
3. **Sin edición ni estado.** Un incidente creado queda en `nuevo`; cambiarlo a `revisado`/`cerrado` es del admin (reglas y bloque de admin), no de esta pantalla.

**Corregidos tras la verificación en el teléfono**

1. **`Requerido` ya no miente y la gravedad es obligatoria.** `TeacherIncidentUi.severity` arranca en `null`: la etiqueta `Requerido` se pinta solo mientras siga así, `canSave` exige severidad y `save()` corta sin ella con "Elige la gravedad del incidente.". Antes el default era `MODERADO`, es decir que el silencio del docente se guardaba como una decisión de triage que nadie tomó. Verificado en el teléfono: al abrir, `Requerido` visible y CTA deshabilitado; al tocar Grave la etiqueta desaparece y el CTA se habilita con alumno y descripción.
2. **La tarjeta de fecha y hora muestra la hora real.** `TeacherIncidentUi.occurredAt` se captura una sola vez al abrir el formulario y es el mismo valor que viaja en `NewIncident.occurredAt`, así que la tarjeta no puede mostrar algo distinto de lo que se guarda. `incidentTimestampLabel()` lo formatea con `SimpleDateFormat` y `es-MX` (nada de `java.time`: `minSdk` 24 sin desugaring). En el teléfono se leyó `Dom 15 nov 2026 · 19:08`.
3. **`IncidentOfflineSyncTest` ya no deja basura en el outbox.** El `@After` borra la fila por `entityLocalId` además del borrador. La fila testigo del defecto quedó en el aparato con 9 intentos y el error `is no longer stored locally`; se descarta sola en el intento 10 y nunca bloqueó la cola. Un run en verde no ejercita ese camino —la cola se drena—, así que la evidencia del arreglo es el código y el síntoma medido, no una corrida del test.

**Criterio:** un incidente creado en modo avión aparece **una vez** en Firestore al reconectar, con
su severidad, su nombre de alumno y su ubicación.

> **`studentName` no es redundancia, es contrato.** Las reglas dejan al tutor leer sus incidentes
> pero no el padrón completo; si el nombre no viaja denormalizado, la pantalla del tutor tiene que
> hacer una lectura que le van a denegar.

---

## Bloque 7 — Media del incidente (PLAN 1.7)

- [ ] **7.1** Adjuntar hasta `incident_media_max` archivos (Remote Config ya validado, default 3)
- [ ] **7.2** Imagen: `ImageCompressor` existente antes de subir
- [ ] **7.3** Subida con el uploader de Cloudinary existente, carpeta `aulasegura/incidents/{schoolId}`
- [ ] **7.4** Progreso **por archivo** (`uploadState`: `PENDING` → `UPLOADING` → `UPLOADED` / `FAILED`)
- [ ] **7.5** Reintento individual: un archivo que falla no pierde el incidente ni los otros adjuntos
- [ ] **7.6** El documento del incidente se escribe **después** de subir, con el array `media` completo

**Criterio:** un incidente con foto hecho en modo avión termina en Firestore con la URL de
Cloudinary, sin perder la foto ni duplicar el incidente.

> **Decisión tomada: video → Fase 4.** Fase 1 entrega **solo imágenes**. `ImageCompressor` procesa
> imágenes y no hay transcodificación de video en el dispositivo; subir video sin re-encode obliga a
> un tope de peso y a un costo que no corresponde pagar en esta fase.
>
> Límites del plan Free de Cloudinary (verificados, para cuando llegue la Fase 4):
> - **25 créditos/mes**, sin tarjeta y sin límite de tiempo.
> - **100 MB máximo por archivo de video.**
> - 1 crédito = 1 GB almacenado · 1 GB de ancho de banda · 500 s de procesamiento SD · 250 s en HD.
> - Incluye conversión automática de formato, ajuste de calidad y entrega por CDN.
>
> **Consecuencia práctica:** un video de 100 MB consume ~1 crédito de los 25 solo por existir, y el
> bandwidth de un aula mirándolo se come el resto. En Fase 4 el video va con compresión previa y
> límite de duración, no "subir lo que el teléfono grabe".
>
> **Orden que no se puede invertir:** subir primero y escribir después. Al revés, el incidente queda
> en Firestore con `media` vacío y el tutor ve un incidente sin evidencia.

---

## Bloque 8 — Tareas y avisos (PLAN 1.8)

- [ ] **8.1** `TaskRepository.publish`: `groupIds` (array), `dueDate`, `createdBy`, `active`
- [ ] **8.2** `NoticeRepository.publish`: `audience.roles`, `audience.groupIds`, `urgent`
- [ ] **8.3** Caché de lectura en Room (`TaskEntity`, `NoticeEntity`) desde el primer momento
- [ ] **8.4** `TeacherTaskNewScreen` y `TeacherNoticeNewScreen` cableadas a grupos reales
- [ ] **8.5** Eventos `task_published` y `notice_published`

**Criterio:** una tarea publicada para 3° B y un aviso urgente para los tutores de 3° B aparecen en
Firestore con la audiencia correcta y se leen sin red después de la primera carga.

---

## Bloque 9 — Chat (PLAN 1.10)

- [ ] **9.1** Agregar `firebase-database` al módulo (ya está en el catálogo)
- [ ] **9.2** `docs/rtdb.rules` con el contrato de conversaciones + `database` en `firebase.json` + deploy
- [ ] **9.3** Esquema RTDB: `messages/{conversationId}/{messageId}` (`senderId`, `text`, `sentAt`) y presencia
- [ ] **9.4** `conversations/{id}` en Firestore como **metadatos**: `participants`, `lastMessage`, `lastMessageAt`, `unread`
- [ ] **9.5** `ChatRepository` con `Flow` de mensajes + envío optimista
- [ ] **9.6** `TeacherChatScreen` y `TeacherConversationScreen` reales; evento `chat_message_sent`

**Criterio:** dos cuentas intercambian mensajes en tiempo real y la lista de chats muestra el último
mensaje y la hora, sin recargar la pantalla.

> **Hoy no hay reglas de RTDB.** Sin `rtdb.rules`, la base está en el modo por defecto del proyecto y
> cualquiera con la config puede leer todo. Este bloque no está terminado hasta que las reglas estén
> publicadas, no solo escritas en `docs/`.
>
> **Dos almacenes, un solo mensaje:** el mensaje vive en RTDB; Firestore solo guarda el resumen para
> pintar la lista. Si se escriben los dos y uno falla, la lista miente. El resumen se actualiza
> **después** de que RTDB acepta el mensaje.

---

## Bloque 10 — Notificaciones y FCM (PLAN 1.11)

- [ ] **10.1** `FirebaseMessagingService`: token en `users/{uid}/fcmTokens/{tokenId}` (`token`, `platform`, `updatedAt`)
- [ ] **10.2** Rotación de token (`onNewToken`) y borrado del token al cerrar sesión
- [ ] **10.3** Canal de notificación + permiso `POST_NOTIFICATIONS` en Android 13+
- [ ] **10.4** Apertura por `deepLink` (el campo `notifications.deepLink` ya está en el esquema)
- [ ] **10.5** `TeacherNotificationsScreen` con la colección `notifications` real (`userId`, `read`)
- [ ] **10.6** Escritura de `fcmTokens` validada contra las reglas

**Criterio:** desde la consola de Firebase se envía una notificación al token del dispositivo, llega,
y al tocarla abre la pantalla correcta. Marcar como leída la quita del contador.

**Cómo probarlo**

1. Copiar el token con `adb logcat` (o desde la pantalla de perfil en debug).
2. Firebase Console → Messaging → *Send test message* → pegar el token → enviar.
3. Tocar la notificación → abre la pantalla del `deepLink`.
4. Login/Logout → el token desaparece de `users/{uid}/fcmTokens` al salir.

> **Esto es Spark: no hay envío automático.** En plan gratuito no hay Cloud Functions, así que un
> push "cuando pasa X" **no existe** todavía. En esta fase el envío es manual desde la consola y el
> documento en `notifications` se escribe por script o consola (las reglas lo reservan al Admin SDK).
> No diseñes un flujo que dependa de push automático: no va a sonar.

---

## Bloque 11 — Perfil (PLAN 1.12)

- [ ] **11.1** `TeacherProfileScreen` con datos reales (`fullName`, `email`, `schoolId`, `subjects`)
- [ ] **11.2** Saludo del `TeacherHomeScreen` desde `fullName` (cierra el pendiente de la Fase 0)
- [ ] **11.3** Foto: comprimir + subir a `aulasegura/profiles/{uid}` + escribir `photoUrl` y `photoPublicId`
- [ ] **11.4** Foto visible en la UI y persistida entre sesiones (Coil)
- [ ] **11.5** Logout y cierre forzado por `active = false` siguen funcionando

**Criterio:** el docente cambia su foto, cierra la app, vuelve a entrar y la foto sigue ahí; el
saludo muestra su nombre real.

---

## Cierre de la Fase 1

- [ ] **C1** Modo avión: pase de lista completo **y** incidente con foto
- [ ] **C2** Reconectar: ambos en Firestore, **sin duplicados**, media subida
- [ ] **C3** `assembleDebug` verde
- [ ] **C4** `testDebugUnitTest` verde (sin bajar el número de la Fase 0: eran 61)
- [ ] **C5** `connectedDebugAndroidTest` verde en device físico
- [ ] **C6** Reglas de Firestore y de RTDB publicadas
- [ ] **C7** `FASE1.md` completo y `CONTEXT.md` actualizado con lo que cambió de esquema

### Evidencia mínima esperada

| Check | Cómo se demuestra |
|---|---|
| Offline real | Logcat del `SyncWorker` + outbox vacía + consola con **una** sesión y **un** incidente |
| Sin duplicados | Repetir el drenaje dos veces y volver a contar documentos |
| Media | `incident.media[0].url` abre en el navegador |
| Geocerca | `location.verified` / `distanceMeters` de una sesión adentro y otra afuera |
| Chat | Dos cuentas, mensaje en ambas pantallas sin recargar |
| Push | Notificación desde consola + apertura por `deepLink` |

---

## Riesgos anotados

1. **El esquema es contrato compartido.** Cada colección nueva o campo nuevo se avisa antes de
   mergear, porque Evelyn escribe las reglas y la Fase 2/3 leen los mismos documentos.
2. **`data/` lo toca una sola persona a la vez.** El pase de lista y los incidentes comparten
   `OutboxRepository` y `SyncRunner`: dos ramas tocándolos en paralelo es un conflicto garantizado.
3. **La outbox no puede crecer sin control.** Todo lo que se guarda offline tiene que tener handler
   registrado; una fila sin handler se reporta y espera, no se finge que sincronizó.
4. **`main` siempre compila.** Un rojo en `main` bloquea a la otra persona, y este proyecto ya tiene
   dos personas.
