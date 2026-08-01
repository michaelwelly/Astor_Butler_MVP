import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";

const sourcePath = path.resolve("lib/clio-voice.ts");
const source = fs.readFileSync(sourcePath, "utf8");
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
});

const cjsModule = { exports: {} };
const sandbox = { exports: cjsModule.exports, module: cjsModule, process };
vm.runInNewContext(compiled.outputText, sandbox, { filename: sourcePath });
const voice = cjsModule.exports;

const sameJson = (actual, expected) => assert.equal(JSON.stringify(actual), JSON.stringify(expected));

sameJson(
  voice.browserVoiceAvailability({
    secureContext: false,
    hasMediaDevices: true,
    hasMediaRecorder: true,
  }),
  { ok: false, code: "insecure_context" },
);

sameJson(
  voice.browserVoiceAvailability({
    secureContext: true,
    hasMediaDevices: true,
    hasMediaRecorder: true,
  }),
  { ok: true },
);

assert.match(
  voice.CLIO_VOICE_STATUS_COPY.permission_denied,
  /Доступ к микрофону отклонён/,
);

sameJson(voice.clioSttUnavailableResult(), {
  ok: false,
  provider: "yandex-speechkit",
  status: "UNAVAILABLE",
  code: "STT_NOT_CONFIGURED",
  message:
    "SpeechKit STT is not configured. Keep credentials server-side and enable only after HTTPS and billing are verified.",
});

sameJson(voice.clioTranscribeTestDouble({ scenario: "empty" }), {
  ok: false,
  provider: "test-double",
  status: "FAILED",
  code: "EMPTY_TRANSCRIPT",
  message: "Audio was accepted by the test double, but no text was recognized.",
});

sameJson(voice.clioTranscribeTestDouble({ transcript: " Нужна реклама " }), {
  ok: true,
  provider: "test-double",
  status: "TRANSCRIBED",
  text: "Нужна реклама",
  confidence: 0.99,
});

console.log("clio voice scenarios ok");
