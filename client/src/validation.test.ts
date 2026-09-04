import { validateLoanAmount, validateLoanTermMonths, validateRiskBand } from './validation';

describe('validateLoanAmount', () => {
  it('returns error for empty string', () => {
    expect(validateLoanAmount('')).toBeTruthy();
  });
  it('returns error for null', () => {
    expect(validateLoanAmount(null)).toBeTruthy();
  });
  it('returns error for undefined', () => {
    expect(validateLoanAmount(undefined)).toBeTruthy();
  });
  it('returns error for zero', () => {
    expect(validateLoanAmount(0)).toBeTruthy();
  });
  it('returns error for negative number', () => {
    expect(validateLoanAmount(-100)).toBeTruthy();
  });
  it('returns error for NaN', () => {
    expect(validateLoanAmount(NaN)).toBeTruthy();
  });
  it('returns error for non-numeric string', () => {
    expect(validateLoanAmount('abc')).toBeTruthy();
  });
  it('returns null for a valid positive number', () => {
    expect(validateLoanAmount(50000)).toBeNull();
  });
  it('returns null for a valid positive float', () => {
    expect(validateLoanAmount(0.01)).toBeNull();
  });
});

describe('validateLoanTermMonths', () => {
  it('returns error for empty string', () => {
    expect(validateLoanTermMonths('')).toBeTruthy();
  });
  it('returns error for zero', () => {
    expect(validateLoanTermMonths(0)).toBeTruthy();
  });
  it('returns error for negative integer', () => {
    expect(validateLoanTermMonths(-1)).toBeTruthy();
  });
  it('returns error for a float', () => {
    expect(validateLoanTermMonths(1.5)).toBeTruthy();
  });
  it('returns error for a non-numeric string', () => {
    expect(validateLoanTermMonths('abc')).toBeTruthy();
  });
  it('returns null for a valid positive integer', () => {
    expect(validateLoanTermMonths(36)).toBeNull();
  });
  it('returns null for 1', () => {
    expect(validateLoanTermMonths(1)).toBeNull();
  });
});

describe('validateRiskBand', () => {
  it('returns error for empty string', () => {
    expect(validateRiskBand('')).toBeTruthy();
  });
  it('returns error for arbitrary string', () => {
    expect(validateRiskBand('unknown')).toBeTruthy();
  });
  it('returns error for uppercase LOW', () => {
    expect(validateRiskBand('LOW')).toBeTruthy();
  });
  it('returns null for "low"', () => {
    expect(validateRiskBand('low')).toBeNull();
  });
  it('returns null for "medium"', () => {
    expect(validateRiskBand('medium')).toBeNull();
  });
  it('returns null for "high"', () => {
    expect(validateRiskBand('high')).toBeNull();
  });
});
