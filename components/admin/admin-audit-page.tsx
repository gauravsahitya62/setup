"use client";

import * as React from "react";
import { Search } from "lucide-react";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { adminErrorMessage } from "@/lib/admin/labels";
import { ApiClientError, adminApi, type AuditLogDto } from "@/services";

export function AdminAuditPage() {
  const [query, setQuery] = React.useState("");
  const [action, setAction] = React.useState("");
  const [resourceType, setResourceType] = React.useState("");
  const [from, setFrom] = React.useState("");
  const [to, setTo] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<AuditLogDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.auditLogs({
        action: action || undefined,
        resourceType: resourceType || undefined,
        resourceId: query.match(/^[0-9a-f-]{36}$/i) ? query : undefined,
        actorUserId: undefined,
        from: from ? new Date(`${from}T00:00:00.000Z`).toISOString() : undefined,
        to: to ? new Date(`${to}T23:59:59.999Z`).toISOString() : undefined,
        page,
        size: 20,
      });
      const q = query.trim().toLowerCase();
      setRows(
        q && !query.match(/^[0-9a-f-]{36}$/i)
          ? result.content.filter(
              (row) =>
                (row.actorName ?? "").toLowerCase().includes(q) ||
                row.action.toLowerCase().includes(q) ||
                (row.resourceType ?? "").toLowerCase().includes(q) ||
                (row.metadata ?? "").toLowerCase().includes(q)
            )
          : result.content
      );
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [action, resourceType, query, from, to, page]);

  React.useEffect(() => {
    const handle = window.setTimeout(() => void load(), 250);
    return () => window.clearTimeout(handle);
  }, [load]);

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Audit logs"
        description="Immutable trail of privileged actions. Logs cannot be edited or deleted."
      />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
        <div className="relative md:col-span-2 xl:col-span-1">
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
          <Input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Actor, action, resource ID"
            className="rounded-xl pl-9"
          />
        </div>
        <Input
          value={action}
          onChange={(e) => {
            setAction(e.target.value);
            setPage(0);
          }}
          placeholder="Action"
          className="rounded-xl"
        />
        <Input
          value={resourceType}
          onChange={(e) => {
            setResourceType(e.target.value);
            setPage(0);
          }}
          placeholder="Resource type"
          className="rounded-xl"
        />
        <Input
          type="date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setPage(0);
          }}
          className="rounded-xl"
        />
        <Input
          type="date"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            setPage(0);
          }}
          className="rounded-xl"
        />
      </div>

      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load audit logs" description={error} /> : null}
      {!loading && !error && rows.length === 0 ? (
        <EmptyState title="No audit events match" />
      ) : null}

      <div className="overflow-x-auto rounded-2xl border">
        <table className="w-full min-w-[920px] text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-left text-xs tracking-wide uppercase">
            <tr>
              <th className="px-4 py-3">Timestamp</th>
              <th className="px-4 py-3">Admin/User</th>
              <th className="px-4 py-3">Action</th>
              <th className="px-4 py-3">Resource</th>
              <th className="px-4 py-3">Resource ID</th>
              <th className="px-4 py-3">IP</th>
              <th className="px-4 py-3">Details</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((log) => (
              <tr key={log.id} className="border-t">
                <td className="text-muted-foreground px-4 py-3 whitespace-nowrap">
                  {new Date(log.createdAt).toLocaleString()}
                </td>
                <td className="px-4 py-3 font-medium">
                  {log.actorName ?? log.actorUserId ?? "System"}
                </td>
                <td className="px-4 py-3">
                  <AdminStatusBadge
                    label={log.action.replaceAll("_", " ")}
                    tone="neutral"
                    className="normal-case"
                  />
                </td>
                <td className="px-4 py-3">{log.resourceType ?? "—"}</td>
                <td className="px-4 py-3 font-mono text-xs">{log.resourceId ?? "—"}</td>
                <td className="text-muted-foreground px-4 py-3 font-mono text-xs">
                  {log.ipAddress ?? "—"}
                </td>
                <td className="text-muted-foreground max-w-xs px-4 py-3">{log.metadata ?? "—"}</td>
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
    </div>
  );
}
