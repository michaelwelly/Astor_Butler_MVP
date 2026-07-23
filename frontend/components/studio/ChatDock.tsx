"use client";

import { useState } from "react";
import { ChevronLeft, MessageSquare, X } from "lucide-react";
import { MOCK_CHATS } from "@/lib/studio";

/**
 * Always-on client-chat dock, bottom-right. Collapsed → a badge bubble;
 * expanded → conversation list ↔ thread. Demo data until GET /api/chats.
 */
export function ChatDock() {
  const [open, setOpen] = useState(false);
  const [activeId, setActiveId] = useState<string | null>(null);
  const active = MOCK_CHATS.find((c) => c.id === activeId);
  const totalUnread = MOCK_CHATS.reduce((sum, c) => sum + c.unread, 0);

  if (!open) {
    return (
      <button type="button" className="chatdock-fab" onClick={() => setOpen(true)} aria-label="Чат с клиентами">
        <MessageSquare size={22} />
        {totalUnread > 0 && <span className="chatdock-badge">{totalUnread}</span>}
      </button>
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
