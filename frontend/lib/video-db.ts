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
 * The next production step is to move this JSON shape into Mongo/Object Storage
 * metadata: keep `sourcePath` as the Yandex.Disk master pointer, add public
 * object keys for web/poster/preview, and keep adapted versions linked to the
 * same video id. The UI should continue to consume the normalized catalog, not
 * filesystem paths.
 *
 *   src / poster forms (same as the manifests): "clip.mp4" → /portfolio/clip.mp4,
 *   "/local/path", or an absolute "https://…" URL.
 */
export type VideoRecord = {
  id: string;
  title: string;
  category: string; // original Yandex folder (e.g. "5. Reels Контент")
  direction: DirectionId; // mapped site row — override per record if wrong
  productCategory?: string; // product-level label, e.g. "PODКАСТ" or "ФILM"
  tags?: string[];
  sourcePath: string; // path inside the Yandex.Disk archive (reference only)
  sizeBytes: number; // master size (reference only)
  src: string; // hosted URL — YOU fill this
  poster: string; // hosted poster — optional
  originalUrl?: string; // canonical master URL / Yandex.Disk link when exported
  objectKey?: string; // Object Storage key for the web rendition
  posterObjectKey?: string; // Object Storage key for the card poster
  previewObjectKey?: string; // Object Storage key for lightweight autoplay-safe preview
  previewUrl?: string; // lightweight autoplay-safe preview
  adaptedUrl?: string; // cropped/subtitled/social/site-specific version
  duration: string; // "01:40" — optional
  featured: boolean;
  orientation: "" | "portrait" | "landscape";
};

export const videoDb = videos as unknown as VideoRecord[];

type VideoClassification = {
  direction: DirectionId;
  category: string;
  tags: string[];
  orientation?: "portrait" | "landscape";
};

function hasPrefix(path: string, prefixes: string[]): boolean {
  return prefixes.some((prefix) => path.startsWith(prefix));
}

function classifyRecord(record: VideoRecord): VideoClassification {
  const path = record.sourcePath;

  if (hasPrefix(path, ["/VIDEO C3AG/5. Reels Контент", "/VIDEO C3AG/10. Рестораны и еда", "/VIDEO C3AG/11.Спорт и танцы", "/FOOD SHOWREEL"])) {
    return {
      direction: "reels",
      category: "RИИLS",
      tags: ["reels", "social", "short-form"],
      orientation: "portrait",
    };
  }

  if (hasPrefix(path, ["/VIDEO C3AG/4. Эвент и репортажи", "/VIDEO C3AG/8. Трансляции и сеты"])) {
    return {
      direction: "events",
      category: "REПОРТАЖ",
      tags: ["reportage", "events", "aftermovie"],
      orientation: "landscape",
    };
  }

  if (hasPrefix(path, ["/VIDEO C3AG/7. Подкасты", "/VIDEO C3AG/6. Интервью"])) {
    return {
      direction: "events",
      category: "PODКАСТ",
      tags: ["podcasts", "interview", "multicamera"],
      orientation: "landscape",
    };
  }

  if (hasPrefix(path, ["/VIDEO C3AG/1.Докментально кино"])) {
    return {
      direction: "events",
      category: "ФILM",
      tags: ["films", "documentary", "corporate-film"],
      orientation: "landscape",
    };
  }

  if (hasPrefix(path, ["/AI/"])) {
    return {
      direction: "commercials",
      category: "ЫI",
      tags: ["ai", "advertising", "generative-video"],
      orientation: "landscape",
    };
  }

  if (hasPrefix(path, [
    "/VIDEO C3AG/2. Клипы",
    "/VIDEO C3AG/3. Реклама",
    "/VIDEO C3AG/9. Автомобили и техника",
    "/VIDEO C3AG/Коммерческие ролики",
    "/VIDEO C3AG/Моушен графика",
  ])) {
    return {
      direction: "commercials",
      category: "RECLAMA",
      tags: ["advertising", "commercial", "brand-film"],
      orientation: "landscape",
    };
  }

  return {
    direction: record.direction,
    category: record.productCategory?.trim() || record.category || record.direction,
    tags: record.tags ?? [],
    orientation: record.orientation || undefined,
  };
}

/** Records you've hosted (src filled), mapped to the catalog's clip shape. */
export function publishedClips(): CatalogClip[] {
  return videoDb
    .map((r, index) => ({ r, index }))
    .filter(({ r }) => r.src.trim() || r.objectKey?.trim())
    .sort((a, b) => {
      const hostedA = a.r.objectKey?.trim() ? 1 : 0;
      const hostedB = b.r.objectKey?.trim() ? 1 : 0;
      if (hostedA !== hostedB) return hostedB - hostedA;
      if (a.r.featured !== b.r.featured) return a.r.featured ? -1 : 1;
      return a.index - b.index;
    })
    .map(({ r }) => {
      const classification = classifyRecord(r);
      const objectRef = (key?: string) => (key?.trim() ? `s3:${key.trim()}` : undefined);
      const src = objectRef(r.objectKey) ?? r.src.trim();
      const c: CatalogClip = {
        title: r.title,
        direction: classification.direction,
        category: classification.category,
        src,
      };
      // The archive folder is the only thing that tells podcasts apart from
      // documentaries — both land in the same site direction.
      if (r.sourcePath) c.folder = r.sourcePath;
      c.poster = objectRef(r.posterObjectKey) ?? r.poster.trim();
      if (r.originalUrl?.trim()) c.originalUrl = r.originalUrl.trim();
      if (r.adaptedUrl?.trim()) c.adaptedUrl = r.adaptedUrl.trim();
      c.previewUrl = objectRef(r.previewObjectKey) ?? r.previewUrl?.trim();
      if (r.duration.trim()) c.duration = r.duration.trim();
      c.tags = Array.from(new Set([...(r.tags ?? []), ...classification.tags]));
      if (r.featured) c.featured = true;
      c.orientation = r.orientation || classification.orientation;
      return c;
    });
}

/** Counts for tooling / the studio (total vs. how many are hosted). */
export function dbStats() {
  const hosted = videoDb.filter((r) => r.src.trim()).length;
  return { total: videoDb.length, hosted, pending: videoDb.length - hosted };
}
