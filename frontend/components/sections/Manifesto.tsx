import { RevealLines } from "@/components/ui/RevealLines";

const TEAM = [
  {
    role: "CEO",
    name: "C3 Agency",
    text: "Стратегия продукта, продажи, переговоры и финальная ответственность за результат клиента.",
  },
  {
    role: "CTO",
    name: "Michael Welly",
    text: "Архитектура Astor Butler, backend, AI-контур, интеграции, данные и production-инфраструктура.",
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
          <article className="team-card" key={member.role}>
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
          C3AG.ru соединяет продакшн, продажи и AI-ассистента в один понятный контур:
          клиент выбирает продукт, уточняет задачу с Astor Butler, а команда получает
          структурированный запрос вместо разрозненных сообщений.
        </p>
      </div>
    </section>
  );
}
