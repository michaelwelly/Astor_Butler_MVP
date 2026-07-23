import { RevealLines } from "@/components/ui/RevealLines";

export function Manifesto() {
  return (
    <section className="manifesto section-pad" id="about">
      <p className="section-label">C3FLEX / Производство с умыслом</p>
      <RevealLines lines={["Мы ловим энергию", <>до того, как она <i>исчезает.</i></>]} />
      <div className="manifesto-grid">
        <p>От электрики живого зала до едва уловимого движения света на продукте — каждая деталь даёт повод смотреть внимательнее.</p>
        <p>Наши фильмы ощущаются как редакционный материал, движутся с намерением и оставляют зрителю пространство, чтобы войти внутрь.</p>
      </div>
    </section>
  );
}
