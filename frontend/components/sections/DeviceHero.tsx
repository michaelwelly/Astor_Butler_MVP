"use client";

import { useEffect, useRef } from "react";
import {
  motion,
  useMotionValue,
  useScroll,
  useSpring,
  useTransform,
  useReducedMotion,
  type Variants,
} from "framer-motion";
import { ArrowDown, ArrowUpRight } from "lucide-react";
import { catalogVideos, POSTER_FALLBACK, type CatalogVideo } from "@/lib/video-catalog";
import { HERO_CLIPS, type HeroClip } from "@/lib/hero-clips";
import { resolveMediaRef } from "@/lib/media-ref";
import { Magnetic } from "@/components/ui/Magnetic";
import { CyclingLine } from "@/components/ui/CyclingLine";

const EASE: [number, number, number, number] = [0.4, 0, 0.2, 1];

/**
 * The endings the headline cycles through. All four finish the same opening
 * ("Ваша история —") and name a different part of the studio's actual range,
 * which is also what the gadget field behind them is showing. Kept to a
 * similar length so the reserved width stays tight.
 */
const HEADLINE_ENDINGS = [
  "на каждом экране.",
  "в каждом формате.",
  "в любом масштабе.",
  "от рилса до зала.",
];

/** What the gadgets actually play — the manifest and the catalog both map to this. */
type Clip = { src: string; poster: string };

function fromManifest(c: HeroClip): Clip {
  return {
    src: resolveMediaRef(c.src) ?? c.src,
    poster: resolveMediaRef(c.poster) ?? POSTER_FALLBACK,
  };
}
/** Best playable source from a catalog item (absolute URL first). */
function fromCatalog(v: CatalogVideo): Clip | null {
  const src =
    v.sources.find((s) => /^https?:\/\//i.test(s.publicUrl))?.publicUrl ??
    v.sources[0]?.publicUrl;
  return src ? { src, poster: v.poster.publicUrl } : null;
}

// Two pools, cycled so no two neighbouring gadgets show the same cut. Each pool
// is the manifest if you've filled it, else the sample catalog — so the field
// works before AND after real clips land, from the single lib/hero-clips.ts file.
function buildPool(manifest: HeroClip[], orientation: "portrait" | "landscape"): Clip[] {
  if (manifest.length) return manifest.map(fromManifest);
  return catalogVideos
    .filter((v) => v.orientation === orientation)
    .map(fromCatalog)
    .filter((c): c is Clip => c !== null);
}
const PORTRAIT = buildPool(HERO_CLIPS.portrait, "portrait");
const LANDSCAPE = buildPool(HERO_CLIPS.landscape, "landscape");

type DeviceType = "phone" | "tablet" | "laptop" | "monitor" | "tv" | "watch";
const LANDSCAPE_TYPES = new Set<DeviceType>(["laptop", "monitor", "tv"]);

type Depth = "back" | "mid" | "front";

type Device = {
  type: DeviceType;
  depth: Depth;
  x: number; // % of field, gadget centre
  y: number;
  scale: number; // size multiplier on top of the type's base width
  dx: number; // drift ellipse radii, container units (cqw / cqh)
  dy: number;
  dz: number; // depth swing, cqw
  dr: number; // roll at the extremes, deg
  dur: number; // seconds per drift loop
  delay: number; // negative → starts mid-loop
  reverse?: boolean;
  mobile?: boolean; // also show on phone-width screens
};

/**
 * Art-directed scatter (deterministic — no Math.random, so SSR and client agree).
 * Front gadgets ring the frame big and sharp; back gadgets fill the gaps small,
 * dim and blurred, drifting behind the headline — "screens everywhere, receding
 * into depth". Each drifts around its own point, not a shared orbit. `mobile`
 * marks the subset that stays on phone width, chosen to cover every region
 * (incl. bottom-right) so the narrow layout never goes empty. Denser than v1 and
 * with tighter radii, so the field reads as full rather than sparsely floating.
 */
const DEVICES: Device[] = [
  // ── Front: large, sharp, foreground ──────────────────────────────────────
  { type: "laptop",  depth: "front", x: 74, y: 32, scale: 1.06, dx: 3.5, dy: 5, dz: 4, dr: 3,   dur: 26, delay: -2 },
  { type: "phone",   depth: "front", x: 12, y: 24, scale: 1.0,  dx: 4.5, dy: 6, dz: 6, dr: 8,   dur: 22, delay: -9,  reverse: true, mobile: true },
  { type: "monitor", depth: "front", x: 89, y: 66, scale: 1.0,  dx: 3,   dy: 4, dz: 3, dr: 2,   dur: 30, delay: -15 },
  { type: "tablet",  depth: "front", x: 10, y: 70, scale: 1.0,  dx: 4,   dy: 5, dz: 5, dr: -6,  dur: 28, delay: -5,  mobile: true },
  { type: "watch",   depth: "front", x: 33, y: 86, scale: 1.0,  dx: 5,   dy: 7, dz: 3, dr: 11,  dur: 18, delay: -11, mobile: true },
  { type: "phone",   depth: "front", x: 86, y: 84, scale: 1.0,  dx: 4,   dy: 6, dz: 5, dr: -9,  dur: 24, delay: -3,  reverse: true, mobile: true }, // R-B: mobile fill
  { type: "tv",      depth: "front", x: 55, y: 85, scale: 0.95, dx: 3,   dy: 4, dz: 4, dr: 3,   dur: 32, delay: -20 },
  // ── Mid: medium, faint blur ──────────────────────────────────────────────
  { type: "tv",      depth: "mid",   x: 38, y: 15, scale: 1.0,  dx: 3,   dy: 4, dz: 4, dr: 3,   dur: 34, delay: -18 },
  { type: "phone",   depth: "mid",   x: 64, y: 60, scale: 1.0,  dx: 4,   dy: 5, dz: 5, dr: -9,  dur: 24, delay: -3,  mobile: true },
  { type: "tablet",  depth: "mid",   x: 90, y: 30, scale: 1.0,  dx: 3.5, dy: 4, dz: 5, dr: 6,   dur: 30, delay: -12, mobile: true },
  { type: "phone",   depth: "mid",   x: 26, y: 50, scale: 1.0,  dx: 4,   dy: 5, dz: 4, dr: 7,   dur: 25, delay: -7,  reverse: true, mobile: true },
  { type: "monitor", depth: "mid",   x: 60, y: 40, scale: 0.95, dx: 3,   dy: 4, dz: 4, dr: -3,  dur: 34, delay: -1 },
  // ── Back: small, blurred, dim — poster stills, drift-fill the gaps ────────
  { type: "phone",   depth: "back",  x: 49, y: 22, scale: 1.0,  dx: 3,   dy: 5, dz: 6, dr: -7,  dur: 27, delay: -13, reverse: true, mobile: true },
  { type: "tablet",  depth: "back",  x: 72, y: 16, scale: 1.0,  dx: 3,   dy: 4, dz: 6, dr: 6,   dur: 34, delay: -7 },
  { type: "phone",   depth: "back",  x: 92, y: 48, scale: 1.0,  dx: 4,   dy: 5, dz: 5, dr: 8,   dur: 25, delay: -20, mobile: true },
  { type: "phone",   depth: "back",  x: 20, y: 40, scale: 1.0,  dx: 3,   dy: 4, dz: 6, dr: -7,  dur: 27, delay: -13, mobile: true },
  { type: "watch",   depth: "back",  x: 47, y: 58, scale: 1.0,  dx: 4,   dy: 6, dz: 4, dr: 9,   dur: 20, delay: -4,  mobile: true },
  { type: "phone",   depth: "back",  x: 52, y: 80, scale: 1.0,  dx: 3,   dy: 4, dz: 5, dr: 6,   dur: 26, delay: -9,  reverse: true, mobile: true }, // B-M: mobile fill
];

/**
 * Screen contents. `still` swaps the <video> for a poster background — used on the
 * blurred, dimmed back tier where a frozen frame is indistinguishable from video,
 * so gadget count can grow without the concurrent-decode cost scaling with it.
 */
function Screen({ clip, still }: { clip: Clip; still: boolean }) {
  if (still) {
    return <div className="dv-still" style={{ backgroundImage: `url(${clip.poster})` }} />;
  }
  return (
    <video autoPlay loop muted playsInline poster={clip.poster} aria-hidden="true">
      <source src={clip.src} type="video/mp4" />
    </video>
  );
}

/** The physical frame for each gadget type; all wrap the same <Screen>. */
function Frame({ type, clip, still }: { type: DeviceType; clip: Clip; still: boolean }) {
  const screen = (
    <div className="dv-screen">
      <Screen clip={clip} still={still} />
    </div>
  );
  switch (type) {
    case "phone":
      return (
        <div className="dv-phone">
          <span className="dv-phone-notch" />
          {screen}
        </div>
      );
    case "tablet":
      return <div className="dv-tablet">{screen}</div>;
    case "laptop":
      return (
        <div className="dv-laptop">
          <div className="dv-laptop-lid">{screen}</div>
          <div className="dv-laptop-base" />
        </div>
      );
    case "monitor":
      return (
        <div className="dv-monitor">
          <div className="dv-monitor-lid">{screen}</div>
          <div className="dv-monitor-neck" />
          <div className="dv-monitor-foot" />
        </div>
      );
    case "tv":
      return <div className="dv-tv">{screen}</div>;
    case "watch":
      return <div className="dv-watch">{screen}</div>;
  }
}

export function DeviceHero() {
  const reduce = useReducedMotion();
  const sectionRef = useRef<HTMLElement>(null);

  // The field is ~11 concurrent decodes. Once the hero has scrolled away they
  // are invisible but still burning CPU, and the catalog below now autoplays
  // its own cards — so hand the budget over instead of stacking the two.
  useEffect(() => {
    const section = sectionRef.current;
    if (!section) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        section.querySelectorAll("video").forEach((v) => {
          if (entry.isIntersecting) void v.play().catch(() => {});
          else v.pause();
        });
      },
      { threshold: 0.05 },
    );
    observer.observe(section);
    return () => observer.disconnect();
  }, []);

  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ["start start", "end start"],
  });
  const fieldY = useTransform(scrollYProgress, [0, 1], [0, -70]);
  const fieldScale = useTransform(scrollYProgress, [0, 1], [1, 1.08]);
  const copyY = useTransform(scrollYProgress, [0, 1], [0, -40]);
  const copyOpacity = useTransform(scrollYProgress, [0, 0.85], [1, 0]);

  // Mouse parallax — spring-smoothed tilt of the whole field.
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const sx = useSpring(mx, { stiffness: 50, damping: 18 });
  const sy = useSpring(my, { stiffness: 50, damping: 18 });
  const tiltY = useTransform(sx, [-0.5, 0.5], [9, -9]);
  const tiltX = useTransform(sy, [-0.5, 0.5], [-6, 6]);

  const onMove = (e: React.MouseEvent<HTMLElement>) => {
    if (reduce) return;
    const r = e.currentTarget.getBoundingClientRect();
    mx.set((e.clientX - r.left) / r.width - 0.5);
    my.set((e.clientY - r.top) / r.height - 0.5);
  };
  const onLeave = () => {
    mx.set(0);
    my.set(0);
  };

  // Cycle each pool independently so gadgets show a spread of different reels.
  let pi = 0;
  let li = 0;
  const pick = (type: DeviceType): Clip | null => {
    if (LANDSCAPE_TYPES.has(type)) {
      const p = LANDSCAPE.length ? LANDSCAPE : PORTRAIT;
      return p.length ? p[li++ % p.length] : null;
    }
    const p = PORTRAIT.length ? PORTRAIT : LANDSCAPE;
    return p.length ? p[pi++ % p.length] : null;
  };

  const stage: Variants = {
    hidden: {},
    show: { transition: { staggerChildren: 0.08, delayChildren: 0.2 } },
  };
  const rise: Variants = {
    hidden: { opacity: 0, y: 24 },
    show: { opacity: 1, y: 0, transition: { duration: 0.7, ease: EASE } },
  };

  return (
    <section
      className="device-hero"
      ref={sectionRef}
      onMouseMove={onMove}
      onMouseLeave={onLeave}
    >
      <div className="device-hero-glow" />

      <motion.div
        className="device-field"
        style={{ y: fieldY, scale: fieldScale, rotateX: tiltX, rotateY: tiltY }}
        aria-hidden="true"
      >
        {DEVICES.map((d, i) => {
          const clip = pick(d.type);
          if (!clip) return null;
          const style = {
            "--x": `${d.x}%`,
            "--y": `${d.y}%`,
            "--s": d.scale,
            "--dx": `${d.dx}cqw`,
            "--dy": `${d.dy}cqh`,
            "--dz": `${d.dz}cqw`,
            "--dr": `${d.dr}deg`,
            animationDuration: `${d.dur}s`,
            animationDelay: `${d.delay}s`,
            animationDirection: d.reverse ? "reverse" : "normal",
          } as React.CSSProperties;
          return (
            <div
              key={i}
              className="dv-anchor"
              data-depth={d.depth}
              data-mobile={d.mobile ? undefined : "hide"}
              style={{ "--x": `${d.x}%`, "--y": `${d.y}%`, "--s": d.scale } as React.CSSProperties}
            >
              <div className="dv-drift" style={style}>
                <Frame type={d.type} clip={clip} still={d.depth === "back"} />
              </div>
            </div>
          );
        })}
      </motion.div>

      <div className="device-hero-scrim" />
      <div className="grain" />

      <motion.div
        className="hero-copy device-hero-copy"
        style={{ y: copyY, opacity: copyOpacity }}
        variants={stage}
        initial="hidden"
        animate="show"
      >
        <motion.p className="hero-eyebrow" variants={rise}>
          <span className="hero-eyebrow-dot" /> Независимая продакшн-студия
        </motion.p>
        {/* The opening steps back so the eye lands on the line that moves. */}
        <h1 className="hero-title">
          <span className="hero-title-line hero-title-line--lead">Ваша история —</span>
          <span className="hero-title-line hero-title-line--cycle">
            <CyclingLine items={HEADLINE_ENDINGS} />
          </span>
        </h1>
        <motion.p className="hero-lede" variants={rise}>
          C3FLEX превращает моменты, продукты и кампании в фильмы с характером.
          Съёмка, монтаж и звук — одной командой.
        </motion.p>
        <motion.div className="hero-cta" variants={rise}>
          <Magnetic>
            <a className="hero-cta-primary" href="#catalog" data-cursor="play">
              Смотреть работы <ArrowDown size={16} />
            </a>
          </Magnetic>
          <Magnetic>
            <a className="hero-cta-ghost" href="#contact">
              Начать проект <ArrowUpRight size={15} />
            </a>
          </Magnetic>
        </motion.div>
      </motion.div>
    </section>
  );
}
