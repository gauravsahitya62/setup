import Link from "next/link";

import { RootShell } from "@/components/layout";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <RootShell>
      <div className="mx-auto flex max-w-lg flex-col items-center px-4 py-24 text-center">
        <p className="text-muted-foreground text-sm font-medium tracking-wide uppercase">404</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-tight">Page not found</h1>
        <p className="text-muted-foreground mt-3 text-sm">
          The page you requested does not exist or is no longer available.
        </p>
        <Button asChild className="mt-8">
          <Link href="/">Back to home</Link>
        </Button>
      </div>
    </RootShell>
  );
}
