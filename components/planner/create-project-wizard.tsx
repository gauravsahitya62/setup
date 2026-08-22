"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight, Check, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Textarea } from "@/components/ui/textarea";
import { ROUTES } from "@/constants";
import {
  EVENT_TYPE_LABELS,
  EVENT_TYPES,
  formatEventDate,
  formatProjectLocation,
} from "@/lib/projects/labels";
import { createProjectSchema, type CreateProjectFormValues } from "@/lib/projects/schema";
import { ApiClientError, projectApi } from "@/services";

const STEPS = ["Event details", "Venue", "Story & cover", "Review"] as const;

export function CreateProjectWizard() {
  const router = useRouter();
  const [step, setStep] = React.useState(0);
  const [cover, setCover] = React.useState<File | null>(null);
  const [savingDraft, setSavingDraft] = React.useState(false);

  const form = useForm<CreateProjectFormValues>({
    resolver: zodResolver(createProjectSchema),
    defaultValues: {
      name: "",
      eventType: "WEDDING",
      eventDate: "",
      guestCount: "",
      venueName: "",
      venueAddress: "",
      city: "",
      state: "",
      country: "India",
      description: "",
    },
  });

  const values = form.watch();

  const submit = async (asDraft: boolean) => {
    const parsed = createProjectSchema.safeParse(form.getValues());
    if (!parsed.success) {
      toast.error(parsed.error.issues[0]?.message ?? "Check the form");
      setStep(0);
      return;
    }
    const payload = {
      ...parsed.data,
      status: asDraft ? ("DRAFT" as const) : ("PLANNING" as const),
    };
    try {
      if (asDraft) setSavingDraft(true);
      const created = await projectApi.create(payload);
      if (cover) {
        await projectApi.uploadCover(created.id, cover);
      }
      toast.success(asDraft ? "Draft saved" : "Project created");
      router.push(ROUTES.planner.project(created.id));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not create project");
    } finally {
      setSavingDraft(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <p className="text-muted-foreground text-sm font-medium">New project</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">Create an event workspace</h1>
        <p className="text-muted-foreground mt-2 text-sm">
          A project is where you shortlist products and vendors. Inventory is not reserved until
          booking.
        </p>
      </div>

      <Progress value={((step + 1) / STEPS.length) * 100} className="h-2" />
      <ol className="text-muted-foreground flex flex-wrap gap-2 text-xs">
        {STEPS.map((label, index) => (
          <li key={label} className={index === step ? "text-foreground font-medium" : undefined}>
            {index + 1}. {label}
          </li>
        ))}
      </ol>

      <Card className="border-border/70 shadow-soft">
        <CardHeader>
          <CardTitle className="text-base">{STEPS[step]}</CardTitle>
          <CardDescription>
            {step === 0
              ? "Name the event and capture the basics."
              : step === 1
                ? "Where is this happening?"
                : step === 2
                  ? "Optional context for your team."
                  : "Confirm details, then create or save as draft."}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {step === 0 ? (
            <>
              <Field label="Project name" error={form.formState.errors.name?.message}>
                <Input
                  className="h-11 rounded-xl"
                  placeholder="Sharma Wedding"
                  {...form.register("name")}
                />
              </Field>
              <Field label="Event type">
                <select
                  className="border-input bg-background h-11 w-full rounded-xl border px-3 text-sm"
                  {...form.register("eventType")}
                >
                  {EVENT_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {EVENT_TYPE_LABELS[type]}
                    </option>
                  ))}
                </select>
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Event date">
                  <Input className="h-11 rounded-xl" type="date" {...form.register("eventDate")} />
                </Field>
                <Field label="Guest count">
                  <Input
                    className="h-11 rounded-xl"
                    type="number"
                    min={0}
                    {...form.register("guestCount")}
                  />
                </Field>
              </div>
            </>
          ) : null}

          {step === 1 ? (
            <>
              <Field label="Venue">
                <Input
                  className="h-11 rounded-xl"
                  placeholder="City Palace"
                  {...form.register("venueName")}
                />
              </Field>
              <Field label="Address">
                <Input className="h-11 rounded-xl" {...form.register("venueAddress")} />
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="City">
                  <Input
                    className="h-11 rounded-xl"
                    placeholder="Jaipur"
                    {...form.register("city")}
                  />
                </Field>
                <Field label="State">
                  <Input
                    className="h-11 rounded-xl"
                    placeholder="Rajasthan"
                    {...form.register("state")}
                  />
                </Field>
              </div>
            </>
          ) : null}

          {step === 2 ? (
            <>
              <Field label="Description">
                <Textarea className="min-h-28 rounded-xl" {...form.register("description")} />
              </Field>
              <Field label="Cover image">
                <Input
                  className="h-11 rounded-xl"
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  onChange={(event) => setCover(event.target.files?.[0] ?? null)}
                />
              </Field>
            </>
          ) : null}

          {step === 3 ? (
            <dl className="grid gap-3 text-sm">
              <Row label="Name" value={values.name || "—"} />
              <Row label="Type" value={EVENT_TYPE_LABELS[values.eventType ?? "OTHER"]} />
              <Row label="Date" value={formatEventDate(values.eventDate) ?? "Not set"} />
              <Row label="Guests" value={values.guestCount ? String(values.guestCount) : "—"} />
              <Row
                label="Venue"
                value={
                  formatProjectLocation({
                    venueName: values.venueName,
                    city: values.city,
                    state: values.state,
                  }) ?? "—"
                }
              />
              <Row label="Cover" value={cover ? cover.name : "None"} />
            </dl>
          ) : null}

          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              className="rounded-xl"
              disabled={step === 0}
              onClick={() => setStep((current) => Math.max(0, current - 1))}
            >
              <ArrowLeft className="size-3.5" />
              Back
            </Button>
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                disabled={form.formState.isSubmitting || savingDraft}
                onClick={() => void submit(true)}
              >
                {savingDraft ? <Loader2 className="size-4 animate-spin" /> : null}
                Save as draft
              </Button>
              {step < STEPS.length - 1 ? (
                <Button
                  type="button"
                  className="rounded-xl"
                  onClick={() => setStep((current) => current + 1)}
                >
                  Continue
                  <ArrowRight className="size-3.5" />
                </Button>
              ) : (
                <Button
                  type="button"
                  className="rounded-xl"
                  disabled={form.formState.isSubmitting}
                  onClick={() => void submit(false)}
                >
                  {form.formState.isSubmitting ? (
                    <Loader2 className="size-4 animate-spin" />
                  ) : (
                    <Check className="size-3.5" />
                  )}
                  Create project
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      {children}
      {error ? <p className="text-destructive text-xs">{error}</p> : null}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-border/50 flex items-start justify-between gap-4 border-b py-2 last:border-0">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
