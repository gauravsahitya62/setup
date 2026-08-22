import { z } from "zod";

const optionalText = z
  .string()
  .trim()
  .optional()
  .transform((value) => (value ? value : undefined));

export const scheduleDeliverySchema = z.object({
  deliveryDate: z.string().min(1, "Delivery date is required"),
  preferredTime: optionalText,
  venueName: z.string().trim().min(1, "Venue is required").max(255),
  venueAddress: z.string().trim().min(1, "Venue address is required").max(500),
  city: optionalText,
  state: optionalText,
  contactPerson: z.string().trim().min(1, "Contact person is required").max(200),
  contactPhone: z.string().trim().min(7, "Contact phone is required").max(40),
  deliveryNotes: optionalText,
  deliveryFee: optionalText,
  securityDeposit: optionalText,
});

export const schedulePickupSchema = z
  .object({
    pickupDate: z.string().min(1, "Pickup date is required"),
    preferredTime: optionalText,
    notes: optionalText,
    deliveryDate: z.string().optional(),
  })
  .superRefine((value, ctx) => {
    if (value.deliveryDate && value.pickupDate < value.deliveryDate) {
      ctx.addIssue({
        code: "custom",
        path: ["pickupDate"],
        message: "Pickup cannot be scheduled before delivery",
      });
    }
  });

export const orderNoteSchema = z.object({
  content: z.string().trim().min(1, "Note content is required").max(4000),
});
