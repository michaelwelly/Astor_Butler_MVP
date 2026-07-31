"use client";

import { useState, useRef, useEffect } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronDown, Send } from "lucide-react";
import { acceptConsent, hasConsent } from "@/lib/consent";
import { persistChatId, persistSessionId, getSessionId, getTempChatId } from "@/lib/session";
import { sendWebChatMessage, type SelectedVideoRef } from "@/lib/web-chat";
import { onButlerAsk } from "@/lib/chat-bus";
import { ConsentNotice } from "@/components/ui/ConsentNotice";
import { CyclingLine } from "@/components/ui/CyclingLine";
import { HINT_CHAT_SEEN, learned, markLearned } from "@/lib/session-hint";

type Message = { from: "bot" | "user"; text: string };

const INITIAL_MESSAGES: Message[] = [
  {
    from: "bot",
    text: "Здравствуйте. Я Astor Butler — AI-менеджер C3AG.ru. Помогаю подобрать продукт, собрать контекст и передать команде точную задачу.",
  },
];

/**
 * What the collapsed launcher shows instead of a placeholder. A generic
 * "напишите сообщение" tells nobody what this box is for; a real question
 * rotating through it does, and it doubles as the one moving thing in the
 * corner of the eye.
 */
const LAUNCHER_PROMPTS = [
  "Сколько стоит ролик?",
  "Нужен ивент на 200 человек",
  "Хочу 10 рилсов для бренда",
  "Когда сможете снять?",
];

/**
 * Tappable openers. The cost of a first message is the whole barrier, so it is
 * one tap — the chip text is sent as-is.
 */
const QUICK_ASKS = [
  "Сколько стоит съёмка ивента?",
  "Нужны рилсы для бренда",
  "Хочу рекламный ролик",
];

/** Product pages narrow these to their own product — see ProductPage. */


const REPLY_TIME = "обычно отвечаем за 15 минут";

/**
 * How long the launcher waits before it starts asking for attention. Late
 * enough that it doesn't compete with the hero on arrival, early enough to
 * catch someone who has started reading and stalled.
 */
const ATTRACT_AFTER = 6000;
/** Longer pause before asking again after a look-but-don't-write. */
const ATTRACT_AGAIN = 25000;
/** Total invitations before we accept the answer is no. */
const ATTRACT_MAX_RUNS = 3;

type Props = {
  /** Embedded full-chat variant (used inside a page section). */
  inline?: boolean;
  /** Current page/video context for the Web Chat payload. */
  selectedVideo?: SelectedVideoRef;
  /** Override the one-tap openers (product pages ask about their product). */
  quickAsks?: string[];
};

export function ChatWidget({ inline, selectedVideo = null, quickAsks = QUICK_ASKS }: Props) {
  // Floating widget collapses to a compact Spotlight-style input.
  const [mode, setMode] = useState<"spotlight" | "full">(inline ? "full" : "spotlight");
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [pendingText, setPendingText] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const open = inline || mode === "full";

  useEffect(() => {
    if (open) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, open]);

  // Expanding from the launcher was a tap that said "I want to write" — put the
  // caret where they expect it instead of making them aim a second time. Only
  // for the floating widget: focusing the inline one would yank the page.
  useEffect(() => {
    if (!inline && mode === "full") inputRef.current?.focus();
  }, [inline, mode]);

  // The launcher pings for attention until the visitor actually writes.
  //
  // Opening it is NOT the finish line: someone who looked, read the greeting
  // and collapsed it again without typing is exactly the person worth asking
  // a second time. So the ping goes quiet while the panel is open, then
  // resumes on a longer delay — up to a limit, because an invitation that
  // never takes no for an answer is just harassment.
  const [attract, setAttract] = useState(false);
  const attractRuns = useRef(0);
  const engaged = messages.some((m) => m.from === "user") || pendingText !== null;

  useEffect(() => {
    if (inline || engaged || learned(HINT_CHAT_SEEN)) return;
    if (mode === "full") {
      setAttract(false);
      return;
    }
    if (attractRuns.current >= ATTRACT_MAX_RUNS) return;
    const delay = attractRuns.current === 0 ? ATTRACT_AFTER : ATTRACT_AGAIN;
    const id = setTimeout(() => {
      attractRuns.current += 1;
      setAttract(true);
    }, delay);
    return () => clearTimeout(id);
  }, [inline, engaged, mode]);

  // Writing is the thing we were asking for — stop asking, for good.
  useEffect(() => {
    if (engaged) markLearned(HINT_CHAT_SEEN);
  }, [engaged]);

  const openFromLauncher = () => {
    setMode("full");
    setAttract(false);
  };

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

  // A CTA button anywhere on the page hands us the КП keyword to send. The ref
  // keeps the listener on the current `submit` without re-binding every render.
  const submitRef = useRef(submit);
  submitRef.current = submit;
  useEffect(() => onButlerAsk((text) => submitRef.current(text)), []);

  // ── Compact Spotlight launcher (floating, collapsed) ───────────────────
  // Reads as a text field you can type into, not a button that might do
  // anything: the manager's face, a live question, and a caret.
  if (!inline && mode === "spotlight") {
    return (
      <div className="chat-widget chat-widget--floating">
        <motion.button
          type="button"
          className="chat-spotlight"
          data-attract={attract ? "" : undefined}
          onClick={openFromLauncher}
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
          aria-label="Открыть чат с менеджером"
        >
          <span className="chat-spotlight-avatar">
            <img src="/ab-logo.jpg" alt="" />
            <span className="chat-presence" />
          </span>
          <span className="chat-spotlight-body">
            <span className="chat-spotlight-placeholder">
              <CyclingLine items={LAUNCHER_PROMPTS} interval={3800} />
            </span>
            <span className="chat-spotlight-meta">Astor Butler · {REPLY_TIME}</span>
          </span>
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
          <span className="chat-spotlight-avatar chat-logo-wrap">
            <img src="/ab-logo.jpg" alt="Astor Butler" className="chat-logo" />
            <span className="chat-presence" />
          </span>
          <div className="chat-header-text">
            <strong>Astor Butler</strong>
            <span>AI assistant · C3AG / Iris · {REPLY_TIME}</span>
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
          {sending && (
            <div className="chat-msg chat-msg-bot chat-msg-thinking" aria-live="polite">
              <img src="/ab-logo.jpg" alt="" className="chat-msg-avatar" />
              <span>
                <em>думаю</em>
                <i />
                <i />
                <i />
              </span>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* One tap = a sent message. Gone as soon as the conversation is real,
            so they never sit under an ongoing exchange. */}
        {!messages.some((m) => m.from === "user") && !pendingText && (
          <div className="chat-asks">
            {quickAsks.map((ask) => (
              <button
                key={ask}
                type="button"
                className="chat-ask"
                onClick={() => submit(ask)}
                disabled={sending}
              >
                {ask}
              </button>
            ))}
          </div>
        )}

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
            ref={inputRef}
            className="chat-input"
            type="text"
            placeholder="Опишите задачу в двух словах"
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

      </motion.div>
    </div>
  );
}
