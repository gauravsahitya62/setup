# Pilot success metrics

Measure behavior, not opinions. Do not ask “Do you like the app?”

Gross Booking Value (**GBV**) is the sum of confirmed order `totalAmount` snapshots. **Never call GBV revenue.** FlowSet does not collect payments.

## Initial targets (first controlled cohort)

| Signal | Target |
| --- | --- |
| Planners onboarded | 10 |
| Vendors onboarded | 10 |
| Products listed (published) | 50+ |
| First real booking request | 1 |
| First real confirmed booking (vendor accept) | 1 |
| First completed event (order `COMPLETED`) | 1 |
| First verified review | 1 |

## Behavioral metrics

| Metric | How to read |
| --- | --- |
| Registered planners | `users` with role `PLANNER` |
| Active planners | Planners who created a project or sent a request in the period |
| Registered vendors | Vendor accounts |
| Verified vendors | `verification_status = VERIFIED` |
| Products listed | Published, not deleted |
| Availability checks | `availability_checked` event (when analytics is on) |
| Booking requests | Rows in booking requests |
| Booking acceptance rate | Confirmed / (confirmed + rejected), exclude cancelled pending |
| Confirmed orders | Orders not cancelled |
| Completed orders | Status `COMPLETED` |
| Gross Booking Value | Sum of confirmed order totals (INR). Not revenue. |
| Repeat planners | Planners with ≥2 booking requests |

## Qualitative

Use [`feedback-template.md`](./feedback-template.md). Track quotes about WhatsApp/call replacement, missing information, and weekly use — not star ratings of the UI.

## Success / fail for this cohort

**Success:** targets above plus at least one planner and one vendor who complete the real workflow without a FlowSet operator doing it for them.

**Fail:** no booking request after onboarding, or a confirmed booking that double-reserved inventory, or a security incident.
