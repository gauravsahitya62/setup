"use client";

import * as React from "react";
import { usePathname, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";

import { cn } from "@/lib/utils";

const START_EVENT = "flowset:nav-start";

/** Call before `router.push` / `router.replace` so redirects show the same loader as link clicks. */
export function startPageTransition() {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(START_EVENT));
}

function isModifiedClick(event: MouseEvent) {
  return event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0;
}

function shouldHandleAnchor(anchor: HTMLAnchorElement, event: MouseEvent) {
  if (isModifiedClick(event)) return false;
  if (anchor.target && anchor.target !== "_self") return false;
  if (anchor.hasAttribute("download")) return false;
  const href = anchor.getAttribute("href");
  if (!href || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) {
    return false;
  }
  try {
    const next = new URL(href, window.location.href);
    if (next.origin !== window.location.origin) return false;
    return (
      `${next.pathname}${next.search}` !== `${window.location.pathname}${window.location.search}`
    );
  } catch {
    return false;
  }
}

function NavigationProgressInner() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const search = searchParams.toString();
  const [active, setActive] = React.useState(false);
  const [showOverlay, setShowOverlay] = React.useState(false);
  const generation = React.useRef(0);
  const pending = React.useRef(false);

  const start = React.useCallback(() => {
    generation.current += 1;
    pending.current = true;
    setActive(true);
  }, []);

  const stop = React.useCallback((token: number) => {
    if (token !== generation.current) return;
    pending.current = false;
    setActive(false);
    setShowOverlay(false);
  }, []);

  React.useEffect(() => {
    const onClick = (event: MouseEvent) => {
      const anchor = (event.target as HTMLElement | null)?.closest("a");
      if (!anchor || !shouldHandleAnchor(anchor, event)) return;
      start();
    };
    window.addEventListener("click", onClick, true);
    window.addEventListener(START_EVENT, start);
    window.addEventListener("popstate", start);
    return () => {
      window.removeEventListener("click", onClick, true);
      window.removeEventListener(START_EVENT, start);
      window.removeEventListener("popstate", start);
    };
  }, [start]);

  React.useEffect(() => {
    if (!pending.current) return;
    const token = generation.current;
    const hideTimer = window.setTimeout(() => stop(token), 280);
    return () => window.clearTimeout(hideTimer);
  }, [pathname, search, stop]);

  React.useEffect(() => {
    if (!active) {
      setShowOverlay(false);
      return;
    }
    const overlayTimer = window.setTimeout(() => setShowOverlay(true), 180);
    const failsafe = window.setTimeout(() => stop(generation.current), 8000);
    return () => {
      window.clearTimeout(overlayTimer);
      window.clearTimeout(failsafe);
    };
  }, [active, stop]);

  return (
    <>
      <div
        role="progressbar"
        aria-hidden={!active}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={active ? "Loading page" : undefined}
        className={cn(
          "pointer-events-none fixed inset-x-0 top-0 z-[100] h-0.5 overflow-hidden transition-opacity duration-200",
          active ? "opacity-100" : "opacity-0"
        )}
      >
        <div
          className={cn(
            "bg-foreground h-full origin-left",
            active ? "animate-nav-progress w-full" : "w-0"
          )}
        />
      </div>

      {showOverlay ? (
        <div
          className="bg-background/60 fixed inset-0 z-[99] flex items-center justify-center backdrop-blur-[2px]"
          role="status"
          aria-live="polite"
          aria-busy="true"
        >
          <div className="border-border/60 bg-background shadow-elevated flex items-center gap-3 rounded-2xl border px-5 py-3">
            <Loader2 className="size-5 animate-spin" aria-hidden />
            <p className="text-sm font-medium">Loading…</p>
          </div>
        </div>
      ) : null}
    </>
  );
}

export function NavigationProgress() {
  return (
    <React.Suspense fallback={null}>
      <NavigationProgressInner />
    </React.Suspense>
  );
}
