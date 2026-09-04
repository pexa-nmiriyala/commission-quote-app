import { useState } from 'react';
import type { AppState, LoanDetails, QuoteResult } from './types';

export function useQuote() {
  const [state, setState] = useState<AppState>({
    status: 'idle',
    quoteResult: null,
    errorMessage: null,
  });

  async function submitQuote(details: LoanDetails): Promise<void> {
    setState({ status: 'loading', quoteResult: null, errorMessage: null });

    try {
      const response = await fetch('/api/commission-quote', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
    } catch {
      setState({
        status: 'error',
        quoteResult: null,
        errorMessage: 'Unable to connect — check your network',
      });
    }
  }

  return { ...state, submitQuote };
}
