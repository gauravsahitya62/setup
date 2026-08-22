"use client";

import * as React from "react";
import { toast } from "sonner";

import { ApiClientError, vendorApi, type VendorDto } from "@/services";

export function useMyVendor() {
  const [vendor, setVendor] = React.useState<VendorDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [missing, setMissing] = React.useState(false);

  const reload = React.useCallback(async () => {
    setLoading(true);
    try {
      const data = await vendorApi.getMine();
      setVendor(data);
      setMissing(false);
      return data;
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 404) {
        setVendor(null);
        setMissing(true);
        return null;
      }
      toast.error(
        error instanceof ApiClientError ? error.message : "Failed to load vendor profile"
      );
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void reload().catch(() => undefined);
  }, [reload]);

  return { vendor, setVendor, loading, missing, reload };
}
