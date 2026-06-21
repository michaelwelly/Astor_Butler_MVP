"use client";

import { ArrowDown, ArrowUpRight } from "lucide-react";
import { portfolioCases } from "@/lib/portfolio";
import { useVideoPlayer } from "@/hooks/useVideoPlayer";
import { VideoControls } from "@/components/ui/VideoControls";

export function Hero() {
  const { videoRef, soundOn, playing, toggleSound, togglePlayback } = useVideoPlayer();
  const heroCase = portfolioCases[0];

  return (
    <section className="hero" id="top">
      <video
        className="hero-video"
        ref={videoRef}
        src={heroCase.video}
        poster={heroCase.image}
        autoPlay
        loop
        muted
        playsInline
      />
      <div className="hero-overlay" />
      <div className="grain" />
      <div className="hero-copy">
        <p className="eyebrow"><span /> Независимая продакшн-студия</p>
        <h1>Истории, которые<br /><i>остаются.</i></h1>
        <p className="hero-description">
          C3FLEX превращает мимолётные моменты, осязаемые продукты и амбициозные кампании в фильмы с характером.
        </p>
        <div className="hero-links">
          <a className="primary-link" href="#work">Смотреть работы <ArrowDown size={16} /></a>
          <a className="text-link" href="#contact">Начать проект <ArrowUpRight size={15} /></a>
        </div>
      </div>
      <div className="hero-meta">
        <span>Избранное / {heroCase.title}</span>
        <span>{heroCase.duration}</span>
      </div>
      <VideoControls
        playing={playing}
        soundOn={soundOn}
        onTogglePlay={togglePlayback}
        onToggleSound={toggleSound}
      />
      <div className="hero-index">
        <span>01</span>
        <div><b /><b /><b /></div>
        <span>03</span>
      </div>
    </section>
  );
}
