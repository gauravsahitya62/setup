"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight, Check, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Textarea } from "@/components/ui/textarea";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { track } from "@/lib/analytics";
import { EVENT_TYPE_LABELS, formatEventDate } from "@/lib/projects/labels";
import { bookingRequestSchema, type BookingRequestFormValues } from "@/lib/booking/schema";
import { bookingErrorMessage, formatAvailabilityHint } from "@/lib/booking/labels";
import {
  formatEstimatedRental,
  lineEstimatedValue,
  rentalDays,
  toMoney,
} from "@/lib/booking/pricing";
import {
  ApiClientError,
  bookingApi,
  marketplaceApi,
  projectApi,
  type ProjectDto,
  type ProjectProductDto,
} from "@/services";
import { cn } from "@/lib/utils";

const STEPS = [
  "Select vendor",
  "Products & quantities",
  "Event details",
  "Delivery details",
  "Review",
] as const;

interface BookingRequestWizardProps {
  projectId: string;
  vendorId?: string;
}

export function BookingRequestWizard({
  projectId,
  vendorId: presetVendorId,
}: BookingRequestWizardProps) {
  const router = useRouter();
  const [step, setStep] = React.useState(0);
  const [project, setProject] = React.useState<ProjectDto | null>(null);
  const [products, setProducts] = React.useState<ProjectProductDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [submitting, setSubmitting] = React.useState(false);
  const [created, setCreated] = React.useState<{ id: string; vendorName: string } | null>(null);
  const [stockByVariant, setStockByVariant] = React.useState<
    Record<string, { available: number; source: "dates" | "on-hand" }>
  >({});
  const [checkingStock, setCheckingStock] = React.useState(false);

  const form = useForm<BookingRequestFormValues>({
    resolver: zodResolver(bookingRequestSchema),
    defaultValues: {
      projectId,
      vendorId: "",
      variantIds: [],
      quantities: {},
      eventDate: "",
      rentalStartDate: "",
      rentalEndDate: "",
      venueName: "",
      venueAddress: "",
      city: "",
      state: "",
      plannerNotes: "",
      deliveryDate: "",
      pickupDate: "",
      contactPerson: "",
      contactPhone: "",
      deliveryNotes: "",
      termsAccepted: false,
    },
  });

  const values = form.watch();

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [projectDto, productRows] = await Promise.all([
          projectApi.get(projectId),
          projectApi.products.list(projectId),
        ]);
        if (cancelled) return;
        setProject(projectDto);
        const listed = productRows.filter((row) => row.listed && row.productVariantId);
        setProducts(listed);
        const vendorExists = Boolean(
          presetVendorId && listed.some((row) => row.vendorId === presetVendorId)
        );
        const selectedVendorId = vendorExists ? presetVendorId! : "";
        const selectedVariants = vendorExists
          ? listed
              .filter((row) => row.vendorId === selectedVendorId && row.productVariantId)
              .map((row) => row.productVariantId as string)
          : [];
        form.reset({
          projectId,
          vendorId: selectedVendorId,
          variantIds: selectedVariants,
          quantities: Object.fromEntries(
            productRows
              .filter((row) => row.productVariantId)
              .map((row) => [row.productVariantId as string, row.quantity])
          ),
          eventDate: projectDto.eventDate ?? "",
          rentalStartDate: projectDto.eventDate ?? "",
          rentalEndDate: projectDto.eventDate ?? "",
          venueName: projectDto.venueName ?? "",
          venueAddress: projectDto.venueAddress ?? "",
          city: projectDto.city ?? "",
          state: projectDto.state ?? "",
          plannerNotes: "",
          deliveryDate: projectDto.eventDate ?? "",
          pickupDate: projectDto.eventDate ?? "",
          contactPerson: "",
          contactPhone: "",
          deliveryNotes: "",
          termsAccepted: false,
        });
        if (vendorExists) {
          setStep(1);
        }
      } catch (error) {
        toast.error(bookingErrorMessage(error, "Could not load project"));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [form, projectId, presetVendorId]);

  const vendors = React.useMemo(() => {
    const map = new Map<string, { id: string; name: string; verified: boolean; count: number }>();
    for (const product of products) {
      const current = map.get(product.vendorId);
      if (current) current.count += 1;
      else {
        map.set(product.vendorId, {
          id: product.vendorId,
          name: product.vendorName,
          verified: product.vendorVerified,
          count: 1,
        });
      }
    }
    return [...map.values()];
  }, [products]);

  const vendorProducts = products.filter((product) => product.vendorId === values.vendorId);
  const selectedProducts = vendorProducts.filter(
    (product) => product.productVariantId && values.variantIds.includes(product.productVariantId)
  );
  const days = rentalDays(String(values.rentalStartDate ?? ""), String(values.rentalEndDate ?? ""));
  const estimated = selectedProducts.reduce((sum, product) => {
    const quantity = Number(values.quantities[product.productVariantId ?? ""] ?? product.quantity);
    return sum + lineEstimatedValue(toMoney(product.snapshotPricePerDay), quantity, days);
  }, 0);
  const vendorName = vendors.find((vendor) => vendor.id === values.vendorId)?.name ?? "Vendor";
  const rentalStart = String(values.rentalStartDate || project?.eventDate || "");
  const rentalEnd = String(values.rentalEndDate || project?.eventDate || "");
  const datesChosen = Boolean(values.rentalStartDate && values.rentalEndDate);

  React.useEffect(() => {
    const fromCatalog: Record<string, { available: number; source: "dates" | "on-hand" }> = {};
    for (const product of products) {
      if (product.productVariantId && product.availableQuantity != null) {
        fromCatalog[product.productVariantId] = {
          available: product.availableQuantity,
          source: "on-hand",
        };
      }
    }
    setStockByVariant(fromCatalog);

    const variantIds = products
      .map((product) => product.productVariantId)
      .filter((id): id is string => Boolean(id));
    if (!rentalStart || !rentalEnd || variantIds.length === 0) return;

    let cancelled = false;
    async function loadDateStock() {
      setCheckingStock(true);
      try {
        const entries = await Promise.all(
          variantIds.map(async (id) => {
            try {
              const result = await marketplaceApi.inventory.check(id, rentalStart, rentalEnd, 1);
              return [
                id,
                { available: result.availableQuantity, source: "dates" as const },
              ] as const;
            } catch {
              return [id, { available: 0, source: "dates" as const }] as const;
            }
          })
        );
        if (!cancelled) setStockByVariant(Object.fromEntries(entries));
      } finally {
        if (!cancelled) setCheckingStock(false);
      }
    }
    void loadDateStock();
    return () => {
      cancelled = true;
    };
  }, [products, rentalStart, rentalEnd]);

  const stockFor = React.useCallback(
    (product: ProjectProductDto) => {
      const id = product.productVariantId ?? "";
      if (stockByVariant[id]) return stockByVariant[id];
      if (product.availableQuantity != null) {
        return { available: product.availableQuantity, source: "on-hand" as const };
      }
      return null;
    },
    [stockByVariant]
  );

  const shortages = selectedProducts.flatMap((product) => {
    const stock = stockFor(product);
    const requested = Number(values.quantities[product.productVariantId ?? ""] ?? product.quantity);
    if (!stock || !Number.isFinite(requested) || requested <= stock.available) return [];
    return [{ product, stock, requested }];
  });

  const canAdvance = () => {
    if (step === 0) return Boolean(values.vendorId);
    if (step === 1) {
      if (values.variantIds.length === 0) return false;
      return selectedProducts.every((product) => {
        const quantity = Number(values.quantities[product.productVariantId ?? ""] ?? 0);
        if (!Number.isInteger(quantity) || quantity < 1) return false;
        const stock = stockFor(product);
        return !stock || quantity <= stock.available;
      });
    }
    if (step === 2) {
      return Boolean(values.eventDate && values.rentalStartDate && values.rentalEndDate);
    }
    if (step === 3) {
      return Boolean(
        values.deliveryDate &&
        values.pickupDate &&
        values.venueName &&
        values.venueAddress &&
        values.city &&
        values.state &&
        values.contactPerson &&
        values.contactPhone
      );
    }
    return true;
  };

  const submit = async () => {
    const parsed = bookingRequestSchema.safeParse(form.getValues());
    if (!parsed.success) {
      toast.error(parsed.error.issues[0]?.message ?? "Check the form");
      return;
    }
    const data = parsed.data;
    setSubmitting(true);
    try {
      const createdRequest = await bookingApi.create({
        projectId: data.projectId,
        vendorId: data.vendorId,
        items: data.variantIds.map((variantId) => ({
          variantId,
          quantity: Number(data.quantities[variantId]),
        })),
        eventDate: data.eventDate,
        rentalStartDate: data.rentalStartDate,
        rentalEndDate: data.rentalEndDate,
        venueName: data.venueName,
        venueAddress: data.venueAddress,
        city: data.city,
        state: data.state,
        plannerNotes: data.plannerNotes,
        delivery: {
          deliveryDate: data.deliveryDate,
          preferredDeliveryTime: data.preferredDeliveryTime,
          venueName: data.venueName,
          venueAddress: data.venueAddress,
          city: data.city,
          state: data.state,
          contactPerson: data.contactPerson,
          contactPhone: data.contactPhone,
          deliveryNotes: data.deliveryNotes,
          pickupDate: data.pickupDate,
          preferredPickupTime: data.preferredPickupTime,
        },
      });
      setCreated({ id: createdRequest.id, vendorName: createdRequest.vendorName ?? vendorName });
      track("booking_request_created");
      toast.success("Booking request sent");
      setStep(5);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : bookingErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <SkeletonLoader variant="text" count={8} />;
  }

  if (!project) {
    return (
      <EmptyState
        title="Project not found"
        description="This project is unavailable."
        actionLabel="Back to projects"
        onAction={() => router.push(ROUTES.planner.projects)}
      />
    );
  }

  if (products.length === 0) {
    return (
      <EmptyState
        title="Add products first"
        description="Booking requests are built from products on this project."
        actionLabel="Browse marketplace"
        onAction={() => router.push(ROUTES.planner.projectMarketplace(projectId))}
      />
    );
  }

  if (created) {
    return (
      <Card className="mx-auto max-w-xl rounded-2xl">
        <CardHeader>
          <CardTitle>Request sent successfully</CardTitle>
          <CardDescription>
            {vendorName} will review availability and respond. Inventory is not reserved until they
            accept.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm">
            Request ID <span className="font-mono text-xs">{created.id}</span>
          </p>
          <p className="text-muted-foreground text-sm">
            Vendor: {created.vendorName} · Status: Pending
          </p>
          <div className="flex flex-wrap gap-2">
            <Button className="rounded-xl" asChild>
              <Link href={ROUTES.planner.bookingRequest(created.id)}>View request</Link>
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <Link href={ROUTES.planner.project(projectId)}>Return to project</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <Button variant="ghost" className="mb-3 -ml-2 rounded-xl" asChild>
          <Link href={ROUTES.planner.project(projectId)}>
            <ArrowLeft className="size-4" />
            {project.name}
          </Link>
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Request booking</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Send an independent request to one vendor. You can repeat this flow for other vendors.
        </p>
      </div>

      <div className="space-y-2">
        <div className="text-muted-foreground flex items-center justify-between text-xs">
          <span>
            Step {step + 1} of {STEPS.length}
          </span>
          <span>{STEPS[step]}</span>
        </div>
        <Progress value={((step + 1) / STEPS.length) * 100} />
      </div>

      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle>{STEPS[step]}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          {step === 0 ? (
            <div className="space-y-3">
              {vendors.map((vendor) => (
                <label
                  key={vendor.id}
                  className="border-border/70 has-[:checked]:border-ring flex cursor-pointer items-center justify-between rounded-xl border px-4 py-3"
                >
                  <div>
                    <p className="text-sm font-medium">{vendor.name}</p>
                    <p className="text-muted-foreground text-xs">
                      {vendor.count} product{vendor.count === 1 ? "" : "s"} on this project
                      {vendor.verified ? " · Verified" : ""}
                    </p>
                  </div>
                  <input
                    type="radio"
                    name="vendorId"
                    checked={values.vendorId === vendor.id}
                    onChange={() => {
                      form.setValue("vendorId", vendor.id);
                      form.setValue("variantIds", []);
                    }}
                  />
                </label>
              ))}
            </div>
          ) : null}

          {step === 1 ? (
            <div className="space-y-3">
              <p className="text-muted-foreground text-xs">
                {checkingStock
                  ? "Checking availability…"
                  : "Select products and set quantities. Stay within the available units shown for each item."}
              </p>
              {vendorProducts.map((product) => {
                const variantId = product.productVariantId ?? "";
                const checked = values.variantIds.includes(variantId);
                const requested = Number(values.quantities[variantId] ?? product.quantity);
                const stock = stockFor(product);
                const over = Boolean(checked && stock && requested > stock.available);
                return (
                  <div
                    key={product.id}
                    className={cn(
                      "border-border/70 rounded-xl border px-4 py-3",
                      checked && "border-ring/60"
                    )}
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start">
                      <label className="flex min-w-0 flex-1 cursor-pointer items-start gap-3">
                        <Checkbox
                          checked={checked}
                          onCheckedChange={(value) => {
                            const next = value
                              ? [...values.variantIds, variantId]
                              : values.variantIds.filter((id) => id !== variantId);
                            form.setValue("variantIds", next);
                            if (value && values.quantities[variantId] == null) {
                              form.setValue("quantities", {
                                ...values.quantities,
                                [variantId]: product.quantity,
                              });
                            }
                          }}
                        />
                        <div className="min-w-0">
                          <p className="text-sm font-medium">{product.productName}</p>
                          <p className="text-muted-foreground text-xs">
                            {product.variantName} ·{" "}
                            {formatEstimatedRental(toMoney(product.snapshotPricePerDay))}/day
                          </p>
                        </div>
                      </label>
                      <div className="w-full shrink-0 sm:w-28">
                        <Label htmlFor={`qty-${variantId}`} className="sr-only">
                          Quantity for {product.productName}
                        </Label>
                        <Input
                          id={`qty-${variantId}`}
                          type="number"
                          min={1}
                          max={stock?.available}
                          disabled={!checked}
                          value={String(values.quantities[variantId] ?? product.quantity)}
                          onChange={(event) =>
                            form.setValue("quantities", {
                              ...values.quantities,
                              [variantId]: event.target.value,
                            })
                          }
                          className="h-10 rounded-xl"
                        />
                      </div>
                    </div>
                    {stock ? (
                      <p
                        className={cn(
                          "mt-2 text-xs sm:pl-8",
                          over ? "text-destructive" : "text-muted-foreground"
                        )}
                      >
                        {over
                          ? formatAvailabilityHint({
                              productName: product.productName,
                              variantName: product.variantName,
                              available: stock.available,
                              requested,
                              datesKnown: datesChosen || stock.source === "dates",
                            })
                          : `${stock.available} units available${
                              datesChosen || stock.source === "dates"
                                ? " for the selected dates"
                                : ""
                            }`}
                      </p>
                    ) : (
                      <p className="text-muted-foreground mt-2 text-xs sm:pl-8">
                        Availability is confirmed after you pick rental dates.
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          ) : null}

          {step === 2 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="bg-muted/40 rounded-xl px-4 py-3 text-sm sm:col-span-2">
                <p className="font-medium">{project.name}</p>
                <p className="text-muted-foreground">
                  {EVENT_TYPE_LABELS[project.eventType]}
                  {project.guestCount ? ` · ${project.guestCount} guests` : ""}
                  {project.venueName ? ` · ${project.venueName}` : ""}
                </p>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="eventDate">Event date</Label>
                <Input id="eventDate" type="date" {...form.register("eventDate")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="rentalStartDate">Rental start</Label>
                <Input id="rentalStartDate" type="date" {...form.register("rentalStartDate")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="rentalEndDate">Rental end</Label>
                <Input id="rentalEndDate" type="date" {...form.register("rentalEndDate")} />
              </div>
              <p className="text-muted-foreground text-xs sm:col-span-2">
                Inclusive rental days: {days || "—"} · Project date{" "}
                {formatEventDate(project.eventDate) ?? "TBD"}
              </p>
            </div>
          ) : null}

          {step === 3 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="deliveryDate">Delivery date</Label>
                <Input id="deliveryDate" type="date" {...form.register("deliveryDate")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="preferredDeliveryTime">Preferred delivery time</Label>
                <Input
                  id="preferredDeliveryTime"
                  type="time"
                  {...form.register("preferredDeliveryTime")}
                />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="venueName">Venue name</Label>
                <Input id="venueName" {...form.register("venueName")} />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="venueAddress">Venue address</Label>
                <Input id="venueAddress" {...form.register("venueAddress")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="city">City</Label>
                <Input id="city" {...form.register("city")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="state">State</Label>
                <Input id="state" {...form.register("state")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="contactPerson">Contact person</Label>
                <Input id="contactPerson" {...form.register("contactPerson")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="contactPhone">Contact phone</Label>
                <Input id="contactPhone" {...form.register("contactPhone")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="pickupDate">Pickup date</Label>
                <Input id="pickupDate" type="date" {...form.register("pickupDate")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="preferredPickupTime">Preferred pickup time</Label>
                <Input
                  id="preferredPickupTime"
                  type="time"
                  {...form.register("preferredPickupTime")}
                />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="deliveryNotes">Delivery notes</Label>
                <Textarea id="deliveryNotes" rows={3} {...form.register("deliveryNotes")} />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="plannerNotes">Planner notes</Label>
                <Textarea id="plannerNotes" rows={3} {...form.register("plannerNotes")} />
              </div>
            </div>
          ) : null}

          {step === 4 ? (
            <div className="space-y-4 text-sm">
              <p>
                <span className="text-muted-foreground">Vendor</span>
                <br />
                {vendorName}
              </p>
              <div className="divide-y rounded-xl border">
                {selectedProducts.map((product) => {
                  const quantity = Number(values.quantities[product.productVariantId ?? ""] ?? 0);
                  const line = lineEstimatedValue(
                    toMoney(product.snapshotPricePerDay),
                    quantity,
                    days
                  );
                  const stock = stockFor(product);
                  const over = Boolean(stock && quantity > stock.available);
                  return (
                    <div
                      key={product.id}
                      className="flex items-start justify-between gap-3 px-4 py-3"
                    >
                      <div>
                        <p className="font-medium">{product.productName}</p>
                        <p className="text-muted-foreground text-xs">
                          {product.variantName} · {quantity} units ·{" "}
                          {formatEstimatedRental(toMoney(product.snapshotPricePerDay))}/day
                        </p>
                        {stock ? (
                          <p
                            className={cn(
                              "mt-1 text-xs",
                              over ? "text-destructive" : "text-muted-foreground"
                            )}
                          >
                            {over
                              ? formatAvailabilityHint({
                                  productName: product.productName,
                                  variantName: product.variantName,
                                  available: stock.available,
                                  requested: quantity,
                                  datesKnown: true,
                                })
                              : `${stock.available} units available for the selected dates`}
                          </p>
                        ) : null}
                      </div>
                      <p className="font-semibold tabular-nums">{formatEstimatedRental(line)}</p>
                    </div>
                  );
                })}
              </div>
              {shortages.length > 0 ? (
                <div className="border-destructive/30 bg-destructive/5 rounded-xl border px-4 py-3 text-sm">
                  <p className="text-destructive font-medium">Reduce quantity before sending</p>
                  <ul className="mt-2 space-y-1">
                    {shortages.map((item) => (
                      <li key={item.product.id} className="text-destructive text-xs">
                        {formatAvailabilityHint({
                          productName: item.product.productName,
                          variantName: item.product.variantName,
                          available: item.stock.available,
                          requested: item.requested,
                          datesKnown: true,
                        })}
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <p>
                Rental {String(values.rentalStartDate)} → {String(values.rentalEndDate)} ({days}{" "}
                days)
              </p>
              <p>
                Venue {String(values.venueName)}, {String(values.city)}
              </p>
              <p>
                Delivery {String(values.deliveryDate)} · Pickup {String(values.pickupDate)}
              </p>
              {values.plannerNotes ? <p>Notes: {String(values.plannerNotes)}</p> : null}
              <p className="text-base font-semibold">
                Estimated rental value {formatEstimatedRental(estimated)}
              </p>
              <p className="text-muted-foreground text-xs">
                This is an estimated rental value, not a final amount. Taxes, commission, and fees
                are not included.
              </p>
              <label className="flex items-start gap-3">
                <Checkbox
                  checked={Boolean(values.termsAccepted)}
                  onCheckedChange={(value) => form.setValue("termsAccepted", value === true)}
                />
                <span>
                  I understand this sends a booking request and does not reserve inventory yet.
                </span>
              </label>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <div className="flex justify-between">
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={step === 0 || submitting}
          onClick={() => setStep((current) => Math.max(0, current - 1))}
        >
          <ArrowLeft className="size-4" />
          Back
        </Button>
        {step < 4 ? (
          <Button
            className="rounded-xl"
            disabled={!canAdvance()}
            onClick={() => setStep((current) => current + 1)}
          >
            Continue
            <ArrowRight className="size-4" />
          </Button>
        ) : (
          <Button
            className="rounded-xl"
            disabled={submitting || !values.termsAccepted || shortages.length > 0}
            onClick={() => void submit()}
          >
            {submitting ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <Check className="size-4" />
            )}
            Send booking request
          </Button>
        )}
      </div>
    </div>
  );
}
