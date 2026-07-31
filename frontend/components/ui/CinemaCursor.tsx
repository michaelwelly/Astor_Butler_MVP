"use client";

import { useEffect, useRef, useState } from "react";
import { Play } from "lucide-react";

/**
 * Signature "reel" cursor: a lagging ring + a tight dot that lerp-follow the
 * mouse. Over anything marked `data-cursor="play"` the ring blooms into a
 * terracotta play-lens. Pure rAF (no deps) and DOM-transform driven, so it
 * never re-renders per frame. Disabled on coarse pointers and reduced-motion.
 */
export function CinemaCursor() {
  const dotRef = useRef<HTMLDivElement>(null);
  const ringRef = useRef<HTMLDivElement>(null);
  const [enabled, setEnabled] = useState(false);
  const [hot, setHot] = useState(false);

  useEffect(() => {
    const fine = window.matchMedia("(pointer: fine)").matches;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (!fine || reduce) return;

    setEnabled(true);
    document.body.classList.add("cursor-none");

    const target = { x: window.innerWidth / 2, y: window.innerHeight / 2 };
    const dot = { ...target };
    const ring = { ...target };
    let raf = 0;

    const onMove = (e: MouseEvent) => {
      target.x = e.clientX;
      target.y = e.clientY;
    };
    const onOver = (e: MouseEvent) => {
      const el = (e.target as HTMLElement | null)?.closest?.('[data-cursor="play"]');
      setHot(Boolean(el));
    };

    const loop = () => {
      dot.x += (target.x - dot.x) * 0.38;
      dot.y += (target.y - dot.y) * 0.38;
      ring.x += (target.x - ring.x) * 0.15;
      ring.y += (target.y - ring.y) * 0.15;
      if (dotRef.current)
        dotRef.current.style.transform = `translate(${dot.x}px, ${dot.y}px) translate(-50%, -50%)`;
      if (ringRef.current)
        ringRef.current.style.transform = `translate(${ring.x}px, ${ring.y}px) translate(-50%, -50%)`;
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);

    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseover", onOver);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseover", onOver);
      document.body.classList.remove("cursor-none");
    };
  }, []);

  if (!enabled) return null;

  return (
    <>
      <div ref={ringRef} className={`cine-cursor-ring${hot ? " is-hot" : ""}`} aria-hidden="true">
        <span className="cine-cursor-label">
          <Play size={22} strokeWidth={0} fill="currentColor" />
        </span>
      </div>
      <div ref={dotRef} className={`cine-cursor-dot${hot ? " is-hot" : ""}`} aria-hidden="true" />
    </>
  );
}
