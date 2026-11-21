# Teacher group and subject assignment

Branch: `feat/fase4-docente-grupo-materia`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`.

## Specs

**S1** — Fixed catalog, verbatim: "normalmente son 6 docentes por grupo ya que cada grupo tiene , español, matematicas, lectura, historia, geografia, ciencias". Exactly one teacher per subject per group; group ids are the existing deterministic `grp-{grado}{letra}` with grades 1..6 and sections A..D.

**S2** — New flow, verbatim: "creemos un nuevo flujo para darle al docente su grupo incluyendo grado y talvez que el escoja la metria de esas seis que quiere dar". The teacher picks grade + section (which resolve to the group id) and one of the six subjects. Assumption: one teacher owns exactly one group and one subject.

**S3** — Uniqueness, verbatim: "un docente no puede dar la misma materia en un grupo si otro profesor ya la tiene asignada". Conflict is checked when choosing and again atomically at commit.

**S4** — Automation, verbatim: "lo que elijio el docente se haga y solko se muestre si hay un error". No director decision about group or subject; on conflict, show an error only.

**S5** — Approval boundary, verbatim: "lo que si sigue estando en aprobacion es para ser aceptado como docente eso si dejemeslo, pero para grupo que se acepte automatico". The director still approves the person; the chosen group/subject are committed automatically in the same approval transaction.

**S6** — Current defect, verbatim: "actualmente en el registro de docente tu pones tu grupo y materia pero eso no tiene sentido asi como actualemnte esta ya que al final no termina haciendo nada". Registration must produce real links, not a free-text label.

**S7** — Motivation, verbatim: "lo que buscamos es que esta app sea lo mas automata posible".

## Tasks

- **T1** — S1,S2,S3: subject catalog + grade/section resolution + pure conflict resolver, with tests. Route: inline. Evidence: `TeacherAssignmentTest` GREEN; `SchoolCatalog` has six subjects, `subjectAvailability`, `teacherAssignmentError`.
- **T2** — S2,S3,S4,S6: registration UI/VM captures grade, section and subject; availability feedback; stores the pending intent. Route: inline. Evidence: `RegisterViewModelTest` GREEN; chips in `RegisterScreen`; `AuthRepositoryImpl` writes `requestedGroupId`/`requestedSubject`.
- **T3** — S3,S4,S5: approval transaction commits group + subject links atomically and reports conflicts. Route: inline. Evidence: `TeacherApprovalPlanTest` GREEN; `FirestoreAdminUserRepository.setTeacherStatus` uses a Firestore transaction; `acceptAllPendingTeachers` goes through the same path.
- **T4** — S2,S6: Firestore rules allow only the pending intent fields on a docente's own profile. Route: inline. Evidence: narrow `allow update` branch in `docs/firestore.rules`; `groupIds` deliberately excluded.
- **T5** — S5,S7: director-created teacher accounts use the same assignment path; existing teachers without a group are handled. Route: inline. Evidence: `newAccountDocument` writes intent for docentes; `AdminUsersUiTest` updated. Existing-teacher backfill remains a manual Console task.
- **T6** — all: full verification, FASE4.md update, merge/push on approval. Route: inline. Evidence: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug` GREEN — 103 suites / 661 tests / 0 failures.

## Log

**L1** — User's original request (verbatim): "Antes de seguir con la 4.9, necesito que em ayudes a hacer de manera logica y que concuerde con todo el flujo que hemos trabajado para la parte de asignarle un grupoa  un docente y que este tambien tenga su materia, normalmente son 6 docentes por grupo ya que cada grupo tiene , español, matematicas, lectura, historia, geografia, ciencias, ahora aqui viene la parte de la logica, actualmente en el registro de docente tu pones tu grupo y materia pero eso no tiene sentido asi como actualemnte esta ya que al final no termina haciendo nada, necesito que analices el flujo que llevamos hast ahora para quye creemos un nuevo flujo para darle al docente su grupo incluyendo grado y talvez que el escoja la metria de esas seis que quiere dar, un docente no puede dar la misma materia en un grupo si otro profesor ya la tiene asignada, necesito que enserio me ayudes a captar y analizar de forma correcta la mejor decision para esta parte."

**L2** — User decision (verbatim): "ES que si hacemos que el director este aceptando las solicitudes de docnetes para las maeterias seria mas trabajo para el director y lo que buscamos es que esta app sea lo mas automata posible, asi que por esta vez decido que lo que elijio el docente se haga y solko se muestre si hay un error, lo que si sigue estando en aprobacion es para ser aceptado como docente eso si dejemeslo, pero para grupo que se acepte automatico."

**L3** — Authorization (verbatim): "Arranca con la implementacion de este flujop".

**L4** — Design decision: the teacher's choice is stored as a pending intent at registration and committed automatically inside the director's approval transaction, because `users.groupIds` and `groups/{id}` are director-only writes in the rules. One atomic transaction on the group document makes two concurrent approvals unable to steal the same subject slot.

**L5** — Implementation complete on `feat/fase4-docente-grupo-materia`: six-subject catalog, grade/section chips, conflict feedback at registration, approval transaction committing `subjectTeachers`/`teacherIds`/`groupIds`/`subjects`, narrow rules branch for the pending intent, and director-created teachers routed through the same intent. Verified 103 suites / 661 tests / 0 failures plus `assembleDebug`. Outstanding: deploy the updated `docs/firestore.rules`, backfill existing teachers manually, and device verification.
