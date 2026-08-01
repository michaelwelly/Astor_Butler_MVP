"use client";

import { useEffect, useState } from "react";

/**
 * Slideshow behind a product card: a still from each of that product's own
 * works, swapped every couple of seconds.
 *
 * Stills, not video, and that is not a shortcut — every clip in the archive is a
 * camera master (2160×3840 and up, 173 MB median). Seven cards decoding 4K would
 * cost more than a gigabyte of RAM, and at ~2 s per clip the masters would never
 * buffer in time to show anything but black: measured off the archive, they take
 * 12 s to reach 13 s of buffer. A poster is 74 KB and appears instantly.
 *
 * This lifts by itself once light renditions exist — swap the <img> stack for a
 * <video> and drop SLIDE_MS to the clip length.
 */

/** Time each still holds before the crossfade to the next one. */
const SLIDE_MS = 2200;
/** Product cards stay visually stable unless a future rollout explicitly enables ambient rotation. */
const AUTO_ROTATE = process.env.NEXT_PUBLIC_PRODUCT_CARD_SLIDES_AUTOPLAY === "true";
/** Stills kept per card. Enough to feel like a reel, few enough to stay light. */
export const SLIDES_PER_CARD = 4;

type Props = {
  posters: string[];
  /**
   * Milliseconds this card waits before its first swap. Seven cards flipping in
   * unison reads as a glitch; offset they read as a room full of screens.
   */
  offset: number;
};

export function ProductSlides({ posters, offset }: Props) {
  const [index, setIndex] = useState(0);
  const [live, setLive] = useState(false);

  // Resolved after mount so server and first client render agree, and so a
  // reduced-motion visitor keeps a single still instead of a carousel.
  useEffect(() => {
    setLive(!window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  }, []);

  useEffect(() => {
    if (!AUTO_ROTATE || !live || posters.length < 2) return;
    let timer: ReturnType<typeof setInterval>;
    const start = setTimeout(() => {
      timer = setInterval(() => setIndex((i) => (i + 1) % posters.length), SLIDE_MS);
    }, offset);
    return () => {
      clearTimeout(start);
      clearInterval(timer);
    };
  }, [live, posters.length, offset]);

  if (!posters.length) return null;

  return (
    <span className="product-slides" aria-hidden="true">
      {posters.map((src, i) => (
        // Decorative: the card's own text names the product.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          key={src}
          src={src}
          alt=""
          loading="lazy"
          decoding="async"
          data-on={i === index ? "" : undefined}
        />
      ))}
    </span>
  );
}
