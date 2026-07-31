#!/usr/bin/env node

import { execFileSync, execSync } from "node:child_process";
import crypto from "node:crypto";

const args = new Map(
  process.argv.slice(2).map((arg) => {
    const [key, ...rest] = arg.replace(/^--/, "").split("=");
    return [key, rest.join("=") || "true"];
  }),
);

const baseUrl = (args.get("base-url") ?? process.env.BASE_URL ?? "http://localhost:8089").replace(/\/$/, "");
const mode = args.get("mode") ?? process.env.E2E_MODE ?? "both";
const text = args.get("text") ?? process.env.E2E_TEXT
  ?? "Хочу забронировать стол завтра в 20:00 на двоих и понять, какой формат C3 RИИLS подойдет ресторану";
const runId = args.get("run-id") ?? `butler-e2e-${new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14)}`;
const sessionId = args.get("session-id") ?? runId;
const chatId = Number.parseInt(args.get("chat-id") ?? process.env.E2E_CHAT_ID ?? String(7_770_000_000 + Math.floor(Math.random() * 100_000)), 10);
const timeoutMs = Number.parseInt(args.get("timeout-ms") ?? process.env.E2E_TIMEOUT_MS ?? "18000", 10);
const psqlCommand = args.get("psql-command") ?? process.env.PSQL_COMMAND;
const databaseUrl = args.get("db-url") ?? process.env.DATABASE_URL;

const folderId = args.get("folder-id") ?? process.env.YANDEX_FOLDER_ID;
const apiKey = args.get("api-key") ?? process.env.YANDEX_API_KEY;
const iamToken = args.get("iam-token") ?? process.env.YANDEX_IAM_TOKEN;
const yandexBaseUrl = (args.get("yandex-base-url") ?? process.env.YANDEX_AI_BASE_URL ?? "https://llm.api.cloud.yandex.net").replace(/\/$/, "");
const yandexModel = args.get("model") ?? process.env.YANDEX_MODEL ?? "yandexgpt-5-lite";
const yandexMaxTokens = Number.parseInt(args.get("max-tokens") ?? process.env.YANDEX_MAX_TOKENS ?? "256", 10);
const liteRubPer1k = Number.parseFloat(args.get("lite-rub-per-1k") ?? process.env.YANDEX_GPT_LITE_RUB_PER_1K_TOKENS ?? "0.20");
const qualityRubPer1k = Number.parseFloat(args.get("quality-rub-per-1k") ?? process.env.YANDEX_GPT_QUALITY_RUB_PER_1K_TOKENS ?? "0.80");

function fail(message, details) {
  const error = new Error(message);
  error.details = details;
  throw error;
}

function modelUri(model) {
  if (model.startsWith("gpt://")) return model;
  if (!folderId) fail("Set YANDEX_FOLDER_ID or pass a full gpt:// model URI");
  return `gpt://${folderId}/${model}`;
}

function safeJson(textValue) {
  try {
    return JSON.parse(textValue);
  } catch {
    const match = textValue.match(/\{[\s\S]*\}/);
    return match ? JSON.parse(match[0]) : null;
  }
}

function usageTokens(usage) {
  if (!usage || typeof usage !== "object") return 0;
  const total = Number(usage.totalTokens ?? usage.total_tokens ?? usage.total ?? 0);
  if (Number.isFinite(total) && total > 0) return total;
  const input = Number(usage.inputTextTokens ?? usage.inputTokens ?? usage.promptTokens ?? usage.prompt_tokens ?? 0);
  const output = Number(usage.completionTokens ?? usage.outputTextTokens ?? usage.outputTokens ?? usage.completion_tokens ?? 0);
  return (Number.isFinite(input) ? input : 0) + (Number.isFinite(output) ? output : 0);
}

function estimateRub(model, usage) {
  const total = usageTokens(usage);
  const rate = /5\.1|pro|quality/i.test(model) ? qualityRubPer1k : liteRubPer1k;
  return {
    totalTokens: total,
    rubPer1kTokens: rate,
    estimatedRub: Number(((total / 1000) * rate).toFixed(6)),
  };
}

async function postJson(path, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  const startedAt = performance.now();
  try {
    const response = await fetch(`${baseUrl}${path}`, {
      method: "POST",
      signal: controller.signal,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const payload = await response.json().catch(async () => ({ raw: await response.text() }));
    return {
      ok: response.ok,
      status: response.status,
      durationMs: Math.round(performance.now() - startedAt),
      payload,
    };
  } finally {
    clearTimeout(timeout);
  }
}

async function probeUnderstanding() {
  if (!apiKey && !iamToken) {
    return { skipped: true, reason: "YANDEX_API_KEY/YANDEX_IAM_TOKEN is not set in this shell" };
  }
  const uri = modelUri(yandexModel);
  const prompt = `Верни только JSON без markdown. Определи intent и slots для FSM ресторана и продаж C3AG.
Intents: TABLE_BOOKING, C3_PRODUCT_REQUEST, EVENT_BOOKING, MENU_ASSETS, MANAGER_HELP, UNKNOWN.
Slots: date, time, partySize, tableNumber, seatingPreference, product, budget, deadline.
Сообщение гостя: ${text}
JSON schema: {"intent":"TABLE_BOOKING","confidence":0.0,"slots":{"date":"","time":"","partySize":"","tableNumber":"","seatingPreference":"","product":"","budget":"","deadline":""},"missingSlots":[]}`;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  const startedAt = performance.now();
  try {
    const response = await fetch(`${yandexBaseUrl}/foundationModels/v1/completion`, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        Authorization: apiKey ? `Api-Key ${apiKey}` : `Bearer ${iamToken}`,
      },
      body: JSON.stringify({
        modelUri: uri,
        completionOptions: {
          stream: false,
          temperature: 0,
          maxTokens: String(yandexMaxTokens),
          reasoningOptions: { mode: "DISABLED" },
        },
        messages: [{ role: "user", text: prompt }],
        jsonObject: true,
      }),
    });
    const body = await response.json().catch(async () => ({ raw: await response.text() }));
    const result = body?.result ?? body;
    const resultText = result?.alternatives?.[0]?.message?.text ?? "";
    const usage = result?.usage ?? null;
    return {
      ok: response.ok,
      status: response.status,
      durationMs: Math.round(performance.now() - startedAt),
      modelUri: uri,
      intent: safeJson(resultText),
      usage,
      cost: estimateRub(uri, usage),
      rawText: resultText,
      error: response.ok ? null : body,
    };
  } finally {
    clearTimeout(timeout);
  }
}

function runSql(sql) {
  if (psqlCommand) {
    return execSync(`${psqlCommand} -AtX -c ${JSON.stringify(sql)}`, {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    }).trim();
  }
  if (databaseUrl) {
    return execFileSync("psql", [databaseUrl, "-AtX", "-c", sql], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    }).trim();
  }
  return null;
}

function dbCheck(correlationIds) {
  if (!psqlCommand && !databaseUrl) {
    return { skipped: true, reason: "Set DATABASE_URL or PSQL_COMMAND for database assertions" };
  }
  const ids = correlationIds.map((id) => `'${id.replaceAll("'", "''")}'`).join(",");
  const sql = `
WITH audits AS (
  SELECT jsonb_agg(jsonb_build_object(
    'correlationId', correlation_id,
    'purpose', purpose,
    'provider', provider,
    'model', model,
    'success', success,
    'generated', generated,
    'fallbackUsed', fallback_used,
    'latencyMs', latency_ms,
    'usage', metadata->'usage',
    'responsePreview', left(coalesce(response_text, ''), 240)
  ) ORDER BY created_at DESC) AS rows
  FROM model_interaction_audit
  WHERE correlation_id IN (${ids})
),
telegram AS (
  SELECT count(*) AS count
  FROM telegram_messages
  WHERE event_id IN (${ids}) OR raw_payload::text LIKE '%' || (SELECT split_part('${correlationIds.at(-1)}', '-', 1)) || '%'
),
web AS (
  SELECT jsonb_agg(jsonb_build_object(
    'correlationId', correlation_id,
    'direction', direction,
    'textPreview', left(coalesce(text, ''), 160)
  ) ORDER BY created_at) AS rows
  FROM web_messages
  WHERE correlation_id IN (${ids})
)
SELECT jsonb_build_object(
  'modelAudit', coalesce((SELECT rows FROM audits), '[]'::jsonb),
  'telegramMessageCount', (SELECT count FROM telegram),
  'webMessages', coalesce((SELECT rows FROM web), '[]'::jsonb)
)::text;
`;
  const raw = runSql(sql);
  return raw ? JSON.parse(raw) : { skipped: true, reason: "SQL command returned no output" };
}

function auditCost(db) {
  const rows = db?.modelAudit ?? [];
  return rows.map((row) => ({
    correlationId: row.correlationId,
    purpose: row.purpose,
    model: row.model,
    success: row.success,
    generated: row.generated,
    fallbackUsed: row.fallbackUsed,
    usage: row.usage,
    cost: estimateRub(row.model ?? "", row.usage),
  }));
}

const webCorrelationId = `${runId}-web`;
const telegramContactCorrelationId = `${runId}-telegram-contact`;
const telegramCorrelationId = `${runId}-telegram`;
const telegramBootstrap = (args.get("telegram-bootstrap") ?? process.env.E2E_TELEGRAM_BOOTSTRAP ?? "true") !== "false";
const result = {
  runId,
  baseUrl,
  text,
  checks: {},
};

if (mode === "both" || mode === "yandex") {
  result.checks.understandingProbe = await probeUnderstanding();
  if (result.checks.understandingProbe.ok === false) {
    fail("Yandex understanding probe failed", result.checks.understandingProbe);
  }
}

if (mode === "both" || mode === "web") {
  const web = await postJson("/api/messages", {
    channel: "WEB",
    externalUserId: `web:anon:${sessionId}`,
    text,
    correlationId: webCorrelationId,
    payload: {
      site: "c3ag",
      sessionId,
      page: "/",
      referrer: "e2e",
      userAgentHash: crypto.createHash("sha256").update(runId).digest("hex").slice(0, 16),
    },
  });
  result.checks.webMessage = web;
  if (!web.ok) fail("WEB /api/messages failed", web);
  if (!web.payload?.actions?.includes("WEB_LEAD_CAPTURED")) {
    fail("WEB response did not include WEB_LEAD_CAPTURED", web.payload);
  }
}

if (mode === "both" || mode === "telegram") {
  if (telegramBootstrap) {
    const contact = await postJson("/api/messages", {
      channel: "TELEGRAM",
      chatId,
      externalUserId: String(chatId),
      firstName: "E2E Butler",
      username: "e2e_butler_smoke",
      contactPhone: "+79000000000",
      text: "",
      correlationId: telegramContactCorrelationId,
      payload: {
        e2e: true,
        runId,
        bootstrap: "contact-consent",
      },
    });
    result.checks.telegramContact = contact;
    if (!contact.ok) fail("TELEGRAM contact bootstrap failed", contact);
  }

  const telegram = await postJson("/api/messages", {
    channel: "TELEGRAM",
    chatId,
    externalUserId: String(chatId),
    firstName: "E2E Butler",
    username: "e2e_butler_smoke",
    text,
    correlationId: telegramCorrelationId,
    payload: {
      e2e: true,
      runId,
      site: "c3ag",
    },
  });
  result.checks.telegramMessage = telegram;
  if (!telegram.ok) fail("TELEGRAM /api/messages failed", telegram);
  if (!telegram.payload?.text || !telegram.payload?.nextState) {
    fail("TELEGRAM response is missing text/nextState", telegram.payload);
  }
}

const db = dbCheck([webCorrelationId, telegramContactCorrelationId, telegramCorrelationId]);
result.checks.database = db;
result.checks.auditCost = auditCost(db);

console.log(JSON.stringify(result, null, 2));
