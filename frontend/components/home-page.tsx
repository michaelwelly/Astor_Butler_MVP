"use client";

import { useState, useEffect } from "react";
import { AnimatePresence, MotionConfig } from "framer-motion";
import { useLenis } from "@/hooks/useLenis";
import { Navigation } from "@/components/layout/Navigation";
import { Footer } from "@/components/layout/Footer";
import { HeroBanner } from "@/components/sections/HeroBanner";
import { Marquee } from "@/components/sections/Marquee";
import { FeaturedCatalog } from "@/components/sections/FeaturedCatalog";
import { Services } from "@/components/sections/Services";
import { Manifesto } from "@/components/sections/Manifesto";
import { Contact } from "@/components/sections/Contact";
import { CinemaCursor } from "@/components/ui/CinemaCursor";
import { ScrollProgress } from "@/components/ui/ScrollProgress";
import { Reveal } from "@/components/ui/Reveal";
import { VideoOverlay } from "@/components/ui/VideoOverlay";
import { VideoPlayer } from "@/components/ui/VideoPlayer";
import { SplashGate } from "@/components/ui/SplashGate";
import { MobileMenu } from "@/components/ui/MobileMenu";
import { ChatWidget } from "@/components/ui/ChatWidget";
import type { PortfolioCase } from "@/lib/portfolio";
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
  const [selectedCase, setSelectedCase] = useState<PortfolioCase | null>(null);
  const [watchingCase, setWatchingCase] = useState<PortfolioCase | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  const handleWatch = (item: PortfolioCase) => {
    setSelectedCase(null);
    setWatchingCase(item);
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
        <HeroBanner />
        <Marquee />
        <FeaturedCatalog onSelect={setSelectedCase} />
        <Reveal>
          <Services />
        </Reveal>
        <Reveal>
          <Manifesto />
        </Reveal>
        <Reveal>
          <Contact />
        </Reveal>
        <Footer />

        <VideoOverlay
          item={selectedCase}
          onWatch={() => selectedCase && handleWatch(selectedCase)}
          onClose={() => setSelectedCase(null)}
        />
        <VideoPlayer item={watchingCase} onClose={() => setWatchingCase(null)} />
        <MobileMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
      </main>

      {introComplete && (
        <ChatWidget selectedVideo={toRef(watchingCase ?? selectedCase)} />
      )}
    </MotionConfig>
  );
}
