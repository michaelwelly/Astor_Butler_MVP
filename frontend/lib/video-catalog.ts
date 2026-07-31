/**
 * Contract-aligned video catalog.
 *
 * Field names and shapes follow docs/contracts/FRONTEND_BACKEND_CONTRACTS.md §3
 * (Video Catalog Contract). This is the MVP local-mock layer: when the
 * backend ships `GET /api/content/c3flex/videos`, the response can replace
 * `catalogVideos` 1:1 without touching UI components.
 *
 * Rules honoured here:
 *  - media binaries never live in git; only metadata + placeholder/env URLs;
 *  - UI must not assume exactly 30 items (30 is the current content target);
 *  - a missing poster falls back to a design-system placeholder;
 *  - the player picks the best source by viewport / orientation.
 */

import { portfolioCases, type PortfolioCase, type DirectionId } from "./portfolio";

export type VideoStatus = "READY" | "DRAFT" | "ARCHIVED";
export type VideoOrientation = "portrait" | "landscape";

export type PosterAsset = {
  assetId: string | null;
  publicUrl: string;
  contentType: string;
  width: number;
  height: number;
};

export type VideoSource = {
  variant: "mobile" | "desktop";
  publicUrl: string;
  contentType: string;
  width: number;
  height: number;
  bitrateKbps: number;
};

export type VideoCta = {
  label: string;
  intent: "PROJECT_REQUEST";
};

export type CatalogVideo = {
  videoId: string;
  slug: string;
  title: string;
  description: string;
  shortDescription: string;
  tags: string[];
  category: string;
  direction: DirectionId;
  featured: boolean;
  durationSeconds: number;
  orientation: VideoOrientation;
  status: VideoStatus;
  poster: PosterAsset;
  sources: VideoSource[];
  assets: {
    originalUrl?: string;
    adaptedUrl?: string;
    previewUrl?: string;
  };
  cta: VideoCta;
  // UI convenience mirrors (not part of the wire contract):
  accent: string;
  year: string;
  durationLabel: string;
};

/**
 * Object-storage base. Backend will eventually return absolute publicUrl /
 * signedUrl values; until then we env-drive a placeholder base so no real
 * S3/MinIO path is hardcoded. See FRONTEND_BACKEND_CONTRACTS.md §2.
 */
const MEDIA_BASE_URL = (
  process.env.NEXT_PUBLIC_MEDIA_BASE_URL ?? "https://media.placeholder.c3flex.local"
).replace(/\/$/, "");

export const POSTER_FALLBACK = "/portfolio/_poster-fallback.svg";

// Per-direction defaults used when an item has no explicit override.
export const DIRECTION_ORIENTATION: Record<DirectionId, VideoOrientation> = {
  events: "landscape",
  reels: "portrait",
  commercials: "landscape",
};

const DIRECTION_TAGS: Record<DirectionId, string[]> = {
  events: ["event", "atmosphere"],
  reels: ["reels", "product"],
  commercials: ["commercial", "brand"],
};

const ORIENTATION_DIMS: Record<VideoOrientation, { w: number; h: number }> = {
  portrait: { w: 1080, h: 1920 },
  landscape: { w: 1920, h: 1080 },
};

/** "01:40" | "00:45" → seconds. */
export function durationToSeconds(label: string): number {
  const parts = label.split(":").map((n) => parseInt(n, 10));
  if (parts.some(Number.isNaN)) return 0;
  return parts.reduce((acc, n) => acc * 60 + n, 0);
}

/** seconds → "1:40" for compact UI labels. */
export function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function isHttp(url: string): boolean {
  return /^https?:\/\//i.test(url);
}

/** Resolve a stored video/poster reference to a usable URL. */
function resolveMediaUrl(ref: string | undefined, fallback: string): string {
  if (!ref) return fallback;
  if (isHttp(ref)) return ref; // already absolute (dev sample / CDN)
  // Local public asset (/portfolio/...) stays as-is; bare object keys get the base.
  if (ref.startsWith("/")) return ref;
  return `${MEDIA_BASE_URL}/${ref.replace(/^\//, "")}`;
}

function buildTags(item: PortfolioCase): string[] {
  if (item.tags?.length) return item.tags;
  const base = DIRECTION_TAGS[item.direction];
  const kickerTag = item.kicker.split(" ")[0]?.toLowerCase();
  return Array.from(new Set([...base, kickerTag].filter(Boolean))) as string[];
}

function buildSources(item: PortfolioCase, orientation: VideoOrientation): VideoSource[] {
  const url = resolveMediaUrl(item.video, "");
  if (!url) return [];
  const dims = ORIENTATION_DIMS[orientation];
  // Single underlying file mapped to both variants; the player picks by viewport.
  // When backend provides true renditions, this array is replaced verbatim.
  return [
    {
      variant: "mobile",
      publicUrl: url,
      contentType: "video/mp4",
      width: Math.round(dims.w * 0.667),
      height: Math.round(dims.h * 0.667),
      bitrateKbps: 1800,
    },
    {
      variant: "desktop",
      publicUrl: url,
      contentType: "video/mp4",
      width: dims.w,
      height: dims.h,
      bitrateKbps: 4500,
    },
  ];
}

function toCatalogVideo(item: PortfolioCase): CatalogVideo {
  const orientation = item.orientation ?? DIRECTION_ORIENTATION[item.direction];
  const durationSeconds = durationToSeconds(item.duration);
  const posterUrl = resolveMediaUrl(item.image, POSTER_FALLBACK);
  const dims = ORIENTATION_DIMS[orientation];

  return {
    videoId: item.videoId ?? `c3flex-${item.id}`,
    slug: item.slug ?? item.id,
    title: item.title,
    description: item.statement,
    shortDescription: item.shortDescription ?? item.kicker,
    tags: buildTags(item),
    category: "portfolio",
    direction: item.direction,
    featured: item.featured ?? false,
    durationSeconds,
    orientation,
    status: item.status ?? (item.video ? "READY" : "DRAFT"),
    poster: {
      assetId: null,
      publicUrl: posterUrl,
      contentType: "image/jpeg",
      width: dims.w,
      height: dims.h,
    },
    sources: buildSources(item, orientation),
    assets: {
      originalUrl: item.originalUrl,
      adaptedUrl: item.adaptedUrl,
      previewUrl: item.previewUrl,
    },
    cta: { label: "Обсудить похожий проект", intent: "PROJECT_REQUEST" },
    accent: item.accent,
    year: item.year,
    durationLabel: item.duration,
  };
}

export const catalogVideos: CatalogVideo[] = portfolioCases.map(toCatalogVideo);

export function getCatalogVideo(slug: string): CatalogVideo | undefined {
  return catalogVideos.find((v) => v.slug === slug);
}

export function getCatalogByDirection(dir: DirectionId, limit?: number): CatalogVideo[] {
  const filtered = catalogVideos.filter((v) => v.direction === dir);
  return limit ? filtered.slice(0, limit) : filtered;
}

export function getFeaturedCatalog(): CatalogVideo[] {
  return catalogVideos.filter((v) => v.featured);
}

/**
 * The video-render TZ ships every rendition in two formats — WebM (VP9)
 * and MP4 (H.264) — and the browser picks the first playable one, so WebM
 * is always listed before MP4. Resolution is chosen by variant.
 */
const FORMAT_ORDER = ["video/webm", "video/mp4"];
function byFormat(a: VideoSource, b: VideoSource): number {
  const rank = (t: string) => {
    const i = FORMAT_ORDER.indexOf(t);
    return i === -1 ? FORMAT_ORDER.length : i;
  };
  return rank(a.contentType) - rank(b.contentType);
}

/**
 * Ordered `<source>` list for the current viewport (mobile ≤768 else desktop),
 * WebM before MP4. Falls back to the other variant, so the UI never breaks on a
 * single-source item. Used by the modal player, which re-picks on resize.
 */
export function selectSources(
  video: Pick<CatalogVideo, "sources">,
  viewportWidth: number,
): VideoSource[] {
  if (!video.sources.length) return [];
  const preferred = viewportWidth <= 768 ? "mobile" : "desktop";
  const chosen = video.sources.filter((s) => s.variant === preferred);
  return (chosen.length ? chosen : video.sources).slice().sort(byFormat);
}

export type HeroSource = { src: string; type: string; media?: string };

/**
 * Declarative `<source>` list for the always-on hero background. Native `media`
 * lets the browser pick resolution once at load — no JS, no reload churn on
 * resize — and `type` prefers WebM. Mobile renditions are media-gated so
 * desktop clients take the heavier 1080p file; WebM before MP4 within each.
 * ponytail: assumes the hero ships both mobile+desktop renditions (the render TZ
 * guarantees it); a single-variant hero would need the media gate dropped.
 */
export function heroSources(video: Pick<CatalogVideo, "sources">): HeroSource[] {
  const forVariant = (variant: VideoSource["variant"], media?: string) =>
    video.sources
      .filter((s) => s.variant === variant)
      .sort(byFormat)
      .map((s) => ({ src: s.publicUrl, type: s.contentType, media }));
  return [...forVariant("mobile", "(max-width: 768px)"), ...forVariant("desktop")];
}

/** Minimal reference passed into the Web Chat payload (selectedVideo). */
export function toSelectedVideoRef(video: CatalogVideo) {
  return { videoId: video.videoId, slug: video.slug, title: video.title };
}
