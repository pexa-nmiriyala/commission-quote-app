import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import { resetKeycloakInitFlag } from './auth/keycloakInitFlag';

// Hoist the mock so it's available before imports are resolved.
// This ensures AuthProvider and useQuote both get the same mock instance.
const mockKeycloak = vi.hoisted(() => ({
  token: 'mock-test-token',
  tokenParsed: { preferred_username: 'test-user' },
  updateToken: vi.fn().mockResolvedValue(true),
  login: vi.fn(),
  logout: vi.fn(),
  // init resolves with true = authenticated, so QuoteApp renders immediately
  init: vi.fn().mockResolvedValue(true),
}));

vi.mock('./keycloak', () => ({ default: mockKeycloak }));

describe('App integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetKeycloakInitFlag();
    mockKeycloak.updateToken.mockResolvedValue(true);
    mockKeycloak.init.mockResolvedValue(true);
  });

  it('renders the form on load', async () => {
    render(<App />);
    // Wait for AuthProvider to finish initialising (keycloak.init resolves async)
    await waitFor(() => {
      expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /generate quote/i })).toBeInTheDocument();
  });

  it('shows QuoteResult after successful submission', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({
        quoteId: 'integration-test-id',
        commission: 5250,
        totalRepayable: 55250,
      }),
    } as Response);

    const user = userEvent.setup();
    render(<App />);

    // Wait for auth to complete and form to render
    await waitFor(() => expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument());

    await user.type(screen.getByLabelText(/loan amount/i), '50000');
    await user.type(screen.getByLabelText(/loan term/i), '36');
    await user.selectOptions(screen.getByLabelText(/risk band/i), 'medium');
    await user.click(screen.getByRole('button', { name: /generate quote/i }));

    await waitFor(() => {
      expect(screen.getByText(/integration-test-id/)).toBeInTheDocument();
    });
  });

  it('shows ErrorMessage after a failed API call', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Commission API error' }),
    } as Response);

    const user = userEvent.setup();
    render(<App />);

    await waitFor(() => expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument());

    await user.type(screen.getByLabelText(/loan amount/i), '50000');
    await user.type(screen.getByLabelText(/loan term/i), '36');
    await user.selectOptions(screen.getByLabelText(/risk band/i), 'medium');
    await user.click(screen.getByRole('button', { name: /generate quote/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText('Commission API error')).toBeInTheDocument();
    });
  });

  it('clears error message on new submission after previous error', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce({ ok: false, json: async () => ({ error: 'Oops' }) } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ quoteId: 'retry-id', commission: 100, totalRepayable: 1100 }),
      } as Response);

    const user = userEvent.setup();
    render(<App />);

    await waitFor(() => expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument());

    // First submit — error
    await user.type(screen.getByLabelText(/loan amount/i), '50000');
    await user.type(screen.getByLabelText(/loan term/i), '36');
    await user.selectOptions(screen.getByLabelText(/risk band/i), 'medium');
    await user.click(screen.getByRole('button', { name: /generate quote/i }));

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());

    // Second submit — success, error should be gone
    await user.click(screen.getByRole('button', { name: /generate quote/i }));

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(screen.getByText(/retry-id/)).toBeInTheDocument();
    });
  });
});
