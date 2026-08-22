"use client";

import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  VENDOR_CATEGORY_SHARE,
  VENDOR_KPI,
  VENDOR_REVENUE_SERIES,
  formatVendorMoney,
} from "@/constants/vendor-dashboard";
import { formatNumber } from "@/lib/utils";

const PIE_COLORS = ["#0f172a", "#334155", "#64748b", "#94a3b8"];

function ChartTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: Array<{ value?: number; dataKey?: string | number; name?: string }>;
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  const revenue = payload.find((p) => p.dataKey === "revenue");
  const bookings = payload.find((p) => p.dataKey === "bookings");

  return (
    <div className="border-border/80 bg-background rounded-xl border px-3 py-2 shadow-lg">
      <p className="text-muted-foreground mb-1 text-xs font-medium">{label}</p>
      {revenue ? (
        <p className="text-sm font-semibold tabular-nums">
          {formatVendorMoney(Number(revenue.value))}
        </p>
      ) : null}
      {bookings ? (
        <p className="text-muted-foreground text-xs">
          {formatNumber(Number(bookings.value))} bookings
        </p>
      ) : null}
    </div>
  );
}

function PieTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: Array<{ name?: string; value?: number }>;
}) {
  if (!active || !payload?.length) return null;
  const item = payload[0];
  return (
    <div className="border-border/80 bg-background rounded-xl border px-3 py-2 shadow-lg">
      <p className="text-sm font-medium">{item?.name}</p>
      <p className="text-muted-foreground text-xs">{item?.value}% of revenue</p>
    </div>
  );
}

export function VendorAnalyticsCharts() {
  return (
    <Card className="rounded-2xl shadow-sm">
      <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <CardTitle className="text-base">Analytics</CardTitle>
          <CardDescription>Revenue and booking volume over the last 7 months.</CardDescription>
        </div>
        <div className="flex gap-6">
          <div>
            <p className="text-muted-foreground text-xs">Avg order</p>
            <p className="text-lg font-semibold tabular-nums">
              {formatVendorMoney(VENDOR_KPI.avgOrderValue)}
            </p>
          </div>
          <div>
            <p className="text-muted-foreground text-xs">This month</p>
            <p className="text-lg font-semibold tabular-nums">
              {formatVendorMoney(VENDOR_KPI.revenueMonth)}
            </p>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-8">
        <div className="h-64 w-full sm:h-72">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart
              data={VENDOR_REVENUE_SERIES}
              margin={{ top: 8, right: 8, left: 0, bottom: 0 }}
            >
              <defs>
                <linearGradient id="vendorRevenueFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="oklch(0.25 0.02 250)" stopOpacity={0.28} />
                  <stop offset="100%" stopColor="oklch(0.25 0.02 250)" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-border/60" />
              <XAxis
                dataKey="month"
                tickLine={false}
                axisLine={false}
                tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
              />
              <YAxis
                tickLine={false}
                axisLine={false}
                tickFormatter={(v) => `$${Math.round(Number(v) / 1000)}k`}
                width={40}
                tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
              />
              <Tooltip content={<ChartTooltip />} />
              <Area
                type="monotone"
                dataKey="revenue"
                stroke="oklch(0.28 0.03 250)"
                strokeWidth={2.25}
                fill="url(#vendorRevenueFill)"
                activeDot={{ r: 4, strokeWidth: 0 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="border-border/60 grid gap-4 border-t pt-6 sm:grid-cols-[180px_1fr] sm:items-center">
          <div className="mx-auto h-40 w-40 sm:mx-0">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={VENDOR_CATEGORY_SHARE}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={48}
                  outerRadius={72}
                  paddingAngle={3}
                  strokeWidth={0}
                >
                  {VENDOR_CATEGORY_SHARE.map((entry, index) => (
                    <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip content={<PieTooltip />} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            {VENDOR_CATEGORY_SHARE.map((entry, index) => (
              <div key={entry.name} className="flex items-center gap-2.5 text-sm">
                <span
                  className="size-2.5 rounded-full"
                  style={{ backgroundColor: PIE_COLORS[index % PIE_COLORS.length] }}
                />
                <span className="text-muted-foreground">{entry.name}</span>
                <span className="ml-auto font-medium tabular-nums">{entry.value}%</span>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
