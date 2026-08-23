import type { Metadata } from "next";

import { RootShell } from "@/components/layout";
import { ProductDetailLoader } from "@/components/marketplace";
import { createPageMetadata } from "@/lib/metadata";
import { fetchProductBySlug } from "@/lib/marketplace-server";

interface ProductPageProps {
  params: Promise<{ slug: string }>;
}

export const dynamic = "force-dynamic";

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  const { slug } = await params;
  const product = await fetchProductBySlug(slug);

  if (!product) {
    return createPageMetadata({
      title: "Product not found",
      path: `/marketplace/${slug}`,
    });
  }

  return createPageMetadata({
    title: product.name,
    description: product.shortDescription ?? product.description ?? undefined,
    path: `/marketplace/${slug}`,
    image: product.featuredImage ?? undefined,
  });
}

export default async function MarketplaceProductPage({ params }: ProductPageProps) {
  const { slug } = await params;

  return (
    <RootShell>
      <ProductDetailLoader slug={slug} />
    </RootShell>
  );
}
