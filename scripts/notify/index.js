#!/usr/bin/env node
'use strict';

/**
 * Un aviso, dos canales, una corrida.
 *
 * En Spark no hay Cloud Functions, así que la bandeja (`notifications`) y el push
 * (FCM) nunca se enteran el uno del otro. Este script hace las dos cosas y evita
 * el error de siempre: escribir el documento y olvidar el mensaje (o al revés).
 *
 *   node scripts/notify/index.js --uid=<uid> --title="Junta de padres" \
 *     --body="El viernes a las 8:00" --type=aviso --deep-link=teacher/history
 *
 * Necesita una cuenta de servicio: Firebase Console → *Project settings* →
 * *Service accounts* → *Generate new private key*, guardada como
 * `scripts/notify/service-account.json` (ignorada por git) o en la ruta que diga
 * `--credentials` / `GOOGLE_APPLICATION_CREDENTIALS`.
 *
 * Opciones:
 *   --uid               uid del destinatario (obligatorio)
 *   --title             título; también es el título de la notificación
 *   --body              cuerpo
 *   --type              asistencia | incidente | tarea | aviso | mensaje | sistema | otro
 *   --deep-link         ruta interna: teacher/home | teacher/groups | teacher/history
 *                       | teacher/notifications | teacher/profile | teacher/chat/<id>
 *   --no-push           solo escribe el documento (útil para llenar la bandeja)
 *   --credentials       ruta de la cuenta de servicio
 *   --dry-run           imprime lo que haría, sin escribir ni enviar
 */

const fs = require('node:fs');
const path = require('node:path');

const TYPES = ['asistencia', 'incidente', 'tarea', 'aviso', 'mensaje', 'sistema', 'otro'];

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

async function main() {
  const args = parseArgs(process.argv.slice(2));

  if (args.help || args.h) {
    const source = fs.readFileSync(__filename, 'utf8');
    console.log(source.slice(source.indexOf('/**'), source.indexOf('*/') + 2));
    return;
  }

  const uid = typeof args.uid === 'string' ? args.uid.trim() : '';
  const title = typeof args.title === 'string' ? args.title.trim() : '';
  const body = typeof args.body === 'string' ? args.body.trim() : '';

  if (!uid) fail('Falta --uid: es el uid del destinatario en users/{uid}.');
  if (!title) fail('Falta --title: si el documento no tiene título, la bandeja lo descarta.');

  const type = typeof args.type === 'string' ? args.type : 'aviso';
  if (!TYPES.includes(type)) {
    fail(`--type debe ser uno de: ${TYPES.join(', ')}.`);
  }

  const deepLink = typeof args['deep-link'] === 'string' ? args['deep-link'].trim() : null;
  const dryRun = Boolean(args['dry-run']);
  const sendPush = !args['no-push'];

  // Antes de pedir credenciales: un ensayo no debe exigir la cuenta de servicio.
  if (dryRun) {
    console.log('dry-run: documento que se escribiría en notifications:');
    console.log(
      JSON.stringify(
        {
          userId: uid,
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
    console.log(
      sendPush
        ? `dry-run: se enviaría push a los tokens de users/${uid}/fcmTokens.`
        : 'dry-run: sin push (--no-push).',
    );
    return;
  }

  const credentialsPath = resolveCredentials(args);

  const admin = require('firebase-admin');
  admin.initializeApp({
    credential: admin.credential.cert(require(credentialsPath)),
  });

  const db = admin.firestore();
  const messaging = admin.messaging();

  const document = {
    userId: uid,
    title,
    body,
    type,
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  };
  if (deepLink) document.deepLink = deepLink;

  // 1. La bandeja. El id sale de Firestore y viaja en el push, para que el tap
  //    pueda marcar leído el documento exacto.
  const reference = await db.collection('notifications').add(document);
  console.log(`Documento creado: notifications/${reference.id}`);

  if (!sendPush) {
    console.log('Push omitido (--no-push).');
    return;
  }

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
      title,
      body,
      notificationId: reference.id,
      ...(deepLink ? { deepLink } : {}),
    },
    android: { priority: 'high' },
  });

  console.log(`Push enviado a ${tokens.length} token(s): ${response.successCount} ok, ${response.failureCount} con error.`);

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
    stale.forEach((token) => batch.delete(db.collection('users').doc(uid).collection('fcmTokens').doc(token)));
    await batch.commit();
    console.log(`Tokens inválidos eliminados: ${stale.length}.`);
  }
}

main().catch((error) => {
  console.error('\n  Error:', error.message || error, '\n');
  process.exit(1);
});
