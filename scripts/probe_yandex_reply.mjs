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
const timeoutMs = Number.parseInt(args.get("timeout-ms") ?? process.env.YANDEX_TIMEOUT_MS ?? "10000", 10);
const maxTokens = Number.parseInt(args.get("max-tokens") ?? process.env.YANDEX_MAX_TOKENS ?? "320", 10);
const temperature = Number.parseFloat(args.get("temperature") ?? process.env.YANDEX_TEMPERATURE ?? "0.1");
const jsonObject = (args.get("json") ?? "false") === "true";
const prompt = args.get("prompt") ?? process.env.YANDEX_PROBE_PROMPT ?? `
Ты Astor, цифровой дворецкий ресторана AERIS.
Ответь гостю коротко: где находится AERIS, чем ты можешь помочь и что не подтверждаешь без команды.
`;

if (!folderId && !model.startsWith("gpt://")) {
  throw new Error("Set YANDEX_FOLDER_ID or pass --model=gpt://...");
}
if (!apiKey && !iamToken) {
  throw new Error("Set YANDEX_API_KEY or YANDEX_IAM_TOKEN");
}

const modelUri = model.startsWith("gpt://") ? model : `gpt://${folderId}/${model}`;
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
        temperature,
        maxTokens: String(maxTokens),
        reasoningOptions: { mode: "DISABLED" },
      },
      messages: [{ role: "user", text: prompt.trim() }],
      ...(jsonObject ? { jsonObject: true } : {}),
    }),
  });

  const durationMs = Math.round(performance.now() - startedAt);
  const body = await response.json().catch(async () => ({ raw: await response.text() }));
  const result = body?.result ?? body;
  const text = result?.alternatives?.[0]?.message?.text ?? "";
  const usage = result?.usage ?? null;

  console.log(JSON.stringify({
    ok: response.ok,
    status: response.status,
    durationMs,
    modelUri,
    usage,
    text,
    error: response.ok ? null : body,
  }, null, 2));

  if (!response.ok || !text.trim()) {
    process.exitCode = 1;
  }
} finally {
  clearTimeout(timeout);
}
