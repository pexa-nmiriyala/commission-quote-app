/**
 * Module-level flag tracking whether Keycloak's init() has been called.
 *
 * Kept in a separate module so it persists across React StrictMode
 * double-invocations (which remount components but don't re-import modules)
 * without violating the react-refresh/only-export-components ESLint rule.
 */
let keycloakInitialized = false;

export function isKeycloakInitialized() {
  return keycloakInitialized;
}

export function markKeycloakInitialized() {
  keycloakInitialized = true;
}

/** Reset the init guard — only used in tests to isolate each test case. */
export function resetKeycloakInitFlag() {
  keycloakInitialized = false;
}
