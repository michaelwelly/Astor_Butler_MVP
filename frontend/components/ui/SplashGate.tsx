"use client";

import { useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";

type Props = { onComplete: () => void };
type Phase = "idle" | "animating";

function synthesizeTadum() {
  try {
    const ctx = new AudioContext();
    const now = ctx.currentTime;

    // "TA" – low thud
    const o1 = ctx.createOscillator();
    const g1 = ctx.createGain();
    o1.type = "sine";
    o1.frequency.setValueAtTime(105, now);
    o1.frequency.exponentialRampToValueAtTime(48, now + 0.38);
    g1.gain.setValueAtTime(0.9, now);
    g1.gain.exponentialRampToValueAtTime(0.001, now + 0.44);
    o1.connect(g1); g1.connect(ctx.destination);
    o1.start(now); o1.stop(now + 0.48);

    // "DUM" – melodic punch
    const t2 = now + 0.29;
    const o2 = ctx.createOscillator();
    const g2 = ctx.createGain();
    o2.type = "sine";
    o2.frequency.setValueAtTime(215, t2);
    o2.frequency.exponentialRampToValueAtTime(185, t2 + 1.0);
    g2.gain.setValueAtTime(0, t2);
    g2.gain.linearRampToValueAtTime(1.0, t2 + 0.03);
    g2.gain.exponentialRampToValueAtTime(0.001, t2 + 1.15);
    o2.connect(g2); g2.connect(ctx.destination);
    o2.start(t2); o2.stop(t2 + 1.2);

    // Harmonic overtone for depth
    const o3 = ctx.createOscillator();
    const g3 = ctx.createGain();
    o3.type = "sine";
    o3.frequency.setValueAtTime(430, t2);
    o3.frequency.exponentialRampToValueAtTime(370, t2 + 0.9);
    g3.gain.setValueAtTime(0, t2);
    g3.gain.linearRampToValueAtTime(0.38, t2 + 0.03);
    g3.gain.exponentialRampToValueAtTime(0.001, t2 + 0.7);
    o3.connect(g3); g3.connect(ctx.destination);
    o3.start(t2); o3.stop(t2 + 0.75);
  } catch {}
}

export function SplashGate({ onComplete }: Props) {
  const [phase, setPhase] = useState<Phase>("idle");

  const handleEnter = useCallback(() => {
    if (phase !== "idle") return;
    synthesizeTadum();
    setPhase("animating");
  }, [phase]);

  return (
    <motion.div
      className="splash-gate"
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.65, ease: "easeInOut" }}
      onClick={handleEnter}
    >
      <div className="intro-grain" />

      <AnimatePresence mode="wait">
        {phase === "idle" ? (
          <motion.div
            key="idle"
            className="splash-idle"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0, scale: 1.05 }}
            transition={{ duration: 0.5 }}
          >
            <img src="/c3flex-logo.png" className="splash-logo-still" alt="C3FLEX" />
            <motion.p
              className="splash-hint"
              animate={{ opacity: [0.3, 0.8, 0.3] }}
              transition={{ repeat: Infinity, duration: 2.4, ease: "easeInOut" }}
            >
              нажми чтобы войти
            </motion.p>
          </motion.div>
        ) : (
          <motion.div key="anim" className="splash-anim">
            <motion.div
              className="intro-flash"
              initial={{ opacity: 0 }}
              animate={{ opacity: [0, 0.92, 0] }}
              transition={{ duration: 0.45, times: [0, 0.18, 1] }}
            />
            <div className="intro-logo-wrap">
              <motion.div
                className="intro-glow"
                initial={{ opacity: 0, scale: 0.35 }}
                animate={{ opacity: [0, 1, 0.15], scale: [0.35, 1.6, 1] }}
                transition={{ duration: 1.3, times: [0, 0.28, 1], ease: [0.16, 1, 0.3, 1] }}
              />
              <motion.img
                src="/c3flex-logo.png"
                alt="C3FLEX"
                className="intro-logo"
                initial={{ opacity: 0, scale: 0.48 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.38, ease: [0.16, 1, 0.3, 1] }}
                onAnimationComplete={() => setTimeout(onComplete, 1050)}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
