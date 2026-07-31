"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import type { PortfolioCase } from "@/lib/portfolio";
import { catalogVideos, selectSources, POSTER_FALLBACK } from "@/lib/video-catalog";
import { isArchiveMaster } from "@/lib/media-ref";

type Props = {
  item: PortfolioCase;
  onClick: (item: PortfolioCase) => void;
  quiet?: boolean;
};

const STATUS_LABEL: Record<string, string> = {
  READY: "",
  DRAFT: "Скоро",
  ARCHIVED: "Архив",
};

/**
 * Cards play by themselves, muted, while they are on screen.
 *
 * Two things keep that from being a bandwidth and decoder disaster:
 *  - the source is always the *mobile* (720p) rendition, whatever the
 *    viewport. A card is never more than a few hundred px wide, and the render
 *    TZ puts the desktop cut at 15–40 MB — far too heavy to stream on scroll;
 *  - nothing loads or decodes until the card is actually in view, and it stops
 *    the moment it leaves.
 *
 * ponytail: the in-view gate is the only throttle — peak concurrency is
 * however many cards fit on screen, ~4-8. If that ever bites, add a global cap
 * on simultaneously playing cards.
 */
// Deliberately below half: a portrait card on a short landscape phone can be
// taller than the viewport, and at 0.5 it would never qualify and never play.
// Peak concurrency is set by how many cards fit on screen, not by this number.
const IN_VIEW = 0.25;

export function VideoCard({ item, onClick, quiet = false }: Props) {
  const meta = useMemo(
    () => catalogVideos.find((v) => v.slug === (item.slug ?? item.id)),
    [item],
  );

  const cardRef = useRef<HTMLButtonElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [autoplay, setAutoplay] = useState(false);
  const [playing, setPlaying] = useState(false);

  // Opt out where a self-starting video is wrong or expensive: a reduced-motion
  // request, or a client that asked us to save data.
  useEffect(() => {
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const conn = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection;
    setAutoplay(!reduce && !conn?.saveData);
  }, []);

  const previewSources = meta ? selectSources(meta, 640) : [];
  const poster = meta?.poster.publicUrl ?? item.image;

  // Cards autoplay only what is actually light enough to autoplay. Clips served
  // straight off the Yandex.Disk archive are camera masters — 173 MB median,
  // 1.3 GB at p90 — and up to eight cards are on screen at once. They show the
  // poster and stream on demand in the reels player instead. This lifts by
  // itself the day real 720p renditions land: the source stops being an
  // archive path and the preview switches back on.
  const tooHeavyToPreview = previewSources.some((s) => isArchiveMaster(s.publicUrl));
  const hasPreview = autoplay && previewSources.length > 0 && !tooHeavyToPreview;

  // Play while on screen, pause off it. Pausing keeps the current frame, so
  // scrolling back resumes the shot instead of cutting to black.
  useEffect(() => {
    const card = cardRef.current;
    if (!hasPreview || !card) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        const v = videoRef.current;
        if (!v) return;
        if (entry.isIntersecting) {
          void v
            .play()
            .then(() => setPlaying(true))
            .catch(() => setPlaying(false));
        } else {
          v.pause();
          setPlaying(false);
        }
      },
      { threshold: IN_VIEW },
    );
    observer.observe(card);
    return () => observer.disconnect();
  }, [hasPreview]);

  const shortDescription = meta?.shortDescription ?? item.kicker;
  const statusLabel = meta ? STATUS_LABEL[meta.status] : "";
  const orientation = meta?.orientation ?? "portrait";

  return (
    <button
      ref={cardRef}
      className="video-card"
      type="button"
      onClick={() => onClick(item)}
      data-cursor="play"
      data-orientation={orientation}
      data-playing={playing ? "" : undefined}
      data-quiet={quiet ? "" : undefined}
      aria-label={`${item.category}: ${item.title}`}
    >
      {/* Decorative: the button's aria-label already names the work. */}
      <img
        src={poster}
        alt=""
        className="video-card-img"
        loading="lazy"
        onError={(e) => {
          const img = e.currentTarget;
          if (img.src.endsWith(POSTER_FALLBACK)) return;
          img.src = POSTER_FALLBACK;
        }}
      />

      {hasPreview && (
        <video
          ref={videoRef}
          className="video-card-preview"
          muted
          loop
          playsInline
          preload="none"
          tabIndex={-1}
          aria-hidden="true"
        >
          {previewSources.map((s) => (
            <source key={`${s.variant}-${s.contentType}`} src={s.publicUrl} type={s.contentType} />
          ))}
        </video>
      )}

      {!quiet && statusLabel && (
        <span className="video-card-badges">
          <span className="video-badge video-badge--status">{statusLabel}</span>
        </span>
      )}

      {!quiet && item.duration && <span className="video-card-duration">{item.duration}</span>}

      {/* Over a moving picture, permanent text is noise. The title stays
          because it identifies the work; the rest waits for intent. */}
      {!quiet && (
        <div className="video-card-overlay">
          <strong className="video-card-title">{item.title}</strong>
          <span className="video-card-kicker">{shortDescription}</span>
        </div>
      )}
    </button>
  );
}
