# Pilot onboarding

Controlled access only. Do not point a public DNS name at staging without authentication and rate limits.

Invite users with a staging URL, unique accounts, and a written rule: **no production customer PII in staging** unless the staging database is treated as confidential.

Demo catalog (disposable databases only):

```bash
DEMO_SEED_ENABLED=true DEMO_SEED_PASSWORD=DemoPassw0rd! mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Never set `DEMO_SEED_ENABLED` in production.

## Planner checklist

The planner should be able to:

- [ ] Register  
- [ ] Complete profile  
- [ ] Create an event project  
- [ ] Browse products  
- [ ] Search  
- [ ] Filter  
- [ ] Check availability  
- [ ] Add products to the project  
- [ ] Save vendors  
- [ ] Send a booking request  
- [ ] Track the request  
- [ ] Track the order  
- [ ] See delivery updates  
- [ ] Leave a review after completion  

Example project for training: Sharma Wedding, Wedding, 300 guests, 12 December 2026, Example Venue.

## Vendor checklist

The vendor should be able to:

- [ ] Register  
- [ ] Complete profile  
- [ ] Upload portfolio  
- [ ] Create products  
- [ ] Add variants  
- [ ] Manage inventory  
- [ ] Set availability / maintenance blocks  
- [ ] Receive a request  
- [ ] Accept or reject  
- [ ] View orders  
- [ ] Schedule delivery  
- [ ] Mark delivered  
- [ ] Schedule pickup  
- [ ] Complete the order  
- [ ] View reviews  

Admin must **verify** the vendor before treating the public profile as trusted. Unverified vendors stay `PENDING`.

## Admin checklist

The admin should be able to:

- [ ] Verify vendors  
- [ ] Moderate products  
- [ ] Moderate reviews  
- [ ] Manage users  
- [ ] View orders  
- [ ] View booking requests  
- [ ] Handle support tickets  
- [ ] View audit logs  

Sensitive admin actions must appear in audit logs.

## Access control for the cohort

- Staging CORS allowlist = the staging frontend origin only.  
- Cookies: `Secure` on HTTPS.  
- Rate limits on.  
- No `DEMO_SEED` on the database that will hold real pilot data unless you intentionally wipe it after training.  
- Operator accounts: one admin, plus support mailbox watched daily.
