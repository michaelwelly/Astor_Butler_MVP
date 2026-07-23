"use client";

import { motion, type Variants } from "framer-motion";
import type { ReactNode } from "react";

// Scroll-into-view fade+rise. once=true so it settles and stays. MotionConfig
// at the app root disables it for reduced-motion visitors.
const variants: Variants = {
  hidden: { opacity: 0, y: 48 },
  show: { opacity: 1, y: 0, transition: { duration: 0.8, ease: [0.22, 1, 0.36, 1] } },
};

export function Reveal({ children }: { children: ReactNode }) {
  return (
    <motion.div
      variants={variants}
      initial="hidden"
      whileInView="show"
      viewport={{ once: true, amount: 0.15 }}
    >
      {children}
    </motion.div>
  );
}
