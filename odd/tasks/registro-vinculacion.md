# Registro, aprobación y vinculación

Rama: documentos en `main`; T1 en rama de trabajo · entrega: commit en `main`, push con OK del dueño · runner: `gradlew.bat testDebugUnitTest assembleDebug`
Fuentes: `AulaSegura-registro-vinculacion.md`, `AulaSegura-pantallas-actualizadas-y-nuevas.md` · backend: Firebase Auth + Firestore, Room + WorkManager

## Specs

**S1 — El cambio de fondo.** «El director deja de dar de alta a docentes y alumnos uno por uno: los docentes piden acceso y el director los aprueba, y cada tutor registra a su hijo con un QR que la directora escanea.»

**S2 — No hay código de alumno.** «Código del alumno | Obligatorio para registrar al tutor | Ya no existe: el correo verificado del tutor basta». «Se elimina el campo del código del alumno y el campo obligatorio del código en el registro del tutor.»

**S3 — Un solo modelo, reflejado en Room.** «Se agregan campos a `users` y `students`; no hace falta una colección nueva. Los mismos campos se reflejan en las entidades de Room, junto con `syncStatus` y `updatedAt`.»

**S4 — Campos nuevos.** `users`: «`fechaSolicitud` | fecha | Para ordenar la lista de solicitudes del director». `students`: «`tutores` | lista de uid | Uno o dos tutores vinculados»; «`grado`, `grupo` | texto | Se dejan vacíos si el alumno está "Sin grupo"»; «`creadoPor` | uid | La directora que aprobó el alta»; «`syncStatus`, `updatedAt` | texto, fecha | Control de sincronización con Room».

**S5 — La seguridad no es sólo UI.** «Validar solo en la interfaz no basta: las reglas de Firestore deben impedir que alguien se apruebe a sí mismo o se vincule a un alumno ajeno.»

**S6 — Sin conexión.** «La directora puede escanear y aprobar sin conexión: el alumno se guarda en Room con `syncStatus = PENDIENTE` y se sube a Firestore con WorkManager al volver la señal.»

**S7 — Roles.** «No existe registro público para el director, cuya cuenta se crea de forma manual en Firebase.» El tutor queda `activo` y `vinculacion = pendiente`; el docente queda `estado = pendiente` hasta que el director lo apruebe.

**S8 — Estado inicial del proyecto (auditoría del 20 nov 2026).** Ya están el login con redirección por estado, el registro Tutor/Docente, Verificar correo, Cuenta en revisión/rechazada, la pantalla de vinculación como placeholder, los chips de hijos y las reglas de autocreación. No existen el QR, el escáner, las pantallas del director ni el modelo nuevo de `students`.

## Tasks

| ID | Spec | Tarea | Ruta | Estado | Commit |
|---|---|---|---|---|---|
| T1 | S3, S4, S5, S8 | Migrar `students`/`users` y Room; reglas base; quitar código de alumno | `di`, `data`, `domain`, `docs/firestore.rules` | en curso | — |
| T2 | S1, S2, S6 | QR del tutor: generación offline, corregir nombre, escucha en tiempo real | `ui/screens/access`, `navigation` | pendiente | — |
| T3 | S1, S5 | Escanear alumno: cámara, permiso, validación de QR | `ui/screens/admin`, `navigation` | pendiente | — |
| T4 | S1, S4, S5, S6 | Revisar alumno: grado/grupo, duplicados, segundo tutor, alta offline | `ui/screens/admin`, `data` | pendiente | — |
| T5 | S1, S5 | Solicitudes de docentes: listar verificados, aceptar/rechazar, lote | `ui/screens/admin`, `data` | pendiente | — |
| T6 | S1 | Panel del director: tarjetas, FAB, navegación con badges | `ui/screens/admin` | pendiente | — |
| T7 | S1, S4 | Alumnos sin grupo: búsqueda, selección y asignación en lote | `ui/screens/admin`, `data` | pendiente | — |
| T8 | S1, S2 | Inicio del tutor: "+ Agregar hijo", grupo por asignar, sin datos | `ui/screens/parent` | pendiente | — |
| T9 | S5, S7 | Reglas completas: `email_verified`, director-only, lectura por tutor/grupo | `docs/firestore.rules` | parcial | — |
| T10 | S1–S8 | Tests JVM por regla y flujo, build verde, prueba en dispositivo | `app/src/test` | pendiente | — |

## Log

**L1 — 21 nov 2026, pedido del dueño.** «Si, has el documento de seguimiento odd/tasks/registro-vinculacion.md, necesito que ese documento este en main para que podamos ir checando avance entre mi colaborador y yo, una vez hecho el documento da inicia con la T1.»

**L2 — 21 nov 2026, análisis del flujo.** El flujo completo es: login sin selección de rol → verificación de correo → por rol (docente en revisión, tutor con QR, director al panel); el tutor muestra el QR y la directora lo escanea, revisa, asigna grupo y acepta; el docente espera aprobación; el director aprueba en lote y asigna grupos en lote. La auditoría previa mostró que el cuello está en los datos: `students` sigue con `tutorIds`/`groupId`/`listNumber`/`schoolId` y las reglas con `registrationCode`, es decir, el modelo que este cambio elimina. Por eso T1 es el punto de partida: sin él, T2–T8 no pueden escribir datos reales.

**L3 — 21 nov 2026, optimización previa ya entregada.** `0119288 perf(navigation): shorten route transitions and add press feedback` — transiciones 300/180 ms → 140/100 ms y respuesta táctil en el enlace de registro. Verificado: 78 suites / 569 tests / 0 fallos, `assembleDebug` verde. Ya en `origin/main`.
