#!/usr/bin/env python3
import os
import sys

from faster_whisper import WhisperModel


def main() -> int:
    if len(sys.argv) < 2:
        print("audio file path is required", file=sys.stderr)
        return 2

    audio_file = sys.argv[1]
    model_name = os.getenv("ASTOR_STT_MODEL", "base")
    device = os.getenv("ASTOR_STT_DEVICE", "cpu")
    compute_type = os.getenv("ASTOR_STT_COMPUTE_TYPE", "int8")
    language = os.getenv("ASTOR_STT_LANGUAGE", "ru")

    model = WhisperModel(model_name, device=device, compute_type=compute_type)
    segments, _ = model.transcribe(
        audio_file,
        language=language,
        vad_filter=True,
        beam_size=1,
        best_of=1,
        condition_on_previous_text=False,
    )
    text = " ".join(segment.text.strip() for segment in segments).strip()
    if not text:
        return 1
    print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
