const VALID_RISK_BANDS = ['low', 'medium', 'high'] as const;

export function validateLoanAmount(value: unknown): string | null {
  if (value === '' || value === null || value === undefined) {
    return 'Loan amount is required';
  }
  const num = Number(value);
  if (isNaN(num) || !isFinite(num) || num <= 0) {
    return 'Loan amount must be a positive number';
  }
  return null;
}

export function validateLoanTermMonths(value: unknown): string | null {
  if (value === '' || value === null || value === undefined) {
    return 'Loan term is required';
  }
  const num = Number(value);
  if (isNaN(num) || !isFinite(num) || num <= 0 || !Number.isInteger(num)) {
    return 'Loan term must be a positive whole number of months';
  }
  return null;
}

export function validateRiskBand(value: unknown): string | null {
  if (!VALID_RISK_BANDS.includes(value as typeof VALID_RISK_BANDS[number])) {
    return 'Risk band must be low, medium, or high';
  }
  return null;
}
