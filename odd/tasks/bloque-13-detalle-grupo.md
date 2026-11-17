# Bloque 13 — Detalle de grupo

Branch `main` · build `./gradlew.bat assembleDebug` · tests `./gradlew.bat testDebugUnitTest`.

## Specs

**S1** — Fuente: `odd/tasks/handoff.md`, fila B13. `Una sola pantalla con tres pestañas (Resumen / Asistencia / Incidentes) para TeacherGroupDetail; hoy es PlaceholderScreen y navega con "3B" fijo.`

**S2** — Diseño: los tres HTML de Stitch entregados por el dueño — "Detalle del Grupo", "Detalle de Grupo (Docente - Asistencia)" y "Detalle de Grupo (Docente - Incidentes)" — son la misma pantalla con la pestaña activa distinta. `Convertirlos para que se usen en el proyecto en Android Studio, no cambiar nada; si alguna pantalla usa algo que en el proyecto no está, quitarlo; incluir sus animaciones. Traer lo que de verdad tenemos en el proyecto, no botones ni cosas innecesarias.`

## Tasks

**T1** — S1 — **hecho** — `TeacherGroupDetailScreen` real en la ruta `teacher/groups/{groupId}`; el `PlaceholderScreen` y el `"3B"` fijo de esa ruta se fueron.
**T2** — S1 — **hecho** — `TeacherGroupDetailViewModel` lee los datos cacheados: grupos, roster del grupo, listas cerradas y incidentes; `open(groupId)` toma el id de la ruta.
**T3** — S1 — **hecho** — `IncidentSummary` gana `groupId` para que la pestaña de incidentes filtre por grupo en la propia pantalla y no dependa de que el llamador ya haya filtrado.
**T4** — S2 — **hecho** — Hero índigo con emblema al 6 %, tarjeta del grupo, métricas, acciones rápidas, tabs animadas, calendario de asistencia con leyenda, últimas listas con chips y estado de sincronización, tarjetas de incidente expandibles y CTA "Pasar lista".
**T5** — S2 — **hecho** — Se quitaron las piezas del mockup que el proyecto no tiene (detalle abajo).
**T6** — S1 — **hecho** — 13 tests puros en `TeacherGroupDetailUiTest`: calendario, porcentajes, faltas de hoy, filtro de incidentes, búsqueda, listas del mes, slot del día y etiqueta de sincronización.

## Log

**L1** — Decisión: una pantalla con tres pestañas, no tres rutas. Los tres HTML comparten hero, métricas y acciones; separarlos habría duplicado la carga del grupo y el docente perdería el contexto al cambiar de pestaña.

**L2** — Decisión: el calendario es de lunes a viernes, como el mockup, y cada celda pinta el porcentaje real de la sesión de ese día; un día sin lista dice `--`, no `0%`. Los umbrales de color son exactamente la leyenda (≥95 %, 85–94 %, <85 %).

**L3** — Decisión: "Asistencia" promedia `presentes + retardos` sobre lo marcado, así que un justificado no cuenta como asistencia pero un retardo sí. "Faltas hoy" sale de la sesión de hoy y, si no hay lista, muestra `—`: un `0` diría que nadie faltó.

**L4** — Decisión: los incidentes salen de los borradores de Room (los que el docente reportó) filtrados por `groupId`, no de una consulta nueva a Firestore. Abrir la pestaña no cuesta lecturas y funciona sin señal; el refresco sigue siendo el del historial.

**L5** — Decisión: "Registrar incidente" abre el formulario real. El formulario elige el alumno y de ahí deriva el grupo, así que no se inventó una ruta con grupo fijo ni un preseleccionado a medias.

**L6** — Quitado del mockup, con motivo:
- Barra de estado simulada (08:15, wifi, batería): la pone Android.
- "Aula 12": no existe en el esquema; el horario no guarda salón.
- Botones de calendario y ajustes del hero, `more_vert` y avatar: no llevan a ninguna pantalla del docente.
- Chat por alumno y porcentaje por alumno: el docente no abre conversación desde un alumno ni hay asistencia por alumno cargada en esta pantalla.
- "AulaBot · Resumen": texto que nadie generó.
- Barra inferior "Iniciar Asistencia Rápida": duplicaba el CTA "Pasar lista" y no existía como flujo.
- Miniatura de video con duración: la evidencia se guarda como foto; mostrar un `0:24` sería inventar un archivo.

**L7** — Animaciones portadas: pulso del punto de "Lista pendiente", `Reveal` escalonado de las zonas, escala al presionar (`rememberPressScale`), color animado del segmento activo, `Crossfade` al cambiar de pestaña y `animateContentSize` al expandir un incidente.

**L8** — 18 nov 2026: `assembleDebug` verde y **274 tests / 0 fallos / 0 errores** (eran 261; +13). **No se probó en dispositivo**: no hay teléfono en este entorno. Para verlo: entrar a Mis grupos → "Ver alumnos"; necesita grupos y roster cacheados (se llenan al abrir Mis grupos) y, para el calendario, que existan listas guardadas de ese grupo.
