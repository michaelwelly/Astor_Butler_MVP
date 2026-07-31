"use client";

import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { getByDirection, getForFolders, type DirectionId, type PortfolioCase } from "@/lib/portfolio";
import { products, type Product } from "@/lib/products";
import { type VideoOrientation } from "@/lib/video-catalog";
import { VideoCard } from "@/components/ui/VideoCard";

type Props = {
  onSelect: (item: PortfolioCase) => void;
};

// Home shows up to two swipe-pages of 4 per category; the rest go to archive.
const PAGE_SIZE = 4;
const HOME_PREVIEW = PAGE_SIZE * 2;

function CategoryCarousel({
  items,
  orientation,
  onSelect,
  quiet = false,
}: {
  items: PortfolioCase[];
  orientation: VideoOrientation;
  onSelect: (item: PortfolioCase) => void;
  quiet?: boolean;
}) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [atStart, setAtStart] = useState(true);
  const [atEnd, setAtEnd] = useState(false);

  // An arrow that scrolls nothing is a dead control — grey them out at the
  // ends instead of letting the user poke at them.
  useEffect(() => {
    const el = trackRef.current;
    if (!el) return;
    const sync = () => {
      const max = el.scrollWidth - el.clientWidth;
      setAtStart(el.scrollLeft <= 1);
      setAtEnd(el.scrollLeft >= max - 1);
    };
    sync();
    el.addEventListener("scroll", sync, { passive: true });
    const ro = new ResizeObserver(sync);
    ro.observe(el);
    return () => {
      el.removeEventListener("scroll", sync);
      ro.disconnect();
    };
  }, [items.length]);

  const scrollByPage = (dir: 1 | -1) => {
    const el = trackRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth, behavior: "smooth" });
  };

  return (
    <div className="cat-carousel" data-orientation={orientation}>
      <button
        type="button"
        className="cat-arrow cat-arrow--prev"
        onClick={() => scrollByPage(-1)}
        disabled={atStart}
        aria-label="Предыдущие"
      >
        <ChevronLeft size={20} />
      </button>
      <div className="cat-track" ref={trackRef}>
        {items.map((item) => (
          <div className="cat-track-item" key={item.id}>
            <VideoCard item={item} onClick={onSelect} quiet={quiet} />
          </div>
        ))}
      </div>
      <button
        type="button"
        className="cat-arrow cat-arrow--next"
        onClick={() => scrollByPage(1)}
        disabled={atEnd}
        aria-label="Следующие"
      >
        <ChevronRight size={20} />
      </button>
    </div>
  );
}

function productFallbackDirection(product: Product): DirectionId {
  if (product.slug === "reels") return "reels";
  if (product.slug === "reclama" || product.slug === "ai") return "commercials";
  return "events";
}

function productOrientation(product: Product): VideoOrientation {
  if (product.slug === "reels" || product.slug === "podcast") return "portrait";
  return "landscape";
}

function getProductFeed(product: Product, offset = 0, limit = HOME_PREVIEW): PortfolioCase[] {
  const byFolders = getForFolders(product.clipFolders);
  const source = byFolders.length ? byFolders : getByDirection(productFallbackDirection(product));
  return source.slice(offset, offset + limit);
}

export function FeaturedCatalog({ onSelect }: Props) {
  const [archiveOpen, setArchiveOpen] = useState(false);

  useEffect(() => {
    if (!archiveOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setArchiveOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onKey);
    };
  }, [archiveOpen]);

  const archiveByProduct = products
    .map((product) => ({ product, items: getProductFeed(product, HOME_PREVIEW) }))
    .filter((g) => g.items.length > 0);

  const archiveCount = archiveByProduct.reduce((acc, g) => acc + g.items.length, 0);

  return (
    <section className="featured-catalog" id="catalog">
      <div className="featured-catalog-intro">
        <p className="section-label">Новостная лента</p>
        <h2>Семь продуктовых линеек</h2>
      </div>

      {products.map((product, i) => {
        const preview = getProductFeed(product);
        return (
          <motion.div
            key={product.slug}
            id={`row-${product.slug}`}
            className="featured-category"
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.12 }}
            transition={{ duration: 0.6, delay: i * 0.08 }}
          >
            <div className="category-header">
              <span className="category-num">{product.num}</span>
              <div>
                <h2 className="category-title">{product.name}</h2>
                <p className="category-desc">{product.tagline}</p>
              </div>
            </div>
            <CategoryCarousel
              items={preview}
              orientation={productOrientation(product)}
              onSelect={onSelect}
              quiet
            />
          </motion.div>
        );
      })}

      {archiveCount > 0 && (
        <div className="archive-row">
          <button className="archive-btn" type="button" onClick={() => setArchiveOpen(true)}>
            <span>Архив работ →</span>
            <small>ещё {archiveCount} в архиве</small>
          </button>
        </div>
      )}

      <AnimatePresence>
        {archiveOpen && (
          <motion.div
            className="archive-modal-backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setArchiveOpen(false)}
          >
            <motion.div
              className="archive-modal"
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 40 }}
              transition={{ duration: 0.3, ease: "easeOut" }}
              onClick={(e) => e.stopPropagation()}
              data-lenis-prevent
              role="dialog"
              aria-modal="true"
              aria-label="Архив работ"
            >
              <div className="archive-modal-header">
                <h3>Архив работ</h3>
                <button type="button" onClick={() => setArchiveOpen(false)} aria-label="Закрыть">
                  <X size={20} />
                </button>
              </div>
              <div className="archive-modal-body">
                {archiveByProduct.map((g) => (
                  <div key={g.product.slug} className="archive-group">
                    <p className="archive-group-title">{g.product.name}</p>
                    <div className="archive-grid">
                      {g.items.map((item) => (
                        <VideoCard
                          key={item.id}
                          item={item}
                          onClick={(it) => {
                            setArchiveOpen(false);
                            onSelect(it);
                          }}
                        />
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  );
}
