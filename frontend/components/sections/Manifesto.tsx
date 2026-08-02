import Image from "next/image";
import { RevealLines } from "@/components/ui/RevealLines";

const TEAM = [
  {
    role: "CEO",
    name: "Sergey Izyrow",
    text: "Стратегия продукта, продажи, переговоры и финальная ответственность за результат клиента.",
    portrait: "/team/sergey-izyrow-portrait.jpeg",
    portraitAlt: "Sergey Izyrow",
  },
  {
    role: "CTO",
    name: "Michael Welly",
    text: "Архитектура backend, AI-контура, интеграций, данных и production-инфраструктуры.",
    portrait: "/team/michael-welly-portrait.jpeg",
    portraitAlt: "Michael Welly",
  },
  {
    role: "CFO",
    name: "C3 Finance",
    text: "Сметы, счета, налоги, платёжный контур, экономика пакетов и прозрачные условия.",
  },
  {
    role: "COO",
    name: "C3 Operations",
    text: "Производственный тайминг, команда на площадке, контроль выдачи и операционный порядок.",
  },
];

export function Manifesto() {
  return (
    <section className="manifesto section-pad" id="about">
      <RevealLines lines={["Мы ловим энергию", <>до того, как она <i>исчезает.</i></>]} />
      <div className="manifesto-grid">
        <p>От электрики живого зала до едва уловимого движения света на продукте — каждая деталь даёт повод смотреть внимательнее.</p>
        <p>Наши фильмы ощущаются как редакционный материал, движутся с намерением и оставляют зрителю пространство, чтобы войти внутрь.</p>
      </div>
      <div className="team-lane" aria-label="Команда C3 Agency">
        {TEAM.map((member, index) => (
          <article
            className={member.portrait ? "team-card team-card--portrait" : "team-card"}
            key={member.role}
          >
            {member.portrait && (
              <div className="team-card-photo">
                <Image
                  src={member.portrait}
                  alt={member.portraitAlt}
                  fill
                  sizes="(max-width: 720px) 78vw, 306px"
                  priority={false}
                />
              </div>
            )}
            <span>{String(index + 1).padStart(2, "0")}</span>
            <strong>{member.role}</strong>
            <h3>{member.name}</h3>
            <p>{member.text}</p>
          </article>
        ))}
      </div>
      <div className="agency-note">
        <p className="section-label">C3 Agency</p>
        <p>
          C3AG.ru соединяет продакшн, каталог направлений и Clio в один понятный контур:
          клиент выбирает продукт, уточняет задачу с Clio, а команда получает
          структурированный запрос вместо разрозненных сообщений.
        </p>
      </div>
    </section>
  );
}
