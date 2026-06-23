import { NextRequest, NextResponse } from "next/server";

/**
 * Local mock for the C3FLEX web chat.
 *
 * It accepts the production-shaped Web Chat body (FRONTEND_BACKEND_CONTRACTS.md
 * §4, `POST /api/messages`) and returns a contract-shaped response. It also
 * still accepts the legacy `{ messages }` shape for backward compatibility.
 *
 * NOTE: This is a frontend mock only. No real FSM/lead logic lives here — that
 * belongs to the backend (Codex). Do not turn this into a backend endpoint.
 */

type LegacyMessage = { from: "bot" | "user"; text: string };

type WebChatBody = {
  channel?: "WEB";
  text?: string;
  correlationId?: string;
  turn?: number; // dev-only hint from the widget to drive the guided script
  payload?: {
    selectedVideo?: { slug?: string } | null;
    consent?: { privacyAccepted?: boolean } | null;
  };
  messages?: LegacyMessage[]; // legacy shape
};

const QUESTIONS = [
  "Понял! Какая главная цель этого материала — что должен почувствовать или сделать зритель после просмотра?",
  "Какие услуги нужны: только съёмка, съёмка + монтаж, или полный продакшн под ключ?",
  "Формат: вертикальный рилс или горизонтальное видео? Примерный хронометраж готового материала?",
  "Когда нужен готовый материал? Укажите дедлайн.",
  "Есть ориентир по бюджету? Любая вилка поможет подобрать правильный состав команды.",
  "Отлично! Как с вами связаться? Укажите имя и телеграм или телефон.",
];

function scriptedReply(step: number): string {
  if (step <= 0) return QUESTIONS[0];
  if (step <= QUESTIONS.length) return QUESTIONS[step - 1];
  return (
    "✅ Запрос принят! Я передам детали команде C3FLEX, мы свяжемся в течение 2–3 часов. " +
    "Пока посмотрите похожие работы в каталоге выше ↑"
  );
}

export async function POST(req: NextRequest) {
  const body = (await req.json()) as WebChatBody;

  // Legacy shape: derive step from user message count.
  if (Array.isArray(body.messages)) {
    const step = body.messages.filter((m) => m.from === "user").length;
    return NextResponse.json({ reply: scriptedReply(step) });
  }

  // Contract shape: use the dev-only `turn` hint for the guided script.
  const step = typeof body.turn === "number" ? body.turn : 1;
  const reply = scriptedReply(step);

  // Mirror the production response contract (subset) alongside `reply`.
  return NextResponse.json({
    channel: "WEB",
    text: reply,
    reply, // convenience for the current widget
    nextState: step > QUESTIONS.length ? "READY_FOR_DIALOG" : "COLLECTING",
    fallback: false,
    actions: step > QUESTIONS.length ? ["WEB_LEAD_CAPTURED"] : [],
    createdAt: new Date().toISOString(),
  });
}
