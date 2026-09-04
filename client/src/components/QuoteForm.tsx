import React, { useState } from 'react';
import type { LoanDetails, RequestStatus } from '../types';
import { validateLoanAmount, validateLoanTermMonths, validateRiskBand } from '../validation';

interface QuoteFormProps {
  onSubmit: (details: LoanDetails) => void;
  status: RequestStatus;
}

const inputBase =
  'w-full rounded-xl border border-gray-300 bg-white px-4 py-2.5 text-gray-900 text-sm placeholder-gray-400 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed';

const inputError = 'border-red-400 focus:ring-red-400 focus:border-red-400';

export function QuoteForm({ onSubmit, status }: QuoteFormProps) {
  const [loanAmount, setLoanAmount] = useState('');
  const [loanTermMonths, setLoanTermMonths] = useState('');
  const [riskBand, setRiskBand] = useState<'low' | 'medium' | 'high' | ''>('');

  const [errors, setErrors] = useState<{
    loanAmount?: string;
    loanTermMonths?: string;
    riskBand?: string;
  }>({});

  const isLoading = status === 'loading';

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    const loanAmountError = validateLoanAmount(loanAmount);
    const loanTermError = validateLoanTermMonths(loanTermMonths);
    const riskBandError = validateRiskBand(riskBand);

    if (loanAmountError || loanTermError || riskBandError) {
      setErrors({
        loanAmount: loanAmountError ?? undefined,
        loanTermMonths: loanTermError ?? undefined,
        riskBand: riskBandError ?? undefined,
      });
      return;
    }

    setErrors({});
    onSubmit({
      loanAmount: Number(loanAmount),
      loanTermMonths: Number(loanTermMonths),
      riskBand: riskBand as 'low' | 'medium' | 'high',
    });
  }

  return (
    <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6">
      <h2 className="text-base font-semibold text-gray-900 mb-5">Loan Details</h2>

      <form
        onSubmit={handleSubmit}
        noValidate
        aria-label="Commission quote form"
        className="space-y-5"
      >
        {/* Loan Amount */}
        <div>
          <label htmlFor="loanAmount" className="block text-sm font-medium text-gray-700 mb-1.5">
            Loan Amount (AUD)
          </label>
          <div className="relative">
            <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-sm font-medium">
              $
            </span>
            <input
              id="loanAmount"
              type="number"
              value={loanAmount}
              onChange={(e) => setLoanAmount(e.target.value)}
              disabled={isLoading}
              placeholder="50,000"
              aria-describedby={errors.loanAmount ? 'loanAmount-error' : undefined}
              aria-invalid={!!errors.loanAmount}
              min="0.01"
              step="0.01"
              className={`${inputBase} pl-8 ${errors.loanAmount ? inputError : ''}`}
            />
          </div>
          {errors.loanAmount && (
            <p
              id="loanAmount-error"
              role="alert"
              className="mt-1.5 text-xs text-red-500 flex items-center gap-1"
            >
              <svg className="w-3.5 h-3.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
              {errors.loanAmount}
            </p>
          )}
        </div>

        {/* Loan Term */}
        <div>
          <label
            htmlFor="loanTermMonths"
            className="block text-sm font-medium text-gray-700 mb-1.5"
          >
            Loan Term
          </label>
          <div className="relative">
            <input
              id="loanTermMonths"
              type="number"
              value={loanTermMonths}
              onChange={(e) => setLoanTermMonths(e.target.value)}
              disabled={isLoading}
              placeholder="36"
              aria-describedby={errors.loanTermMonths ? 'loanTermMonths-error' : undefined}
              aria-invalid={!!errors.loanTermMonths}
              min="1"
              step="1"
              className={`${inputBase} pr-20 ${errors.loanTermMonths ? inputError : ''}`}
            />
            <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
              months
            </span>
          </div>
          {errors.loanTermMonths && (
            <p
              id="loanTermMonths-error"
              role="alert"
              className="mt-1.5 text-xs text-red-500 flex items-center gap-1"
            >
              <svg className="w-3.5 h-3.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
              {errors.loanTermMonths}
            </p>
          )}
        </div>

        {/* Risk Band */}
        <div>
          <label htmlFor="riskBand" className="block text-sm font-medium text-gray-700 mb-1.5">
            Risk Band
          </label>
          <select
            id="riskBand"
            value={riskBand}
            onChange={(e) => setRiskBand(e.target.value as 'low' | 'medium' | 'high' | '')}
            disabled={isLoading}
            aria-describedby={errors.riskBand ? 'riskBand-error' : undefined}
            aria-invalid={!!errors.riskBand}
            className={`${inputBase} ${errors.riskBand ? inputError : ''} appearance-none`}
          >
            <option value="">Select risk band</option>
            <option value="low">🟢 Low</option>
            <option value="medium">🟡 Medium</option>
            <option value="high">🔴 High</option>
          </select>
          {errors.riskBand && (
            <p
              id="riskBand-error"
              role="alert"
              className="mt-1.5 text-xs text-red-500 flex items-center gap-1"
            >
              <svg className="w-3.5 h-3.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
              {errors.riskBand}
            </p>
          )}
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold py-3 px-4 rounded-xl transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 flex items-center justify-center gap-2"
        >
          {isLoading ? (
            <>
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                />
              </svg>
              Generating...
            </>
          ) : (
            'Generate Quote'
          )}
        </button>
      </form>
    </div>
  );
}
