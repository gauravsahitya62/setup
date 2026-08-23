import { LoadingSpinner } from "@/components/shared/loading-spinner";

export default function Loading() {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3 px-4">
      <LoadingSpinner size="lg" label="Loading page" />
      <p className="text-muted-foreground text-sm">Loading…</p>
    </div>
  );
}
