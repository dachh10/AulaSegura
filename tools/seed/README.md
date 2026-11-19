# Firestore seed (Fase 0)

One-shot bootstrap for the demo data: the school, four groups, their rosters and
the links between the three demo accounts. Run it once after the accounts exist;
it is safe to run again.

The Admin SDK is used on purpose. A client cannot write another user's
`studentIds`, and the linking flow that will do it later is not built yet, so the
only honest way to seed those links is from a privileged context.

## What it writes

| Collection | Documents |
| --- | --- |
| `schools` | `sch-benito-juarez` |
| `groups` | `grp-3b` (28), `grp-3a` (30), `grp-2c` (27), `grp-1a` (26) |
| `students` | one per roster entry, `stu-<group>-<nn>` |
| `users` | link fields only: `schoolId`, and for the teacher `subjects`/`groupIds`, for the tutor `groupIds`/`studentIds` |

`users` identity fields (`role`, `fullName`, `email`, `active`) are never touched.
Only the links are merged, because those accounts were created by the auth flow.

The tutor is linked to the two children in `CONTEXT.md` §11: Mariana Gómez Ruiz
(3° B, list 06) and Diego Gómez Ruiz (1° A, list 07). The tutor's `groupIds` is
denormalised from those children — the security rules cannot walk
`studentIds -> groupId`, so without it the tutor cannot read a task or a notice
addressed to their children's group.

## Prerequisites

1. The three demo accounts exist in Firebase Auth:
   `docente@aulasegura.com`, `directora@escuela.mx`, `carlos.gomez@familia.mx`.
   The script fails with a clear message if any is missing.
2. A service account key with the Firebase Admin SDK role, stored **outside the
   repository**. The default path is
   `%USERPROFILE%\.aulasegura\firebase-adminsdk.json`.

## Run

```powershell
cd tools/seed
npm install
$env:GOOGLE_APPLICATION_CREDENTIALS = "$env:USERPROFILE\.aulasegura\firebase-adminsdk.json"
npm run seed
```

Add `-- --dry-run` to read and report without writing:

```powershell
npm run seed -- --dry-run
```

The script prints what it created or updated, then reads the data back and
compares each group's `studentCount` against the real number of students. A
mismatch exits non-zero, so "seed ran" and "seed is consistent" are not the same
claim.

## Security

The key file must never be committed. `.gitignore` already excludes `*.json`
secrets by name, but keep the key outside the working tree anyway: a key that
lives in the repo leaks through one careless `git add -A`.

After seeding, revoke the key in the console (IAM → Service Accounts → Keys).
`node_modules/` is ignored.

## Crear una cuenta (Fase 2, bloque 1)

`create-user.mjs` crea una cuenta real en Firebase Auth **y** su documento
`users/{uid}` en una sola corrida. Se usa en lugar de un alta desde la app:
`createUserWithEmailAndPassword` desloguea a la directora, y en Spark no hay
Cloud Functions que hagan el alta desde un contexto privilegiado.

```powershell
cd tools/seed
npm run create-user -- --email=ana@aulasegura.com --password=secreto123 `
  --role=docente --first-name=Ana --last-name=López `
  --subjects=Matemáticas --group-ids=grp-3b
```

Opciones: `--email`, `--password`, `--role` (`docente` | `directivo` | `tutor`),
`--first-name`, `--last-name`, `--school-id`, `--phone`, `--subjects=a,b`,
`--group-ids=a,b`, `--student-ids=a,b`, `--custom-claim` y `--dry-run`.

Reglas del script:

- **Idempotente por email.** Si la cuenta ya existe, resuelve el `uid` y **no**
  cambia la contraseña. Si `users/{uid}` ya existe con la misma identidad, no
  escribe nada; si existe con otro rol o correo, falla en vez de sobrescribirlo.
- **Repara el documento faltante.** Una cuenta creada a mano en la consola que
  no tenga `users/{uid}` recibe el documento completo sin tocar su contraseña.
- **El rol de Firestore manda.** Las reglas leen `users/{uid}.role`, no un claim.
  `--custom-claim` escribe un claim igual como copia opcional, nunca como fuente.
- **Verifica leyendo.** Después de escribir, vuelve a leer Auth y Firestore y
  comprueba email, rol, `schoolId`, `fullName` y `active`. Un mismatch sale con
  código de error.

Probá primero con `--dry-run`: muestra el plan, resuelve si el email existe y
no escribe nada. Después de la tanda de altas, **revocá la llave** igual que
después del seed.

> No hace falta el claim para que la app funcione. Si se usa, tiene que escribirse
> siempre **desde** `users.{role}`, nunca al revés: dos fuentes de verdad para el
> rol es cómo aparece un directivo que la app no reconoce.

