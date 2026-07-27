"use client";

/**
 * C3FLEX.com set as live type rather than an image, so the site's signature
 * motion — a slow luminance wave travelling left to right through the letters
 * — can run through it. The wave is pure opacity: it inherits whatever colour
 * the context sets, which is why the same component works on the dark header
 * and on the splash screen without a variant.
 *
 * Per-letter <span>s carry `--i`; CSS turns that index into an animation
 * delay, so there is no JS driving the loop (see `.wm-ch` in globals.css).
 */

type Segment = { text: string; wrap?: "span" | "sup" };

const SEGMENTS: Segment[] = [
  { text: "C3" },
  { text: "FLEX", wrap: "span" },
  { text: ".com", wrap: "sup" },
];

export function Wordmark({ className = "" }: { className?: string }) {
  // One running index across the whole mark so the wave crosses segment
  // boundaries as a single sweep instead of restarting per segment.
  let i = 0;
  const letters = (text: string) =>
    [...text].map((ch) => (
      <span
        key={`${ch}-${i}`}
        className="wm-ch"
        style={{ "--i": i++ } as React.CSSProperties}
      >
        {ch}
      </span>
    ));

  return (
    // The mark reads as one word to assistive tech; the per-letter spans are
    // presentational only.
    <span className={`wordmark ${className}`.trim()} role="img" aria-label="C3FLEX.com">
      <span aria-hidden="true">
        {SEGMENTS.map(({ text, wrap }) => {
          const kids = letters(text);
          if (wrap === "span") return <span key={text} className="wm-flex">{kids}</span>;
          if (wrap === "sup") return <sup key={text} className="wm-tld">{kids}</sup>;
          return kids;
        })}
      </span>
    </span>
  );
}
