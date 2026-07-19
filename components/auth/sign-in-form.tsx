"use client";

import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiClientError } from "@/services";
import { useAuth } from "@/providers";
import { ROUTES } from "@/constants";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

type FormValues = z.infer<typeof schema>;

export function SignInForm() {
  const { login } = useAuth();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await login(values);
      toast.success("Welcome back");
    } catch (error) {
      const message = error instanceof ApiClientError ? error.message : "Unable to sign in";
      setError("root", { message });
      toast.error(message);
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          className="h-10 rounded-xl"
          aria-invalid={Boolean(errors.email)}
          {...register("email")}
        />
        {errors.email ? <p className="text-destructive text-xs">{errors.email.message}</p> : null}
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between gap-2">
          <Label htmlFor="password">Password</Label>
          <Link
            href={ROUTES.auth.forgotPassword}
            className="text-muted-foreground hover:text-foreground text-xs font-medium"
          >
            Forgot password?
          </Link>
        </div>
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
          className="h-10 rounded-xl"
          aria-invalid={Boolean(errors.password)}
          {...register("password")}
        />
        {errors.password ? (
          <p className="text-destructive text-xs">{errors.password.message}</p>
        ) : null}
      </div>

      {errors.root ? <p className="text-destructive text-sm">{errors.root.message}</p> : null}

      <Button type="submit" className="h-10 w-full rounded-xl" disabled={isSubmitting}>
        {isSubmitting ? "Signing in…" : "Sign in"}
      </Button>
    </form>
  );
}
