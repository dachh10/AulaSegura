# Dificultades encontradas y soluciones

Fecha: 2026-11-25. Base: `main` @ `0f5e016`.

Este documento resume los problemas detectados durante la verificación del proyecto y
cómo se resolvieron. Se armó a partir del historial de commits y de
`correcciones-pendientes.md`; cada dificultad se apoya en una causa confirmada con
evidencia en código, un log o un dry-run, no en una corazonada. Los commits citados
están en `main`.

## 1. IDs de grupo no canónicos y datos duplicados

**Dificultad.** "Crear grupo" desde el panel usaba un ID aleatorio de Firestore, mientras
que la asignación y el QR usaban `grp-{grado}{letra}`. El resultado: el mismo grado+sección
existía dos veces, un documento con el docente y otro con los alumnos. Además, en la
aprobación por QR una clave con punto dentro de `set()` se guardaba literalmente como
`subjectTeachers.g5-espanol-v`, dejando `subjectTeachers` aparentemente vacío y
permitiendo que dos docentes tomaran la misma materia.

**Solución.** Usar `groupIdFor()` con un `merge` que respeta los campos calculados, haciendo
idempotente crear el mismo salón dos veces; y escribir un mapa anidado real en vez de una
clave con punto. Commit `e28ee48`, verificado en dispositivo (`aa2706d`).

**Lección.** Dos vocabularios de ID para la misma entidad rompen consultas y reportería.
Y un `set()` con claves punteadas no crea estructura anidada en Firestore.

## 2. Asignación de grupos y pérdida de docentes

**Dificultad.** Al aprobar a un alumno, el docente aprobado en el registro desaparecía
de sus grupos. `assignGroup()` hacía un `merge()` del documento de grupo nuevo dentro de
un grupo existente; como la plantilla trae `teacherIds` vacío, sobrescribía a los
docentes ya asignados. El docente dejaba de aparecer en `observeMyGroups()` hasta que la
directora lo reasignaba a mano.

**Solución.** Escribir `teacherIds` solo cuando el grupo es realmente nuevo y
incrementar `studentCount` en ambos caminos. La decisión se extrajo a una función pura y
se cubrió con tres tests JVM. Commit `2cc1295`.

**Dificultad (segunda capa).** El mismo bug seguía vivo en el camino que la directora
usa de verdad: la aprobación por QR hacía el mismo merge, así que el fix anterior no la
cubría.

**Solución.** Compartir un único plan de escritura entre la aprobación online y la
offline/replay, y excluir `teacherIds` del merge de un grupo existente. Dos tests fijan
ambos caminos. Commit `4799b7e`.

**Lección.** Un fix de un único punto de entrada no arregla un flujo: había dos caminos
de aprobación con la misma escritura. Buscar todas las call sites antes de declarar un
problema cerrado.

## 3. Materia global en vez de materia por grupo

**Dificultad.** `users.subjects` era una lista global que se sobrescribía en cada
asignación: asignar a un docente a otro grupo cambiaba la materia que dictaba en todos
lados. El formulario de nueva tarea armaba las opciones una sola vez desde esa lista,
así que un docente con dos grupos veía ambas materias incluso después de elegir uno.

**Solución.** `subjectTeachers` de cada grupo pasó a ser la fuente de verdad: la directora
elige una materia libre del grado al asignar, desasignar la libera, y el perfil es una
proyección de todos los puestos con el grupo entre paréntesis. Pase de lista, tareas,
tarjetas de grupo y perfil leen la materia del grupo concreto; la lista global queda solo
como fallback para registros viejos. Se reconstruyó `users.subjects` en Firestore desde
los puestos actuales. Commits `e26f3a4` y `bff1499`.

**Dificultad (efecto colateral).** La proyección de materias del docente se leyó con una
consulta `groups where teacherIds array_contains uid`. Las reglas de Firestore no pueden
probar esa query, así que el listener fue rechazado con `PERMISSION_DENIED` y la
excepción no atrapada **mataba la app al iniciar sesión**.

**Solución.** Leer cada documento de grupo por los IDs que ya vienen en el perfil, y hacer
que un fallo de la proyección nunca bloquee la sesión. Commit `959f88c`.

**Lección.** Una consulta que las reglas no pueden autorizar es un crash de startup, no
un simple error de pantalla. Nunca dejar que una proyección decorativa tumbe la sesión.

## 4. Justificaciones: el dato se escribía pero nadie lo leía

**Dificultad.** El tutor podía enviar una justificación: se guardaba en disco, el worker
subía la foto a Cloudinary y escribía `justifications/{uuid}` con `status: "pendiente"`.
Pero no existía ninguna pantalla que consultara esa colección. El comentario en el
código lo decía explícitamente: *"a justification is write only — no tutor screen reads
one back"*. No era un problema de notificación ni de sincronización: la funcionalidad
no existía.

**Solución (por etapas).**

1. Bandeja de lectura de la directora leyendo por `schoolId`, con tarjeta compartida y
   aprobación/rechazo que actualiza solo los cuatro campos permitidos por
   `firestore.rules` (`status`, `reviewedBy`, `reviewedAt`, `reviewNote`). Commit `8713958`.
2. Efecto en asistencia: en un único `WriteBatch`, actualizar la justificación y pasar
   el registro a `justified`, recalculando `counts` con la lógica existente. La lectura
   del documento se hace desde `Source.SERVER` y sesiones viejas se resuelven por
   `groupId` + fecha, limitadas a la escuela. Commits `eb3d4d9` y `be2233c`.
3. Foto obligatoria para todo motivo excepto "Asunto familiar", tanto en `canSend()`
   como en `send()`, con error explícito. Commit `0fc25a9`.
4. La decisión pasó al **docente dueño de la sesión donde se marcó la falta**; la directora
   conserva solo lectura supervisora, sin botón de decisión ni entrada en el menú. El
   repositorio verifica al dueño de la sesión antes del batch para devolver `DENIED` en
   vez de un `PERMISSION_DENIED` a mitad de la escritura. Commit `cc30a9c`.
5. En un grupo con varias materias, un aviso llegaba a **todos** los docentes del grupo.
   El formulario ahora lista todas las faltas del día con su materia, permite elegir una
   o aplicar el mismo informe a todas, y preselecciona la sesión tocada. Cada falta
   genera su propio documento con `teacherId`, `subjectId`, `subject` y `sessionId`, así
   que cada docente decide solo su informe. Los documentos viejos sin destinatario siguen
   visibles por un fallback de grupo y ninguno se borra ni se reescribe. Se desplegaron
   las reglas y el índice compuesto `(groupId, teacherId)`. Cinco tests JVM. Commit
   `4865cd0`.

**Dificultad (diagnóstico).** La bandeja del docente aparecía vacía. Se sospechó de dos
vocabularios de ID de grupo (`grp-{grado}{letra}` vs. IDs aleatorios de "Crear grupo").
El script de solo lectura `tools/seed/repair-group-ids.mjs` confirmó que los datos ya
eran canónicos: no había nada que reparar. La causa real era que no había lector, no un
problema de datos. Commit `f3aae73`.

**Lección.** Antes de escribir un script de reparación de datos, verificar con un
dry-run. Y antes de culpar a la sincronización, buscar si existe un lector.

## 5. Asistencia del tutor: "No se pudo cargar" con datos ausentes

**Dificultad.** Un tutor recién vinculado veía un error de carga en Inicio y Asistencia.
Los datos no estaban rotos: ese alumno tenía sesiones de pase de lista en el grupo pero
cero registros propios en esas sesiones. `readRecord()` tragaba todas las excepciones y
devolvía `null`, y `loadMonth()` interpretaba "todos null" como fallo de carga.

**Solución.** Distinguir `Missing` (mes vacío) de `Failed` (motivo real: denegado o sin
conexión), y mostrar el motivo cuando la lectura realmente falla. Cubierto por
`ParentRecordReadTest`, commit `e1b11c4`.

**Lección.** Un `catch` que devuelve `null` destruye información: convierte "no hay
registro" en "no se pudo leer", dos estados que la UI debe trinar distinto.

**Dificultad (nueva).** Un docente podía reabrir un grupo y guardar infinitas sesiones
de asistencia para el mismo día. La tarjeta del tutor ya mostraba solo la primera, pero
las demás se escribían igual.

**Solución.** `StartRollCallUseCase` consulta si ya existe sesión antes de abrir la
pantalla y devuelve `AlreadyTaken` sin crear borrador ni registrar el evento;
`SaveRollCallUseCase` vuelve a comprobar antes de escribir, para que dos pantallas abiertas
antes del primer guardado no cometan ambas. La clave busca grupo, fecha, docente y la
etiqueta durable de materia, así que las sesiones viejas sin ID canónico también bloquean,
mientras otros docentes y otras materias del mismo grupo siguen siendo independientes.
Seis tests JVM. Commit `7abfd38`.

## 6. Docente rechazado: bucle de navegación y cuenta atrapada

**Dificultad 1 — bucle infinito.** La pantalla "Solicitud rechazada" se reiniciaba
constantemente (el scroll volvía arriba y "Cerrar sesión" era casi imposible de tocar).
La cadena completa: `LaunchedEffect(Unit)` al entrar → `refreshState()` seteaba
`proceed = true` también para `estado == "rechazado"` → `LaunchedEffect(ui.proceed)`
navegaba con `popUpTo` → `nextRoute()` para un docente rechazado devolvía de nuevo
`account_review` → se recreaba el composable y empezaba otra vez.

**Solución.** Para `UserRole.TEACHER`, `proceed = user.estado == "aprobado"`; un rechazo
nunca habilita navegación. Además se evita el auto-refresh al entrar cuando ya está en
estado rechazado. Commit `ebbee55`.

**Dificultad 2 — cuenta atrapada.** Incluso con el bucle corregido, un rechazo por error
dejaba la cuenta de Auth y el `users/{uid}` con `estado: rechazado`: el docente no podía
registrarse de nuevo (*"Ya existe una cuenta con ese correo"*) ni entrar.

**Solución.** Botón "Volver a enviar solicitud": reabre el formulario para elegir grado,
grupo y materia y devuelve el perfil a `pendiente` sin crear ni borrar cuentas de Auth.
La regla de Firestore permite ese único write sobre el propio documento y restringe los
campos a `estado`, `requestedGroupId`, `requestedSubject`, `subjects` y `updatedAt`,
manteniendo identidad, escuela y `active` intactos. Reglas desplegadas. Commit `a6e8d1a`.

**Lección.** Un síntoma visible (el bucle) puede esconder un problema de producto más
profundo (no hay camino de recuperación). Arreglar el primero sin el segundo deja al
usuario igual de atrapado.

## 7. Aviso a un grupo específico

**Dificultad.** Al publicar un aviso desde el detalle de un grupo, aparecía "Ese grupo
ya no está en tu lista", aunque el grupo estuviera visible. "Todos mis grupos" sí
funcionaba. La ruta pasaba el ID directamente a la preselección; si ese ID estaba
vencido, `publish()` lo validaba contra un roster recién leído y devolvía `GroupNotMine`,
mientras "Todos mis grupos" recalculaba la selección en el momento del clic.

**Solución.** Validar la preselección contra los grupos vigentes del docente y fijar
ambos casos con tests. No se debilitó la validación de propiedad del grupo, porque es
una regla de seguridad. Commit `a932dfc`.

**Lección.** No arreglar validaciones de seguridad relajándolas: hay que corregir el
dato de entrada (la preselección), no la comprobación.

## 8. Botón "Usar mi ubicación actual" inerte

**Dificultad.** Con permiso concedido pero sin fix de GPS disponible, el botón hacía
una tentativa silenciosa y terminaba. Para el docente parecía un botón muerto: no había
estado de "Ubicando…" ni mensaje de error.

**Solución.** Estado `failed` en `LocationUiState` cuando una lectura con permiso no
devuelve nada. La decisión del click vive en `locationButtonUi()`, función pura: sin
permiso pide permiso, con fix lo aplica, durante la lectura muestra "Ubicando…" con el
botón deshabilitado, y si falla muestra "Reintentar ubicación" con un mensaje concreto.
El handler y la UI consumen la misma decisión, así que no puede haber etiqueta sin
acción. Geocerca y guardado del incidente intactos. Cubierto por 6 tests JVM. Commit
`8d7ff74`.

**Dificultad (nueva).** "Registrar incidente" esperaba hasta 8 segundos por un fix fresco
de GPS al entrar, retrasando la apertura del formulario.

**Solución.** Empezar desde la última posición conocida y pedir un fix preciso solo cuando
el docente toca el botón de ubicación. Las pantallas de geocerca conservan el fix fresco
al entrar. Commit `ce1de5b`.

**Excepción documentada.** No hay runner determinista para el estado Compose ni para el
GPS real en la suite unitaria; queda pendiente la verificación en dispositivo con GPS
activado y desactivado.

## 9. Valores falsos y controles inertes en la UI

**Dificultad.** Varias pantallas mostraban datos inventados: contador de "Alumnos sin
grupo" fijo en `0`, secciones E y F que no existen en el catálogo, "2 nuevos" en avisos
del tutor, el chip del hijo mostrando el ID `grp-3b` cuando fallaba el nombre, "3 grupos"
en la tarjeta del inicio del docente y la vista previa del aviso con
"Prof. Salinas · 3° B" hardcodeado.

**Solución.** Leer los datos reales de los repositorios, usar el catálogo
(`AvailableSections`, A–D) y extraer helpers puros: `displayGroupLabel()` con fallback
nombre → etiqueta canónica derivada del ID → ID como último recurso, y etiquetas de
audiencia con fallback neutro. Commits `170cddd` y `fa99130`.

**Dificultad (nueva).** En Instalación, los chips de filtro y los chips de materia eran
hermanos dentro de un `Box` de `Reveal`, que los apila en el mismo origen: la fila de
materias tapaba los filtros. En otras pantallas, `Row`s fijas hacían que las etiquetas
largas se superpusieran y que el grado 6 se fuera de pantalla.

**Solución.** Envolver ambos `FlowRow` en una `Column` para que se apilen, y reemplazar
las `Row` fijas por `FlowRow` con etiquetas elipsadas. Commits `ce1de5b` y `4c1c07a`.
Además se retiraron controles que no hacían nada (escudo de chat duplicado, iconos de
ayuda/overflow, tarjetas de notificaciones deshabilitadas, botones de cámara del chat,
menú/mic de IA, login falso de Google) y se agregaron los grados 4–6 a los filtros. Commit
`13ea19f`.

**Dificultad (nueva).** El inicio del docente leía los grupos solo de la caché de Room y
nunca los refrescaba: después de instalar, mostraba "Sin grupos" hasta visitar la
pantalla de Grupos y volver. También consultaba justificaciones antes de tener los IDs
de grupo.

**Solución.** El ViewModel trae el roster de Firestore en `init` y solo entonces cuenta
los grupos y carga la insignia de informes. Un fallo de refresh queda silencioso porque
la caché ya responde y la pantalla de Grupos muestra su propio error. Commit `0f5e016`.

**Lección.** Un número falso es peor que un número ausente: el usuario toma decisiones
basadas en él. Y un control que no hace nada es peor que no tenerlo.

## 10. Perfil del docente: ID de escuela en vez del nombre

**Dificultad.** El perfil mostraba `users.schoolId` (`sch-benito-juarez`) sin importar lo
que la directora hubiera guardado en "Configuración del plantel".

**Solución.** Observar `SchoolRepository.observeSchool()` y renderizar `SchoolSite.name`,
con el ID solo como fallback legacy. Se refresca la escuela al entrar para que un nombre
recién guardado aparezca sin pasar por otra pantalla. Tres tests JVM fijan la precedencia
de la etiqueta. Commit `291cd80`.

## 11. App Check rechazando la atestación

**Dificultad.** El asistente IA fallaba por un rechazo de atestación de App Check, que
se manifestaba como un error de la funcionalidad y no como un problema de configuración.

**Solución.** Tratar el rechazo de atestación como problema de setup, con un mensaje que
apunte a la configuración. Commit `05d205b`.

## 12. Patrón que se repitió en casi todos los casos

1. **Confirmar la causa antes de tocar código.** Cada arreglo se apoyó en `archivo:línea`,
   un log o un dry-run; los diagnósticos que quedaron como hipótesis se marcaron como
   tales en vez de arreglarse a ciegas.
2. **Extraer la decisión a una función pura** y testearla con JVM (no hay emulador ni
   tests de instrumentación en la suite).
3. **Un cambio de comportamiento lleva su test.** Cuando no había runner determinista
   (Compose, GPS real, `WriteBatch` de Firestore), se documentó la excepción y se hizo
   verificación estructural.
4. **Cerrar todas las call sites**, no solo el punto donde se vio el síntoma (casos 2, 3 y 7).
5. **No debilitar reglas de seguridad** para que un flujo "funcione": corregir la entrada
   y verificar la identidad antes de escribir.
6. **Diferenciar "no hay dato" de "no se pudo leer"**: varios bugs vinieron de aplanar
   ambos estados en `null`.

## Estado final

Los cinco ítems de la guía de arreglos (D1–D5) están cerrados y verificados en dispositivo
según `correcciones-pendientes.md` y los commits `23f77c5`, `0fadfba`, `5bb06dd`. La suite
completa quedó en **129 suites / 799 tests / 0 fallos** (`0f5e016`). Queda pendiente solo
la verificación en dispositivo del estado del botón de ubicación con GPS activado y
desactivado, documentada como excepción en el punto 8.
