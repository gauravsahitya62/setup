import { Badge } from "@/components/ui/badge";
import {
  ORDER_STATUS_ICONS,
  ORDER_STATUS_LABELS,
  ORDER_STATUS_STYLES,
  type OrderStatus,
} from "@/lib/orders/labels";
import { cn } from "@/lib/utils";

export function OrderStatusBadge({
  status,
  className,
}: {
  status: OrderStatus;
  className?: string;
}) {
  const Icon = ORDER_STATUS_ICONS[status];
  return (
    <Badge
      className={cn(
        "inline-flex items-center gap-1 rounded-md border-0",
        ORDER_STATUS_STYLES[status],
        className
      )}
    >
      <Icon className="size-3.5 shrink-0" aria-hidden />
      <span>{ORDER_STATUS_LABELS[status]}</span>
    </Badge>
  );
}
