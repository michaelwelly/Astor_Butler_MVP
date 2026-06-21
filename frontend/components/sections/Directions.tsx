import { ArrowUpRight } from "lucide-react";
import { directions } from "@/lib/portfolio";

export function Directions() {
  return (
    <section className="directions section-pad" id="directions">
      <div className="section-heading compact">
        <p className="section-label">Выберите направление</p>
        <h2>У каждой истории<br />своя <i>температура.</i></h2>
      </div>
      <div className="direction-list">
        {directions.map((direction) => (
          <a className="direction-row" href="#work" key={direction.id}>
            <span className="direction-number">{direction.index}</span>
            <span className="direction-title">{direction.title}</span>
            <span className="direction-description">{direction.description}</span>
            <span className="direction-arrow"><ArrowUpRight size={21} /></span>
          </a>
        ))}
      </div>
    </section>
  );
}
