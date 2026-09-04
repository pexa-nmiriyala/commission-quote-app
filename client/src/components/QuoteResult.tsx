import type { QuoteResult as QuoteResultType } from '../types';
import { formatCurrency } from '../formatCurrency';

interface QuoteResultProps {
  quoteResult: QuoteResultType;
}

export function QuoteResult({ quoteResult }: QuoteResultProps) {
  return (
    <section aria-label="Quote result">
      <h2>Your Commission Quote</h2>
      <dl>
        <div>
          <dt>Quote ID</dt>
          <dd>{quoteResult.quoteId}</dd>
        </div>
        <div>
          <dt>Commission</dt>
          <dd>{formatCurrency(quoteResult.commission)}</dd>
        </div>
        <div>
          <dt>Total Repayable</dt>
          <dd>{formatCurrency(quoteResult.totalRepayable)}</dd>
        </div>
      </dl>
    </section>
  );
}
