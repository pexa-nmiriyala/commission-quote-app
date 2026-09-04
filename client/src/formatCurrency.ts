const formatter = new Intl.NumberFormat('en-AU', {
  style: 'currency',
  currency: 'AUD',
});

export function formatCurrency(value: number): string {
  return formatter.format(value);
}
