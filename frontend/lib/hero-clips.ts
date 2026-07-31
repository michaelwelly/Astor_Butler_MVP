/**
 * Hero showreel manifest — the ONE place to point the floating gadgets at videos.
 *
 * Two lists. Portrait clips fill the phones / tablets / watches; landscape clips
 * fill the laptops / monitors / TVs. The gadgets cycle through each list, so you
 * just add clips here and they spread across the whole field automatically — no
 * need to touch DeviceHero or the 30-item portfolio catalog.
 *
 * `src` accepts, in order of laziness:
 *   "reel.mp4"               → a file you dropped in  frontend/public/portfolio/
 *   "/anything/clip.mp4"     → any local /public path, used as-is
 *   "https://cdn…/clip.mp4"  → an absolute URL (Yandex.Disk direct link, CDN, …)
 *
 * `poster` (optional) is the still shown before play and on the blurred back
 * layer; omit it and a dark fallback is used. Same three src forms apply.
 *
 * Leave a list empty and the hero falls back to the sample catalog, so the page
 * never breaks while you fill this in. Keep heavy originals OUT of git — drop
 * only light web renditions in /public, or use absolute URLs for the big files.
 */
export type HeroClip = { src: string; poster?: string };

export const HERO_CLIPS: { portrait: HeroClip[]; landscape: HeroClip[] } = {
  // → phones, tablets, watches (vertical)
  portrait: [
    // { src: "my-reel-vertical.mp4", poster: "my-reel-vertical.jpg" },
  ],
  // → laptops, monitors, TVs (horizontal)
  landscape: [
    // { src: "my-showreel.mp4" },
  ],
};
