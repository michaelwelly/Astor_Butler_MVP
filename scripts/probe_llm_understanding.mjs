#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 1) {
  const arg = process.argv[index];
  if (arg.startsWith("--")) {
    const [key, inlineValue] = arg.slice(2).split("=", 2);
    const value = inlineValue ?? process.argv[index + 1];
    args.set(key, value);
    if (inlineValue === undefined) {
      index += 1;
    }
  }
}

const root = process.cwd();
const corpusPath = args.get("corpus")
  ?? path.join(root, "src/test/resources/understanding/table-booking-target-corpus.jsonl");
const baseUrl = (args.get("base-url") ?? process.env.LLM_OLLAMA_BASE_URL ?? "http://localhost:11434").replace(/\/$/, "");
const model = args.get("model") ?? process.env.LLM_OLLAMA_MODEL ?? "qwen2.5:3b";
const limit = Number.parseInt(args.get("limit") ?? "30", 10);
const timeoutMs = Number.parseInt(args.get("timeout-ms") ?? "45000", 10);

function prompt(testCase) {
  return `
Верни только JSON. Ты классифицируешь сообщение гостя ресторана.
Не подтверждай бронь и не делай бизнес-действий.
Allowed intent: TABLE_BOOKING, CHANGE_CANCEL, PROVIDE_DATE, PROVIDE_TIME, PROVIDE_PARTY_SIZE, PROVIDE_TABLE_SELECTION, PROVIDE_SEATING_PREFERENCE, UNKNOWN.
Allowed slots: date, time, partySize, tableNumber, seatingPreference.
State: ${testCase.state}
Text: ${testCase.text}
JSON schema:
{"intent":"TABLE_BOOKING","confidence":0.0,"slots":{"date":"","time":"","partySize":"","tableNumber":"","seatingPreference":""},"missingSlots":[],"replyDraft":""}
`.trim();
}

function extractJson(text) {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) {
    throw new Error(`No JSON object found in response: ${text.slice(0, 200)}`);
  }
  return JSON.parse(text.slice(start, end + 1));
}

async function askLlm(testCase) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  const response = await fetch(`${baseUrl}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    signal: controller.signal,
    body: JSON.stringify({
      model,
      stream: false,
      format: "json",
      options: {
        temperature: 0,
        num_ctx: 2048,
        num_predict: 180
      },
      messages: [
        {
          role: "user",
          content: prompt(testCase)
        }
      ]
    })
  }).finally(() => clearTimeout(timeout));
  if (!response.ok) {
    throw new Error(`LLM HTTP ${response.status}: ${await response.text()}`);
  }
  const body = await response.json();
  return extractJson(body.message?.content ?? body.response ?? "");
}

function slotKeys(value) {
  return Object.entries(value ?? {})
    .filter(([, slotValue]) => slotValue !== null && String(slotValue).trim() !== "")
    .map(([key]) => key)
    .sort();
}

function expectedSlotKeys(testCase) {
  return Object.keys(testCase.slots ?? {}).sort();
}

function evaluate(testCase, actual) {
  const expectedIntent = testCase.intent;
  const actualIntent = actual.intent ?? "UNKNOWN";
  const actualSlots = new Set(slotKeys(actual.slots));
  const missingExpectedSlots = expectedSlotKeys(testCase)
    .filter((key) => !actualSlots.has(key));
  return {
    intentOk: expectedIntent === actualIntent,
    slotsOk: missingExpectedSlots.length === 0,
    missingExpectedSlots,
    actualIntent,
    actualSlots: [...actualSlots]
  };
}

const lines = fs.readFileSync(corpusPath, "utf8")
  .split(/\r?\n/)
  .filter((line) => line.trim().length > 0)
  .slice(0, Number.isFinite(limit) ? limit : 30);

console.log(`LLM understanding probe`);
console.log(`baseUrl=${baseUrl}`);
console.log(`model=${model}`);
console.log(`corpus=${corpusPath}`);
console.log(`cases=${lines.length}`);
console.log("");

let intentOk = 0;
let slotsOk = 0;

for (const line of lines) {
  const testCase = JSON.parse(line);
  const started = Date.now();
  try {
    const actual = await askLlm(testCase);
    const result = evaluate(testCase, actual);
    if (result.intentOk) {
      intentOk += 1;
    }
    if (result.slotsOk) {
      slotsOk += 1;
    }
    const elapsed = Date.now() - started;
    console.log([
      testCase.id,
      testCase.status,
      `${result.intentOk ? "intent:ok" : `intent:${result.actualIntent}`}`,
      `${result.slotsOk ? "slots:ok" : `missing:${result.missingExpectedSlots.join(",")}`}`,
      `confidence:${actual.confidence ?? ""}`,
      `ms:${elapsed}`,
      testCase.text
    ].join(" | "));
  } catch (error) {
    const elapsed = Date.now() - started;
    console.log(`${testCase.id} | ERROR | ms:${elapsed} | ${error.message}`);
  }
}

console.log("");
console.log(`summary intentOk=${intentOk}/${lines.length} slotsOk=${slotsOk}/${lines.length}`);
