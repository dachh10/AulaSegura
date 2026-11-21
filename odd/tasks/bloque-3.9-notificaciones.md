# Bloque 3.9 — Notificaciones del tutor, deep links y push

Rama: `main` · entrega: commit en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantalla: `ParentNotificationsScreen` (Stitch `95a5d9030ce548b890a8491db6e9161f`, bloque 2.12)

## Specs

**S1 — La bandeja del tutor usa datos reales.** `ParentNotificationsScreen` deja de usar `sampleNotifications()` y el modelo privado `Notification`: `ParentNotificationsViewModel` lee `notifications/{id}` con `FirestoreNotificationRepository` y la pantalla sólo pinta el estado. La bandeja tiene carga, vacío real y error de lectura; nunca “sin notificaciones” cuando en realidad falló la consulta.

**S2 — El corte por día y la hora no se reimplementan.** `splitByToday` (nuevo, en `ui/notifications/NotificationBuckets.kt`) separa `HOY` y `ANTERIORES` por fecha en la zona del dispositivo y conserva el orden del repositorio. La hora de cada fila usa `chatTimeLabel`, la misma convención del chat: `10:41`, `Ayer`, `5 nov`.

**S3 — Los chips filtran por `type`, no por título.** Se mantienen los del diseño — `Todas`, `Asistencia`, `Tareas`, `Avisos`, `Mensajes` — y sólo se agregan `Incidentes` y `Sistema` cuando hay filas de ese tipo. `Sistema` incluye `OTRO`, el valor al que cae un `type` desconocido.

**S4 — “Importante” sale del documento, no del título.** Una fila es urgente si su `type` es `ASISTENCIA` o `INCIDENTE`. Una tarea, un aviso o un mensaje no llevan la etiqueta. El documento no tiene severidad, así que la etiqueta se deriva del tipo.

**S5 — Leer es escribir sólo `read`.** Tocar una fila la marca leída; “Marcar todo como leído” usa el batch del repositorio y el botón se apaga cuando no hay pendientes. El badge de la campana del inicio es real: `ParentHomeViewModel.unreadNotifications` observa la misma colección, cuenta sólo `read == false` y no dibuja nada a cero.

**S6 — El deep link se abre por whitelist.** La fila entrega `notifications.deepLink` a la navegación; `resolveDeepLink` ya tenía las rutas del tutor (`parent/tasks/{id}`, `parent/incidents/{id}`, `parent/chat/{id}`, `parent/notices`, `parent/notifications`, entre otras). Un enlace desconocido se consume y no navega, para que un segundo toque con el mismo valor sí vuelva a disparar.

**S7 — Tests y documentación.** Tests JVM para el corte por día, los filtros, la urgencia, el ViewModel de la bandeja y el badge del inicio; `FASE3.md`, `CONTEXT.md` y este logbook al día en el mismo commit.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S1, S2, S3, S4 | `ParentNotificationsUi`, grupos, filas, chips y etiqueta de urgencia | `ui/screens/parent/ParentNotificationsUi.kt` | ✅ |
| T2 | S1, S5, S6 | Estado, listener, filtro, marcar leída, marcar todas y deep link pendiente | `ui/screens/parent/ParentNotificationsViewModel.kt` | ✅ |
| T3 | S1, S3, S4 | Pantalla real sobre el diseño 2.12, sin maqueta | `ui/screens/parent/ParentNotificationsScreen.kt` | ✅ |
| T4 | S5 | Badge real en la campana del inicio | `ui/screens/parent/ParentHomeViewModel.kt`, `ui/screens/parent/ParentHomeScreen.kt` | ✅ |
| T5 | S6 | Ruta con ViewModel, consumo del deep link y factory | `navigation/AulaSeguraNavHost.kt`, `di/AppContainer.kt` | ✅ |
| T6 | S2 | Corte `HOY`/`ANTERIORES` compartido y reutilizado por la bandeja del docente | `ui/notifications/NotificationBuckets.kt`, `ui/screens/teacher/TeacherNotificationsUi.kt` | ✅ |
| T7 | S7 | Tests de grupos, filtros, urgencia, ViewModel y badge | `ui/screens/parent/ParentNotificationsUiTest.kt`, `ParentNotificationsViewModelTest.kt`, `ParentHomeViewModelTest.kt` | ✅ (3 suites, 16 tests) |
| T8 | S7 | Docs: fase, contexto y logbook | `FASE3.md`, `CONTEXT.md`, `odd/tasks/bloque-3.9-notificaciones.md` | ✅ |

## Log

**L1 — 20 nov 2026, pedido del dueño.** «He pensado en quitar el punto 3.9 de notificaciones ya quecomo tal lo lelga ahi ya lo podemos ver de forma independiente en cada uno de sus apartados, siento que ese punto ya es mejor eliminarlo, dime que opinas, lo dejamos o que se vaya.»

**L2 — 20 nov 2026, decisión.** «No elijo ni A ni B, entocnes mejor dejemos como esta el 3.9 y pasa a implementarlo, tal cual lo dice el plan y la fase3.md». Se implementa completo: bandeja real del tutor, badge, marcado de lectura y deep links; el push sigue saliendo desde la consola porque Spark no tiene trigger.

**L3 — 20 nov 2026, decisiones de ingeniería.**

- **Se reusa el repositorio, no se copia por rol.** `FirestoreNotificationRepository` ya consulta `userId + createdAt desc`; el tutor, el docente y el directivo comparten la misma instancia de `AppContainer`. Una copia por rol habría duplicado la query y el índice.
- **El corte por día se movió a `ui/notifications`.** Docente y tutor lo necesitan igual; tener dos `SimpleDateFormat` con la misma zona es la clase de detalle que se desincroniza sin que nadie lo note. La bandeja del docente ahora lo reutiliza y su API pública no cambió.
- **“Importante” no se inventa.** El documento no lleva severidad: se deriva de `type` (`ASISTENCIA`/`INCIDENTE`) y se documenta, en vez de mirar el título como hacía la maqueta (`contains("justificada")`).
- **Los chips extra sólo aparecen con datos.** Un tutor sin incidentes ve exactamente los cinco chips aprobados; uno con incidentes puede filtrarlos en vez de buscarlos a mano bajo `Todas`.
- **El badge no miente.** La campana tenía “3” escrito en el código. Ahora cuenta lo que el listener ve y desaparece a cero; un fallo de lectura deja 0, no un número viejo.
- **Un deep link desconocido también se consume.** Si no, el `LaunchedEffect` no volvería a dispararse con el mismo valor y la fila quedaría muerta.
- **No hubo cambios de reglas, colecciones ni índices.** `NotificationRepository` y `FirestoreNotificationRepository` sólo cambiaron comentarios para dejar de hablar sólo del docente.

**L4 — 20 nov 2026, verificación.**

- **Tests nuevos (3 suites, 16 tests; total 75 suites / 548 tests / 0 fallos):** `ParentNotificationsUiTest` (8) — corte por día, cubetas vacías, hora del chat, badge, urgencia por tipo, chips del diseño, filtro por tipo y deep link/leído de la fila; `ParentNotificationsViewModelTest` (6) — filas y chips reales, filtro, apertura con `read` + deep link, fallo de “marcar todas”, fallo del listener y bandeja vacía; `ParentHomeViewModelTest` (2) — badge sólo con no leídas y fallo del listener en 0.
- **Build:** `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**, `app-debug.apk` generado.
- **Pendiente en dispositivo:** crear una notificación desde la consola con `userId` del tutor y `deepLink` real, enviar el push al token del dispositivo y comprobar que la fila aparece, el badge coincide y el detalle se abre.
