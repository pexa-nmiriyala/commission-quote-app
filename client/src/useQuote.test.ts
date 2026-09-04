import { renderHook, act } from '@testing-library/react';
import { useQuote } from './useQuote';

const mockLoanDetails = {
  loanAmount: 50000,
  loanTermMonths: 36,
  riskBand: 'medium' as const,
};

const mockQuoteResult = {
  quoteId: 'test-id-123',
  commission: 5250,
  totalRepayable: 55250,
};

describe('useQuote', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('starts in idle state', () => {
    const { result } = renderHook(() => useQuote());
    expect(result.current.status).toBe('idle');
    expect(result.current.quoteResult).toBeNull();
    expect(result.current.errorMessage).toBeNull();
  });

  it('sets status to loading when submitQuote is called', async () => {
    // Never resolves — keeps it in loading state
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}));

    const { result } = renderHook(() => useQuote());
    act(() => {
      result.current.submitQuote(mockLoanDetails);
    });
    expect(result.current.status).toBe('loading');
    expect(result.current.quoteResult).toBeNull();
    expect(result.current.errorMessage).toBeNull();
  });

  it('sets status to success and quoteResult on successful response', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => mockQuoteResult,
    } as Response);

    const { result } = renderHook(() => useQuote());
    await act(async () => {
      await result.current.submitQuote(mockLoanDetails);
    });

    expect(result.current.status).toBe('success');
    expect(result.current.quoteResult).toEqual(mockQuoteResult);
    expect(result.current.errorMessage).toBeNull();
  });

  it('sets status to error and errorMessage on API error response', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Commission API error' }),
    } as Response);

    const { result } = renderHook(() => useQuote());
    await act(async () => {
      await result.current.submitQuote(mockLoanDetails);
    });

    expect(result.current.status).toBe('error');
    expect(result.current.errorMessage).toBe('Commission API error');
    expect(result.current.quoteResult).toBeNull();
  });

  it('sets connectivity error message when fetch throws', async () => {
    vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useQuote());
    await act(async () => {
      await result.current.submitQuote(mockLoanDetails);
    });

    expect(result.current.status).toBe('error');
    expect(result.current.errorMessage).toContain('connect');
    expect(result.current.quoteResult).toBeNull();
  });

  it('clears error when a new successful request is made', async () => {
    // First call: error
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce({ ok: false, json: async () => ({ error: 'Oops' }) } as Response)
      .mockResolvedValueOnce({ ok: true, json: async () => mockQuoteResult } as Response);

    const { result } = renderHook(() => useQuote());

    await act(async () => { await result.current.submitQuote(mockLoanDetails); });
    expect(result.current.status).toBe('error');

    await act(async () => { await result.current.submitQuote(mockLoanDetails); });
    expect(result.current.status).toBe('success');
    expect(result.current.errorMessage).toBeNull();
    expect(result.current.quoteResult).toEqual(mockQuoteResult);
  });

  it('quoteResult and errorMessage are never both non-null', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => mockQuoteResult,
    } as Response);

    const { result } = renderHook(() => useQuote());
    await act(async () => { await result.current.submitQuote(mockLoanDetails); });

    const { quoteResult, errorMessage } = result.current;
    expect(quoteResult !== null && errorMessage !== null).toBe(false);
  });
});
