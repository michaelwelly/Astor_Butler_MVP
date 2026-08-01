import { NextRequest, NextResponse } from "next/server";
import { clioSttUnavailableResult, clioTranscribeTestDouble } from "@/lib/clio-voice";

type TranscribeResponse = {
  text?: string;
  confidence?: number;
  provider: "test-double" | "yandex-speechkit";
  status: "TRANSCRIBED" | "UNAVAILABLE" | "FAILED";
  code?: string;
  message?: string;
  createdAt: string;
};

export async function POST(req: NextRequest) {
  const testDoubleEnabled = process.env.CLIO_VOICE_TEST_DOUBLE_ENABLED === "true";
  if (!testDoubleEnabled) {
    const result = clioSttUnavailableResult();
    return NextResponse.json<TranscribeResponse>(
      {
        provider: result.provider,
        status: result.status,
        code: result.code,
        message: result.message,
        createdAt: new Date().toISOString(),
      },
      { status: 503 },
    );
  }

  const form = await req.formData();
  const result = clioTranscribeTestDouble({
    scenario: req.headers.get("X-Clio-Test-Scenario"),
    transcript:
      req.headers.get("X-Clio-Test-Transcript") ||
      String(form.get("mockTranscript") || ""),
  });
  if (!result.ok) {
    return NextResponse.json<TranscribeResponse>(
      {
        provider: result.provider,
        status: result.status,
        code: result.code,
        message: result.message,
        createdAt: new Date().toISOString(),
      },
      { status: 422 },
    );
  }

  return NextResponse.json<TranscribeResponse>({
    provider: result.provider,
    status: result.status,
    text: result.text,
    confidence: result.confidence,
    createdAt: new Date().toISOString(),
  });
}
