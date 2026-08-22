import Link from "next/link";
import { BadgeCheck, MapPin } from "lucide-react";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Rating } from "@/components/shared/rating";
import type { Vendor } from "@/types";
import { cn } from "@/lib/utils";

interface VendorCardProps {
  vendor: Vendor;
  href?: string;
  className?: string;
}

export function VendorCard({ vendor, href, className }: VendorCardProps) {
  const initials = vendor.name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const content = (
    <Card
      className={cn(
        "group h-full transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md",
        className
      )}
    >
      <div className="from-muted via-muted/60 to-background relative aspect-[16/9] overflow-hidden bg-gradient-to-br">
        {vendor.coverImageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={vendor.coverImageUrl}
            alt={vendor.name}
            className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
          />
        ) : null}
        <div className="absolute -bottom-6 left-4">
          <Avatar className="ring-background size-12 shadow-md ring-2">
            <AvatarImage src={vendor.logoUrl} alt={vendor.name} />
            <AvatarFallback>{initials}</AvatarFallback>
          </Avatar>
        </div>
      </div>

      <CardHeader className="pt-8">
        <div className="flex items-start justify-between gap-2">
          <CardTitle className="line-clamp-1 flex items-center gap-1.5 text-base">
            {vendor.name}
            {vendor.verificationStatus === "verified" ? (
              <BadgeCheck className="size-4 text-sky-500" aria-label="Verified vendor" />
            ) : null}
          </CardTitle>
        </div>
        <p className="text-muted-foreground line-clamp-2 text-sm">{vendor.description}</p>
      </CardHeader>

      <CardContent className="space-y-3">
        <div className="text-muted-foreground flex items-center gap-1.5 text-sm">
          <MapPin className="size-3.5" />
          <span className="line-clamp-1">{vendor.location}</span>
        </div>
        <Rating value={vendor.rating} reviewCount={vendor.reviewCount} />
      </CardContent>

      <CardFooter className="flex-wrap gap-1.5">
        {vendor.categories.slice(0, 3).map((category) => (
          <Badge key={category} variant="secondary">
            {category}
          </Badge>
        ))}
      </CardFooter>
    </Card>
  );

  if (href) {
    return (
      <Link href={href} className="block focus-visible:outline-none">
        {content}
      </Link>
    );
  }

  return content;
}
