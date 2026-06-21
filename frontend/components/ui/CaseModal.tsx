import { AnimatePresence, motion } from "framer-motion";
import { ArrowUpRight, X } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";

type Props = {
  item: PortfolioCase | null;
  onClose: () => void;
};

export function CaseModal({ item, onClose }: Props) {
  return (
    <AnimatePresence>
      {item && (
        <motion.div
          className="case-modal"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="case-modal-card"
            initial={{ y: 50 }}
            animate={{ y: 0 }}
            exit={{ y: 50 }}
          >
            <video src={item.video} poster={item.image} autoPlay loop muted playsInline />
            <button type="button" onClick={onClose} aria-label="Close case">
              <X size={20} />
            </button>
            <div>
              <p>{item.category} / {item.year}</p>
              <h2>{item.title}</h2>
              <span>{item.statement}</span>
              <a href="#contact" onClick={onClose}>
                Сделаем что-то вместе <ArrowUpRight size={15} />
              </a>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
