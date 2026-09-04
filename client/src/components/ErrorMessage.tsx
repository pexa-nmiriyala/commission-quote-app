interface ErrorMessageProps {
  message: string;
}

export function ErrorMessage({ message }: ErrorMessageProps) {
  return (
    <div role="alert" aria-live="assertive">
      <p>{message}</p>
    </div>
  );
}
