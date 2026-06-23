"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Maximize, Minimize, X } from "lucide-react";
import type { PortfolioCase } from "@/lib/portfolio";
import { catalogVideos, selectSource } from "@/lib/video-catalog";

type Props = {
  item: PortfolioCase | null;
  onClose: () => void;
};

/**
 * Adaptive, mobile-first player.
 *  - reserves aspect-ratio by orientation so the stage never jumps on load;
 *  - portrait media is letterboxed in a 9:16 stage, landscape in 16:9;
 *  - chooses the best source for the current viewport;
 *  - exposes a fullscreen toggle and uses playsInline for iOS.
 */
export function VideoPlayer({ item, onClose }: Props) {
  const stageRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [viewportWidth, setViewportWidth] = useState(1280);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // The catalog entry carries orientation + viewport-aware sources.
  const catalog = useMemo(
    () => (item ? catalogVideos.find((v) => v.slug === (item.slug ?? item.id)) : null),
    [item],
  );

  useEffect(() => {
    const onResize = () => setViewportWidth(window.innerWidth);
    onResize();
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => {
    const onFsChange = () => setIsFullscreen(Boolean(document.fullscreenElement));
    document.addEventListener("fullscreenchange", onFsChange);
    return () => document.removeEventListener("fullscreenchange", onFsChange);
  }, []);

  // Close on Escape (when not in native fullscreen, which handles its own Esc).
  useEffect(() => {
    if (!item) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !document.fullscreenElement) onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [item, onClose]);

  const orientation = catalog?.orientation ?? "landscape";
  const source = catalog ? selectSource(catalog, viewportWidth) : null;
  const videoSrc = source?.publicUrl ?? item?.video;
  const poster = catalog?.poster.publicUrl ?? item?.image;

  const toggleFullscreen = async () => {
    const el = stageRef.current;
    if (!el) return;
    try {
      if (document.fullscreenElement) {
        await document.exitFullscreen();
      } else {
        await el.requestFullscreen();
      }
    } catch {
      /* fullscreen unsupported — ignore */
    }
  };

  return (
    <AnimatePresence>
      {item && (
        <motion.div
          className="video-player"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          role="dialog"
          aria-modal="true"
          aria-label={`${item.category}: ${item.title}`}
        >
          <div className="video-player-header">
            <span className="video-player-title">
              {item.category} · {item.title}
            </span>
            <div className="video-player-actions">
              <button
                type="button"
                className="video-player-icon"
                onClick={toggleFullscreen}
                aria-label={isFullscreen ? "Выйти из полноэкранного режима" : "Полный экран"}
              >
                {isFullscreen ? <Minimize size={18} /> : <Maximize size={18} />}
              </button>
              <button
                type="button"
                className="video-player-close"
                onClick={onClose}
                aria-label="Закрыть"
              >
                <X size={22} />
              </button>
            </div>
          </div>

          <div className="video-player-body">
            <div
              ref={stageRef}
              className={`video-stage video-stage--${orientation}`}
              data-orientation={orientation}
            >
              {videoSrc ? (
                <video
                  ref={videoRef}
                  className="video-stage-media"
                  src={videoSrc}
                  poster={poster}
                  autoPlay
                  controls
                  playsInline
                />
              ) : (
                <div
                  className="video-stage-empty"
                  style={poster ? { backgroundImage: `url(${poster})` } : undefined}
                >
                  <p>Видео скоро появится</p>
                  <span>Файл готовится к публикации</span>
                </div>
              )}
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
