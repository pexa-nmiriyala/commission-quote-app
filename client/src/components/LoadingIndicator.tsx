export function LoadingIndicator() {
  return (
    <div
      role="status"
      aria-label="Loading"
      aria-live="polite"
      className="bg-white rounded-2xl border border-gray-200 shadow-sm p-8 flex flex-col items-center gap-3"
    >
      <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      <p className="text-sm text-gray-500 font-medium">Calculating your quote...</p>
    </div>
  );
}
