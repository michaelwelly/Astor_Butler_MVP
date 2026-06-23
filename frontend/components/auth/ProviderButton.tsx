"use client";

import type { OAuthProvider } from "@/lib/auth-api";

type Props = {
  provider: OAuthProvider;
  onClick: (provider: OAuthProvider) => void;
  disabled?: boolean;
};

const LABELS: Record<OAuthProvider, string> = {
  google: "Продолжить с Google",
  yandex: "Войти с Яндекс ID",
};

/** Official brand glyphs as inline SVG (no emojis, per design-system rules). */
function ProviderGlyph({ provider }: { provider: OAuthProvider }) {
  if (provider === "google") {
    return (
      <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true" focusable="false">
        <path
          fill="#4285F4"
          d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.7-1.57 2.68-3.88 2.68-6.62Z"
        />
        <path
          fill="#34A853"
          d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.92v2.33A9 9 0 0 0 9 18Z"
        />
        <path
          fill="#FBBC05"
          d="M3.97 10.72A5.4 5.4 0 0 1 3.68 9c0-.6.1-1.18.29-1.72V4.95H.92A9 9 0 0 0 0 9c0 1.45.35 2.82.92 4.05l3.05-2.33Z"
        />
        <path
          fill="#EA4335"
          d="M9 3.58c1.32 0 2.5.46 3.44 1.35l2.58-2.58C13.47.9 11.43 0 9 0A9 9 0 0 0 .92 4.95l3.05 2.33C4.68 5.16 6.66 3.58 9 3.58Z"
        />
      </svg>
    );
  }
  // Yandex ID — white "Я" on brand red disc.
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="12" fill="#FC3F1D" />
      <path
        fill="#fff"
        d="M13.3 5.6h-1.7c-2.2 0-3.85 1.55-3.85 3.86 0 1.73.83 2.74 2.36 3.78L7.4 18.4h1.96l2.62-4.96h.97V18.4h1.74V5.6Zm-1.74 6.4h-.74c-1.05 0-1.64-.6-1.64-1.7 0-1.16.66-1.78 1.66-1.78h.72v3.48Z"
      />
    </svg>
  );
}

export function ProviderButton({ provider, onClick, disabled }: Props) {
  return (
    <button
      type="button"
      className={`provider-btn provider-btn--${provider}`}
      onClick={() => onClick(provider)}
      disabled={disabled}
    >
      <span className="provider-btn-glyph">
        <ProviderGlyph provider={provider} />
      </span>
      <span className="provider-btn-label">{LABELS[provider]}</span>
    </button>
  );
}
