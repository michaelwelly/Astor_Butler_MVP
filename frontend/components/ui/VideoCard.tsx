"use client";

import { useMemo } from "react";
import { motion } from "framer-motion";
import type { PortfolioCase } from "@/lib/portfolio";
import { catalogVideos, POSTER_FALLBACK } from "@/lib/video-catalog";

type Props = {
  item: PortfolioCase;
  onClick: (item: PortfolioCase) => void;
};

const STATUS_LABEL: Record<string, string> = {
  READY: "",
  DRAFT: "Скоро",
  ARCHIVED: "Архив",
};

export function VideoCard({ item, onClick }: Props) {
  const meta = useMemo(
    () => catalogVideos.find((v) => v.slug === (item.slug ?? item.id)),
    [item],
  );

  const tags = meta?.tags.slice(0, 3) ?? [];
  const shortDescription = meta?.shortDescription ?? item.kicker;
  const statusLabel = meta ? STATUS_LABEL[meta.status] : "";

  return (
    <motion.button
      className="video-card"
      type="button"
      onClick={() => onClick(item)}
      whileHover={{ scale: 1.06, zIndex: 10 }}
      transition={{ duration: 0.2 }}
      aria-label={`${item.category}: ${item.title}`}
    >
      <img
        src={meta?.poster.publicUrl ?? item.image}
        alt={item.title}
        className="video-card-img"
        loading="lazy"
        onError={(e) => {
          const img = e.currentTarget;
          if (img.src.endsWith(POSTER_FALLBACK)) return;
          img.src = POSTER_FALLBACK;
        }}
      />

      <div className="video-card-badges">
        {item.featured && <span className="video-badge video-badge--featured">Избранное</span>}
        {statusLabel && <span className="video-badge video-badge--status">{statusLabel}</span>}
      </div>

      <span className="video-card-duration">{item.duration}</span>

      <div className="video-card-overlay">
        <span className="video-card-category">{item.category}</span>
        <strong className="video-card-title">{item.title}</strong>
        <span className="video-card-kicker">{shortDescription}</span>
        {tags.length > 0 && (
          <ul className="video-card-tags">
            {tags.map((tag) => (
              <li key={tag} className="video-card-tag">
                {tag}
              </li>
            ))}
          </ul>
        )}
      </div>
    </motion.button>
  );
}
