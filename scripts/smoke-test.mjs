#!/usr/bin/env node
/**
 * Lightweight staging smoke test. Does not seed data.
 *
 *   SMOKE_FRONTEND_URL=https://staging.example.com \
 *   SMOKE_API_URL=https://api-staging.example.com \
 *   node scripts/smoke-test.mjs
 *
 * Exit 1 if any check fails. Skip locally unless the stack is running.
 */
const frontend = (process.env.SMOKE_FRONTEND_URL || "http://localhost:3000").replace(/\/$/, "");
const api = (process.env.SMOKE_API_URL || "http://localhost:8080").replace(/\/$/, "");

async function check(name, url, init, assert) {
  const response = await fetch(url, init);
  const body = await response.text();
  try {
    assert(response, body);
    console.warn(`PASS  ${name}  ${response.status}  ${url}`);
  } catch (error) {
    console.error(`FAIL  ${name}  ${response.status}  ${url}`);
    console.error(body.slice(0, 500));
    throw error;
  }
}

async function main() {
  await check("frontend", frontend, { redirect: "manual" }, (res) => {
    if (res.status >= 400) throw new Error("Frontend not reachable");
  });
  await check("liveness", `${api}/actuator/health/liveness`, {}, (res) => {
    if (!res.ok) throw new Error("Liveness failed");
  });
  await check("readiness", `${api}/actuator/health/readiness`, {}, (res) => {
    if (!res.ok) throw new Error("Readiness failed");
  });
  await check(
    "login validation",
    `${api}/api/v1/auth/login`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "not-an-email", password: "x" }),
    },
    (res) => {
      if (res.status !== 400 && res.status !== 401 && res.status !== 422) {
        throw new Error(`Expected validation/auth error, got ${res.status}`);
      }
    }
  );
  await check("marketplace search", `${api}/api/v1/search?size=1`, {}, (res) => {
    if (!res.ok) throw new Error("Search failed");
  });
  await check(
    "project create unauthorized",
    `${api}/api/v1/projects`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "Smoke", eventType: "WEDDING" }),
    },
    (res) => {
      if (res.status !== 401 && res.status !== 403) {
        throw new Error(`Expected unauthorized project create, got ${res.status}`);
      }
    }
  );
  await check(
    "booking create unauthorized",
    `${api}/api/v1/booking-requests`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({}),
    },
    (res) => {
      if (res.status !== 401 && res.status !== 403) {
        throw new Error(`Expected unauthorized booking create, got ${res.status}`);
      }
    }
  );
  console.warn("Smoke checks passed.");
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
});
