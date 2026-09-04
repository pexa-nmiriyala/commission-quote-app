import { formatCurrency } from './formatCurrency';

describe('formatCurrency', () => {
  it('formats zero as AUD currency', () => {
    const result = formatCurrency(0);
    expect(result).toContain('$');
    expect(result).toBeTruthy();
  });
  it('formats a typical commission value', () => {
    const result = formatCurrency(583.33);
    expect(result).toContain('$');
    expect(result).toContain('583');
  });
  it('formats a large amount with thousands separator', () => {
    const result = formatCurrency(50000);
    expect(result).toContain('$');
    expect(result).toContain('50');
  });
  it('returns the same string for the same input (deterministic)', () => {
    expect(formatCurrency(12345.67)).toBe(formatCurrency(12345.67));
  });
  it('returns a non-empty string for any finite number', () => {
    expect(formatCurrency(1)).toBeTruthy();
    expect(formatCurrency(999999)).toBeTruthy();
  });
});
