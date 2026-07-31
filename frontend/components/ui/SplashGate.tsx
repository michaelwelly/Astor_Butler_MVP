"use client";

import { useState, useCallback, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Wordmark } from "@/components/ui/Wordmark";
import { BRAND_LOGO_URL, BRAND_NAME } from "@/lib/brand";

type Props = { onComplete: () => void };
type Phase = "idle" | "animating";

const EASE = [0.4, 0, 0.2, 1] as const;

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

  // The gate is released by the logo's onAnimationComplete. If that callback
  // never arrives — a throttled tab, a background window, anything that stalls
  // the animation loop — the visitor is stranded on a black screen with no way
  // in. This is the floor: once the click happened, the site opens.
  useEffect(() => {
    if (phase !== "animating") return;
    const id = setTimeout(onComplete, 2200);
    return () => clearTimeout(id);
  }, [phase, onComplete]);

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
            exit={{ opacity: 0 }}
            transition={{ duration: 0.45, ease: EASE }}
          >
            {/* The mark breathes on luminance only — see .splash-logo-still. */}
            <img src={BRAND_LOGO_URL} className="splash-logo-still" alt="" />
            <Wordmark className="splash-wordmark" />
            <motion.p
              className="splash-hint"
              animate={{ opacity: [0.35, 0.85, 0.35] }}
              transition={{ repeat: Infinity, duration: 3.2, ease: EASE }}
            >
              нажмите, чтобы войти
            </motion.p>
          </motion.div>
        ) : (
          // Entry punch, restyled: no white flash, no radial glow. The mark
          // fades up and the wordmark wipes open from its own centre.
          <motion.div key="anim" className="splash-anim">
            <div className="intro-logo-wrap">
              <motion.img
                src={BRAND_LOGO_URL}
                alt={BRAND_NAME}
                className="intro-logo"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.5, ease: EASE }}
                onAnimationComplete={() => setTimeout(onComplete, 900)}
              />
              <motion.span
                className="intro-wipe"
                initial={{ scaleX: 0 }}
                animate={{ scaleX: 1 }}
                transition={{ duration: 0.7, delay: 0.18, ease: EASE }}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
