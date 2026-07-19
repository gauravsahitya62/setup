import type { Metadata } from "next";
import { Suspense } from "react";

import { AuthShell } from "@/components/auth/auth-shell";
import { SignUpForm } from "@/components/auth/sign-up-form";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Create account",
  description: "Register as a planner or vendor on FlowSet.",
  path: "/sign-up",
});

export default function SignUpPage() {
  return (
    <AuthShell
      title="Create your account"
      description="Join FlowSet as an event planner or rental vendor."
    >
      <Suspense fallback={<div className="bg-muted/40 h-64 animate-pulse rounded-xl" />}>
        <SignUpForm />
      </Suspense>
    </AuthShell>
  );
}
