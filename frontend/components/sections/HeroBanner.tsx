"use client";

import { getFeatured } from "@/lib/portfolio";

export function HeroBanner() {
  const hero = getFeatured()[0];

  return (
    <section className="hero-banner">
      <video
        className="hero-banner-video"
        src={hero.video}
        poster={hero.image}
        autoPlay
        loop
        muted
        playsInline
      />
      <div className="hero-banner-overlay" />
      <div className="grain" />
    </section>
  );
}
