# Bloque 12 — Inicio del docente con datos reales

Branch `feat/docente-pase-lista` · entrega `ask-on-risk` (el corte de commits se decide con el diff real al cierre) · tests `./gradlew.bat testDebugUnitTest` · build `./gradlew.bat assembleDebug` · `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

Registro oficial del bloque: `FASE1.md` §Bloque 12. Este archivo es el logbook de ejecución.

## Specs

**S1** — Usuario: `1.-Enel inicio de docente la card que dice siguiente clase, hora, materia, grupo alumnos, pasar lista en realidad siemrpe se muestra igual, deme si eso se actualizara en algun momento posterior cuando se implemeneten las fases de directivo o tutor, o si es de nostros hacer que eso de verdad muestre informacion verdadera.`

**S2** — Usuario: `2.- Tambien en inicio las tres card de "resumen de hoy" siempre muestran lo mismo, nunca cambian y deberian de mostrar datos reales.`

**S3** — Usuario: `3.-"Actividad reciente", pasa lo mismo siempre muestra lo mismo nnca muestra datos de verdad reales.`

**S4** — Usuario: `7.- En la campanita de notificaciones siempre sale un numero dos, quita ese numero y que solo aparezca y vaya aumentando o dismunuyendo de acuerdo a las notificaciones que tengamos, si se abre una notificacion disminuira el numero ya que ya leimos esa notificacion.`

**S5** — Usuario: `Dale inicia con B12`

**S6** — La card "SIGUIENTE CLASE" muestra la próxima clase real del docente: el bloque horario de `groups/{groupId}/schedule/{slotId}` (weekday, startTime, endTime, subject) más el nombre real del grupo y su cantidad real de alumnos. Sin horario no se inventa nada: estado vacío honesto que diga que no hay clases programadas.

**S7** — "Resumen de hoy" muestra tres números reales: listas de hoy, faltas de hoy y pendientes de sincronizar.

**S8** — "Actividad reciente" lista datos reales (asistencia, incidentes, tareas y avisos del docente), ordenados por fecha, con la hora cuando es de hoy.

**S9** — La campanita de la barra superior muestra la cantidad real de notificaciones no leídas, oculta el badge cuando es 0, y baja al abrir una notificación.

## Tasks

**T1** — S1,S6 — inline — **hecho** — `ClassSlot` (weekday 1=lunes de `CONTEXT.md`), `ScheduleRepository` + `FirestoreScheduleRepository` (una lectura por grupo, timeout 8 s, `toRosterFailure` para el error) y `nextClassOf` (en curso gana a siguiente; slot sin hora legible se descarta).
**T2** — S1,S6 — inline — **hecho** — `TeacherHeroCard` recibe `NextClassUi?` + `scheduleNotice`; `SIGUIENTE CLASE · hh:mm` / `EN CURSO · hh:mm – hh:mm`, materia · grupo y alumnos reales. Sin horario: "No hay clases próximas" y el motivo debajo. "Pasar lista" navega al grupo de esa clase.
**T3** — S2,S7 — inline — **hecho** — `todaySummary`: listas de hoy por grupo distinto, faltas sumadas, `OutboxDao.observeCount()` vía `SyncQueueRepository`.
**T4** — S3,S8 — inline — **hecho** — `recentActivity` combina asistencia, incidentes, tareas y avisos, ordena por fecha y recorta a 4; etiqueta de hora hoy/ayer/fecha. Estado vacío propio.
**T5** — S4,S9 — inline — **hecho** — `unreadNotifications` del repositorio de notificaciones en `TeacherHomeUi`; el badge no se dibuja en 0 y baja al abrir una notificación (misma fuente que marca `read`).
**T6** — S1..S9 — inline — **hecho** — `TeacherHomeViewModel` + `TeacherHomeUi`, factory en `AppContainer`, cableado en el NavHost, 12 tests nuevos en `TeacherHomeUiTest`, build y commit.

## Log

**L1** — 17 nov 2026, pedido original (10 puntos de pantallas docente, verbatim):

```
1.-Enel inicio de docente la card que dice siguiente clase, hora, materia, grupo alumnos, pasar lista en realidad siemrpe se muestra igual, deme si eso se actualizara en algun momento posterior cuando se implemeneten las fases de directiva o tutor, o si es de nostros hacer que eso de verdad muestre informacion verdadera.
2.- Tambien en inicio las tres card de "resumen de hoy" siempre muestran lo mismo, nunca cambian y deberian de mostrar datos reales.
3.-"Actividad reciente", pasa lo mismo siempre muestra lo mismo nnca muestra datos de verdad reales.
4.- En registrar incidente su pantalla no muestra el mapa real de google maps con la ubicacion que hoy en dia usamos, solo muestra un mapa simple, no el mapa de verdad, cuando tendria que mostrar el mapa verdadero.
5.- En mis grupos cuando le doy click a ver alumnos se supone que deberia de entrar uan pantalla de detalle de grupo pero nunca agregamos esta pantalla, en el proyecto de stitch acabo de crear tres pantallas con los siguientes nombres, que son las que usaremos aqui:
Detalle de Grupo (Docente) - AulaSegura
Detalle de Grupo (Docente - Asistencia) - AulaSegura
Detalle de Grupo (Docente - Incidentes) - AulaSegura
6.- El pase de lista debe de realizarce cada dia de lunes a viernes necesito que, pongas un radio mayor a la ubicacion que usamos para que la ubicacion donde me encuentro ahora este dentro de ese radio y asi poder prbar que si se guarde la asistencia de manera correcta, y que aparezca correctamente en historial.
7.- En la campanita de notificaciones siempre sale un numero dos, quita ese numero y que solo aparezca y vaya aumentando o dismunuyendo de acuerdo a las notificaciones que tengamos, si se abre una notificacion disminuira el numero ya que ya leimos esa notificacion.
8.- En historial, por ejemplo en incidentes si adjunto una foto sale como 1 foto pero raro y no buen diseño, lo mismo pasa en listas en presente falta o retardo, algunos de esos numeritos hacen que se vea raro yas que no tienen buen espacio.
9.- Tambien para la parte de docente en perfil, si le das click a editar perfil te aparece una ventana muy chafa, acabo de crear un pantalla en stitch ia en el proyecto Auala segura para que te bases en ese diseño y crees algo asi, obvio ocupando datos que de verdad podamos ocupar, asi se llama la pantalla, Editar Perfil (Docente) - AulaSegura.
10.- Tambien en perfil de docente te permite cambiar de contraseña pero ahorita solo abre una ventana chafa necesito que copes el diseño de esta pantalla que igual acabo de crear en stitch Cambiar contraseña - AulaSegura.
```

**L2** — 17 nov 2026: el análisis de los 10 puntos quedó guardado en Engram (#534, topic `aulasegura/fase1/analisis-pantallas-docente`) y el usuario aprobó arrancar por B12 con `Dale inicia con B12`.

**L3** — Decisión de alcance: B12 cubre los puntos 1, 2, 3 y 7. Los puntos 4, 5, 6, 8, 9 y 10 quedan para B13-B17 y no se tocan en este bloque.

**L4** — Decisión: el horario existe en el esquema (`groups/{groupId}/schedule/{slotId}`) y las reglas dejan leerlo a cualquier docente activo de la escuela, pero **los documentos los crea el rol directivo** (que todavía no existe) y hoy no hay ninguno. Por eso la card tiene que tener un estado vacío honesto: si no hay horario, lo dice. Los documentos de prueba los crea el dueño en la consola de Firebase.

**L5** — Decisión: "Por sincronizar" sale del `outbox` de Room, que ya tiene consultas `COUNT(*)`; "Faltas hoy" y "Listas pasadas" salen del historial de asistencia (cache-first); "Actividad reciente" combina historial de asistencia, incidentes, tareas y avisos del propio docente.

**L6** — Decisión: el horario **no** se cachea en Room. Los documentos los escribe el rol directivo, que aún no existe, y una tabla nueva sería una migración para datos que pueden no llegar nunca. La lectura es una por grupo (no `collectionGroup`) porque las reglas autorizan `schedule` con un `get()` del grupo padre.

**L7** — Decisión: la card muestra la clase **en curso** si hay una. A las 08:10 el docente está frente a ese grupo; "Siguiente clase 10:00" lo mandaría al salón equivocado. Se distingue con `EN CURSO · inicio – fin`.

**L8** — Decisión: cuatro mensajes distintos en el hero vacío (`Aún no hay horario programado`, `No hay más clases hoy`, `Sin conexión para revisar el horario`, `No tienes acceso al horario del plantel`). Decir "no hay clases" cuando en realidad no se pudo leer sería mentirle al docente en la puerta.

**L9** — Decisión: `java.time` sigue sin usarse (`minSdk` 24 sin desugaring). El día de la semana se convierte desde `Calendar` con `((calendarDay + 5) % 7) + 1`, que es la única línea donde el esquema domingo-primero de Android se cruza con el lunes-primero del documento; tiene test propio porque un desfase de un día se ve plausible.

**L10** — 18 nov 2026: `assembleDebug` verde y **248 tests / 0 fallos / 0 errores** (eran 236; +12 de `TeacherHomeUiTest`). Un fallo durante la implementación: una aserción esperaba "Ayer" en la última fila de actividad cuando el límite de 4 ya había descartado el aviso de ayer; se corrigió la aserción y se probó `activityTimeLabel` por separado. No se probó en dispositivo.

**L11** — Datos para ver la card: `groups/{grp-3b|grp-3a|grp-2c|grp-1a}/schedule/slot-1` con `weekday` (1=lunes … 7=domingo), `startTime` `"08:00"`, `endTime` `"08:45"`, `subject`, `teacherId`. Sin esos documentos la card dice que no hay horario, que es el estado correcto.
