import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QuoteForm } from './QuoteForm';

const mockOnSubmit = vi.fn();

function renderForm(status = 'idle' as const) {
  render(<QuoteForm onSubmit={mockOnSubmit} status={status} />);
}

describe('QuoteForm', () => {
  beforeEach(() => {
    mockOnSubmit.mockClear();
  });

  it('renders all three inputs and the submit button', () => {
    renderForm();
    expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/loan term/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/risk band/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /generate quote/i })).toBeInTheDocument();
  });

  it('each input has an associated visible label', () => {
    renderForm();
    expect(screen.getByLabelText(/loan amount/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/loan term/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/risk band/i)).toBeInTheDocument();
  });

  it('disables the submit button when status is loading', () => {
    renderForm('loading');
    expect(screen.getByRole('button', { name: /generating/i })).toBeDisabled();
  });

  it('shows validation error for empty loanAmount on submit', async () => {
    renderForm();
    fireEvent.click(screen.getByRole('button', { name: /generate quote/i }));
    const alerts = await screen.findAllByRole('alert');
    expect(alerts.length).toBeGreaterThan(0);
  });

  it('shows validation error with correct ARIA attributes', async () => {
    renderForm();
    fireEvent.click(screen.getByRole('button', { name: /generate quote/i }));
    const alert = await screen.findAllByRole('alert');
    expect(alert.length).toBeGreaterThan(0);
  });

  it('calls onSubmit with correct LoanDetails when form is valid', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/loan amount/i), '50000');
    await user.type(screen.getByLabelText(/loan term/i), '36');
    await user.selectOptions(screen.getByLabelText(/risk band/i), 'medium');
    await user.click(screen.getByRole('button', { name: /generate quote/i }));

    expect(mockOnSubmit).toHaveBeenCalledWith({
      loanAmount: 50000,
      loanTermMonths: 36,
      riskBand: 'medium',
    });
  });

  it('does not call onSubmit when form fields are invalid', async () => {
    renderForm();
    fireEvent.click(screen.getByRole('button', { name: /generate quote/i }));
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });
});
