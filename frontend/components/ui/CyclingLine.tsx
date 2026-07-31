"use client";

import { useEffect, useState } from "react";
import { useReducedMotion } from "framer-motion";

/**
 * One headline line that cycles through a set of endings forever: the outgoing
 * phrase rolls up out of a mask while the incoming one rolls in behind it.
 *
 * Deliberately *not* framer-motion. The hero copy above this is a motion.div
 * driven by variants with staggerChildren, and variant context propagates to
 * every nested motion component — nested motion spans end up waiting on an
 * orchestration that finished at page load, so they mount at their `initial`
 * state and never animate or exit. Plain CSS transitions have no such
 * coupling, and they cost one class instead of a presence tracker.
 *
 * Every phrase stays mounted, so the widest one holds the box open and the
 * swap never reflows the layout around it — the phrases are their own sizers.
 */

type Props = {
  items: string[];
  /** ms a phrase stays put before the next one rolls in. */
  interval?: number;
};

export function CyclingLine({ items, interval = 3400 }: Props) {
  const reduce = useReducedMotion();
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (reduce || items.length < 2) return;
    const id = setInterval(
      () => setIndex((n) => (n + 1) % items.length),
      interval,
    );
    return () => clearInterval(id);
  }, [reduce, items.length, interval]);

  if (reduce) return <span className="cycle-static">{items[0]}</span>;

  const leaving = (index - 1 + items.length) % items.length;

  return (
    <span className="cycle">
      {items.map((text, i) => (
        <span
          key={text}
          className="cycle-word"
          // in = at rest, out = rolling up and away, idle = parked below,
          // waiting its turn. Only in/out carry a transition.
          data-state={i === index ? "in" : i === leaving ? "out" : "idle"}
          // Only the phrase actually on screen is exposed; the others are
          // off-frame duplicates that exist to hold the box open.
          aria-hidden={i === index ? undefined : "true"}
        >
          {text}
        </span>
      ))}
    </span>
  );
}
