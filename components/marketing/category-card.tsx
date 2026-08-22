import Link from "next/link";
import { ArrowUpRight } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import type { Category } from "@/types";
import { cn, formatNumber } from "@/lib/utils";

interface CategoryCardProps {
  category: Category;
  href?: string;
  className?: string;
}

export function CategoryCard({ category, href, className }: CategoryCardProps) {
  const content = (
    <Card
      className={cn(
        "group relative h-full overflow-hidden transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md",
        className
      )}
    >
      <div className="from-muted/80 via-background to-background absolute inset-0 bg-gradient-to-br" />
      {category.imageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={category.imageUrl}
          alt=""
          className="absolute inset-0 size-full object-cover opacity-20 transition-opacity duration-300 group-hover:opacity-30"
        />
      ) : null}

      <CardContent className="relative flex min-h-[140px] flex-col justify-between p-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-base font-semibold tracking-tight">{category.name}</h3>
            {category.description ? (
              <p className="text-muted-foreground mt-1 line-clamp-2 text-sm">
                {category.description}
              </p>
            ) : null}
          </div>
          <ArrowUpRight className="text-muted-foreground size-4 shrink-0 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
        </div>
        <p className="text-muted-foreground text-xs font-medium">
          {formatNumber(category.productCount)} products
        </p>
      </CardContent>
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
