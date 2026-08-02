import { getSessionId } from "./session";

const USERNAME_RE = /^[A-Za-z][A-Za-z0-9_]{4,31}$/;

export type TelegramHandoffConfig =
  | {
      enabled: true;
      botUsername: string;
      startParameter: string;
      url: string;
    }
  | {
      enabled: false;
      reason: "disabled" | "missing_username" | "invalid_username";
      botUsername: string | null;
    };

function normalizeUsername(raw: string | undefined): string | null {
  const value = raw?.trim().replace(/^@/, "");
  return value || null;
}

function buildStartParameter(sessionId: string): string {
  const safeSession = sessionId.replace(/[^A-Za-z0-9_-]/g, "").slice(0, 40);
  return `c3ag_${safeSession}`.slice(0, 64);
}

export function readTelegramHandoffConfig(): TelegramHandoffConfig {
  const enabled = process.env.NEXT_PUBLIC_CLIO_TELEGRAM_HANDOFF_ENABLED === "true";
  const botUsername = normalizeUsername(process.env.NEXT_PUBLIC_CLIO_TELEGRAM_BOT_USERNAME);

  if (!enabled) return { enabled: false, reason: "disabled", botUsername };
  if (!botUsername) return { enabled: false, reason: "missing_username", botUsername: null };
  if (!USERNAME_RE.test(botUsername)) {
    return { enabled: false, reason: "invalid_username", botUsername };
  }

  const startParameter = buildStartParameter(getSessionId());
  return {
    enabled: true,
    botUsername,
    startParameter,
    url: `https://t.me/${botUsername}?start=${encodeURIComponent(startParameter)}`,
  };
}

export function telegramHandoffDisabledCopy(reason: TelegramHandoffConfig & { enabled: false }) {
  if (reason.reason === "invalid_username") {
    return "Telegram-переход скрыт: имя бота в конфигурации не прошло проверку.";
  }
  if (reason.reason === "missing_username") {
    return "Telegram-переход включим после проверки публичного имени бота.";
  }
  return "Telegram-переход сейчас выключен. Оставьте сообщение здесь, Clio сохранит контекст чата.";
}
