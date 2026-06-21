"use client";

import { motion } from "framer-motion";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  item: PortfolioCase;
  onClick: (item: PortfolioCase) => void;
};

export function VideoCard({ item, onClick }: Props) {
  return (
    <motion.button
      className="video-card"
      type="button"
      onClick={() => onClick(item)}
      whileHover={{ scale: 1.06, zIndex: 10 }}
      transition={{ duration: 0.2 }}
    >
      <img src={item.image} alt={item.title} className="video-card-img" />
      <span className="video-card-duration">{item.duration}</span>
      <div className="video-card-overlay">
        <span className="video-card-category">{item.category}</span>
        <strong className="video-card-title">{item.title}</strong>
        <span className="video-card-kicker">{item.kicker}</span>
      </div>
    </motion.button>
  );
}
