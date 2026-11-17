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

**T1** — S1,S6 — inline — pendiente — Horario real: modelo de bloque horario, lectura de `groups/{groupId}/schedule`, selección de la próxima clase según día y hora.
**T2** — S1,S6 — inline — pendiente — Card hero con hora, materia, grupo y alumnos reales; estado vacío cuando no hay horario.
**T3** — S2,S7 — inline — pendiente — "Resumen de hoy": listas de hoy, faltas de hoy y pendientes de sincronizar con datos reales.
**T4** — S3,S8 — inline — pendiente — "Actividad reciente": combinación de asistencia, incidentes, tareas y avisos del docente.
**T5** — S4,S9 — inline — pendiente — Campanita con el conteo real de no leídas y badge oculto en cero.
**T6** — S1..S9 — inline — pendiente — Tests, documentación en `FASE1.md`, verificación y commit.

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
