import Link from "next/link";
import { products } from "@/lib/products";
import { Wordmark } from "@/components/ui/Wordmark";

export function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="site-footer">
      <div className="footer-top">
        <div className="footer-brand">
          <Link className="brand" href="/">
            <Wordmark />
          </Link>
          <p>Независимая продакшн-студия / {year}</p>
        </div>
        {/* Product links in the footer are the crawlable sitemap — every
            page links to every other one, which is what a КП sent as a link
            needs. */}
        <nav className="footer-products" aria-label="Направления">
          {products.map((product) => (
            <Link key={product.slug} href={`/${product.slug}`}>
              {product.name}
            </Link>
          ))}
        </nav>
      </div>
      <div className="footer-bottom">
        <Link href="/#catalog">Работы</Link>
        <Link href="/#contact">Контакт</Link>
        <Link href="/studio">Приёмка</Link>
      </div>
    </footer>
  );
}
