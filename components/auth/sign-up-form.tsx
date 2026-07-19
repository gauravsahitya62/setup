"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiClientError } from "@/services";
import { useAuth } from "@/providers";
import { ROUTES } from "@/constants";
import * as React from "react";

const passwordRule = z
  .string()
  .min(8, "At least 8 characters")
  .regex(/[a-z]/, "Include a lowercase letter")
  .regex(/[A-Z]/, "Include an uppercase letter")
  .regex(/\d/, "Include a number")
  .regex(/[^A-Za-z0-9]/, "Include a special character");

const baseSchema = z.object({
  firstName: z.string().min(1, "First name is required").max(100),
  lastName: z.string().min(1, "Last name is required").max(100),
  email: z.string().email("Enter a valid email"),
  phone: z
    .string()
    .optional()
    .refine((v) => !v || /^[+]?[0-9()\-\s]{7,20}$/.test(v), "Phone format is invalid"),
  password: passwordRule,
  companyName: z.string().max(255).optional(),
  businessName: z.string().max(255).optional(),
});

type FormValues = z.infer<typeof baseSchema>;

export function SignUpForm() {
  const searchParams = useSearchParams();
  const initialRole = searchParams.get("role") === "vendor" ? "vendor" : "planner";
  const [role, setRole] = React.useState<"planner" | "vendor">(initialRole);
  const { registerPlanner, registerVendor } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormValues>({
    resolver: zodResolver(baseSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      password: "",
      companyName: "",
      businessName: "",
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      if (role === "planner") {
        await registerPlanner({
          firstName: values.firstName,
          lastName: values.lastName,
          email: values.email,
          phone: values.phone || undefined,
          password: values.password,
          companyName: values.companyName || undefined,
        });
      } else {
        if (!values.businessName?.trim()) {
          setError("businessName", { message: "Business name is required" });
          return;
        }
        await registerVendor({
          firstName: values.firstName,
          lastName: values.lastName,
          email: values.email,
          phone: values.phone || undefined,
          password: values.password,
          businessName: values.businessName,
        });
      }
      toast.success("Account created");
    } catch (error) {
      const message = error instanceof ApiClientError ? error.message : "Unable to create account";
      if (error instanceof ApiClientError && error.fieldErrors) {
        Object.entries(error.fieldErrors).forEach(([field, msg]) => {
          setError(field as keyof FormValues, { message: msg });
        });
      }
      setError("root", { message });
      toast.error(message);
    }
  });

  return (
    <div className="space-y-5">
      <Tabs value={role} onValueChange={(v) => setRole(v as "planner" | "vendor")}>
        <TabsList className="grid w-full grid-cols-2 rounded-xl">
          <TabsTrigger value="planner" className="rounded-lg">
            Planner
          </TabsTrigger>
          <TabsTrigger value="vendor" className="rounded-lg">
            Vendor
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <form onSubmit={onSubmit} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="firstName">First name</Label>
            <Input id="firstName" className="h-10 rounded-xl" {...register("firstName")} />
            {errors.firstName ? (
              <p className="text-destructive text-xs">{errors.firstName.message}</p>
            ) : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="lastName">Last name</Label>
            <Input id="lastName" className="h-10 rounded-xl" {...register("lastName")} />
            {errors.lastName ? (
              <p className="text-destructive text-xs">{errors.lastName.message}</p>
            ) : null}
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="email">Work email</Label>
          <Input id="email" type="email" className="h-10 rounded-xl" {...register("email")} />
          {errors.email ? <p className="text-destructive text-xs">{errors.email.message}</p> : null}
        </div>

        <div className="space-y-2">
          <Label htmlFor="phone">Phone (optional)</Label>
          <Input id="phone" className="h-10 rounded-xl" {...register("phone")} />
          {errors.phone ? <p className="text-destructive text-xs">{errors.phone.message}</p> : null}
        </div>

        {role === "planner" ? (
          <div className="space-y-2">
            <Label htmlFor="companyName">Company (optional)</Label>
            <Input id="companyName" className="h-10 rounded-xl" {...register("companyName")} />
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="businessName">Business name</Label>
            <Input id="businessName" className="h-10 rounded-xl" {...register("businessName")} />
            {errors.businessName ? (
              <p className="text-destructive text-xs">{errors.businessName.message}</p>
            ) : null}
          </div>
        )}

        <div className="space-y-2">
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            className="h-10 rounded-xl"
            {...register("password")}
          />
          {errors.password ? (
            <p className="text-destructive text-xs">{errors.password.message}</p>
          ) : (
            <p className="text-muted-foreground text-xs">
              8+ characters with upper, lower, number, and special character.
            </p>
          )}
        </div>

        {errors.root ? <p className="text-destructive text-sm">{errors.root.message}</p> : null}

        <Button type="submit" className="h-10 w-full rounded-xl" disabled={isSubmitting}>
          {isSubmitting ? "Creating account…" : "Create account"}
        </Button>
      </form>

      <p className="text-muted-foreground text-center text-sm">
        Already have an account?{" "}
        <Link href={ROUTES.auth.signIn} className="text-foreground font-medium hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
