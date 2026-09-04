import React, { useState } from 'react';
import type { LoanDetails, RequestStatus } from '../types';
import {
  validateLoanAmount,
  validateLoanTermMonths,
  validateRiskBand,
} from '../validation';

interface QuoteFormProps {
  onSubmit: (details: LoanDetails) => void;
  status: RequestStatus;
}

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
    <form onSubmit={handleSubmit} noValidate aria-label="Commission quote form">
      <div>
        <label htmlFor="loanAmount">Loan Amount (AUD)</label>
        <input
          id="loanAmount"
          type="number"
          value={loanAmount}
          onChange={(e) => setLoanAmount(e.target.value)}
          disabled={isLoading}
          aria-describedby={errors.loanAmount ? 'loanAmount-error' : undefined}
          aria-invalid={!!errors.loanAmount}
          min="0.01"
          step="0.01"
        />
        {errors.loanAmount && (
          <span id="loanAmount-error" role="alert">
            {errors.loanAmount}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="loanTermMonths">Loan Term (months)</label>
        <input
          id="loanTermMonths"
          type="number"
          value={loanTermMonths}
          onChange={(e) => setLoanTermMonths(e.target.value)}
          disabled={isLoading}
          aria-describedby={errors.loanTermMonths ? 'loanTermMonths-error' : undefined}
          aria-invalid={!!errors.loanTermMonths}
          min="1"
          step="1"
        />
        {errors.loanTermMonths && (
          <span id="loanTermMonths-error" role="alert">
            {errors.loanTermMonths}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="riskBand">Risk Band</label>
        <select
          id="riskBand"
          value={riskBand}
          onChange={(e) => setRiskBand(e.target.value as 'low' | 'medium' | 'high' | '')}
          disabled={isLoading}
          aria-describedby={errors.riskBand ? 'riskBand-error' : undefined}
          aria-invalid={!!errors.riskBand}
        >
          <option value="">Select risk band</option>
          <option value="low">Low</option>
          <option value="medium">Medium</option>
          <option value="high">High</option>
        </select>
        {errors.riskBand && (
          <span id="riskBand-error" role="alert">
            {errors.riskBand}
          </span>
        )}
      </div>

      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Generating...' : 'Generate Quote'}
      </button>
    </form>
  );
}
