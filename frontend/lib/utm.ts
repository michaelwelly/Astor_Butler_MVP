/**
 * UTM + referrer capture for the Web Chat payload (FRONTEND_BACKEND_CONTRACTS.md §4).
 * First-touch UTM is persisted for the session so a later chat message still
 * carries the original campaign attribution.
 */

export type Utm = {
  source: string | null;
  medium: string | null;
  campaign: string | null;
  term?: string | null;
  content?: string | null;
};

const UTM_KEY = "c3flex.utm";

function readStored(): Utm | null {
  try {
    if (typeof window === "undefined") return null;
    const raw = window.localStorage.getItem(UTM_KEY);
    return raw ? (JSON.parse(raw) as Utm) : null;
  } catch {
    return null;
  }
}

function store(utm: Utm): void {
  try {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(UTM_KEY, JSON.stringify(utm));
    }
  } catch {
    /* ignore */
  }
}

/** Parse UTM params from the current URL; persist first-touch values. */
export function captureUtm(): Utm {
  const empty: Utm = { source: null, medium: null, campaign: null, term: null, content: null };
  if (typeof window === "undefined") return empty;

  const params = new URLSearchParams(window.location.search);
  const fromUrl: Utm = {
    source: params.get("utm_source"),
    medium: params.get("utm_medium"),
    campaign: params.get("utm_campaign"),
    term: params.get("utm_term"),
    content: params.get("utm_content"),
  };

  const hasAny = Object.values(fromUrl).some((v) => v != null && v !== "");
  if (hasAny) {
    store(fromUrl);
    return fromUrl;
  }
  return readStored() ?? empty;
}

export function getReferrer(): string | null {
  if (typeof document === "undefined") return null;
  return document.referrer || null;
}
