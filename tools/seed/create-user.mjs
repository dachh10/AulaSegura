/**
 * AulaSegura — create a real account (Fase 2, bloque 1).
 *
 * Why a script and not the app: `createUserWithEmailAndPassword` signs the new
 * user in and logs the principal out. On Spark there are no Cloud Functions to
 * do this from a privileged context, so the Admin SDK is the only honest path.
 *
 * The script is idempotent by email:
 *   - If the Auth user already exists, its uid is resolved and NO password is
 *     changed. Re-running never resets a password someone already set.
 *   - If `users/{uid}` already exists, identity fields (role, email, active)
 *     are never overwritten. A conflicting role is reported as an error.
 *   - A missing `users/{uid}` for an existing Auth account is repaired: this is
 *     the case of an account created by hand in the console.
 *
 * The role in Firestore is the source of truth. The security rules read
 * `users/{uid}.role`, not a custom claim. `--custom-claim` writes a matching
 * claim as an optional optimisation; it is never read back as authority.
 *
 * Usage:
 *   node create-user.mjs \
 *     --email=nuevo@aulasegura.com --password=... \
 *     --role=docente --first-name=Ana --last-name=López \
 *     [--school-id=sch-benito-juarez] \
 *     [--subjects=Matemáticas,Ciencias] [--group-ids=grp-3b,grp-3a] \
 *     [--student-ids=stu-3b-01] [--phone=...] [--custom-claim] [--dry-run]
 */

import { readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

import { cert, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';

// ---------------------------------------------------------------- constants

const DEFAULT_SCHOOL_ID = 'sch-benito-juarez';
const ROLES = ['docente', 'directivo', 'tutor'];
const DRY_RUN = process.argv.includes('--dry-run');

const BOOLEAN_FLAGS = new Set(['help', 'custom-claim']);
const LIST_FLAGS = new Set(['subjects', 'group-ids', 'student-ids']);
const KNOWN_FLAGS = new Set([
  'email', 'password', 'role', 'first-name', 'last-name', 'school-id',
  'phone', 'subjects', 'group-ids', 'student-ids', 'help', 'custom-claim',
]);

function usage() {
  return `Crea una cuenta real de AulaSegura (Auth + users/{uid}).

Opciones:
  --email=...         Obligatorio. Correo de la cuenta nueva.
  --password=...      Obligatorio al crear (mínimo 6 caracteres).
  --role=...          Obligatorio: ${ROLES.join(' | ')}.
  --first-name=...    Obligatorio.
  --last-name=...     Obligatorio.
  --school-id=...     Por defecto ${DEFAULT_SCHOOL_ID}.
  --subjects=a,b      Solo docente. Materias que imparte.
  --group-ids=a,b     Docente: grupos que imparte. Tutor: grupos de sus hijos.
  --student-ids=a,b   Solo tutor. Hijos vinculados.
  --phone=...         Opcional.
  --custom-claim      Escribe custom claim { role } además del documento.
  --dry-run           Lee y muestra el plan; no crea ni escribe nada.

Ejemplo:
  node create-user.mjs --email=ana@aulasegura.com --password=secreto123 \\
    --role=docente --first-name=Ana --last-name=López \\
    --subjects=Matemáticas --group-ids=grp-3b --dry-run`;
}

// ------------------------------------------------------------------- helpers

/** Mirrors tools/seed/seed.mjs on purpose: keep the verified seed untouched. */
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
  } catch {
    throw new Error(
      `No se encontró la llave de servicio en ${path}.\n` +
        'Descargala desde la consola o apuntá GOOGLE_APPLICATION_CREDENTIALS al archivo.',
    );
  }
  return cert(JSON.parse(raw));
}

function parseArgs(argv) {
  const args = {};
  for (const token of argv) {
    if (token === '--dry-run') continue;
    if (!token.startsWith('--')) {
      throw new Error(`Argumento inesperado: ${token}`);
    }
    const eq = token.indexOf('=');
    const name = eq === -1 ? token.slice(2) : token.slice(2, eq);
    const value = eq === -1 ? 'true' : token.slice(eq + 1);
    if (!KNOWN_FLAGS.has(name)) {
      throw new Error(`Opción desconocida: --${name}`);
    }
    args[name] = value;
  }
  if (args.help) return { help: true };
  return args;
}

function parseList(value) {
  if (!value) return [];
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function validate(args) {
  const errors = [];
  const required = ['email', 'role', 'first-name', 'last-name'];
  for (const name of required) {
    if (!args[name]) errors.push(`Falta --${name}`);
  }
  if (!DRY_RUN && !args.password) errors.push('Falta --password');
  if (args.password && args.password.length < 6) {
    errors.push('La contraseña debe tener al menos 6 caracteres');
  }
  if (args.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(args.email)) {
    errors.push('El correo no tiene un formato válido');
  }
  if (args.role && !ROLES.includes(args.role)) {
    errors.push(`El rol debe ser uno de: ${ROLES.join(', ')}`);
  }
  for (const name of LIST_FLAGS) {
    if (name !== 'subjects' && args[name] && parseList(args[name]).length === 0) {
      errors.push(`--${name} quedó vacío después de limpiar la lista`);
    }
  }
  return errors;
}

/** Full document for a brand-new account. Never used to overwrite an existing one. */
function buildUserDocument({ uid, email, role, firstName, lastName, args }) {
  const doc = {
    uid,
    email,
    role,
    firstName,
    lastName,
    fullName: `${firstName} ${lastName}`,
    schoolId: args['school-id'] || DEFAULT_SCHOOL_ID,
    active: true,
    preferences: { notificationsEnabled: true },
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  };
  if (args.phone) doc.phone = args.phone;

  // These are denormalised because the rules cannot walk a relationship.
  // groupIds is what lets a teacher see their groups and a tutor read tasks
  // addressed to their children's groups.
  if (role === 'docente') {
    doc.subjects = parseList(args.subjects);
    doc.groupIds = parseList(args['group-ids']);
  }
  if (role === 'tutor') {
    doc.studentIds = parseList(args['student-ids']);
    doc.groupIds = parseList(args['group-ids']);
  }
  return doc;
}

function printPlan(args) {
  const role = args.role;
  console.log('Plan (dry-run, no se escribe nada):');
  console.log(`  email:      ${args.email}`);
  console.log(`  role:       ${role}`);
  console.log(`  nombre:     ${args['first-name']} ${args['last-name']}`);
  console.log(`  schoolId:   ${args['school-id'] || DEFAULT_SCHOOL_ID}`);
  if (role === 'docente') {
    console.log(`  subjects:   ${JSON.stringify(parseList(args.subjects))}`);
  }
  if (role === 'tutor') {
    console.log(`  studentIds: ${JSON.stringify(parseList(args['student-ids']))}`);
  }
  if (role !== 'directivo') {
    console.log(`  groupIds:   ${JSON.stringify(parseList(args['group-ids']))}`);
  }
  console.log(`  claim:      ${args['custom-claim'] ? 'sí' : 'no (la regla lee users.{role})'}`);
}

// --------------------------------------------------------------------- main

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    console.log(usage());
    return;
  }

  const errors = validate(args);
  if (errors.length > 0) {
    throw new Error(errors.join('\n'));
  }

  const email = args.email.toLowerCase();
  const role = args.role;

  initializeApp({ credential: loadCredentials() });
  const auth = getAuth();
  const db = getFirestore();

  // Resolve by email first. Never change a password on a re-run.
  let existingAuth = null;
  try {
    existingAuth = await auth.getUserByEmail(email);
  } catch (error) {
    if (error.code !== 'auth/user-not-found') throw error;
  }

  let uid = existingAuth?.uid ?? null;
  let existingDoc = null;
  if (uid) {
    const snap = await db.collection('users').doc(uid).get();
    if (snap.exists) existingDoc = snap.data();
  }

  if (DRY_RUN) {
    console.log(
      existingAuth
        ? `La cuenta ya existe en Auth: ${uid}`
        : 'La cuenta no existe en Auth: se crearía con este plan.',
    );
    if (existingDoc) {
      console.log(`users/{uid} ya existe con role=${existingDoc.role}; no se sobrescribiría.`);
    }
    printPlan(args);
    return;
  }

  if (existingDoc) {
    // Identity is immutable from the client and must stay immutable here.
    if (existingDoc.role !== role || existingDoc.email !== email) {
      throw new Error(
        `users/${uid} ya existe con role=${existingDoc.role} y email=${existingDoc.email}. ` +
          'No se sobrescribió: resolvé el conflicto a mano antes de seguir.',
      );
    }
    console.log(`users/${uid} ya existe con la identidad pedida. Sin cambios.`);
  } else {
    if (!uid) {
      const created = await auth.createUser({
        email,
        password: args.password,
        displayName: `${args['first-name']} ${args['last-name']}`,
      });
      uid = created.uid;
      console.log(`Auth: cuenta creada ${uid}`);
    } else {
      console.log(`Auth: cuenta existente ${uid}, documento ausente → reparando`);
    }

    await db.collection('users').doc(uid).set(
      buildUserDocument({
        uid,
        email,
        role,
        firstName: args['first-name'],
        lastName: args['last-name'],
        args,
      }),
      { merge: true },
    );
    console.log(`Firestore: users/${uid} escrito`);

    if (args['custom-claim']) {
      await auth.setCustomUserClaims(uid, { role });
      console.log(`Custom claim { role: ${role} } escrito (copia, no autoridad)`);
    }
  }

  // Read-back: a script that prints "listo" without reading is a script that lies.
  console.log('\nVerificación');
  const finalAuth = await auth.getUser(uid);
  const finalDoc = await db.collection('users').doc(uid).get();
  if (!finalDoc.exists) throw new Error(`FALTA users/${uid} después de la escritura`);
  const data = finalDoc.data();

  const checks = [
    ['email', finalAuth.email === email],
    ['role', data.role === role],
    ['schoolId', data.schoolId === (args['school-id'] || DEFAULT_SCHOOL_ID)],
    ['fullName', data.fullName === `${args['first-name']} ${args['last-name']}`],
    ['active', data.active === true],
  ];
  let problems = 0;
  for (const [name, ok] of checks) {
    if (!ok) problems++;
    console.log(`  ${name.padEnd(9)} ${ok ? 'OK' : 'MISMATCH'}`);
  }
  if (problems > 0) {
    throw new Error(`${problems} problema(s) en la verificación`);
  }
  console.log(`\nListo: ${email} -> ${uid} (${role})`);
}

main().catch((error) => {
  console.error(`\nFALLÓ: ${error.message}`);
  process.exitCode = 1;
});
