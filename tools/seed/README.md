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
