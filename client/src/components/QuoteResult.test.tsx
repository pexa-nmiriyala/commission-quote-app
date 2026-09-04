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
    // quoteId is rendered as "ID: abc-123"
    expect(screen.getByText(/abc-123/)).toBeInTheDocument();
  });

  it('renders formatted commission as AUD currency', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    expect(screen.getByText(/^\$5[,.]?250/)).toBeInTheDocument();
  });

  it('renders formatted totalRepayable as AUD currency', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    expect(screen.getByText(/\$55[,.]?250/)).toBeInTheDocument();
  });

  it('renders all three data labels', () => {
    render(<QuoteResult quoteResult={mockResult} />);
    // "Quote Generated" heading is in the header
    expect(screen.getByText(/quote generated/i)).toBeInTheDocument();
    expect(screen.getByText('Commission')).toBeInTheDocument();
    expect(screen.getByText(/total repayable/i)).toBeInTheDocument();
  });
});
