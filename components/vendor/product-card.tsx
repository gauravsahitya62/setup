import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import type { Product } from "@/types";
import { cn, formatCurrency } from "@/lib/utils";

interface ProductCardProps {
  product: Product;
  href?: string;
  className?: string;
}

export function ProductCard({ product, href, className }: ProductCardProps) {
  const content = (
    <Card
      className={cn(
        "group h-full transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md",
        className
      )}
    >
      <div className="from-muted to-muted/30 relative aspect-square overflow-hidden bg-gradient-to-br">
        {product.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={product.imageUrl}
            alt={product.name}
            className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
          />
        ) : null}
        <Badge
          variant={product.available ? "secondary" : "outline"}
          className="absolute top-3 right-3 shadow-sm"
        >
          {product.available ? "Available" : "Unavailable"}
        </Badge>
      </div>

      <CardHeader>
        <CardTitle className="line-clamp-1 text-base">{product.name}</CardTitle>
        <p className="text-muted-foreground line-clamp-2 text-sm">{product.description}</p>
      </CardHeader>

      <CardContent />

      <CardFooter className="justify-between">
        <div>
          <p className="text-base font-semibold tracking-tight">
            {formatCurrency(product.dailyRate, product.currency)}
          </p>
          <p className="text-muted-foreground text-xs">per day</p>
        </div>
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
