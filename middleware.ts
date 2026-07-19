import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const SESSION_COOKIE = "flowset_session";
const ROLE_COOKIE = "flowset_role";

function dashboardForRole(role: string | undefined): string {
  switch (role) {
    case "vendor":
      return "/vendor";
    case "admin":
      return "/admin";
    case "planner":
      return "/planner";
    default:
      return "/sign-in";
  }
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const session = request.cookies.get(SESSION_COOKIE)?.value;
  const role = request.cookies.get(ROLE_COOKIE)?.value;
  const isAuthed = session === "1" && Boolean(role);

  const isAuthPage =
    pathname.startsWith("/sign-in") ||
    pathname.startsWith("/sign-up") ||
    pathname.startsWith("/forgot-password") ||
    pathname.startsWith("/reset-password");

  if (isAuthPage && isAuthed) {
    return NextResponse.redirect(new URL(dashboardForRole(role), request.url));
  }

  if (pathname.startsWith("/planner")) {
    if (!isAuthed) {
      return NextResponse.redirect(new URL("/sign-in?next=/planner", request.url));
    }
    if (role !== "planner") {
      return NextResponse.redirect(new URL(dashboardForRole(role), request.url));
    }
  }

  if (pathname.startsWith("/vendor")) {
    if (!isAuthed) {
      return NextResponse.redirect(new URL("/sign-in?next=/vendor", request.url));
    }
    if (role !== "vendor") {
      return NextResponse.redirect(new URL(dashboardForRole(role), request.url));
    }
  }

  if (pathname.startsWith("/admin")) {
    if (!isAuthed) {
      return NextResponse.redirect(new URL("/sign-in?next=/admin", request.url));
    }
    if (role !== "admin") {
      return NextResponse.redirect(new URL(dashboardForRole(role), request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/planner/:path*",
    "/vendor/:path*",
    "/admin/:path*",
    "/sign-in",
    "/sign-up",
    "/forgot-password",
    "/reset-password",
  ],
};
