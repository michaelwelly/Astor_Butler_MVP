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
const DIRECTION_ORIENTATION: Record<DirectionId, VideoOrientation> = {
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
 * Pick the best playable source for the current viewport.
 * UI must not break if only one source exists.
 */
export function selectSource(
  video: Pick<CatalogVideo, "sources">,
  viewportWidth: number,
): VideoSource | null {
  if (!video.sources.length) return null;
  const preferred = viewportWidth <= 768 ? "mobile" : "desktop";
  return video.sources.find((s) => s.variant === preferred) ?? video.sources[0];
}

/** Minimal reference passed into the Web Chat payload (selectedVideo). */
export function toSelectedVideoRef(video: CatalogVideo) {
  return { videoId: video.videoId, slug: video.slug, title: video.title };
}
