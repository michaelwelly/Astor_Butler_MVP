"use client";

import { useState, useRef, useEffect } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronDown, Search, Send } from "lucide-react";
import { acceptConsent, hasConsent } from "@/lib/consent";
import { persistChatId, persistSessionId, getSessionId, getTempChatId } from "@/lib/session";
import { sendWebChatMessage, type SelectedVideoRef } from "@/lib/web-chat";
import { ConsentNotice } from "@/components/ui/ConsentNotice";
import { LoginPanel } from "@/components/auth/LoginPanel";

type Message = { from: "bot" | "user"; text: string };

const INITIAL_MESSAGES: Message[] = [
  {
    from: "bot",
    text: "Здравствуйте! Я Astor Butler — персональный менеджер C3FLEX. Расскажите о вашем проекте, подберём нужный формат и команду.",
  },
];

type Props = {
  /** Embedded full-chat variant (used inside a page section). */
  inline?: boolean;
  /** Current page/video context for the Web Chat payload. */
  selectedVideo?: SelectedVideoRef;
};

export function ChatWidget({ inline, selectedVideo = null }: Props) {
  // Floating widget collapses to a compact Spotlight-style input.
  const [mode, setMode] = useState<"spotlight" | "full">(inline ? "full" : "spotlight");
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [pendingText, setPendingText] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const open = inline || mode === "full";

  useEffect(() => {
    if (open) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, open]);

  const deliver = async (text: string) => {
    const next: Message[] = [...messages, { from: "user", text }];
    setMessages(next);
    setSending(true);
    // Persist anonymous identity now that consent exists.
    persistSessionId(getSessionId());
    persistChatId(getTempChatId());
    const turn = next.filter((m) => m.from === "user").length;
    try {
      const { reply } = await sendWebChatMessage(text, selectedVideo, { turn });
      setMessages((prev) => [...prev, { from: "bot", text: reply }]);
    } catch {
      setMessages((prev) => [
        ...prev,
        { from: "bot", text: "Ошибка связи. Попробуйте ещё раз." },
      ]);
    } finally {
      setSending(false);
    }
  };

  const submit = (raw: string) => {
    const text = raw.trim();
    if (!text || sending) return;
    setInput("");
    if (mode === "spotlight") setMode("full");
    // Consent gate: hold the message until the user accepts the privacy notice.
    if (!hasConsent()) {
      setPendingText(text);
      return;
    }
    void deliver(text);
  };

  const onConsentAccept = () => {
    acceptConsent();
    const text = pendingText;
    setPendingText(null);
    if (text) void deliver(text);
  };

  const handleKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") submit(input);
  };

  // ── Compact Spotlight launcher (floating, collapsed) ───────────────────
  if (!inline && mode === "spotlight") {
    return (
      <div className="chat-widget chat-widget--floating">
        <motion.button
          type="button"
          className="chat-spotlight"
          onClick={() => setMode("full")}
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, ease: "easeOut" }}
        >
          <Search size={16} className="chat-spotlight-icon" />
          <span className="chat-spotlight-placeholder">Расскажите о проекте…</span>
          <img src="/ab-logo.jpg" alt="" className="chat-spotlight-logo" />
        </motion.button>
      </div>
    );
  }

  return (
    <div className={`chat-widget${inline ? " chat-widget--inline" : " chat-widget--floating"}`}>
      <motion.div
        className="chat-panel"
        initial={false}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.25, ease: "easeOut" }}
      >
        <div className="chat-panel-header">
          <img src="/ab-logo.jpg" alt="Astor Butler" className="chat-logo" />
          <div className="chat-header-text">
            <strong>Astor Butler</strong>
            <span>Персональный менеджер</span>
          </div>
          {!inline && (
            <button type="button" onClick={() => setMode("spotlight")} aria-label="Свернуть чат">
              <ChevronDown size={18} />
            </button>
          )}
        </div>

        <div className="chat-messages">
          {messages.map((msg, i) => (
            <div key={i} className={`chat-msg chat-msg-${msg.from}`}>
              {msg.from === "bot" && <img src="/ab-logo.jpg" alt="" className="chat-msg-avatar" />}
              <span>{msg.text}</span>
            </div>
          ))}
          <div ref={bottomRef} />
        </div>

        <AnimatePresence>
          {pendingText && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
            >
              <ConsentNotice onAccept={onConsentAccept} onDecline={() => setPendingText(null)} />
            </motion.div>
          )}
        </AnimatePresence>

        <div className="chat-input-row">
          <input
            className="chat-input"
            type="text"
            placeholder="Напишите сообщение..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKey}
            aria-label="Сообщение"
          />
          <button
            type="button"
            className="chat-send"
            onClick={() => submit(input)}
            aria-label="Отправить"
            disabled={sending}
          >
            <Send size={15} />
          </button>
        </div>

        <div className="chat-login-row">
          <LoginPanel compact />
        </div>
      </motion.div>
    </div>
  );
}
