"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeft, ArrowRight, Check, Loader2, Plus, Trash2, Upload } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { ROUTES } from "@/constants";
import { useMyVendor } from "@/hooks/use-my-vendor";
import {
  ApiClientError,
  marketplaceApi,
  type CategoryDto,
  type ImageDto,
  type InventoryDto,
  type ProductDto,
  type ProductRequestPayload,
  type ProductStatus,
  type VariantDto,
} from "@/services";
import { formatCurrency } from "@/lib/utils";

const STEPS = [
  "Basic info",
  "Category",
  "Variants",
  "Inventory",
  "Images",
  "Pricing",
  "Review & publish",
] as const;

const emptyBasic = {
  name: "",
  slug: "",
  shortDescription: "",
  description: "",
};

const emptyVariantDraft = {
  variantName: "",
  sku: "",
  color: "",
  material: "",
  quantity: "1",
  pricePerDay: "",
};

const emptyPricing = {
  pricePerDay: "",
  securityDeposit: "",
  minimumRentalDays: "1",
  deliveryAvailable: false,
  setupAvailable: false,
  pickupAvailable: false,
  featured: false,
};

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

export function ProductWizard({ productId }: { productId?: string }) {
  const router = useRouter();
  const { vendor, loading: vendorLoading } = useMyVendor();
  const isEditing = Boolean(productId);

  const [step, setStep] = React.useState(0);
  const [saving, setSaving] = React.useState(false);
  const [loadingProduct, setLoadingProduct] = React.useState(isEditing);
  const [loadError, setLoadError] = React.useState<string | null>(null);

  const [categories, setCategories] = React.useState<CategoryDto[]>([]);
  const [basic, setBasic] = React.useState(emptyBasic);
  const [categoryId, setCategoryId] = React.useState<string>("");
  const [subcategoryId, setSubcategoryId] = React.useState<string>("");
  const [pricing, setPricing] = React.useState(emptyPricing);

  const [product, setProduct] = React.useState<ProductDto | null>(null);
  const [variants, setVariants] = React.useState<VariantDto[]>([]);
  const [variantDraft, setVariantDraft] = React.useState(emptyVariantDraft);
  const [inventories, setInventories] = React.useState<Record<string, InventoryDto>>({});
  const [images, setImages] = React.useState<ImageDto[]>([]);

  React.useEffect(() => {
    let cancelled = false;
    async function loadCategories() {
      try {
        const tree = await marketplaceApi.categories.tree();
        if (!cancelled) setCategories(tree);
      } catch {
        if (!cancelled) setCategories([]);
      }
    }
    void loadCategories();
    return () => {
      cancelled = true;
    };
  }, []);

  React.useEffect(() => {
    if (!productId) return;
    let cancelled = false;

    async function loadExisting() {
      setLoadingProduct(true);
      setLoadError(null);
      try {
        const dto = await marketplaceApi.products.getById(productId);
        if (cancelled) return;
        setProduct(dto);
        setBasic({
          name: dto.name ?? "",
          slug: dto.slug ?? "",
          shortDescription: dto.shortDescription ?? "",
          description: dto.description ?? "",
        });
        setCategoryId(dto.categoryId ?? "");
        setSubcategoryId(dto.subcategoryId ?? "");
        setPricing({
          pricePerDay:
            dto.pricePerDay != null && dto.pricePerDay !== "" ? String(dto.pricePerDay) : "",
          securityDeposit:
            dto.securityDeposit != null && dto.securityDeposit !== ""
              ? String(dto.securityDeposit)
              : "",
          minimumRentalDays: String(dto.minimumRentalDays ?? 1),
          deliveryAvailable: Boolean(dto.deliveryAvailable),
          setupAvailable: Boolean(dto.setupAvailable),
          pickupAvailable: dto.pickupAvailable !== false,
          featured: Boolean(dto.featured),
        });
        setImages(dto.images ?? []);
        const list = dto.variants?.length
          ? dto.variants
          : await marketplaceApi.variants.list(dto.id);
        if (cancelled) return;
        setVariants(list);
        const entries = await Promise.all(
          list.map(async (variant) => {
            try {
              const inv = await marketplaceApi.inventory.get(variant.id);
              return [variant.id, inv] as const;
            } catch {
              return null;
            }
          })
        );
        if (cancelled) return;
        setInventories(
          Object.fromEntries(
            entries.filter((entry): entry is readonly [string, InventoryDto] => entry !== null)
          )
        );
      } catch (error) {
        if (!cancelled) {
          setLoadError(
            error instanceof ApiClientError ? error.message : "Could not load this product"
          );
        }
      } finally {
        if (!cancelled) setLoadingProduct(false);
      }
    }

    void loadExisting();
    return () => {
      cancelled = true;
    };
  }, [productId]);

  const selectedCategory = categories.find((c) => c.id === categoryId);
  const progress = ((step + 1) / STEPS.length) * 100;

  function toPayload(status?: ProductStatus): ProductRequestPayload {
    return {
      name: basic.name.trim(),
      slug: basic.slug.trim() || undefined,
      categoryId,
      subcategoryId: subcategoryId || undefined,
      shortDescription: basic.shortDescription.trim() || undefined,
      description: basic.description.trim() || undefined,
      pricePerDay: pricing.pricePerDay
        ? Number(pricing.pricePerDay)
        : product
          ? Number(product.pricePerDay)
          : 0,
      securityDeposit: pricing.securityDeposit ? Number(pricing.securityDeposit) : undefined,
      minimumRentalDays: pricing.minimumRentalDays ? Number(pricing.minimumRentalDays) : undefined,
      deliveryAvailable: pricing.deliveryAvailable,
      setupAvailable: pricing.setupAvailable,
      pickupAvailable: pricing.pickupAvailable,
      featured: pricing.featured,
      status: status ?? product?.status ?? "DRAFT",
    };
  }

  async function ensureProduct(status?: ProductStatus): Promise<ProductDto> {
    if (!categoryId) {
      throw new ApiClientError({ message: "Choose a category first" });
    }
    const payload = toPayload(status);
    if (product) {
      const updated = await marketplaceApi.products.update(product.id, payload);
      setProduct(updated);
      return updated;
    }
    const created = await marketplaceApi.products.create(payload);
    setProduct(created);
    return created;
  }

  async function reloadVariantsAndInventory(productId: string) {
    try {
      const list = await marketplaceApi.variants.list(productId);
      setVariants(list);
      const entries = await Promise.all(
        list.map(async (variant) => {
          try {
            const inv = await marketplaceApi.inventory.get(variant.id);
            return [variant.id, inv] as const;
          } catch {
            return null;
          }
        })
      );
      setInventories(
        Object.fromEntries(
          entries.filter((entry): entry is readonly [string, InventoryDto] => entry !== null)
        )
      );
    } catch {
      // best-effort; keep whatever state we already had
    }
  }

  async function reloadImages(productId: string) {
    try {
      const fresh = await marketplaceApi.products.getById(productId);
      setImages(fresh.images);
    } catch {
      // ignore
    }
  }

  function nextFromBasic() {
    if (!basic.name.trim()) {
      toast.error("Product name is required");
      return;
    }
    setStep(1);
  }

  async function nextFromCategory() {
    if (!categoryId) {
      toast.error("Choose a category");
      return;
    }
    setSaving(true);
    try {
      const saved = await ensureProduct(product?.status ?? "DRAFT");
      await reloadVariantsAndInventory(saved.id);
      await reloadImages(saved.id);
      setStep(2);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save product");
    } finally {
      setSaving(false);
    }
  }

  async function addVariant() {
    if (!product) return;
    if (!variantDraft.variantName.trim() || !variantDraft.sku.trim()) {
      toast.error("Variant name and SKU are required");
      return;
    }
    setSaving(true);
    try {
      await marketplaceApi.variants.create(product.id, {
        variantName: variantDraft.variantName.trim(),
        sku: variantDraft.sku.trim(),
        color: variantDraft.color.trim() || undefined,
        material: variantDraft.material.trim() || undefined,
        quantity: Number(variantDraft.quantity) || 0,
        pricePerDay: variantDraft.pricePerDay ? Number(variantDraft.pricePerDay) : undefined,
        status: "ACTIVE",
      });
      setVariantDraft(emptyVariantDraft);
      await reloadVariantsAndInventory(product.id);
      toast.success("Variant added");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not add variant");
    } finally {
      setSaving(false);
    }
  }

  async function removeVariant(variantId: string) {
    if (!product) return;
    setSaving(true);
    try {
      await marketplaceApi.variants.remove(product.id, variantId);
      await reloadVariantsAndInventory(product.id);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove variant");
    } finally {
      setSaving(false);
    }
  }

  async function updateInventoryRow(variantId: string, patch: Partial<InventoryDto>) {
    const current = inventories[variantId];
    if (!current) return;
    setSaving(true);
    try {
      const updated = await marketplaceApi.inventory.update(variantId, {
        totalQuantity: patch.totalQuantity ?? current.totalQuantity,
        reserved: patch.reserved ?? current.reserved,
        maintenance: patch.maintenance ?? current.maintenance,
        damaged: patch.damaged ?? current.damaged,
      });
      setInventories((prev) => ({ ...prev, [variantId]: updated }));
      toast.success("Inventory updated");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not update inventory");
    } finally {
      setSaving(false);
    }
  }

  async function uploadImage(file: File | null) {
    if (!file || !product) {
      if (!product) toast.error("Save the product first");
      return;
    }
    setSaving(true);
    try {
      await marketplaceApi.products.addImage(product.id, file);
      await reloadImages(product.id);
      toast.success("Image uploaded");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    } finally {
      setSaving(false);
    }
  }

  async function removeImage(imageId: string) {
    if (!product) return;
    setSaving(true);
    try {
      await marketplaceApi.products.deleteImage(product.id, imageId);
      await reloadImages(product.id);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove image");
    } finally {
      setSaving(false);
    }
  }

  async function nextFromPricing() {
    if (!pricing.pricePerDay || Number(pricing.pricePerDay) <= 0) {
      toast.error("Enter a valid price per day");
      return;
    }
    setSaving(true);
    try {
      await ensureProduct(product?.status ?? "DRAFT");
      setStep(6);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save pricing");
    } finally {
      setSaving(false);
    }
  }

  async function saveDraft() {
    if (!basic.name.trim()) {
      toast.error("Product name is required to save a draft");
      setStep(0);
      return;
    }
    if (!categoryId) {
      toast.error("Choose a category to save a draft");
      setStep(1);
      return;
    }
    const keepPublished = isEditing && product?.status === "PUBLISHED";
    setSaving(true);
    try {
      await ensureProduct(keepPublished ? "PUBLISHED" : "DRAFT");
      toast.success(keepPublished ? "Changes saved" : "Draft saved");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save draft");
    } finally {
      setSaving(false);
    }
  }

  async function publish() {
    if (!pricing.pricePerDay || Number(pricing.pricePerDay) <= 0) {
      toast.error("Set a price per day before publishing");
      setStep(5);
      return;
    }
    const alreadyPublished = product?.status === "PUBLISHED";
    setSaving(true);
    try {
      await ensureProduct("PUBLISHED");
      toast.success(alreadyPublished ? "Changes saved" : "Product published");
      router.push(ROUTES.vendor.catalog);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not publish product");
    } finally {
      setSaving(false);
    }
  }

  if (vendorLoading || loadingProduct) {
    return (
      <div className="text-muted-foreground flex items-center justify-center py-24">
        <Loader2 className="size-5 animate-spin" />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="mx-auto max-w-xl space-y-4 py-16 text-center">
        <h1 className="text-xl font-semibold">Could not open this product</h1>
        <p className="text-muted-foreground text-sm">{loadError}</p>
        <Button className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.catalog}>Back to inventory</Link>
        </Button>
      </div>
    );
  }

  if (!vendor) {
    return (
      <div className="mx-auto max-w-xl space-y-4 py-16 text-center">
        <h1 className="text-xl font-semibold">Set up your storefront first</h1>
        <p className="text-muted-foreground text-sm">
          You need a vendor profile before you can add products to your catalog.
        </p>
        <Button className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.profileWizard}>Set up storefront</Link>
        </Button>
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
            {isEditing
              ? "Update this rental product. You can save changes at any time."
              : "Add a new rental product to your catalog. You can save a draft at any time."}
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.catalog}>Exit</Link>
        </Button>
      </div>

      <div className="space-y-2">
        <Progress value={progress} className="h-1.5" />
        <div className="flex flex-wrap gap-1.5">
          {STEPS.map((label, index) => (
            <Badge
              key={label}
              variant={index === step ? "default" : index < step ? "secondary" : "outline"}
              className={`rounded-md text-[0.7rem] ${product ? "cursor-pointer" : ""}`}
              onClick={() => {
                if (product) setStep(index);
              }}
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
              ? "Review your product, then publish or keep as draft."
              : "Complete this step to continue. Fields marked optional can be filled later."}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {step === 0 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Product name *" className="sm:col-span-2">
                <Input
                  value={basic.name}
                  onChange={(e) => setBasic((b) => ({ ...b, name: e.target.value }))}
                  placeholder="Cross-Back Dining Chair"
                />
              </Field>
              <Field label="Slug (optional)">
                <Input
                  value={basic.slug}
                  onChange={(e) => setBasic((b) => ({ ...b, slug: e.target.value }))}
                  placeholder="cross-back-dining-chair"
                />
              </Field>
              <Field label="Short description">
                <Input
                  value={basic.shortDescription}
                  onChange={(e) => setBasic((b) => ({ ...b, shortDescription: e.target.value }))}
                  placeholder="Natural wood finish, woven seat"
                />
              </Field>
              <Field label="Full description" className="sm:col-span-2">
                <Textarea
                  value={basic.description}
                  onChange={(e) => setBasic((b) => ({ ...b, description: e.target.value }))}
                  rows={5}
                />
              </Field>
            </div>
          ) : null}

          {step === 1 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Category *">
                <Select
                  value={categoryId}
                  onValueChange={(value) => {
                    setCategoryId(value);
                    setSubcategoryId("");
                  }}
                >
                  <SelectTrigger className="w-full rounded-xl">
                    <SelectValue placeholder="Choose a category" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id}>
                        {cat.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
              {selectedCategory && selectedCategory.children.length > 0 ? (
                <Field label="Subcategory (optional)">
                  <Select value={subcategoryId} onValueChange={setSubcategoryId}>
                    <SelectTrigger className="w-full rounded-xl">
                      <SelectValue placeholder="Choose a subcategory" />
                    </SelectTrigger>
                    <SelectContent>
                      {selectedCategory.children.map((child) => (
                        <SelectItem key={child.id} value={child.id}>
                          {child.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              ) : null}
            </div>
          ) : null}

          {step === 2 ? (
            <div className="space-y-4">
              {!product ? (
                <p className="text-muted-foreground text-sm">Save the category step first.</p>
              ) : (
                <>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <Field label="Variant name *">
                      <Input
                        value={variantDraft.variantName}
                        onChange={(e) =>
                          setVariantDraft((v) => ({ ...v, variantName: e.target.value }))
                        }
                        placeholder="Standard"
                      />
                    </Field>
                    <Field label="SKU *">
                      <Input
                        value={variantDraft.sku}
                        onChange={(e) => setVariantDraft((v) => ({ ...v, sku: e.target.value }))}
                        placeholder="ATL-1001"
                      />
                    </Field>
                    <Field label="Quantity *">
                      <Input
                        inputMode="numeric"
                        value={variantDraft.quantity}
                        onChange={(e) =>
                          setVariantDraft((v) => ({ ...v, quantity: e.target.value }))
                        }
                      />
                    </Field>
                    <Field label="Color">
                      <Input
                        value={variantDraft.color}
                        onChange={(e) => setVariantDraft((v) => ({ ...v, color: e.target.value }))}
                      />
                    </Field>
                    <Field label="Material">
                      <Input
                        value={variantDraft.material}
                        onChange={(e) =>
                          setVariantDraft((v) => ({ ...v, material: e.target.value }))
                        }
                      />
                    </Field>
                    <Field label="Price per day (optional)">
                      <Input
                        inputMode="decimal"
                        value={variantDraft.pricePerDay}
                        onChange={(e) =>
                          setVariantDraft((v) => ({ ...v, pricePerDay: e.target.value }))
                        }
                        placeholder="Defaults to product price"
                      />
                    </Field>
                  </div>
                  <Button
                    type="button"
                    className="rounded-xl"
                    disabled={saving}
                    onClick={() => void addVariant()}
                  >
                    <Plus className="size-4" />
                    Add variant
                  </Button>

                  <ul className="space-y-2">
                    {variants.map((variant) => (
                      <li
                        key={variant.id}
                        className="border-border/70 flex items-center justify-between rounded-xl border px-3 py-2 text-sm"
                      >
                        <span>
                          {variant.variantName} · {variant.sku} · qty {variant.quantity}
                        </span>
                        <Button
                          type="button"
                          size="icon-sm"
                          variant="ghost"
                          onClick={() => void removeVariant(variant.id)}
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </li>
                    ))}
                    {variants.length === 0 ? (
                      <p className="text-muted-foreground text-sm">
                        No variants yet — add at least one to publish.
                      </p>
                    ) : null}
                  </ul>
                </>
              )}
            </div>
          ) : null}

          {step === 3 ? (
            <div className="space-y-3">
              {variants.length === 0 ? (
                <p className="text-muted-foreground text-sm">
                  Add variants in the previous step to manage inventory.
                </p>
              ) : (
                variants.map((variant) => {
                  const inv = inventories[variant.id];
                  return (
                    <div key={variant.id} className="border-border/70 rounded-xl border p-3">
                      <p className="mb-2 text-sm font-medium">
                        {variant.variantName} · {variant.sku}
                      </p>
                      {inv ? (
                        <div className="grid gap-3 sm:grid-cols-4">
                          <Field label="Total">
                            <Input
                              inputMode="numeric"
                              defaultValue={inv.totalQuantity}
                              onBlur={(e) =>
                                void updateInventoryRow(variant.id, {
                                  totalQuantity: Number(e.target.value) || 0,
                                })
                              }
                            />
                          </Field>
                          <Field label="Reserved">
                            <Input
                              inputMode="numeric"
                              defaultValue={inv.reserved}
                              onBlur={(e) =>
                                void updateInventoryRow(variant.id, {
                                  reserved: Number(e.target.value) || 0,
                                })
                              }
                            />
                          </Field>
                          <Field label="Maintenance">
                            <Input
                              inputMode="numeric"
                              defaultValue={inv.maintenance}
                              onBlur={(e) =>
                                void updateInventoryRow(variant.id, {
                                  maintenance: Number(e.target.value) || 0,
                                })
                              }
                            />
                          </Field>
                          <Field label="Damaged">
                            <Input
                              inputMode="numeric"
                              defaultValue={inv.damaged}
                              onBlur={(e) =>
                                void updateInventoryRow(variant.id, {
                                  damaged: Number(e.target.value) || 0,
                                })
                              }
                            />
                          </Field>
                        </div>
                      ) : (
                        <p className="text-muted-foreground text-xs">Loading inventory…</p>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          ) : null}

          {step === 4 ? (
            <div className="space-y-4">
              {!product ? (
                <p className="text-muted-foreground text-sm">Save the category step first.</p>
              ) : (
                <>
                  <Label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed px-4 py-3 text-sm">
                    <Upload className="size-4" />
                    Upload image
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={(e) => void uploadImage(e.target.files?.[0] ?? null)}
                    />
                  </Label>
                  <div className="grid gap-3 sm:grid-cols-3">
                    {images.map((image) => (
                      <div key={image.id} className="overflow-hidden rounded-xl border">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                          src={image.imageUrl}
                          alt={image.altText ?? "Product"}
                          className="h-32 w-full object-cover"
                        />
                        <div className="flex items-center justify-between gap-2 p-2 text-xs">
                          <span>{image.featured ? "Featured" : "Gallery"}</span>
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="ghost"
                            onClick={() => void removeImage(image.id)}
                          >
                            <Trash2 className="size-4" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          ) : null}

          {step === 5 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Price per day *">
                <Input
                  inputMode="decimal"
                  value={pricing.pricePerDay}
                  onChange={(e) => setPricing((p) => ({ ...p, pricePerDay: e.target.value }))}
                  placeholder="45"
                />
              </Field>
              <Field label="Security deposit">
                <Input
                  inputMode="decimal"
                  value={pricing.securityDeposit}
                  onChange={(e) => setPricing((p) => ({ ...p, securityDeposit: e.target.value }))}
                />
              </Field>
              <Field label="Minimum rental days">
                <Input
                  inputMode="numeric"
                  value={pricing.minimumRentalDays}
                  onChange={(e) => setPricing((p) => ({ ...p, minimumRentalDays: e.target.value }))}
                />
              </Field>
              <div className="space-y-3 sm:col-span-2">
                <label className="flex items-center gap-3">
                  <Checkbox
                    checked={pricing.deliveryAvailable}
                    onCheckedChange={(checked) =>
                      setPricing((p) => ({ ...p, deliveryAvailable: checked === true }))
                    }
                  />
                  <span className="text-sm">Delivery available</span>
                </label>
                <label className="flex items-center gap-3">
                  <Checkbox
                    checked={pricing.setupAvailable}
                    onCheckedChange={(checked) =>
                      setPricing((p) => ({ ...p, setupAvailable: checked === true }))
                    }
                  />
                  <span className="text-sm">Setup available</span>
                </label>
                <label className="flex items-center gap-3">
                  <Checkbox
                    checked={pricing.pickupAvailable}
                    onCheckedChange={(checked) =>
                      setPricing((p) => ({ ...p, pickupAvailable: checked === true }))
                    }
                  />
                  <span className="text-sm">Customer pickup available</span>
                </label>
                <label className="flex items-center gap-3">
                  <Checkbox
                    checked={pricing.featured}
                    onCheckedChange={(checked) =>
                      setPricing((p) => ({ ...p, featured: checked === true }))
                    }
                  />
                  <span className="text-sm">Feature this product</span>
                </label>
              </div>
            </div>
          ) : null}

          {step === 6 ? (
            <div className="space-y-3 text-sm">
              <ReviewRow label="Name" value={basic.name || product?.name} />
              <ReviewRow label="Category" value={selectedCategory?.name} />
              <ReviewRow
                label="Price / day"
                value={
                  pricing.pricePerDay ? formatCurrency(Number(pricing.pricePerDay)) : undefined
                }
              />
              <ReviewRow label="Variants" value={String(variants.length)} />
              <ReviewRow label="Images" value={String(images.length)} />
              <ReviewRow label="Status" value={product?.status ?? "DRAFT"} />
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
            {product?.status === "PUBLISHED" ? "Save changes" : "Save as draft"}
          </Button>
          {step < 6 ? (
            <Button
              type="button"
              className="rounded-xl"
              disabled={saving}
              onClick={() => {
                if (step === 0) nextFromBasic();
                else if (step === 1) void nextFromCategory();
                else if (step === 5) void nextFromPricing();
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
              {product?.status === "PUBLISHED" ? "Save & finish" : "Publish product"}
            </Button>
          )}
        </div>
      </div>
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
