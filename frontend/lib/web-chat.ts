/**
 * Web Chat request builder.
 *
 * Produces a body compatible with the existing backend endpoint
 * `POST /api/messages` (FRONTEND_BACKEND_CONTRACTS.md §4). The frontend posts
 * to a local mock (`/api/chat`) for now, but the payload shape is the real
 * contract so the only change at integration time is the endpoint URL.
 */

import { CURRENT_POLICY, getConsent, type ConsentEvidence } from "./consent";
import {
  buildCorrelationId,
  getExternalUserId,
  getSessionId,
  getTempChatId,
} from "./session";
import { captureUtm, getReferrer, type Utm } from "./utm";

export type SelectedVideoRef = {
  videoId: string;
  slug: string;
  title: string;
} | null;

export type ViewportInfo = {
  width: number;
  height: number;
  devicePixelRatio: number;
  locale: string;
  timezone: string;
};

export type WebChatPayload = {
  site: "c3flex";
  sessionId: string;
  page: string;
  referrer: string | null;
  utm: Utm;
  selectedVideo: SelectedVideoRef;
  viewport: ViewportInfo;
  consent: ConsentEvidence | null;
};

export type WebChatRequest = {
  channel: "WEB";
  externalUserId: string;
  chatId: number;
  text: string;
  contactPhone: string | null;
  firstName: string | null;
  username: string | null;
  correlationId: string;
  payload: WebChatPayload;
};

function readViewport(): ViewportInfo {
  if (typeof window === "undefined") {
    return { width: 0, height: 0, devicePixelRatio: 1, locale: "ru-RU", timezone: "UTC" };
  }
  let timezone = "UTC";
  try {
    timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";
  } catch {
    /* ignore */
  }
  return {
    width: window.innerWidth,
    height: window.innerHeight,
    devicePixelRatio: window.devicePixelRatio || 1,
    locale: navigator.language || "ru-RU",
    timezone,
  };
}

function currentPage(): string {
  if (typeof window === "undefined") return "/";
  return window.location.pathname + window.location.hash;
}

/** Assemble the full contract request for a single outbound message. */
export function buildWebChatRequest(
  text: string,
  selectedVideo: SelectedVideoRef,
): WebChatRequest {
  const sessionId = getSessionId();
  const consent = getConsent() ?? {
    privacyAccepted: false,
    policyVersion: CURRENT_POLICY.version,
    acceptedAt: new Date().toISOString(),
  };

  return {
    channel: "WEB",
    externalUserId: getExternalUserId(),
    chatId: getTempChatId(),
    text,
    contactPhone: null,
    firstName: null,
    username: null,
    correlationId: buildCorrelationId(sessionId),
    payload: {
      site: "c3flex",
      sessionId,
      page: currentPage(),
      referrer: getReferrer(),
      utm: captureUtm(),
      selectedVideo,
      viewport: readViewport(),
      consent,
    },
  };
}

/**
 * Send a message. Posts the contract body to the local mock endpoint today;
 * swap NEXT_PUBLIC_WEB_CHAT_ENDPOINT to the real gateway path when ready.
 *
 * `devHint.turn` is a dev-only sibling field used solely by the local mock to
 * advance its guided KP script. It sits OUTSIDE `payload`, so the contract
 * `payload` stays byte-compatible and the real backend ignores unknown fields.
 */
export async function sendWebChatMessage(
  text: string,
  selectedVideo: SelectedVideoRef,
  devHint?: { turn: number },
): Promise<{ reply: string }> {
  const endpoint = process.env.NEXT_PUBLIC_WEB_CHAT_ENDPOINT ?? "/api/chat";
  const body = buildWebChatRequest(text, selectedVideo);

  const res = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Request-Id": body.correlationId,
    },
    body: JSON.stringify(devHint ? { ...body, turn: devHint.turn } : body),
  });

  if (!res.ok) throw new Error("web-chat request failed");
  const data = await res.json();
  // Mock returns { reply }; real contract returns { text, ... }.
  return { reply: data.reply ?? data.text ?? "" };
}
