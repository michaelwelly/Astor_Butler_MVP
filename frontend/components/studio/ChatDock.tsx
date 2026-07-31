"use client";

import { useEffect, useState } from "react";
import { ChevronLeft, MessageSquare, X } from "lucide-react";
import { MOCK_CHATS } from "@/lib/studio";

/**
 * Always-on client-chat dock, bottom-right. Collapsed → a badge bubble;
 * expanded → conversation list ↔ thread. Demo data until GET /api/chats.
 *
 * A bubble with a number on it says "there is work"; it does not say whose or
 * what, so it is easy to leave for later. When something is unread the dock
 * peeks: the client's name and their actual last line, one click from the
 * thread it belongs to. Dismiss it and it stays dismissed for the session —
 * a nag that cannot be silenced stops being read.
 */
const PEEK_DELAY = 1400;

export function ChatDock() {
  const [open, setOpen] = useState(false);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [peeking, setPeeking] = useState(false);
  const [peekDismissed, setPeekDismissed] = useState(false);
  const active = MOCK_CHATS.find((c) => c.id === activeId);
  const totalUnread = MOCK_CHATS.reduce((sum, c) => sum + c.unread, 0);
  const waiting = MOCK_CHATS.find((c) => c.unread > 0) ?? null;

  useEffect(() => {
    if (open || peekDismissed || !waiting) return;
    const id = setTimeout(() => setPeeking(true), PEEK_DELAY);
    return () => clearTimeout(id);
  }, [open, peekDismissed, waiting]);

  if (!open) {
    const lastLine = waiting?.messages[waiting.messages.length - 1]?.text;
    return (
      <>
        {peeking && waiting && (
          <div className="chatdock-peek">
            <button
              type="button"
              className="chatdock-peek-body"
              onClick={() => {
                setOpen(true);
                setActiveId(waiting.id);
                setPeeking(false);
              }}
            >
              <span className="chatdock-peek-name">
                {waiting.name}
                <em>{waiting.updated}</em>
              </span>
              <span className="chatdock-peek-text">{lastLine}</span>
            </button>
            <button
              type="button"
              className="chatdock-peek-close"
              onClick={() => {
                setPeeking(false);
                setPeekDismissed(true);
              }}
              aria-label="Скрыть"
            >
              <X size={14} />
            </button>
          </div>
        )}
        <button
          type="button"
          className="chatdock-fab"
          data-unread={totalUnread > 0 ? "" : undefined}
          onClick={() => {
            setOpen(true);
            setPeeking(false);
          }}
          aria-label={
            totalUnread > 0
              ? `Чат с клиентами, непрочитанных: ${totalUnread}`
              : "Чат с клиентами"
          }
        >
          <MessageSquare size={22} />
          {totalUnread > 0 && <span className="chatdock-badge">{totalUnread}</span>}
        </button>
      </>
    );
  }

  return (
    <div className="chatdock" role="dialog" aria-label="Чат с клиентом">
      <div className="chatdock-head">
        {active ? (
          <button type="button" className="chatdock-icon" onClick={() => setActiveId(null)} aria-label="К списку">
            <ChevronLeft size={18} />
          </button>
        ) : (
          <MessageSquare size={16} />
        )}
        <strong>{active ? active.name : "Чат с клиентом"}</strong>
        <button type="button" className="chatdock-icon chatdock-close" onClick={() => setOpen(false)} aria-label="Свернуть">
          <X size={18} />
        </button>
      </div>

      {!active ? (
        <div className="chatdock-list">
          {MOCK_CHATS.map((c) => (
            <button key={c.id} type="button" className="chatdock-item" onClick={() => setActiveId(c.id)}>
              <div className="chatdock-item-top">
                <strong>{c.name}</strong>
                <span>{c.updated}</span>
              </div>
              <span className="chatdock-preview">{c.messages[c.messages.length - 1]?.text}</span>
              {c.unread > 0 && <span className="chatdock-unread">{c.unread}</span>}
            </button>
          ))}
        </div>
      ) : (
        <>
          <div className="chatdock-context">
            страница {active.page}
            {active.video ? ` · видео «${active.video}»` : ""}
          </div>
          <div className="chatdock-msgs">
            {active.messages.map((m, i) => (
              <div key={i} className={`cab-msg cab-msg--${m.from}`}>
                <p>{m.text}</p>
                <time>{m.at}</time>
              </div>
            ))}
          </div>
          <div className="chatdock-reply">
            <input placeholder="Ответить клиенту…" disabled />
          </div>
        </>
      )}
    </div>
  );
}
