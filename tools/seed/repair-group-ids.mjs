/**
 * AulaSegura — repair the two group-id vocabularies in Firestore.
 *
 * Why: `assignGroup()`/QR write canonical ids (`grp-3b`) while "Crear grupo"
 * writes a random document id. A teacher linked to the random doc and a student
 * in the canonical doc makes the teacher's justification inbox query return
 * zero documents, even though the principal (who reads by schoolId) sees and
 * approves the report.
 *
 * This script makes the canonical id win, WITHOUT deleting documents:
 *   - the canonical group doc is created/merged (never overwritten),
 *   - every group reference is rewritten from the random id to the canonical id,
 *   - teachers and tutors get the canonical id added to `users.groupIds`,
 *   - the old group doc stays and is marked with `mergedInto`.
 *
 * Safe to re-run. `groupIds` uses arrayUnion, so an existing canonical link is
 * a no-op. Nothing is written unless --write is passed.
 *
 * Usage:
 *   node repair-group-ids.mjs            # read-only report
 *   node repair-group-ids.mjs --write    # apply the repair
 *
 * Credentials: GOOGLE_APPLICATION_CREDENTIALS or ~/.aulasegura/firebase-adminsdk.json
 * (service-account JSON). If neither exists, gcloud ADC is used
 * (`gcloud auth application-default login`).
 */

import { readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

import { applicationDefault, cert, initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';

const SCHOOL_ID = 'sch-benito-juarez';
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'aulasegu';
const WRITE = process.argv.includes('--write');
const BATCH_LIMIT = 400;

const CANONICAL_ID = /^grp-[1-6][a-dA-D]$/i;

const SERVICE_ACCOUNT_PATH = join(homedir(), '.aulasegura', 'firebase-adminsdk.json');

function loadCredentials() {
  const explicitPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  const path = explicitPath || SERVICE_ACCOUNT_PATH;
  try {
    return cert(JSON.parse(readFileSync(path, 'utf8')));
  } catch (error) {
    if (error.code === 'ENOENT' && !explicitPath) {
      console.log(`Sin llave de servicio en ${path}; usando ADC de gcloud.`);
      return applicationDefault();
    }
    throw new Error(
      `No se pudo leer la llave de servicio en ${path}.\n` +
        'Descargala desde la consola o apuntá GOOGLE_APPLICATION_CREDENTIALS al archivo.',
      { cause: error },
    );
  }
}

/** Mirrors `groupIdFor()` in domain/model/SchoolCatalog.kt. */
function groupIdFor(grade, section) {
  return `grp-${grade}${String(section).trim().toLowerCase()}`;
}

function groupNameFor(grade, section) {
  return `${grade}° ${String(section).trim().toUpperCase()}`;
}

function asString(value) {
  return typeof value === 'string' ? value : '';
}

function stringList(value) {
  return Array.isArray(value) ? value.filter((item) => typeof item === 'string') : [];
}

function replaceId(values, from, to) {
  const result = values.filter((id) => id !== from);
  if (!result.includes(to)) result.push(to);
  return result;
}

/** One write per document: a batch must not update the same doc twice. */
function uniqueDocs(docs) {
  const seen = new Set();
  return docs.filter((doc) => {
    if (seen.has(doc.ref.path)) return false;
    seen.add(doc.ref.path);
    return true;
  });
}

/** All batches commit before the next one starts; a failure stops the repair. */
async function commitAll(db, operations) {
  for (let index = 0; index < operations.length; index += BATCH_LIMIT) {
    const batch = db.batch();
    for (const operation of operations.slice(index, index + BATCH_LIMIT)) {
      operation(batch);
    }
    await batch.commit();
  }
}

async function docsOf(query) {
  return (await query.get()).docs;
}

function collectionBySchool(db, name) {
  return db.collection(name).where('schoolId', '==', SCHOOL_ID);
}

async function main() {
  initializeApp({ credential: loadCredentials(), projectId: PROJECT_ID });
  const db = getFirestore();

  const groupDocs = await docsOf(collectionBySchool(db, 'groups'));
  const plans = [];

  for (const groupDoc of groupDocs) {
    const id = groupDoc.id;
    if (CANONICAL_ID.test(id)) continue;

    const grade = Number(groupDoc.get('grade'));
    const section = asString(groupDoc.get('section'));
    if (!Number.isInteger(grade) || !section) {
      console.log(`  ! ${id}: sin grade/section, no se puede derivar el id canónico`);
      continue;
    }

    const canonical = groupIdFor(grade, section);
    if (canonical === id) continue;

    const [students, justifications, sessions, incidents, tasks, notices, codes, teacherUsers, canonicalStudents] =
      await Promise.all([
        docsOf(collectionBySchool(db, 'students').where('groupId', '==', id)),
        docsOf(collectionBySchool(db, 'justifications').where('groupId', '==', id)),
        docsOf(collectionBySchool(db, 'attendanceSessions').where('groupId', '==', id)),
        docsOf(collectionBySchool(db, 'incidents').where('groupId', '==', id)),
        docsOf(collectionBySchool(db, 'tasks').where('groupIds', 'array-contains', id)),
        docsOf(collectionBySchool(db, 'notices').where('audience.groupIds', 'array-contains', id)),
        docsOf(collectionBySchool(db, 'linkingCodes').where('groupId', '==', id)),
        docsOf(db.collection('users').where('groupIds', 'array-contains', id)),
        docsOf(collectionBySchool(db, 'students').where('groupId', '==', canonical)),
      ]);

    // array-contains-any needs at least one value, and Firestore caps it at 10.
    // One batch of ten per group is enough for the demo; the dry-run prints who
    // was found so a missed tutor is visible before writing.
    const tutors = [];
    for (let start = 0; start < students.length; start += 10) {
      const slice = students.slice(start, start + 10).map((doc) => doc.id);
      tutors.push(
        ...(await docsOf(
          db.collection('users').where('schoolId', '==', SCHOOL_ID).where('studentIds', 'array-contains-any', slice),
        )),
      );
    }

    const canonicalDoc = await db.collection('groups').doc(canonical).get();
    const created = !canonicalDoc.exists;
    const canonicalTeachers = stringList(canonicalDoc.get('teacherIds'));
    const oldTeachers = stringList(groupDoc.get('teacherIds'));
    const teachers = [...new Set([...canonicalTeachers, ...oldTeachers])];

    const canonicalSubjects = (canonicalDoc.get('subjectTeachers') && typeof canonicalDoc.get('subjectTeachers') === 'object')
      ? canonicalDoc.get('subjectTeachers')
      : {};
    const oldSubjects = (groupDoc.get('subjectTeachers') && typeof groupDoc.get('subjectTeachers') === 'object')
      ? groupDoc.get('subjectTeachers')
      : {};
    const subjectTeachers = Object.entries(oldSubjects).filter(([key]) => !(key in canonicalSubjects));

    plans.push({
      id,
      canonical,
      created,
      teachers,
      subjectTeachers,
      studentCount: canonicalStudents.length + students.length,
      students,
      justifications,
      sessions,
      incidents,
      tasks,
      notices,
      codes,
      teacherUsers,
      tutors,
      name: groupNameFor(grade, section),
      grade,
      section,
      schoolId: SCHOOL_ID,
      cycle: asString(groupDoc.get('cycle')),
      titular: asString(groupDoc.get('titularTeacherId')),
    });
  }

  if (plans.length === 0) {
    console.log('No hay grupos con id no canónico. Nada que reparar.');
    return;
  }

  console.log(WRITE ? 'REPARANDO Firestore...' : 'DRY-RUN (no se escribe nada).');
  for (const plan of plans) {
    console.log(`\n${plan.id} -> ${plan.canonical} ${plan.created ? '(crear doc canónico)' : '(doc canónico ya existe)'}`);
    console.log(`  alumnos=${plan.students.length} justificaciones=${plan.justifications.length} sesiones=${plan.sessions.length}`);
    console.log(`  incidentes=${plan.incidents.length} tareas=${plan.tasks.length} avisos=${plan.notices.length} códigos=${plan.codes.length}`);
    console.log(`  docentes vinculados=${plan.teachers.length} usuarios con groupIds=${plan.teacherUsers.length} tutores=${plan.tutors.length}`);
  }

  if (!WRITE) {
    console.log('\nPara aplicar: node repair-group-ids.mjs --write');
    return;
  }

  const operations = [];

  for (const plan of plans) {
    const canonicalRef = db.collection('groups').doc(plan.canonical);

    if (plan.created) {
      operations.push((batch) =>
        batch.set(
          canonicalRef,
          {
            name: plan.name,
            grade: plan.grade,
            section: plan.section,
            schoolId: plan.schoolId,
            cycle: plan.cycle,
            studentCount: plan.studentCount,
            teacherIds: plan.teachers,
            createdAt: FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        ),
      );
    } else {
      const changes = {
        studentCount: plan.studentCount,
        updatedAt: FieldValue.serverTimestamp(),
      };
      if (plan.titular) changes.titularTeacherId = plan.titular;
      operations.push((batch) => batch.update(canonicalRef, changes));
      if (plan.teachers.length) {
        operations.push((batch) =>
          batch.update(canonicalRef, { teacherIds: FieldValue.arrayUnion(...plan.teachers) }),
        );
      }
      for (const [key, value] of plan.subjectTeachers) {
        operations.push((batch) => batch.update(canonicalRef, { [`subjectTeachers.${key}`]: value }));
      }
    }

    for (const user of uniqueDocs([...plan.teacherUsers, ...plan.tutors])) {
      operations.push((batch) =>
        batch.update(user.ref, { groupIds: FieldValue.arrayUnion(plan.canonical) }),
      );
    }

    for (const doc of plan.students) {
      operations.push((batch) =>
        batch.update(doc.ref, { groupId: plan.canonical, grupo: plan.name }),
      );
    }
    const groupIdCollections = {
      justifications: plan.justifications,
      sessions: plan.sessions,
      incidents: plan.incidents,
      codes: plan.codes,
    };
    for (const docs of Object.values(groupIdCollections)) {
      for (const doc of docs) {
        operations.push((batch) => batch.update(doc.ref, { groupId: plan.canonical }));
      }
    }
    for (const doc of plan.tasks) {
      operations.push((batch) =>
        batch.update(doc.ref, { groupIds: replaceId(stringList(doc.get('groupIds')), plan.id, plan.canonical) }),
      );
    }
    for (const doc of plan.notices) {
      const audience = doc.get('audience') && typeof doc.get('audience') === 'object' ? doc.get('audience') : {};
      operations.push((batch) =>
        batch.update(doc.ref, {
          'audience.groupIds': replaceId(stringList(audience.groupIds), plan.id, plan.canonical),
        }),
      );
    }

    // Non-destructive: the old doc stays so nothing disappears if a reference
    // was missed. Delete it by hand once the app has been verified.
    operations.push((batch) =>
      batch.update(db.collection('groups').doc(plan.id), {
        mergedInto: plan.canonical,
        updatedAt: FieldValue.serverTimestamp(),
      }),
    );
  }

  await commitAll(db, operations);
  console.log(`\nListo: ${operations.length} escrituras en ${plans.length} grupo(s).`);
  console.log('Verificá en la app y recién después borrá los docs viejos marcados con mergedInto.');
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
