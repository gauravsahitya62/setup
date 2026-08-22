"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Building2, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { ApiClientError, vendorApi, type VendorDto } from "@/services";
import { ROUTES } from "@/constants";

export function VendorProfilePage() {
  const router = useRouter();
  const [vendor, setVendor] = React.useState<VendorDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [missing, setMissing] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const data = await vendorApi.getMine();
        if (!cancelled) {
          setVendor(data);
          setMissing(false);
        }
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 404) {
          if (!cancelled) setMissing(true);
        } else {
          toast.error(error instanceof ApiClientError ? error.message : "Failed to load profile");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="text-muted-foreground flex items-center justify-center py-24">
        <Loader2 className="size-5 animate-spin" />
      </div>
    );
  }

  if (missing || !vendor) {
    return (
      <div className="space-y-6">
        <EmptyState
          title="Create your company profile"
          description="Set up your digital storefront so planners can discover and trust your rental catalog."
          icon={<Building2 className="size-5" />}
          actionLabel="Start profile wizard"
          onAction={() => router.push(ROUTES.vendor.profileWizard)}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Company profile</h1>
          <p className="text-muted-foreground text-sm">
            Manage the public storefront planners see when evaluating your brand.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" className="rounded-xl" asChild>
            <Link href={`${ROUTES.vendors}/${vendor.slug}`} target="_blank">
              View public page
            </Link>
          </Button>
          <Button className="rounded-xl" asChild>
            <Link href={ROUTES.vendor.profileWizard}>Edit / complete wizard</Link>
          </Button>
        </div>
      </div>

      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <div className="flex flex-wrap items-center gap-2">
            <CardTitle className="text-base">{vendor.companyName}</CardTitle>
            <Badge variant="secondary" className="rounded-md">
              {vendor.status}
            </Badge>
            {vendor.verified ? (
              <Badge className="rounded-md">Verified</Badge>
            ) : (
              <Badge variant="outline" className="rounded-md">
                Unverified
              </Badge>
            )}
          </div>
          <CardDescription>/{vendor.slug}</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <Field label="Short description" value={vendor.shortDescription} />
          <Field label="Business type" value={vendor.businessType} />
          <Field label="Email" value={vendor.email} />
          <Field label="Phone" value={vendor.phone} />
          <Field label="Website" value={vendor.website} />
          <Field label="GST" value={vendor.gstNumber} />
          <Field
            label="Established"
            value={vendor.establishedYear ? String(vendor.establishedYear) : null}
          />
          <Field label="Service areas" value={`${vendor.serviceAreas.length} locations`} />
          <div className="sm:col-span-2">
            <Field label="About" value={vendor.description} />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="space-y-1">
      <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">{label}</p>
      <p className="text-sm whitespace-pre-wrap">{value?.trim() ? value : "—"}</p>
    </div>
  );
}
