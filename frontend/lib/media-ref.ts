/** Anything served straight off the Yandex.Disk archive goes through here. */
export const YADISK_ROUTE = "/api/yadisk";

/**
 * Resolve a video/poster reference used by the site's media manifests
 * (hero-clips.ts, catalog-clips.ts). One rule, shared, so both behave the same:
 *   "reel.mp4"              → /portfolio/reel.mp4  (drop the file in public/portfolio)
 *   "/anything/clip.mp4"    → used as-is (any local /public path)
 *   "https://…/clip.mp4"    → used as-is (absolute URL: CDN, object storage, …)
 *   "yadisk:/VIDEO C3AG/…"  → /api/yadisk?path=… (streamed from the public archive)
 * Returns undefined for an empty ref so callers can apply their own fallback.
 *
 * The `yadisk:` form exists because Yandex only issues short-lived signed URLs:
 * a real URL would be stale before anyone clicked it, so the manifest stores the
 * archive path and the route resolves it per request.
 */
export function resolveMediaRef(ref?: string): string | undefined {
  if (!ref) return undefined;
  if (ref.startsWith("yadisk-poster:"))
    return yadiskUrl(ref.slice("yadisk-poster:".length), "poster");
  if (ref.startsWith("yadisk:")) return yadiskUrl(ref.slice("yadisk:".length));
  if (/^https?:\/\//i.test(ref) || ref.startsWith("/")) return ref;
  return `/portfolio/${ref}`;
}

export function yadiskUrl(path: string, kind: "video" | "poster" = "video"): string {
  const params = new URLSearchParams({ path });
  if (kind === "poster") params.set("kind", "poster");
  return `${YADISK_ROUTE}?${params}`;
}

/**
 * True for sources that come off the archive. Those are camera masters — a
 * 173 MB median, 1.3 GB at the 90th percentile — so they are fine to stream
 * when someone asks for one, and hopeless as a silently autoplaying preview.
 */
export function isArchiveMaster(url?: string): boolean {
  return Boolean(url?.startsWith(YADISK_ROUTE));
}

/**
 * Turn an /api/yadisk?… URL into the direct Yandex link a <video> can actually
 * load. Needed because a media element hangs on the cross-origin redirect the
 * route would otherwise serve, so the URL has to be fetched first and assigned
 * to `src` by hand.
 *
 * Results are memoised per path — one archive clip appears in several places
 * and the signed link is good for a while, so a second lookup is waste. The
 * cache holds the promise, not the value, so ten simultaneous callers still
 * make one request.
 */
const archiveSrcCache = new Map<string, Promise<string | null>>();

export function resolveArchiveSrc(routeUrl: string): Promise<string | null> {
  const cached = archiveSrcCache.get(routeUrl);
  if (cached) return cached;

  const pending = fetch(`${routeUrl}&resolve=1`)
    .then((r) => (r.ok ? r.json() : null))
    .then((j: { url?: string } | null) => j?.url ?? null)
    .catch(() => null)
    .then((url) => {
      // A failed lookup must not be remembered as "this clip is broken".
      if (!url) archiveSrcCache.delete(routeUrl);
      return url;
    });

  archiveSrcCache.set(routeUrl, pending);
  return pending;
}
