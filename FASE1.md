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
| **7** — Media del incidente | foto comprimida a Cloudinary | ✅ **hecho** · `assembleDebug` + 154 tests |
| **7B** — Mis incidentes | historial propio cache-first | ✅ **hecho** · `assembleDebug` + 167 tests |
| **8** — Tareas y avisos | publicar con audiencia | ✅ **hecho** · `c87e1eb` · `assembleDebug` + 179 tests |
| **8B** — PDF en tareas | un solo PDF (10 MB) subido antes de publicar | ✅ **hecho** · `4ddc7b7` + `e837e16` · `assembleDebug` + 189 tests |
| **9** — Chat | conversaciones y mensajes en RTDB | ✅ **hecho** · `396830d` · `assembleDebug` + 205 tests |
| **10** — Notificaciones y FCM | push desde la consola + bandeja real + script de avisos | ✅ **cerrado 17 nov 2026** · probado en teléfono · 219 tests |
| **11** — Perfil | datos reales del docente + foto en Cloudinary + ajustes de cuenta | ✅ **hecho** · `assembleDebug` + 234 tests |

> Los conteos de cada fila son los de su entrega. Con la Fase 2 integrada en `main`, la
> verificación final es **379 tests, 0 fallos** (52 suites) y `assembleDebug` en verde.

**Criterio de cierre de la fase** (`PLAN.md`): con **modo avión** activo, el docente pasa lista y
registra un incidente con foto; al reconectar, ambos aparecen en Firestore **sin duplicados** y
con la media subida.

---

## Puesta en marcha en otra máquina

Para que el repo compile apenas se clona (por ejemplo, al arrancar la Fase 2):

1. Clonar `main` y abrirlo en Android Studio. `local.properties` se genera solo con `sdk.dir`.
2. **`app/google-services.json` sí está versionado.** Es la configuración pública del proyecto de
   Firebase y sin ella el build falla por el plugin `com.google.gms.google-services`.
3. **Las claves de Cloudinary no están en el repo.** Agregar a `local.properties`
   `CLOUDINARY_CLOUD_NAME` y `CLOUDINARY_UPLOAD_PRESET`; sin ellas la app compila igual y solo la
   subida de archivos queda como "no configurada".
4. **Chat y RTDB ya están configurados** en el proyecto de Firebase (base creada y reglas publicadas):
   clonar y correr alcanza para probar los mensajes.
5. Antes de mergear: `./gradlew :app:assembleDebug :app:testDebugUnitTest` en verde. `main` siempre
   compila.

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

- [x] **7.1** Adjuntar hasta `incident_media_max` archivos (Remote Config ya validado, default 3)
- [x] **7.2** Imagen: `ImageCompressor` existente antes de subir
- [x] **7.3** Subida con el uploader de Cloudinary existente, carpeta `aulasegura/incidents/{incidentId}` (ver nota)
- [x] **7.4** Progreso **por archivo** (`uploadState`: `PENDING` → `UPLOADING` → `UPLOADED` / `FAILED`)
- [x] **7.5** Reintento individual: un archivo que falla no pierde el incidente ni los otros adjuntos
- [x] **7.6** El documento del incidente se escribe **después** de subir, con el array `media` completo

> **7.3 dice `{schoolId}` y se implementó `{incidentId}`.** Manda `CONTEXT.md` §7.1: la carpeta agrupa
> la evidencia de **un incidente** para que un borrado futuro sea una sola llamada. Con `{schoolId}`
> todas las fotos de la escuela caen juntas y no hay forma de borrar las de un incidente sin listar y
> filtrar por prefijo. El ítem del checklist queda corregido aquí; el plan no se reescribe.

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/Incident.kt` | `NewIncidentMedia` (solo `localPath`), `IncidentMediaStatus`, `MediaUploadPhase` y `media` en `NewIncident` |
| `domain/repository/IncidentRepository.kt` | `observeMedia(incidentId)`: la subida termina antes que el documento, así que se sigue aparte del `syncState` |
| `data/local/media/IncidentMediaStore.kt` | **nuevo** `FileIncidentMediaStore`: comprime con `ImageCompressor`, guarda en `filesDir/incident-media/` y devuelve el thumbnail del tile |
| `data/local/dao/IncidentDao.kt` | `findMedia(mediaId)` para el handler y `observeMedia(incidentId)` para la pantalla |
| `data/mapper/IncidentSyncMapper.kt` | `toFirestoreMap(media)`: array `media` con solo los `UPLOADED`; `toMediaUploadPhase()` |
| `sync/OutboxRepository.kt` | `enqueueIncident(draft, media)`: filas `UPLOAD_INCIDENT_MEDIA` **antes** de la del incidente, todas con el mismo `createdAt` |
| `sync/UploadIncidentMediaHandler.kt` | **nuevo**: un outbox por archivo, sube los bytes ya comprimidos y borra el archivo local al quedar `UPLOADED` |
| `sync/IncidentSyncHandler.kt` | no escribe con archivos en vuelo, publica el array completo y concilia el replay denegado leyendo el documento |
| `data/repository/RoomIncidentRepository.kt` | crea las filas de media junto al draft y expone `observeMedia` |
| `ui/screens/teacher/TeacherIncidentUi.kt` | `IncidentEvidenceUi`, `canAddEvidence`, `mediaPhaseLabel()` e `incidentMediaCountLabel()` |
| `ui/screens/teacher/TeacherIncidentViewModel.kt` | captura real, límite de Remote Config, fase por archivo y limpieza de fotos huérfanas en `onCleared` |
| `ui/screens/teacher/TeacherIncidentNewScreen.kt` | `TakePicture` real con FileProvider, grid sin video y etiqueta de subida por tile |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | store, handler en el registro y callbacks de foto |
| `test/.../IncidentSyncMapperTest.kt` · `TeacherIncidentUiTest.kt` · `CreateIncidentUseCaseTest.kt` | array `media`, mapeo de fases, límite, etiquetas y espera de compresión |

**Decisiones que importan**

- **La compresión ocurre al capturar, no al subir.** El docente ve la foto aparecer en el grid
  mientras el formulario sigue abierto, y cada reintento sube los mismos bytes sin volver a pagar el
  costo de comprimir.
- **Los archivos viven en `filesDir`, no en `cacheDir`.** Un incidente puede esperar días a que haya
  señal y el sistema es libre de borrar un archivo de caché mucho antes que eso: la evidencia no puede
  depender de la política de limpieza de Android.
- **Un outbox por archivo, no uno por incidente.** Un archivo que falla detiene solo su fila. El orden
  de inserción (archivos primero, incidente después) es lo que garantiza que el documento se escriba
  con el array completo; el handler, además, se niega a escribir con algo en vuelo.
- **Fallo transitorio ≠ fallo permanente.** `NetworkError`/`ServerError` devuelven el archivo a
  `PENDING` con `Retry` — nada se pierde y la pasada se corta ahí. Archivo borrado, ilegible,
  `Rejected` o `NotConfigured` lo dejan en `FAILED` y la cola sigue: el incidente se escribe con las
  fotos que sí subieron, y el tile dice cuál no.
- **El documento del incidente es de escritura única.** Las reglas desplegadas no tienen cláusula
  `update` para docente, así que un replay se resuelve leyendo el documento y comprobando que ya está,
  en vez de agregar un permiso que nadie pidió.
- **El video sale de la pantalla.** Fase 1 entrega solo imágenes; dejar el tile de video era prometer
  algo que el worker no iba a subir.

**Pendientes conocidos de este bloque**

1. **`media_count` del evento `incident_created` cuenta archivos adjuntos**, no archivos subidos. Si
   una foto falla, el evento dice 1 y el documento queda con 0. Contar lo que quedó en el documento
   obligaría a emitir el evento desde el handler, que es otro cambio de contrato.
2. **`IncidentMediaStatus.localPath` no se limpia después de subir**: el tile se une a su fila por ese
   camino y vaciarlo rompería la unión. El comentario de la entidad que dice "null after upload" es
   inexacto; se corrija cuando se toque esa entidad.
3. **El `Retry` de `IncidentSyncHandler` por archivos en vuelo no es alcanzable en producción**: las
   filas de media siempre acompañan a la del incidente en la misma transacción. Queda como red de
   seguridad, no como camino esperado.
4. **La verificación en el teléfono la corre el dueño del proyecto** (modo avión → foto → reconectar),
   con la lista de pasos entregada en el reporte del bloque.

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

## Bloque 7B — Mis incidentes en el historial (agregado, no estaba en `PLAN.md`)

> **Por qué existe:** el bloque 7 dejó al docente creando incidentes que no podía volver a leer. El
> plan solo contempla la bandeja del admin/tutor (`PLAN.md` 2.7, Fase 2), así que en Fase 1 el
> reporte era de una sola dirección: se escribía y desaparecía de la app. El segmento "Incidentes"
> del historial ya existía como placeholder; esto lo convierte en la lectura real.

- [x] **7B.1** `IncidentHistoryRepository` cache-first: `observeIncidents()` desde Room + `refresh()` contra `incidents where reportedBy == uid`
- [x] **7B.2** Refresco sin `orderBy` a propósito: la consulta coincide con la regla y no necesita índice compuesto
- [x] **7B.3** Mapeo inverso documento → fila cacheada (`SYNCED`) y entrada `media` → fila `UPLOADED` sin archivo local
- [x] **7B.4** Segmento "Incidentes" real en `TeacherHistoryScreen`, con las mismas etiquetas de fecha que las listas
- [x] **7B.5** Detalle expandible: descripción, categoría, fecha, pin y archivos con **Abrir** a la URL de Cloudinary
- [x] **7B.6** Sin conexión se lee lo cacheado, incluidos los incidentes que todavía no subieron

**Criterio:** el docente vuelve a leer desde la app cualquier incidente que reportó, con su detalle y
sus fotos, con o sin conexión.

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/IncidentHistory.kt` | **nuevo** `IncidentSummary`, `IncidentStatus` y `IncidentEvidence` |
| `domain/repository/IncidentHistoryRepository.kt` | **nuevo** contrato de lectura: lista, refresco y archivos de un incidente |
| `data/local/dao/IncidentDao.kt` | `observeHistory()` con el conteo de archivos por subconsulta y `upsertDrafts` |
| `data/mapper/IncidentHistoryMapper.kt` | **nuevo** entidad → resumen, documento → entidad y entrada `media` → fila cacheada |
| `data/repository/FirestoreIncidentHistoryRepository.kt` | **nuevo** cache-first: `Source.SERVER` con filtro `reportedBy == uid`, sin `orderBy` |
| `ui/screens/teacher/TeacherHistoryUi.kt` | `incidentSections()`, `HistoryIncidentUi`, `incidentLocationLabel()`, `evidenceLabel()` |
| `ui/screens/teacher/TeacherHistoryViewModel.kt` | segunda lista y expansión independiente de la de listas |
| `ui/screens/teacher/TeacherHistoryScreen.kt` | segmento "Incidentes" real: tarjeta, detalle y "Abrir" del archivo |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | repositorio, ViewModel y callback cableados |
| `test/.../IncidentHistoryUiTest.kt` · `IncidentHistoryMapperTest.kt` | **nuevos**: buckets, etiquetas, archivos y mapeo inverso |

**Decisiones que importan**

- **Se lee bajo la regla que ya existe.** `resource.data.reportedBy == request.auth.uid` es la
  condición que `docs/firestore.rules` ya acepta para docente, así que la consulta no pide permisos
  nuevos ni toca la bandeja del admin, que es de Fase 2.
- **Sin `orderBy` en la consulta.** `reportedBy` + `occurredAt` pediría un índice compuesto y un
  despliegue más; el orden lo pone Room sobre lo cacheado, que además es lo correcto sin conexión.
- **El detalle no vuelve al servidor.** El refresco de la lista ya cacheó el documento y su array
  `media`; abrir una fila es una consulta local, no una lectura por incidente.
- **La foto se abre por URL.** La copia local se borra al terminar la subida, así que la única copia
  que queda es la de Cloudinary; el detalle abre esa y no inventa una miniatura que no puede pintar.
- **Un documento incompleto no se cachea.** Si le falta escuela, grupo o autor, se descarta: una fila
  sin nombre en el historial se lee como un bug de la app, no como un documento roto.

**Pendientes conocidos de este bloque**

1. ~~Sin `Coil`: las fotos se abren en el navegador~~ **resuelto**: Coil 3.6.3 ya está integrado y
   las fotos se ven como miniatura dentro de la app (bloque 11 y perfil de la directora).
2. **"Publicaciones" sigue con su placeholder** hasta el bloque 8, igual que antes.
3. **La consulta trae los últimos 120 incidentes**: paginar es de otra fase, como en el historial de
   listas.

---

## Bloque 8 — Tareas y avisos (PLAN 1.8)

> **Estado: ✅ HECHO** — 15 nov 2026 · `assembleDebug` verde · **179 tests unitarios, 0 fallos**
> (+12 de este bloque: 5 del documento de Firestore y 7 de la línea de tiempo de Publicaciones).
> **Commit principal `c87e1eb`** (26 archivos); el copy de los controles no disponibles se ajustó en
> `800621c`. El PDF de las tareas llegó después, como **bloque 8B** (`4ddc7b7` + `e837e16`).
> **La verificación en el teléfono la corre el dueño del proyecto**; los pasos van al final de cada
> bloque.

- [x] **8.1** `TaskRepository.publish`: `groupIds` (array), `dueDate`, `createdBy`, `active`
- [x] **8.2** `NoticeRepository.publish`: `audience.roles`, `audience.groupIds`, `urgent`
- [x] **8.3** Caché de lectura en Room (`TaskEntity`, `NoticeEntity`) desde el primer momento
- [x] **8.4** `TeacherTaskNewScreen` y `TeacherNoticeNewScreen` cableadas a grupos reales
- [x] **8.5** Eventos `task_published` y `notice_published`
- [x] **8.6** Segmento **Publicaciones** real en el historial del docente (línea de tiempo cache-first)
- [x] **8.7** Controles que aún no funcionan, honestos: adjuntos (resuelto en 8B) y notificación (bloque 10)

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/Publication.kt` | **nuevo** `NewTask`, `NewNotice`, `PublishOutcome`, `PublishedTask`, `PublishedNotice` |
| `domain/repository/TaskRepository.kt` · `NoticeRepository.kt` | **nuevos** contratos: `publish` offline-first + lectura cache-first |
| `data/mapper/PublicationMapper.kt` | **nuevo** documento `tasks/{taskId}` y `notices/{noticeId}` (audiencia anidada), y fila cacheada → fila de historial |
| `data/repository/RoomTaskRepository.kt` · `RoomNoticeRepository.kt` | **nuevos**: resuelven el grupo del roster cacheado, escriben caché + outbox y emiten el evento |
| `data/local/dao/TaskDao.kt` · `NoticeDao.kt` | `observeByCreator(uid)` para la lectura del docente |
| `sync/TaskSyncHandler.kt` · `NoticeSyncHandler.kt` | **nuevos**: `set()` idempotente por UUID sobre el documento del cliente |
| `sync/OutboxRepository.kt` · `sync/SyncOperation.kt` | `enqueueTask`/`enqueueNotice` transaccionales y `UPSERT_TASK`/`UPSERT_NOTICE` |
| `ui/screens/teacher/TeacherTaskNewUi.kt` · `TeacherTaskNewViewModel.kt` | **nuevos**: grupos reales, materia del perfil, fecha/hora de entrega, `canPublishTask` y etiquetas |
| `ui/screens/teacher/TeacherNoticeNewUi.kt` · `TeacherNoticeNewViewModel.kt` | **nuevos**: chips multigrupo, "todos mis grupos", urgencia y `canPublishNotice` |
| `ui/screens/teacher/TeacherTaskNewScreen.kt` · `TeacherNoticeNewScreen.kt` | cableadas a su ViewModel; footer con mensaje, estado de envío y confirmación; controles no disponibles con su motivo (adjuntos → 8B, notificación → bloque 10) |
| `ui/screens/teacher/TeacherHistoryUi.kt` · `TeacherHistoryViewModel.kt` · `TeacherHistoryScreen.kt` | segmento **Publicaciones** real: una sola línea de tiempo de tareas y avisos con su píldora de sync |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | repos, handlers registrados, ViewModels y las dos rutas cableadas |
| `test/.../PublicationMapperTest.kt` · `TeacherPublicationUiTest.kt` | **nuevos**: forma de los documentos, orden de la línea de tiempo, etiquetas y `canPublish` |

Lo que este bloque **no** trajo, y se resolvió después: el PDF de la tarea (bloque 8B), la notificación
push (bloque 10) y la lista de publicaciones desde otro teléfono (reglas, Fase 2).

**Decisiones que importan**

- **"Publicado" en esta pantalla significa encolado.** Igual que en incidentes: Room + outbox en una
  transacción, y el worker es lo único que habla con Firestore. El docente ve "guardada" porque eso
  es lo que es verdad en ese momento, no porque el documento ya esté en la nube.
- **La píldora de sync es por fila, no por lista.** La fuente es el outbox: una tarea escrita sin red
  queda "Pendiente de enviar" aunque el aviso que se publicó después ya haya llegado.
- **El grupo se resuelve del roster cacheado, nunca del formulario.** Las reglas exigen
  `groupIds.hasOnly(myGroups())`; mandar un id que el docente ya no imparte se rechaza en la nube
  cuando el formulario ya está cerrado.
- **`category` del aviso se deriva de `urgent`.** Una sola perilla: el chip que elige el docente y el
  valor por el que filtra el tutor no pueden contradecirse.
- **Un aviso multigrupo es un documento, no varios.** La audiencia es un array, así que marcar 3° B y
  4° A es el mismo aviso para las dos familias; duplicarlo habría dado dos veces el mismo timbre.
- **Nada de audiencia preseleccionada.** Ni grupo en la tarea ni "todos mis grupos" en el aviso: los
  dos formularios empiezan vacíos y el botón está deshabilitado hasta que el docente elige.
- **`attachments` y `expiresAt` se escriben explícitos** (lista vacía o el PDF del bloque 8B, y `null`).
  La caducidad no la decide Fase 1, pero el documento tiene la forma completa desde hoy.
- **"Publicaciones" lee solo Room.** Un docente no puede listar `tasks` ni `notices` con las reglas
  actuales (ver pendientes), así que el segmento es lo que este dispositivo publicó —que es
  exactamente lo que la caché ya tiene— y por eso funciona sin red.
- **Un control deshabilitado dice el motivo, no "próxima versión".** El mockup traía esa frase y
  confundía dos casos distintos: los adjuntos no eran de Fase 1 (hoy hay un PDF, ver 8B) y la
  notificación es el bloque 10 de esta misma fase. El interruptor del aviso, además, se veía activo
  aunque no respondía; ahora está atenuado e inerte (`800621c`).

**Cómo probarlo en el teléfono** (lo corre el dueño del proyecto)

1. Con red: publica una tarea para 3° B y un aviso urgente a 3° B y 4° A. En Firestore,
   `tasks/{id}` trae `groupIds`, `dueDate`, `createdBy`, `active` y `attachments: []`; `notices/{id}`
   trae `audience.roles: ["tutor"]`, `audience.groupIds`, `urgent: true`, `category: "urgente"`,
   `expiresAt: null`.
2. **Modo avión**: publica una tarea y un aviso → las dos aparecen en **Publicaciones** como
   "Pendiente de enviar". Cierra y reabre la app: siguen ahí. Reconecta → pasan a "Publicado" y hay
   **un solo documento** de cada uno.
3. Los formularios no dejan publicar sin grupo ni título: el botón está deshabilitado y el footer
   explica qué falta.
4. "Adjuntar PDF" ya funciona (ver 8B) y "Notificar a las familias" sigue deshabilitado con el motivo.

**Pendientes conocidos de este bloque**

1. **La lista del docente es local.** `notices` deja leer al docente solo si `audience.roles` incluye
   `'docente'` (los avisos van a tutores), y `tasks` exige `groupIds.hasAny(myGroups())` sin filtro por
   autor: ninguna de las dos se puede probar como consulta. Ver lo publicado desde otro teléfono pide
   ampliar reglas —decisión de Fase 2, con la bandeja del tutor—, no un parche aquí.
2. **Sin editar ni borrar publicaciones.** Las reglas sí permiten `update`/`delete` al autor; la
   pantalla de gestión no está en FASE 1.
3. **La fecha de entrega no se valida contra el presente**: el selector arranca en una semana y deja
   elegir una fecha pasada, que se publica igual.
4. **"Notificar a las familias" sigue deshabilitado**: el bloque 10 trajo el push manual (consola y
   `scripts/notify`), no un envío automático por evento, que en Spark requiere Blaze. Dejarlo activo
   sería prometer un envío que no ocurre. Los adjuntos se resolvieron en el bloque 8B.
5. **Verificación en el teléfono** a cargo del dueño del proyecto (modo avión → publicar tarea y aviso
   → reconectar → revisar Firestore y el segmento Publicaciones).

**Criterio:** una tarea publicada para 3° B y un aviso urgente para los tutores de 3° B aparecen en
Firestore con la audiencia correcta y se leen sin red después de la primera carga.

---

## Bloque 8B — Un PDF por tarea (agregado al bloque 8)

> **Estado: ✅ HECHO** — 17 nov 2026 · `assembleDebug` verde · **189 tests unitarios, 0 fallos**
> (+10 de este bloque: 9 de `TaskAttachmentMapperTest` y el adjunto en la línea de tiempo).

- [x] **8B.1** Un solo PDF por tarea, máximo 10 MB; ni fotos ni Word
- [x] **8B.2** Selector del sistema filtrado a `application/pdf`, con tope aplicado **mientras se lee**
- [x] **8B.3** Copia en `filesDir` (no caché) mientras la tarea espera señal
- [x] **8B.4** `UPLOAD_TASK_ATTACHMENT` encolado **antes** de `UPSERT_TASK`
- [x] **8B.5** Subida a `/raw/upload` de Cloudinary, con `asset_folder` por tarea

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `data/remote/cloudinary/CloudinaryConfig.kt` | `rawUploadUrl` para documentos |
| `data/remote/cloudinary/CloudinaryUploader.kt` · `OkHttpCloudinaryUploader.kt` | **nuevo** `uploadDocument` (`/raw/upload`, sin transformaciones); el código de request se comparte con imágenes |
| `data/local/media/TaskAttachmentStore.kt` | **nuevo**: copia el PDF a `filesDir/task-attachments` y lo borra al subir o al quitarlo |
| `data/mapper/TaskAttachmentJson.kt` | **nuevo**: `StoredTaskAttachment` en `TaskEntity.attachmentsJson` (`PENDING`/`UPLOADED`/`FAILED`) y el mapa de Firestore |
| `domain/model/Publication.kt` | `NewTask.attachment`, `NewTaskAttachment`, `PublishedTask.attachmentName` + `attachmentState` y el enum `AttachmentSyncState` |
| `data/mapper/PublicationMapper.kt` | `attachments` sale del estado real del archivo; la fila de historial lleva el nombre y el estado del PDF |
| `data/repository/RoomTaskRepository.kt` | guarda el adjunto en la fila y encola la subida antes del documento |
| `sync/SyncOperation.kt` · `OutboxRepository.enqueueTask` | **nuevo** `UPLOAD_TASK_ATTACHMENT`, con `createdAt` compartido para que se drene primero |
| `sync/UploadTaskAttachmentHandler.kt` | **nuevo**: sube el PDF, marca `UPLOADED` y borra la copia local |
| `sync/TaskSyncHandler.kt` | no escribe la tarea mientras el PDF esté `PENDING` |
| `ui/screens/teacher/TeacherTaskNewUi.kt` · `TeacherTaskNewViewModel.kt` | **nuevos** `TASK_ATTACHMENT_MAX_BYTES`, `taskAttachmentSizeLabel`, `attachPdf`/`removeAttachment` y limpieza en `onCleared` |
| `ui/screens/teacher/TeacherHistoryUi.kt` · `TeacherHistoryScreen.kt` | la tarjeta de Publicaciones muestra el PDF con su estado (`Subido` / `Pendiente` / `No se subió`), para que el docente pueda corroborarlo después |
| `ui/screens/teacher/TeacherTaskNewScreen.kt` | zona de adjuntos real: selector, chip con nombre y tamaño, cambiar y quitar |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | store y handler registrados; callbacks cableados |
| `test/.../TaskAttachmentMapperTest.kt` | **nuevo**: documento con y sin URL, columna JSON, columna corrupta y los tres estados en el historial |
| `test/.../TeacherPublicationUiTest.kt` | **ampliado**: la píldora del adjunto con `Subido` / `Pendiente` / `No se subió` |

**Decisiones que importan**

- **Un solo PDF.** Una tarea es una instrucción, no una transferencia de archivos. PDF porque abre
  igual en cualquier teléfono; Word y HEIC no.
- **Sin migración de Room.** El adjunto vive en `attachmentsJson`, la columna que ya existía para el
  array del documento. Una tabla aparte y su migración para un único archivo no compraban nada.
- **El archivo se sube antes de escribir la tarea.** `UPLOAD_TASK_ATTACHMENT` y `UPSERT_TASK` comparten
  `createdAt`, así que se drenan en ese orden; `TaskSyncHandler` además se niega a escribir con el
  archivo en vuelo. Publicar primero dejaría `attachments: []` para siempre.
- **`/raw/upload` y no `/image/upload`.** El endpoint de imágenes convierte el PDF en imagen; raw guarda
  los bytes y la familia descarga el documento que el docente adjuntó.
- **Un adjunto que no se puede subir no tumba la tarea.** Se marca `FAILED`, se borra la copia local y
  la tarea se publica sin archivo: perder el anuncio por un PDF ilegible sería peor. El historial sigue
  mostrando el nombre.
- **El tope se aplica leyendo**, no después: se consulta `OpenableColumns.SIZE` y, si el proveedor miente,
  la lectura se corta a los 10 MB. Cargar un escaneo de 40 MB para rechazarlo después es un `OOM`.

**Configuración que depende de Cloudinary (no de Firebase)**

1. El preset unsigned debe admitir PDF (o no tener lista de formatos). Con `allowed_formats=jpg,png,webp`
   la subida responde 400 y el adjunto se marca `FAILED`.
2. *Security → Allow delivery of PDF and ZIP files*. Sin ese interruptor el archivo sube, pero la URL
   responde 401 y el tutor no lo puede abrir.

Ninguno de los dos se configura en Firebase: las reglas de `tasks` no validan `attachments`, así que
esta parte no pide tocar el proyecto de Firebase.

**Cómo probarlo en el teléfono** (lo corre el dueño del proyecto)

1. Publica una tarea y toca **Adjuntar PDF**: el selector solo muestra PDFs. Elige uno chico → aparece
   el chip con nombre y tamaño; **Cambiar** lo reemplaza y **Quitar** lo elimina.
2. Prueba un PDF de más de 10 MB → "El PDF supera los 10 MB." (se corta durante la lectura).
3. Con red: publica con PDF → en Firestore, `tasks/{id}.attachments` trae **un elemento** con
   `fileName`, `mimeType: "application/pdf"`, `bytes` y `url` de `.../raw/upload/...`. Abre la URL: debe
   descargar el PDF (si da 401, falta el interruptor de PDF delivery en Cloudinary).
4. En **Publicaciones**, la tarjeta muestra `archivo.pdf · Subido` en verde; si publicaste en modo
   avión, primero dice `· Pendiente` y cambia sola a `· Subido` al reconectar.
5. Caso de fallo: si Cloudinary rechaza el archivo (p. ej. el preset no admite `pdf`), la tarea se
   publica igual y la tarjeta dice `· No se subió`.
6. Una tarea sin PDF no muestra ninguna píldora de archivo.

**Pendientes conocidos**

1. **El tutor todavía no ve el adjunto**: su pantalla es mock hasta su fase. El documento ya lleva la URL.
2. **Sin reintento manual del archivo.** El historial dice si el PDF subió, quedó pendiente o no se subió,
   pero no deja reintentarlo: si falló, el docente vuelve a adjuntarlo desde el formulario.
3. **Sin vista previa dentro de la app**: el PDF se abre con el visor del teléfono.

**Criterio:** en modo avión, adjuntar un PDF y publicar deja la tarea "Pendiente de enviar" con el
adjunto en "Pendiente"; al reconectar, `tasks/{id}.attachments` tiene un elemento con `url` de
Cloudinary, el historial pasa a "hoja.pdf · Subido" y hay un solo documento.

---

## Bloque 9 — Chat (PLAN 1.10)

> **Estado: ✅ HECHO** — 17 nov 2026 · commit `396830d` · `assembleDebug` verde · **205 tests unitarios,
> 0 fallos** (+16: 5 del id de conversación y 11 de etiquetas, filtros y contactos).
> Realtime Database creada y `docs/rtdb.rules` **publicadas** en la consola; verificado en el teléfono
> (abrir chat, enviar, lista con último mensaje y hora).

- [x] **9.1** `firebase-database` en el módulo (ya estaba desde la Fase 0)
- [x] **9.2** `docs/rtdb.rules` + `database` en `firebase.json` — **publicado en la consola**
- [x] **9.3** Esquema RTDB: `conversations/{cid}/messages/{id}` (`senderId`, `text`, `sentAt`) y `presence/{cid}/{uid}`
- [x] **9.4** `conversations/{id}` en Firestore: `participants`, `participantNames`, `lastMessage`, `lastMessageAt`, `unread`
- [x] **9.5** `ChatRepository` con `Flow` de conversaciones y mensajes + envío que aparece al instante
- [x] **9.6** `TeacherChatScreen` y `TeacherConversationScreen` reales; evento `chat_message_sent`

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `docs/rtdb.rules` | **nuevo**: membresía derivada del id, mensajes inmutables de 1..2000 caracteres, presencia por uid |
| `firebase.json` | sección `database` apuntando a `docs/rtdb.rules` |
| `domain/model/Chat.kt` | **nuevo**: `ChatConversation`, `ChatContact`, `ChatMessage`, `chatConversationId`, `otherParticipant` y los dos resultados tipados |
| `domain/repository/ChatRepository.kt` | **nuevo** contrato: conversaciones, mensajes, presencia, contactos, abrir, enviar, marcar leído |
| `data/repository/FirestoreChatRepository.kt` | **nuevo**: Firestore para resúmenes y contactos, RTDB para mensajes y presencia |
| `ui/screens/teacher/TeacherChatUi.kt` | **nuevo**: fila con hora relativa, iniciales, filtros y búsqueda puros y testeables |
| `ui/screens/teacher/TeacherChatViewModel.kt` | **nuevo**: lista real, búsqueda, chip de grupo, hoja de tutores y apertura de chat |
| `ui/screens/teacher/TeacherConversationUi.kt` (en `TeacherChatUi.kt`) · `TeacherConversationViewModel.kt` | **nuevos**: mensajes de RTDB, presencia, borrador, envío y recibo de lectura |
| `ui/screens/teacher/TeacherChatScreen.kt` · `TeacherConversationScreen.kt` | cableadas: filas reales, hoja "Nuevo mensaje", cabecera con nombre y presencia, burbujas reales |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | `chatRepository`, `FirebaseDatabase`, los dos ViewModels y las rutas de chat cableadas |
| `test/.../ChatTest.kt` · `TeacherChatUiTest.kt` | **nuevos**: id determinista, membresía, "Hoy/Ayer/Lun", iniciales, filtros y contactos |

**Configuración de Firebase (hecha el 17 nov 2026, se deja como registro)**

1. **Realtime Database creada**: *Build → Realtime Database*, región `us-central1`, reglas no abiertas.
2. **`docs/rtdb.rules` publicadas** en la pestaña **Rules** de la base (o `firebase deploy --only database`).
   Son la única barrera de la base: sin ellas, cualquiera con la config lee todas las conversaciones.
3. **Índice de Firestore** `conversations: participants + lastMessageAt desc` desplegado (está versionado en
   `docs/firestore.indexes.json`).
4. **App Check** no está forzado para RTDB; si algún día se fuerza, el build de debug necesita su token.

> Al clonar el repo no hay que repetir nada de esto: la base existe en el proyecto y las reglas están
> publicadas. Solo se repite si se recrea el proyecto de Firebase desde cero.

**Decisiones que importan**

- **Un chat, un id determinista.** `conversations/{uidMenor__uidMayor}`: los dos lados calculan el mismo
  documento sin consultar, y abrir el mismo chat dos veces no lo duplica.
- **La membresía de RTDB se deduce del id.** Las reglas de RTDB **no pueden leer Firestore**, así que el
  id lleva los dos uids y las reglas comprueban `beginsWith`/`endsWith`. Sin eso, o se abre la base a
  todos o no hay forma de autorizar.
- **El docente no lee perfiles de tutores.** Las reglas de `users` solo dejan leer el propio perfil o al
  directivo; por eso los contactos salen del roster (`students.tutorIds`) y se llaman "Tutor de
  Mariana Gómez". Inventar un nombre sería mentir en la lista.
- **RTDB primero, Firestore después.** El mensaje es la verdad; el resumen es para pintar la lista. En
  ese orden, un resumen nunca describe un mensaje que no existe.
- **Sin Room para el chat.** RTDB ya trae su propia caché en disco; una segunda copia sería una segunda
  verdad. La lista de conversaciones sí necesita red la primera vez.
- **`sentAt` del cliente, no `ServerValue.TIMESTAMP`.** El marcador de servidor viaja como un mapa y la
  burbuja optimista mostraría la hora vacía hasta que respondiera el servidor. El desfase entre dos
  teléfonos es aceptable en un chat escolar.
- **Ventana de 200 mensajes.** Paginar hacia atrás es una decisión de producto, no algo que se resuelva
  con un `limitToLast` más grande.

**Pendientes conocidos**

1. **El chat del tutor sigue siendo visual** (mock): es de su fase. La otra cuenta se puede simular
escribiendo en RTDB desde la consola, que es como se prueba el tiempo real hoy.
2. **Sin adjuntos en el chat.** Fotos en la conversación no están en FASE 1.
3. **Sin editar ni borrar mensajes**: la regla exige `!data.exists()`; un mensaje enviado es definitivo.
4. **El resumen puede quedar atrás sin red.** RTDB encola el mensaje y lo entrega al reconectar, pero el
   resumen de Firestore se intenta una sola vez: el mensaje llega y la lista puede mostrar el anterior
   hasta el siguiente envío.
5. **Sin paginación** más allá de los últimos 200 mensajes.

**Cómo probarlo en el teléfono** (lo corre el dueño del proyecto)

1. Con la RTDB creada y las reglas publicadas: **Mensajes → botón de editar** → aparece la lista de
   tutores de tus grupos ("Tutor de … · 3° B").
2. Elige uno → se abre la conversación vacía y se crea
   `conversations/{uidDocente__uidTutor}` en Firestore con `participants`, `lastMessage`, `lastMessageAt`
   y `unread`.
3. Escribe "Hola" → la burbuja aparece de inmediato y en RTDB queda
   `conversations/{cid}/messages/{id}` con `senderId`, `text` y `sentAt`.
4. Vuelve a **Mensajes**: la fila muestra "Hola" y la hora. Entra otra vez: el contador de no leídos
   queda en 0.
5. **Tiempo real**: escribe un mensaje en RTDB desde la consola (`senderId` = uid del tutor, `text`,
   `sentAt` numérico) con la conversación abierta → aparece sin recargar, y la cabecera muestra
   "En línea" mientras el otro esté conectado.
6. **Modo avión**: escribe y envía → la burbuja aparece y el aviso dice "se enviará al reconectar"; al
   volver la red, el mensaje está en RTDB.

**Criterio:** dos cuentas intercambian mensajes en tiempo real y la lista de chats muestra el último
mensaje y la hora, sin recargar la pantalla. **Hoy se cumple con la segunda cuenta simulada desde la
consola**; el intercambio real entre dos teléfonos llega con el chat del tutor (Fase 2).

---

## Bloque 10 — Notificaciones y FCM (PLAN 1.11)

> **Estado: ✅ HECHO Y PROBADO EN TELÉFONO** — 17 nov 2026 · `assembleDebug` verde · **219 tests
> unitarios, 0 fallos** (+14: 5 de deep links, 7 de la bandeja y 2 del token al cerrar sesión).
>
> Probado en un dispositivo real: el token aparece en `users/{uid}/fcmTokens/{token}` al iniciar
> sesión (y se borra al cerrarla), el *Send test message* de la consola llega con la app abierta y
> cerrada, y un documento creado a mano en `notifications` aparece en la bandeja con su hora, su
> tipo y el badge de no leídos, se marca leído al tocarlo y navega al `deepLink`.

- [x] **10.1** `FirebaseMessagingService`: token en `users/{uid}/fcmTokens/{token}` (`token`, `platform`, `updatedAt`)
- [x] **10.2** Rotación de token (`onNewToken`) y borrado del token al cerrar sesión
- [x] **10.3** Canal de notificación + permiso `POST_NOTIFICATIONS` en Android 13+
- [x] **10.4** Apertura por `deepLink` (lista blanca de rutas, no un pass-through)
- [x] **10.5** `TeacherNotificationsScreen` con la colección `notifications` real (`userId`, `read`)
- [x] **10.6** Escritura de `fcmTokens` validada contra las reglas (solo el dueño escribe su subcolección)

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/AppNotification.kt` | **nuevo**: modelo del documento y `NotificationType` con su `firestoreValue` |
| `domain/repository/NotificationRepository.kt` · `PushTokenRepository.kt` | **nuevos** contratos: bandeja de solo lectura + token del dispositivo |
| `data/repository/FirestoreNotificationRepository.kt` | **nuevo**: snapshot en vivo de `notifications` por `userId`, `markRead` y `markAllRead` en batch |
| `data/repository/FirestorePushTokenRepository.kt` | **nuevo**: `users/{uid}/fcmTokens/{token}` con el token como id |
| `push/AulaSeguraMessagingService.kt` | **nuevo**: `onNewToken` (registra + log en debug) y `onMessageReceived` (data messages → notificación local) |
| `push/NotificationPoster.kt` | **nuevo**: canal único, permiso de Android 13 y `PendingIntent` con `deepLink` |
| `res/drawable/ic_stat_notification.xml` | **nuevo**: icono monocromo de la notificación |
| `navigation/DeepLinks.kt` | **nuevo**: `resolveDeepLink` con lista blanca (rechaza rutas desconocidas y chats con segmentos extra) |
| `MainActivity.kt` | lee los extras del intent, pide `POST_NOTIFICATIONS` y pasa el `deepLink` al NavHost |
| `navigation/AulaSeguraNavHost.kt` | navega al `deepLink` cuando hay sesión resuelta |
| `ui/screens/access/SessionViewModel.kt` | registra el token al entrar y lo borra antes de cerrar sesión (con timeout) |
| `ui/screens/teacher/TeacherNotificationsUi.kt` · `TeacherNotificationsViewModel.kt` | **nuevos**: agrupación HOY/ANTERIORES, chips por tipo, no leídos y tap que marca leído y sigue el `deepLink` |
| `ui/screens/teacher/TeacherNotificationsScreen.kt` | cableada a la colección real; iconos y tintes por `type` |
| `test/.../DeepLinkTest.kt` · `TeacherNotificationsUiTest.kt` · `AuthViewModelsTest.kt` | **nuevos/ampliados**: whitelist, agrupación, filtros, badge y borrado del token al salir |
| `scripts/notify/index.js` · `package.json` | **nuevos**: una corrida escribe el documento de la bandeja y manda el push al usuario |

**Decisiones que importan**

- **El id del documento es el token.** Registrar dos veces escribe el mismo documento, y dos teléfonos
  no se pisan (una subcolección y no un array, por lo mismo que `fcmTokens` en `CONTEXT.md` §6.2).
- **El token se borra antes de cerrar sesión**, con timeout de 2.5 s: un teléfono compartido no puede
  seguir recibiendo el push del docente anterior, pero el logout tampoco puede colgarse sin red.
- **`deepLink` es una lista blanca.** El valor viene de un documento escrito fuera de la app; pasarlo
  directo a `navigate()` permitiría abrir cualquier ruta. Desconocido → no abre nada.
- **La bandeja es de solo lectura salvo `read`.** Las reglas ya lo limitan así: un cliente que pudiera
  crear notificaciones podría falsificar un comunicado de la dirección.
- **Sin caché local.** Es un documento pequeño por aviso, el índice ya existe, y una bandeja de ayer
  engaña más que una que pide red.
- **Un solo canal de notificación.** Dividir en "mensajes", "asistencia" y "avisos" es una decisión de
  producto, y un canal creado no se puede borrar.

**Configuración de Firebase (comprobar, no crear)**

1. **No hay nada que activar para recibir.** `google-services.json` ya está versionado y FCM viene con
   el proyecto; si en *Project settings → Cloud Messaging* aparece deshabilitada la API **V1**, hay que
   habilitarla (es un botón, no un plan de pago).
2. **Enviar la prueba**: *Messaging → Send test message* pide un token, no una campaña.
3. **Los documentos de `notifications` los escribe la consola o el script**: las reglas prohíben que un
   cliente los cree. Para probar la bandeja, crear un documento a mano con `userId` = tu uid, `title`,
   `body`, `type`, `deepLink`, `read: false` y `createdAt`.

**Cómo probarlo en el teléfono** (lo corre el dueño del proyecto)

1. Instalar el build de debug, entrar como docente y aceptar el permiso de notificaciones de Android 13.
2. Copiar el token de `adb logcat -s FCM` (solo aparece en debug).
3. Verificar en Firestore que quedó `users/{tuUid}/fcmTokens/{token}` con `platform: "android"`.
4. Firebase Console → Messaging → *Send test message* → pegar el token → enviar. La notificación debe
   llegar con la app abierta y cerrada.
5. En el campo de datos (Custom data), usar `deepLink` = `teacher/history` (o `teacher/chat/<id>`): al
   tocar la notificación debe abrir esa pantalla. Un valor inventado no debe abrir nada.
6. Bandeja: crear un documento en `notifications` con tu uid → aparece con su hora y su tipo; tocarlo lo
   marca leído; "marcar todo leído" baja el contador a cero.
7. Cerrar sesión → el documento del token desaparece de `users/{uid}/fcmTokens`.

**Enviar un aviso completo (documento + push) en una corrida**

En Spark `notifications` y FCM no se hablan: la consola manda el push pero no escribe el documento, y
un documento por su cuenta no hace sonar nada. El script hace las dos cosas, en ese orden, y borra los
tokens muertos que encuentre.

```bash
# Una sola vez: dependencias del script
cd scripts/notify && npm install && cd ../..

# La cuenta de servicio NO va al repo (scripts/notify/service-account*.json está ignorado):
# Firebase Console → Project settings → Service accounts → Generate new private key

node scripts/notify/index.js \
  --uid=<uidDelDocente> \
  --title="Junta de padres de familia" \
  --body="Viernes 20 de noviembre, 8:00, patio central" \
  --type=aviso \
  --deep-link=teacher/history
```

- `--no-push` escribe solo la bandeja; `--dry-run` imprime lo que haría sin tocar nada.
- El push sale solo con `data` (título, cuerpo, `notificationId`, `deepLink`): si llevara payload
  `notification`, el sistema mostraría la suya y el tap perdería el `deepLink`.
- Si `users/{uid}/fcmTokens` está vacío, el documento queda en la bandeja y el script avisa: ese usuario
  nunca abrió la app en su teléfono (o cerró sesión, que es cuando se borra el token).

**Pendientes conocidos**

1. **Sin envío automático.** En Spark no hay Cloud Functions: el push lo dispara una persona con
   `scripts/notify` o desde la consola. Un push "cuando pasa X" no existe todavía y no se debe diseñar
   como si existiera.
2. **Los `deepLink` solo cubren rutas del docente.** Cuando existan las pantallas del tutor y del
   directivo se agregan a la lista blanca.
3. **Sin contador de no leídos en la campana** de la barra inferior: el número vive en la pantalla.
4. **Sin agrupación de notificaciones ni sonido/prioridad por tipo** (canal único).

> **Esto es Spark: no hay envío automático.** En plan gratuito no hay Cloud Functions, así que un
> push "cuando pasa X" **no existe** todavía. En esta fase el envío es manual desde la consola y el
> documento en `notifications` se escribe por script o consola (las reglas lo reservan al Admin SDK).
> No diseñes un flujo que dependa de push automático: no va a sonar.

---

## Bloque 11 — Perfil (PLAN 1.12)

> **Estado: ✅ HECHO** — 17 nov 2026 · `assembleDebug` verde · **234 tests unitarios, 0 fallos**
> (+15: 2 del documento de la foto, 1 de la caché del perfil, 5 de etiquetas del perfil y el saludo,
> 2 del `update()` de perfil y 5 de validaciones de edición y contraseña).
> **La verificación en el teléfono la corre el dueño del proyecto**; los pasos van al final.

- [x] **11.1** `TeacherProfileScreen` con datos reales (`fullName`, `email`, `schoolId`, `subjects`)
- [x] **11.2** Saludo del `TeacherHomeScreen` desde `fullName` (cierra el pendiente de la Fase 0)
- [x] **11.3** Foto: comprimir + subir a `aulasegura/profiles/{uid}` + escribir `photoUrl` y `photoPublicId`
- [x] **11.4** Foto visible en la UI y persistida entre sesiones (Coil)
- [x] **11.5** Logout y cierre forzado por `active = false` siguen funcionando
- [x] **11.6** "Editar perfil": nombre y materias reales, guardados en `users/{uid}` y en la caché
- [x] **11.7** "Cambiar contraseña": actual + nueva + confirmación, con reautenticación de Firebase
- [x] **11.8** "Permisos de la app": la pantalla consulta el estado real del sistema y abre Ajustes
- [x] **11.9** "Notificaciones": interruptor que refleja el permiso real de Android y lo cambia

**Qué cambió en este bloque**

| Archivo | Cambio |
|---|---|
| `domain/model/AppUser.kt` | `photoPublicId`, para representar el campo que ya existe en `users/{uid}` |
| `domain/repository/AuthRepository.kt` · `data/repository/AuthRepositoryImpl.kt` | **nuevo** `updateProfilePhoto`: `update()` de solo dos campos, refresco de la memoria y de la caché Room, y resultado tipado |
| `data/mapper/ProfilePhotoMapper.kt` | **nuevo**: el mapa de `photoUrl` + `photoPublicId`, sin tocar `role`, `schoolId` ni `active` |
| `ui/screens/teacher/TeacherProfileUi.kt` · `TeacherProfileViewModel.kt` | **nuevos**: estado del perfil, iniciales, materia del perfil, saludo y flujo cámara/galería → compresión → Cloudinary → perfil |
| `ui/screens/teacher/TeacherProfileScreen.kt` | cableada: nombre, correo, escuela y materias reales; `AsyncImage` con iniciales de respaldo; diálogo para tomar foto o elegirla; aviso de subida y de resultado |
| `ui/screens/teacher/TeacherHomeScreen.kt` | saludo y avatar desde el perfil de la sesión, en vez de "Hola, profe Ricardo" / "RS" |
| `di/AppContainer.kt` · `navigation/AulaSeguraNavHost.kt` | factory del ViewModel del perfil, ruta cableada y perfil de la sesión propagado al inicio |
| `gradle/libs.versions.toml` · `app/build.gradle.kts` | **Coil3 3.6.3** (alias `coil-compose` + `coil-network-okhttp`), versión actual del proyecto tras integrar la Fase 2 |
| `app/src/main/res/xml/file_paths.xml` | `cache-path` propio para la foto del perfil, separado del de incidentes |
| `test/.../ProfilePhotoMapperTest.kt` · `TeacherProfileUiTest.kt` · `ProfileMappersTest.kt` · `AuthViewModelsTest.kt` | **nuevos/ampliados**: forma del `update`, round-trip de la caché, iniciales, saludo, materia y fake del repositorio |

**Decisiones que importan**

- **Sin migración de Room para el perfil.** `photoPublicId` se escribe en Firestore y viaja en
  `AppUser`, pero no se guarda en `ProfileEntity`. El criterio solo necesita `photoUrl`, que ya tiene
  columna, para volver a mostrar la foto después de reiniciar. (La base pasó a `version = 4` en la
  Fase 2 por `requireLocation` de `SchoolEntity`; el perfil no participó en esa migración.)
- **Dos campos, no un `set()`.** El perfil lo escriben también los flujos del directivo; mandar el
  documento completo desde el teléfono del docente competiría con `role`, `schoolId` y `active`.
  Las reglas ya prohíben cambiarlos, y el `update()` tampoco los menciona.
- **La foto no pasa por la outbox.** Es una escritura pequeña y el perfil se relee al entrar; una
  cola de sincronización para un avatar no compraba nada y dejaría la pantalla con una URL vieja.
- **El permiso de notificaciones no se toca desde aquí.** El interruptor del mockup queda atenuado
  con el motivo: Android 13 lo pide `MainActivity` al abrir la app.
- **"MIS MATERIAS", no tres grupos inventados.** El mockup mostraba 3° B / 3° A / 2° C con "28
  alumnos" cada uno; los grupos reales están en "Mis grupos" y aquí solo se muestra lo que el
  perfil sí contiene.
- **La subida usa el mismo stack que los incidentes.** `ImageCompressor` y `CloudinaryFolder.PROFILES`
  ya existían; el avatar solo cambia el `scopeId` por el uid y escribe la URL devuelta en el perfil.

**Cómo probarlo en el teléfono** (lo corre el dueño del proyecto)

1. Entra como docente: el saludo dice tu nombre real y el avatar muestra tu foto si la tienes, o tus
   iniciales si no. La tarjeta "MIS MATERIAS" muestra tus materias del perfil.
2. Toca la cámara → **Tomar foto** o **Elegir de la galería** → el aviso "Subiendo foto…" y después
   "Foto actualizada." En Firestore, `users/{tuUid}` trae `photoUrl` y `photoPublicId` con la ruta
   `aulasegura/profiles/{uid}`.
3. Cierra la app con el botón de atrás y vuelve a abrirla: la foto sigue ahí sin haber tocado red
   (viene de la caché Room del perfil).
4. Cierra sesión: `users/{uid}/fcmTokens` queda vacío y vuelve la pantalla de acceso.
5. Desde la consola, pon `active: false` en tu usuario con la app abierta: la sesión se cierra sola.

**Pendientes conocidos**

1. **La foto anterior no se borra de Cloudinary.** Queda como asset huérfano cuando se reemplaza;
   borrarla pide una llamada de administración con la API de Cloudinary, fuera de Fase 1.
2. **`photoPublicId` no se cachea en Room.** Si abres la app sin red, la foto se muestra por su URL,
   pero el `publicId` local es `null`; no afecta porque una foto nueva genera uno nuevo.
3. **Sin recorte ni filtros.** La foto se comprime y se sube tal cual; el recorte interactivo es de
   una fase posterior.
4. **Sin cámara en tablet sin cámara.** La opción de galería sigue disponible.
5. **Verificación en el teléfono** a cargo del dueño del proyecto (los 5 pasos de arriba).

**Extensión de los ajustes de la cuenta** (mismo commit del bloque, 17 nov 2026)

Las cuatro filas que seguían decorativas en el perfil ya hacen algo:

| Fila | Comportamiento |
|---|---|
| Editar perfil | Diálogo con nombre y materias (separadas por coma); `update()` de solo `fullName` y `subjects`, con refresco de la memoria y de la caché Room |
| Cambiar contraseña | Diálogo con actual + nueva + confirmación; reautenticación con la contraseña actual antes de `updatePassword`, y mensaje concreto para cada fallo |
| Permisos de la app | Abre `Routes.PERMISSIONS`, que ahora consulta ubicación y notificaciones con `ContextCompat.checkSelfPermission` y permite pedirlas o abrir Ajustes |
| Notificaciones | Muestra el permiso real del teléfono; si falta lo pide (API 33+) y si ya está concedido abre los ajustes de notificaciones de la app |

Decisiones que importan:

- **No se escribe `preferences.notificationsEnabled`.** Ningún código de `data/`, `domain/` ni el
  script de envío lo lee, y en Spark no hay Cloud Functions: escribirlo no cambiaría que el teléfono
  suene o deje de sonar. El permiso del sistema es la única verdad, y se consulta también al volver
  de Ajustes (`ON_RESUME`).
- **El `update()` del perfil manda dos campos, nunca el documento completo.** Las reglas usan una
  lista negra de campos inmutables; no mandar `role`, `schoolId` ni `active` evita pisar un cambio
  que la dirección haga al mismo tiempo.
- **La contraseña pide reautenticación.** Firebase exige un login reciente para `updatePassword`;
  sin eso el cambio falla a mitad de jornada.
- **La tarjeta de "Cámara" de la pantalla de permisos se quitó.** El manifest no declara `CAMERA`:
  las fotos usan la cámara del sistema o *GetContent*, y un botón para conceder un permiso que no
  existe sería una mentira. Quedó una nota que lo explica.

Cómo probarlo en el teléfono:

1. **Editar perfil**: cambia el nombre y las materias, guarda, y vuelve al inicio: el saludo usa el
   nombre nuevo y "MIS MATERIAS" las nuevas. En Firestore cambiaron solo esos dos campos.
2. **Cambiar contraseña**: prueba contraseña actual equivocada (mensaje específico), nueva corta,
   confirmación distinta y finally el caso válido. Cierra sesión y entra con la nueva.
3. **Permisos de la app**: la fila abre la pantalla; ubicación y notificaciones muestran su estado
   real. Quita la ubicación desde Ajustes, vuelve y la tarjeta debe decir "Pendiente".
4. **Notificaciones**: con el permiso concedido, tocar la fila abre los ajustes de notificaciones
   de la app; al desactivarlas desde ahí y volver, el interruptor debe aparecer apagado.

**Criterio:** el docente cambia su foto, cierra la app, vuelve a entrar y la foto sigue ahí; el
saludo muestra su nombre real.

---

## Cierre de la Fase 1

- [ ] **C1** Modo avión: pase de lista completo **y** incidente con foto
- [ ] **C2** Reconectar: ambos en Firestore, **sin duplicados**, media subida
- [x] **C3** `assembleDebug` verde
- [x] **C4** `testDebugUnitTest` verde (379 tests, 0 fallos al integrar la Fase 2; la Fase 0 tenía 61)
- [ ] **C5** `connectedDebugAndroidTest` verde en device físico
- [x] **C6** Reglas de Firestore, índices y reglas de RTDB publicadas con `firebase deploy --project aulasegu`
- [x] **C7** `FASE1.md` completo y `CONTEXT.md` actualizado (Room v4 y Coil3 3.6.3)

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
