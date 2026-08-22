"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2, Trash2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useMyVendor } from "@/hooks/use-my-vendor";
import { ApiClientError, vendorApi } from "@/services";
import { ROUTES } from "@/constants";

const DOC_TYPES = [
  { value: "GST_CERTIFICATE", label: "GST Certificate" },
  { value: "BUSINESS_LICENSE", label: "Business License" },
  { value: "PAN", label: "PAN" },
  { value: "IDENTITY", label: "Identity" },
  { value: "OTHER", label: "Other" },
] as const;

export function VendorDocumentsPage() {
  const router = useRouter();
  const { vendor, loading, missing, reload } = useMyVendor();
  const [docType, setDocType] = React.useState<string>("GST_CERTIFICATE");
  const [saving, setSaving] = React.useState(false);

  if (loading) {
    return (
      <div className="text-muted-foreground flex justify-center py-24">
        <Loader2 className="size-5 animate-spin" />
      </div>
    );
  }

  if (missing || !vendor) {
    return (
      <EmptyState
        title="Create a company profile first"
        actionLabel="Start wizard"
        onAction={() => router.push(ROUTES.vendor.profileWizard)}
      />
    );
  }

  async function upload(file: File | null) {
    if (!file) return;
    setSaving(true);
    try {
      await vendorApi.uploadDocument(vendor!.id, file, docType);
      await reload();
      toast.success("Document uploaded");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string) {
    setSaving(true);
    try {
      await vendorApi.deleteDocument(vendor!.id, id);
      await reload();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Delete failed");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Documents</h1>
          <p className="text-muted-foreground text-sm">
            Upload verification documents. Only metadata is stored in the database.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.profile}>Back to profile</Link>
        </Button>
      </div>

      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">Upload document</CardTitle>
          <CardDescription>GST, license, PAN, or identity files for admin review.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="max-w-xs space-y-1.5">
            <Label>Document type</Label>
            <Select value={docType} onValueChange={setDocType}>
              <SelectTrigger className="rounded-xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {DOC_TYPES.map((t) => (
                  <SelectItem key={t.value} value={t.value}>
                    {t.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed px-4 py-3 text-sm">
            <Upload className="size-4" />
            Choose file
            <input
              type="file"
              className="hidden"
              disabled={saving}
              onChange={(e) => void upload(e.target.files?.[0] ?? null)}
            />
          </Label>
          <ul className="space-y-2">
            {vendor.documents.map((doc) => (
              <li
                key={doc.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-xl border px-3 py-2 text-sm"
              >
                <div className="min-w-0 space-y-1">
                  <p className="truncate font-medium">{doc.fileName}</p>
                  <div className="flex flex-wrap gap-2">
                    <Badge variant="secondary" className="rounded-md">
                      {doc.documentType}
                    </Badge>
                    <Badge variant="outline" className="rounded-md">
                      {doc.verificationStatus}
                    </Badge>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" className="rounded-xl" asChild>
                    <a href={doc.fileUrl} target="_blank" rel="noopener noreferrer">
                      Open
                    </a>
                  </Button>
                  <Button size="icon-sm" variant="ghost" onClick={() => void remove(doc.id)}>
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
