import type { DirectionId } from "./portfolio";
import type { CatalogClip } from "./catalog-clips";
import videos from "@/data/videos.json";

/**
 * Video database — the full C3AG portfolio inventory pulled from the Yandex.Disk
 * archive (data/videos.json, 197 records). The heavy masters live OUTSIDE the
 * repo (135 GB); this DB only stores metadata + where you host each file.
 *
 * Workflow: host a clip somewhere (Object Storage / CDN / a light rendition in
 * public/portfolio) and paste that URL into the record's `src` (and `poster`).
 * Any record with a non-empty `src` becomes live in the catalog automatically —
 * records without one stay in the DB as "not yet hosted" and don't render.
 *
 *   src / poster forms (same as the manifests): "clip.mp4" → /portfolio/clip.mp4,
 *   "/local/path", or an absolute "https://…" URL.
 */
export type VideoRecord = {
  id: string;
  title: string;
  category: string; // original Yandex folder (e.g. "5. Reels Контент")
  direction: DirectionId; // mapped site row — override per record if wrong
  sourcePath: string; // path inside the Yandex.Disk archive (reference only)
  sizeBytes: number; // master size (reference only)
  src: string; // hosted URL — YOU fill this
  poster: string; // hosted poster — optional
  duration: string; // "01:40" — optional
  featured: boolean;
  orientation: "" | "portrait" | "landscape";
};

export const videoDb = videos as unknown as VideoRecord[];

/** Records you've hosted (src filled), mapped to the catalog's clip shape. */
export function publishedClips(): CatalogClip[] {
  return videoDb
    .filter((r) => r.src.trim())
    .map((r) => {
      const c: CatalogClip = { title: r.title, direction: r.direction, src: r.src.trim() };
      if (r.poster.trim()) c.poster = r.poster.trim();
      if (r.duration.trim()) c.duration = r.duration.trim();
      if (r.featured) c.featured = true;
      if (r.orientation) c.orientation = r.orientation;
      return c;
    });
}

/** Counts for tooling / the studio (total vs. how many are hosted). */
export function dbStats() {
  const hosted = videoDb.filter((r) => r.src.trim()).length;
  return { total: videoDb.length, hosted, pending: videoDb.length - hosted };
}
