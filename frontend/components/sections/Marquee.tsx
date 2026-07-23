"use client";

// Editorial ticker. Words alternate solid / outline serif; the track is
// duplicated once so the -50% keyframe loops seamlessly. Pauses on hover and
// for reduced-motion (CSS).
const WORDS = ["Ивенты", "Рилсы", "Реклама", "Подкасты", "Кинематограф", "Продакшн"];

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
