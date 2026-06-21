"use client";

import { motion } from "framer-motion";

type Props = {
  onComplete: () => void;
};

export function IntroLogo({ onComplete }: Props) {
  return (
    <motion.div
      className="intro-screen"
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.7, ease: "easeInOut" }}
    >
      <div className="intro-grain" />

      {/* White flash on punch-in */}
      <motion.div
        className="intro-flash"
        initial={{ opacity: 0 }}
        animate={{ opacity: [0, 0.85, 0] }}
        transition={{ duration: 0.5, times: [0, 0.25, 1], delay: 0.05 }}
      />

      <motion.div className="intro-logo-wrap">
        <motion.div
          className="intro-glow"
          initial={{ opacity: 0, scale: 0.4 }}
          animate={{ opacity: [0, 1, 0.2], scale: [0.4, 1.4, 1.0] }}
          transition={{ duration: 1.4, times: [0, 0.3, 1], ease: [0.16, 1, 0.3, 1] }}
        />
        <motion.img
          src="/c3flex-logo.png"
          alt="C3FLEX"
          className="intro-logo"
          initial={{ opacity: 0, scale: 0.55 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.45, delay: 0.05, ease: [0.16, 1, 0.3, 1] }}
          onAnimationComplete={() => setTimeout(onComplete, 1100)}
        />
      </motion.div>
    </motion.div>
  );
}
