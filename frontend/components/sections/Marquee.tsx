"use client";

// Editorial ticker. Words alternate solid / outline serif; the track is
// duplicated once so the -50% keyframe loops seamlessly. Pauses on hover and
// for reduced-motion (CSS).
const WORDS = [
  "C3 RИИLS",
  "C3 REПОРТАЖ",
  "C3 ВЭDDING",
  "C3 PODКАСТ",
  "C3 RECLAMA",
  "C3 ФILM",
  "C3 ЫI",
  "Smart Solutions",
];

export function Marquee() {
  const items = [...WORDS, ...WORDS];
  return (
    <div className="marquee" aria-hidden="true">
      <div className="marquee-track">
        {items.map((word, i) => (
          <span className="marquee-item" key={i}>
            {word}
            <span className="marquee-dot" />
          </span>
        ))}
      </div>
    </div>
  );
}
