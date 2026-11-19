# Revisión de reglas de Firestore — 19 nov 2026

Motivo: cerrar los bloques 3.5 y 3.6 dejó dos cambios de reglas hechos a mano. Se revisaron contra las consultas reales del cliente, no contra lo que "debería" ser el modelo, y se endurecieron solo donde la app ya cumple el contrato.

Lo que **no** cambió: ninguna consulta del cliente. Las tres formas documentadas en `FirestoreParentRepository` (`whereArrayContains('tutorIds')`, `whereEqualTo('groupId')`, `whereArrayContains('groupIds')`, `whereEqualTo('studentId')`, `whereArrayContains('audience.groupIds')`) siguen siendo las mismas y siguen siendo lo que las reglas prueban.

## Se conserva de los commits anteriores

- **3.5 — rama del tutor en `notices` sin `audience.roles`.** Correcto y no se puede arreglar dentro de esa rama: la bandeja del tutor es **una** consulta por grupo, Firestore solo admite un `array-contains` por consulta y una condición que la consulta no puede probar niega la lectura completa. La restricción de rol se mueve al `create` (abajo), que es donde sí se puede exigir.
- **3.6 — `tutorOfStudent()` en `incidents`.** La lista del tutor filtra por `studentId`, así que la ruta del `get()` es constante y la regla prueba la consulta; la actualización de "Enterado" ya no depende de la copia desnormalizada `users.studentIds`.

## Cambios de esta revisión

**1. `users/{uid}` — edición del propio perfil: de lista negra a lista blanca.**

Antes la rama "uno mismo" solo prohibía los campos de identidad (`uid`, `email`, `role`, `active`, `schoolId`, `createdAt`). Eso dejaba escribibles `groupIds` y `studentIds`, que son justo lo que las reglas leen para decidir qué tareas, avisos y registros ve un tutor: un tutor podía agregarse el grupo de otro y leer su contenido, o apuntar a otro alumno en `justifications` y `records`.

Ahora la rama exige `changedKeys().hasOnly(selfEditableUserKeys())` — `firstName`, `lastName`, `fullName`, `phone`, `photoUrl`, `photoPublicId`, `subjects`, `preferences`, `updatedAt`. Es exactamente lo que escriben las pantallas que existen hoy (`profileUpdate` = `fullName` + `subjects`; `profilePhotoUpdate` = `photoUrl` + `photoPublicId`; el interruptor de avisos = `preferences`). Asignar grupos e hijos queda donde estaba: el directivo y el registro por código.

**2. `notices` — `create` exige audiencia de tutores.**

`request.resource.data.audience.roles.hasAny(['tutor'])`, más la comprobación de que `audience` es mapa y `roles` es lista. El panel del docente ya publica siempre a `['tutor']`, así que no cambia nada para la app; cierra el hueco de que un aviso dirigido solo a personal, con `audience.groupIds` del grupo, fuera **descargado** por todas las familias de ese grupo y ocultado únicamente por el cliente. La comunicación interna tiene su colección: `notifications`, un documento por destinatario.

**3. `tutorOfStudent()` — a prueba de documentos sin alumno.**

`studentId` es nulo en un incidente de grupo (`IncidentDraftEntity.studentId: String?`). Leer `.data` de un documento inexistente en reglas es un **error**, y un error niega el `allow` entero en vez de caer a la rama que sí autorizaba. Ahora la función exige `studentId is string`, no vacío, `exists(...)` y lee `tutorIds` con `get('tutorIds', [])`.

**4. `linkingCodes` — comentario, sin cambio de comportamiento.**

La consulta del directivo es `whereEqualTo('studentId', id)`. Agregarle `resource.data.schoolId == mySchool()` a `list` **rompería** "ver código": una regla de lista solo es válida si la consulta puede probarla, y esa consulta no filtra por escuela. Queda documentado para que nadie lo "arregle" y tire la pantalla.

## Límites que quedan, a propósito

- **El tutor lee todas las sesiones y los avisos del grupo de su hijo**, no solo los de su hijo. Es inevitable: el calendario se arma con las sesiones del grupo y la bandeja con un `array-contains` por grupo. Lo que sí está acotado es lo del menor: `records/{studentId}` solo si es su hijo, e `incidents` solo por `tutorIds`.
- **Un aviso solo para personal no existe como caso soportado.** Se rechaza en `create`; si algún día hace falta, hay que añadir el campo booleano que la consulta del tutor pueda filtrar, no relajar la regla.
- **`conversations` no restringe qué campos edita un participante** (`allow update` valida solo que `participants` no cambie). El chat real vive en RTDB; se endurece cuando el bloque 3.7 esté construido y se sepa qué escribe.
- **`linkingCodes` `get` está abierto a cualquier autenticado**: es el canje. No hay límite de intentos (no hay Cloud Functions en Spark).
- **Una sola escuela.** Varias reglas se apoyan en que `schoolId` es único en la práctica.

## Cómo se valida

No hay pruebas automáticas de reglas en el repo (no hay emulador ni `@firebase/rules-unit-testing`). La verificación es:

```bash
firebase deploy --only firestore:rules --project aulasegu
```

Y en el playground de la consola, con tokens reales, los cuatro casos que estas reglas deciden:

1. Tutor edita su propio `fullName` → permitido; tutor intenta escribir `groupIds` → denegado.
2. Docente publica un aviso con `audience.roles: ['tutor']` y su grupo → permitido; con `['docente']` → denegado.
3. Tutor abre la lista de incidentes de su hijo y pulsa "Enterado" → permitido; lo mismo con el id de un incidente ajeno → denegado.
4. Directivo abre "ver código" de un alumno → permitido.

Sin desplegar, la app sigue corriendo con las reglas anteriores: los cambios son de endurecimiento, no de compatibilidad, y ninguna pantalla necesita esperarlos para funcionar.
