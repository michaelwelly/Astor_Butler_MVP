"use client";

import { portfolioCases, type PortfolioCase } from "@/lib/portfolio";
import { CaseCard } from "@/components/ui/CaseCard";

type Props = {
  onCaseSelect: (item: PortfolioCase) => void;
};

export function WorkGrid({ onCaseSelect }: Props) {
  const year = new Date().getFullYear();
  return (
    <section className="work section-pad" id="work">
      <div className="section-heading">
        <p className="section-label">Избранные работы / {year}</p>
        <h2>Погрузитесь<br />в <i>ощущение.</i></h2>
        <p>Три направления. Один неугомонный взгляд.</p>
      </div>
      <div className="case-grid">
        {portfolioCases.map((item, index) => (
          <CaseCard key={item.id} item={item} index={index} onClick={onCaseSelect} />
        ))}
      </div>
    </section>
  );
}
