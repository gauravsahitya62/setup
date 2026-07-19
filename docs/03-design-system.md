# FlowSet Design System

## Principles

- Premium B2B SaaS (Stripe / Linear / Vercel tone)
- Soft elevation: `rounded-xl` / `rounded-2xl`, subtle rings, generous spacing
- Avoid purple glow, Material density, and decorative card spam on auth screens

## Typography

Plus Jakarta Sans (app-wide). Expressive but restrained on auth forms.

## Color

CSS variables via shadcn theme tokens (`--background`, `--foreground`, `--primary`, etc.). Support light/dark through `next-themes`.

## Components

Prefer existing `components/ui/*` and `components/shared/*`. Do not invent parallel Button/Input/Card primitives.

## Auth UI constraints

- Do not redesign landing or dashboards when wiring auth
- Auth pages should feel calm: one column form, clear validation, loading states
- Role redirect happens after successful login / session restore
