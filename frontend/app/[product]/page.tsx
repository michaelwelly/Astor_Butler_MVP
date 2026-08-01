import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ProductPage } from "@/components/product/ProductPage";
import { products, productBySlug } from "@/lib/products";

/**
 * A root-level dynamic segment so a proposal can link c3ag.ru/wedding rather
 * than c3ag.ru/#wedding — КП go out as links, and a fragment is not a page.
 * `dynamicParams = false` keeps it from swallowing every other single-segment
 * URL: only known product slugs resolve, everything else is a real 404.
 */
export const dynamicParams = false;

export function generateStaticParams() {
  return products.map((p) => ({ product: p.slug }));
}

type Params = { params: Promise<{ product: string }> };

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { product: slug } = await params;
  const product = productBySlug.get(slug);
  if (!product) return {};
  return {
    title: product.seoTitle,
    description: product.seoDescription,
    alternates: { canonical: `/${product.slug}` },
    openGraph: {
      type: "website",
      locale: "ru_RU",
      url: `/${product.slug}`,
      title: product.seoTitle,
      description: product.seoDescription,
    },
  };
}

export default async function Page({ params }: Params) {
  const { product: slug } = await params;
  const product = productBySlug.get(slug);
  if (!product) notFound();
  return <ProductPage product={product} />;
}
