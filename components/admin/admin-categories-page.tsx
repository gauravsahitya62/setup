"use client";

import * as React from "react";
import { Plus, Search, Star } from "lucide-react";
import { toast } from "sonner";

import { AdminConfirmDialog } from "@/components/admin/admin-confirm-dialog";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/shared/modal";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { CATEGORY_STATUS_LABEL, adminErrorMessage } from "@/lib/admin/labels";
import {
  ApiClientError,
  adminApi,
  flattenCategories,
  type CategoryDto,
  type CategoryStatus,
} from "@/services";

interface CategoryDraft {
  name: string;
  slug: string;
  description: string;
  parentCategoryId: string;
  status: CategoryStatus;
}

const EMPTY_DRAFT: CategoryDraft = {
  name: "",
  slug: "",
  description: "",
  parentCategoryId: "",
  status: "ACTIVE",
};

export function AdminCategoriesPage() {
  const [query, setQuery] = React.useState("");
  const [tree, setTree] = React.useState<CategoryDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [formOpen, setFormOpen] = React.useState(false);
  const [editing, setEditing] = React.useState<CategoryDto | null>(null);
  const [draft, setDraft] = React.useState<CategoryDraft>(EMPTY_DRAFT);
  const [archiveTarget, setArchiveTarget] = React.useState<CategoryDto | null>(null);

  const flat = React.useMemo(() => flattenCategories(tree), [tree]);
  const filtered = React.useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return flat;
    return flat.filter((c) => c.name.toLowerCase().includes(q) || c.slug.toLowerCase().includes(q));
  }, [flat, query]);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setTree(await adminApi.categories());
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    setDraft(EMPTY_DRAFT);
    setFormOpen(true);
  }

  function openEdit(category: CategoryDto) {
    setEditing(category);
    setDraft({
      name: category.name,
      slug: category.slug,
      description: category.description ?? "",
      parentCategoryId: category.parentCategoryId ?? "",
      status: category.status,
    });
    setFormOpen(true);
  }

  async function save() {
    const payload = {
      name: draft.name,
      slug: draft.slug || undefined,
      description: draft.description || undefined,
      parentCategoryId: draft.parentCategoryId || undefined,
      status: draft.status,
    };
    try {
      if (editing) {
        await adminApi.updateCategory(editing.id, payload);
      } else {
        await adminApi.createCategory(payload);
      }
      toast.success("Category saved");
      setFormOpen(false);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function toggleFeatured(category: CategoryDto) {
    try {
      await adminApi.featureCategory(category.id, !category.featured);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function move(category: CategoryDto, direction: -1 | 1) {
    const siblings = flat
      .filter((item) => (item.parentCategoryId ?? "") === (category.parentCategoryId ?? ""))
      .sort((a, b) => a.displayOrder - b.displayOrder);
    const index = siblings.findIndex((item) => item.id === category.id);
    const next = index + direction;
    if (index < 0 || next < 0 || next >= siblings.length) return;
    const ordered = [...siblings];
    const [moved] = ordered.splice(index, 1);
    ordered.splice(next, 0, moved!);
    try {
      await adminApi.reorderCategories(ordered.map((item) => item.id));
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function uploadImage(category: CategoryDto, file: File) {
    try {
      await adminApi.uploadCategoryImage(category.id, file);
      toast.success("Image uploaded");
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Category management"
        description="Create, edit, archive, reorder, and feature categories. Archiving keeps products assigned."
        actions={
          <Button className="rounded-xl" onClick={openCreate}>
            <Plus className="size-4" />
            Add category
          </Button>
        }
      />

      <div className="relative max-w-md">
        <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search categories"
          className="rounded-xl pl-9"
        />
      </div>

      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load categories" description={error} /> : null}
      {!loading && !error && filtered.length === 0 ? (
        <EmptyState title="No categories match" />
      ) : null}

      <div className="overflow-x-auto rounded-2xl border">
        <table className="w-full min-w-[860px] text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-left text-xs tracking-wide uppercase">
            <tr>
              <th className="px-4 py-3">Category</th>
              <th className="px-4 py-3">Parent</th>
              <th className="px-4 py-3">Featured</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Updated</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((category) => {
              const parent = flat.find((item) => item.id === category.parentCategoryId);
              return (
                <tr key={category.id} className="border-t">
                  <td className="px-4 py-3">
                    <p className="font-medium">{category.name}</p>
                    <p className="text-muted-foreground text-xs">{category.slug}</p>
                  </td>
                  <td className="text-muted-foreground px-4 py-3">{parent?.name ?? "—"}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={Boolean(category.featured)}
                        onCheckedChange={() => void toggleFeatured(category)}
                      />
                      {category.featured ? (
                        <Star className="size-3.5 fill-amber-400 text-amber-400" />
                      ) : null}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <AdminStatusBadge
                      label={CATEGORY_STATUS_LABEL[category.status] ?? category.status}
                      tone={category.status === "ACTIVE" ? "success" : "neutral"}
                    />
                  </td>
                  <td className="text-muted-foreground px-4 py-3">
                    {new Date(category.updatedAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-lg"
                        onClick={() => openEdit(category)}
                      >
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="rounded-lg"
                        onClick={() => void move(category, -1)}
                      >
                        Up
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="rounded-lg"
                        onClick={() => void move(category, 1)}
                      >
                        Down
                      </Button>
                      {category.status !== "ARCHIVED" ? (
                        <Button
                          size="sm"
                          variant="outline"
                          className="rounded-lg"
                          onClick={() => setArchiveTarget(category)}
                        >
                          Archive
                        </Button>
                      ) : null}
                      <label className="text-muted-foreground hover:bg-muted inline-flex cursor-pointer items-center rounded-lg px-2 py-1 text-xs">
                        Image
                        <input
                          type="file"
                          accept="image/*"
                          className="hidden"
                          onChange={(event) => {
                            const file = event.target.files?.[0];
                            if (file) void uploadImage(category, file);
                          }}
                        />
                      </label>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <Modal
        open={formOpen}
        onOpenChange={setFormOpen}
        title={editing ? "Edit category" : "Create category"}
        description="Slugs must be unique. Circular parents are rejected by the server."
        footer={
          <>
            <Button variant="outline" className="rounded-xl" onClick={() => setFormOpen(false)}>
              Cancel
            </Button>
            <Button
              className="rounded-xl"
              onClick={() => void save()}
              disabled={!draft.name.trim()}
            >
              Save
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="space-y-1">
            <Label htmlFor="category-name">Name</Label>
            <Input
              id="category-name"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="rounded-xl"
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="category-slug">Slug</Label>
            <Input
              id="category-slug"
              value={draft.slug}
              onChange={(e) => setDraft({ ...draft, slug: e.target.value })}
              className="rounded-xl"
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="category-parent">Parent</Label>
            <select
              id="category-parent"
              className="bg-background w-full rounded-xl border px-3 py-2 text-sm"
              value={draft.parentCategoryId}
              onChange={(e) => setDraft({ ...draft, parentCategoryId: e.target.value })}
            >
              <option value="">None</option>
              {flat
                .filter((item) => item.id !== editing?.id)
                .map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
            </select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="category-status">Status</Label>
            <select
              id="category-status"
              className="bg-background w-full rounded-xl border px-3 py-2 text-sm"
              value={draft.status}
              onChange={(e) => setDraft({ ...draft, status: e.target.value as CategoryStatus })}
            >
              <option value="ACTIVE">Active</option>
              <option value="HIDDEN">Hidden</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="category-description">Description</Label>
            <Textarea
              id="category-description"
              value={draft.description}
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              className="rounded-xl"
            />
          </div>
        </div>
      </Modal>

      <AdminConfirmDialog
        open={Boolean(archiveTarget)}
        title="Archive category"
        description="Products stay assigned. They will not silently disappear from vendor catalogs."
        confirmLabel="Archive"
        onOpenChange={(open) => {
          if (!open) setArchiveTarget(null);
        }}
        onConfirm={async () => {
          if (!archiveTarget) return;
          try {
            await adminApi.archiveCategory(archiveTarget.id);
            toast.success("Category archived");
            setArchiveTarget(null);
            await load();
          } catch (err) {
            toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
          }
        }}
      />
    </div>
  );
}
