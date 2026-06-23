/**
 * OAuth login wiring (frontend half).
 *
 * Matches FRONTEND_BACKEND_CONTRACTS.md §5. Frontend offers ONLY Google and
 * Yandex; Keycloak is the OIDC boundary on the backend. The frontend performs a
 * REAL browser redirect into the backend OAuth2 entry point — it does not (and
 * must not) handle client secrets or validate tokens. That stays with Codex.
 *
 * Flow:
 *   1. startLogin(provider) resolves the backend authorization URL.
 *   2. The caller navigates the browser there.
 *   3. Backend (Keycloak) federates Google/Yandex and redirects to
 *      `${origin}/auth/callback`.
 *   4. The callback page calls GET /api/auth/me to read the session.
 */

export type OAuthProvider = "google" | "yandex";

export type LoginStartRequest = {
  provider: OAuthProvider;
  redirectUri: string;
  returnTo: string;
};

export type LoginStartResponse = {
  provider: string;
  authorizationUrl: string;
  redirectUri: string;
  issuedAt: string;
};

export type CurrentUser = {
  subject: string;
  roles: string[];
  claims: Record<string, string>;
  resolvedAt: string;
};

const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "").replace(/\/$/, "");
const RETURN_TO_KEY = "c3flex.auth.returnTo";

function origin(): string {
  return typeof window !== "undefined" ? window.location.origin : "";
}

export function currentPath(): string {
  if (typeof window === "undefined") return "/";
  return window.location.pathname + window.location.search + window.location.hash;
}

/** Make a possibly-relative backend URL absolute against the API gateway. */
function absolute(url: string): string {
  if (/^https?:\/\//i.test(url)) return url;
  return `${API_BASE}${url.startsWith("/") ? "" : "/"}${url}`;
}

/**
 * Resolve the backend authorization URL for a provider.
 *
 * If NEXT_PUBLIC_AUTH_LOGIN_ENDPOINT is configured, POST the contract request
 * and use the returned `authorizationUrl`. Otherwise fall back to the Spring
 * Security default entry point `${API_BASE}/oauth2/authorization/keycloak`.
 */
export async function startLogin(provider: OAuthProvider): Promise<string> {
  const returnTo = currentPath();
  const redirectUri = `${origin()}/auth/callback`;
  const endpoint = process.env.NEXT_PUBLIC_AUTH_LOGIN_ENDPOINT;

  // Remember where to send the user back after a successful login.
  try {
    if (typeof window !== "undefined") {
      window.sessionStorage.setItem(RETURN_TO_KEY, returnTo);
    }
  } catch {
    /* ignore */
  }

  if (endpoint) {
    const body: LoginStartRequest = { provider, redirectUri, returnTo };
    const res = await fetch(absolute(endpoint), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error("login start failed");
    const data = (await res.json()) as LoginStartResponse;
    return absolute(data.authorizationUrl);
  }

  // Spring Security OAuth2 default entry point. `kc_idp_hint` asks Keycloak to
  // pre-select the upstream IdP (google / yandex).
  const params = new URLSearchParams({
    kc_idp_hint: provider,
    provider,
    redirect_uri: redirectUri,
  });
  return `${API_BASE}/oauth2/authorization/keycloak?${params.toString()}`;
}

/** Read the authenticated session (after callback). */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    const res = await fetch(absolute("/api/auth/me"), {
      method: "GET",
      credentials: "include",
      headers: { Accept: "application/json" },
    });
    if (!res.ok) return null;
    return (await res.json()) as CurrentUser;
  } catch {
    return null;
  }
}

export async function logout(): Promise<void> {
  try {
    await fetch(absolute("/api/auth/logout"), { method: "POST", credentials: "include" });
  } catch {
    /* ignore */
  }
}

export function consumeReturnTo(): string {
  try {
    if (typeof window === "undefined") return "/";
    const v = window.sessionStorage.getItem(RETURN_TO_KEY);
    window.sessionStorage.removeItem(RETURN_TO_KEY);
    return v || "/";
  } catch {
    return "/";
  }
}
