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
const baseUrl = (args.get("base-url") ?? process.env.YANDEX_RESPONSES_BASE_URL ?? "https://ai.api.cloud.yandex.net/v1").replace(/\/$/, "");
const agentId = args.get("agent-id") ?? process.env.YANDEX_AGENT_ID ?? "fvt18kmmnas336paia3g";
const model = args.get("model") ?? process.env.YANDEX_AGENT_MODEL ?? process.env.YANDEX_MODEL ?? "yandexgpt-5-lite";
const timeoutMs = Number.parseInt(args.get("timeout-ms") ?? process.env.YANDEX_TIMEOUT_MS ?? "10000", 10);
const maxOutputTokens = Number.parseInt(args.get("max-output-tokens") ?? args.get("max-tokens") ?? process.env.YANDEX_MAX_TOKENS ?? "256", 10);
const temperature = Number.parseFloat(args.get("temperature") ?? process.env.YANDEX_TEMPERATURE ?? "0.1");
const prompt = args.get("prompt") ?? process.env.YANDEX_AGENT_PROBE_PROMPT ?? `
Ты Astor Butler, цифровой дворецкий ресторана AERIS.
Ответь гостю коротко: чем ты можешь помочь и что подтверждаешь только через команду ресторана.
`;

if (!folderId && !model.startsWith("gpt://")) {
  throw new Error("Set YANDEX_FOLDER_ID or pass --model=gpt://...");
}
if (!apiKey && !iamToken) {
  throw new Error("Set YANDEX_API_KEY or YANDEX_IAM_TOKEN");
}
if (!agentId) {
  throw new Error("Set YANDEX_AGENT_ID");
}

const modelUri = model.startsWith("gpt://") ? model : `gpt://${folderId}/${model}`;
const controller = new AbortController();
const timeout = setTimeout(() => controller.abort(), timeoutMs);
const startedAt = performance.now();

try {
  const response = await fetch(`${baseUrl}/responses`, {
    method: "POST",
    signal: controller.signal,
    headers: {
      "Content-Type": "application/json",
      Authorization: apiKey ? `Api-Key ${apiKey}` : `Bearer ${iamToken}`,
      ...(folderId ? { "OpenAI-Project": folderId } : {}),
    },
    body: JSON.stringify({
      model: modelUri,
      prompt: { id: agentId },
      input: prompt.trim(),
      max_output_tokens: maxOutputTokens,
      temperature,
    }),
  });

  const durationMs = Math.round(performance.now() - startedAt);
  const body = await response.json().catch(async () => ({ raw: await response.text() }));
  const text = body?.output_text
    ?? body?.output?.flatMap((item) => item?.content ?? [])
      .map((content) => content?.text)
      .filter(Boolean)
      .join("\n")
    ?? "";

  console.log(JSON.stringify({
    ok: response.ok,
    status: response.status,
    durationMs,
    agentId,
    modelUri,
    responseId: body?.id ?? null,
    responseStatus: body?.status ?? null,
    incompleteDetails: body?.incomplete_details ?? null,
    usage: body?.usage ?? null,
    text,
    error: response.ok ? null : body,
  }, null, 2));

  if (!response.ok || !String(text).trim()) {
    process.exitCode = 1;
  }
} finally {
  clearTimeout(timeout);
}
