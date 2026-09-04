import { render, screen } from '@testing-library/react';
import { ErrorMessage } from './ErrorMessage';

describe('ErrorMessage', () => {
  it('renders the error message text', () => {
    render(<ErrorMessage message="Commission API error" />);
    expect(screen.getByText('Commission API error')).toBeInTheDocument();
  });

  it('has role="alert" for screen readers', () => {
    render(<ErrorMessage message="Something went wrong" />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});
