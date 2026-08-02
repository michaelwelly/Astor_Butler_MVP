"use client";

import { useState, useCallback, useEffect, type PointerEvent } from "react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { BRAND_LOGO_URL, BRAND_NAME } from "@/lib/brand";

type Props = { onComplete: () => void };
type Phase = "idle" | "animating";

const EASE = [0.4, 0, 0.2, 1] as const;

function synthesizeHandpanEntry() {
  try {
    const AudioCtx = window.AudioContext ?? (window as typeof window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return;

    const ctx = new AudioCtx();
    const now = ctx.currentTime;
    const master = ctx.createGain();
    const compressor = ctx.createDynamicsCompressor();
    master.gain.setValueAtTime(0.62, now);
    master.connect(compressor);
    compressor.connect(ctx.destination);

    const notes = [
      { time: 0.00, frequency: 146.83, pan: -0.35, gain: 0.58 },
      { time: 0.17, frequency: 220.00, pan: 0.32, gain: 0.46 },
      { time: 0.34, frequency: 174.61, pan: 0.18, gain: 0.42 },
      { time: 0.51, frequency: 261.63, pan: -0.18, gain: 0.38 },
    ];

    const strike = (frequency: number, time: number, pan: number, gain: number) => {
      const body = ctx.createOscillator();
      const bodyGain = ctx.createGain();
      const overtone = ctx.createOscillator();
      const overtoneGain = ctx.createGain();
      const shimmer = ctx.createOscillator();
      const shimmerGain = ctx.createGain();
      const filter = ctx.createBiquadFilter();
      const panner = ctx.createStereoPanner();

      body.type = "sine";
      overtone.type = "triangle";
      shimmer.type = "sine";

      body.frequency.setValueAtTime(frequency, time);
      overtone.frequency.setValueAtTime(frequency * 2.01, time);
      shimmer.frequency.setValueAtTime(frequency * 3.02, time);

      filter.type = "lowpass";
      filter.frequency.setValueAtTime(2400, time);
      filter.frequency.exponentialRampToValueAtTime(860, time + 0.72);
      filter.Q.setValueAtTime(7.5, time);
      panner.pan.setValueAtTime(pan, time);

      bodyGain.gain.setValueAtTime(0.0001, time);
      bodyGain.gain.exponentialRampToValueAtTime(gain, time + 0.012);
      bodyGain.gain.exponentialRampToValueAtTime(0.0001, time + 0.82);

      overtoneGain.gain.setValueAtTime(0.0001, time);
      overtoneGain.gain.exponentialRampToValueAtTime(gain * 0.22, time + 0.009);
      overtoneGain.gain.exponentialRampToValueAtTime(0.0001, time + 0.46);

      shimmerGain.gain.setValueAtTime(0.0001, time);
      shimmerGain.gain.exponentialRampToValueAtTime(gain * 0.1, time + 0.006);
      shimmerGain.gain.exponentialRampToValueAtTime(0.0001, time + 0.24);

      body.connect(bodyGain);
      overtone.connect(overtoneGain);
      shimmer.connect(shimmerGain);
      bodyGain.connect(filter);
      overtoneGain.connect(filter);
      shimmerGain.connect(filter);
      filter.connect(panner);
      panner.connect(master);

      body.start(time);
      overtone.start(time);
      shimmer.start(time);
      body.stop(time + 0.86);
      overtone.stop(time + 0.5);
      shimmer.stop(time + 0.28);
    };

    notes.forEach(({ time, frequency, pan, gain }) => strike(frequency, now + time, pan, gain));
    window.setTimeout(() => void ctx.close(), 1800);
  } catch {}
}

export function SplashGate({ onComplete }: Props) {
  const [phase, setPhase] = useState<Phase>("idle");
  const reduceMotion = useReducedMotion();

  const handleEnter = useCallback(() => {
    if (phase !== "idle") return;
    if (!reduceMotion) synthesizeHandpanEntry();
    setPhase("animating");
  }, [phase, reduceMotion]);

  const handlePointerMove = useCallback((event: PointerEvent<HTMLDivElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect();
    const x = ((event.clientX - bounds.left) / bounds.width) * 100;
    const y = ((event.clientY - bounds.top) / bounds.height) * 100;
    event.currentTarget.style.setProperty("--pointer-x", `${x.toFixed(2)}%`);
    event.currentTarget.style.setProperty("--pointer-y", `${y.toFixed(2)}%`);
  }, []);

  // The gate is released by the logo's onAnimationComplete. If that callback
  // never arrives — a throttled tab, a background window, anything that stalls
  // the animation loop — the visitor is stranded on a black screen with no way
  // in. This is the floor: once the click happened, the site opens.
  useEffect(() => {
    if (phase !== "animating") return;
    const id = setTimeout(onComplete, reduceMotion ? 480 : 1500);
    return () => clearTimeout(id);
  }, [phase, reduceMotion, onComplete]);

  return (
    <motion.div
      className="splash-gate"
      data-phase={phase}
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.65, ease: "easeInOut" }}
      onClick={handleEnter}
      onPointerMove={handlePointerMove}
      role="dialog"
      aria-modal="true"
      aria-label="Приветствие C3 AG"
    >
      <div className="splash-bg splash-bg--content" aria-hidden="true" />
      <div className="splash-bg splash-bg--sand" aria-hidden="true" />
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
            <motion.button
              type="button"
              className="splash-mark-button"
              aria-label="Войти на сайт C3 AG"
              onClick={(event) => {
                event.stopPropagation();
                handleEnter();
              }}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: reduceMotion ? 0.01 : 0.45, delay: reduceMotion ? 0 : 0.25, ease: EASE }}
            >
              <img src={BRAND_LOGO_URL} className="splash-logo-still" alt="" />
            </motion.button>
          </motion.div>
        ) : (
          // Entry punch, restyled: no white flash, no radial glow.
          <motion.div key="anim" className="splash-anim">
            <div className="intro-logo-wrap">
              <motion.img
                src={BRAND_LOGO_URL}
                alt={BRAND_NAME}
                className="intro-logo"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.5, ease: EASE }}
                onAnimationComplete={() => setTimeout(onComplete, reduceMotion ? 0 : 520)}
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
