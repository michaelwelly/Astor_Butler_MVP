"use client";

import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  item: PortfolioCase | null;
  onClose: () => void;
};

export function VideoPlayer({ item, onClose }: Props) {
  return (
    <AnimatePresence>
      {item && (
        <motion.div
          className="video-player"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <div className="video-player-header">
            <span className="video-player-title">{item.category} · {item.title}</span>
            <button type="button" className="video-player-close" onClick={onClose} aria-label="Закрыть">
              <X size={22} />
            </button>
          </div>
          <video
            className="video-player-video"
            src={item.video}
            poster={item.image}
            autoPlay
            controls
            playsInline
          />
        </motion.div>
      )}
    </AnimatePresence>
  );
}
