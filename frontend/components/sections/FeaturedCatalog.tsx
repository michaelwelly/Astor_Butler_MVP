"use client";

import { motion } from "framer-motion";
import { directions, getByDirection } from "@/lib/portfolio";
import { VideoCard } from "@/components/ui/VideoCard";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  onSelect: (item: PortfolioCase) => void;
};

export function FeaturedCatalog({ onSelect }: Props) {
  return (
    <section className="featured-catalog" id="catalog">
      {directions.map((dir, i) => {
        const top3 = getByDirection(dir.id, 3);
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
            <div className="top3-grid">
              {top3.map((item) => (
                <VideoCard key={item.id} item={item} onClick={onSelect} />
              ))}
            </div>
          </motion.div>
        );
      })}

      <div className="archive-row">
        <button className="archive-btn" disabled type="button">
          <span>Архив работ →</span>
          <small>доступно после подключения сервера</small>
        </button>
      </div>
    </section>
  );
}
