# Bloque 15 — Radio de prueba, lunes a viernes y mes real del historial

Branch `feat/docente-pase-lista` · build `./gradlew.bat assembleDebug` · tests `./gradlew.bat testDebugUnitTest`.

## Specs

**S1** — Usuario: `El pase de lista debe de realizarse cada dia de lunes a viernes necesito que, pongas un radio mayor a la ubicacion que usamos para que la ubicacion donde me encuentro ahora este dentro de ese radio y asi poder probar que si se guarde la asistencia de manera correcta, y que aparezca correctamente en historial.`

**S2** — Usuario: `Si que se bloque cuando sea con un msj "Hoy no hay clases"`.

**S3** — Hallazgo del análisis, aceptado en el plan: el selector de mes del historial decía "Noviembre 2026" fijo y sus flechas no hacían nada.

## Tasks

**T1** — S2 — **hecho** — `SchoolCalendar.isClassDay` (lunes a viernes) y `TeacherNoClassScreen` ("Hoy no hay clases", Volver / Ir al inicio).
**T2** — S2 — **hecho** — La ruta `teacher/roll-call/{groupId}` se rechaza antes de verificar ubicación: no se abre sesión nueva en fin de semana. Lo ya guardado no se toca.
**T3** — S3 — **hecho** — `monthOffset` en el ViewModel, `monthLabel`/`monthKey`/`monthKeyBefore`/`nextMonthOffset` puras, flechas reales (la derecha se deshabilita en el mes actual) y filtro por mes aplicado a las tres pestañas.
**T4** — S3 — **hecho** — Estados vacíos por mes ("No hay listas guardadas en este mes").
**T5** — S1 — **fuera de código** — El radio vive en `schools/{schoolId}.radiusMeters` y las reglas solo dejan escribirlo a un directivo; se cambia desde la consola de Firestore (ver abajo).
**T6** — S1..S3 — **hecho** — 9 tests nuevos (`SchoolCalendarTest`, `TeacherHistoryMonthTest`), build y commit.

## Log

**L1** — Decisión: el bloqueo va en la entrada del pase de lista y no dentro de la pantalla de lista. Una sesión iniciada en sábado sería una asistencia guardada que no corresponde a un día de clases; rechazar el arranque deja el historial intacto y el mensaje claro.

**L2** — Decisión: solo lunes a viernes, **sin feriados**. No existe colección de días inhábiles y crearla necesita las pantallas del directivo. `SchoolCalendar` es el único lugar que tendrá que enterarse cuando exista.

**L3** — Decisión: el mes del historial se compara como `año * 12 + mes` (un int). Evita `java.time` (`minSdk` 24 sin desugaring) y hace que "mismo mes" sea una igualdad en las tres listas.

**L4** — Decisión: el filtro de mes también aplica a Publicaciones. El encabezado es uno solo para las tres pestañas; que una lista muestre octubre y otra noviembre sería la pantalla contradiciéndose.

**L5** — Radio: `SchoolMappers` acepta `radiusMeters` entre 20 y 5000 m y cae al default de Remote Config (150) si falta o está fuera de rango. `evaluateGeofence` exige además `accuracy <= radius`, así que subir el radio relaja también esa condición. Para la prueba del dueño: en la consola, `schools/{schoolId}` → `location` = coordenadas reales del lugar donde está, `radiusMeters` = 500–1000. Con el centro en San Agustín (19.2826, -99.6557) ningún radio razonable alcanza otra ciudad.

**L6** — 18 nov 2026: `assembleDebug` verde y **257 tests / 0 fallos / 0 errores** (eran 248; +9). No se probó en dispositivo; el bloqueo de fin de semana no se puede ver sin cambiar la fecha del teléfono, y el radio depende de la consola.
