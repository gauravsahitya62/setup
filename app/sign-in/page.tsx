import type { Metadata } from "next";
import Link from "next/link";

import { AuthShell } from "@/components/auth/auth-shell";
import { SignInForm } from "@/components/auth/sign-in-form";
import { createPageMetadata } from "@/lib/metadata";
import { ROUTES } from "@/constants";

export const metadata: Metadata = createPageMetadata({
  title: "Sign in",
  description: "Sign in to FlowSet.",
  path: "/sign-in",
});

export default function SignInPage() {
  return (
    <AuthShell
      title="Sign in"
      description="Access your planner, vendor, or admin workspace."
      footer={
        <p className="text-muted-foreground text-center text-sm">
          New to FlowSet?{" "}
          <Link href={ROUTES.auth.signUp} className="text-foreground font-medium hover:underline">
            Create an account
          </Link>
        </p>
      }
    >
      <SignInForm />
    </AuthShell>
  );
}
