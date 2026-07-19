"use client";

import * as React from "react";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";

import { AuthProvider } from "./auth-provider";
import { QueryProvider } from "./query-provider";
import { ThemeProvider } from "./theme-provider";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryProvider>
        <AuthProvider>
          <TooltipProvider delayDuration={200}>
            {children}
            <Toaster richColors position="top-right" closeButton />
          </TooltipProvider>
        </AuthProvider>
      </QueryProvider>
    </ThemeProvider>
  );
}
