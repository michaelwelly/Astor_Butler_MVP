#!/usr/bin/env node

const args = new Map(
  process.argv.slice(2).map((arg) => {
    const [key, ...rest] = arg.replace(/^--/, "").split("=");
    return [key, rest.join("=") || "true"];
  }),
);

const folderId = args.get("folder-id") ?? process.env.YANDEX_FOLDER_ID;
const apiKey = args.get("api-key") ?? process.env.YANDEX_API_KEY;
const iamToken = args.get("iam-token") ?? process.env.YANDEX_IAM_TOKEN;
const baseUrl = (args.get("base-url") ?? process.env.YANDEX_AI_BASE_URL ?? "https://llm.api.cloud.yandex.net").replace(/\/$/, "");
const model = args.get("model") ?? process.env.YANDEX_MODEL ?? "yandexgpt-5-lite";
const timeoutMs = Number.parseInt(args.get("timeout-ms") ?? process.env.YANDEX_TIMEOUT_MS ?? "8000", 10);
const maxTokens = Number.parseInt(args.get("max-tokens") ?? process.env.YANDEX_MAX_TOKENS ?? "256", 10);
const text = args.get("text") ?? "Хочу забронировать столик завтра на 20:00 на двоих, лучше тихий у окна";

if (!folderId && !model.startsWith("gpt://")) {
  throw new Error("Set YANDEX_FOLDER_ID or pass a full gpt:// model URI");
}
if (!apiKey && !iamToken) {
  throw new Error("Set YANDEX_API_KEY or YANDEX_IAM_TOKEN");
}

const modelUri = model.startsWith("gpt://") ? model : `gpt://${folderId}/${model}`;
const prompt = `Верни только JSON без markdown. Определи intent и slots для FSM ресторана.
Intents: TABLE_BOOKING, CHANGE_CANCEL, MENU_ASSETS, MANAGER_HELP, UNKNOWN.
Slots: date, time, partySize, tableNumber, seatingPreference.
Сообщение гостя: ${text}
JSON schema: {"intent":"TABLE_BOOKING","confidence":0.0,"slots":{"date":"","time":"","partySize":"","tableNumber":"","seatingPreference":""},"missingSlots":[]}`;

const controller = new AbortController();
const timeout = setTimeout(() => controller.abort(), timeoutMs);
const startedAt = performance.now();

try {
  const response = await fetch(`${baseUrl}/foundationModels/v1/completion`, {
    method: "POST",
    signal: controller.signal,
    headers: {
      "Content-Type": "application/json",
      Authorization: apiKey ? `Api-Key ${apiKey}` : `Bearer ${iamToken}`,
    },
    body: JSON.stringify({
      modelUri,
      completionOptions: {
        stream: false,
        temperature: 0,
        maxTokens: String(maxTokens),
        reasoningOptions: { mode: "DISABLED" },
      },
      messages: [{ role: "user", text: prompt }],
      jsonObject: true,
    }),
  });
  const durationMs = Math.round(performance.now() - startedAt);
  const body = await response.json().catch(async () => ({ raw: await response.text() }));
  const result = body?.result ?? body;
  const resultText = result?.alternatives?.[0]?.message?.text ?? "";
  console.log(JSON.stringify({
    ok: response.ok,
    status: response.status,
    durationMs,
    modelUri,
    usage: result?.usage ?? null,
    text: resultText,
    error: response.ok ? null : body,
  }, null, 2));
} finally {
  clearTimeout(timeout);
}
