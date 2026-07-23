import { ArrowUpRight } from "lucide-react";
import { servicePackages } from "@/lib/portfolio";
import { RevealLines } from "@/components/ui/RevealLines";

export function Services() {
  return (
    <section className="services section-pad" id="services">
      <div className="section-heading compact">
        <p className="section-label">Как работать вместе</p>
        <RevealLines lines={["Производство", <>без <i>лишних слов.</i></>]} />
      </div>
      <div className="service-grid">
        {servicePackages.map((item) => (
          <article className="service-card" key={item.number}>
            <span>{item.number}</span>
            <h3>{item.title}</h3>
            <p>{item.description}</p>
            <div>
              <strong>{item.price}</strong>
              <ArrowUpRight size={17} />
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
