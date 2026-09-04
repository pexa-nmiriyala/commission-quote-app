import { AuthProvider } from './auth/AuthProvider';
import { useAuth } from './auth/AuthContext';
import { useQuote } from './useQuote';
import { QuoteForm } from './components/QuoteForm';
import { LoadingIndicator } from './components/LoadingIndicator';
import { QuoteResult } from './components/QuoteResult';
import { ErrorMessage } from './components/ErrorMessage';

function Header({ username, onLogout }: { username?: string; onLogout: () => void }) {
  return (
    <header className="bg-white border-b border-gray-200 shadow-sm">
      <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center">
            <span className="text-white text-sm font-bold">CQ</span>
          </div>
          <span className="text-gray-900 font-semibold text-lg">Commission Quote</span>
        </div>
        {username && (
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-500">
              Signed in as <span className="font-medium text-gray-700">{username}</span>
            </span>
            <button
              type="button"
              onClick={onLogout}
              className="text-sm text-gray-500 hover:text-gray-700 border border-gray-300 rounded-md px-3 py-1.5 hover:bg-gray-50 transition-colors"
            >
              Sign out
            </button>
          </div>
        )}
      </div>
    </header>
  );
}

function LoginScreen({ onLogin }: { onLogin: () => void }) {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 rounded-2xl bg-indigo-600 flex items-center justify-center mx-auto mb-4 shadow-lg">
            <span className="text-white text-2xl font-bold">CQ</span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Commission Quote</h1>
          <p className="mt-2 text-gray-500">Lending Platform — Staff Portal</p>
        </div>
        <div className="bg-white rounded-2xl shadow-md border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-1">Welcome back</h2>
          <p className="text-gray-500 text-sm mb-6">
            Sign in with your staff account to generate commission quotes.
          </p>
          <button
            type="button"
            onClick={onLogin}
            className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-3 px-4 rounded-xl transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
          >
            Sign in with SSO
          </button>
        </div>
      </div>
    </div>
  );
}

function LoadingScreen() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-gray-500 text-sm" aria-live="polite">
          Loading...
        </p>
      </div>
    </div>
  );
}

function QuoteApp() {
  const { isAuthenticated, isLoading, login, logout, username } = useAuth();
  const { status, quoteResult, errorMessage, submitQuote } = useQuote();

  if (isLoading) return <LoadingScreen />;
  if (!isAuthenticated) return <LoginScreen onLogin={login} />;

  return (
    <div className="min-h-screen bg-gray-50">
      <Header username={username} onLogout={logout} />

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Generate Quote</h1>
          <p className="text-gray-500 mt-1">
            Enter loan details below to calculate the commission quote.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
          <QuoteForm onSubmit={submitQuote} status={status} />

          <div className="space-y-4">
            {status === 'loading' && <LoadingIndicator />}
            {status === 'success' && quoteResult && <QuoteResult quoteResult={quoteResult} />}
            {status === 'error' && errorMessage && <ErrorMessage message={errorMessage} />}
            {status === 'idle' && (
              <div className="bg-white rounded-2xl border border-gray-200 p-8 text-center">
                <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-3">
                  <svg
                    className="w-6 h-6 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.5}
                      d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 11h.01M12 11h.01M15 11h.01M4 19h16a2 2 0 002-2V7a2 2 0 00-2-2H4a2 2 0 00-2 2v10a2 2 0 002 2z"
                    />
                  </svg>
                </div>
                <p className="text-gray-400 text-sm">Your quote will appear here</p>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <QuoteApp />
    </AuthProvider>
  );
}

export default App;
