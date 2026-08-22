# Vendor verification

Vendors register and can use their dashboard immediately. Public marketplace visibility is separate from verification.

## States

`PENDING` → `UNDER_REVIEW` → `VERIFIED`

Also: `REJECTED`, `SUSPENDED`

Admin actions (`POST /api/v1/admin/vendors/{id}/…`):

| Action | Result |
| --- | --- |
| Start review | `UNDER_REVIEW` |
| Verify | `VERIFIED`, `verified = true` |
| Reject | `REJECTED`, reason required |
| Suspend | `SUSPENDED`, storefront `SUSPENDED`, prior statuses saved |
| Reactivate | Restores prior verification and storefront status |

Every action writes an immutable audit row with admin identity, timestamp, and reason where required.

## Verified badge

Display the badge **only** when `verificationStatus = VERIFIED` (the `verified` column is derived on persist). Vendors cannot set `verified = true` through storefront APIs.

## Unverified vendors

They may:

- Open the vendor dashboard
- Complete profile, documents, portfolio
- Upload products

They may not appear as a verified vendor. Product public listing still requires `PUBLISHED`, vendor not `SUSPENDED`, and not deleted — enforced in `ProductService` / search specifications, not in the frontend.

## Documents

Types: GST, PAN, Business License, Identity, Other.

Admin can approve, reject, or request resubmission with a reason. Document URLs are omitted from public vendor DTOs. Cross-vendor document list returns **404**.
