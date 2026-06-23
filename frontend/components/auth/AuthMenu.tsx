"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronDown, LogOut, User } from "lucide-react";
import { startLogin, type OAuthProvider } from "@/lib/auth-api";
import { useAuth } from "@/hooks/useAuth";
import { ProviderButton } from "./ProviderButton";

/**
 * Header auth control.
 *  - logged out → "Войти" opens a dropdown with branded Google / Yandex buttons;
 *  - logged in  → shows the display name + "Выйти".
 * Session comes from GET /api/auth/me (cookie-based, backend-owned).
 */
export function AuthMenu() {
  const { user, loading, displayName, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState<OAuthProvider | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  const handleLogin = async (provider: OAuthProvider) => {
    setPending(provider);
    try {
      const url = await startLogin(provider);
      window.location.assign(url);
    } catch {
      setPending(null);
    }
  };

  if (loading) {
    return <span className="auth-pill auth-pill--ghost" aria-hidden="true" />;
  }

  if (user) {
    return (
      <div className="auth-menu" ref={ref}>
        <button
          type="button"
          className="auth-pill"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
        >
          <User size={15} />
          <span className="auth-pill-name">{displayName}</span>
          <ChevronDown size={14} />
        </button>
        {open && (
          <div className="auth-dropdown" role="menu">
            <p className="auth-dropdown-name">{displayName}</p>
            <button
              type="button"
              className="auth-logout"
              onClick={() => {
                setOpen(false);
                void logout();
              }}
            >
              <LogOut size={15} /> Выйти
            </button>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="auth-menu" ref={ref}>
      <button
        type="button"
        className="auth-pill"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        Войти
        <ChevronDown size={14} />
      </button>
      {open && (
        <div className="auth-dropdown auth-dropdown--login" role="menu">
          <p className="auth-dropdown-hint">Войдите, чтобы сохранить историю обращений</p>
          <ProviderButton provider="google" onClick={handleLogin} disabled={pending !== null} />
          <ProviderButton provider="yandex" onClick={handleLogin} disabled={pending !== null} />
        </div>
      )}
    </div>
  );
}
