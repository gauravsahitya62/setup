"use client";

import { useEffect } from "react";

import { reportClientError } from "@/lib/observability";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    reportClientError(error, { digest: error.digest, scope: "global" });
  }, [error]);

  return (
    <html lang="en">
      <body>
        <div style={{ maxWidth: 480, margin: "20vh auto", padding: 24, fontFamily: "sans-serif" }}>
          <h1>Something went wrong</h1>
          <p>The application failed to load. Please try again.</p>
          <button type="button" onClick={() => reset()}>
            Retry
          </button>
        </div>
      </body>
    </html>
  );
}
