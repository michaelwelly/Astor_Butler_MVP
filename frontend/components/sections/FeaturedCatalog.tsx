"use client";

import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { directions, getByDirection, type PortfolioCase } from "@/lib/portfolio";
import { VideoCard } from "@/components/ui/VideoCard";

type Props = {
  onSelect: (item: PortfolioCase) => void;
};

// Home shows up to two swipe-pages of 4 per category; the rest go to archive.
const PAGE_SIZE = 4;
const HOME_PREVIEW = PAGE_SIZE * 2;

function CategoryCarousel({
  items,
  onSelect,
}: {
  items: PortfolioCase[];
  onSelect: (item: PortfolioCase) => void;
}) {
  const trackRef = useRef<HTMLDivElement>(null);

  const scrollByPage = (dir: 1 | -1) => {
    const el = trackRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth, behavior: "smooth" });
  };

  return (
    <div className="cat-carousel">
      <button
        type="button"
        className="cat-arrow cat-arrow--prev"
        onClick={() => scrollByPage(-1)}
        aria-label="Предыдущие"
      >
        <ChevronLeft size={20} />
      </button>
      <div className="cat-track" ref={trackRef}>
        {items.map((item) => (
          <div className="cat-track-item" key={item.id}>
            <VideoCard item={item} onClick={onSelect} />
          </div>
        ))}
      </div>
      <button
        type="button"
        className="cat-arrow cat-arrow--next"
        onClick={() => scrollByPage(1)}
        aria-label="Следующие"
      >
        <ChevronRight size={20} />
      </button>
    </div>
  );
}

export function FeaturedCatalog({ onSelect }: Props) {
  const [archiveOpen, setArchiveOpen] = useState(false);

  useEffect(() => {
    if (!archiveOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setArchiveOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [archiveOpen]);

  const archiveByDirection = directions
    .map((dir) => ({ dir, items: getByDirection(dir.id).slice(HOME_PREVIEW) }))
    .filter((g) => g.items.length > 0);

  const archiveCount = archiveByDirection.reduce((acc, g) => acc + g.items.length, 0);

  return (
    <section className="featured-catalog" id="catalog">
      {directions.map((dir, i) => {
        const preview = getByDirection(dir.id, HOME_PREVIEW);
        return (
          <motion.div
            key={dir.id}
            id={`row-${dir.id}`}
            className="featured-category"
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.12 }}
            transition={{ duration: 0.6, delay: i * 0.08 }}
          >
            <div className="category-header">
              <span className="category-num">{dir.index}</span>
              <div>
                <h2 className="category-title">{dir.title}</h2>
                <p className="category-desc">{dir.description}</p>
              </div>
            </div>
            <CategoryCarousel items={preview} onSelect={onSelect} />
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
                {archiveByDirection.map((g) => (
                  <div key={g.dir.id} className="archive-group">
                    <p className="archive-group-title">{g.dir.title}</p>
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
