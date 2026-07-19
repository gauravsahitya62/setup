"use client";

import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiClientError, authApi } from "@/services";
import { ROUTES } from "@/constants";

const schema = z
  .object({
    token: z.string().min(1, "Token is required"),
    newPassword: z
      .string()
      .min(8, "At least 8 characters")
      .regex(/[a-z]/, "Include a lowercase letter")
      .regex(/[A-Z]/, "Include an uppercase letter")
      .regex(/\d/, "Include a number")
      .regex(/[^A-Za-z0-9]/, "Include a special character"),
    confirmPassword: z.string().min(1, "Confirm your password"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type FormValues = z.infer<typeof schema>;

export function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      token: searchParams.get("token") ?? "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      const result = await authApi.resetPassword(values.token, values.newPassword);
      toast.success(result.message);
      router.replace(ROUTES.auth.signIn);
    } catch (error) {
      const message = error instanceof ApiClientError ? error.message : "Unable to reset password";
      toast.error(message);
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="token">Reset token</Label>
        <Input id="token" className="h-10 rounded-xl font-mono text-xs" {...register("token")} />
        {errors.token ? <p className="text-destructive text-xs">{errors.token.message}</p> : null}
      </div>
      <div className="space-y-2">
        <Label htmlFor="newPassword">New password</Label>
        <Input
          id="newPassword"
          type="password"
          className="h-10 rounded-xl"
          {...register("newPassword")}
        />
        {errors.newPassword ? (
          <p className="text-destructive text-xs">{errors.newPassword.message}</p>
        ) : null}
      </div>
      <div className="space-y-2">
        <Label htmlFor="confirmPassword">Confirm password</Label>
        <Input
          id="confirmPassword"
          type="password"
          className="h-10 rounded-xl"
          {...register("confirmPassword")}
        />
        {errors.confirmPassword ? (
          <p className="text-destructive text-xs">{errors.confirmPassword.message}</p>
        ) : null}
      </div>
      <Button type="submit" className="h-10 w-full rounded-xl" disabled={isSubmitting}>
        {isSubmitting ? "Updating…" : "Update password"}
      </Button>
      <p className="text-muted-foreground text-center text-sm">
        <Link href={ROUTES.auth.signIn} className="text-foreground font-medium hover:underline">
          Back to sign in
        </Link>
      </p>
    </form>
  );
}
