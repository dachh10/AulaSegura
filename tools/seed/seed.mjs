/**
 * AulaSegura — Firestore seed (Fase 0, bloque K).
 *
 * Why a script and not the app: the demo needs a school, four groups, a full
 * roster and the links between them before any screen can show real data. The
 * Admin SDK bypasses security rules on purpose — a client cannot write another
 * user's `studentIds`, and the linking flow that will do it later does not exist
 * yet.
 *
 * The script is idempotent and safe to re-run:
 *   - Documents are merged, not replaced.
 *   - `users/{uid}` identity fields (role, fullName, phone, photo) are never
 *     touched. Only the link fields are written, because the accounts already
 *     exist and were created by the auth flow.
 *   - `createdAt` is set on creation and left alone afterwards.
 *
 * Usage:
 *   node seed.mjs            # writes
 *   node seed.mjs --dry-run  # reads and reports, writes nothing
 */

import { readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

import { cert, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';

// ---------------------------------------------------------------- constants

const SCHOOL_ID = 'sch-benito-juarez';
const CYCLE = '2026-2027';
const DRY_RUN = process.argv.includes('--dry-run');

/** The three demo accounts created by the auth flow. Resolved by email. */
const USER_EMAILS = {
  docente: 'docente@aulasegura.com',
  directivo: 'directora@escuela.mx',
  tutor: 'carlos.gomez@familia.mx',
};

const GROUP_3B = 'grp-3b';
const GROUP_3A = 'grp-3a';
const GROUP_2C = 'grp-2c';
const GROUP_1A = 'grp-1a';

// ------------------------------------------------------------------- school

/**
 * Fields the seed owns. `address`, `schedule` and `requireLocation` are only
 * written when the document is created: if someone already adjusted them in the
 * console, re-running the seed must not undo it.
 */
const SCHOOL_FIELDS = {
  name: 'Escuela Primaria "Benito Juárez"',
  key: '15EPR0123X',
  cycle: CYCLE,
  location: { lat: 19.2926, lng: -99.6557 },
  radiusMeters: 150,
};

const SCHOOL_CREATION_FIELDS = {
  address: 'Av. Benito Juárez 123, Col. Centro, Toluca, Estado de México',
  schedule: { entryTime: '08:00', exitTime: '14:00' },
  requireLocation: true,
};

// ------------------------------------------------------------------ roster
//
// 3° B is the group from the mockup: the six names the screens already show,
// then a plausible rest to reach the 28 the school actually has. A seed with
// six students would not exercise pagination, search or the absence tally.

const ROSTER_3B = [
  ['Ana Sofía', 'Aguilar'],
  ['Bruno', 'Castillo'],
  ['Camila', 'Domínguez'],
  ['Diego', 'Escobar'],
  ['Elena', 'Fuentes'],
  ['Mariana', 'Gómez Ruiz'],
  ['Fernando', 'Herrera'],
  ['Gabriela', 'Ibarra'],
  ['Hugo', 'Jiménez'],
  ['Isabela', 'Lara'],
  ['Joaquín', 'Medina'],
  ['Karina', 'Núñez'],
  ['Leonardo', 'Ortiz'],
  ['Lucía', 'Pacheco'],
  ['Mateo', 'Quintero'],
  ['Natalia', 'Ramírez'],
  ['Óscar', 'Reyes'],
  ['Paula', 'Ríos'],
  ['Rodrigo', 'Salazar'],
  ['Sofía', 'Sandoval'],
  ['Tomás', 'Serrano'],
  ['Valeria', 'Solís'],
  ['Ximena', 'Tapia'],
  ['Yamileth', 'Torres'],
  ['Zoe', 'Urbina'],
  ['Andrés', 'Valdés'],
  ['Bárbara', 'Vargas'],
  ['Cristóbal', 'Zamora'],
];

/**
 * Names for the groups that exist only to give the teacher and the principal
 * something realistic to navigate. Generated from two pools instead of typed
 * out: they are filler, and a fixed formula keeps re-runs identical.
 */
const FIRST_NAMES = [
  'Adrián', 'Alondra', 'Ángel', 'Aurora', 'Benjamín', 'Brenda', 'César',
  'Cecilia', 'Damián', 'Daniela', 'Emiliano', 'Estefanía', 'Fabiola', 'Felipe',
  'Gael', 'Gloria', 'Héctor', 'Irene', 'Iván', 'Jazmín', 'Julián', 'Lorena',
  'Mauricio', 'Mía', 'Nicolás', 'Noemí', 'Pablo', 'Regina', 'Santiago', 'Teresa',
];

const LAST_NAMES = [
  'Acosta', 'Bautista', 'Beltrán', 'Cabrera', 'Cano', 'Cervantes', 'Cordero',
  'Delgado', 'Duarte', 'Espinoza', 'Estrada', 'Figueroa', 'Galván', 'Guerrero',
  'Hernández', 'León', 'Maldonado', 'Márquez', 'Mendoza', 'Montes', 'Navarro',
  'Ochoa', 'Padilla', 'Peña', 'Ponce', 'Rosales', 'Ruiz', 'Sánchez', 'Trejo',
  'Vega',
];

/**
 * Deterministic filler names. The two different strides keep the pool from
 * producing "Acosta Acosta" pairs.
 */
function generatedRoster(size) {
  const names = [];
  for (let i = 0; i < size; i++) {
    names.push([
      FIRST_NAMES[i % FIRST_NAMES.length],
      `${LAST_NAMES[(i * 7) % LAST_NAMES.length]} ${LAST_NAMES[(i * 11 + 3) % LAST_NAMES.length]}`,
    ]);
  }
  return names;
}

// --------------------------------------------------------------- data model

/**
 * Puts a specific student at a 1-based list position, so a named child and the
 * generated filler around them stay in the same roster.
 */
function replaceAt(roster, listNumber, name) {
  const copy = [...roster];
  copy[listNumber - 1] = name;
  return copy;
}

const GROUPS = [
  {
    id: GROUP_3B,
    name: '3° B',
    grade: 3,
    section: 'B',
    titular: true,
    roster: ROSTER_3B,
  },
  {
    id: GROUP_3A,
    name: '3° A',
    grade: 3,
    section: 'A',
    titular: false,
    roster: generatedRoster(30),
  },
  {
    id: GROUP_2C,
    name: '2° C',
    grade: 2,
    section: 'C',
    titular: false,
    roster: generatedRoster(27),
  },
  {
    id: GROUP_1A,
    name: '1° A',
    grade: 1,
    section: 'A',
    titular: false,
    // The tutor's younger child. Replacing the generated name at his position
    // keeps the list length honest and the id stable.
    roster: replaceAt(generatedRoster(26), 7, ['Diego', 'Gómez Ruiz']),
  },
];

/** The tutor's children, by group and list position (1-based). */
const CHILDREN = [
  { groupId: GROUP_3B, listNumber: 6 }, // Mariana Gómez Ruiz
  { groupId: GROUP_1A, listNumber: 7 },
];

// ------------------------------------------------------------------ helpers

function credentialsPath() {
  const fromEnv = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (fromEnv) return fromEnv;
  return join(homedir(), '.aulasegura', 'firebase-adminsdk.json');
}

function loadCredentials() {
  const path = credentialsPath();
  let raw;
  try {
    raw = readFileSync(path, 'utf8');
  } catch (error) {
    throw new Error(
      `No se encontró la llave de servicio en ${path}.\n` +
        'Descargala desde la consola o apuntá GOOGLE_APPLICATION_CREDENTIALS al archivo.',
    );
  }
  return cert(JSON.parse(raw));
}

function studentId(groupId, listNumber) {
  // Readable and stable: re-running never creates a second Ana Sofía.
  return `stu-${groupId.replace(/^grp-/, '')}-${String(listNumber).padStart(2, '0')}`;
}

function fullName([first, last]) {
  return `${first} ${last}`;
}

function chunks(items, size) {
  const out = [];
  for (let i = 0; i < items.length; i += size) out.push(items.slice(i, i + size));
  return out;
}

/**
 * Merge `fields` into `ref`, adding `creationFields` only on a first write.
 *
 * `merge: true` is what makes the seed re-runnable: re-running must correct the
 * links it owns without erasing anything else the app or the console added.
 */
async function upsert(ref, fields, creationFields = {}) {
  const snap = await ref.get();
  const payload = snap.exists
    ? { ...fields, updatedAt: FieldValue.serverTimestamp() }
    : {
        ...creationFields,
        ...fields,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      };

  if (!DRY_RUN) await ref.set(payload, { merge: true });
  return snap.exists ? 'updated' : 'created';
}

async function writeAll(items, label) {
  let created = 0;
  let updated = 0;

  for (const chunk of chunks(items, 20)) {
    const results = await Promise.all(chunk.map((item) => item()));
    for (const result of results) {
      if (result === 'created') created++;
      else updated++;
    }
  }

  const verb = DRY_RUN ? 'verificados' : 'escritos';
  console.log(`  ${label}: ${created} nuevos, ${updated} existentes (${verb})`);
}

// --------------------------------------------------------------------- main

async function main() {
  initializeApp({ credential: loadCredentials() });

  const db = getFirestore();
  const auth = getAuth();

  if (DRY_RUN) console.log('Modo --dry-run: no se escribe nada.\n');

  // --- users -------------------------------------------------------------
  //
  // The accounts were created by the auth flow, so they must exist. Failing
  // here is better than seeding a school nobody can sign in to.

  const uids = {};
  for (const [role, email] of Object.entries(USER_EMAILS)) {
    try {
      const record = await auth.getUserByEmail(email);
      uids[role] = record.uid;
      console.log(`  ${role.padEnd(9)} ${email} -> ${record.uid}`);
    } catch {
      throw new Error(
        `No existe la cuenta ${email}. Abrí la app e iniciá sesión con ella antes de sembrar.`,
      );
    }
  }

  const docenteId = uids.docente;
  const tutorId = uids.tutor;

  // --- students ----------------------------------------------------------
  //
  // Built first because the tutor's `studentIds` and each group's `studentCount`
  // are read off the roster, not typed twice.

  const students = [];
  const studentIdsByGroup = new Map();

  for (const group of GROUPS) {
    const ids = [];
    group.roster.forEach((name, index) => {
      const listNumber = index + 1;
      const id = studentId(group.id, listNumber);
      ids.push(id);
      students.push({
        id,
        groupId: group.id,
        listNumber,
        firstName: name[0],
        lastName: name[1],
        fullName: fullName(name),
      });
    });
    studentIdsByGroup.set(group.id, ids);
  }

  const childIds = CHILDREN.map((child) => studentId(child.groupId, child.listNumber));
  const childGroups = [...new Set(CHILDREN.map((child) => child.groupId))];

  // --- school ------------------------------------------------------------

  console.log('\nPlantel');
  await writeAll(
    [
      () =>
        upsert(
          db.collection('schools').doc(SCHOOL_ID),
          SCHOOL_FIELDS,
          SCHOOL_CREATION_FIELDS,
        ),
    ],
    'schools',
  );

  // --- groups ------------------------------------------------------------

  console.log('\nGrupos');
  await writeAll(
    GROUPS.map((group) => () => {
      const fields = {
        name: group.name,
        grade: group.grade,
        section: group.section,
        schoolId: SCHOOL_ID,
        // 1° A exists for the tutor's child; no teacher is assigned to it yet.
        teacherIds: group.id === GROUP_1A ? [] : [docenteId],
        studentCount: group.roster.length,
        cycle: CYCLE,
      };

      // Only 3° B has a titular: the other groups are taught but not chaired by
      // the demo teacher, and inventing more titulars would imply staff that the
      // seed does not create.
      if (group.titular) fields.titularTeacherId = docenteId;

      return upsert(db.collection('groups').doc(group.id), fields);
    }),
    'groups',
  );

  // --- students ----------------------------------------------------------

  console.log('\nAlumnos');
  await writeAll(
    students.map((student) => () => {
      const fields = {
        firstName: student.firstName,
        lastName: student.lastName,
        fullName: student.fullName,
        listNumber: student.listNumber,
        groupId: student.groupId,
        schoolId: SCHOOL_ID,
        active: true,
      };

      // `tutorIds` starts empty and only the demo children are linked. Writing
      // `[]` on every student would erase a link added later by the real flow.
      const creationFields = { tutorIds: [] };
      if (childIds.includes(student.id)) fields.tutorIds = [tutorId];

      return upsert(
        db.collection('students').doc(student.id),
        fields,
        creationFields,
      );
    }),
    'students',
  );

  // --- user links --------------------------------------------------------
  //
  // Only the link fields. `role`, `fullName`, `email` and `active` were set by
  // the auth flow and are not the seed's business.

  console.log('\nVínculos');
  await writeAll(
    [
      () =>
        upsert(db.collection('users').doc(docenteId), {
          schoolId: SCHOOL_ID,
          subjects: ['Matemáticas'],
          groupIds: [GROUP_3B, GROUP_3A, GROUP_2C],
        }),
      () =>
        upsert(db.collection('users').doc(uids.directivo), {
          schoolId: SCHOOL_ID,
        }),
      () =>
        upsert(db.collection('users').doc(tutorId), {
          schoolId: SCHOOL_ID,
          // Rules cannot walk studentIds -> groupId, so the tutor's groups are
          // denormalised here. Without this the tutor cannot read a task or a
          // notice addressed to their children's group.
          groupIds: childGroups,
          studentIds: childIds,
        }),
    ],
    'users',
  );

  // --- dry-run ------------------------------------------------------------
  //
  // The read-back below compares what is stored against what the seed intends,
  // so it can only pass after a real write. In dry-run the plan is the output.

  if (DRY_RUN) {
    console.log('\nPlan');
    for (const group of GROUPS) {
      console.log(`  ${String(group.name).padEnd(5)} ${group.roster.length} alumnos`);
    }
    console.log(
      `  tutor: ${childIds.join(', ')} en ${childGroups.join(', ')}`,
    );
    console.log('\nDry-run completo: no se escribió nada.');
    return;
  }

  // --- read-back ---------------------------------------------------------
  //
  // A seed that reports success without reading back is a seed that lies. These
  // reads are also what a human would check in the console, so they are printed
  // in the same shape.

  console.log('\nVerificación');

  const school = await db.collection('schools').doc(SCHOOL_ID).get();
  console.log(
    school.exists
      ? `  school: ${school.get('name')} (${school.get('key')}), radio ${school.get('radiusMeters')} m`
      : '  school: FALTA',
  );

  let problems = 0;

  for (const group of GROUPS) {
    const doc = await db.collection('groups').doc(group.id).get();
    const snapshot = await db
      .collection('students')
      .where('groupId', '==', group.id)
      .get();

    const declared = doc.exists ? doc.get('studentCount') : -1;
    const actual = snapshot.size;
    const ok = declared === actual;
    if (!ok) problems++;

    console.log(
      `  ${String(group.name).padEnd(5)} alumnos=${actual} studentCount=${declared} ${ok ? 'OK' : 'MISMATCH'}`,
    );
  }

  const docente = await db.collection('users').doc(docenteId).get();
  const tutor = await db.collection('users').doc(tutorId).get();

  console.log(
    `  docente: groupIds=${JSON.stringify(docente.get('groupIds') ?? [])} subjects=${JSON.stringify(docente.get('subjects') ?? [])}`,
  );
  console.log(
    `  tutor:   groupIds=${JSON.stringify(tutor.get('groupIds') ?? [])} studentIds=${JSON.stringify(tutor.get('studentIds') ?? [])}`,
  );

  for (const childId of childIds) {
    const child = await db.collection('students').doc(childId).get();
    const linked = (child.get('tutorIds') ?? []).includes(tutorId);
    if (!linked) problems++;
    console.log(
      `  ${childId}: ${child.get('fullName') ?? 'FALTA'} tutorIds=${linked ? 'OK' : 'MISSING'}`,
    );
  }

  if (problems > 0) {
    console.error(`\n${problems} problema(s) en la verificación.`);
    process.exitCode = 1;
    return;
  }

  console.log(DRY_RUN ? '\nDry-run completo: la semilla está consistente.' : '\nSemilla lista.');
}

main().catch((error) => {
  console.error(`\nFALLÓ: ${error.message}`);
  process.exitCode = 1;
});
