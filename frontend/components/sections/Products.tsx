import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { products, type Product } from "@/lib/products";
import { getForFolders } from "@/lib/portfolio";
import { RevealLines } from "@/components/ui/RevealLines";
import { ProductSlides, SLIDES_PER_CARD } from "@/components/sections/ProductSlides";

/**
 * Seven products in the commercial order. The first three are the money pages:
 * Reels, event reportage and advertising.
 */

/** Every card starts its slideshow at a different moment. */
const STAGGER_MS = 260;

function postersFor(product: Product): string[] {
  return getForFolders(product.clipFolders, SLIDES_PER_CARD)
    .map((c) => c.image)
    .filter(Boolean);
}

function ProductCard({ product, offset }: { product: Product; offset: number }) {
  const posters = postersFor(product);
  return (
    <Link className="product-card" href={`/${product.slug}`} data-slides={posters.length ? "" : undefined}>
      <ProductSlides posters={posters} offset={offset} />
      <span className="product-num">{product.num}</span>
      <h3>{product.name}</h3>
      <p className="product-audience">{product.audience}</p>
      <p className="product-tagline">{product.tagline}</p>
      <div className="product-foot">
        <strong>{product.priceFrom ?? "Смета под проект"}</strong>
        <ArrowUpRight size={17} />
      </div>
    </Link>
  );
}

export function Products() {
  // Offsets follow the site-wide order, so neighbours in either group differ.
  const offsetOf = (p: Product) => products.indexOf(p) * STAGGER_MS;

  return (
    <section className="products section-pad" id="products">
      <div className="section-heading compact">
        <p className="section-label">Семь направлений</p>
        <RevealLines lines={["Производство", <>без <i>лишних слов.</i></>]} />
      </div>

      <div className="product-group">
        <p className="product-group-label">Продукты</p>
        <div className="product-grid product-grid--ordered">
          {products.map((p) => (
            <ProductCard key={p.slug} product={p} offset={offsetOf(p)} />
          ))}
        </div>
      </div>
    </section>
  );
}
