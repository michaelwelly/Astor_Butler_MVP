"use client";

import { useEffect, useState } from "react";
import { Menu } from "lucide-react";
import { Wordmark } from "@/components/ui/Wordmark";

type Props = {
  onMenuOpen: () => void;
};

const PILLS = [
  { label: "Работы", href: "#catalog" },
  { label: "Услуги", href: "#services" },
  { label: "О студии", href: "#about" },
  { label: "Контакт", href: "#contact" },
];

export function Navigation({ onMenuOpen }: Props) {
  const [scrolled, setScrolled] = useState(false);
  const [active, setActive] = useState("");

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 60);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Highlight the pill whose section is crossing the viewport middle.
  useEffect(() => {
    const sections = PILLS.map((p) => document.getElementById(p.href.slice(1))).filter(
      (el): el is HTMLElement => Boolean(el),
    );
    if (!sections.length) return;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) setActive(entry.target.id);
        });
      },
      { rootMargin: "-45% 0px -50% 0px", threshold: 0 },
    );
    sections.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, []);

  return (
    <header className={`site-header${scrolled ? " site-header--scrolled" : ""}`}>
      <a className="brand" href="#top">
        <Wordmark />
      </a>
      <nav className="desktop-nav" aria-label="Main navigation">
        {PILLS.map((p) => (
          <a
            key={p.href}
            href={p.href}
            className={`nav-pill${active === p.href.slice(1) ? " nav-pill--active" : ""}`}
            aria-current={active === p.href.slice(1) ? "true" : undefined}
          >
            {p.label}
          </a>
        ))}
      </nav>
      <div className="header-actions">
        <button className="menu-button" type="button" onClick={onMenuOpen} aria-label="Открыть меню">
          <Menu size={20} />
        </button>
      </div>
    </header>
  );
}
