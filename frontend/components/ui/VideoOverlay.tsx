"use client";

import { useEffect } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Play, X } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  item: PortfolioCase | null;
  onWatch: () => void;
  onClose: () => void;
};

export function VideoOverlay({ item, onWatch, onClose }: Props) {
  useEffect(() => {
    if (!item) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [item, onClose]);

  return (
    <AnimatePresence>
      {item && (
        <motion.div
          className="video-overlay-backdrop"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          role="dialog"
          aria-modal="true"
          aria-label={`${item.category}: ${item.title}`}
        >
          <motion.div
            className="video-overlay-card"
            initial={{ opacity: 0, y: 60 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 60 }}
            transition={{ duration: 0.35, ease: "easeOut" }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="video-overlay-poster">
              <img src={item.image} alt={item.title} />
            </div>
            <div className="video-overlay-info">
              <span className="video-overlay-category">{item.category}</span>
              <h2 className="video-overlay-title">{item.title}</h2>
              <p className="video-overlay-meta">{item.year} · {item.duration}</p>
              <p className="video-overlay-statement">{item.statement}</p>
              <div className="video-overlay-actions">
                <button className="btn-watch" type="button" onClick={onWatch}>
                  <Play size={16} /> Смотреть
                </button>
                <button className="btn-close-overlay" type="button" onClick={onClose}>
                  <X size={16} /> Закрыть
                </button>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
