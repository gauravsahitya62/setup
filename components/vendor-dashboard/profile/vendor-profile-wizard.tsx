"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeft, ArrowRight, Check, Loader2, Plus, Trash2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Textarea } from "@/components/ui/textarea";
import { useMyVendor } from "@/hooks/use-my-vendor";
import {
  ApiClientError,
  vendorApi,
  type CreateVendorPayload,
  type VendorDto,
  type VendorSocialLinksDto,
  type VendorStatus,
} from "@/services";
import { ROUTES } from "@/constants";

const STEPS = [
  "Business information",
  "Service areas",
  "Social links",
  "Logo",
  "Banner",
  "Portfolio",
  "Review & publish",
] as const;

type AreaDraft = { country: string; state: string; city: string };

const emptyBusiness = {
  companyName: "",
  slug: "",
  shortDescription: "",
  description: "",
  gstNumber: "",
  businessType: "",
  establishedYear: "",
  website: "",
  email: "",
  phone: "",
};

export function VendorProfileWizard() {
  const router = useRouter();
  const { vendor, setVendor, loading, reload } = useMyVendor();
  const [step, setStep] = React.useState(0);
  const [saving, setSaving] = React.useState(false);
  const [business, setBusiness] = React.useState(emptyBusiness);
  const [areaDraft, setAreaDraft] = React.useState<AreaDraft>({
    country: "India",
    state: "",
    city: "",
  });
  const [social, setSocial] = React.useState<VendorSocialLinksDto>({});
  const [portfolioTitle, setPortfolioTitle] = React.useState("");

  React.useEffect(() => {
    if (!vendor) return;
    setBusiness({
      companyName: vendor.companyName ?? "",
      slug: vendor.slug ?? "",
      shortDescription: vendor.shortDescription ?? "",
      description: vendor.description ?? "",
      gstNumber: vendor.gstNumber ?? "",
      businessType: vendor.businessType ?? "",
      establishedYear: vendor.establishedYear ? String(vendor.establishedYear) : "",
      website: vendor.website ?? "",
      email: vendor.email ?? "",
      phone: vendor.phone ?? "",
    });
    setSocial(vendor.socialLinks ?? {});
  }, [vendor]);

  const progress = ((step + 1) / STEPS.length) * 100;

  function toPayload(status?: VendorStatus): CreateVendorPayload {
    return {
      companyName: business.companyName.trim(),
      slug: business.slug.trim() || undefined,
      shortDescription: business.shortDescription.trim() || undefined,
      description: business.description.trim() || undefined,
      gstNumber: business.gstNumber.trim() || undefined,
      businessType: business.businessType.trim() || undefined,
      establishedYear: business.establishedYear
        ? Number.parseInt(business.establishedYear, 10)
        : undefined,
      website: business.website.trim() || undefined,
      email: business.email.trim() || undefined,
      phone: business.phone.trim() || undefined,
      status,
    };
  }

  async function ensureVendor(status: VendorStatus = "DRAFT"): Promise<VendorDto> {
    if (vendor) {
      const updated = await vendorApi.update(vendor.id, toPayload(status));
      setVendor(updated);
      return updated;
    }
    const created = await vendorApi.create(toPayload(status));
    setVendor(created);
    return created;
  }

  async function saveDraft() {
    if (!business.companyName.trim()) {
      toast.error("Company name is required to save a draft");
      setStep(0);
      return;
    }
    setSaving(true);
    try {
      const current = await ensureVendor("DRAFT");
      if (Object.values(social).some((v) => Boolean(v?.trim()))) {
        await vendorApi.upsertSocialLinks(current.id, social);
      }
      await reload();
      toast.success("Draft saved");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save draft");
    } finally {
      setSaving(false);
    }
  }

  async function nextFromBusiness() {
    if (!business.companyName.trim()) {
      toast.error("Company name is required");
      return;
    }
    setSaving(true);
    try {
      await ensureVendor(vendor?.status ?? "DRAFT");
      setStep(1);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save business info");
    } finally {
      setSaving(false);
    }
  }

  async function addArea() {
    if (!areaDraft.state.trim() || !areaDraft.city.trim()) {
      toast.error("State and city are required");
      return;
    }
    setSaving(true);
    try {
      const current = await ensureVendor(vendor?.status ?? "DRAFT");
      await vendorApi.addServiceArea(current.id, {
        country: areaDraft.country.trim() || "India",
        state: areaDraft.state.trim(),
        city: areaDraft.city.trim(),
      });
      setAreaDraft((prev) => ({ ...prev, state: "", city: "" }));
      await reload();
      toast.success("Service area added");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not add service area");
    } finally {
      setSaving(false);
    }
  }

  async function removeArea(areaId: string) {
    if (!vendor) return;
    setSaving(true);
    try {
      await vendorApi.deleteServiceArea(vendor.id, areaId);
      await reload();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove area");
    } finally {
      setSaving(false);
    }
  }

  async function saveSocialAndContinue() {
    setSaving(true);
    try {
      const current = await ensureVendor(vendor?.status ?? "DRAFT");
      await vendorApi.upsertSocialLinks(current.id, social);
      await reload();
      setStep(3);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save social links");
    } finally {
      setSaving(false);
    }
  }

  async function uploadImage(kind: "logo" | "banner", file: File | null) {
    if (!file) return;
    setSaving(true);
    try {
      const current = await ensureVendor(vendor?.status ?? "DRAFT");
      const updated =
        kind === "logo"
          ? await vendorApi.uploadLogo(current.id, file)
          : await vendorApi.uploadBanner(current.id, file);
      setVendor(updated);
      toast.success(kind === "logo" ? "Logo uploaded" : "Banner uploaded");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    } finally {
      setSaving(false);
    }
  }

  async function uploadPortfolio(file: File | null) {
    if (!file || !vendor) {
      if (!vendor) toast.error("Save business information first");
      return;
    }
    setSaving(true);
    try {
      await vendorApi.addPortfolio(vendor.id, file, portfolioTitle.trim() || undefined);
      setPortfolioTitle("");
      await reload();
      toast.success("Portfolio item added");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    } finally {
      setSaving(false);
    }
  }

  async function removePortfolio(itemId: string) {
    if (!vendor) return;
    setSaving(true);
    try {
      await vendorApi.deletePortfolio(vendor.id, itemId);
      await reload();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove item");
    } finally {
      setSaving(false);
    }
  }

  async function publish() {
    if (!business.companyName.trim()) {
      toast.error("Company name is required");
      setStep(0);
      return;
    }
    setSaving(true);
    try {
      const published = await ensureVendor("PUBLISHED");
      if (Object.values(social).some((v) => Boolean(v?.trim()))) {
        await vendorApi.upsertSocialLinks(published.id, social);
      }
      await reload();
      toast.success("Profile published");
      router.push(ROUTES.vendor.profile);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not publish");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="text-muted-foreground flex items-center justify-center py-24">
        <Loader2 className="size-5 animate-spin" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <p className="text-muted-foreground text-sm">
            Step {step + 1} of {STEPS.length}
          </p>
          <h1 className="text-2xl font-semibold tracking-tight">{STEPS[step]}</h1>
          <p className="text-muted-foreground text-sm">
            Build a storefront planners can trust. You can save a draft at any time.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.profile}>Exit</Link>
        </Button>
      </div>

      <div className="space-y-2">
        <Progress value={progress} className="h-1.5" />
        <div className="flex flex-wrap gap-1.5">
          {STEPS.map((label, index) => (
            <Badge
              key={label}
              variant={index === step ? "default" : index < step ? "secondary" : "outline"}
              className="rounded-md text-[0.7rem]"
            >
              {index < step ? <Check className="size-3" /> : null}
              {index + 1}. {label}
            </Badge>
          ))}
        </div>
      </div>

      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">{STEPS[step]}</CardTitle>
          <CardDescription>
            {step === 6
              ? "Review your storefront, then publish or keep as draft."
              : "Complete this step to continue. Fields marked optional can be filled later."}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {step === 0 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Company name *" className="sm:col-span-2">
                <Input
                  value={business.companyName}
                  onChange={(e) => setBusiness((b) => ({ ...b, companyName: e.target.value }))}
                  placeholder="Atlas Event Rentals"
                />
              </Field>
              <Field label="Slug (optional)">
                <Input
                  value={business.slug}
                  onChange={(e) => setBusiness((b) => ({ ...b, slug: e.target.value }))}
                  placeholder="atlas-event-rentals"
                />
              </Field>
              <Field label="Business type">
                <Input
                  value={business.businessType}
                  onChange={(e) => setBusiness((b) => ({ ...b, businessType: e.target.value }))}
                  placeholder="Furniture rental"
                />
              </Field>
              <Field label="Short description" className="sm:col-span-2">
                <Input
                  value={business.shortDescription}
                  onChange={(e) => setBusiness((b) => ({ ...b, shortDescription: e.target.value }))}
                  placeholder="Premium furniture & décor for corporate events"
                />
              </Field>
              <Field label="About company" className="sm:col-span-2">
                <Textarea
                  value={business.description}
                  onChange={(e) => setBusiness((b) => ({ ...b, description: e.target.value }))}
                  rows={5}
                />
              </Field>
              <Field label="Email">
                <Input
                  type="email"
                  value={business.email}
                  onChange={(e) => setBusiness((b) => ({ ...b, email: e.target.value }))}
                />
              </Field>
              <Field label="Phone">
                <Input
                  value={business.phone}
                  onChange={(e) => setBusiness((b) => ({ ...b, phone: e.target.value }))}
                  placeholder="+91..."
                />
              </Field>
              <Field label="Website">
                <Input
                  value={business.website}
                  onChange={(e) => setBusiness((b) => ({ ...b, website: e.target.value }))}
                  placeholder="https://"
                />
              </Field>
              <Field label="GST number">
                <Input
                  value={business.gstNumber}
                  onChange={(e) => setBusiness((b) => ({ ...b, gstNumber: e.target.value }))}
                />
              </Field>
              <Field label="Established year">
                <Input
                  inputMode="numeric"
                  value={business.establishedYear}
                  onChange={(e) => setBusiness((b) => ({ ...b, establishedYear: e.target.value }))}
                  placeholder="2018"
                />
              </Field>
            </div>
          ) : null}

          {step === 1 ? (
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-3">
                <Field label="Country">
                  <Input
                    value={areaDraft.country}
                    onChange={(e) => setAreaDraft((a) => ({ ...a, country: e.target.value }))}
                  />
                </Field>
                <Field label="State *">
                  <Input
                    value={areaDraft.state}
                    onChange={(e) => setAreaDraft((a) => ({ ...a, state: e.target.value }))}
                  />
                </Field>
                <Field label="City *">
                  <Input
                    value={areaDraft.city}
                    onChange={(e) => setAreaDraft((a) => ({ ...a, city: e.target.value }))}
                  />
                </Field>
              </div>
              <Button
                type="button"
                className="rounded-xl"
                disabled={saving}
                onClick={() => void addArea()}
              >
                <Plus className="size-4" />
                Add area
              </Button>
              <ul className="space-y-2">
                {(vendor?.serviceAreas ?? []).map((area) => (
                  <li
                    key={area.id}
                    className="border-border/70 flex items-center justify-between rounded-xl border px-3 py-2 text-sm"
                  >
                    <span>
                      {area.city}, {area.state} · {area.country}
                    </span>
                    <Button
                      type="button"
                      size="icon-sm"
                      variant="ghost"
                      onClick={() => void removeArea(area.id)}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          {step === 2 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              {(
                [
                  ["instagram", "Instagram"],
                  ["facebook", "Facebook"],
                  ["linkedin", "LinkedIn"],
                  ["youtube", "YouTube"],
                  ["pinterest", "Pinterest"],
                  ["website", "Website"],
                ] as const
              ).map(([key, label]) => (
                <Field key={key} label={label}>
                  <Input
                    value={social[key] ?? ""}
                    onChange={(e) => setSocial((s) => ({ ...s, [key]: e.target.value }))}
                    placeholder="https://"
                  />
                </Field>
              ))}
            </div>
          ) : null}

          {step === 3 || step === 4 ? (
            <div className="space-y-3">
              <p className="text-muted-foreground text-sm">
                Upload a {step === 3 ? "square logo" : "wide banner"} image. Files are stored via
                the storage service (local now; S3/R2 later) — not in the database.
              </p>
              {(step === 3 ? vendor?.logoUrl : vendor?.bannerUrl) ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={(step === 3 ? vendor?.logoUrl : vendor?.bannerUrl) ?? ""}
                  alt={step === 3 ? "Logo preview" : "Banner preview"}
                  className={
                    step === 3
                      ? "size-28 rounded-2xl border object-cover"
                      : "h-40 w-full rounded-2xl border object-cover"
                  }
                />
              ) : null}
              <Label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed px-4 py-3 text-sm">
                <Upload className="size-4" />
                Choose image
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) =>
                    void uploadImage(step === 3 ? "logo" : "banner", e.target.files?.[0] ?? null)
                  }
                />
              </Label>
            </div>
          ) : null}

          {step === 5 ? (
            <div className="space-y-4">
              <Field label="Caption (optional)">
                <Input
                  value={portfolioTitle}
                  onChange={(e) => setPortfolioTitle(e.target.value)}
                  placeholder="Corporate gala, Mumbai 2025"
                />
              </Field>
              <Label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed px-4 py-3 text-sm">
                <Upload className="size-4" />
                Upload portfolio image
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => void uploadPortfolio(e.target.files?.[0] ?? null)}
                />
              </Label>
              <div className="grid gap-3 sm:grid-cols-2">
                {(vendor?.portfolio ?? []).map((item) => (
                  <div key={item.id} className="overflow-hidden rounded-xl border">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={item.imageUrl}
                      alt={item.title ?? "Portfolio"}
                      className="h-36 w-full object-cover"
                    />
                    <div className="flex items-center justify-between gap-2 p-2 text-sm">
                      <span className="truncate">{item.title || "Untitled"}</span>
                      <Button
                        type="button"
                        size="icon-sm"
                        variant="ghost"
                        onClick={() => void removePortfolio(item.id)}
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {step === 6 ? (
            <div className="space-y-3 text-sm">
              <ReviewRow label="Company" value={business.companyName || vendor?.companyName} />
              <ReviewRow label="Slug" value={business.slug || vendor?.slug} />
              <ReviewRow label="Status" value={vendor?.status ?? "DRAFT"} />
              <ReviewRow label="Service areas" value={String(vendor?.serviceAreas.length ?? 0)} />
              <ReviewRow label="Portfolio items" value={String(vendor?.portfolio.length ?? 0)} />
              <ReviewRow label="Logo" value={vendor?.logoUrl ? "Uploaded" : "Missing"} />
              <ReviewRow label="Banner" value={vendor?.bannerUrl ? "Uploaded" : "Missing"} />
            </div>
          ) : null}
        </CardContent>
      </Card>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <Button
          type="button"
          variant="outline"
          className="rounded-xl"
          disabled={step === 0 || saving}
          onClick={() => setStep((s) => Math.max(0, s - 1))}
        >
          <ArrowLeft className="size-4" />
          Back
        </Button>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="secondary"
            className="rounded-xl"
            disabled={saving}
            onClick={() => void saveDraft()}
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : null}
            Save as draft
          </Button>
          {step < 6 ? (
            <Button
              type="button"
              className="rounded-xl"
              disabled={saving}
              onClick={() => {
                if (step === 0) void nextFromBusiness();
                else if (step === 2) void saveSocialAndContinue();
                else setStep((s) => Math.min(STEPS.length - 1, s + 1));
              }}
            >
              Continue
              <ArrowRight className="size-4" />
            </Button>
          ) : (
            <Button
              type="button"
              className="rounded-xl"
              disabled={saving}
              onClick={() => void publish()}
            >
              {saving ? <Loader2 className="size-4 animate-spin" /> : null}
              Publish profile
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({
  label,
  children,
  className,
}: {
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`space-y-1.5 ${className ?? ""}`}>
      <Label>{label}</Label>
      {children}
    </div>
  );
}

function ReviewRow({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="border-border/60 flex items-center justify-between gap-4 rounded-xl border px-3 py-2">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value?.trim() ? value : "—"}</span>
    </div>
  );
}
