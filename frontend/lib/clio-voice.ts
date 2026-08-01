export type ClioVoiceStatus =
  | "idle"
  | "listening"
  | "processing"
  | "speaking"
  | "error"
  | "permission_denied"
  | "unavailable";

export type BrowserVoiceProbe = {
  secureContext: boolean;
  hasMediaDevices: boolean;
  hasMediaRecorder: boolean;
};

export type BrowserVoiceAvailability =
  | { ok: true }
  | { ok: false; code: "insecure_context" | "media_devices_unavailable" | "media_recorder_unavailable" };

export type BrowserVoiceUnavailableCode = Exclude<BrowserVoiceAvailability, { ok: true }>["code"];

export type ClioTranscribeMockResult =
  | {
      ok: true;
      status: "TRANSCRIBED";
      text: string;
      confidence: number;
      provider: "test-double";
    }
  | {
      ok: false;
      status: "UNAVAILABLE" | "FAILED";
      code: string;
      message: string;
      provider: "test-double" | "yandex-speechkit";
    };

export const CLIO_VOICE_STATUS_COPY: Record<ClioVoiceStatus, string> = {
  idle: "Сказать голосом",
  listening: "Clio слушает. Запись идёт только сейчас.",
  processing: "Clio распознаёт голос и готовит текст.",
  speaking: "Clio готовит голосовой ответ.",
  error: "Голос сейчас не сработал. Можно написать текстом.",
  permission_denied: "Доступ к микрофону отклонён. Разрешите микрофон в браузере или напишите текстом.",
  unavailable: "Голосовой ввод пока недоступен. Напишите сообщение текстом.",
};

export const CLIO_VOICE_STARTERS = [
  "Помоги оценить стоимость съёмки",
  "Подскажи сроки под мой проект",
  "Помоги выбрать формат",
  "Хочу отправить бриф",
  "Хочу поговорить с продюсером",
] as const;

export function readClioVoiceClientConfig() {
  return {
    voiceEnabled: process.env.NEXT_PUBLIC_CLIO_VOICE_ENABLED === "true",
    ttsEnabled: process.env.NEXT_PUBLIC_CLIO_TTS_ENABLED === "true",
    transcribeEndpoint:
      process.env.NEXT_PUBLIC_CLIO_VOICE_TRANSCRIBE_ENDPOINT ?? "/api/chat/transcribe",
    speakEndpoint: process.env.NEXT_PUBLIC_CLIO_TTS_ENDPOINT ?? "/api/chat/speak",
  };
}

export function browserVoiceAvailability(
  probe: BrowserVoiceProbe,
): BrowserVoiceAvailability {
  if (!probe.secureContext) return { ok: false, code: "insecure_context" };
  if (!probe.hasMediaDevices) return { ok: false, code: "media_devices_unavailable" };
  if (!probe.hasMediaRecorder) return { ok: false, code: "media_recorder_unavailable" };
  return { ok: true };
}

export function browserVoiceErrorMessage(code: BrowserVoiceUnavailableCode) {
  switch (code) {
    case "insecure_context":
      return "Микрофон работает только на HTTPS или localhost. Текущий HTTP-preview оставляет голос выключенным.";
    case "media_devices_unavailable":
      return "Браузер не дал доступ к mediaDevices. Попробуйте другой браузер или напишите текстом.";
    case "media_recorder_unavailable":
      return "Браузер не поддерживает безопасную запись аудио для сайта. Напишите сообщение текстом.";
    default:
      return CLIO_VOICE_STATUS_COPY.unavailable;
  }
}

export function clioSttUnavailableResult(): Extract<ClioTranscribeMockResult, { ok: false }> {
  return {
    ok: false,
    provider: "yandex-speechkit",
    status: "UNAVAILABLE",
    code: "STT_NOT_CONFIGURED",
    message:
      "SpeechKit STT is not configured. Keep credentials server-side and enable only after HTTPS and billing are verified.",
  };
}

export function clioTranscribeTestDouble(
  input: { scenario?: string | null; transcript?: string | null },
): ClioTranscribeMockResult {
  if (input.scenario === "empty") {
    return {
      ok: false,
      provider: "test-double",
      status: "FAILED",
      code: "EMPTY_TRANSCRIPT",
      message: "Audio was accepted by the test double, but no text was recognized.",
    };
  }
  return {
    ok: true,
    provider: "test-double",
    status: "TRANSCRIBED",
    text: input.transcript?.trim() || "Хочу подобрать формат и смету для проекта",
    confidence: 0.99,
  };
}
