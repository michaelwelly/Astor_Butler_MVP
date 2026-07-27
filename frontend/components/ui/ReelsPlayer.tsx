"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ChevronUp, Volume2, VolumeX, X } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";
import { getCatalogVideo, selectSources } from "@/lib/video-catalog";
import { isArchiveMaster, resolveArchiveSrc } from "@/lib/media-ref";
import {
  HINT_REELS_SOUND,
  HINT_REELS_SWIPE,
  learned,
  markLearned,
} from "@/lib/session-hint";

/**
 * Instagram-Reels-style vertical feed. Native CSS scroll-snap does the
 * swipe/scroll; an IntersectionObserver plays only the centred clip.
 *
 * Getting out is deliberately over-served, because a full-screen player that
 * traps you is the thing people complain about: Escape, the close button, a
 * pull-down from the top of the feed, and a click on the letterbox margin all
 * exit. Tapping the picture itself pauses instead — that is what the gesture
 * means everywhere else.
 *
 * Sound starts off (browsers block audible autoplay outright) and is one tap
 * away, which is also what the render TZ asks for.
 */

const PULL_TO_CLOSE = 110; // px dragged down from the top of the feed

/**
 * Both hints teach a gesture, so both disappear the moment that gesture
 * happens — a hint still on screen after you've obeyed it is just noise.
 */
const SOUND_HINT_IN = 1600;
const SOUND_HINT_OUT = 7000;

export function ReelsPlayer({
  items,
  startIndex,
  onClose,
  onIndexChange,
}: {
  items: PortfolioCase[];
  startIndex: number;
  onClose: () => void;
  /** Which clip is centred now — the chat sends it as page context. */
  onIndexChange?: (index: number) => void;
}) {
  const feedRef = useRef<HTMLDivElement>(null);
  const [muted, setMuted] = useState(true);
  const [index, setIndex] = useState(startIndex);
  const [swipeHint, setSwipeHint] = useState(false);
  const [soundHint, setSoundHint] = useState(false);
  // Public Yandex.Disk links are throttled to roughly 100–285 KB/s (measured),
  // so an archive master can sit there for a long time before the first frame.
  // Without this the player just looks broken.
  const [loading, setLoading] = useState(false);

  // The opening jump below fires a scroll event. Until this flips, a scroll
  // isn't the user's — otherwise the swipe hint would dismiss itself before
  // anyone saw it.
  const settled = useRef(false);

  // Jump to the tapped clip on open, before paint, so it never reads as a
  // scroll the user didn't ask for.
  useEffect(() => {
    const el = feedRef.current;
    const slide = el?.children[startIndex] as HTMLElement | undefined;
    if (el && slide) el.scrollTop = slide.offsetTop;
    const id = setTimeout(() => {
      settled.current = true;
    }, 400);
    return () => clearTimeout(id);
  }, [startIndex]);

  useEffect(() => {
    if (items.length > 1 && !learned(HINT_REELS_SWIPE)) setSwipeHint(true);
    if (learned(HINT_REELS_SOUND)) return;
    const show = setTimeout(() => setSoundHint(true), SOUND_HINT_IN);
    const hide = setTimeout(() => setSoundHint(false), SOUND_HINT_OUT);
    return () => {
      clearTimeout(show);
      clearTimeout(hide);
    };
  }, [items.length]);

  const dismissSwipeHint = () => {
    if (!settled.current || !swipeHint) return;
    setSwipeHint(false);
    markLearned(HINT_REELS_SWIPE);
  };

  const toggleSound = () => {
    setMuted((m) => !m);
    setSoundHint(false);
    markLearned(HINT_REELS_SOUND);
  };

  /**
   * Start a clip, resolving the archive link first if it needs one.
   *
   * Archive clips carry their /api/yadisk URL in `data-archive` rather than
   * `src`, because pointing a <video> at the route makes it hang on the
   * cross-origin redirect. The real signed link is fetched once, on the way to
   * the first play, and then lives on the element.
   */
  const startClip = useCallback(async (v: HTMLVideoElement) => {
    const archive = v.dataset.archive;
    if (archive && !v.src) {
      setLoading(true);
      const direct = await resolveArchiveSrc(archive);
      // The user may have swiped past while the lookup was in flight.
      if (!direct || !v.isConnected) return setLoading(false);
      v.src = direct;
    }
    await v.play().catch(() => {});
  }, []);

  // Play the centred clip, pause the rest, and remember which one it is.
  useEffect(() => {
    const el = feedRef.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          const v = e.target as HTMLVideoElement;
          if (e.isIntersecting && e.intersectionRatio > 0.6) {
            void startClip(v);
            const i = Number((v.closest(".reels-slide") as HTMLElement)?.dataset.index);
            if (!Number.isNaN(i)) {
              setIndex(i);
              onIndexChange?.(i);
            }
          } else {
            v.pause();
          }
        });
      },
      { root: el, threshold: [0, 0.6, 1] },
    );
    el.querySelectorAll("video").forEach((v) => io.observe(v));
    return () => io.disconnect();
  }, [items, onIndexChange, startClip]);

  // `muted` is a property the DOM owns once a <video> is live, so set it
  // directly on every clip — that way the toggle also applies to the next one
  // you swipe to, not just the one on screen.
  useEffect(() => {
    feedRef.current?.querySelectorAll("video").forEach((v) => {
      v.muted = muted;
    });
  }, [muted]);

  // The page behind must not scroll while we sit over it. This is deliberately
  // its own effect with no dependencies: `onClose` is an inline arrow in the
  // parent, so a combined effect would tear down and re-establish the lock on
  // every parent render and could capture "hidden" as the value to restore —
  // leaving the page permanently unscrollable after the player closes.
  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  // Pull down from the top of the feed to dismiss.
  const drag = useRef({ y: 0, armed: false });
  const onTouchStart = (e: React.TouchEvent) => {
    drag.current = { y: e.touches[0].clientY, armed: (feedRef.current?.scrollTop ?? 1) <= 0 };
  };
  const onTouchMove = (e: React.TouchEvent) => {
    if (!drag.current.armed) return;
    if (e.touches[0].clientY - drag.current.y > PULL_TO_CLOSE) {
      drag.current.armed = false;
      onClose();
    }
  };

  const togglePlay = useCallback((v: HTMLVideoElement) => {
    if (v.paused) void v.play().catch(() => {});
    else v.pause();
  }, []);

  return (
    <div className="reels" role="dialog" aria-modal="true" aria-label="Просмотр роликов">
      <div className="reels-actions">
        <button
          type="button"
          onClick={toggleSound}
          data-hinted={soundHint && muted ? "" : undefined}
          aria-label={muted ? "Включить звук" : "Выключить звук"}
        >
          {muted ? <VolumeX size={20} /> : <Volume2 size={20} />}
        </button>
        <button type="button" onClick={onClose} aria-label="Закрыть">
          <X size={22} />
        </button>
      </div>

      {items.length > 1 && (
        <p className="reels-count" aria-hidden="true">
          {index + 1} / {items.length}
        </p>
      )}

      {/* Points at the sound button above it. Goes away on the first tap of
          that button, or on its own if it was never the thing you wanted. */}
      {soundHint && muted && (
        <p className="reels-hint reels-hint--sound" aria-hidden="true">
          Включить звук
        </p>
      )}

      {swipeHint && !loading && (
        <p className="reels-hint reels-hint--swipe" aria-hidden="true">
          <ChevronUp size={16} />
          Листайте
        </p>
      )}

      {loading && (
        <p className="reels-loading" role="status">
          <span className="reels-spinner" aria-hidden="true" />
          Загружаем из архива
        </p>
      )}

      <div
        className="reels-feed"
        ref={feedRef}
        onScroll={dismissSwipeHint}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
      >
        {items.map((c, i) => {
          const catalog = getCatalogVideo(c.slug ?? c.id);
          const sources = catalog ? selectSources(catalog, 480) : [];
          const poster = catalog?.poster.publicUrl ?? c.image;
          // Archive clips can't be declared as <source> — see startClip.
          const archive = sources.find((s) => isArchiveMaster(s.publicUrl))?.publicUrl;
          return (
            <div
              className="reels-slide"
              key={c.id}
              data-index={i}
              // Only the margin around the picture closes; the picture itself
              // belongs to the pause gesture.
              onClick={(e) => {
                if (e.target === e.currentTarget) onClose();
              }}
            >
              <video
                poster={poster}
                muted
                loop
                playsInline
                // The neighbours are one swipe away, so it is worth their
                // metadata; the rest of the feed stays off the wire.
                preload={Math.abs(i - startIndex) <= 1 ? "metadata" : "none"}
                data-archive={archive}
                onClick={(e) => togglePlay(e.currentTarget)}
                onWaiting={() => setLoading(true)}
                onPlaying={() => setLoading(false)}
                onLoadedData={() => setLoading(false)}
              >
                {!archive &&
                  sources.map((s) => (
                    <source
                      key={`${s.variant}-${s.contentType}`}
                      src={s.publicUrl}
                      type={s.contentType}
                    />
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
