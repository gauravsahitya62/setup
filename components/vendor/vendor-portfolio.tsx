"use client";

import * as React from "react";
import Image from "next/image";

import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { VendorProfile } from "@/types";

interface VendorPortfolioProps {
  portfolio: VendorProfile["portfolio"];
}

export function VendorPortfolio({ portfolio }: VendorPortfolioProps) {
  const [active, setActive] = React.useState<(typeof portfolio)[number] | null>(null);

  if (portfolio.length === 0) return null;

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Portfolio</CardTitle>
          <p className="text-muted-foreground text-sm">
            Selected installs and event environments from recent productions.
          </p>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
            {portfolio.map((item, index) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setActive(item)}
                className="group focus-visible:ring-ring relative aspect-[4/3] overflow-hidden rounded-2xl focus-visible:ring-2 focus-visible:outline-none"
              >
                <Image
                  src={item.imageUrl}
                  alt={item.caption ?? `Portfolio image ${index + 1}`}
                  fill
                  sizes="(max-width: 768px) 50vw, 33vw"
                  className="object-cover transition-transform duration-500 group-hover:scale-[1.04]"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-transparent to-transparent opacity-0 transition-opacity group-hover:opacity-100" />
                {item.caption ? (
                  <span className="absolute inset-x-0 bottom-0 p-3 text-left text-xs font-medium text-white opacity-0 transition-opacity group-hover:opacity-100">
                    {item.caption}
                  </span>
                ) : null}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      <Dialog open={Boolean(active)} onOpenChange={(open) => !open && setActive(null)}>
        <DialogContent className="max-w-4xl overflow-hidden p-0 sm:max-w-4xl">
          <DialogTitle className="sr-only">{active?.caption ?? "Portfolio image"}</DialogTitle>
          {active ? (
            <div className="bg-muted relative aspect-[16/10] w-full">
              <Image
                src={active.imageUrl}
                alt={active.caption ?? "Portfolio image"}
                fill
                sizes="100vw"
                className="object-cover"
              />
            </div>
          ) : null}
          {active?.caption ? (
            <p className="text-muted-foreground px-6 py-4 text-sm">{active.caption}</p>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}
