/**
 * Site privacy consent (FRONTEND_BACKEND_CONTRACTS.md §6).
 *
 * Consent must be accepted before sending chat/contact data. Until the backend
 * supports anonymous consent persistence, evidence travels inside the Web Chat
 * payload. This module is frontend-only state; the backend Consent Vault stays
 * the source of truth.
 */

export const CURRENT_POLICY = {
  version: "2026-06-02-local",
  title: "Astor Butler local MVP privacy policy placeholder",
  url: "/docs/policy.html",
  effectiveFrom: "2026-06-02T00:00:00Z",
} as const;

export type ConsentEvidence = {
  privacyAccepted: boolean;
  policyVersion: string;
  acceptedAt: string; // ISO
};

const CONSENT_KEY = "c3flex.consent";

export function getConsent(): ConsentEvidence | null {
  try {
    if (typeof window === "undefined") return null;
    const raw = window.localStorage.getItem(CONSENT_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as ConsentEvidence;
    // Re-prompt if the policy version moved on.
    if (parsed.policyVersion !== CURRENT_POLICY.version) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function hasConsent(): boolean {
  return getConsent()?.privacyAccepted === true;
}

export function acceptConsent(): ConsentEvidence {
  const evidence: ConsentEvidence = {
    privacyAccepted: true,
    policyVersion: CURRENT_POLICY.version,
    acceptedAt: new Date().toISOString(),
  };
  try {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(CONSENT_KEY, JSON.stringify(evidence));
    }
  } catch {
    /* ignore storage failure */
  }
  return evidence;
}
