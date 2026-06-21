import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";

const NAV_ITEMS = [
  { label: "Работы", href: "#work" },
  { label: "Направления", href: "#directions" },
  { label: "Услуги", href: "#services" },
  { label: "Написать нам", href: "#contact" },
];

type Props = {
  open: boolean;
  onClose: () => void;
};

export function MobileMenu({ open, onClose }: Props) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="menu-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <button type="button" onClick={onClose} aria-label="Close menu">
            <X size={22} />
          </button>
          <p>C3FLEX / Навигация</p>
          {NAV_ITEMS.map((item, index) => (
            <a key={item.label} href={item.href} onClick={onClose}>
              <span>0{index + 1}</span>
              {item.label}
            </a>
          ))}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
