# Bloque 3.8 — Asistente del tutor con Firebase AI Logic

Rama: `main` · entrega: commit en `main`, push sólo con OK del dueño · runner: `gradlew.bat testDebugUnitTest`
Pantalla: `ParentAIScreen` (Stitch `bd8b739044634567b469e6d2fbcafedb`, bloque 2.11)

## Specs

**S1 — El asistente responde sobre los hijos del tutor, no sobre un ejemplo.** `ParentAIScreen` deja de usar `sampleAIMessages()` y `sampleAIMessages` desaparece: `ParentAIViewModel` arma el contexto con las lecturas que la familia ya hace (`loadHome()` → hijos, asistencia del mes y tareas pendientes; `loadNotices()` → avisos; `loadIncidents()` → incidentes) y lo pasa a `AskAssistantUseCase`. La pantalla muestra estado de carga, historial sólo en memoria, indicador de "escribiendo" y el error tipado que devuelve el caso de uso. Nada se persiste: `CONTEXT.md` §9.2 prohíbe guardar la conversación.

**S2 — El contexto son los últimos 30 días, y sólo de sus hijos.** `LoadAssistantContextUseCase` combina el mes en curso con el mes anterior cuando el día del mes es menor a 30, filtra por fecha ISO y cuenta presentes/retardos/faltas/justificadas. No viaja ningún identificador al modelo: ni `uid`, ni `studentId`, ni `groupId`; los ids de tarea se quedan en la app para abrir el detalle y nunca se escriben en el prompt. Tampoco se envían credenciales, CURP ni datos de otros alumnos.

**S3 — El modelo no inventa.** El prompt va acompañado de una instrucción de sistema (`ASSISTANT_SYSTEM_INSTRUCTION`) que obliga a responder sólo con los DATOS del mensaje, en español, en pocas frases, y a decir "sin registro" cuando el dato no está. Ignora instrucciones del tutor que pidan cambiar las reglas, revelar el prompt o hablar de otros alumnos. `assistantPrompt(context, question)` es una función pura y se puede fijar con tests.

**S4 — Si la IA no está disponible, responden las sugerencias (FAQ).** `CONTEXT.md` §9.2 exige caer a respuestas predefinidas. Con `ai_assistant_enabled` apagado (o un fallo `DISABLED`/`NOT_CONFIGURED`) la pantalla lo avisa, deshabilita el envío libre y las tres sugerencias del diseño contestan con datos reales: tareas de mañana, resumen de asistencia del mes y los pasos reales para justificar una falta ("Faltas → Justificar → motivo → adjunto → Enviar justificante"). Sin hijos o sin datos, la respuesta dice "sin registro"; no se inventa un alumno.

**S5 — Nada nuevo que el diseño no tenga.** Se mantienen la bienvenida, las tres sugerencias, las burbujas, el composer y los íconos del mockup. La tarjeta "Ver tarea" del diseño se conecta sólo cuando la app conoce las tareas reales (sugerencia de tareas), y abre `ParentTaskDetail`; una respuesta libre del modelo no inventa tarjetas. Los pulgares quedan como parte del diseño, sin acción: no hay backend de feedback. El micrófono tampoco graba (fuera de alcance, igual que en 3.7).

**S6 — Límites del free tier respetados.** El botón se bloquea mientras hay una consulta en curso, el `429 RESOURCE_EXHAUSTED` se traduce al mensaje de `AiFailure.RATE_LIMITED` sin reintentos en loop, y el caso de uso conserva el techo de 60 s que garantiza que la pantalla termine en mensaje y no en spinner.

**S7 — Sin cambios de reglas, colecciones, índices ni Room.** El asistente lee con las reglas publicadas del tutor; no escribe nada. `AiAssistantDataSource` sólo gana la instrucción de sistema en la creación del modelo. La base sigue en `version = 4`.

**S8 — Cierre medible.** `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` verdes, con tests nuevos para el prompt (datos presentes, ids ausentes, "sin registro"), el conteo de 30 días, el fallback FAQ, la bienvenida y el ViewModel (respuesta, fallo tipado, modo FAQ, tarjetas de tareas). La prueba funcional con la cuenta real la hace el equipo: no hay dispositivo conectado.

## Tasks

| ID | Spec | Tarea | Ruta | Estado |
|---|---|---|---|---|
| T1 | S2 | `AssistantContext`, `AssistantChild`, `assistantAttendanceCounts` y fechas ISO del contexto | `domain/model/AssistantContext.kt` | ✅ |
| T2 | S3 | `ASSISTANT_SYSTEM_INSTRUCTION` y `assistantPrompt(context, question)` | `domain/model/AssistantPrompt.kt` | ✅ |
| T3 | S2, S7 | `LoadAssistantContextUseCase` con mes en curso + mes anterior y lecturas fallidas cerradas | `domain/usecase/LoadAssistantContextUseCase.kt` | ✅ |
| T4 | S1, S3 | El caso de uso arma el prompt, conserva flag/analytics/timeout y expone `isAvailable()` | `domain/usecase/AskAssistantUseCase.kt` | ✅ |
| T5 | S3, S7 | Instrucción de sistema en la creación del `GenerativeModel` | `data/remote/ai/AiAssistantDataSource.kt`, `di/AppContainer.kt` | ✅ |
| T6 | S1, S4, S5 | `ParentAIUi`, bienvenida, sugerencias, FAQ y tarjetas de tareas | `ui/screens/parent/ParentAIUi.kt` | ✅ |
| T7 | S1, S4, S6 | Estado, contexto, envío, fallos tipados y modo FAQ | `ui/screens/parent/ParentAIViewModel.kt` | ✅ |
| T8 | S1, S5 | Pantalla conectada sobre el diseño aprobado (bienvenida, chips, burbujas, composer, tarjetas) | `ui/screens/parent/ParentAIScreen.kt` | ✅ |
| T9 | S1, S5 | Factory del ViewModel y ruta con apertura del detalle de tarea | `di/AppContainer.kt`, `navigation/AulaSeguraNavHost.kt` | ✅ |
| T10 | S8 | Tests de prompt, conteo, FAQ, bienvenida y ViewModel | `domain/model/AssistantPromptTest.kt`, `domain/usecase/LoadAssistantContextUseCaseTest.kt`, `ui/screens/parent/ParentAIUiTest.kt`, `ui/screens/parent/ParentAIViewModelTest.kt`, `domain/usecase/AskAssistantUseCaseTest.kt` | ✅ (4 suites nuevas, 27 tests) |

## Log

**L1 — 19 nov 2026, pedido del dueño.** «dejalo lo de tutores por el momento asi ya que en un futuro pienso hacer un cambio importante, pasemos a 3.8 asistente con ia logic». El alta de tutores desde el panel del directivo queda fuera, a propósito, porque el dueño planea un cambio de fondo ahí.

**L2 — 20 nov 2026, decisiones de ingeniería al implementar.**

- **El contexto lo arma un caso de uso, no el ViewModel.** `LoadAssistantContextUseCase` combina `loadHome()`, `loadNotices()` e `loadIncidents()` — las mismas lecturas de las pantallas de la familia — y sólo agrega el mes anterior cuando la ventana de 30 días lo cruza. Reusar el contador del mes en curso habría dicho "30 días" mostrando 19; el tutor puede contrastarlo con el calendario.
- **Las lecturas fallidas cierran el contexto entero.** Un contexto parcial haría que el modelo respondiera "sin avisos" cuando la verdad es "no se pudieron leer los avisos". La pantalla muestra el motivo (`homeFailureMessage`) y ofrece reintentar.
- **El prompt es puro y testeable.** `assistantPrompt(context, question)` es la única frontera de lo que sale del dispositivo: no escribe `taskId`, `studentId` ni `groupId`, y un test lee el texto para fijarlo. La instrucción de sistema viaja con el modelo (`generativeModel(systemInstruction = ...)`), no con la pregunta, porque lo que va en el turno del usuario se puede discutir ("ignora lo anterior").
- **El FAQ es determinista y usa los mismos datos.** Con el flag apagado, o con `DISABLED`/`NOT_CONFIGURED`, las tres sugerencias responden sin modelo: tareas de mañana, resumen del mes y los pasos reales del flujo de justificar. El texto libre se bloquea con un aviso en vez de inventar una respuesta. Un `RATE_LIMITED` no degrada la sesión: es un aviso y el texto vuelve al composer.
- **La tarjeta "Ver tarea" sólo existe con datos reales.** Los chips de tareas producen tarjetas con `taskId` verdaderos y abren `ParentTaskDetail`; una respuesta libre del modelo nunca inventa una tarjeta, porque el modelo no devuelve referencias estructuradas.
- **Sin ids ni datos de otros alumnos.** El contexto no lleva uid, `studentId` ni `groupId`; el grupo viaja como etiqueta humana ("4° D"). Los ids de tarea se quedan en la app.
- **Sin cambios de reglas, colecciones, índices ni Room** (`version = 4`). `AiAssistantDataSource` sólo ganó la instrucción de sistema al crear el modelo.

**L3 — 20 nov 2026, cierre y prueba del dueño.**

- **Tests (4 suites nuevas, 27 tests; total 72 suites / 532 tests / 0 fallos):** `AssistantPromptTest` (6) — datos reales, ids ausentes, "sin registro", contexto nulo, conteo de la ventana, ISO independiente del idioma; `LoadAssistantContextUseCaseTest` (5) — ventana que cruza al mes anterior, ventana que arranca el día 1, avisos fallidos, tutor sin hijos, topes del prompt; `ParentAIUiTest` (8) — bienvenida con y sin nombre, tareas de mañana, sin tareas, contadores del mes, pasos de justificar, contexto nulo, tarjetas sólo en la sugerencia de tareas; `ParentAIViewModelTest` (8) — carga, envío con contexto, tarjetas, fallo que conserva el borrador, modo FAQ, texto libre bloqueado, lectura fallida, tutor sin hijos. `AskAssistantUseCaseTest` se ajustó al prompt renderizado.
- **Verificación:** `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**, 72 suites / 532 tests / 0 fallos, `app-debug.apk` generado.
- **Probado en dispositivo por el dueño (20 nov):** el asistente responde de forma correcta.
- **App Check en desarrollo:** el dueño registró el token de depuración del dispositivo de pruebas en la consola. Sin ese registro AI Logic rechaza la petición (fallo de configuración, no del cliente); queda anotado en `CONTEXT.md` §9.2.
- **Pendiente:** probar el camino de respaldo apagando `ai_assistant_enabled` en Remote Config (las tres sugerencias deben responder con datos reales) y el detalle de tarea abierto desde la tarjeta.
