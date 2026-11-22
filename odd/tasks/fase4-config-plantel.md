# Fase 4 — Configuración del plantel y limpieza del panel directivo

Branch: `fix/fase4-directivo-config`. Delivery: work-unit commits on this branch; push/merge only on user approval. Test runner: `./gradlew testDebugUnitTest`; final check: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug`.

## Specs

**S1** — User request, verbatim: "Has todo esto quitar ambos FABs, sacar los badges falsos y 'Requiere atención', y hacer que el nombre del plantel se configure a mano y se muestre en perfil/panel, implementalo, lo que si quiero que quede bien implementado es lo de la configuracion del plantel."

**S2** — Remove the manual "+" from Usuarios (docentes, padres y alumnos) and from Grupos. Student creation remains reachable from Grupos / group detail / deep links; deterministic groups are created by the QR flows.

**S3** — Remove the hardcoded `2` and `4` badges from the Más screen and the static "Requiere atención" card from the panel. No invented counters.

**S4** — The school name is manually editable in Configuración del plantel, persisted in `schools/{schoolId}`, allowed by Firestore rules for the director, and displayed in the director profile and the panel instead of the raw `schoolId`. No automatic reverse geocoding.

**S5** — "Centro guardado" only appears when a centre is actually stored; coordinate fields show an error only after a save attempt, not on first entry.

## Tasks

- **T1** — S2,S3: FAB and fake-badge removal. Route: inline. Evidence: both FABs removed, Más badges null, static attention card deleted; build GREEN.
- **T2** — S4: school name write path and rules. Route: inline. Evidence: pending.
- **T3** — S4: name shown in profile and panel. Route: inline. Evidence: pending.
- **T4** — S5: honest campus UI states. Route: inline. Evidence: pending.
- **T5** — all: full verification, FASE4.md update, merge/push on approval. Route: inline. Evidence: pending.

## Log

**L1** — User request (verbatim): "Has todo esto quitar ambos FABs, sacar los badges falsos y 'Requiere atención', y hacer que el nombre del plantel se configure a mano y se muestre en perfil/panel, implementalo, lo que si quiero que quede bien implementado es lo de la configuracion del plantel."

**L2** — Decision: the school name is entered manually; reverse geocoding is deliberately excluded because it can disagree with the name the school wants.
