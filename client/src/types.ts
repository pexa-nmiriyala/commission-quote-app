export interface LoanDetails {
  loanAmount: number;
  loanTermMonths: number;
  riskBand: 'low' | 'medium' | 'high';
}

export interface QuoteResult {
  quoteId: string;
  commission: number;
  totalRepayable: number;
}

export type RequestStatus = 'idle' | 'loading' | 'success' | 'error';

export interface AppState {
  status: RequestStatus;
  quoteResult: QuoteResult | null;
  errorMessage: string | null;
}
