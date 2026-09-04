import { useQuote } from './useQuote';
import { QuoteForm } from './components/QuoteForm';
import { LoadingIndicator } from './components/LoadingIndicator';
import { QuoteResult } from './components/QuoteResult';
import { ErrorMessage } from './components/ErrorMessage';

function App() {
  const { status, quoteResult, errorMessage, submitQuote } = useQuote();

  return (
    <main>
      <h1>Commission Quote</h1>

      <QuoteForm onSubmit={submitQuote} status={status} />

      {status === 'loading' && <LoadingIndicator />}

      {status === 'success' && quoteResult && (
        <QuoteResult quoteResult={quoteResult} />
      )}

      {status === 'error' && errorMessage && (
        <ErrorMessage message={errorMessage} />
      )}
    </main>
  );
}

export default App;
