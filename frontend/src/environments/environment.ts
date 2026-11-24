const apiBaseUrl = globalThis.document
  ?.querySelector('meta[name="transport-api-base-url"]')
  ?.getAttribute('content') || '/api';

export const environment = {
  production: false,
  apiBaseUrl,
};
