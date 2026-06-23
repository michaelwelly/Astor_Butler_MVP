"use client";

import { useEffect, useState } from "react";
import { consumeReturnTo, fetchCurrentUser, type CurrentUser } from "@/lib/auth-api";

type Phase = "loading" | "ok" | "anon";

/**
 * OAuth return landing (FRONTEND_BACKEND_CONTRACTS.md §5).
 *
 * Keycloak redirects here after federating Google/Yandex. We read the session
 * via GET /api/auth/me (cookie-based) and send the user back to where they
 * started. No tokens are stored in the frontend.
 */
export default function AuthCallbackPage() {
  const [phase, setPhase] = useState<Phase>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const me = await fetchCurrentUser();
      if (cancelled) return;
      setUser(me);
      setPhase(me ? "ok" : "anon");
      const returnTo = consumeReturnTo();
      // Brief confirmation, then return the user to their previous page.
      window.setTimeout(() => {
        window.location.replace(returnTo || "/");
      }, me ? 900 : 1600);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="auth-callback">
      <div className="auth-callback-card">
        {phase === "loading" && <p>Завершаем вход…</p>}
        {phase === "ok" && (
          <p>
            Добро пожаловать{user?.claims?.name ? `, ${user.claims.name}` : ""}. Возвращаем вас
            обратно…
          </p>
        )}
        {phase === "anon" && (
          <p>
            Сессия не найдена — сервер авторизации ещё не подключён. Вы можете продолжить как
            гость; возвращаем вас обратно…
          </p>
        )}
      </div>
    </main>
  );
}
