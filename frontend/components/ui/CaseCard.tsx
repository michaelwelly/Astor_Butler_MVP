import { motion } from "framer-motion";
import { Plus } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  item: PortfolioCase;
  index: number;
  onClick: (item: PortfolioCase) => void;
};

export function CaseCard({ item, index, onClick }: Props) {
  return (
    <motion.button
      className={`case-card case-card-${index + 1}`}
      type="button"
      onClick={() => onClick(item)}
      initial={{ opacity: 0, y: 36 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.18 }}
      transition={{ duration: 0.7, delay: index * 0.09 }}
    >
      <img src={item.image} alt="" />
      <span className="case-shade" />
      <span className="case-top">
        <em>{item.category}</em>
        <em>{item.year}</em>
      </span>
      <span className="case-bottom">
        <strong>{item.title}</strong>
        <span>{item.kicker}</span>
      </span>
      <span className="case-open">
        <Plus size={18} />
      </span>
    </motion.button>
  );
}
