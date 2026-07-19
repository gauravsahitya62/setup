"use client";

import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiClientError, authApi } from "@/services";
import { ROUTES } from "@/constants";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
});

type FormValues = z.infer<typeof schema>;

export function ForgotPasswordForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      const result = await authApi.forgotPassword(values.email);
      toast.success(result.message);
    } catch (error) {
      const message = error instanceof ApiClientError ? error.message : "Unable to request reset";
      toast.error(message);
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input id="email" type="email" className="h-10 rounded-xl" {...register("email")} />
        {errors.email ? <p className="text-destructive text-xs">{errors.email.message}</p> : null}
      </div>
      <Button type="submit" className="h-10 w-full rounded-xl" disabled={isSubmitting}>
        {isSubmitting ? "Sending…" : "Send reset link"}
      </Button>
      <p className="text-muted-foreground text-center text-sm">
        <Link href={ROUTES.auth.signIn} className="text-foreground font-medium hover:underline">
          Back to sign in
        </Link>
      </p>
    </form>
  );
}
