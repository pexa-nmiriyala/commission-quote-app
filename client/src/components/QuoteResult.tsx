import type { QuoteResult as QuoteResultType } from '../types';
import { formatCurrency } from '../formatCurrency';

interface QuoteResultProps {
  quoteResult: QuoteResultType;
}

export function QuoteResult({ quoteResult }: QuoteResultProps) {
  return (
    <section
      aria-label="Quote result"
      className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden"
    >
      {/* Header */}
      <div className="bg-indigo-600 px-6 py-4">
        <div className="flex items-center gap-2">
          <svg
            className="w-5 h-5 text-indigo-200"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <h2 className="text-white font-semibold text-base">Quote Generated</h2>
        </div>
        <p className="text-indigo-200 text-xs mt-0.5 font-mono">ID: {quoteResult.quoteId}</p>
      </div>

      {/* Values */}
      <dl className="divide-y divide-gray-100">
        <div className="px-6 py-4 flex items-center justify-between">
          <dt className="text-sm text-gray-500 font-medium">Commission</dt>
          <dd className="text-lg font-bold text-gray-900">
            {formatCurrency(quoteResult.commission)}
          </dd>
        </div>
        <div className="px-6 py-4 flex items-center justify-between bg-gray-50">
          <dt className="text-sm text-gray-500 font-medium">Total Repayable</dt>
          <dd className="text-lg font-bold text-indigo-600">
            {formatCurrency(quoteResult.totalRepayable)}
          </dd>
        </div>
      </dl>
    </section>
  );
}
