import type { UserRole } from "@/types";

const SESSION_COOKIE = "flowset_session";
const ROLE_COOKIE = "flowset_role";

function isBrowser() {
  return typeof document !== "undefined";
}

export function setAuthHintCookies(role: UserRole) {
  if (!isBrowser() || role === "guest") return;
  const maxAge = 60 * 60 * 24 * 7;
  document.cookie = `${SESSION_COOKIE}=1; Path=/; Max-Age=${maxAge}; SameSite=Lax`;
  document.cookie = `${ROLE_COOKIE}=${role}; Path=/; Max-Age=${maxAge}; SameSite=Lax`;
}

export function clearAuthHintCookies() {
  if (!isBrowser()) return;
  document.cookie = `${SESSION_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
  document.cookie = `${ROLE_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
}

export { SESSION_COOKIE, ROLE_COOKIE };
