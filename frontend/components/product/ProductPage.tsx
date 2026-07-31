"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft, ArrowUpRight, Check } from "lucide-react";
import { MotionConfig } from "framer-motion";
import { useLenis } from "@/hooks/useLenis";
import { Navigation } from "@/components/layout/Navigation";
import { Footer } from "@/components/layout/Footer";
import { MobileMenu } from "@/components/ui/MobileMenu";
import { ChatWidget } from "@/components/ui/ChatWidget";
import { ScrollProgress } from "@/components/ui/ScrollProgress";
import { Reveal } from "@/components/ui/Reveal";
import { ReelsPlayer } from "@/components/ui/ReelsPlayer";
import { VideoCard } from "@/components/ui/VideoCard";
import { askButler } from "@/lib/chat-bus";
import { getForFolders, type PortfolioCase } from "@/lib/portfolio";
import type { Product } from "@/lib/products";

/**
 * Below this, a "работы" grid is mostly empty slots — and an empty grid on a
 * premium production site reads worse than no grid at all. Under the threshold
 * the page offers the reel on request instead.
 */
const MIN_WORKS = 3;
/** Works shown inline; the rest live in the home catalog. */
const WORKS_SHOWN = 6;

function AskButton({ product, variant }: { product: Product; variant?: "ghost" }) {
  return (
    <button
      type="button"
      className={variant === "ghost" ? "pr-cta pr-cta--ghost" : "pr-cta"}
      onClick={() => askButler(product.ctaWord)}
    >
      {product.ctaLabel}
      <ArrowUpRight size={17} />
    </button>
  );
}

function Block({
  label,
  title,
  children,
  id,
}: {
  label: string;
  title?: string;
  children: React.ReactNode;
  id?: string;
}) {
  return (
    <Reveal>
      <section className="pr-block" id={id}>
        <div className="pr-block-head">
          <p className="section-label">{label}</p>
          {title && <h2>{title}</h2>}
        </div>
        {children}
      </section>
    </Reveal>
  );
}

export function ProductPage({ product }: { product: Product }) {
  useLenis();
  const [menuOpen, setMenuOpen] = useState(false);
  const [reelsStart, setReelsStart] = useState<number | null>(null);

  const works: PortfolioCase[] = getForFolders(product.clipFolders);
  const hasWorks = works.length >= MIN_WORKS;
  const shown = works.slice(0, WORKS_SHOWN);

  const priceLine = product.priceFrom ?? "Индивидуальная смета";
  const priceNote =
    product.priceNote ?? (product.model === "quote" ? "после встречи и брифа" : undefined);

  return (
    <MotionConfig reducedMotion="user">
      <ScrollProgress />
      <div className="film-overlay" aria-hidden="true" />

      <main className="netflix-main product-main" id="top">
        <Navigation onMenuOpen={() => setMenuOpen(true)} />

        {/* ── Hero ───────────────────────────────────────────────────────── */}
        <header className="pr-hero">
          <Link className="pr-back" href="/#products">
            <ArrowLeft size={15} /> Все направления
          </Link>
          <p className="pr-hero-meta">
            <span className="pr-num">{product.num}</span>
            <span className="pr-audience">{product.audience}</span>
          </p>
          <h1>{product.name}</h1>
          <p className="pr-tagline">{product.tagline}</p>
          <p className="pr-lead">{product.lead}</p>
          <div className="pr-hero-foot">
            <div className="pr-price">
              <strong>{priceLine}</strong>
              {priceNote && <small>{priceNote}</small>}
            </div>
            <AskButton product={product} />
          </div>
        </header>

        {/* ── Проблема и решение ─────────────────────────────────────────── */}
        {product.pains && (
          <Block label="Если у вас было так">
            <ul className="pr-pains">
              {product.pains.map((pain) => (
                <li key={pain}>{pain}</li>
              ))}
            </ul>
            {product.solution && <p className="pr-solution">{product.solution}</p>}
          </Block>
        )}

        {product.fitFor && (
          <Block label="Для кого подходит">
            <ul className="pr-fit">
              {product.fitFor.map((who) => (
                <li key={who}>{who}</li>
              ))}
            </ul>
          </Block>
        )}

        {product.deliverables && (
          <Block label="Что мы создаём">
            <ul className="pr-tags">
              {product.deliverables.map((d) => (
                <li key={d}>{d}</li>
              ))}
            </ul>
          </Block>
        )}

        {/* ── Цена ───────────────────────────────────────────────────────── */}
        {product.packages && (
          <Block label="Стоимость" title="Фиксированная цена" id="price">
            <div className="pr-packages" data-count={product.packages.length}>
              {product.packages.map((pkg) => (
                <article
                  className={`pr-package${pkg.featured ? " pr-package--featured" : ""}`}
                  key={pkg.name}
                >
                  {pkg.featured && <span className="pr-package-flag">Чаще всего выбирают</span>}
                  <h3>{pkg.name}</h3>
                  <p className="pr-package-price">
                    <strong>{pkg.price}</strong>
                    {pkg.note && <small>{pkg.note}</small>}
                  </p>
                  <ul>
                    {pkg.includes.map((line) => (
                      <li key={line}>
                        <Check size={14} aria-hidden="true" />
                        {line}
                      </li>
                    ))}
                  </ul>
                </article>
              ))}
            </div>
          </Block>
        )}

        {/* ── Работы ─────────────────────────────────────────────────────── */}
        <Block label="Примеры работ" id="works">
          {hasWorks ? (
            <div className="pr-works">
              {shown.map((item) => (
                <VideoCard
                  key={item.id}
                  item={item}
                  onClick={() => setReelsStart(works.findIndex((w) => w.id === item.id))}
                />
              ))}
            </div>
          ) : (
            /* TODO(assets): реальные работы по этому направлению ещё не залиты.
               Пустая сетка выглядит хуже, чем честная строка — см. handoff. */
            <div className="pr-works-empty">
              <p>
                Подборку работ по этому направлению отправим в чат — под вашу задачу, а не
                общим шоурилом.
              </p>
              <AskButton product={product} variant="ghost" />
            </div>
          )}
        </Block>

        {/* ── Что входит ─────────────────────────────────────────────────── */}
        {product.includes && (
          <Block label="Что входит">
            <div className="pr-includes">
              {product.includes.map((group) => (
                <div key={group.group}>
                  <h3>{group.group}</h3>
                  <ul>
                    {group.items.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </Block>
        )}

        {/* ── Процесс ────────────────────────────────────────────────────── */}
        {product.steps && (
          <Block label="Как это работает">
            <ol className="pr-steps">
              {product.steps.map((step, i) => (
                <li key={step.title}>
                  <span>{String(i + 1).padStart(2, "0")}</span>
                  <h3>{step.title}</h3>
                  <p>{step.text}</p>
                </li>
              ))}
            </ol>
          </Block>
        )}

        {product.timeline && (
          <Block label="Сроки">
            <ul className="pr-list">
              {product.timeline.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          </Block>
        )}

        {product.terms && (
          <Block label="Условия">
            <ul className="pr-list pr-list--muted">
              {product.terms.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          </Block>
        )}

        {/* ── Кейсы ──────────────────────────────────────────────────────── */}
        {product.cases && (
          <Block label="Короткие кейсы">
            <div className="pr-cases">
              {product.cases.map((c) => (
                <article key={c.title}>
                  <h3>{c.title}</h3>
                  <p>
                    <span>Задача</span>
                    {c.task}
                  </p>
                  <p>
                    <span>Что сделали</span>
                    {c.did}
                  </p>
                  <p>
                    <span>Результат</span>
                    {c.result}
                  </p>
                </article>
              ))}
            </div>
          </Block>
        )}

        {product.why && (
          <Block label="Почему C3 Agency">
            <div className="pr-why">
              {product.why.map((point) => (
                <div key={point.title}>
                  <h3>{point.title}</h3>
                  <p>{point.text}</p>
                </div>
              ))}
            </div>
          </Block>
        )}

        {product.geo && (
          <Block label="География">
            <p className="pr-solution">{product.geo}</p>
          </Block>
        )}

        {/* ── Финальный CTA ──────────────────────────────────────────────── */}
        <section className="pr-final" id="contact">
          <p className="section-label">Следующий шаг</p>
          <h2>
            Одно слово — <i>и мы начали.</i>
          </h2>
          <p>
            {product.model === "fixed"
              ? "Ответим в чате, зададим пару вопросов и предложим дату."
              : "Назначим встречу — лично или онлайн — и подготовим индивидуальную смету."}
          </p>
          <AskButton product={product} />
        </section>

        <Footer />
        <MobileMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
      </main>

      {reelsStart !== null && (
        <ReelsPlayer items={works} startIndex={reelsStart} onClose={() => setReelsStart(null)} />
      )}

      <ChatWidget
        quickAsks={[
          product.ctaWord,
          product.model === "fixed" ? "Что входит в стоимость?" : "Сколько это будет стоить?",
          "Когда сможете снять?",
        ]}
      />
    </MotionConfig>
  );
}
