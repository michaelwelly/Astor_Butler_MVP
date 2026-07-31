"use client";

import { ArrowUpRight, Bot, Database, MessagesSquare, Sparkles } from "lucide-react";
import { RevealLines } from "@/components/ui/RevealLines";
import { askButler } from "@/lib/chat-bus";

const PREVIEW_MESSAGES = [
  {
    from: "guest",
    text: "Хочу 10 рилсов для ресторана и понять, можно ли снять перед ивентом.",
  },
  {
    from: "butler",
    text: "Я Astor Butler. Зафиксировал запрос, вижу C3 RИИLS и сценарий ресторана. Уточню дату, задачу и бюджет.",
  },
  {
    from: "butler",
    text: "Сообщение сохраняется в Postgres, интент уходит в understanding, а контекст продуктов C3AG добавляется через RAG перед ответом YandexGPT.",
  },
];

const PIPELINE = [
  { icon: MessagesSquare, title: "Диалог", text: "Человек пишет в окно сайта или Telegram." },
  { icon: Database, title: "Память", text: "Сообщение, сессия и профиль сохраняются в Postgres." },
  { icon: Sparkles, title: "Понимание", text: "Интент и факты попадают в векторную базу и RAG." },
  { icon: Bot, title: "Личность", text: "YandexGPT отвечает как Astor Butler: ресторан Iris, брони и продукты C3AG." },
];

export function Contact() {
  const openProjectDialog = () => askButler("Хочу обсудить продукт C3AG и подобрать формат");
  const openBookingDialog = () => askButler("Хочу забронировать стол на ивент в Iris");

  return (
    <section className="contact" id="contact">
      <div className="contact-copy">
        <p className="section-label">Astor Butler</p>
        <RevealLines
          lines={["Не форма.", "Живой диалог", <>с <i>памятью.</i></>]}
        />
        <p>
          Под блоком о студии человек попадает не в пустую заявку, а в диалог:
          продажа продукта C3AG, бронь стола на ивент или полезная информация
          проходят через один backend-контур.
        </p>
        <div className="contact-actions">
          <button type="button" onClick={openProjectDialog}>
            Подобрать продукт <ArrowUpRight size={15} />
          </button>
          <button type="button" onClick={openBookingDialog}>
            Бронь в Iris <ArrowUpRight size={15} />
          </button>
        </div>
      </div>
      <div className="butler-window" aria-label="Пример диалога с Astor Butler">
        <div className="butler-window-top">
          <span>Astor Butler</span>
          <small>WEB → Postgres → RAG → YandexGPT</small>
        </div>
        <div className="butler-dialog">
          {PREVIEW_MESSAGES.map((message, index) => (
            <p key={index} data-from={message.from}>
              {message.text}
            </p>
          ))}
        </div>
        <div className="butler-pipeline">
          {PIPELINE.map((step) => {
            const Icon = step.icon;
            return (
              <div key={step.title}>
                <Icon size={16} />
                <strong>{step.title}</strong>
                <span>{step.text}</span>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
