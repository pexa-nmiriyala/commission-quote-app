import { createContext, useContext } from 'react';
import type Keycloak from 'keycloak-js';

export interface AuthContextValue {
  keycloak: Keycloak;
  isAuthenticated: boolean;
  isLoading: boolean;
  token: string | undefined;
  login: () => void;
  logout: () => void;
  username: string | undefined;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
