#!/usr/bin/env node
'use strict';

/**
 * Un aviso, dos canales, una corrida.
 *
 * En Spark no hay Cloud Functions, así que la bandeja (`notifications`) y el push
 * (FCM) nunca se enteran el uno del otro. Este script hace las dos cosas y evita
 * el error de siempre: escribir el documento y olvidar el mensaje (o al revés).
 * El envío es SIEMPRE manual: no existe "notificar cuando pase X" y no se debe
 * diseñar. Una persona dispara este script (o usa la consola de Firebase).
 *
 *   node scripts/notify/index.js --uid=<uid> --title="Junta de padres" \
 *     --body="El viernes a las 8:00" --type=aviso --deep-link=teacher/history
 *
 *   node scripts/notify/index.js --school-id=<id> --role=docente \
 *     --title="Consejo escolar" --body="Sesión extraordinaria" --type=aviso
 *
 * Necesita una cuenta de servicio: Firebase Console → *Project settings* →
 * *Service accounts* → *Generate new private key*, guardada como
 * `scripts/notify/service-account.json` (ignorada por git) o en la ruta que diga
 * `--credentials` / `GOOGLE_APPLICATION_CREDENTIALS`. Nunca se versiona un secreto.
 *
 * Opciones:
 *   --uid               uid del destinatario (modo individual, como siempre)
 *   --school-id         schoolId para resolver destinatarios en users (modo masivo)
 *   --role              docente | directivo | tutor | all (combinable con --school-id;
 *                       "all" = sin filtro de rol). Requiere --school-id o --role.
 *   --title             título; también es el título de la notificación
 *   --body              cuerpo
 *   --type              asistencia | incidente | tarea | aviso | mensaje | sistema | otro
 *   --deep-link         ruta interna permitida (ver navigation/DeepLinks.kt):
 *                       teacher/... | admin/... | parent/...
 *   --no-push           solo escribe el documento (útil para llenar la bandeja)
 *   --credentials       ruta de la cuenta de servicio
 *   --dry-run           imprime lo que haría, sin escribir ni enviar
 *
 * Si se da --uid, gana y se ignora --school-id/--role. Si no, se consultan los
 * uids en users con where("schoolId","==",schoolId) y, si aplica,
 * where("role","==",role). Se escribe un documento por destinatario y se envía
 * el push a cada una de sus fcmTokens; los tokens muertos se borran.
 */

const fs = require('node:fs');
const path = require('node:path');

const TYPES = ['asistencia', 'incidente', 'tarea', 'aviso', 'mensaje', 'sistema', 'otro'];
const ROLES = ['docente', 'directivo', 'tutor', 'all'];

/** Parsea `--clave=valor` y `--clave valor`; los `--flag` quedan en `true`. */
function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (!arg.startsWith('--')) continue;

    const [rawKey, inlineValue] = arg.slice(2).split(/=(.*)/s);
    const key = rawKey.trim();
    if (inlineValue !== undefined) {
      args[key] = inlineValue;
    } else if (argv[i + 1] && !argv[i + 1].startsWith('--')) {
      args[key] = argv[i + 1];
      i += 1;
    } else {
      args[key] = true;
    }
  }
  return args;
}

function fail(message) {
  console.error(`\n  ${message}\n`);
  process.exit(1);
}

function resolveCredentials(args) {
  const provided = args.credentials || process.env.GOOGLE_APPLICATION_CREDENTIALS;
  const candidates = [
    provided,
    path.join(__dirname, 'service-account.json'),
    path.join(__dirname, 'service-account.json.json'),
  ].filter(Boolean);

  for (const candidate of candidates) {
    const resolved = path.resolve(candidate);
    if (fs.existsSync(resolved)) return resolved;
  }

  fail(
    'No encontré la cuenta de servicio.\n' +
      '  Descárgala en Firebase Console → Project settings → Service accounts →\n' +
      '  "Generate new private key" y guárdala como scripts/notify/service-account.json,\n' +
      '  o pásala con --credentials=<ruta>.',
  );
}

/** One notification document plus one push, for a single recipient. */
async function notifyUser(db, messaging, admin, uid, fields, sendPush) {
  const document = {
    userId: uid,
    title: fields.title,
    body: fields.body,
    type: fields.type,
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    ...(fields.deepLink ? { deepLink: fields.deepLink } : {}),
  };

  // 1. La bandeja. El id sale de Firestore y viaja en el push, para que el tap
  //    pueda marcar leído el documento exacto.
  const reference = await db.collection('notifications').add(document);
  console.log(`Documento creado: notifications/${reference.id} (${uid})`);

  if (!sendPush) return;

  // 2. El push, a todos los teléfonos registrados de ese usuario.
  const tokensSnapshot = await db.collection('users').doc(uid).collection('fcmTokens').get();
  const tokens = tokensSnapshot.docs
    .map((doc) => doc.get('token') || doc.id)
    .filter((token) => typeof token === 'string' && token.length > 0);

  if (tokens.length === 0) {
    console.warn(
      `No hay tokens en users/${uid}/fcmTokens: el documento quedó en la bandeja, pero\n` +
        'nadie recibirá el push. Ese usuario debe abrir la app en su teléfono al menos una vez.',
    );
    return;
  }

  const response = await messaging.sendEachForMulticast({
    tokens,
    // Solo `data`: el SDK de Android muestra la notificación de sistema él mismo
    // cuando el payload trae `notification`, y entonces el tap perdería el deepLink.
    data: {
      title: fields.title,
      body: fields.body,
      notificationId: reference.id,
      ...(fields.deepLink ? { deepLink: fields.deepLink } : {}),
    },
    android: { priority: 'high' },
  });

  console.log(
    `Push a ${uid}: ${tokens.length} token(s), ${response.successCount} ok, ` +
      `${response.failureCount} con error.`,
  );

  // 3. Un token muerto es basura: se borra para no volver a intentarlo.
  const deadCodes = new Set([
    'messaging/registration-token-not-registered',
    'messaging/invalid-registration-token',
    'messaging/invalid-argument',
  ]);
  const stale = [];
  response.responses.forEach((result, index) => {
    if (result.success) return;
    const code = result.error?.code;
    console.warn(`  token ${tokens[index].slice(0, 12)}… → ${code}`);
    if (deadCodes.has(code)) stale.push(tokens[index]);
  });

  if (stale.length > 0) {
    const batch = db.batch();
    stale.forEach((token) =>
      batch.delete(db.collection('users').doc(uid).collection('fcmTokens').doc(token)),
    );
    await batch.commit();
    console.log(`Tokens inválidos eliminados en ${uid}: ${stale.length}.`);
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));

  if (args.help || args.h) {
    const source = fs.readFileSync(__filename, 'utf8');
    console.log(source.slice(source.indexOf('/**'), source.indexOf('*/') + 2));
    return;
  }

  const uid = typeof args.uid === 'string' ? args.uid.trim() : '';
  const schoolId = typeof args['school-id'] === 'string' ? args['school-id'].trim() : '';
  const role = typeof args.role === 'string' ? args.role.trim() : '';
  const title = typeof args.title === 'string' ? args.title.trim() : '';
  const body = typeof args.body === 'string' ? args.body.trim() : '';

  if (!title) fail('Falta --title: si el documento no tiene título, la bandeja lo descarta.');
  if (role && !ROLES.includes(role)) fail(`--role debe ser uno de: ${ROLES.join(', ')}.`);
  if (!uid && !schoolId && !role) {
    fail('Falta --uid o, en su lugar, --school-id y/o --role para elegir destinatarios.');
  }
  if (uid && (schoolId || role)) {
    console.warn('Aviso: con --uid se ignoran --school-id y --role (modo individual).');
  }

  const type = typeof args.type === 'string' ? args.type : 'aviso';
  if (!TYPES.includes(type)) {
    fail(`--type debe ser uno de: ${TYPES.join(', ')}.`);
  }

  const deepLink = typeof args['deep-link'] === 'string' ? args['deep-link'].trim() : null;
  const dryRun = Boolean(args['dry-run']);
  const sendPush = !args['no-push'];

  const fields = { title, body, type, deepLink };

  // Antes de pedir credenciales: un ensayo no debe exigir la cuenta de servicio.
  if (dryRun) {
    const sampleUid = uid || '<uid resuelto de users>';
    console.log('dry-run: documento que se escribiría en notifications:');
    console.log(
      JSON.stringify(
        {
          userId: sampleUid,
          title,
          body,
          type,
          read: false,
          createdAt: '<serverTimestamp>',
          ...(deepLink ? { deepLink } : {}),
        },
        null,
        2,
      ),
    );
    if (uid) {
      console.log(
        sendPush
          ? `dry-run: se enviaría push a los tokens de users/${uid}/fcmTokens.`
          : 'dry-run: sin push (--no-push).',
      );
    } else {
      const filters = [
        schoolId ? `schoolId == "${schoolId}"` : null,
        role && role !== 'all' ? `role == "${role}"` : null,
      ].filter(Boolean);
      console.log(
        `dry-run: se resolverían los uids en users con ${filters.join(' y ') || 'sin filtros'}.`,
      );
      console.log(
        sendPush
          ? 'dry-run: un documento y un push por destinatario, en una sola corrida.'
          : 'dry-run: sin push (--no-push).',
      );
    }
    return;
  }

  const credentialsPath = resolveCredentials(args);

  const admin = require('firebase-admin');
  admin.initializeApp({
    credential: admin.credential.cert(require(credentialsPath)),
  });

  const db = admin.firestore();
  const messaging = admin.messaging();

  // --uid gana y mantiene el comportamiento de siempre. Si no, se resuelven los
  // destinatarios con los filtros pedidos; un documento por usuario, porque
  // notifications.userId es por destinatario.
  let recipients;
  if (uid) {
    recipients = [uid];
  } else {
    let query = db.collection('users');
    if (schoolId) query = query.where('schoolId', '==', schoolId);
    if (role && role !== 'all') query = query.where('role', '==', role);
    const snapshot = await query.get();
    recipients = snapshot.docs.map((doc) => doc.id).filter(Boolean).sort();
    console.log(`Destinatarios resueltos: ${recipients.length}.`);
  }

  if (recipients.length === 0) {
    fail('No hay usuarios que coincidan con los filtros; no se escribió nada.');
  }

  const failures = [];
  for (const recipient of recipients) {
    try {
      await notifyUser(db, messaging, admin, recipient, fields, sendPush);
    } catch (error) {
      failures.push(recipient);
      console.error(`Error con ${recipient}: ${error.message || error}`);
    }
  }

  console.log(`Corrida terminada: ${recipients.length - failures.length}/${recipients.length} ok.`);
  if (failures.length > 0) process.exitCode = 1;
}

main().catch((error) => {
  console.error('\n  Error:', error.message || error, '\n');
  process.exit(1);
});
