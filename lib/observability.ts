export function reportClientError(error: unknown, context?: Record<string, unknown>): void {
  if (process.env.NODE_ENV !== "production") {
    console.error(error, context);
  }
  const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN;
  if (!dsn) {
    return;
  }
  // Wire Sentry (or equivalent) here only after a real DSN is provided. Do not ship placeholder credentials.
}
