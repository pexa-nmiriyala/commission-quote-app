import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

describe('App integration', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders the form on load', () => {
    render(<App />);
    expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument();
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

    await user.type(screen.getByLabelText(/loan amount/i), '50000');
    await user.type(screen.getByLabelText(/loan term/i), '36');
    await user.selectOptions(screen.getByLabelText(/risk band/i), 'medium');
    await user.click(screen.getByRole('button', { name: /generate quote/i }));

    await waitFor(() => {
      expect(screen.getByText('integration-test-id')).toBeInTheDocument();
    });
  });

  it('shows ErrorMessage after a failed API call', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Commission API error' }),
    } as Response);

    const user = userEvent.setup();
    render(<App />);

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
      expect(screen.getByText('retry-id')).toBeInTheDocument();
    });
  });
});
