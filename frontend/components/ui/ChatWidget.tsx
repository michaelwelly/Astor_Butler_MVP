"use client";

import { useState, useRef, useEffect } from "react";
import { motion } from "framer-motion";
import { ChevronDown, Send, X } from "lucide-react";

type Message = { from: "bot" | "user"; text: string };

const INITIAL_MESSAGES: Message[] = [
  {
    from: "bot",
    text: "Здравствуйте! Я Astor Butler — персональный менеджер C3FLEX. Расскажите о вашем проекте, подберём нужный формат и команду.",
  },
];

export function ChatWidget({ inline }: { inline?: boolean }) {
  const [open, setOpen] = useState(true);
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [input, setInput] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, open]);

  const [sending, setSending] = useState(false);

  const send = async () => {
    const text = input.trim();
    if (!text || sending) return;
    const next: Message[] = [...messages, { from: "user", text }];
    setMessages(next);
    setInput("");
    setSending(true);
    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messages: next }),
      });
      const { reply } = await res.json();
      setMessages((prev) => [...prev, { from: "bot", text: reply }]);
    } catch {
      setMessages((prev) => [...prev, { from: "bot", text: "Ошибка связи. Попробуйте ещё раз." }]);
    } finally {
      setSending(false);
    }
  };

  const handleKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") send();
  };

  return (
    <div className={`chat-widget${inline ? " chat-widget--inline" : ""}`}>
      <motion.div
        className="chat-panel"
        initial={false}
        animate={open ? { opacity: 1, y: 0, scale: 1, pointerEvents: "auto" } : { opacity: 0, y: 24, scale: 0.96, pointerEvents: "none" }}
        transition={{ duration: 0.25, ease: "easeOut" }}
      >
        <div className="chat-panel-header">
          <img src="/ab-logo.jpg" alt="Astor Butler" className="chat-logo" />
          <div className="chat-header-text">
            <strong>Astor Butler</strong>
            <span>Персональный менеджер</span>
          </div>
          {!inline && (
            <button type="button" onClick={() => setOpen(false)} aria-label="Свернуть чат">
              <ChevronDown size={18} />
            </button>
          )}
        </div>

        <div className="chat-messages">
          {messages.map((msg, i) => (
            <div key={i} className={`chat-msg chat-msg-${msg.from}`}>
              {msg.from === "bot" && (
                <img src="/ab-logo.jpg" alt="" className="chat-msg-avatar" />
              )}
              <span>{msg.text}</span>
            </div>
          ))}
          <div ref={bottomRef} />
        </div>

        <div className="chat-input-row">
          <input
            className="chat-input"
            type="text"
            placeholder="Напишите сообщение..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKey}
          />
          <button type="button" className="chat-send" onClick={send} aria-label="Отправить" disabled={sending}>
            <Send size={15} />
          </button>
        </div>
      </motion.div>

      {!inline && (
        <button type="button" className={`chat-toggle${open ? " chat-toggle--open" : ""}`} onClick={() => setOpen((v) => !v)}>
          <img src="/ab-logo.jpg" alt="Astor Butler" className="chat-logo" />
          <div className="chat-toggle-text">
            <strong>Astor Butler</strong>
            <span>{open ? "Свернуть" : "Написать нам"}</span>
          </div>
          {open && <X size={16} />}
        </button>
      )}
    </div>
  );
}
