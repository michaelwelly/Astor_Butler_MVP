import { NextRequest, NextResponse } from "next/server";

type SpeakResponse = {
  provider: "test-double" | "yandex-speechkit";
  status: "READY" | "UNAVAILABLE";
  voice: string;
  audioUrl: string | null;
  message?: string;
  createdAt: string;
};

export async function POST(req: NextRequest) {
  const testDoubleEnabled = process.env.CLIO_TTS_TEST_DOUBLE_ENABLED === "true";
  const body = (await req.json().catch(() => ({}))) as { text?: string };

  if (!testDoubleEnabled) {
    return NextResponse.json<SpeakResponse>(
      {
        provider: "yandex-speechkit",
        status: "UNAVAILABLE",
        voice: "built-in feminine Yandex SpeechKit voice, exact option TBD",
        audioUrl: null,
        message:
          "SpeechKit TTS is not configured. Cloud credentials must stay on the server; no paid calls are made by this mock.",
        createdAt: new Date().toISOString(),
      },
      { status: 503 },
    );
  }

  return NextResponse.json<SpeakResponse>({
    provider: "test-double",
    status: "READY",
    voice: "built-in feminine Yandex SpeechKit voice, test-double",
    audioUrl: null,
    message: body.text ? "TTS test double accepted text; no audio file was generated." : "No text provided.",
    createdAt: new Date().toISOString(),
  });
}
