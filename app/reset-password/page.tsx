import type { Metadata } from "next";
import { Suspense } from "react";

import { AuthShell } from "@/components/auth/auth-shell";
import { ResetPasswordForm } from "@/components/auth/reset-password-form";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Reset password",
  description: "Choose a new FlowSet password.",
  path: "/reset-password",
});

export default function ResetPasswordPage() {
  return (
    <AuthShell title="Reset password" description="Enter your reset token and a new password.">
      <Suspense fallback={<div className="bg-muted/40 h-48 animate-pulse rounded-xl" />}>
        <ResetPasswordForm />
      </Suspense>
    </AuthShell>
  );
}
