"use client";

import { useState } from "react";
import { ArrowUpRight, Check, Send } from "lucide-react";
import { submitLead, type LeadRequest } from "@/lib/lead-api";

const EMPTY_LEAD: LeadRequest = {
  projectType: "",
  description: "",
  services: "",
  format: "",
  deadline: "",
  budget: "",
  contact: "",
};

type LeadStatus = "idle" | "sending" | "sent" | "error";

export function Contact() {
  const [lead, setLead] = useState<LeadRequest>(EMPTY_LEAD);
  const [status, setStatus] = useState<LeadStatus>("idle");

  const handleChange = (field: keyof LeadRequest, value: string) => {
    setLead((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setStatus("sending");
    try {
      await submitLead(lead);
      setStatus("sent");
      setLead(EMPTY_LEAD);
    } catch {
      setStatus("error");
    }
  };

  return (
    <section className="contact" id="contact">
      <div className="contact-copy">
        <p className="section-label">Начать проект</p>
        <h2>Расскажите, что вы хотите,<br />чтобы люди<br /><i>почувствовали.</i></h2>
        <p>Поделитесь главным. Мы вернёмся с правильной производственной формой.</p>
        <a href="https://t.me/" target="_blank" rel="noreferrer">
          Предпочитаете Telegram? Напишите напрямую <ArrowUpRight size={15} />
        </a>
      </div>
      <form className="brief-form" onSubmit={handleSubmit}>
        <label>
          <span>Тип проекта</span>
          <input
            required
            value={lead.projectType}
            onChange={(e) => handleChange("projectType", e.target.value)}
            placeholder="Кампания, мероприятие, рилсы..."
          />
        </label>
        <label className="wide">
          <span>Что снимаем?</span>
          <textarea
            required
            value={lead.description}
            onChange={(e) => handleChange("description", e.target.value)}
            placeholder="Несколько строк о задаче, настроении и аудитории."
          />
        </label>
        <label>
          <span>Нужные услуги</span>
          <input
            value={lead.services}
            onChange={(e) => handleChange("services", e.target.value)}
            placeholder="Концепция, съёмка, монтаж..."
          />
        </label>
        <label>
          <span>Формат</span>
          <input
            value={lead.format}
            onChange={(e) => handleChange("format", e.target.value)}
            placeholder="Рилсы, фильм, подкаст..."
          />
        </label>
        <label>
          <span>Дедлайн</span>
          <input
            value={lead.deadline}
            onChange={(e) => handleChange("deadline", e.target.value)}
            placeholder="Когда нужно выйти в эфир?"
          />
        </label>
        <label>
          <span>Бюджет</span>
          <input
            value={lead.budget}
            onChange={(e) => handleChange("budget", e.target.value)}
            placeholder="Комфортный диапазон"
          />
        </label>
        <label className="wide">
          <span>Имя и контакт</span>
          <input
            required
            value={lead.contact}
            onChange={(e) => handleChange("contact", e.target.value)}
            placeholder="Telegram, email или телефон"
          />
        </label>
        <button className="submit-button" disabled={status === "sending"} type="submit">
          {status === "sending" ? "Отправляем..." : status === "sent" ? "Заявка получена" : "Отправить заявку"}
          {status === "sent" ? <Check size={17} /> : <Send size={16} />}
        </button>
        {status === "error" && <p className="form-error">Что-то пошло не так. Попробуйте ещё раз.</p>}
      </form>
    </section>
  );
}
