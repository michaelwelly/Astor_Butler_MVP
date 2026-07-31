"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Menu } from "lucide-react";
import { Wordmark } from "@/components/ui/Wordmark";
import { ThemeToggle } from "@/components/ui/ThemeToggle";

type Props = {
  onMenuOpen: () => void;
};

// Root-relative hashes so the same header works on product pages. On the home
// page the path already matches, so the browser treats these as same-document
// fragment jumps — no reload.
const PILLS = [
  { label: "Направления", id: "products" },
  { label: "Работы", id: "catalog" },
  { label: "О студии", id: "about" },
  { label: "Контакт", id: "contact" },
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
    const sections = PILLS.map((p) => document.getElementById(p.id)).filter(
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
      <Link className="brand" href="/">
        <Wordmark />
      </Link>
      <nav className="desktop-nav" aria-label="Main navigation">
        {PILLS.map((p) => (
          <Link
            key={p.id}
            href={`/#${p.id}`}
            className={`nav-pill${active === p.id ? " nav-pill--active" : ""}`}
            aria-current={active === p.id ? "true" : undefined}
          >
            {p.label}
          </Link>
        ))}
      </nav>
      <div className="header-actions">
        <ThemeToggle />
        <button className="menu-button" type="button" onClick={onMenuOpen} aria-label="Открыть меню">
          <Menu size={20} />
        </button>
      </div>
    </header>
  );
}
