"use client";

import { motion } from "framer-motion";
import { VideoCard } from "@/components/ui/VideoCard";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  id?: string;
  title: string;
  items: PortfolioCase[];
  onSelect: (item: PortfolioCase) => void;
};

export function CategoryRow({ id, title, items, onSelect }: Props) {
  return (
    <motion.section
      className="category-row"
      id={id}
      initial={{ opacity: 0 }}
      whileInView={{ opacity: 1 }}
      viewport={{ once: true, amount: 0.1 }}
      transition={{ duration: 0.5 }}
    >
      <h2 className="category-row-title">{title}</h2>
      <div className="row-scroll">
        {items.map((item) => (
          <VideoCard key={item.id} item={item} onClick={onSelect} />
        ))}
      </div>
    </motion.section>
  );
}
