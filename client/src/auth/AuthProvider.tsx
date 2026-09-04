import { useEffect, useState, type ReactNode } from 'react';
import keycloak from '../keycloak';
import { AuthContext, type AuthContextValue } from './AuthContext';

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  // Seed initial state from the keycloak instance so that if init() has already
  // run (React StrictMode double-invokes effects), the state is correct on mount
  // without needing to call setState synchronously inside the effect.
  const [isAuthenticated, setIsAuthenticated] = useState(() => keycloak.authenticated ?? false);
  const [isLoading, setIsLoading] = useState(() => !keycloak.didInitialize);

  useEffect(() => {
    // Keycloak's JS adapter throws if init() is called more than once on the
    // same instance. React StrictMode mounts effects twice in development, so
    // we guard with the didInitialize flag that persists across re-mounts.
    if (keycloak.didInitialize) {
      return;
    }

    keycloak
      .init({
        // 'check-sso' silently checks for an existing session without redirecting.
        // Use 'login-required' to redirect immediately to login if not authenticated.
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
      })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        setIsLoading(false);

        // Proactively refresh the token 60 seconds before it expires
        if (authenticated) {
          setInterval(() => {
            keycloak.updateToken(60).catch(() => {
              console.warn('Token refresh failed — logging out');
              keycloak.logout();
            });
          }, 30_000);
        }
      })
      .catch(() => {
        setIsLoading(false);
      });
  }, []);

  const value: AuthContextValue = {
    keycloak,
    isAuthenticated,
    isLoading,
    token: keycloak.token,
    login: () => keycloak.login(),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    username: keycloak.tokenParsed?.preferred_username as string | undefined,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
