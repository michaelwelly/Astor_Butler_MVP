import type { DirectionId } from "./portfolio";

/**
 * Catalog manifest — the ONE place to manage the videos in the work grid at the
 * bottom of the page (cards + player + archive). Sibling of hero-clips.ts.
 *
 * Add an entry and it appears in its direction's row automatically. `src` and
 * `poster` take the same three forms as the hero manifest:
 *   "clip.mp4"              → file you dropped in  frontend/public/portfolio/
 *   "/anything/clip.mp4"    → any local /public path, used as-is
 *   "https://…/clip.mp4"    → an absolute URL (Yandex.Disk direct link, CDN, …)
 *
 * Only `title`, `direction`, `src` are required; the rest have sensible defaults
 * (see buildCases in portfolio.ts). Leave the list empty and the grid falls back
 * to the 30 sample cases, so the page never breaks while you fill it in. Keep
 * heavy originals OUT of git — light web renditions in /public, or absolute URLs.
 */
export type CatalogClip = {
  title: string;
  direction: DirectionId; // "events" | "reels" | "commercials"
  src: string; // video
  poster?: string; // card image / player still
  duration?: string; // "01:40" — shown on the card
  kicker?: string; // short line under the title
  statement?: string; // longer line in the player overlay
  tags?: string[];
  year?: string;
  featured?: boolean;
  orientation?: "portrait" | "landscape"; // else derived from direction
  accent?: string; // card accent hex
  /**
   * Archive folder the clip came from ("/VIDEO C3AG/7. Подкасты/…"). Set
   * automatically for records out of the video DB; it is how a product page
   * finds its own work, since the three site directions are far coarser than
   * the seven products. Hand-written entries can leave it out.
   */
  folder?: string;
};

export const CATALOG_CLIPS: CatalogClip[] = [
  // { title: "Segreto", direction: "events", src: "segreto_hero.mp4", poster: "segreto.jpg",
  //   duration: "01:40", kicker: "Ресторан в кадре", featured: true },
];
