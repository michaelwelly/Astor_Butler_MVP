"use client";

import { motion, type Variants } from "framer-motion";
import type { ReactNode } from "react";

// Title-sequence heading: each line rises from behind a clipping mask when it
// scrolls into view. Reuses the hero's signature on every section heading.
// MotionConfig (app root) disables it for reduced-motion visitors.
const container: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.12, delayChildren: 0.04 } },
};
const line: Variants = {
  hidden: { y: "118%" },
  show: { y: 0, transition: { duration: 0.9, ease: [0.22, 1, 0.36, 1] } },
};

export function RevealLines({ lines, className }: { lines: ReactNode[]; className?: string }) {
  return (
    <motion.h2
      className={className}
      variants={container}
      initial="hidden"
      whileInView="show"
      viewport={{ once: true, amount: 0.4 }}
    >
      {lines.map((content, i) => (
        <span className="reveal-line-mask" key={i}>
          <motion.span className="reveal-line" variants={line}>
            {content}
          </motion.span>
        </span>
      ))}
    </motion.h2>
  );
}
