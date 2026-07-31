"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchCurrentUser, logout as apiLogout, type CurrentUser } from "@/lib/auth-api";
import {
  clearLocalSession,
  onLocalSessionChange,
  readLocalSession,
  type LocalSession,
} from "@/lib/local-session";

type AuthState = {
  user: CurrentUser | null;
  loading: boolean;
  displayName: string | null;
  /** true when the session comes from the local email fallback, not the backend. */
  isLocal: boolean;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

/**
 * Resolves the session from the backend (GET /api/auth/me, cookie-based) and,
 * when that is absent, from the local email session. Tokens live in backend
 * cookies; the frontend only mirrors session presence.
 */
export function useAuth(): AuthState {
  const [backendUser, setBackendUser] = useState<CurrentUser | null>(null);
  const [backendLoading, setBackendLoading] = useState(true);
  const [local, setLocal] = useState<LocalSession | null>(null);
  const [localReady, setLocalReady] = useState(false);

  const refresh = useCallback(async () => {
    setBackendLoading(true);
    const me = await fetchCurrentUser();
    setBackendUser(me);
    setBackendLoading(false);
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    setLocal(readLocalSession());
    setLocalReady(true);
    return onLocalSessionChange(() => setLocal(readLocalSession()));
  }, []);

  const isLocal = !backendUser && !!local;
  const user: CurrentUser | null =
    backendUser ??
    (local
      ? {
          subject: local.email,
          roles: ["editor"],
          claims: { email: local.email, name: local.name },
          resolvedAt: "",
        }
      : null);

  const logout = useCallback(async () => {
    clearLocalSession();
    setLocal(null);
    await apiLogout();
    setBackendUser(null);
  }, []);

  const displayName =
    user?.claims?.name || user?.claims?.email || (user ? "Гость" : null);

  return {
    user,
    loading: backendLoading || !localReady,
    displayName,
    isLocal,
    logout,
    refresh,
  };
}
