import { ORDER_TIMELINE_LABELS, formatOrderDateTime } from "@/lib/orders/labels";
import type { OrderTimelineEventDto } from "@/services/order-api";

export function OrderTimeline({ events }: { events: OrderTimelineEventDto[] }) {
  if (events.length === 0) {
    return <p className="text-muted-foreground text-sm">No timeline events yet.</p>;
  }
  return (
    <ol className="border-border/70 space-y-3 border-l pl-4">
      {events.map((event) => (
        <li key={event.id}>
          <p className="text-sm font-medium">
            {ORDER_TIMELINE_LABELS[event.eventType] ?? event.eventType}
          </p>
          <p className="text-muted-foreground text-xs">{event.description}</p>
          {event.note ? <p className="text-muted-foreground text-xs">{event.note}</p> : null}
          <p className="text-muted-foreground text-xs">{formatOrderDateTime(event.createdAt)}</p>
        </li>
      ))}
    </ol>
  );
}
