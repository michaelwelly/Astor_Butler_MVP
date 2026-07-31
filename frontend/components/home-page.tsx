"use client";

import { useState, useEffect } from "react";
import { AnimatePresence, MotionConfig } from "framer-motion";
import { useLenis } from "@/hooks/useLenis";
import { Navigation } from "@/components/layout/Navigation";
import { Footer } from "@/components/layout/Footer";
import { DeviceHero } from "@/components/sections/DeviceHero";
import { Marquee } from "@/components/sections/Marquee";
import { FeaturedCatalog } from "@/components/sections/FeaturedCatalog";
import { Products } from "@/components/sections/Products";
import { Manifesto } from "@/components/sections/Manifesto";
import { Contact } from "@/components/sections/Contact";
import { CinemaCursor } from "@/components/ui/CinemaCursor";
import { ScrollProgress } from "@/components/ui/ScrollProgress";
import { Reveal } from "@/components/ui/Reveal";
import { ReelsPlayer } from "@/components/ui/ReelsPlayer";
import { SplashGate } from "@/components/ui/SplashGate";
import { MobileMenu } from "@/components/ui/MobileMenu";
import { ChatWidget } from "@/components/ui/ChatWidget";
import { portfolioCases, type PortfolioCase } from "@/lib/portfolio";
import { catalogVideos, toSelectedVideoRef } from "@/lib/video-catalog";
import type { SelectedVideoRef } from "@/lib/web-chat";

function toRef(item: PortfolioCase | null): SelectedVideoRef {
  if (!item) return null;
  const meta = catalogVideos.find((v) => v.slug === (item.slug ?? item.id));
  return meta ? toSelectedVideoRef(meta) : null;
}

export function HomePage() {
  useLenis();
  const [introComplete, setIntroComplete] = useState(false);

  useEffect(() => {
    if (!introComplete) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => { document.body.style.overflow = ""; };
  }, [introComplete]);
  const [menuOpen, setMenuOpen] = useState(false);

  // Tapping a card drops you straight into the reels feed at that clip —
  // no "do you want to watch this?" step in between. `reelsStart` doubles as
  // the open/closed flag; `reelsAt` follows the swiping so the chat keeps
  // sending the clip you're actually looking at.
  const [reelsStart, setReelsStart] = useState<number | null>(null);
  const [reelsAt, setReelsAt] = useState(0);

  const openReels = (item: PortfolioCase) => {
    const i = portfolioCases.findIndex((c) => c.id === item.id);
    const start = i >= 0 ? i : 0;
    setReelsStart(start);
    setReelsAt(start);
  };

  return (
    <MotionConfig reducedMotion="user">
      <CinemaCursor />
      <ScrollProgress />
      <div className="film-overlay" aria-hidden="true" />
      <AnimatePresence>
        {!introComplete && (
          <SplashGate key="splash" onComplete={() => { window.scrollTo({ top: 0, behavior: "instant" }); setIntroComplete(true); }} />
        )}
      </AnimatePresence>

      <main className="netflix-main" id="top">
        <Navigation onMenuOpen={() => setMenuOpen(true)} />
        <DeviceHero />
        <Marquee />
        {/* Products before works: the site's job is routing to seven offers,
            and the reel now also lives on each product page. */}
        <Reveal>
          <Products />
        </Reveal>
        <FeaturedCatalog onSelect={openReels} />
        <Reveal>
          <Manifesto />
        </Reveal>
        <Reveal>
          <Contact />
        </Reveal>
        <Footer />

        <MobileMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
      </main>

      {reelsStart !== null && (
        <ReelsPlayer
          items={portfolioCases}
          startIndex={reelsStart}
          onIndexChange={setReelsAt}
          onClose={() => setReelsStart(null)}
        />
      )}

      {introComplete && (
        <ChatWidget
          selectedVideo={toRef(reelsStart !== null ? portfolioCases[reelsAt] : null)}
        />
      )}
    </MotionConfig>
  );
}
