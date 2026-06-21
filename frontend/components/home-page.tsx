"use client";

import { useState, useEffect } from "react";
import { AnimatePresence } from "framer-motion";
import { useLenis } from "@/hooks/useLenis";
import { Navigation } from "@/components/layout/Navigation";
import { Footer } from "@/components/layout/Footer";
import { HeroBanner } from "@/components/sections/HeroBanner";
import { FeaturedCatalog } from "@/components/sections/FeaturedCatalog";
import { VideoOverlay } from "@/components/ui/VideoOverlay";
import { VideoPlayer } from "@/components/ui/VideoPlayer";
import { SplashGate } from "@/components/ui/SplashGate";
import { MobileMenu } from "@/components/ui/MobileMenu";
import { ChatWidget } from "@/components/ui/ChatWidget";
import type { PortfolioCase } from "@/lib/portfolio";

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
    <>
      <AnimatePresence>
        {!introComplete && (
          <SplashGate key="splash" onComplete={() => { window.scrollTo({ top: 0, behavior: "instant" }); setIntroComplete(true); }} />
        )}
      </AnimatePresence>

      <main className="netflix-main" id="top">
        <Navigation onMenuOpen={() => setMenuOpen(true)} />
        <HeroBanner />
        <FeaturedCatalog onSelect={setSelectedCase} />
        <section className="chat-section">
          <h2 className="chat-section-heading">Обсудим ваш проект</h2>
          <p className="chat-section-sub">Astor Butler подберёт формат и команду под вашу задачу</p>
          <ChatWidget inline />
        </section>
        <Footer />

        <VideoOverlay
          item={selectedCase}
          onWatch={() => selectedCase && handleWatch(selectedCase)}
          onClose={() => setSelectedCase(null)}
        />
        <VideoPlayer item={watchingCase} onClose={() => setWatchingCase(null)} />
        <MobileMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
      </main>
    </>
  );
}
