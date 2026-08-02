#!/usr/bin/env node

import { execFileSync, execSync } from "node:child_process";

const args = new Map(
  process.argv.slice(2).map((arg) => {
    const [key, ...rest] = arg.replace(/^--/, "").split("=");
    return [key, rest.join("=") || "true"];
  }),
);

const baseUrl = (args.get("base-url") ?? process.env.BASE_URL ?? "http://localhost:8089").replace(/\/$/, "");
const timeoutMs = Number.parseInt(args.get("timeout-ms") ?? process.env.SMOKE_TIMEOUT_MS ?? "30000", 10);
const runId = args.get("run-id") ?? `logging-smoke-${new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14)}`;
const channel = (args.get("channel") ?? "TELEGRAM").toUpperCase();
const chatId = Number.parseInt(args.get("chat-id") ?? process.env.SMOKE_CHAT_ID ?? String(7_790_000_000 + Math.floor(Math.random() * 100_000)), 10);
const text = args.get("text") ?? process.env.SMOKE_TEXT ?? "Расскажи коротко про AERIS, винную карту и где находится ресторан";
const psqlCommand = args.get("psql-command") ?? process.env.PSQL_COMMAND;
const databaseUrl = args.get("db-url") ?? process.env.DATABASE_URL;
const expectModelAudit = (args.get("expect-model-audit") ?? "true") !== "false";

async function postMessage(correlationId, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  const startedAt = performance.now();
  try {
    const response = await fetch(`${baseUrl}/api/messages`, {
      method: "POST",
      signal: controller.signal,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...body, correlationId }),
    });
    const payload = await response.json().catch(async () => ({ raw: await response.text() }));
    return {
      ok: response.ok,
      status: response.status,
      durationMs: Math.round(performance.now() - startedAt),
      correlationId,
      payload,
    };
  } finally {
    clearTimeout(timeout);
  }
}

function sqlQuote(value) {
  return `'${String(value).replaceAll("'", "''")}'`;
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
  return "";
}

function dbEvidence(correlationIds) {
  if (!psqlCommand && !databaseUrl) {
    return { skipped: true, reason: "Set DATABASE_URL or PSQL_COMMAND to verify persisted logs" };
  }
  const ids = correlationIds.map(sqlQuote).join(",");
  const sql = `
SELECT jsonb_build_object(
  'modelAuditCount', (
    SELECT count(*) FROM model_interaction_audit WHERE correlation_id IN (${ids})
  ),
  'modelAuditRows', (
    SELECT coalesce(jsonb_agg(jsonb_build_object(
      'correlationId', correlation_id,
      'scenario', scenario,
      'state', state,
      'purpose', purpose,
      'provider', provider,
      'model', model,
      'generated', generated,
      'fallbackUsed', fallback_used,
      'success', success,
      'latencyMs', latency_ms,
      'responsePreview', left(coalesce(response_text, ''), 220)
    ) ORDER BY created_at DESC), '[]'::jsonb)
    FROM model_interaction_audit
    WHERE correlation_id IN (${ids})
  ),
  'telegramMessageCount', (
    SELECT count(*) FROM telegram_messages WHERE event_id IN (${ids})
  ),
  'webMessageCount', (
    SELECT count(*) FROM web_messages WHERE correlation_id IN (${ids})
  )
)::text;
`;
  const raw = runSql(sql);
  return raw ? JSON.parse(raw) : { skipped: true, reason: "SQL returned no output" };
}

const correlationIds = [];
const checks = [];

if (channel === "TELEGRAM") {
  const consentId = `${runId}-consent`;
  correlationIds.push(consentId);
  checks.push(await postMessage(consentId, {
    channel,
    chatId,
    externalUserId: String(chatId),
    firstName: "Logging Smoke",
    username: "logging_smoke",
    contactPhone: "+79000000000",
    text: "consent logging smoke",
    payload: { smoke: true, runId, bootstrap: "contact-consent" },
  }));
}

const messageId = `${runId}-message`;
correlationIds.push(messageId);
checks.push(await postMessage(messageId, channel === "WEB" ? {
  channel,
  externalUserId: `web:${runId}`,
  text,
  payload: {
    site: "c3ag",
    sessionId: runId,
    page: "/",
    smoke: true,
  },
} : {
  channel,
  chatId,
  externalUserId: String(chatId),
  firstName: "Logging Smoke",
  username: "logging_smoke",
  text,
  payload: { smoke: true, runId },
}));

const failedHttp = checks.filter((check) => !check.ok);
const database = dbEvidence(correlationIds);
const modelAuditCount = Number(database?.modelAuditCount ?? 0);

const result = {
  ok: failedHttp.length === 0 && (!expectModelAudit || database.skipped || modelAuditCount > 0),
  runId,
  baseUrl,
  channel,
  correlationIds,
  checks: checks.map((check) => ({
    ok: check.ok,
    status: check.status,
    durationMs: check.durationMs,
    correlationId: check.correlationId,
    nextState: check.payload?.nextState,
    actions: check.payload?.actions,
    textPreview: String(check.payload?.text ?? check.payload?.raw ?? "").slice(0, 260),
  })),
  database,
};

console.log(JSON.stringify(result, null, 2));

if (!result.ok) {
  process.exitCode = 1;
}
