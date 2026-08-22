"use client";

import * as React from "react";
import { Search } from "lucide-react";
import { toast } from "sonner";

import { AdminConfirmDialog } from "@/components/admin/admin-confirm-dialog";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge, type AdminStatusTone } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/shared/modal";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { USER_STATUS_LABEL, adminErrorMessage } from "@/lib/admin/labels";
import { ApiClientError, adminApi, type AdminUserDto } from "@/services";

const TONE: Record<string, AdminStatusTone> = {
  ACTIVE: "success",
  PENDING_VERIFICATION: "warning",
  SUSPENDED: "danger",
};

export function AdminUsersPage() {
  const [query, setQuery] = React.useState("");
  const [role, setRole] = React.useState("");
  const [status, setStatus] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<AdminUserDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [pending, setPending] = React.useState<AdminUserDto | null>(null);
  const [action, setAction] = React.useState<"suspend" | "reactivate" | "reset" | null>(null);
  const [activityUser, setActivityUser] = React.useState<AdminUserDto | null>(null);
  const [activity, setActivity] = React.useState("");

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.users({
        q: query || undefined,
        role: role || undefined,
        status: status || undefined,
        page,
        size: 20,
      });
      setRows(result.content);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [query, role, status, page]);

  React.useEffect(() => {
    const handle = window.setTimeout(() => void load(), 250);
    return () => window.clearTimeout(handle);
  }, [load]);

  async function confirm(reason: string) {
    if (!pending || !action) return;
    try {
      if (action === "suspend") await adminApi.suspendUser(pending.id, reason);
      if (action === "reactivate") await adminApi.reactivateUser(pending.id);
      if (action === "reset") await adminApi.resetUserPassword(pending.id);
      toast.success("Saved");
      setPending(null);
      setAction(null);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Users"
        description="Search, filter, suspend, and send password reset emails. Passwords are never set directly."
      />

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative min-w-0 flex-1 sm:max-w-md">
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
          <Input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Search name, email, phone"
            className="rounded-xl pl-9"
          />
        </div>
        <select
          className="bg-background rounded-xl border px-3 py-2 text-sm"
          value={role}
          onChange={(e) => {
            setRole(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All roles</option>
          <option value="PLANNER">Planner</option>
          <option value="VENDOR">Vendor</option>
          <option value="ADMIN">Admin</option>
        </select>
        <select
          className="bg-background rounded-xl border px-3 py-2 text-sm"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="PENDING_VERIFICATION">Pending</option>
        </select>
      </div>

      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load users" description={error} /> : null}
      {!loading && !error && rows.length === 0 ? (
        <EmptyState title="No users match these filters" />
      ) : null}

      <div className="overflow-x-auto rounded-2xl border">
        <table className="w-full text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-left text-xs tracking-wide uppercase">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Phone</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Created</th>
              <th className="px-4 py-3">Last login</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((user) => (
              <tr key={user.id} className="border-t">
                <td className="px-4 py-3 font-medium">
                  {user.firstName} {user.lastName}
                </td>
                <td className="px-4 py-3">{user.email}</td>
                <td className="px-4 py-3">{user.phone ?? "—"}</td>
                <td className="px-4 py-3">{user.role}</td>
                <td className="px-4 py-3">
                  <AdminStatusBadge
                    label={USER_STATUS_LABEL[user.status] ?? user.status}
                    tone={TONE[user.status]}
                  />
                </td>
                <td className="px-4 py-3">{new Date(user.createdAt).toLocaleDateString()}</td>
                <td className="px-4 py-3">
                  {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : "—"}
                </td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {user.status !== "SUSPENDED" ? (
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-lg"
                        onClick={() => {
                          setPending(user);
                          setAction("suspend");
                        }}
                      >
                        Suspend
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-lg"
                        onClick={() => {
                          setPending(user);
                          setAction("reactivate");
                        }}
                      >
                        Reactivate
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="ghost"
                      className="rounded-lg"
                      onClick={() => {
                        setPending(user);
                        setAction("reset");
                      }}
                    >
                      Reset password
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="rounded-lg"
                      onClick={() => {
                        setActivityUser(user);
                        void adminApi.userActivity(user.id).then((result) => {
                          setActivity(
                            result.content
                              .map(
                                (row) =>
                                  `${new Date(row.createdAt).toLocaleString()} · ${row.action}`
                              )
                              .join("\n") || "No activity recorded."
                          );
                        });
                      }}
                    >
                      Activity
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex justify-end gap-2">
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </Button>
      </div>

      <Modal
        open={Boolean(activityUser)}
        onOpenChange={(open) => {
          if (!open) {
            setActivityUser(null);
            setActivity("");
          }
        }}
        title={activityUser ? `${activityUser.firstName} ${activityUser.lastName}` : "Activity"}
        description={activityUser?.email}
      >
        <pre className="max-h-80 overflow-auto text-xs whitespace-pre-wrap">
          {activity || "Loading…"}
        </pre>
      </Modal>

      <AdminConfirmDialog
        open={Boolean(pending && action)}
        title={
          action === "suspend"
            ? "Suspend user"
            : action === "reactivate"
              ? "Reactivate user"
              : "Send password reset"
        }
        description={
          action === "reset"
            ? "A reset email will be sent through the existing secure flow. The password is never set by an admin."
            : "This is recorded in the audit log."
        }
        confirmLabel={
          action === "suspend" ? "Suspend" : action === "reactivate" ? "Reactivate" : "Send email"
        }
        requireReason={action === "suspend"}
        onOpenChange={(open) => {
          if (!open) {
            setPending(null);
            setAction(null);
          }
        }}
        onConfirm={confirm}
      />
    </div>
  );
}
