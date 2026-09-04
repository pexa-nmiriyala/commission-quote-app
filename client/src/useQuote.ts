import { useState } from 'react';
import type { AppState, LoanDetails, QuoteResult } from './types';
import keycloak from './keycloak';

export function useQuote() {
  const [state, setState] = useState<AppState>({
    status: 'idle',
    quoteResult: null,
    errorMessage: null,
  });

  async function submitQuote(details: LoanDetails): Promise<void> {
    setState({ status: 'loading', quoteResult: null, errorMessage: null });

    try {
      // Refresh token if it expires within the next 30 seconds
      await keycloak.updateToken(30).catch(() => {
        keycloak.login();
        throw new Error('Session expired — redirecting to login');
      });

      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      // Attach Bearer token if available (will always be set when authenticated)
      if (keycloak.token) {
        headers['Authorization'] = `Bearer ${keycloak.token}`;
      }

      const response = await fetch('/api/commission-quote', {
        method: 'POST',
        headers,
        body: JSON.stringify(details),
      });

      if (!response.ok) {
        let errorMessage = 'An error occurred. Please try again.';
        try {
          const errorBody = await response.json();
          if (errorBody.error) {
            errorMessage = errorBody.error;
          }
        } catch {
          // ignore JSON parse errors
        }
        setState({ status: 'error', quoteResult: null, errorMessage });
        return;
      }

      const quoteResult: QuoteResult = await response.json();
      setState({ status: 'success', quoteResult, errorMessage: null });
    } catch (err) {
      const message =
        err instanceof Error && err.message.includes('redirecting')
          ? err.message
          : 'Unable to connect — check your network';
      setState({
        status: 'error',
        quoteResult: null,
        errorMessage: message,
      });
    }
  }

  return { ...state, submitQuote };
}
