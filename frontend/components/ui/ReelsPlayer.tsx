"use client";

import { useEffect, useRef, useState } from "react";
import { Volume2, VolumeX, X } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";
import { getCatalogVideo, selectSources } from "@/lib/video-catalog";

/**
 * Instagram-Reels-style vertical feed. Native CSS scroll-snap does the
 * swipe/scroll; an IntersectionObserver plays only the centered clip (muted
 * autoplay, sound on tap). Escape or the close button exits.
 */
export function ReelsPlayer({
  items,
  startIndex,
  onClose,
}: {
  items: PortfolioCase[];
  startIndex: number;
  onClose: () => void;
}) {
  const feedRef = useRef<HTMLDivElement>(null);
  const [muted, setMuted] = useState(true);

  // Jump to the tapped clip on open.
  useEffect(() => {
    const el = feedRef.current;
    const slide = el?.children[startIndex] as HTMLElement | undefined;
    if (el && slide) el.scrollTop = slide.offsetTop;
  }, [startIndex]);

  // Play the in-view clip, pause the rest.
  useEffect(() => {
    const el = feedRef.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          const v = e.target as HTMLVideoElement;
          if (e.isIntersecting && e.intersectionRatio > 0.6) {
            v.muted = muted;
            void v.play().catch(() => {});
          } else {
            v.pause();
          }
        });
      },
      { root: el, threshold: [0, 0.6, 1] },
    );
    el.querySelectorAll("video").forEach((v) => io.observe(v));
    return () => io.disconnect();
  }, [muted, items]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="reels" role="dialog" aria-modal="true" aria-label="Просмотр роликов">
      <div className="reels-actions">
        <button type="button" onClick={() => setMuted((m) => !m)} aria-label={muted ? "Включить звук" : "Выключить звук"}>
          {muted ? <VolumeX size={20} /> : <Volume2 size={20} />}
        </button>
        <button type="button" onClick={onClose} aria-label="Закрыть">
          <X size={22} />
        </button>
      </div>
      <div className="reels-feed" ref={feedRef}>
        {items.map((c) => {
          const catalog = getCatalogVideo(c.slug ?? c.id);
          const sources = catalog ? selectSources(catalog, 480) : [];
          const poster = catalog?.poster.publicUrl ?? c.image;
          return (
            <div className="reels-slide" key={c.id}>
              <video poster={poster} muted={muted} loop playsInline preload="metadata">
                {sources.map((s) => (
                  <source key={`${s.variant}-${s.contentType}`} src={s.publicUrl} type={s.contentType} />
                ))}
              </video>
              <div className="reels-overlay" />
              <div className="reels-info">
                <span className="reels-cat">{c.category}</span>
                <strong>{c.title}</strong>
                <span className="reels-kicker">{c.kicker}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
