import { z } from "zod";

const eventTypes = [
  "WEDDING",
  "RECEPTION",
  "ENGAGEMENT",
  "HALDI",
  "MEHENDI",
  "CORPORATE",
  "CONFERENCE",
  "EXHIBITION",
  "CONCERT",
  "FILM_SHOOT",
  "PRIVATE_EVENT",
  "OTHER",
] as const;

const statuses = ["DRAFT", "PLANNING", "ACTIVE", "COMPLETED", "CANCELLED"] as const;

const optionalText = z
  .string()
  .trim()
  .optional()
  .transform((value) => (value ? value : undefined));

export const createProjectSchema = z.object({
  name: z.string().trim().min(1, "Give your project a name").max(200),
  eventType: z.enum(eventTypes),
  eventDate: z
    .string()
    .optional()
    .transform((value) => (value ? value : undefined)),
  guestCount: z
    .union([z.number(), z.string()])
    .optional()
    .transform((value) => {
      if (value === undefined || value === "") return undefined;
      const parsed = typeof value === "number" ? value : Number(value);
      return Number.isFinite(parsed) ? parsed : undefined;
    })
    .pipe(z.number().int().min(0, "Guest count cannot be negative").optional()),
  venueName: optionalText,
  venueAddress: optionalText,
  city: optionalText,
  state: optionalText,
  country: optionalText,
  description: optionalText,
  status: z.enum(statuses).optional(),
});

export type CreateProjectFormValues = z.input<typeof createProjectSchema>;
export type CreateProjectPayload = z.output<typeof createProjectSchema>;

export const noteSchema = z.object({
  title: z.string().trim().min(1, "Title is required").max(200),
  content: z.string().trim().min(1, "Write a note").max(20000),
});

export type NoteFormValues = z.infer<typeof noteSchema>;
