import type { Metadata } from "next";

import { AuthShell } from "@/components/auth/auth-shell";
import { ForgotPasswordForm } from "@/components/auth/forgot-password-form";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Forgot password",
  description: "Request a FlowSet password reset.",
  path: "/forgot-password",
});

export default function ForgotPasswordPage() {
  return (
    <AuthShell
      title="Forgot password"
      description="We’ll email reset instructions if an account exists."
    >
      <ForgotPasswordForm />
    </AuthShell>
  );
}
