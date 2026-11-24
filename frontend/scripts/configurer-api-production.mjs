import { readFile, writeFile } from 'node:fs/promises';

const variable = process.env.TRANSPORT_API_BASE_URL;
if (!variable && process.env.VERCEL_ENV === 'production') {
  throw new Error('TRANSPORT_API_BASE_URL est requis sur Vercel en production.');
}

if (variable) {
  const url = new URL(variable);
  if (url.protocol !== 'https:' || url.username || url.password
      || url.pathname !== '/api' || url.search || url.hash) {
    throw new Error('TRANSPORT_API_BASE_URL doit être une URL HTTPS terminée par /api.');
  }
  const index = new URL('../dist/frontend/browser/index.html', import.meta.url);
  const html = await readFile(index, 'utf8');
  const defaultMetadata = '<meta name="transport-api-base-url" content="/api">';
  if (!html.includes(defaultMetadata)) {
    throw new Error('Métadonnée de configuration de l’API absente du build Angular.');
  }
  await writeFile(index, html.replace(defaultMetadata,
      `<meta name="transport-api-base-url" content="${url.origin}/api">`));
}
