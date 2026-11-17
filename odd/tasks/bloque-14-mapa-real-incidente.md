# Bloque 14 — Mapa real en el registro de incidente

Branch `main` · build `./gradlew.bat assembleDebug` · tests `./gradlew.bat testDebugUnitTest`.

## Specs

**S1** — Fuente: `odd/tasks/handoff.md`, fila B14. `Reemplazar el Canvas de TeacherIncidentNewScreen por CampusMap (Google Maps ya está en el proyecto), pin real y guardar coordenadas reales en IncidentLocation; hoy las coordenadas salen de la fracción del dibujo (BASE_LAT/BASE_LON).`

**S2** — Hallazgo al abrir el bloque: el chip de la tarjeta decía **siempre** "Dentro del plantel · Verificada", aunque el pin estuviera a kilómetros del plantel, y el botón "Usar mi ubicación actual" regresaba el marcador a una fracción del dibujo. Las dos eran respuestas inventadas sobre dónde ocurrió el incidente.

## Tasks

**T1** — S1 — **hecho** — `CampusMap` acepta `pin` opcional, `onPinChange` (tocar el mapa mueve el pin) y `scrollGesturesEnabled`. El pase de lista lo sigue usando sin pin y con gestos, así que no cambia.
**T2** — S1 — **hecho** — `TeacherIncidentNewScreen` usa `CampusMap` en lugar del dibujo vectorial; se borraron `drawCampusMap`, `IncidentTeardrop`, `MARKER_X/Y` y `BASE_LAT/BASE_LON`.
**T3** — S1 — **hecho** — El pin se siembra con la lectura real del GPS (`rememberCurrentLocation`) y, si no hay lectura, con el centro real de `schools.location`. `IncidentLocation` guarda esas coordenadas.
**T4** — S2 — **hecho** — "Usar mi ubicación actual" usa el fix real; el chip sale de `incidentPinVerdict` (dentro / fuera con distancia / sin geocerca / sin pin) y la geocerca se pinta roja con el pin fuera.
**T5** — S1..S2 — **hecho** — 4 tests nuevos en `TeacherIncidentUiTest`; build y commit.

## Log

**L1** — Decisión: el pin nunca se inventa. Orden de siembra: fix del GPS → centro del plantel cacheado → sin pin (la tarjeta explica por qué y deja el botón de ubicación). Cualquier coordenada de ejemplo habría vuelto a mandar el incidente a un patio que no es el del hecho.

**L2** — Decisión: el pin se mueve **tocando** el mapa, no arrastrándolo. El formulario tiene scroll y un marcador arrastrable se pelea con el desplazamiento; el texto de ayuda cambió a "Toca el mapa para mover el marcador a donde ocurrió". Si algún día se quiere arrastre, hay que sacar el mapa del scroll o abrirlo en pantalla completa.

**L3** — Decisión: dentro del formulario el mapa se dibuja con `scrollGesturesEnabled = false`; el toque sigue funcionando porque no es un gesto. Así el docente puede seguir bajando al resto del formulario sin quedar atrapado en la tarjeta.

**L4** — Decisión: `accuracyMeters` solo viaja cuando el pin salió de una lectura real del GPS; un punto tocado se guarda con `accuracy = null`. `verified` es `distancia <= schools.radiusMeters`, calculada con `haversineMeters`, y es la misma condición que pinta el chip: la pantalla no puede decir "Verificada" y el documento guardar `verified = false`.

**L5** — Trampa: `rememberUpdatedMarkerState` no existe en `maps-compose` 6.1.2 (error de compilación). `rememberMarkerState` solo usa `position` al inicializar, así que el marcador del pin se crea con `key` = coordenada para que cada toque se pinte en el lugar nuevo.

**L6** — 18 nov 2026: `assembleDebug` verde y **261 tests / 0 fallos / 0 errores** (eran 257; +4). **No se probó en dispositivo**: este entorno no tiene teléfono ni GPS. Para verlo hay que correrlo en el teléfono con permiso de ubicación concedido y `MAPS_API_KEY` en `local.properties`; el centro del plantel y el radio se cambian solo desde la consola de Firestore (`schools/{schoolId}`).
