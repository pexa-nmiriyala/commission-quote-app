import { render, screen } from '@testing-library/react';
import { QuoteResult } from './QuoteResult';

const mockResult = {
  quoteId: 'abc-123',
  commission: 5250,
  totalRepayable: 55250,
};

describe('QuoteResult', () => {
  it('renders the quoteId', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    expect(screen.getByText('abc-123')).toBeInTheDocument();
  });

  it('renders formatted commission as AUD currency', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    // $5,250.00 — match exactly the commission value, not the totalRepayable
    expect(screen.getByText(/^\$5[,.]?250/)).toBeInTheDocument();
  });

  it('renders formatted totalRepayable as AUD currency', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    expect(screen.getByText(/\$55[,.]?250/)).toBeInTheDocument();
  });

  it('renders all three data labels', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    expect(screen.getByText(/quote id/i)).toBeInTheDocument();
    // Use exact dt label, not the heading which also contains "Commission"
    expect(screen.getByText('Commission')).toBeInTheDocument();
    expect(screen.getByText(/total repayable/i)).toBeInTheDocument();
  });
});
