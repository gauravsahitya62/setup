import { ROUTES } from "@/constants";
import type { UserRole } from "@/types";

export function accountNavForRole(role: UserRole) {
  switch (role) {
    case "planner":
      return {
        dashboardHref: ROUTES.planner.root,
        dashboardLabel: "Dashboard",
        settingsHref: ROUTES.planner.settings,
        extraHref: ROUTES.planner.projects,
        extraLabel: "My projects",
      };
    case "vendor":
      return {
        dashboardHref: ROUTES.vendor.root,
        dashboardLabel: "Dashboard",
        settingsHref: ROUTES.vendor.settings,
        extraHref: ROUTES.vendor.catalog,
        extraLabel: "Catalog",
      };
    case "admin":
      return {
        dashboardHref: ROUTES.admin.root,
        dashboardLabel: "Admin",
        settingsHref: ROUTES.admin.settings,
        extraHref: ROUTES.admin.users,
        extraLabel: "Users",
      };
    default:
      return {
        dashboardHref: ROUTES.home,
        dashboardLabel: "Home",
        settingsHref: ROUTES.home,
        extraHref: null,
        extraLabel: null,
      };
  }
}
