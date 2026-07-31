"use client";

import { ArrowUpRight, Bot, BriefcaseBusiness, Clapperboard, Film, Sparkles } from "lucide-react";
import { RevealLines } from "@/components/ui/RevealLines";
import { askButler } from "@/lib/chat-bus";

const PREVIEW_MESSAGES = [
  {
    from: "guest",
    text: "Хочу 10 рилсов для ресторана и понять, можно ли снять перед ивентом.",
  },
  {
    from: "butler",
    text: "Я Astor Butler. Вижу C3 RИИLS и ресторанный сценарий. Сейчас уточню задачу, дату, бюджет и передам команде C3AG.",
  },
  {
    from: "butler",
    text: "Могу собрать смету, сценарий съемки, референсы, тайминг смены и следующие шаги для менеджера.",
  },
];

const QUICK_ACTIONS = [
  {
    icon: Sparkles,
    title: "C3 RИИLS",
    text: "10 рилсов для ресторана",
    prompt: "Хочу 10 рилсов для ресторана. Помоги собрать задачу, цену и дату съемки.",
  },
  {
    icon: Clapperboard,
    title: "C3 REПОРТАЖ",
    text: "Съемка события",
    prompt: "Нужен репортаж с мероприятия. Помоги выбрать пакет и подготовить brief.",
  },
  {
    icon: BriefcaseBusiness,
    title: "C3 RECLAMA",
    text: "Рекламный ролик",
    prompt: "Хочу рекламный ролик для бренда. Нужна смета, сценарий и план производства.",
  },
  {
    icon: Film,
    title: "C3 ФILM",
    text: "Имиджевый фильм",
    prompt: "Хочу имиджевый фильм C3AG. Помоги сформировать идею, референсы и бюджет.",
  },
  {
    icon: Bot,
    title: "C3 ЫI",
    text: "AI-контент и автоматизация",
    prompt: "Хочу AI-сценарий для контента: разбор задачи, генерация идеи и план запуска.",
  },
];

export function Contact() {
  const openProjectDialog = () => askButler("Хочу обсудить продукт C3AG и подобрать формат съемки");
  const openEstimateDialog = () => askButler("Хочу получить смету и сценарий съемки контента");

  return (
    <section className="contact" id="contact">
      <div className="contact-copy">
        <p className="section-label">Astor Butler</p>
        <RevealLines
          lines={["Не форма.", "Живой диалог", <>с <i>памятью.</i></>]}
        />
        <p>
          Под блоком о студии человек попадает не в пустую заявку, а в живой
          подбор продукта: рилсы, репортаж, реклама, подкаст, свадебная съемка,
          фильм или AI-сценарий собираются в диалоге.
        </p>
        <div className="contact-actions">
          <button type="button" onClick={openProjectDialog}>
            Подобрать продукт <ArrowUpRight size={15} />
          </button>
          <button type="button" onClick={openEstimateDialog}>
            Собрать смету <ArrowUpRight size={15} />
          </button>
        </div>
      </div>
      <div className="butler-window" aria-label="Пример диалога с Astor Butler">
        <div className="butler-window-top">
          <span>Astor Butler</span>
          <small>C3AG sales assistant</small>
        </div>
        <div className="butler-dialog">
          {PREVIEW_MESSAGES.map((message, index) => (
            <p key={index} data-from={message.from}>
              {message.text}
            </p>
          ))}
        </div>
        <div className="butler-quick-grid" aria-label="Быстрые действия Astor Butler">
          {QUICK_ACTIONS.map((step) => {
            const Icon = step.icon;
            return (
              <button key={step.title} type="button" onClick={() => askButler(step.prompt)}>
                <Icon size={15} />
                <strong>{step.title}</strong>
                <span>{step.text}</span>
              </button>
            );
          })}
        </div>
      </div>
    </section>
  );
}
