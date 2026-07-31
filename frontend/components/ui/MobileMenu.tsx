import { AnimatePresence, motion } from "framer-motion";
import Link from "next/link";
import { X } from "lucide-react";
import { products } from "@/lib/products";

/**
 * On mobile this is the sitemap, not a decoration: seven products with their
 * price is the fastest route to the page someone actually wants. The section
 * anchors sit underneath, small, because they are the secondary need.
 */
const SECTIONS = [
  { label: "Работы", href: "/#catalog" },
  { label: "О студии", href: "/#about" },
  { label: "Контакт", href: "/#contact" },
  { label: "Приёмка", href: "/studio" },
];

type Props = {
  open: boolean;
  onClose: () => void;
};

export function MobileMenu({ open, onClose }: Props) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="menu-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <button type="button" onClick={onClose} aria-label="Закрыть меню">
            <X size={22} />
          </button>
          <nav className="menu-scroll" aria-label="Направления">
            <p className="menu-kicker">C3 Agency / Направления</p>
            <ul className="menu-products">
              {products.map((product) => (
                <li key={product.slug}>
                  <Link href={`/${product.slug}`} onClick={onClose}>
                    <span className="menu-num">{product.num}</span>
                    <span className="menu-name">{product.name}</span>
                    <span className="menu-price">{product.priceFrom ?? "смета"}</span>
                  </Link>
                </li>
              ))}
            </ul>
            <div className="menu-sections">
              {SECTIONS.map((item) => (
                <Link key={item.href} href={item.href} onClick={onClose}>
                  {item.label}
                </Link>
              ))}
            </div>
          </nav>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
