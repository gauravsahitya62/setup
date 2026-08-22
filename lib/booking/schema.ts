import { z } from "zod";

import { REJECTION_REASONS } from "@/lib/booking/labels";

const optionalText = z
  .string()
  .trim()
  .optional()
  .transform((value) => (value ? value : undefined));

export const bookingRequestSchema = z
  .object({
    projectId: z.string().uuid("Project is required"),
    vendorId: z.string().uuid("Select a vendor"),
    variantIds: z.array(z.string().uuid()).min(1, "Select at least one product"),
    quantities: z.record(z.string(), z.union([z.number(), z.string()])),
    eventDate: z.string().min(1, "Event date is required"),
    rentalStartDate: z.string().min(1, "Rental start date is required"),
    rentalEndDate: z.string().min(1, "Rental end date is required"),
    venueName: z.string().trim().min(1, "Venue name is required").max(255),
    venueAddress: z.string().trim().min(1, "Venue address is required").max(500),
    city: z.string().trim().min(1, "City is required").max(120),
    state: z.string().trim().min(1, "State is required").max(120),
    plannerNotes: optionalText,
    deliveryDate: z.string().min(1, "Delivery date is required"),
    preferredDeliveryTime: optionalText,
    pickupDate: z.string().min(1, "Pickup date is required"),
    preferredPickupTime: optionalText,
    contactPerson: z.string().trim().min(1, "Contact person is required").max(200),
    contactPhone: z.string().trim().min(7, "Contact phone is required").max(40),
    deliveryNotes: optionalText,
    termsAccepted: z.boolean().refine((value) => value === true, {
      message: "Acknowledge the estimated rental value to send this request",
    }),
  })
  .superRefine((value, ctx) => {
    if (value.rentalEndDate < value.rentalStartDate) {
      ctx.addIssue({
        code: "custom",
        path: ["rentalEndDate"],
        message: "Rental end date must be on or after the start date",
      });
    }
    if (value.eventDate < value.rentalStartDate || value.eventDate > value.rentalEndDate) {
      ctx.addIssue({
        code: "custom",
        path: ["eventDate"],
        message: "Event date must fall within the rental period",
      });
    }
    for (const variantId of value.variantIds) {
      const raw = value.quantities[variantId];
      const quantity = typeof raw === "number" ? raw : Number(raw);
      if (!Number.isInteger(quantity) || quantity < 1) {
        ctx.addIssue({
          code: "custom",
          path: ["quantities", variantId],
          message: "Quantity must be at least 1",
        });
      }
    }
  });

export type BookingRequestFormValues = z.input<typeof bookingRequestSchema>;
export type BookingRequestFormPayload = z.output<typeof bookingRequestSchema>;

export const rejectBookingSchema = z.object({
  reason: z.enum(REJECTION_REASONS),
  note: optionalText,
});

export type RejectBookingFormValues = z.input<typeof rejectBookingSchema>;
