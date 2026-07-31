"use client";

import { useRef } from "react";
import {
  motion,
  useScroll,
  useTransform,
  type Variants,
} from "framer-motion";
import { ArrowDown, ArrowUpRight } from "lucide-react";
import { getFeatured } from "@/lib/portfolio";
import { getCatalogVideo, heroSources } from "@/lib/video-catalog";
import { Magnetic } from "@/components/ui/Magnetic";

// Editorial ease-out. MotionConfig at the app root sets reducedMotion="user",
// so every animation here self-disables when the visitor asks for less motion.
const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1];

const stage: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.09, delayChildren: 0.2 } },
};
const rise: Variants = {
  hidden: { opacity: 0, y: 22 },
  show: { opacity: 1, y: 0, transition: { duration: 0.7, ease: EASE } },
};
// Words rise from behind a clipping mask — a title-sequence reveal.
const reveal: Variants = {
  hidden: { y: "120%" },
  show: { y: 0, transition: { duration: 0.9, ease: EASE } },
};

function MaskedWords({ text }: { text: string }) {
  return text.split(" ").map((word, i) => (
    <span className="word-mask" key={`${word}-${i}`}>
      <motion.span className="word" variants={reveal}>
        {word}
      </motion.span>
    </span>
  ));
}

export function HeroBanner() {
  const hero = getFeatured()[0];
  const catalog = getCatalogVideo(hero.slug ?? hero.id);
  const sources = catalog ? heroSources(catalog) : [];

  const sectionRef = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ["start start", "end start"],
  });
  const videoScale = useTransform(scrollYProgress, [0, 1], [1, 1.18]);
  const copyY = useTransform(scrollYProgress, [0, 1], [0, -90]);
  const copyOpacity = useTransform(scrollYProgress, [0, 0.75], [1, 0]);

  return (
    <section className="hero-banner" ref={sectionRef}>
      <motion.video
        className="hero-banner-video"
        style={{ scale: videoScale }}
        poster={hero.image}
        autoPlay
        loop
        muted
        playsInline
      >
        {sources.length > 0
          ? sources.map((s) => (
              <source
                key={`${s.media ?? "default"}-${s.type}`}
                src={s.src}
                type={s.type}
                media={s.media}
              />
            ))
          : hero.video && <source src={hero.video} type="video/mp4" />}
      </motion.video>
      <div className="hero-banner-overlay" />
      <div className="grain" />

      <motion.div
        className="hero-copy"
        style={{ y: copyY, opacity: copyOpacity }}
        variants={stage}
        initial="hidden"
        animate="show"
      >
        <motion.p className="hero-eyebrow" variants={rise}>
          <span className="hero-eyebrow-dot" /> Независимая продакшн-студия
        </motion.p>
        <h1 className="hero-title">
          <span className="hero-title-line">
            <MaskedWords text="Истории, которые" />
          </span>
          <span className="hero-title-line hero-title-line--accent">
            <span className="word-mask">
              <motion.span className="word" variants={reveal}>
                <i>остаются.</i>
              </motion.span>
            </span>
          </span>
        </h1>
        <motion.p className="hero-lede" variants={rise}>
          C3AG.ru превращает мимолётные моменты, осязаемые продукты и амбициозные
          кампании в фильмы с характером.
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

      <div className="hero-meta">
        <span>Избранное · {hero.title}</span>
        <span>{hero.duration}</span>
      </div>
    </section>
  );
}
