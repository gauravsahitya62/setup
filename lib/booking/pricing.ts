export function rentalDays(start: string, end: string): number {
  if (!start || !end) return 0;
  const from = new Date(`${start}T00:00:00`);
  const to = new Date(`${end}T00:00:00`);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || to < from) return 0;
  return Math.round((to.getTime() - from.getTime()) / 86_400_000) + 1;
}

export function lineEstimatedValue(pricePerDay: number, quantity: number, days: number): number {
  if (!Number.isFinite(pricePerDay) || !Number.isFinite(quantity) || !Number.isFinite(days))
    return 0;
  return Math.round(pricePerDay * quantity * days * 100) / 100;
}

export function formatEstimatedRental(amount: number, currency = "INR"): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function toMoney(value: number | string | null | undefined): number {
  if (value == null || value === "") return 0;
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}
