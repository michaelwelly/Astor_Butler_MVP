"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchCurrentUser, logout as apiLogout, type CurrentUser } from "@/lib/auth-api";

type AuthState = {
  user: CurrentUser | null;
  loading: boolean;
  displayName: string | null;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

/**
 * Reads the current session via GET /api/auth/me and exposes logout.
 * Tokens live in backend cookies — the frontend only mirrors session presence.
 */
export function useAuth(): AuthState {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    const me = await fetchCurrentUser();
    setUser(me);
    setLoading(false);
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const logout = useCallback(async () => {
    await apiLogout();
    setUser(null);
  }, []);

  const displayName =
    user?.claims?.name || user?.claims?.email || (user ? "Гость" : null);

  return { user, loading, displayName, logout, refresh };
}
