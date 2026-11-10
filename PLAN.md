# AulaSegura — Plan de implementación

**Fuente de verdad visual:** proyecto Stitch `AulaSegura Mobile Design System` (`projects/7677408035297541086`).
**Fuente de verdad funcional:** `AulaSegura_resumen_mockups.md`.
**Fuente de verdad técnica:** `CONTEXT.md` (arquitectura, modelo de datos, reglas del proyecto).

El proyecto tiene dos etapas:

| Etapa | Estado | Alcance |
|---|---|---|
| **1. Diseño de pantallas** | ✅ Cerrada | 45 pantallas en Compose, con datos de ejemplo. |
| **2. Lógica de negocio** | 🔜 En curso | Firebase, Room, Cloudinary y servicios transversales. |

---

## Etapa 1 — Diseño de pantallas

### Reglas de trabajo

1. **Una pantalla por vez.** No avanzo a la siguiente hasta que me digas explícitamente que está aprobada.
2. **Fidelidad a Stitch.** Descargo el HTML real de la pantalla y lo replico. Si algo no se puede reproducir literal (mapas, fotos), lo imito lo más cerca posible y te lo aviso.
3. **Aviso de sync.** Al terminar cada pantalla te digo si necesitás **Sync** o solo **Run**:
   - Cambios solo en Kotlin/Compose → **no** hace falta Sync, alcanza con Run (▶) o Apply Changes.
   - Cambios en `build.gradle.kts`, `libs.versions.toml` o recursos nuevos (fuentes, drawables, strings) → **sí**, Sync.
4. **Verificación obligatoria.** Ninguna pantalla se marca como terminada sin `assembleDebug` en verde.

### Definición de terminado (por pantalla)

- [ ] HTML de Stitch descargado y analizado
- [ ] Implementada en Compose, con los datos de ejemplo del documento
- [ ] `assembleDebug` verde
- [ ] Aviso de Sync/Run enviado
- [ ] Probada por vos en el teléfono y aprobada

---

## Estado global

Leyenda: `[x]` aprobada · `[~]` implementada, esperando prueba · `[ ]` pendiente

| Bloque | Pantallas | Aprobadas |
|---|---|---|
| 0. Fundación | — | ✅ |
| 1. Acceso | 8 | 8 |
| 2. Familia | 14 | 14 |
| 3. Docente | 15 | 15 |
| 4. Administración | 11 | 11 |
| **Total** | **45** | **45** |

---

## Bloque 0 — Fundación ✅

- [x] **0.1** Proyecto, Gradle, dependencias (Navigation Compose, Material Icons Extended)
- [x] **0.2** Tipografía: Bricolage Grotesque + DM Sans + JetBrains Mono
- [x] **0.3** Paleta M3 completa del design system de Stitch
- [x] **0.4** Formas: 24 dp tarjetas, 28 dp hojas, píldoras
- [x] **0.5** `Routes.kt` + `AulaSeguraNavHost.kt` con todas las rutas navegables
- [x] **0.6** `assembleDebug` en verde

---

## Bloque 1 — Acceso (8 pantallas)

- [x] **1.1** Splash — `Routes.SPLASH` — `365fa467756743fb9ae312b7472eccc9` ✅ Aprobada
- [x] **1.2** Onboarding 1: Pasa lista — `Routes.ONBOARDING` — `457d7441afca4caf8e7cedd89038e01e` ✅ Aprobada
- [x] **1.3** Onboarding 2: Familias enteradas — `Routes.ONBOARDING` — `d9bd36fa9fb64a04b7c77c884560a5ef` ✅ Aprobada
- [x] **1.4** Onboarding 3: Modo Offline — `Routes.ONBOARDING` — `28503734721f4b0a984df90c4e8e9e97` ✅ Aprobada
- [x] **1.5** Iniciar Sesión — `Routes.LOGIN` — `ae9f321aff24410a9a383e5ff002bcb9` ✅ Aprobada (fondo escudo integrado)
- [x] **1.6** Recuperar Contraseña — `Routes.FORGOT_PASSWORD` — `198d897b3c6c45e7a67832e5393649cc` ✅ Aprobada
- [x] **1.7** Registro de Familias — `Routes.REGISTER` — `4ed8ecd6ef044004bce46d44e15fc520` ✅ Aprobada
- [x] **1.8** Permisos de la Aplicación — `Routes.PERMISSIONS` — `48ae43854c954d2c8764f1a608ab69a7` ✅ Aprobada

## Bloque 2 — Familia (14 pantallas)

- [x] **2.1** Inicio Familias — `Routes.PARENT_HOME` — `54371f57ccfe4427870eb7e7092542ff` ✅ Aprobada
- [x] **2.2** Asistencia — `Routes.PARENT_ATTENDANCE` — `9f38f3246cab47ebb7de406444a03096` ✅ Aprobada
- [x] **2.3** Tareas — `Routes.PARENT_TASKS` — `58f3dbb8b1f2486abcfd8efe5dde189b` ✅ Aprobada
- [x] **2.4** Detalle de Tarea — `Routes.PARENT_TASK_DETAIL` — `29ac02e4480a4828a0fe2ba1351e3014` ✅ Aprobada
- [x] **2.4b** Avisos — `Routes.PARENT_NOTICES` — `a60924077dd1436e895010e33504e0c1` ✅ Aprobada
- [x] **2.5** Justificar Falta — `Routes.PARENT_JUSTIFY` — `9a38ede8a0894eb9886bd7197031d841` ✅ Aprobada
- [x] **2.6** Incidentes Escolares — `Routes.PARENT_INCIDENTS` — `6ae883e957924dd0a00d08578915749b` ✅ Aprobada
- [x] **2.7** Detalle del Incidente — `Routes.PARENT_INCIDENT_DETAIL` — `98e94dd122564074be5ecb0978a219df` ✅ Aprobada
- [x] **2.8** Detalle del Incidente con Mapa — `Routes.PARENT_INCIDENT_MAP` — `65e5f2a843054caa8acc844ef4d74681` ✅ Aprobada
- [x] **2.9** Mensajes — `Routes.PARENT_CHAT` — `c52d05bdfa5b42f494d9cc0d7062ff90` ✅ Aprobada
- [x] **2.10** Conversación de Chat — `Routes.PARENT_CONVERSATION` — `889c39e23e6142c09cd11698a057830e` ✅ Aprobada
- [x] **2.11** Asistente IA — `Routes.PARENT_AI` — `bd8b739044634567b469e6d2fbcafedb` ✅ Aprobada
- [x] **2.12** Notificaciones — `Routes.PARENT_NOTIFICATIONS` — `95a5d9030ce548b890a8491db6e9161f` (`ParentNotificationsScreen`; la campana navega al contenido real) ✅ Aprobada
- [x] **2.13** Perfil — `Routes.PARENT_PROFILE` — `7ad5b00011fe438390ac923e9e0d28e6` (`ParentProfileScreen`; tab del shell con `ParentBottomBar`) ✅ Aprobada

## Bloque 3 — Docente (15 pantallas)

- [x] **3.1** Inicio Docente — `Routes.TEACHER_HOME` — `b668bc5d2e214c139c8f0e3342910af4` ✅ Aprobada
- [x] **3.2** Mis Grupos — `Routes.TEACHER_GROUPS` — `46e9f0d2b76e4ab3af223d2f3f7903ee` ✅ Aprobada
- [x] **3.3** Pase de lista — `Routes.TEACHER_ROLL_CALL` — `3d8ed962b05c4f27b7b4d2a6947e1d1c` ✅ Aprobada
- [x] **3.4** Pase de lista: Dentro de radio — variante — `322bcf1ed5794db6ad8f046d2cd3224d` ✅ Aprobada
- [x] **3.5** Pase de lista: Fuera de radio — variante — `9e26e73fbb6f4083afb711025fb09747` ✅ Aprobada
- [x] **3.6** Pase de lista: Mapa sin conexión — variante — `1fc8a561ec084affbb43dd2666b26c41` ✅ Aprobada
- [x] **3.7** Pase de lista: Verificando GPS — variante — `0879467b1682473d8d4429311b83b6ac` ✅ Aprobada
- [x] **3.8** Pase de lista: Verificando ubicación — variante — `35a0556e68c549f0b3552c5a23cad46a` ✅ Aprobada
- [x] **3.9** Registrar Incidente — `Routes.TEACHER_INCIDENT_NEW` — `2b5038f52ef64c8aa1f541755b2860e7` ✅ Aprobada
- [x] **3.10** Registrar Incidente con Mapa — variante de 3.9 — `c0c76972a26549c387f8febc138ac124` ✅ Aprobada (mapa esquemático dentro de `TeacherIncidentNewScreen`)
- [x] **3.11** Publicar Tarea — `Routes.TEACHER_TASK_NEW` — `8acb44e85be44ad5801bd1532a7ab798` ✅ Aprobada
- [x] **3.12** Publicar Aviso — `Routes.TEACHER_NOTICE_NEW` — `bac6bd729b1f46129bcbc5092e0d5b2d` ✅ Aprobada
- [x] **3.13** Historial — `Routes.TEACHER_HISTORY` — `cbe12103d8494161b9e19735c062de66` ✅ Aprobada
- [x] **3.14** Notificaciones Docente — `Routes.TEACHER_NOTIFICATIONS` — `cd4bc66529b14e1a9aed9bfa3bcc4208` ✅ Aprobada
- [x] **3.15** Perfil Docente — `Routes.TEACHER_PROFILE` — `6e82dee0cb5245af83656dbbd9073ce9` ✅ Aprobada

> Mensajes y Conversación del docente reutilizan los diseños 2.9 y 2.10 (misma pantalla, roles invertidos) y tienen implementación propia: `TeacherChatScreen` y `TeacherConversationScreen`.

### Desvíos conocidos del bloque Docente

- **Nav compartida.** La barra flotante se extrajo a `ui/components/TeacherBottomBar.kt` (`enum TeacherTab`), igual que `ParentBottomBar`. Inicio y Mis grupos la usan; las pantallas siguientes deben reutilizarla, no reimplementarla.
- **Watermarks sin dibujar.** El patrón de puntos (Inicio) y el de escudos al 4% (Mis grupos) del mockup no están implementados, por consistencia con el resto de la app. Pendiente decidir si se agrega como componente global.
- **Foto del docente.** El mockup usa un retrato real; se imita con monograma en el mismo círculo con aro.
- **Iniciales.** El mockup de Mis grupos muestra "MG"; se usa "RS" (Ricardo Salinas) en todo el flujo para no mostrar dos docentes distintos.
- **Filtros de Mis grupos.** "Todos / Hoy / Matemáticas" filtran sobre el modelo de datos, pero con los datos del mockup los 3 grupos son de hoy y de Matemáticas, así que todavía no se ve diferencia.
- **Barra de estado.** Se usa la real del sistema (`statusBarsPadding()`), no la fila "07:58" / "08:15" del HTML.

## Bloque 4 — Administración (11 pantallas) ✅

- [x] **4.1** Panel Directivo — `Routes.ADMIN_DASHBOARD` — `ffacff9f30304998885d2b9ca9eff58f` (`AdminDashboardScreen` + `AdminBottomBar`)
- [x] **4.2** Usuarios — `Routes.ADMIN_USERS` — `17231bc66b8344c293f2c00a39447dc3` (`AdminUsersScreen`, 3 pestañas + búsqueda local)
- [x] **4.3** Nuevo alumno — `Routes.ADMIN_NEW_STUDENT` (`admin/users/new`) — `e23d5bbd8dec4b788e60dad0ebaec45f` (`AdminNewStudentScreen`)
- [x] **4.4** Grupos — `Routes.ADMIN_GROUPS` — `2f00e426ce184e8b9ca538b7f2163d77` (`AdminGroupsScreen` + filtro por grado)
- [x] **4.5** Gestión de Incidentes — `Routes.ADMIN_INCIDENTS` — `62d964e2813648fdbeb22d9f9e4a2b03` (`AdminIncidentsScreen` + chips de severidad)
- [x] **4.6** Reportes de Asistencia — `Routes.ADMIN_REPORTS` — `d88102c4afaf4471bee6d03ee139ff86` (`AdminReportsScreen`: anillo, barras semanales, por grupo, más faltas)
- [x] **4.7** Configuración del plantel — `Routes.ADMIN_CAMPUS` — `b3a4491d635d48af914f42eb44f17cb2` (`AdminCampusScreen`: mapa estilizado, slider 50–500 m, horario, switch)
- [x] **4.8** Perfil (Administración) — `Routes.ADMIN_PROFILE` — adaptado de `7ad5b00011fe438390ac923e9e0d28e6` (`AdminProfileScreen` con datos de la directora)
- [x] **4.9** Más (Administración) — `Routes.ADMIN_MORE` — `5811ea92d5ff4f3fbf335cfaf1e51c75` (`AdminMoreScreen`: hub de 4 accesos con badges)
- [x] **4.10** Notificaciones (Administración) — `Routes.ADMIN_NOTIFICATIONS` — `7b4875da56794dfcb2d138bb3d77089b` (`AdminNotificationsScreen`: resumen, chips de categoría, HOY/ANTERIORES)
- [x] **4.11** Detalle de grupo — `Routes.ADMIN_GROUP_DETAIL` (`admin/groups/{groupId}`) — `d58f984a153d45c195613ad7cb18b38d` (`AdminGroupDetailScreen`: header 210dp, tiles, tabs Alumnos/Docentes/Horario)

> El "Más" viejo (`c6458c…`) ya no se usa: era la pantalla combinada de Más + Perfil.

## Ajustes transversales (todos los roles)

- **Login:** los tres roles entran solo con credenciales hardcodeadas (padre: `carlos.gomez@familia.mx` / `AulaSegura#2024`). Se eliminó el acceso por "Regístrate": fuera el link de `LoginScreen`, fuera la ruta `Routes.REGISTER` y fuera el atajo `PERMISSIONS → PARENT_HOME`. Permisos ahora solo se abre desde el Perfil de padre y vuelve con `popBackStack`. `RegisterScreen.kt` queda en disco pero inalcanzable.
- **Patrón de escudos:** overlay global en `MainActivity` (`ShieldPattern`, alpha 0.035, `PrimaryContainer`) dibujado sobre el NavHost, así que aplica a todas las pantallas de los tres roles sin editar cada archivo.
- **Animación de entrada:** transiciones globales en `AulaSeguraNavHost` (`fadeIn` 300 ms + `slideInVertically` altura/40 al entrar, `fadeOut` 180 ms al salir) para toda navegación y cambio de pestaña.
- **Animación de entrada en docente:** además de la transición global, todas las pantallas de docente entran con el stagger de `Reveal` (Inicio 0/60/120/180/240, Grupos 0/60/120, Asistencia 0…240) o con `Modifier.revealOnEnter(60)` en el contenedor que lleva `weight` (Incidente nuevo, Tarea nueva, Aviso nuevo, y el panel inferior de las pantallas de mapa).
- **Animación de entrada en directivo:** mismo criterio en las 11 pantallas. `Reveal` en las que tienen bloques directos (Panel 0…240, Usuarios 0…240, Grupos 0…240 vía modifier, Incidentes 0…180, Reportes 0…300, Perfil 0/60/120, Notificaciones 0…180); `Modifier.revealOnEnter` en los contenedores con `weight` (Configuración del plantel, Nuevo alumno, Más hub, Detalle de grupo) y en los `Row` de cabecera/filtros que no admiten envoltura limpia.
- **Escudos en headers índigo:** `Modifier.shieldPattern(alpha, color)` en `ui/components/ShieldLogo.kt` pinta el patrón como fondo del propio contenedor. Aplicado en Perfil de docente, Perfil de admin y Detalle de grupo de admin (el overlay global es índigo y no se ve sobre índigo).

---

## Datos de ejemplo (usar siempre estos)

- **Plantel:** Escuela Primaria "Benito Juárez" · Ciclo 2026-2027 · 85 alumnos · 4 docentes · 3 grupos
- **Padre:** Carlos Gómez Pérez (carlos.gomez@familia.mx) · hijos Mariana Gómez Ruiz (3° B) y Diego Gómez Ruiz (1° A)
- **Docente:** Prof. Ricardo Salinas Ortega, Matemáticas · 3° B (28), 3° A (30), 2° C (27)
- **Administradora:** Directora Patricia Rangel Vega (directora@escuela.mx)
- **Escuela:** 19.2926, -99.6557 · radio 150 m
- **Fecha de referencia:** martes 3 de noviembre de 2026

---

# Etapa 2 — Lógica de negocio

> Arquitectura, modelo de datos, carpetas de Cloudinary, entidades de Room, reglas y convenciones: **`CONTEXT.md`**.
> Este documento es el tablero de tareas; `CONTEXT.md` es el contrato técnico.

## Decisión: por qué rol empezar

**Orden recomendado: Docente → Directivo → Tutor.**

### 1. Docente primero (Diego)

Es el rol donde vive el producto y donde vive el riesgo:

- **Es el corazón funcional.** El pase de lista con GPS es la razón de existir de la app; si esa experiencia no funciona, el resto no importa.
- **Concentra todo lo difícil.** Es el único rol que necesita Room + cola de sincronización + subida de media + chat en tiempo real + notificaciones. Los otros dos reutilizan lo que aquí se construya.
- **Genera los datos que consumen los demás.** Asistencia, incidentes, tareas y avisos alimentan al tutor y a los reportes del directivo. Sin docente, el tutor no tiene nada que leer.
- **Si el modelo offline no cierra, hay que rediseñar la capa de datos.** Mejor descubrirlo en la primera fase que en la última.
- **No tiene la complejidad de alta de cuentas.** El docente se crea una vez en la consola/seed y ya puede entrar.

### 2. Directivo segundo (Evelyn)

- Es principalmente **CRUD y agregados** sobre datos que ya existen.
- Su parte delicada es el **alta de cuentas**, que en cliente no se puede hacer sin cerrar la sesión del directivo. Sin Cloud Functions (plan Spark), se resuelve con un script local con Admin SDK. Ese riesgo se aísla en su propia fase y no bloquea el núcleo.
- Puede arrancar **en paralelo** con docente en cuanto la Fase 0 cierre, porque solo depende del esquema acordado, no del código de docente.

### 3. Tutor tercero (Evelyn, con apoyo de Diego)

- **Solo lee y reacciona.** Depende de asistencia, incidentes, tareas, avisos y chat, que ya existen al terminar las fases 1 y 2.
- Su única pieza exclusiva es el **asistente con Firebase AI Logic** (modelos Gemini del proveedor *Gemini Developer API*, gratis en el plan Spark). Va al final porque depende de que el resto esté estable, no porque tenga costo.

> **Restricción de costo: todo el proyecto corre en planes gratuitos.**
> El proyecto Firebase se queda en **Spark (sin facturación vinculada)** y Cloudinary en **free**. Consecuencia directa: **no hay Cloud Functions**. Eso cambia dos tareas — el alta de cuentas y el envío de push — y está explicado en la Fase 0 y en `CONTEXT.md` §9.1.

> **Resumen ejecutivo:** Docente primero porque es el núcleo y el mayor riesgo técnico. Directivo después porque construye sobre datos existentes. Tutor al final porque consume todo lo anterior.

---

## Fase 0 — Fundación técnica (bloqueante, ambos)

Sin esto no empieza ningún rol.

| # | Tarea | Entregable | Responsable |
|---|---|---|---|
| 0.1 | Crear proyecto Firebase (dev) y agregar `google-services.json` fuera de git | App conectada, build verde | Diego |
| 0.2 | Dependencias: Firebase BOM, Auth, Firestore, RTDB, Messaging, Analytics, Crashlytics, Remote Config, App Check, Room, WorkManager, Hilt, Coil, OkHttp | `build.gradle.kts` + catálogo de versiones | Diego |
| 0.3 | **App Check**: debug provider en debug, Play Integrity en release | Consola + código de arranque | Diego |
| 0.4 | **Auth real**: login, sesión persistente, logout, recuperar contraseña, mapeo `uid → role → grafo` | `AuthRepository` + ViewModel de Login | Diego |
| 0.5 | **Firestore**: reglas base, índices y emulador local | `docs/firestore.rules` + `docs/firestore.indexes.json` | Ambos |
| 0.6 | **Room**: base, DAOs vacíos, entidades y `OutboxEntity` | `data/local/` | Diego |
| 0.7 | **WorkManager**: `SyncWorker` esqueleto con backoff | `sync/` | Diego |
| 0.8 | **Cloudinary** (plan free): cuenta, presets por carpeta, cliente de subida | `data/remote/cloudinary/` | Evelyn |
| 0.9 | **DI** (Hilt o `AppContainer`) + Coil + Coroutines/Flow + `UiState` | `di/`, `core/` | Diego |
| 0.10 | **Analytics + Crashlytics + Remote Config** inicializados | `core/observability/` | Evelyn |
| 0.11 | Script de semilla **local** con Admin SDK (crea usuarios con service account) | `tools/seed/` | Evelyn |
| 0.12 | `.gitignore`: `google-services.json`, `local.properties`, `.idea/`, claves | repo limpio | Diego |
| 0.13 | **AI Logic** habilitado con proveedor Gemini Developer API + `firebase-ai` | `data/remote/ai/` | Diego |

**Criterio de cierre de la Fase 0:** un docente creado por el seed local inicia sesión real, aterriza en `TeacherHome`, la app lee su perfil desde Firestore, App Check está activo, AI Logic responde una consulta de prueba y `assembleDebug` compila verde. El proyecto queda en **Spark, sin Cloud Functions**.

---

## Fase 1 — Docente (Diego)

| # | Tarea | Pantalla | Servicios |
|---|---|---|---|
| 1.1 | Repositorios de grupos y alumnos del docente | Inicio, Mis grupos | Firestore |
| 1.2 | Pase de lista **online**: crear sesión + registros | Pase de lista | Firestore |
| 1.3 | Room: caché de grupos/alumnos + borrador de sesión | Pase de lista | Room |
| 1.4 | Pase de lista **offline** + sincronización con outbox | Pase de lista | Room + WorkManager |
| 1.5 | GPS y geocerca con `schools.radiusMeters` | Pase de lista (todas las variantes) | Location |
| 1.6 | Registrar incidente: severidad, ubicación, borrador offline | Registrar incidente | Firestore + Room |
| 1.7 | Subida de fotos/videos a Cloudinary con progreso y reintento | Registrar incidente | Cloudinary |
| 1.8 | Publicar tarea y publicar aviso a grupos reales | Tarea nueva, Aviso nuevo | Firestore |
| 1.9 | Historial: consulta de sesiones del docente | Historial | Firestore |
| 1.10 | Chat docente: conversaciones y mensajes | Chat, Conversación | RTDB + Firestore |
| 1.11 | FCM: token, recepción, apertura por deep link. **En Spark los envíos se hacen desde la consola de Firebase** (a tema o a token); el envío automático por evento requiere Blaze y queda fuera del alcance. | Notificaciones | FCM |
| 1.12 | Perfil: datos reales, foto, logout | Perfil | Auth + Cloudinary |

**Criterio de cierre:** con modo avión activo, el docente pasa lista y registra un incidente con foto; al reconectar, ambos aparecen en Firestore sin duplicados y con la media subida.

---

## Fase 2 — Directivo (Evelyn)

| # | Tarea | Pantalla | Servicios |
|---|---|---|---|
| 2.1 | Alta de cuentas **sin Cloud Functions** (Spark): script local con Admin SDK + custom claim de rol; en la app, el directivo solo edita perfiles e inactiva usuarios | Usuarios | Admin SDK + Auth |
| 2.2 | Usuarios: alta, edición, baja lógica, filtros reales | Usuarios | Firestore |
| 2.3 | Grupos: CRUD y asignación de docentes | Grupos, Detalle de grupo | Firestore |
| 2.4 | Alumnos: alta con CURP, número de lista, grupo y código de vinculación | Nuevo alumno | Firestore |
| 2.5 | Panel: métricas reales del plantel | Panel | Firestore |
| 2.6 | Reportes: consultas por rango y agregados | Reportes | Firestore |
| 2.7 | Bandeja de incidentes y cambio de estado | Incidentes | Firestore |
| 2.8 | Configuración del plantel: radio, horario, switch | Configuración | Firestore + Remote Config |
| 2.9 | Notificaciones reales + push | Notificaciones | Firestore + FCM |
| 2.10 | Perfil de la directora | Perfil | Auth + Cloudinary |

**Criterio de cierre:** la directora crea un grupo y dos alumnos desde la app, y el docente los ve en su pase de lista sin tocar código.

---

## Fase 3 — Tutor (Evelyn, apoyo de Diego)

| # | Tarea | Pantalla | Servicios |
|---|---|---|---|
| 3.1 | Vinculación tutor ↔ alumnos por código | Perfil / Nuevo alumno | Firestore |
| 3.2 | Inicio: resumen real de los hijos | Inicio Familias | Firestore |
| 3.3 | Asistencia: calendario desde sesiones y registros | Asistencia | Firestore |
| 3.4 | Justificar falta con adjunto | Justificar falta | Firestore + Cloudinary |
| 3.5 | Tareas y avisos reales, con marca de leído | Tareas, Avisos | Firestore |
| 3.6 | Incidentes de sus hijos + marcar visto | Incidentes, Detalle, Mapa | Firestore |
| 3.7 | Chat con el docente | Mensajes, Conversación | RTDB |
| 3.8 | Asistente con **Firebase AI Logic** (Gemini Developer API, free tier), directo desde el cliente con App Check | Asistente IA | AI Logic |
| 3.9 | Notificaciones + push | Notificaciones | Firestore + FCM |
| 3.10 | Perfil del tutor y foto | Perfil | Auth + Cloudinary |

**Criterio de cierre:** el tutor ve la asistencia que cargó el docente, recibe el push del incidente y conversa con el docente en tiempo real.

---

## Fase 4 — Servicios transversales y entrega (ambos)

| # | Tarea | Servicio |
|---|---|---|
| 4.1 | Sitio complementario: landing, privacidad, soporte | Hosting |
| 4.2 | Robo test de los flujos críticos + matriz de dispositivos (cuota gratuita limitada; verificar qué dispositivos no requieren facturación) | Test Lab |
| 4.3 | Eventos y embudos definitivos | Analytics |
| 4.4 | Alertas y non-fatals de sincronización | Crashlytics |
| 4.5 | Flags: radio, IA, mantenimiento, versión mínima | Remote Config |
| 4.6 | Exigir App Check en Firestore, RTDB y AI Logic | App Check |
| 4.7 | Reglas e índices finales + revisión de seguridad | Firestore |
| 4.8 | README, video demo, APK release firmado | — |

---

## Servicios: quién los usa y en qué fase

| Servicio | Fase | Rol que lo estrena |
|---|---|---|
| Authentication | 0 | Acceso |
| App Check | 0 | Global |
| Analytics / Crashlytics / Remote Config | 0 | Global |
| Room + WorkManager | 1 | Docente |
| Cloudinary | 1 | Docente |
| Realtime Database | 1 | Docente |
| Cloud Messaging | 1 | Docente |
| Firebase AI Logic | 3 | Tutor |
| Test Lab / Hosting | 4 | Global |

> **Cloudinary reemplaza a Firebase Storage en todo el proyecto.** No se agrega la dependencia `firebase-storage` ni se activa Storage en la consola.

> **Sin Cloud Functions.** El proyecto se queda en Spark para conservar el free tier de AI Logic: vincular facturación (Blaze) mueve Gemini Developer API a tier pago. Por eso el alta de cuentas es un script local y los push se envían desde la consola.

---

## Antes de subir a GitHub

Repo todavía no inicializado. Checklist:

- [ ] `git init` en `C:\Users\diego\AndroidStudioProjects\AulaSegura`
- [ ] Ampliar `.gitignore` con: `google-services.json`, `.idea/`, `*.jks`, `*.keystore`, `keystore.properties`
- [ ] Confirmar que `local.properties` **no** está trackeado (contiene rutas y la API key de Maps)
- [ ] Primer commit: `chore: initial commit — design stage complete`
- [ ] Crear el repo remoto y agregar a Evelyn como colaboradora
- [ ] Compartir `google-services.json` y `MAPS_API_KEY` por canal privado
- [ ] Proteger `main`: PR obligatorio y build verde
