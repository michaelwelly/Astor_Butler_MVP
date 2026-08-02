"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { ChevronDown, Mic, Send, Square, Volume2, X } from "lucide-react";
import { acceptConsent, CURRENT_POLICY, hasConsent } from "@/lib/consent";
import { persistChatId, persistSessionId, getSessionId, getTempChatId } from "@/lib/session";
import { sendWebChatMessage, type SelectedVideoRef } from "@/lib/web-chat";
import { onButlerAsk } from "@/lib/chat-bus";
import { ConsentNotice } from "@/components/ui/ConsentNotice";
import { CLIO_AVATAR, CLIO_GREETING, CLIO_NAME, CLIO_REPLY_TIME } from "@/lib/clio-persona";
import { CLIO_SUBTITLE } from "@/lib/clio-persona";
import {
  browserVoiceAvailability,
  browserVoiceErrorMessage,
  CLIO_VOICE_STARTERS,
  CLIO_VOICE_STATUS_COPY,
  readClioVoiceClientConfig,
  type ClioVoiceStatus,
} from "@/lib/clio-voice";
import { readTelegramHandoffConfig, telegramHandoffDisabledCopy } from "@/lib/telegram-handoff";

type Message = { from: "bot" | "user"; text: string; kind?: "privacy_intro" | "telegram_handoff" };

const INITIAL_MESSAGES: Message[] = [
  {
    from: "bot",
    text: CLIO_GREETING,
    kind: "privacy_intro",
  },
];

/**
 * Tappable openers. The cost of a first message is the whole barrier, so it is
 * one tap — the chip text is sent as-is.
 */
const QUICK_ASKS = [
  "Помоги оценить стоимость съёмки",
  "Подскажи возможные сроки",
  "Помоги выбрать формат",
  "Хочу отправить бриф",
  "Хочу поговорить с продюсером",
];

/** Product pages narrow these to their own product — see ProductPage. */


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
  const [pendingVoiceConsent, setPendingVoiceConsent] = useState(false);
  const [pendingTelegramConsent, setPendingTelegramConsent] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState<ClioVoiceStatus>("idle");
  const [voiceError, setVoiceError] = useState<string | null>(null);
  const voiceConfig = readClioVoiceClientConfig();
  const telegramConfig = readTelegramHandoffConfig();
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const launcherRef = useRef<HTMLButtonElement>(null);
  const restoreFocusOnCloseRef = useRef(false);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const reduceMotion = useReducedMotion();
  const shellTransition = reduceMotion
    ? { duration: 0 }
    : { type: "spring" as const, stiffness: 440, damping: 38, mass: 0.85 };

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

  const openFromLauncher = () => {
    setMode("full");
  };

  const closeToLauncher = useCallback(() => {
    restoreFocusOnCloseRef.current = true;
    setMode("spotlight");
  }, []);

  useEffect(() => {
    if (inline || mode !== "spotlight" || !restoreFocusOnCloseRef.current) return;
    restoreFocusOnCloseRef.current = false;
    const id = window.requestAnimationFrame(() => launcherRef.current?.focus());
    return () => window.cancelAnimationFrame(id);
  }, [closeToLauncher, inline, mode]);

  useEffect(() => {
    if (inline || mode !== "full") return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeToLauncher();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [closeToLauncher, inline, mode]);

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
      if (voiceConfig.ttsEnabled) void requestSpokenReply(reply);
    } catch {
      setMessages((prev) => [
        ...prev,
        { from: "bot", text: "Ошибка связи. Попробуйте ещё раз." },
      ]);
    } finally {
      setSending(false);
    }
  };

  const cleanupVoiceCapture = () => {
    recorderRef.current = null;
    chunksRef.current = [];
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  };

  useEffect(() => cleanupVoiceCapture, []);

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
    acceptConsent(
      pendingTelegramConsent
        ? "web-chat-telegram-handoff"
        : pendingVoiceConsent
          ? "web-chat-voice"
          : "web-chat-message",
    );
    if (pendingTelegramConsent) {
      setPendingTelegramConsent(false);
      openTelegramHandoff();
      return;
    }
    if (pendingVoiceConsent) {
      setPendingVoiceConsent(false);
      void startVoiceCapture();
      return;
    }
    const text = pendingText;
    setPendingText(null);
    if (text) void deliver(text);
  };

  const openTelegramHandoff = () => {
    if (!telegramConfig.enabled) return;
    persistSessionId(getSessionId());
    persistChatId(getTempChatId());
    setMessages((prev) => [
      ...prev,
      {
        from: "bot",
        kind: "telegram_handoff",
        text:
          "Открою Telegram. Номер телефона или Telegram-контакт передаётся только если вы сами подтвердите это внутри Telegram.",
      },
    ]);
    window.open(telegramConfig.url, "_blank", "noopener,noreferrer");
  };

  const requestTelegramHandoff = () => {
    if (!telegramConfig.enabled) return;
    if (mode === "spotlight") setMode("full");
    if (!hasConsent()) {
      setPendingTelegramConsent(true);
      return;
    }
    openTelegramHandoff();
  };

  const transcribeVoice = async (audio: Blob) => {
    setVoiceStatus("processing");
    setVoiceError(null);
    try {
      const form = new FormData();
      form.append("audio", audio, "clio-voice.webm");
      form.append("source", "c3ag-web-chat");
      const res = await fetch(voiceConfig.transcribeEndpoint, {
        method: "POST",
        headers: {
          "X-Request-Id": `clio-voice-${Date.now()}`,
        },
        body: form,
      });
      const data = (await res.json()) as { text?: string; message?: string; code?: string };
      if (!res.ok || !data.text?.trim()) {
        throw new Error(data.message || data.code || "STT unavailable");
      }
      setVoiceStatus("idle");
      submit(data.text);
    } catch (e) {
      setVoiceStatus("error");
      setVoiceError(
        e instanceof Error
          ? e.message
          : "Голос сейчас не удалось распознать. Напишите сообщение текстом.",
      );
    } finally {
      cleanupVoiceCapture();
    }
  };

  const requestSpokenReply = async (reply: string) => {
    setVoiceStatus("speaking");
    try {
      const res = await fetch(voiceConfig.speakEndpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: reply, voice: "clio-feminine-built-in" }),
      });
      const data = (await res.json()) as { audioUrl?: string | null };
      if (res.ok && data.audioUrl) {
        const audio = new Audio(data.audioUrl);
        await audio.play();
      }
    } catch {
      /* Voice reply is optional; keep the text reply as source of truth. */
    } finally {
      setVoiceStatus("idle");
    }
  };

  const startVoiceCapture = async () => {
    if (!voiceConfig.voiceEnabled) {
      setVoiceStatus("unavailable");
      setVoiceError(CLIO_VOICE_STATUS_COPY.unavailable);
      return;
    }
    const availability = browserVoiceAvailability({
      secureContext: window.isSecureContext,
      hasMediaDevices: Boolean(navigator.mediaDevices?.getUserMedia),
      hasMediaRecorder: typeof MediaRecorder !== "undefined",
    });
    if (!availability.ok) {
      setVoiceStatus("unavailable");
      setVoiceError(browserVoiceErrorMessage(availability.code));
      return;
    }
    setVoiceError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      chunksRef.current = [];
      const recorder = new MediaRecorder(stream);
      recorderRef.current = recorder;
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) chunksRef.current.push(event.data);
      };
      recorder.onstop = () => {
        const audio = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" });
        if (!audio.size) {
          setVoiceStatus("error");
          setVoiceError("Запись получилась пустой. Попробуйте ещё раз или напишите текстом.");
          cleanupVoiceCapture();
          return;
        }
        void transcribeVoice(audio);
      };
      recorder.start();
      setVoiceStatus("listening");
    } catch (e) {
      cleanupVoiceCapture();
      const denied = e instanceof DOMException && (e.name === "NotAllowedError" || e.name === "SecurityError");
      setVoiceStatus(denied ? "permission_denied" : "error");
      setVoiceError(
        denied
          ? CLIO_VOICE_STATUS_COPY.permission_denied
          : "Не удалось включить микрофон. Проверьте устройство или напишите текстом.",
      );
    }
  };

  const toggleVoice = () => {
    if (voiceStatus === "listening") {
      recorderRef.current?.stop();
      return;
    }
    if (!hasConsent()) {
      if (mode === "spotlight") setMode("full");
      setPendingVoiceConsent(true);
      return;
    }
    void startVoiceCapture();
  };

  const cancelVoice = () => {
    const recorder = recorderRef.current;
    if (!recorder) {
      cleanupVoiceCapture();
      setVoiceStatus("idle");
      setVoiceError(null);
      return;
    }
    recorder.onstop = null;
    if (recorder.state !== "inactive") recorder.stop();
    cleanupVoiceCapture();
    setVoiceStatus("idle");
    setVoiceError(null);
  };

  const handleKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") submit(input);
  };

  // A CTA button anywhere on the page hands us the КП keyword to send. The ref
  // keeps the listener on the current `submit` without re-binding every render.
  const submitRef = useRef(submit);
  submitRef.current = submit;
  useEffect(() => onButlerAsk((text) => submitRef.current(text)), []);

  // ── Compact orb launcher (floating, collapsed) ─────────────────────────
  if (!inline && mode === "spotlight") {
    return (
      <motion.div
        className="chat-widget chat-widget--floating chat-widget--collapsed"
        layout
        transition={shellTransition}
      >
        <motion.button
          ref={launcherRef}
          layoutId="clio-widget-shell"
          type="button"
          className="chat-orb"
          onClick={openFromLauncher}
          initial={false}
          animate={{ opacity: 1, scale: 1 }}
          transition={shellTransition}
          aria-label={`Открыть чат с ${CLIO_NAME}`}
          aria-expanded="false"
        >
          <span className="chat-orb-halo" aria-hidden="true" />
          <span className="chat-spotlight-avatar">
            <img src={CLIO_AVATAR} alt="" />
            <span className="chat-presence" />
          </span>
        </motion.button>
      </motion.div>
    );
  }

  return (
    <motion.div
      className={`chat-widget${inline ? " chat-widget--inline" : " chat-widget--floating chat-widget--expanded"}`}
      layout={!inline}
      transition={shellTransition}
    >
      <motion.div
        layoutId={!inline ? "clio-widget-shell" : undefined}
        className="chat-panel"
        initial={false}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={shellTransition}
      >
        <div className="chat-panel-header">
          <span className="chat-spotlight-avatar chat-logo-wrap">
            <img src={CLIO_AVATAR} alt={CLIO_NAME} className="chat-logo" />
            <span className="chat-presence" />
          </span>
          <div className="chat-header-text">
            <strong>{CLIO_NAME}</strong>
            <span>{CLIO_SUBTITLE} · {CLIO_REPLY_TIME}</span>
          </div>
          {!inline && (
            <button
              type="button"
              onClick={closeToLauncher}
              aria-label="Свернуть чат"
              aria-expanded="true"
            >
              <ChevronDown size={18} />
            </button>
          )}
        </div>

        <div className="chat-messages">
          {messages.map((msg, i) => (
            <div key={i} className={`chat-msg chat-msg-${msg.from}`}>
              {msg.from === "bot" && <img src={CLIO_AVATAR} alt="" className="chat-msg-avatar" />}
              <span>
                {msg.text}
                {msg.kind === "privacy_intro" && (
                  <>
                    <br />
                    <a className="chat-policy-link" href={CURRENT_POLICY.url} target="_blank" rel="noopener noreferrer">
                      Политика обработки данных
                    </a>
                  </>
                )}
              </span>
            </div>
          ))}
          {sending && (
            <div className="chat-msg chat-msg-bot chat-msg-thinking" aria-live="polite">
              <img src={CLIO_AVATAR} alt="" className="chat-msg-avatar" />
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

        {!messages.some((m) => m.from === "user") && (
          <section className="chat-intro-card" aria-label="Что умеет Clio">
            <p>
              Clio помогает выбрать продукт, уточнить смету и сроки, собрать короткий бриф
              и подготовить разговор с продюсером.
            </p>
            <ul>
              <li>подобрать формат</li>
              <li>разобрать бюджет</li>
              <li>собрать бриф</li>
              <li>передать команде</li>
            </ul>
            {!voiceConfig.voiceEnabled && (
              <small>Голосовой ввод включим после HTTPS и SpeechKit-конфигурации.</small>
            )}
          </section>
        )}

        {/* One tap = a sent message. Gone as soon as the conversation is real,
            so they never sit under an ongoing exchange. */}
        {!messages.some((m) => m.from === "user") && !pendingText && !pendingVoiceConsent && !pendingTelegramConsent && (
          <div className="chat-asks">
            {[...quickAsks, ...CLIO_VOICE_STARTERS]
              .filter((ask, index, arr) => arr.indexOf(ask) === index)
              .slice(0, 5)
              .map((ask) => (
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

        <div className="chat-telegram-handoff" data-enabled={telegramConfig.enabled ? "true" : "false"}>
          <button
            type="button"
            className="chat-telegram-button"
            onClick={requestTelegramHandoff}
            disabled={!telegramConfig.enabled}
            aria-disabled={!telegramConfig.enabled}
          >
            Продолжить в Telegram
          </button>
          <p>
            {telegramConfig.enabled
              ? "Откроется подтверждённый бот. Контакт попросим отдельно в Telegram, только через ваше явное действие."
              : telegramHandoffDisabledCopy(telegramConfig)}
          </p>
        </div>

        <AnimatePresence>
          {(pendingText || pendingVoiceConsent || pendingTelegramConsent) && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
            >
              <ConsentNotice
                onAccept={onConsentAccept}
                onDecline={() => {
                  setPendingText(null);
                  setPendingVoiceConsent(false);
                  setPendingTelegramConsent(false);
                }}
              />
            </motion.div>
          )}
        </AnimatePresence>

        {voiceConfig.voiceEnabled && (
          <div className="chat-voice-state" data-state={voiceStatus} role="status" aria-live="polite">
            <span>{voiceError || CLIO_VOICE_STATUS_COPY[voiceStatus]}</span>
            {voiceStatus === "listening" && (
              <button type="button" onClick={cancelVoice} aria-label="Отменить голосовую запись">
                <X size={13} />
                Отменить
              </button>
            )}
          </div>
        )}

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
          {voiceConfig.voiceEnabled && (
            <button
              type="button"
              className="chat-voice"
              data-state={voiceStatus}
              onClick={toggleVoice}
              aria-label={voiceStatus === "listening" ? "Остановить запись" : "Записать голосовое сообщение"}
              aria-pressed={voiceStatus === "listening"}
              disabled={sending || voiceStatus === "processing" || voiceStatus === "speaking"}
            >
              {voiceStatus === "listening" ? (
                <Square size={15} />
              ) : voiceStatus === "speaking" ? (
                <Volume2 size={15} />
              ) : (
                <Mic size={15} />
              )}
            </button>
          )}
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
    </motion.div>
  );
}
