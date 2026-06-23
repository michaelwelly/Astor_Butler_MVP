"use client";

import { useState } from "react";
import { startLogin, type OAuthProvider } from "@/lib/auth-api";
import { ProviderButton } from "./ProviderButton";

type Props = {
  /** Compact variant for embedding inside the chat widget. */
  compact?: boolean;
};

/**
 * Login for Google and Yandex (FRONTEND_BACKEND_CONTRACTS.md §5).
 *
 * Performs a REAL redirect into the backend OAuth2 entry point. OAuth remains
 * optional: a guest can send a first lead with consent only; sign-in later
 * enriches the profile. Token handling lives entirely on the backend.
 */
export function LoginPanel({ compact }: Props) {
  const [pending, setPending] = useState<OAuthProvider | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handle = async (provider: OAuthProvider) => {
    setPending(provider);
    setError(null);
    try {
      const url = await startLogin(provider);
      // Hand the browser to Keycloak / the gateway OAuth entry point.
      window.location.assign(url);
    } catch {
      setError("Не удалось начать авторизацию. Сервер авторизации недоступен.");
      setPending(null);
    }
  };

  return (
    <div className={`login-panel${compact ? " login-panel--compact" : ""}`}>
      {!compact && <p className="login-panel-hint">Войти, чтобы сохранить историю обращений</p>}
      <div className="login-panel-buttons">
        <ProviderButton provider="google" onClick={handle} disabled={pending !== null} />
        <ProviderButton provider="yandex" onClick={handle} disabled={pending !== null} />
      </div>
      {error && <p className="login-panel-note">{error}</p>}
    </div>
  );
}
