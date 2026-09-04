import Keycloak from 'keycloak-js';

// Keycloak URL is injected at build time via Vite's env system.
// For local dev: http://localhost:9090
// For Docker: http://localhost:9090 (Keycloak is exposed on host port 9090)
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:9090';
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM ?? 'commission-app';
const keycloakClientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'commission-app-client';

const keycloak = new Keycloak({
  url: keycloakUrl,
  realm: keycloakRealm,
  clientId: keycloakClientId,
});

export default keycloak;
