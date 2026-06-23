"use client";

import { useEffect, useState } from "react";
import { Menu } from "lucide-react";
import { AuthMenu } from "@/components/auth/AuthMenu";

type Props = {
  onMenuOpen: () => void;
};

const PILLS = [
  { label: "Все работы", href: "#catalog" },
  { label: "Ивенты", href: "#row-events" },
  { label: "Рилсы", href: "#row-reels" },
  { label: "Реклама", href: "#row-commercials" },
];

export function Navigation({ onMenuOpen }: Props) {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 60);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header className={`site-header${scrolled ? " site-header--scrolled" : ""}`}>
      <a className="brand" href="#top" aria-label="C3FLEX home">
        C3<span>FLEX</span><sup>.com</sup>
      </a>
      <nav className="desktop-nav" aria-label="Main navigation">
        {PILLS.map((p) => (
          <a key={p.href} href={p.href} className="nav-pill">{p.label}</a>
        ))}
      </nav>
      <div className="header-actions">
        <AuthMenu />
        <button className="menu-button" type="button" onClick={onMenuOpen} aria-label="Открыть меню">
          <Menu size={20} />
        </button>
      </div>
    </header>
  );
}
