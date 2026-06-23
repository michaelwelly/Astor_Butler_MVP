"use client";

import { CURRENT_POLICY } from "@/lib/consent";

type Props = {
  onAccept: () => void;
  onDecline?: () => void;
};

/**
 * Short privacy notice shown before any chat/contact data leaves the browser
 * (FRONTEND_BACKEND_CONTRACTS.md §6). Accepting records consent evidence that
 * travels inside the Web Chat payload until backend persists anonymous consent.
 */
export function ConsentNotice({ onAccept, onDecline }: Props) {
  return (
    <div className="consent-notice" role="group" aria-label="Согласие на обработку данных">
      <p className="consent-notice-text">
        Перед отправкой сообщения подтвердите согласие с{" "}
        <a href={CURRENT_POLICY.url} target="_blank" rel="noopener noreferrer">
          политикой обработки персональных данных
        </a>
        . Мы используем эти данные только чтобы ответить на ваш запрос.
      </p>
      <div className="consent-notice-actions">
        <button type="button" className="consent-accept" onClick={onAccept}>
          Согласен и продолжить
        </button>
        {onDecline && (
          <button type="button" className="consent-decline" onClick={onDecline}>
            Позже
          </button>
        )}
      </div>
    </div>
  );
}
