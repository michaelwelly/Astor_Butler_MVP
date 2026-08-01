"use client";

import { ArrowUpRight, Bot, BriefcaseBusiness, Clapperboard, Film, Sparkles } from "lucide-react";
import { RevealLines } from "@/components/ui/RevealLines";
import { askButler } from "@/lib/chat-bus";
import { CLIO_AVATAR, CLIO_NAME } from "@/lib/clio-persona";

const PREVIEW_MESSAGES = [
  {
    from: "guest",
    text: "Хочу 10 рилсов для ресторана и понять, можно ли снять перед ивентом.",
  },
  {
    from: "butler",
    text: "Я Clio, ассистентка по брифу. Вижу ресторанный сценарий и помогу собрать задачу, дату, бюджет и контекст для команды.",
  },
  {
    from: "butler",
    text: "Могу подготовить бриф, сценарную основу, референсы, тайминг смены и следующие шаги для менеджера.",
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
    prompt: "Нужен репортаж с мероприятия. Помоги выбрать пакет и подготовить бриф.",
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
    prompt: "Хочу имиджевый фильм. Помоги сформировать идею, референсы и бюджет.",
  },
  {
    icon: Bot,
    title: "C3 ЫI",
    text: "AI-контент и автоматизация",
    prompt: "Хочу AI-сценарий для контента: разбор задачи, генерация идеи и план запуска.",
  },
  {
    icon: BriefcaseBusiness,
    title: "Smart Solutions",
    text: "Сайт, AI и интеграции",
    prompt: "Хочу Smart Solutions: сайт, AI-интеграции или CRM-контур. Помоги собрать задачу и этапы.",
  },
];

export function Contact() {
  const openProjectDialog = () => askButler("Хочу обсудить продукт и подобрать формат съемки");
  const openEstimateDialog = () => askButler("Хочу получить смету и сценарий съемки контента");

  return (
    <section className="contact" id="contact">
      <div className="contact-copy">
        <p className="section-label">Clio</p>
        <RevealLines
          lines={["Не форма.", "Живой диалог", <>с <i>памятью.</i></>]}
        />
        <p>
          Под блоком о студии человек попадает не в пустую заявку, а в живой
          подбор продукта: рилсы, репортаж, реклама, подкаст, свадебная съемка,
          фильм, AI-сценарий или Smart Solutions собираются в диалоге.
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
      <div className="butler-window" aria-label="Пример диалога с Clio">
        <div className="butler-window-top">
          <span>{CLIO_NAME}</span>
          <small>ассистентка по брифу</small>
        </div>
        <div className="butler-dialog">
          {PREVIEW_MESSAGES.map((message, index) => (
            <p key={index} data-from={message.from}>
              {message.from === "butler" && <img src={CLIO_AVATAR} alt="" />}
              {message.text}
            </p>
          ))}
        </div>
        <div className="butler-quick-grid" aria-label="Быстрые действия Clio">
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
